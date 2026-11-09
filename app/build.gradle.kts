import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    // Lee app/google-services.json y genera la configuración de Firebase para cada variante
    alias(libs.plugins.google.services)
}

// Credenciales de firma: keystore.properties en la raíz del proyecto (no se sube a Git)
val keystoreProperties = Properties().apply {
    val archivo = rootProject.file("keystore.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

android {
    namespace = "com.example.recordatorios"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.recordatorios"
        minSdk = 24
        targetSdk = 37
        // Actividad 4: versión candidata para testers.
        // versionCode: entero que crece en cada entrega (Android lo usa para saber qué es una actualización).
        // versionName: lo que lee el equipo de pruebas en sus reportes.
        versionCode = 4
        versionName = "4.0.0-rc1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Firma de la release. Las credenciales se leen de keystore.properties (fuera del repositorio, ver
    // keystore.properties.example). Si el archivo no existe, la release se genera sin firmar en vez de fallar.
    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    // Ambientes estables, sin tocar la configuración a mano antes de cada compilación:
    //  - dev:  desarrollo y pruebas internas. Otro applicationId (se instala junto a la de prod),
    //          otro nombre visible y herramientas de prueba en debug.
    //  - prod: la versión candidata que se entrega a los testers.
    // Las dos apps están registradas en el mismo proyecto de Firebase (google-services.json tiene ambas).
    flavorDimensions += "ambiente"
    productFlavors {
        create("dev") {
            dimension = "ambiente"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "Recordatorios Dev")
            buildConfigField("String", "AMBIENTE", "\"dev\"")
        }
        create("prod") {
            dimension = "ambiente"
            resValue("string", "app_name", "Recordatorios")
            buildConfigField("String", "AMBIENTE", "\"prod\"")
        }
    }

    buildTypes {
        release {
            // R8: reduce y optimiza el código de la release
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true      // permite que cada flavor defina su propio app_name
    }
}

// Room guarda el esquema de cada versión de la base en app/schemas (sirve para las migraciones)
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.compose.material.icons.core)

    // Actividad 3: preferencias y base de datos local
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Actividad 4: Firebase (las versiones las fija el BoM, así Auth y Firestore son compatibles entre sí)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    // .await(): convierte las Task de Firebase en funciones suspend para usarlas con Coroutines
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)   // runTest y Dispatchers.setMain para probar ViewModels
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
