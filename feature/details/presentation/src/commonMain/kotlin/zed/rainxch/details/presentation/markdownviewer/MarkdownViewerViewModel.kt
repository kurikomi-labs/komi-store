package zed.rainxch.details.presentation.markdownviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import zed.rainxch.details.domain.repository.DetailsRepository
import zed.rainxch.details.domain.repository.TranslationRepository
import zed.rainxch.details.presentation.model.SupportedLanguages
import zed.rainxch.details.presentation.model.TranslationState
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.failed_to_load
import zed.rainxch.githubstore.core.presentation.res.translation_failed

class MarkdownViewerViewModel(
    private val url: String,
    private val detailsRepository: DetailsRepository,
    private val translationRepository: TranslationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        MarkdownViewerState(
            url = url,
            deviceLanguageCode = translationRepository.getDeviceLanguageCode(),
        ),
    )
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    fun translate(targetLanguageCode: String) {
        val markdown = _state.value.markdown
        if (markdown.isBlank()) return
        val displayName = SupportedLanguages.all.firstOrNull { it.code == targetLanguageCode }?.displayName
        _state.update {
            it.copy(
                translation = it.translation.copy(
                    isTranslating = true,
                    targetLanguageCode = targetLanguageCode,
                    targetLanguageDisplayName = displayName,
                    error = null,
                ),
            )
        }
        viewModelScope.launch {
            runCatching {
                translationRepository.translate(markdown, targetLanguageCode)
            }.onSuccess { result ->
                _state.update {
                    it.copy(
                        translation = it.translation.copy(
                            isTranslating = false,
                            translatedText = result.translatedText,
                            isShowingTranslation = true,
                            detectedSourceLanguage = result.detectedSourceLanguage,
                            error = null,
                        ),
                    )
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        translation = it.translation.copy(
                            isTranslating = false,
                            error = e.message ?: getString(Res.string.translation_failed),
                        ),
                    )
                }
            }
        }
    }

    fun toggleTranslation() {
        _state.update {
            it.copy(
                translation = it.translation.copy(
                    isShowingTranslation = !it.translation.isShowingTranslation,
                ),
            )
        }
    }

    fun clearTranslation() {
        _state.update {
            it.copy(translation = TranslationState())
        }
    }

    fun showLanguagePicker() {
        _state.update { it.copy(isLanguagePickerVisible = true) }
    }

    fun dismissLanguagePicker() {
        _state.update { it.copy(isLanguagePickerVisible = false) }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                detailsRepository.fetchRawMarkdown(url)
            }.onSuccess { rawMarkdown ->
                if (rawMarkdown != null) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            markdown = rawMarkdown,
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false, errorMessage = getString(Res.string.failed_to_load)) }
                }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, errorMessage = e.message ?: getString(Res.string.failed_to_load)) }
            }
        }
    }
}
