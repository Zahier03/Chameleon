plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.application)
    kotlin("kapt")
    id("com.chaquo.python") version "15.0.1"
}

android {
    namespace = "com.sotech.chameleon"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sotech.chameleon"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    sourceSets {
        getByName("main") {
            assets {
                srcDirs("src/main/assets")
            }
        }
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
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/native/osx/libjansi.jnilib"
            excludes += "/META-INF/native/**"
            excludes += "META-INF/versions/9/previous-compilation-data.bin"
            pickFirsts += "META-INF/native/osx/libjansi.jnilib"
            pickFirsts += "kotlin/coroutines/coroutines.kotlin_builtins"
            pickFirsts += "kotlin/**/*.kotlin_builtins"
            pickFirsts += "kotlin/**.kotlin_builtins"
            pickFirsts += "META-INF/**.kotlin_module"
        }
        jniLibs {
            pickFirsts += "**/*.so"
        }
    }
}

chaquopy {
    defaultConfig {
        version = "3.12"

        buildPython("py", "-3.12")

        pip {
            install("numpy")
            install("matplotlib")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.mediapipe.tasks.genai)
    implementation(libs.gson)
    implementation(libs.richtext.commonmark)
    implementation(libs.richtext.ui.material3)

    implementation("com.google.accompanist:accompanist-systemuicontroller:0.36.0")
    implementation("com.google.accompanist:accompanist-pager:0.36.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.36.0")
    implementation("androidx.compose.animation:animation:1.7.8")
    implementation("androidx.compose.animation:animation-graphics:1.7.8")

    implementation("com.google.ai.edge.localagents:localagents-rag:0.3.0")
    implementation("com.google.guava:guava:33.3.1-android")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.9.0")
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")

    implementation("org.mozilla:rhino:1.7.14")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jsr223:1.9.20")
    implementation("com.github.javaparser:javaparser-core:3.25.7")
    implementation("org.python:jython-standalone:2.7.3")
    implementation(libs.androidx.compose.foundation)

    kapt(libs.hilt.android.compiler)
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("com.google.mediapipe:tasks-vision:0.10.18")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}