package zed.rainxch.core.data.services.external

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
import android.os.Build
import java.security.MessageDigest

object SigningFingerprintComputer {
    fun compute(
        packageManager: PackageManager,
        packageName: String,
    ): String? =
        runCatching {
            val info = loadPackageInfo(packageManager, packageName) ?: return null
            certBytes(info)?.let(::sha256Hex)
        }.getOrNull()

    fun computeFrom(info: PackageInfo): String? =
        runCatching {
            certBytes(info)?.let(::sha256Hex)
        }.getOrNull()

    private fun loadPackageInfo(
        pm: PackageManager,
        packageName: String,
    ): PackageInfo? {
        val flags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                GET_SIGNING_CERTIFICATES.toLong()
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES.toLong()
            }
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, flags.toInt())
            }
        }.getOrNull()
    }

    private fun certBytes(info: PackageInfo): ByteArray? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val sigInfo = info.signingInfo ?: return null
            // Prefer the active signer set. signingCertificateHistory is ordered with
            // the original cert at index 0 and the current cert at the last index, so
            // .firstOrNull() would return the pre-rotation cert for v3-rotated apps
            // and break fingerprint matching against signed binaries.
            val current = sigInfo.apkContentsSigners?.firstOrNull()
            val fallback = sigInfo.signingCertificateHistory?.lastOrNull()
            (current ?: fallback)?.toByteArray()
        } else {
            @Suppress("DEPRECATION")
            info.signatures?.firstOrNull()?.toByteArray()
        }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(bytes)
            .joinToString(":") { "%02X".format(it) }
}
