package zed.rainxch.core.data.services

interface LocalizationManager {
    fun getCurrentLanguageCode(): String

    fun getPrimaryLanguageCode(): String

    fun setActiveLanguageTag(tag: String?)
}
