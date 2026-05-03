import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.convention.cmp.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.hot.reload)
}

android {
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)

            implementation(libs.core.splashscreen)

            implementation(libs.koin.android)
        }
        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(projects.core.domain)
            implementation(projects.core.presentation)

            implementation(projects.feature.apps.domain)
            implementation(projects.feature.apps.data)
            implementation(projects.feature.apps.presentation)

            implementation(projects.feature.auth.domain)
            implementation(projects.feature.auth.data)
            implementation(projects.feature.auth.presentation)

            implementation(projects.feature.details.domain)
            implementation(projects.feature.details.data)
            implementation(projects.feature.details.presentation)

            implementation(projects.feature.devProfile.domain)
            implementation(projects.feature.devProfile.data)
            implementation(projects.feature.devProfile.presentation)

            implementation(projects.feature.favourites.domain)
            implementation(projects.feature.favourites.data)
            implementation(projects.feature.favourites.presentation)

            implementation(projects.feature.home.domain)
            implementation(projects.feature.home.data)
            implementation(projects.feature.home.presentation)

            implementation(projects.feature.search.domain)
            implementation(projects.feature.search.data)
            implementation(projects.feature.search.presentation)

            implementation(projects.feature.profile.domain)
            implementation(projects.feature.profile.data)
            implementation(projects.feature.profile.presentation)

            implementation(projects.feature.starred.domain)
            implementation(projects.feature.starred.data)
            implementation(projects.feature.starred.presentation)

            implementation(projects.feature.recentlyViewed.presentation)
            implementation(projects.feature.tweaks.presentation)

            implementation(libs.jetbrains.compose.navigation)
            implementation(libs.bundles.koin.common)
            implementation(libs.liquid)
            implementation(libs.jetbrains.compose.material.icons.extended)

            implementation(libs.touchlab.kermit)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.foundation)
            implementation(libs.jetbrains.compose.material3)
            implementation(libs.jetbrains.compose.ui)
            implementation(libs.jetbrains.compose.components.resources)

            implementation(libs.androidx.compose.ui.tooling.preview)
            implementation(libs.jetbrains.compose.viewmodel)
            implementation(libs.jetbrains.lifecycle.compose)
        }

        jvmMain {
            dependencies {
                implementation(projects.core.presentation)
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.kotlin.stdlib)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.koin.compose.viewmodel)

                implementation(libs.slf4j.simple)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "zed.rainxch.githubstore.DesktopAppKt"
        nativeDistributions {
            packageName = "GitHub-Store"
            packageVersion =
                libs.versions.projectVersionName
                    .get()
                    .toString()
            vendor = "rainxchzed"
            includeAllModules = true

            val currentOs =
                org.gradle.internal.os.OperatingSystem
                    .current()
            targetFormats(
                *when {
                    currentOs.isWindows -> arrayOf(TargetFormat.Exe, TargetFormat.Msi)
                    currentOs.isMacOsX -> arrayOf(TargetFormat.Dmg, TargetFormat.Pkg)
                    currentOs.isLinux -> arrayOf(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.AppImage)
                    else -> emptyArray()
                },
            )
            windows {
                iconFile.set(project.file("src/jvmMain/resources/logo/app_icon.ico"))
                menuGroup = "Github Store"
                shortcut = true
                perUserInstall = true
            }
            macOS {
                iconFile.set(project.file("src/jvmMain/resources/logo/app_icon.icns"))
                bundleID = "zed.rainxch.githubstore"

                infoPlist {
                    extraKeysRawXml =
                        """
                        <key>CFBundleURLTypes</key>
                        <array>
                            <dict>
                                <key>CFBundleURLName</key>
                                <string>GitHub Store Deep Link</string>
                                <key>CFBundleURLSchemes</key>
                                <array>
                                    <string>githubstore</string>
                                </array>
                            </dict>
                        </array>
                        """.trimIndent()
                }
            }
            linux {
                iconFile.set(project.file("src/jvmMain/resources/logo/app_icon.png"))
                appRelease =
                    libs.versions.projectVersionName
                        .get()
                        .toString()
                debPackageVersion =
                    libs.versions.projectVersionName
                        .get()
                        .toString()
                menuGroup = "Development"
                appCategory = "Development"
            }
        }
    }
}
