import java.time.Duration

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// CI calcula la versión con scripts/compute-version.sh y la pasa con
// -PappVersionName=... -PappVersionCode=...; en local se usan estos valores por defecto.
val appVersionName = providers.gradleProperty("appVersionName").orElse("0.1.0-dev")
val appVersionCode = providers.gradleProperty("appVersionCode").map { it.toInt() }.orElse(1)

android {
    // El namespace (paquete del código) no cambia; solo el identificador de la app instalada.
    namespace = "com.example.scopeinspector"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.emaza.scopeinspector"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode.get()
        versionName = appVersionName.get()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        // Robolectric necesita los recursos y el manifest reales para los tests de UI.
        unitTests.isIncludeAndroidResources = true
        // Red de seguridad: un test colgado (p. ej. un bucle con delay en coroutines) no debe bloquear la CI.
        unitTests.all { test ->
            test.timeout.set(Duration.ofMinutes(5))
            // Robolectric accede a internos del JDK (FileDescriptor, SharedSecrets) que Java 17+ encapsula.
            test.jvmArgs(
                "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.play.services.location)
    implementation(libs.osmdroid.android)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.navigation.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
