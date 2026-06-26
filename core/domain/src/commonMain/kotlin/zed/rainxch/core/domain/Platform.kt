package zed.rainxch.core.domain

import zed.rainxch.core.domain.model.system.Platform

expect fun getPlatform(): Platform

expect fun getOsVersion(): String

expect fun getSystemLocaleTag(): String

fun isAndroid(): Boolean = getPlatform() == Platform.ANDROID

fun isDesktop(): Boolean = getPlatform().isDesktop

fun isMobile(): Boolean = getPlatform().isMobile
