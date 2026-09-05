import org.gradle.kotlin.dsl.implementation
import java.util.Properties

fun gitExec(vararg args: String): String = try {
    ProcessBuilder("git", *args)
        .directory(rootProject.projectDir)
        .redirectErrorStream(true)
        .start()
        .inputStream.bufferedReader().readText().trim()
} catch (_: Exception) { "" }

val gitCommitCount = gitExec("rev-list", "--count", "HEAD").toIntOrNull() ?: 1
val gitShortHash  = gitExec("rev-parse", "--short", "HEAD").ifEmpty { "unknown" }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    id("com.google.gms.google-services")
    kotlin("plugin.serialization") version "2.0.21"
    id("com.google.devtools.ksp") version "2.3.2"

}

val localProps = Properties().apply {
    val secretsPath = findProperty("secretsFile") as? String
    val source = when {
        secretsPath != null -> File(secretsPath)
        else -> rootProject.file("local.properties")
    }
    source.takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

val copyGoogleServices = tasks.register("copyGoogleServices") {
    val srcFile = file(localProps["GOOGLE_SERVICES_FILE"] as? String
        ?: error("GOOGLE_SERVICES_FILE not set in secrets"))
    val destFile = layout.projectDirectory.file("google-services.json").asFile
    inputs.file(srcFile)
    outputs.file(destFile)
    doLast { srcFile.copyTo(destFile, overwrite = true) }
}

val copyConfig = tasks.register("copyConfig") {
    val srcFile = file(localProps["CONFIG_FILE"] as? String
        ?: error("CONFIG_FILE not set in secrets"))
    val destFile = layout.projectDirectory.file("src/main/assets/config.json").asFile
    inputs.file(srcFile)
    outputs.file(destFile)
    doLast { srcFile.copyTo(destFile, overwrite = true) }
}

val copyConfigDev = tasks.register("copyConfigDev") {
    val srcFile = file(localProps["CONFIG_DEV_FILE"] as? String
        ?: error("CONFIG_DEV_FILE not set in secrets"))
    val destFile = layout.projectDirectory.file("src/main/assets/config_dev.json").asFile
    inputs.file(srcFile)
    outputs.file(destFile)
    doLast { srcFile.copyTo(destFile, overwrite = true) }
}

tasks.configureEach {
    if (name.matches(Regex("process.*GoogleServices"))) {
        dependsOn(copyGoogleServices)
    }
    if (name.matches(Regex("merge.*Assets"))) {
        dependsOn(copyConfig)
        dependsOn(copyConfigDev)
    }
}

android {
    namespace = "pl.kmalysiak.personalnotificator"
    compileSdk = 36

    signingConfigs {
        create("release") {
            storeFile = file(localProps["KEYSTORE_PATH"] as? String ?: "personalnotificator.jks")
            storePassword = localProps["KEYSTORE_PASS"] as? String ?: ""
            keyAlias = localProps["KEY_ALIAS"] as? String ?: ""
            keyPassword = localProps["KEY_PASS"] as? String ?: ""
        }
    }

    defaultConfig {
        applicationId = "pl.kmalysiak.personalnotificator"
        minSdk = 29
        targetSdk = 35
        versionCode = gitCommitCount
        versionName = "1.0-$gitShortHash"
        buildConfigField("long", "BUILD_TIME", "${System.currentTimeMillis()}L")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.firebase.auth)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.security.crypto)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.security.crypto.v110alpha06)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.slf4j.api)
    implementation(libs.logback.android)
}