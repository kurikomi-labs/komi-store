package zed.rainxch.core.domain.util

/**
 * Packs a foreign-host repo identity into the `Long` `repoId` slot
 * used throughout the schema (which was originally GitHub-only, where
 * IDs are globally unique).
 *
 * Layout (64-bit `Long`):
 * - bit 63   — sign flag. Negative means "foreign source" (see
 *              [isForeignSource]); positive means native GitHub.
 * - bits 40-62 — 23-bit host fingerprint, derived from
 *              `host.lowercase().hashCode()`. 23 bits gives 8 388 608
 *              buckets — birthday-collision probability for a user
 *              tracking N self-hosted forges is roughly
 *              `N² / (2 × 8 388 608)`; e.g. 200 forges → ~0.0025
 *              chance of any two sharing a bucket. The previous 15-bit
 *              layout (32 768 buckets) hit ~0.6 probability at that
 *              scale, which CR flagged as a real risk.
 * - bits 0-39  — 40-bit raw repository ID. Forgejo / Gitea instances
 *              auto-increment from 1; 2⁴⁰ ≈ 1 trillion comfortably
 *              covers any realistic instance.
 *
 * Note: this layout is NOT backwards-compatible with the original
 * 15-bit fingerprint encoding. Database rows persisted under the old
 * scheme will decode to different `repoId`s after upgrade. Because the
 * Codeberg / Forgejo source feature is shipping in a preview release
 * (versionCode 18 — see whatsnew/18.json), the migration cost is
 * accepted; existing rows for non-GitHub sources will simply be
 * re-fetched on next scan.
 */
object RepoIdCodec {
    private const val HOST_FINGERPRINT_MASK = 0x7FFFFFL    // 23 bits
    private const val RAW_ID_MASK = 0xFFFFFFFFFFL          // 40 bits
    private const val HOST_FINGERPRINT_SHIFT = 40

    fun encode(host: String?, rawId: Long): Long {
        if (host.isNullOrBlank()) return rawId
        val hostBits = (host.lowercase().hashCode().toLong() and HOST_FINGERPRINT_MASK) shl
            HOST_FINGERPRINT_SHIFT
        val idBits = rawId and RAW_ID_MASK
        return -(hostBits or idBits)
    }

    fun isForeignSource(repoId: Long): Boolean = repoId < 0L
}
