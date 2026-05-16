plugins {
    alias(libs.plugins.convention.cmp.feature)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)

                implementation(projects.core.domain)
                implementation(projects.core.presentation)
                implementation(projects.feature.details.domain)
                implementation(projects.feature.profile.domain)

                implementation(libs.markdown.renderer)
                implementation(libs.markdown.renderer.coil3)
                implementation(libs.highlights)

                implementation(libs.jetbrains.compose.components.resources)
                implementation(libs.kotlinx.datetime)

                implementation(libs.androidx.compose.ui.tooling.preview)
                implementation(libs.bundles.landscapist)
            }
        }

        androidMain {
            dependencies {
            }
        }

        jvmMain {
            dependencies {
            }
        }
    }
}
