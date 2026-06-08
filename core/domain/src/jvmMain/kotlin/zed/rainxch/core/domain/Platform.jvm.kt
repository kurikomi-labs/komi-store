package zed.rainxch.core.domain

import java.util.Locale
import zed.rainxch.core.domain.model.system.Platform

actual fun getPlatform(): Platform =
    when {
        System.getProperty("os.name").lowercase().contains("win") -> Platform.WINDOWS
        System.getProperty("os.name").lowercase().contains("mac") -> Platform.MACOS
        else -> Platform.LINUX
    }

actual fun getOsVersion(): String = System.getProperty("os.version") ?: "unknown"

actual fun getSystemLocaleTag(): String =
    Locale.getDefault().toLanguageTag().takeIf { it.isNotBlank() } ?: "und"
