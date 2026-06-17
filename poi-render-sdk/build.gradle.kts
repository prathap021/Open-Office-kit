plugins {
    id("com.android.library")
    id("maven-publish")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.poirender.sdk"
    compileSdk = 37

    defaultConfig {
        minSdk = 21
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }



    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/NOTICE",
                "META-INF/*.RSA",
                "META-INF/*.SF",
                "META-INF/*.DSA"
            )
        }
    }
}

dependencies {
    // Apache POI
    implementation(libs.poi)
    implementation(libs.poi.ooxml)

    // POI dependencies
    implementation(libs.poi.scratchpad)
    implementation(libs.xmlbeans)
    implementation(libs.commons.compress)
    implementation(libs.commons.io)
    implementation(libs.curvesapi)

    // Logging
    implementation(libs.slf4j.simple)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
}