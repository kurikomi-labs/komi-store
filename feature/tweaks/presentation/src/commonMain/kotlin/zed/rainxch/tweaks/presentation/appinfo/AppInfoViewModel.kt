package zed.rainxch.tweaks.presentation.appinfo

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import zed.rainxch.core.domain.system.AppVersionInfo

class AppInfoViewModel(
    appVersionInfo: AppVersionInfo,
) : ViewModel() {
    private val _state = MutableStateFlow(AppInfoState(versionName = appVersionInfo.versionName))
    val state = _state.asStateFlow()
}
