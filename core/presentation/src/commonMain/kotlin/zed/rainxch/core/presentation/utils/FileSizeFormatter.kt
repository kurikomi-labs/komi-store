package zed.rainxch.core.presentation.utils

fun formatFileSize(
    bytes: Long,
    decimals: Int = 1,
): String {
    if (bytes < 1_024L) return "$bytes B"

    val units = listOf("KB", "MB", "GB", "TB", "PB")
    var size = bytes.toDouble() / 1_024.0
    var index = 0
    while (size >= 1_024.0 && index < units.lastIndex) {
        size /= 1_024.0
        index++
    }

    return "%.${decimals}f ${units[index]}".format(size)
}
