package zed.rainxch.githubstore.app

fun categorizeCrash(throwable: Throwable): String =
    when {
        throwable is OutOfMemoryError -> "other"
        throwable.message?.contains("DataStore", ignoreCase = true) == true -> "data_loss"
        throwable.message?.contains("install", ignoreCase = true) == true -> "install_fail"
        throwable.message?.contains("version", ignoreCase = true) == true -> "version_detect"
        else -> "other"
    }
