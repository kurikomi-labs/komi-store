package zed.rainxch.core.domain.util

object RepoIdCodec {
    fun encode(host: String?, rawId: Long): Long {
        if (host.isNullOrBlank()) return rawId
        val hostBits = (host.lowercase().hashCode().toLong() and 0x7FFFL) shl 48
        val idBits = rawId and 0x0000FFFFFFFFFFFFL
        return -(hostBits or idBits)
    }

    fun isForeignSource(repoId: Long): Boolean = repoId < 0L
}
