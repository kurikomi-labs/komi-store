package zed.rainxch.core.data.services.installer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import zed.rainxch.core.data.services.dhizuku.DhizukuServiceManager
import zed.rainxch.core.data.services.dhizuku.model.DhizukuStatus
import zed.rainxch.core.data.services.root.RootServiceManager
import zed.rainxch.core.data.services.root.model.RootStatus
import zed.rainxch.core.data.services.shizuku.ShizukuServiceManager
import zed.rainxch.core.data.services.shizuku.model.ShizukuStatus
import zed.rainxch.core.domain.model.DhizukuAvailability
import zed.rainxch.core.domain.model.RootAvailability
import zed.rainxch.core.domain.model.ShizukuAvailability
import zed.rainxch.core.domain.system.InstallerStatusProvider

class AndroidInstallerStatusProvider(
    private val shizukuServiceManager: ShizukuServiceManager,
    private val dhizukuServiceManager: DhizukuServiceManager,
    private val rootServiceManager: RootServiceManager,
    scope: CoroutineScope,
) : InstallerStatusProvider {
    override val shizukuAvailability: StateFlow<ShizukuAvailability> =
        shizukuServiceManager.status
            .map { status ->
                when (status) {
                    ShizukuStatus.NOT_INSTALLED -> ShizukuAvailability.UNAVAILABLE
                    ShizukuStatus.NOT_RUNNING -> ShizukuAvailability.NOT_RUNNING
                    ShizukuStatus.PERMISSION_NEEDED -> ShizukuAvailability.PERMISSION_NEEDED
                    ShizukuStatus.READY -> ShizukuAvailability.READY
                }
            }.stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = ShizukuAvailability.UNAVAILABLE,
            )

    override val dhizukuAvailability: StateFlow<DhizukuAvailability> =
        dhizukuServiceManager.status
            .map { status ->
                when (status) {
                    DhizukuStatus.NOT_INSTALLED -> DhizukuAvailability.UNAVAILABLE
                    DhizukuStatus.NOT_RUNNING -> DhizukuAvailability.NOT_RUNNING
                    DhizukuStatus.PERMISSION_NEEDED -> DhizukuAvailability.PERMISSION_NEEDED
                    DhizukuStatus.READY -> DhizukuAvailability.READY
                }
            }.stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = DhizukuAvailability.UNAVAILABLE,
            )

    override val rootAvailability: StateFlow<RootAvailability> =
        rootServiceManager.status
            .map { status ->
                when (status) {
                    RootStatus.NOT_AVAILABLE -> RootAvailability.UNAVAILABLE
                    RootStatus.PERMISSION_NEEDED -> RootAvailability.PERMISSION_NEEDED
                    RootStatus.READY -> RootAvailability.READY
                }
            }.stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = RootAvailability.UNAVAILABLE,
            )

    override fun requestShizukuPermission() {
        shizukuServiceManager.requestPermission()
    }

    override fun requestDhizukuPermission() {
        dhizukuServiceManager.requestPermission()
    }

    override fun requestRootPermission() {
        rootServiceManager.requestPermission()
    }
}
