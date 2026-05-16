package zed.rainxch.core.domain.util

import zed.rainxch.core.domain.model.GithubAsset

/**
 * Identifies the "variant" of a GitHub release asset — the part of the
 * filename that stays stable across releases (architecture, packaging
 * flavour, etc.) so the user's choice can survive a version bump.
 *
 * # Why this is non-trivial
 *
 * Release filenames are wildly inconsistent. The naïve approach
 * (substring after the version segment) breaks on a handful of common
 * shapes:
 *
 *   - **Arch-before-version**: `app-arm64-v8a-1.2.3.apk` — version is at
 *     the end, the variant token is in front of it
 *   - **Counter between version and arch**: `app-1.2.3-beta.1-arm64-v8a.apk`
 *     — substring tail would include `beta.1`, drifting release-over-release
 *   - **Version-code-only naming**: `app_1234_arm64-v8a.apk` — no dotted
 *     version anchor
 *   - **OS-version interlopers**: `app-android-12.0-1.2.3-arm64.apk` — the
 *     OS version is the first dotted-digit substring
 *
 * # The strategy
 *
 * Three layers of identity, each cheaper to compute than the next, all
 * consulted when matching against a fresh release:
 *
 *   1. **Token-set fingerprint** ([extractTokens]): pulls a *set* of
 *      well-known arch / flavor / qualifier tokens out of the filename
 *      regardless of position. Two filenames are "the same variant" if
 *      their token sets are equal. This is the primary identity and
 *      handles arch-before-version, OS-version interlopers, and counters
 *      in one go.
 *
 *   2. **Glob-pattern fingerprint** ([deriveGlob]): replaces version-shaped
 *      substrings with `*` (and digit-only run substrings with `*`),
 *      yielding e.g. `app-*-arm64-v8a.apk` from `app-1.2.3-arm64-v8a.apk`.
 *      Used as a secondary key when the token vocabulary doesn't recognise
 *      anything in the filename — covers custom flavor names like
 *      `myapp-foss-1.2.3.apk` → `myapp-foss-*.apk`.
 *
 *   3. **Substring-tail extract** ([extract]): the legacy fallback. Kept
 *      for surface compatibility with the existing UI (badge labels) and
 *      because it's a cheap way to display *something* readable to the
 *      user when the token vocabulary returns nothing.
 *
 * # Vocabulary policy
 *
 * The token vocabulary is intentionally a closed set. Open-ended token
 * extraction (e.g. "anything that's not a digit run") leaks app names,
 * release qualifiers, and date components into the variant identity,
 * which is exactly what the substring-tail approach gets wrong. The
 * vocabulary covers the tokens that actually distinguish APK variants
 * in the wild — architectures, install flavors, signing qualifiers.
 *
 * Casing is normalised to lowercase before comparison; some maintainers
 * flip casing release-over-release.
 */
object AssetVariant {
    /**
     * Closed vocabulary of tokens that are meaningful for variant
     * identity. Anything *not* in this set is considered noise (app name,
     * release qualifier, date component, etc.) and is ignored.
     *
     * Architecture tokens dominate the list because that's the most
     * common variant axis on Android. Flavor tokens (`fdroid`, `play`,
     * `foss`, `gms`, `nogms`, `huawei`) cover the second-most-common
     * axis: same arch, different distribution channel.
     *
     * `release` and `signed` are explicitly excluded — they appear in
     * a lot of filenames but rarely *distinguish* assets within a
     * single release. Including them would create false negatives
     * when one release uses `release-arm64-v8a` and the next uses
     * `arm64-v8a`. Same reasoning applies to `aligned`, `unsigned`.
     */
    private val ARCH_TOKENS =
        setOf(
            // 64-bit ARM (the modern default on Android)
            "arm64-v8a",
            "arm64",
            "aarch64",
            // 32-bit ARM
            "armeabi-v7a",
            "armeabi",
            "armv7",
            "armv7a",
            "armv8",
            // x86 family
            "x86_64",
            "x86-64",
            "x64",
            "x86",
            "i386",
            "i686",
            // MIPS (rare but real on legacy hardware)
            "mips",
            "mips64",
            // Universal / fat APKs
            "universal",
            "all",
        )

    private val FLAVOR_TOKENS =
        setOf(
            // Distribution channels
            "fdroid",
            "f-droid",
            "play",
            "playstore",
            "googleplay",
            "gms",
            "nogms",
            "huawei",
            "amazon",
            "samsung",
            // Build flavors
            "foss",
            "libre",
            "free",
            "pro",
            "premium",
            "full",
            "lite",
            "beta",
            "stable",
            "canary",
            "nightly",
        )

    /**
     * The full vocabulary used for token-set comparison. Lazily merged
     * because the JVM `Set` union allocates and there's no reason to do
     * it on every call.
     */
    private val VOCABULARY: Set<String> by lazy { ARCH_TOKENS + FLAVOR_TOKENS }

    /**
     * Strips hyphens/underscores so alias spellings of the same flavor
     * (`f-droid` ⇄ `fdroid`, `play-store` ⇄ `playstore`) collapse to a
     * single comparable form. Used by [filterByPackageFlavor] to align
     * asset-token output (which may carry hyphenated bi-grams) with
     * package-segment input (which can't legally contain hyphens).
     */
    private fun canonicalFlavorToken(token: String): String =
        token.replace('-', ' ').replace('_', ' ').replace(" ", "").lowercase()

    /**
     * Splits a filename into candidate tokens. Splits on the usual
     * separators (`-`, `_`, ` `, `.`) so that compound names like
     * `arm64-v8a` survive intact only after the recombine step below.
     *
     * Note: tokens are *normalised to lowercase* and the file extension
     * is stripped first.
     */
    private fun tokenize(assetName: String): List<String> {
        val withoutExt = assetName.substringBeforeLast('.')
        return withoutExt
            .lowercase()
            .split('-', '_', ' ', '.')
            .filter { it.isNotEmpty() }
    }

    /**
     * Extracts the **token-set fingerprint** of [assetName]. Returns the
     * subset of [VOCABULARY] that appears in the filename, accounting for
     * compound tokens that span the splitter (e.g. `arm64-v8a` is
     * tokenised as `["arm64", "v8a"]` but recognised as the compound
     * `arm64-v8a` via a sliding-window check).
     *
     * Two assets share the same variant identity iff [extractTokens]
     * returns equal sets.
     *
     * Returns an empty set when the filename contains no recognisable
     * tokens — that's a deliberate "no token-based identity available,
     * fall through to the next layer" signal.
     */
    fun extractTokens(assetName: String): Set<String> {
        val tokens = tokenize(assetName)
        if (tokens.isEmpty()) return emptySet()

        val found = mutableSetOf<String>()

        // First pass: 3-grams, 2-grams, then 1-grams. Order matters
        // because longer matches should take precedence: matching
        // `armeabi-v7a` should not also match the bare `armeabi`
        // afterwards (they're the same conceptual variant in different
        // tokenisations of the maintainer's filename).
        val consumed = BooleanArray(tokens.size)

        // 3-grams (rare but `armeabi-v7a-release` style exists)
        for (i in 0 until tokens.size - 2) {
            if (consumed[i] || consumed[i + 1] || consumed[i + 2]) continue
            val candidate = "${tokens[i]}-${tokens[i + 1]}-${tokens[i + 2]}"
            if (candidate in VOCABULARY) {
                found += candidate
                consumed[i] = true
                consumed[i + 1] = true
                consumed[i + 2] = true
            }
        }

        // 2-grams: `arm64-v8a`, `armeabi-v7a`, `x86-64`, `x86_64`
        // The tokenizer split on both `-` and `_`, so `x86_64` becomes
        // `["x86", "64"]`; we recombine with `-` for vocabulary lookup
        // *and* try the underscore form to cover `x86_64`. Both forms
        // appear in the wild from different maintainers.
        for (i in 0 until tokens.size - 1) {
            if (consumed[i] || consumed[i + 1]) continue
            val dashed = "${tokens[i]}-${tokens[i + 1]}"
            val underscored = "${tokens[i]}_${tokens[i + 1]}"
            val match =
                when {
                    dashed in VOCABULARY -> dashed
                    underscored in VOCABULARY -> underscored
                    else -> null
                }
            if (match != null) {
                found += match
                consumed[i] = true
                consumed[i + 1] = true
            }
        }

        // 1-grams: bare tokens
        for (i in tokens.indices) {
            if (consumed[i]) continue
            if (tokens[i] in VOCABULARY) {
                found += tokens[i]
                consumed[i] = true
            }
        }

        return found
    }

    /**
     * Derives a **glob-pattern fingerprint** from [assetName]: replaces
     * any dotted-digit version segment and any standalone digit run with
     * `*`, leaving everything else intact. Used as a secondary identity
     * when [extractTokens] returns an empty set.
     *
     * Examples:
     *
     *   `app-1.2.3-arm64-v8a.apk`        → `app-*-arm64-v8a.apk`
     *   `myapp-foss-1.2.3.apk`           → `myapp-foss-*.apk`
     *   `Project_v2.0.1_universal.apk`   → `project_v*_universal.apk`
     *   `release-2024.04.10-debug.apk`   → `release-*-debug.apk`
     *   `app_1234_arm64.apk`             → `app_*_arm64.apk`
     *
     * The result is also lowercased so two maintainers with different
     * casing conventions still produce the same fingerprint.
     *
     * Returns `null` when the filename has no version-shaped or
     * digit-run substring at all — there'd be nothing to wildcard, so
     * the glob would just equal the filename and provides no rescue
     * value beyond exact-match.
     */
    /**
     * Extracts the **base-name stem** of an asset — the lowercased,
     * separator-stripped concatenation of every token that isn't a
     * version-like number, an arch token, or a flavor token. Used to
     * detect "sibling app in the same repo" cases where two releases
     * ship `AppA-1.10.apk` and `AppB-2.20.apk` and the auto-picker
     * would otherwise swap one for the other based on numeric version
     * alone (issue #591).
     *
     * `AppA-1.10.apk` → `"appa"`
     * `AppB-2.20.apk` → `"appb"`
     * `app-arm64-v8a-1.10.apk` and `app-x86_64-1.10.apk` → both `"app"`
     * `app-1.0.apk` and `app-fdroid-1.0.apk` → both `"app"`
     *
     * Returns an empty string when stripping leaves nothing behind
     * (release ships only a versioned filename like `2.0.apk`). Callers
     * treat empty as "no stem signal — don't filter".
     */
    fun extractBaseStem(assetName: String): String {
        val tokens = tokenize(assetName)
        if (tokens.isEmpty()) return ""

        // Mirror `extractTokens`' n-gram consumption pass so the
        // fragments of compound vocab entries (e.g. `arm64-v8a` →
        // tokens `["arm64","v8a"]`) are both stripped, not just the
        // canonical-form half. Without this, `v8a` / `v7a` would
        // survive the filter and `app-arm64-v8a-1.10.apk` would yield
        // a different stem than `app-x86_64-1.10.apk`, defeating the
        // sibling-app detection for arch-variant releases.
        val consumed = BooleanArray(tokens.size)

        for (i in 0 until tokens.size - 2) {
            if (consumed[i] || consumed[i + 1] || consumed[i + 2]) continue
            val candidate = "${tokens[i]}-${tokens[i + 1]}-${tokens[i + 2]}"
            if (candidate in VOCABULARY) {
                consumed[i] = true; consumed[i + 1] = true; consumed[i + 2] = true
            }
        }
        for (i in 0 until tokens.size - 1) {
            if (consumed[i] || consumed[i + 1]) continue
            val dashed = "${tokens[i]}-${tokens[i + 1]}"
            val underscored = "${tokens[i]}_${tokens[i + 1]}"
            if (dashed in VOCABULARY || underscored in VOCABULARY) {
                consumed[i] = true; consumed[i + 1] = true
            }
        }

        val out = StringBuilder()
        for (i in tokens.indices) {
            if (consumed[i]) continue
            val t = tokens[i]
            if (t in VOCABULARY) continue
            if (isVersionLikeToken(t)) continue
            out.append(t)
        }
        return out.toString()
    }

    /**
     * `1`, `10`, `1.0.0`, `v2.0.1`, `2024.04.10`, `1.0-rc1`, `beta3` —
     * common patterns used in release filenames to encode the version.
     * Conservative on purpose: false positives here just lose a stem
     * character; false negatives would let a numeric variant leak into
     * the stem and break the sibling-app detection.
     */
    private fun isVersionLikeToken(token: String): Boolean {
        if (token.isEmpty()) return false
        if (token.all { it.isDigit() }) return true
        if (token.startsWith("v") && token.drop(1).all { it.isDigit() }) return true
        return false
    }

    fun deriveGlob(assetName: String): String? {
        val lower = assetName.lowercase()
        // Match either:
        //   - a versioned segment with at least one dot (e.g. `1.2.3`,
        //     `v2.0.1`, `2024.04.10`)
        //   - OR a standalone digit run of length >= 2 (the `1234`
        //     version-code-only case). Length >= 2 avoids replacing
        //     legitimate single-digit tokens like `v8` in `arm64-v8a`.
        val versionPattern =
            Regex("""v?\d+(?:\.\d+)+|(?<![A-Za-z\d])\d{2,}(?![A-Za-z\d])""")
        if (!versionPattern.containsMatchIn(lower)) return null
        return versionPattern.replace(lower, "*")
    }

    /**
     * Legacy substring-tail extractor — kept for surface compatibility
     * with the existing badge UI. Anchors on the *first* dotted-digit
     * version segment in the filename and returns whatever follows it,
     * stripped of leading separators.
     *
     * Returns the empty string when a version anchor exists but nothing
     * meaningful follows it (single-asset releases, version-tail names),
     * and `null` when no version anchor is found at all. The two cases
     * stay distinct so callers can decide whether to fall back.
     *
     * **Prefer [extractTokens] for matching.** This function exists
     * because the UI has historically displayed the tail string as the
     * variant label, and that text is now part of users' mental model.
     */
    fun extract(assetName: String): String? {
        val withoutExt = assetName.substringBeforeLast('.')
        val match = VERSION_SEGMENT.find(withoutExt) ?: return null
        var tail =
            withoutExt
                .substring(match.range.last + 1)
                .trimStart(*LEADING_SEPARATORS)
                .trim()
        // Strip pre-release qualifier counters (beta.24, rc.1, m12, …)
        // that drift release-over-release. Issue #612.
        while (true) {
            val qmatch = PRE_RELEASE_PREFIX.find(tail) ?: break
            tail = tail.substring(qmatch.range.last + 1)
                .trimStart(*LEADING_SEPARATORS)
                .trim()
        }
        return tail
    }

    /**
     * Stricter version segment matcher used by [extract]. Requires:
     *  - leading separator (so `app2-x` doesn't false-match on `2`)
     *  - optional `v` prefix
     *  - at least two dotted-digit groups (so `-v8` in `arm64-v8a` is
     *    rejected — `arm64-v8a` is an arch token, not a version)
     *  - trailing token boundary (so `1.2.3pre` doesn't leak `pre`)
     */
    private val VERSION_SEGMENT =
        Regex("[-_ ]v?\\d+(?:\\.\\d+)+(?=[-_. ]|$)", RegexOption.IGNORE_CASE)

    private val PRE_RELEASE_PREFIX =
        Regex(
            "^(beta|rc|alpha|dev|nightly|snapshot|pre|preview|m|milestone|pr)[.\\-_]?\\d+(?=[-_. ]|$)",
            RegexOption.IGNORE_CASE,
        )

    private val LEADING_SEPARATORS = charArrayOf('-', '_', ' ', '.')

    /**
     * Resolves the asset that matches the user's pinned variant, walking
     * the three identity layers in order:
     *
     *   1. **Token-set match** — pinned tokens equal the asset's tokens
     *   2. **Glob match** — pinned glob equals the asset's glob
     *   3. **Tail-string match** — case-insensitive equality of the
     *      legacy substring-tail extract
     *
     * Returns the first asset that satisfies any layer, or `null` when
     * no asset matches and the caller should fall back to the platform
     * auto-picker.
     *
     * [pinnedTokens] and [pinnedGlob] are the persisted fingerprints from
     * `InstalledApp.preferredAssetTokens` and `assetGlobPattern`. Either
     * may be null/empty (older rows or fingerprints we couldn't derive
     * at pin time). When *both* are absent the function still tries the
     * tail-string match to keep older rows working.
     */
    fun resolvePreferredAsset(
        assets: List<GithubAsset>,
        pinnedVariant: String?,
        pinnedTokens: Set<String>? = null,
        pinnedGlob: String? = null,
    ): GithubAsset? {
        // Layer 1: token-set match. The strongest signal — survives
        // arch-before-version, OS-version interlopers, and counters.
        if (!pinnedTokens.isNullOrEmpty()) {
            val match = assets.firstOrNull { asset ->
                extractTokens(asset.name) == pinnedTokens
            }
            if (match != null) return match
        }

        // Layer 2: glob match. Catches custom flavor names that the
        // closed token vocabulary doesn't know about.
        if (!pinnedGlob.isNullOrBlank()) {
            val match = assets.firstOrNull { asset ->
                deriveGlob(asset.name) == pinnedGlob
            }
            if (match != null) return match
        }

        // Layer 3: legacy tail-string match. Keeps rows pinned before
        // the multi-layer rewrite working without forcing a re-pick.
        val target = pinnedVariant?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return assets.firstOrNull { asset ->
            extract(asset.name)?.equals(target, ignoreCase = true) == true
        }
    }

    /**
     * Same-position fallback used by the resolver as a last resort
     * before falling back to the platform auto-picker. When the new
     * release contains exactly the same number of installable assets
     * as the original picked-from release, returning the asset at the
     * same index preserves the user's intent in cases where every
     * asset has been renamed in lockstep (e.g. an entire flavor
     * dimension was added).
     *
     * Returns `null` when [siblingCountAtPickTime] is null, zero,
     * or doesn't match `assets.size`.
     */
    fun resolveBySamePosition(
        assets: List<GithubAsset>,
        originalIndex: Int?,
        siblingCountAtPickTime: Int?,
    ): GithubAsset? {
        if (originalIndex == null || siblingCountAtPickTime == null) return null
        if (siblingCountAtPickTime <= 0) return null
        if (assets.size != siblingCountAtPickTime) return null
        return assets.getOrNull(originalIndex)
    }

    /**
     * Pulls the variant tag out of a sample asset filename and returns
     * it normalised, or `null` when the name doesn't carry a meaningful
     * variant. Skips the work entirely when [siblingAssetCount] is 1 or 0
     * because single-asset releases have nothing to remember.
     *
     * The returned tail is the **display label** that gets stored in
     * `InstalledApp.preferredAssetVariant` and shown to the user. The
     * matching algorithm itself uses [extractTokens] / [deriveGlob] —
     * those are also derived at pin time and stored separately.
     *
     * Single-asset releases and "no variant suffix" filenames both return
     * `null` rather than the empty string — there's nothing to pin.
     */
    fun deriveFromPickedAsset(
        pickedAssetName: String,
        siblingAssetCount: Int,
    ): String? {
        if (siblingAssetCount <= 1) return null
        val variant = extract(pickedAssetName) ?: return null
        return variant.takeIf { it.isNotEmpty() }
    }

    /**
     * Bundle of all three identity layers derived from a single picked
     * asset filename. Used by the persistence path so the caller can
     * write all fingerprints atomically and the resolver can match on
     * any of them later.
     *
     * - [variant]: legacy substring tail (display label)
     * - [tokens]: token-set fingerprint, empty when the filename has
     *   no vocabulary tokens
     * - [glob]: glob-pattern fingerprint, null when there's no
     *   version-shaped substring to wildcard
     *
     * Returns `null` when [siblingAssetCount] <= 1 (nothing to pin) or
     * when *all three* identity layers came up empty — at that point
     * the asset has no stable fingerprint and pinning would be a lie.
     */
    data class VariantFingerprint(
        val variant: String?,
        val tokens: Set<String>,
        val glob: String?,
    )

    fun fingerprintFromPickedAsset(
        pickedAssetName: String,
        siblingAssetCount: Int,
    ): VariantFingerprint? {
        if (siblingAssetCount <= 1) return null
        val variant = extract(pickedAssetName)?.takeIf { it.isNotEmpty() }
        val tokens = extractTokens(pickedAssetName)
        val glob = deriveGlob(pickedAssetName)
        // If everything is empty there's nothing to remember — return
        // null so callers persist `null` and fall back to the platform
        // auto-picker on update.
        if (variant == null && tokens.isEmpty() && glob == null) return null
        return VariantFingerprint(variant = variant, tokens = tokens, glob = glob)
    }

    /**
     * Serializes a token set to a stable string for storage. Sorted so
     * that identical sets always serialize to identical strings, which
     * is what makes string equality a valid set-equality check at the
     * SQL layer (avoiding a JSON column or a join table).
     *
     * Format: tokens joined by `|`. Returns `null` for empty sets so
     * the column can stay nullable and the resolver knows when there's
     * no token fingerprint to compare against.
     */
    fun serializeTokens(tokens: Set<String>): String? {
        if (tokens.isEmpty()) return null
        return tokens.sorted().joinToString("|")
    }

    fun deserializeTokens(serialized: String?): Set<String> {
        if (serialized.isNullOrBlank()) return emptySet()
        return serialized.split('|').filter { it.isNotBlank() }.toSet()
    }

    /**
     * Narrows [assets] to the subset whose filename flavor matches the
     * flavor implied by [trackedPackageName]. Used by the update-check
     * auto-picker to avoid swapping the user's installed APK for a
     * sibling release artifact that ships a different package id
     * (typically a `.fdroid` variant alongside the stock package).
     *
     * The picker only knows asset names — the actual APK package is
     * inside the binary, which would require downloading every
     * candidate. Filenames are usually honest about the flavor though:
     * an `.apk` named `app-fdroid-release.apk` almost always installs
     * with package id `<base>.fdroid`. We exploit that to keep the
     * auto-pick safe by default.
     *
     * Rules:
     *  - If the tracked package contains any [FLAVOR_TOKENS] segment
     *    (case-insensitive, dot-separated), keep only assets whose
     *    filename tokens contain at least one of those flavor tokens.
     *  - Otherwise (stock package, no flavor marker), keep only assets
     *    whose filename tokens contain NO flavor markers.
     *  - If either filter would eliminate every candidate the input is
     *    returned unchanged — losing the auto-update prompt is worse
     *    than picking a marginally wrong-flavor asset, and the user
     *    can still pin the right variant from the picker UI.
     */
    fun filterByPackageFlavor(
        assets: List<GithubAsset>,
        trackedPackageName: String,
    ): List<GithubAsset> {
        if (assets.isEmpty()) return assets
        val packageSegments =
            trackedPackageName.lowercase().split('.').filter { it.isNotBlank() }
        // Canonicalise both sides via hyphen/underscore strip so package
        // segments (`fdroid`, can't legally hold `-`) match against asset
        // tokens that may come back hyphenated (`f-droid`) when the
        // filename uses an aliased spelling. Without this, the set
        // intersection misses the alias forms `FLAVOR_TOKENS` deliberately
        // covers.
        val packageFlavorTokens = packageSegments.filter { it in FLAVOR_TOKENS }
            .map(::canonicalFlavorToken)
            .toSet()

        return if (packageFlavorTokens.isNotEmpty()) {
            val matching = assets.filter { asset ->
                extractTokens(asset.name).any { canonicalFlavorToken(it) in packageFlavorTokens }
            }
            matching.ifEmpty { assets }
        } else {
            val unflavoured = assets.filter { asset ->
                extractTokens(asset.name).none { it in FLAVOR_TOKENS }
            }
            unflavoured.ifEmpty { assets }
        }
    }
}
