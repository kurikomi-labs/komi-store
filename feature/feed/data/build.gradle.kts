plugins {
    alias(libs.plugins.convention.kmp.library)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)

                implementation(projects.core.domain)
                implementation(projects.core.data)
                implementation(projects.feature.feed.domain)

                implementation(libs.bundles.ktor.common)
                implementation(libs.bundles.koin.common)
            }
        }
    }
}
