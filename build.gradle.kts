plugins {
    id("com.diffplug.spotless") version "8.10.3"
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

subprojects {
    apply(plugin = "com.diffplug.spotless")
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            ktfmt("0.64").googleStyle()
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktfmt("0.64").googleStyle()
        }
    }
}

