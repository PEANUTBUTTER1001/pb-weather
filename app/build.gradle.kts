import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
}

// Only the build reads this file. Never print Properties, generated constants or URLs.
val privateConfiguration = Properties().apply {
    val source = rootProject.file("local.properties")
    if (source.isFile) source.inputStream().use(::load)
}
fun configuredKey(name: String): String {
    val value = providers.environmentVariable(name).orNull
        ?: privateConfiguration.getProperty(name).orEmpty()
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

android {
    namespace = "com.pbweather.weather"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.pbweather.weather"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "KMA_SERVICE_KEY", configuredKey("KMA_SERVICE_KEY"))
        buildConfigField("String", "AIRKOREA_SERVICE_KEY", configuredKey("AIRKOREA_SERVICE_KEY"))
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug" }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
    packaging.resources.excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1")
}

kotlin { compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
ktlint {
    android.set(true)
    version.set("1.7.1")
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.tooling.preview)
    debugImplementation(libs.compose.tooling)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore)
    implementation(libs.work)
    implementation(libs.glance)
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.serialization)
    implementation(libs.coroutines)
    implementation(libs.location)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockwebserver)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.espresso)
    debugImplementation(libs.compose.test.manifest)
}

// AGP 9 built-in Kotlin is not discovered by ktlint-gradle 13.x source-set hooks.
val ktlintCli by configurations.creating
dependencies { add(ktlintCli.name, "com.pinterest.ktlint:ktlint-cli:1.7.1") }

val ktlintSourceCheck by tasks.registering(JavaExec::class) {
    group = "verification"
    classpath = ktlintCli
    mainClass.set("com.pinterest.ktlint.Main")
    args("src/**/*.kt")
}
val ktlintSourceFormat by tasks.registering(JavaExec::class) {
    group = "formatting"
    classpath = ktlintCli
    mainClass.set("com.pinterest.ktlint.Main")
    args("-F", "src/**/*.kt")
}
tasks.named("ktlintCheck") { dependsOn(ktlintSourceCheck) }
tasks.named("ktlintFormat") { dependsOn(ktlintSourceFormat) }
