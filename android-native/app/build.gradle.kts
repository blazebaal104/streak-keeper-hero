plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("io.github.takahirom.roborazzi")
}

android {
    namespace = "com.sbo.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sbo.app"
        minSdk = 26
        targetSdk = 35
        val vc = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionCode = 1000 + vc // above every TWA build, so it installs as an update
        versionName = "2.0.$vc"
        buildConfigField("String", "WEB_BASE", "\"https://sbo.perlospam696.workers.dev\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = "sbo"
            keyPassword = System.getenv("KEYSTORE_PASSWORD")
        }
    }

    flavorDimensions += "env"
    productFlavors {
        create("prod") {
            dimension = "env"
            resValue("string", "app_name", "SBO")
            buildConfigField("String", "SUPABASE_URL", "\"https://kxgswrwitsedppjzcokh.supabase.co\"")
            buildConfigField("String", "SUPABASE_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imt4Z3N3cndpdHNlZHBwanpjb2toIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTE3MDc0ODgsImV4cCI6MjEwNzI4MzQ4OH0.8eXZjeKJ-gX5X6bPmJNnURNv-KfpjhVrFkIT9Ezkc3g\"")
            buildConfigField("String", "SERVER_BASE", "\"https://sbo.perlospam696.workers.dev\"")
            buildConfigField("boolean", "DEV", "false")
            buildConfigField("String", "DEV_EMAIL", "\"\"")
            buildConfigField("String", "DEV_PASSWORD", "\"\"")
        }
        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            resValue("string", "app_name", "SBO Dev")
            buildConfigField("String", "SUPABASE_URL", "\"https://oiqxydvtxshdqwaxguud.supabase.co\"")
            buildConfigField("String", "SUPABASE_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9pcXh5ZHZ0eHNoZHF3YXhndXVkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTI2NzYyOTEsImV4cCI6MjEwODI1MjI5MX0.DCYcFH9xO0yIBzvkFYzC9InX39qxEDO8ExmzfR4-0Vs\"")
            buildConfigField("String", "SERVER_BASE", "\"https://sbo-dev.perlospam696.workers.dev\"")
            buildConfigField("boolean", "DEV", "true")
            // Shared test account of the throwaway dev database (fake data only).
            buildConfigField("String", "DEV_EMAIL", "\"dev@sbo.dev\"")
            buildConfigField("String", "DEV_PASSWORD", "\"dev-Ad9h0GJG4eCseg3Z\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests { isIncludeAndroidResources = true } }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    implementation("androidx.media3:media3-exoplayer:1.5.0")

    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.3"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.ktor:ktor-client-okhttp:3.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    // Screenshot tests: real Compose rendering on the JVM (Robolectric native graphics)
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.32.2")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.32.2")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
