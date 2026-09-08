plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("plugin.serialization")
}

android {
    namespace = "ru.bel.servicecenter"
    compileSdk = 36

    defaultConfig {
        applicationId = "ru.bel.servicecenter"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Переменные Supabase
        buildConfigField("String", "SUPABASE_URL", "\"${project.findProperty("SUPABASE_URL") ?: ""}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${project.findProperty("SUPABASE_KEY") ?: ""}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true   // включить генерацию BuildConfig для пользовательских полей
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation("androidx.core:core:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.activity:activity:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("androidx.navigation:navigation-compose:2.7.0")

    // Timber — логирование
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Coil для загрузки изображений
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Ktor — HTTP-клиент для REST
    implementation("io.ktor:ktor-client-android:2.3.3")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.3")

    // bcrypt — хеширование паролей
    implementation("org.mindrot:jbcrypt:0.4")
}