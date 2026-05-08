package zed.rainxch.core.domain.util

/**
 * Single source of truth for version-string normalization and ordering
 * across the app. Both the periodic update check
 * (`InstalledAppsRepositoryImpl.checkForUpdates`) and the external-install
 * detection path (`ExternalInstallVerdict`) now call through here so a
 * single comparator change propagates everywhere instead of drifting
 * between private copies.
 *
 * Design invariants:
 *  - Every public function is pure; no I/O, no time, no randomness.
 *  - Inputs are `String?` where realistic so callers don't have to
 *    guard against nulls from the DB or the release feed.
 *  - Semver-compatible strings get semver semantics (including
 *    `-preRelease` ordering per spec: `1.0.0-beta < 1.0.0`).
 *  - Non-semver strings degrade gracefully: we try to extract a
 *    dotted-digit core (so `release-1.2.3` still compares like
 *    `1.2.3`), and only fall back to lexicographic comparison when
 *    the string has no recognisable version core at all.
 */
object VersionMath {
    /**
     * Reduces a tag or installed-version string to a form that
     * [parseSemanticVersion] can digest.
     *
     * Strategy, in order:
     *   1. Trim and strip common tag prefixes (`refs/tags/`, `v`, `V`).
     *   2. Drop `+build` metadata (per semver spec, ignored for
     *      ordering).
     *   3. If the result parses as semver, return it.
     *   4. Otherwise extract the first dotted-digit substring
     *      (optionally followed by a `-pre` identifier) and return
     *      that — handles maintainer prefixes like `release-1.2.0`,
     *      `App-v1.2.0-stable`, `build-2025.04.10`.
     *   5. If nothing numeric is found at all, return the cleaned
     *      string so the caller can fall back to equality / lex.
     *
     * Examples:
     *   `v1.2.3`               → `1.2.3`
     *   `1.2.3+sha.abcd`       → `1.2.3`
     *   `1.2.3-rc1`            → `1.2.3-rc1`
     *   `release-1.2.0`        → `1.2.0`
     *   `App-v1.2.0-stable`    → `1.2.0-stable`
     *   `build-2025.04.10`     → `2025.04.10`
     *   `refs/tags/v1.2.3`     → `1.2.3`
     *   `not-a-version`        → `not-a-version`
     *   `null`                 → `""`
     */
    fun normalizeVersion(version: String?): String {
        if (version.isNullOrBlank()) return ""
        val cleaned = stripFullPrefix(version)
        val withoutBuildMetadata = cleaned.substringBefore('+')
        // Hyphenated calver (`2024-10-15`) — semver would otherwise read
        // the trailing `-10-15` as a pre-release identifier and rank
        // `2024-10-15 < 2024`, which is the opposite of what users mean
        // by a date-stamped release. Re-shape into dotted calver so the
        // semver path numeric-compares each segment.
        val calverNormalized = normalizeCalverHyphen(withoutBuildMetadata)
        // Adjacent-letter pre-release (`1.2.0beta01`) — insert the
        // missing hyphen so the numeric core and the marker can be
        // parsed independently. Only inserts before known markers to
        // avoid mauling architecture suffixes like `1arm64`.
        val separated = insertHyphenBeforeKnownMarker(calverNormalized)
        if (parseSemanticVersion(separated) != null) {
            return separated
        }
        val match = DOTTED_DIGIT_PATTERN.find(separated)
        return match?.value ?: separated
    }

    private fun stripFullPrefix(version: String): String {
        val trimmed =
            version
                .trim()
                .removePrefix("refs/tags/")
                .trim()
        // Word-style prefixes like `version-`, `release/`, `app_`,
        // `build-`, `ver.` — common in maintainer-customised tag
        // schemes. Strips a single leading word and any single
        // separator before the version core. Case-insensitive.
        val wordMatch = VERSION_WORD_PREFIX.find(trimmed)
        val withoutWord = if (wordMatch != null) trimmed.substring(wordMatch.range.last + 1) else trimmed
        return withoutWord.removePrefix("v").removePrefix("V").trim()
    }

    private fun normalizeCalverHyphen(s: String): String {
        val m = CALVER_HYPHEN_PATTERN.matchEntire(s) ?: return s
        val year = m.groupValues[1]
        val month = m.groupValues[2].padStart(2, '0')
        val day = m.groupValues[3].padStart(2, '0')
        val tail = m.groupValues.getOrNull(4).orEmpty()
        val core = "$year.$month.$day"
        return if (tail.isNotEmpty()) "$core-$tail" else core
    }

    private fun insertHyphenBeforeKnownMarker(s: String): String {
        val match = ADJACENT_ALPHA_PATTERN.find(s) ?: return s
        val letterStart = match.range.first + 1
        val tail = s.substring(letterStart).lowercase()
        // `m\d+` covers the JetBrains-style milestone shorthand (`m5`,
        // `m12`) which `PRE_RELEASE_MARKER_PATTERN` already matches but
        // a string `startsWith` over [KNOWN_PRE_RELEASE_PREFIXES] does
        // not — without the explicit regex check, `1.2.0m5` would slip
        // past detection and silently rank as stable `1.2.0`.
        val isKnownMarker =
            KNOWN_PRE_RELEASE_PREFIXES.any { tail.startsWith(it) } ||
                M_DIGIT_TAIL_PATTERN.containsMatchIn(tail)
        if (!isKnownMarker) return s
        return s.substring(0, letterStart) + "-" + s.substring(letterStart)
    }

    /**
     * Returns `true` if [candidate] is strictly newer than [current]
     * after normalization. Handles semver (including pre-release
     * ordering per spec) and falls back to lexicographic comparison
     * for strings with no parseable version core.
     *
     * Both arguments are normalized via [normalizeVersion] before
     * comparison, so callers can pass raw tag strings.
     */
    fun isVersionNewer(candidate: String?, current: String?): Boolean {
        val normCandidate = normalizeVersion(candidate)
        val normCurrent = normalizeVersion(current)
        if (normCandidate.isEmpty() || normCurrent.isEmpty()) return false
        if (normCandidate == normCurrent) return false
        return compareNormalized(normCandidate, normCurrent) > 0
    }

    /**
     * Three-way comparison of two raw version strings after
     * normalization. Returns a positive int if [a] > [b], negative if
     * [a] < [b], `0` if equal or both empty.
     *
     * Use this when you need the full ordering (e.g. detecting
     * downgrades). Prefer [isVersionNewer] when you just need a
     * boolean.
     */
    fun compareVersions(a: String?, b: String?): Int {
        val normA = normalizeVersion(a)
        val normB = normalizeVersion(b)
        return compareNormalized(normA, normB)
    }

    /**
     * Returns `true` when both inputs normalize to the same version.
     * Tolerates prefix/format drift the GitHub feed routinely produces
     * (e.g. release tag `v3.1.3` vs system-reported `3.1.3`,
     * `release-1.2.0` vs `1.2.0`, `1.2.3+sha.abcd` vs `1.2.3`).
     *
     * Two empty/null/blank inputs are treated as equal — this is fine
     * for the UI-text guards that use this (don't render a redundant
     * "installed: …" subtext when there's nothing meaningful to show).
     * Callers that need a stricter "both present and equal" check
     * should compare [normalizeVersion] against `""` first.
     */
    fun isSameVersion(a: String?, b: String?): Boolean = compareVersions(a, b) == 0

    /**
     * Strict literal equality after the conservative cleanup pass that
     * [stripCommonPrefixes] (delegating to [stripFullPrefix]) applies:
     * trim, strip `refs/tags/`, strip a single case-insensitive
     * word-style prefix with separator (`version-`, `release/`, `app_`,
     * `build-`, `ver.`), strip a leading `v` / `V`, trim again.
     *
     * Differs from [isSameVersion] in that it does NOT strip `+build`
     * metadata, does NOT extract a dotted-digit core from arbitrarily
     * prefixed tags, and is case-sensitive on the suffix. Use this in
     * UI branches that gate "Open" vs "Install" CTAs — semver treats
     * `1.0.0+build.1` and `1.0.0+build.2` as equivalent for ordering,
     * but users (and maintainers who abuse build metadata to ship
     * distinct artifacts under the same numeric core) consider them
     * different versions.
     *
     * Two null/blank inputs return `false`. The check requires both
     * sides to be present; otherwise the caller would gate UI on
     * "two unknowns are the same", which is never the intent.
     */
    fun isExactSameVersion(a: String?, b: String?): Boolean {
        val cleanedA = stripCommonPrefixes(a) ?: return false
        val cleanedB = stripCommonPrefixes(b) ?: return false
        return cleanedA == cleanedB
    }

    private fun stripCommonPrefixes(version: String?): String? {
        if (version.isNullOrBlank()) return null
        val cleaned = stripFullPrefix(version)
        return cleaned.takeIf { it.isNotEmpty() }
    }

    private fun compareNormalized(a: String, b: String): Int {
        if (a == b) return 0
        val parsedA = parseSemanticVersion(a)
        val parsedB = parseSemanticVersion(b)
        if (parsedA != null && parsedB != null) {
            return compareSemver(parsedA, parsedB)
        }
        // Neither is parseable as semver — last-resort lexicographic
        // comparison. Callers should treat this as low-confidence.
        return a.compareTo(b)
    }

    private fun compareSemver(a: SemanticVersion, b: SemanticVersion): Int {
        val maxLen = maxOf(a.numbers.size, b.numbers.size)
        for (i in 0 until maxLen) {
            val ai = a.numbers.getOrElse(i) { 0L }
            val bi = b.numbers.getOrElse(i) { 0L }
            if (ai != bi) return ai.compareTo(bi)
        }
        // Numeric parts equal — spec: stable > pre-release when
        // pre-release only present on one side.
        return when {
            a.preRelease == null && b.preRelease == null -> 0
            a.preRelease == null -> 1 // a has no pre, so a > b
            b.preRelease == null -> -1
            else -> comparePreRelease(a.preRelease, b.preRelease)
        }
    }

    /**
     * Compare pre-release identifiers per semver spec:
     *  - Identifiers consisting of only digits are compared
     *    numerically.
     *  - Identifiers with letters are compared lexically.
     *  - Numeric identifiers always have lower precedence than
     *    alphanumeric.
     *  - A larger set of pre-release fields has higher precedence if
     *    all preceding are equal.
     */
    private fun comparePreRelease(a: String, b: String): Int {
        val aParts = a.split(".")
        val bParts = b.split(".")
        for (i in 0 until minOf(aParts.size, bParts.size)) {
            val ap = aParts[i]
            val bp = bParts[i]
            val aNum = ap.toLongOrNull()
            val bNum = bp.toLongOrNull()
            val cmp =
                when {
                    aNum != null && bNum != null -> aNum.compareTo(bNum)
                    aNum != null -> -1 // numeric < alphanumeric
                    bNum != null -> 1
                    else -> ap.compareTo(bp)
                }
            if (cmp != 0) return cmp
        }
        return aParts.size.compareTo(bParts.size)
    }

    private data class SemanticVersion(
        val numbers: List<Long>,
        val preRelease: String?,
    )

    private fun parseSemanticVersion(version: String): SemanticVersion? {
        if (version.isEmpty()) return null
        val hyphenIndex = version.indexOf('-')
        val numberPart = if (hyphenIndex >= 0) version.substring(0, hyphenIndex) else version
        val preRelease =
            if (hyphenIndex >= 0 && hyphenIndex < version.length - 1) {
                version.substring(hyphenIndex + 1)
            } else {
                null
            }
        val parts = numberPart.split(".")
        val numbers = parts.mapNotNull { it.toLongOrNull() }
        if (numbers.isEmpty() || numbers.size != parts.size) return null
        return SemanticVersion(numbers, preRelease)
    }

    private val DOTTED_DIGIT_PATTERN = Regex("""\d+(?:\.\d+)*(?:-[\w.]+)?""")

    /**
     * Word-style tag prefix that some maintainers use instead of the
     * usual leading `v`. Recognises a single leading word followed by
     * an optional separator and the version core. Case-insensitive so
     * `Release_1.2.0`, `release/1.2.0`, `App-1.2.0` all collapse.
     *
     * The trailing class allows `-`, `_`, `/`, `.` or whitespace as
     * separators; the regex explicitly does NOT match the bare prefix
     * with no separator (`version1.2.3`) — that's an unusual format
     * and safer left to the dotted-digit fallback.
     */
    private val VERSION_WORD_PREFIX =
        Regex(
            """^(version|release|app|build|ver)\s*[-_/.]\s*""",
            RegexOption.IGNORE_CASE,
        )

    /**
     * Hyphenated calver: `2024-10-15`, `2024-3-1`, optionally followed
     * by a trailing identifier (`2024-10-15-rc1`). Year is constrained
     * to 1900–2199 to avoid swallowing semver pre-release identifiers
     * that start with a small integer (`1.0-10-rc1` should NOT be
     * treated as the year 10).
     */
    private val CALVER_HYPHEN_PATTERN =
        Regex("""^((?:19|20|21)\d{2})-(\d{1,2})-(\d{1,2})(?:[-.](.+))?$""")

    /**
     * Catches `1.2.0beta01` / `2.0RC1` / `0.9preview2` — a digit
     * directly followed by a letter, no separator. Used to insert the
     * missing hyphen ONLY when the tail starts with a known
     * pre-release marker (architecture suffixes like `1.2.0arm64` are
     * left intact).
     */
    private val ADJACENT_ALPHA_PATTERN = Regex("""\d[A-Za-z]""")

    /**
     * JetBrains-style milestone shorthand match used in
     * [insertHyphenBeforeKnownMarker]. Matches `m1`, `m12`, `M5`,
     * etc. at the start of a tail like `m5-arm64`. Kept separate
     * from [KNOWN_PRE_RELEASE_PREFIXES] because that list is
     * `startsWith`-friendly literal prefixes; this one needs a
     * regex.
     */
    private val M_DIGIT_TAIL_PATTERN = Regex("""^m\d+""", RegexOption.IGNORE_CASE)

    /**
     * 8-digit date integer like `20260502`. Year constrained to
     * 1900-2199 to keep this from swallowing arbitrary 8-digit
     * integers that maintainers might use as monotonic build numbers
     * unrelated to the calendar.
     */
    private val DATE_INTEGER_PATTERN = Regex("""(?:19|20|21)\d{2}\d{2}\d{2}""")

    /**
     * Dotted calver — `2024.10.15`, optionally with a trailing build
     * identifier (`2024.10.15.4567`). Year guard same as [DATE_INTEGER_PATTERN].
     */
    private val DOTTED_CALVER_PATTERN =
        Regex("""(?:19|20|21)\d{2}\.\d{1,2}\.\d{1,2}(?:\.\d+)?""")

    /**
     * Bare commit-hash style tag (7-40 lowercase hex chars). Some
     * repositories use commit SHAs as release tags — these never
     * compare meaningfully but should be classified as such so UIs
     * can render them as "build" rather than as a version number.
     */
    private val COMMIT_HASH_PATTERN = Regex("""[0-9a-f]{7,40}""")

    /**
     * Marker prefixes recognised when separating an adjacent-letter
     * pre-release. Mirrors [PRE_RELEASE_MARKER_PATTERN] but as plain
     * strings for the `startsWith` check.
     */
    private val KNOWN_PRE_RELEASE_PREFIXES =
        listOf(
            "alpha",
            "beta",
            "rc",
            "preview",
            "prerelease",
            "snapshot",
            "canary",
            "nightly",
            "milestone",
            "ea",
            "dev",
            "pre",
        )

    /**
     * Heuristic: returns `true` when [tag] contains a well-known
     * pre-release marker.
     *
     * Why this exists: the GitHub API exposes a `prerelease: bool`
     * flag on every release, but **maintainers regularly forget to
     * set it**. A release tagged `v2.0.0-rc.1` with `prerelease:
     * false` is still semantically a pre-release, and surfacing it
     * as a stable update to opted-out users is a silent foot-gun.
     * `GithubRelease.isEffectivelyPreRelease()` combines the API flag
     * with this tag heuristic so one is enough.
     *
     * Recognised markers (case-insensitive), preceded by `-`, `.`,
     * or `_`, and followed by a separator, digit, or end-of-string:
     *  - `alpha`, `beta`, `rc` — classic semver pre-release labels
     *  - `preview`, `snapshot`, `canary`, `nightly` — CI / early builds
     *  - `milestone` / `m\d+` — JetBrains-style milestone builds
     *  - `ea` — early access (Oracle / JetBrains / vendor convention)
     *  - `dev` — dev build shorthand
     *  - `pre` — generic pre-release prefix when followed by digit or dot
     *
     * Intentionally **not** recognised (too ambiguous / too many
     * false positives):
     *  - `test` (`-test-build` is often a real release artefact)
     *  - `a\d+` / `b\d+` alone (collides with `-arm64`, `-amd64`, etc.)
     *  - `stable` / `release` (explicit non-markers)
     *
     * Examples that match:
     *   `v1.2.3-beta`, `1.2.3-alpha.1`, `v2-rc.2`, `1.0.0-preview2`,
     *   `2025.04-nightly`, `v1.0.0-canary.3`, `1.0-m5`,
     *   `0.9.0-snapshot`, `7.0-ea`
     *
     * Examples that DO NOT match:
     *   `v1.2.3`, `1.2.3-stable`, `v1.2.3-android`, `release-1.2.3`,
     *   `v2.0-final`, `v1.0.0-test-3`
     */
    fun isPreReleaseTag(tag: String?): Boolean {
        if (tag.isNullOrBlank()) return false
        // Pre-process so the regex's `\b` boundary catches markers that
        // sit flush against the numeric core (e.g. `1.2.0beta01`) — the
        // regex itself stays anchored on word boundaries to keep the
        // false-positive rate low for embedded substrings.
        val separated = insertHyphenBeforeKnownMarker(tag)
        return PRE_RELEASE_MARKER_PATTERN.containsMatchIn(separated)
    }

    /**
     * Returns the canonical label for the first pre-release marker
     * found in [tag], or `null` if none. Intended for UI badges that
     * want to show "Beta" / "Alpha" / "RC" instead of a generic
     * "Pre-release" pill — a much better signal for users deciding
     * whether to install.
     *
     * Labels are returned in title-case regardless of how they were
     * spelled in the tag (so `V1.0-BETA` and `v1.0-beta` both
     * resolve to `"Beta"`).
     *
     * Mapping rules:
     *  - `alpha`       → `Alpha`
     *  - `beta`        → `Beta`
     *  - `rc` / `rc\d+` → `RC`
     *  - `preview`     → `Preview`
     *  - `prerelease`  → `Pre-release`
     *  - `snapshot`    → `Snapshot`
     *  - `canary`      → `Canary`
     *  - `nightly`     → `Nightly`
     *  - `milestone` / `m\d+` → `Milestone`
     *  - `ea`          → `Early Access`
     *  - `dev`         → `Dev`
     *  - `pre`         → `Pre`
     *
     * Callers that also want to treat the API `prerelease` flag as
     * authoritative should use this alongside
     * [zed.rainxch.core.domain.model.isEffectivelyPreRelease].
     */
    fun preReleaseMarkerLabel(tag: String?): String? {
        if (tag.isNullOrBlank()) return null
        val separated = insertHyphenBeforeKnownMarker(tag)
        val match = PRE_RELEASE_MARKER_PATTERN.find(separated) ?: return null
        val raw = match.groupValues.getOrNull(1)?.lowercase().orEmpty()
        return when {
            raw.startsWith("alpha") -> "Alpha"
            raw.startsWith("beta") -> "Beta"
            raw.startsWith("rc") -> "RC"
            raw == "preview" -> "Preview"
            raw == "prerelease" -> "Pre-release"
            raw == "snapshot" -> "Snapshot"
            raw == "canary" -> "Canary"
            raw == "nightly" -> "Nightly"
            raw == "milestone" || raw.startsWith("m") -> "Milestone"
            raw == "ea" -> "Early Access"
            raw == "dev" -> "Dev"
            raw == "pre" -> "Pre"
            else -> null
        }
    }

    private val PRE_RELEASE_MARKER_PATTERN =
        // `\b` word boundaries cleanly separate markers from the
        // surrounding tag (so `alpha` matches `v1.0-alpha` but not
        // `alphabet`). The trailing `\d*` allows shorthand suffixes
        // like `rc1`, `beta2`, `preview3` without requiring a
        // separator between the word and the number. Longer
        // alternatives (`prerelease`) come before shorter prefixes
        // (`pre`) so the regex engine finds the longest match.
        Regex(
            "\\b(alpha|beta|rc|preview|prerelease|snapshot|canary|nightly|milestone|ea|dev|pre|m\\d+)\\d*\\b",
            RegexOption.IGNORE_CASE,
        )

    /**
     * Coarse classification of the versioning scheme a tag string
     * appears to follow. Useful for UI surfaces that want to render
     * a date-stamped release differently from a semver one ("Released
     * 2024-10-15" vs "Version 1.2.3"), or warn when a maintainer
     * appears to have switched schemes mid-history (which silently
     * breaks ordering — `1.2.0` would always read as older than
     * `20260502` under numeric semver compare even if it was tagged
     * later).
     *
     * The classification is intentionally rough; the underlying
     * comparator does NOT branch on it. Callers can combine
     * [detectScheme] outputs from two tags to detect cross-scheme
     * comparisons that warrant a UI hint.
     */
    fun detectScheme(version: String?): Scheme {
        if (version.isNullOrBlank()) return Scheme.Unknown
        val cleaned = stripFullPrefix(version).substringBefore('+')
        if (cleaned.isEmpty()) return Scheme.Unknown
        // Hyphenated calver — yyyy-mm-dd, optionally with a trailing
        // identifier we don't care about for the classification.
        if (CALVER_HYPHEN_PATTERN.matchEntire(cleaned) != null) return Scheme.CalVer
        // Single 8-digit run looks like yyyymmdd, e.g. `20260502`.
        DATE_INTEGER_PATTERN.matchEntire(cleaned)?.let { return Scheme.CalVer }
        // Dotted calver — yyyy.mm.dd inside a semver-shaped string.
        DOTTED_CALVER_PATTERN.matchEntire(cleaned)?.let { return Scheme.CalVer }
        // Anything that parses as semver after our normalisation pass
        // is semver, including adjacent-letter pre-release variants.
        val separated = insertHyphenBeforeKnownMarker(cleaned)
        if (parseSemanticVersion(separated) != null) return Scheme.SemVer
        // Hex-ish commit pointers (`v1.2.0+abc1234` strips the build
        // metadata, but a bare commit hash falls here).
        if (COMMIT_HASH_PATTERN.matchEntire(cleaned) != null) return Scheme.CommitHash
        return Scheme.Unknown
    }

    enum class Scheme {
        SemVer,
        CalVer,
        CommitHash,
        Unknown,
    }
}
