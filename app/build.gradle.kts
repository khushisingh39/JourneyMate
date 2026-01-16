plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.journeymatesample"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.journeymatesample"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(platform("com.google.firebase:firebase-bom:33.8.0"))
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-auth")
    implementation ("com.google.auth:google-auth-library-oauth2-http:1.15.0")
    implementation ("com.google.firebase:firebase-firestore:24.7.1")
    implementation ("androidx.recyclerview:recyclerview:1.3.2")




    // Google Maps and Places API
    implementation("com.google.android.gms:play-services-maps:18.1.0")
   // implementation("com.google.android.gms:play-services-places:18.0.1")
    // Google Places API
    implementation ("com.google.android.libraries.places:places:3.2.0")
    // ✅ Add Volley for Network Requests
    implementation("com.android.volley:volley:1.2.1")



}