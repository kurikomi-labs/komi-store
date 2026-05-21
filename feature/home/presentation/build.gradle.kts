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
                implementation(projects.feature.home.domain)
                implementation(projects.feature.profile.domain)

                implementation(libs.kotlinx.collections.immutable)

                implementation(libs.jetbrains.compose.components.resources)

                implementation(libs.androidx.compose.ui.tooling.preview)
            }
        }
    }
}
