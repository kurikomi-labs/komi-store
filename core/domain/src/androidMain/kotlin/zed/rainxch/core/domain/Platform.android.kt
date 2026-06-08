package zed.rainxch.core.domain

import android.os.Build
import java.util.Locale
import zed.rainxch.core.domain.model.system.Platform

actual fun getPlatform(): Platform = Platform.ANDROID

actual fun getOsVersion(): String = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

actual fun getSystemLocaleTag(): String =
    Locale.getDefault().toLanguageTag().takeIf { it.isNotBlank() } ?: "und"
