plugins {
    id("com.android.library")
    id("maven-publish")
}

android {
    namespace = "com.poirender.sdk"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
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
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")

    // POI dependencies
    implementation("org.apache.xmlbeans:xmlbeans:5.1.1")
    implementation("org.apache.commons:commons-compress:1.23.0")
    implementation("commons-io:commons-io:2.13.0")
    implementation("com.github.virtuald:curvesapi:1.08")

    // Logging
    implementation("org.slf4j:slf4j-simple:2.0.7")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
}


