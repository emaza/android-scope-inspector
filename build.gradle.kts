plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless)
}

// Formato: `./gradlew spotlessCheck` comprueba (lo hace la CI) y `./gradlew spotlessApply` corrige.
// El estilo va aquí y no en .editorconfig porque Spotless no lee `ktlint_code_style` de ese archivo
// (sí lee `max_line_length`). Se mantiene el estilo de Android Studio con comas finales, que es el del código.
val ktlintStyle =
    mapOf(
        "ktlint_code_style" to "android_studio",
        "ij_kotlin_allow_trailing_comma" to "true",
        "ij_kotlin_allow_trailing_comma_on_call_site" to "true",
    )

spotless {
    kotlin {
        target("app/src/**/*.kt")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintStyle)
    }
    kotlinGradle {
        target("*.kts", "app/*.kts")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintStyle)
    }
}
