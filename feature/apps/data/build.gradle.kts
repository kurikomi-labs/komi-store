plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.buildkonfig)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.kotlinx.datetime)

                implementation(projects.core.domain)
                implementation(projects.core.data)
                implementation(projects.feature.apps.domain)

                implementation(libs.bundles.ktor.common)
                implementation(libs.bundles.koin.common)
            }
        }
    }
}
