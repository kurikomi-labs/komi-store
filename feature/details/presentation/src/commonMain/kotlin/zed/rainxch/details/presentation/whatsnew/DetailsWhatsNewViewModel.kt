package zed.rainxch.details.presentation.whatsnew

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

class DetailsWhatsNewViewModel(
    private val repositoryId: Long,
    private val owner: String,
    private val repo: String,
    private val sourceHost: String?,
    private val detailsRepository: DetailsRepository,
    private val translationRepository: TranslationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        DetailsWhatsNewState(deviceLanguageCode = translationRepository.getDeviceLanguageCode()),
    )
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    fun translate(targetLanguageCode: String) {
        val releases = _state.value.releases
        val body = releases.firstOrNull()?.description.orEmpty()
        if (body.isBlank()) return
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
                translationRepository.translate(body, targetLanguageCode)
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
        _state.update { it.copy(translation = TranslationState()) }
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
                val resolved = if (owner.isNotBlank() && repo.isNotBlank()) {
                    detailsRepository.getRepositoryByOwnerAndName(owner, repo, sourceHost)
                } else {
                    detailsRepository.getRepositoryById(repositoryId)
                }
                val releases = detailsRepository.getAllReleases(
                    owner = resolved.owner.login,
                    repo = resolved.name,
                    defaultBranch = resolved.defaultBranch,
                    sourceHost = sourceHost,
                )
                resolved to releases
            }.onSuccess { (resolved, releases) ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        repoName = resolved.name,
                        releases = releases,
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, errorMessage = e.message ?: getString(Res.string.failed_to_load)) }
            }
        }
    }
}
