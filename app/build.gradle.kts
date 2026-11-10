plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.reto9"
    compileSdk = 35

    defaultConfig {
        // IMPORTANTE: NO usar "com.example.*". OpenStreetMap bloquea (HTTP 403) los
        // User-Agent de apps de ejemplo. Debe ser un applicationId único y propio.
        applicationId = "io.github.jfloresriera.reto9"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // OSMDroid para el mapa (OpenStreetMap)
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Play Services Location para GPS
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Retrofit y Gson para API de Overpass
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
