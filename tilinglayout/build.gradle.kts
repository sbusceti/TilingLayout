@file:OptIn(ExperimentalComposeLibrary::class)

import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    `maven-publish`
}

group = "com.github.sbusceti"
version = "1.0.0"

kotlin {
    jvm()
    android {
        namespace = "it.stefanobusceti.tilinglayout.lib"
        compileSdk = 37
        minSdk = 24

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    js { browser() }
    wasmJs { browser() }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(compose.uiTest)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}
