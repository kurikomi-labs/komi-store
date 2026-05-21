package zed.rainxch.core.domain.model

data class ApkInspection(

    val appLabel: String,
    val packageName: String,
    val versionName: String?,
    val versionCode: Long?,

    val signingFingerprint: String?,

    val minSdk: Int?,

    val targetSdk: Int?,

    val permissions: List<ApkPermission>,

    val mainActivity: String?,

    val activityCount: Int,

    val serviceCount: Int,

    val receiverCount: Int,

    val fileSizeBytes: Long?,

    val filePath: String?,

    val debuggable: Boolean,

    val source: Source,
) {
    enum class Source {

        FILE,

        INSTALLED,
    }
}

data class ApkPermission(

    val name: String,

    val displayName: String,

    val description: String?,
    val protectionLevel: ProtectionLevel,

    val granted: Boolean?,
)

enum class ProtectionLevel {

    NORMAL,

    DANGEROUS,

    SIGNATURE,

    PRIVILEGED,

    UNKNOWN,
}
