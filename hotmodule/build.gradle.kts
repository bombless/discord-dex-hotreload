plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "dev.bombless.dexhot.sample"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation(project(":contract"))
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.compose.material3:material3")
}

/**
 * Build the AAR first, then convert its classes.jar to classes.dex using the
 * D8 executable from the installed Android SDK Build Tools.
 * Usage: ./gradlew :hotmodule:assembleDebug :hotmodule:makeDex
 */
tasks.register<Exec>("makeDex") {
    dependsOn("assembleDebug")
    val sdk = android.sdkDirectory
    val buildTools = providers.gradleProperty("dexBuildTools").orElse("36.0.0")
    val aar = layout.buildDirectory.file("outputs/aar/hotmodule-debug.aar")
    val extracted = layout.buildDirectory.file("dex-input/classes.jar")
    val outputDir = layout.buildDirectory.dir("outputs/dex")
    doFirst {
        val aarFile = aar.get().asFile
        val dir = extracted.get().asFile.parentFile
        dir.mkdirs()
        copy {
            from(zipTree(aarFile))
            include("classes.jar")
            into(dir)
        }
        outputDir.get().asFile.mkdirs()
        val d8 = sdk.resolve("build-tools/${buildTools.get()}/d8")
        commandLine(d8.absolutePath, "--min-api", "26", "--output",
            outputDir.get().asFile.absolutePath, extracted.get().asFile.absolutePath)
    }
}
