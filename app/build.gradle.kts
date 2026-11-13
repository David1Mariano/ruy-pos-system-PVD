plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}
android {
    namespace = "br.com.ruy.pos"
    compileSdk = 34
    defaultConfig {
        applicationId = "br.com.ruy.pos"
        minSdk = 23
        targetSdk = 34
        versionCode = 22
        versionName = "0.10.0-rc1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["stoneReturnScheme"] = "ruypos-stone"
        buildConfigField("boolean", "HOMOLOGATION", "false")
        buildConfigField("boolean", "SMART_POS_REAL_ENABLED", "false")
    }
    val signingEnv = listOf("RUY_RELEASE_STORE_FILE", "RUY_RELEASE_STORE_PASSWORD", "RUY_RELEASE_KEY_ALIAS", "RUY_RELEASE_KEY_PASSWORD").map { System.getenv(it) }
    require(signingEnv.all { it.isNullOrBlank() } || signingEnv.all { !it.isNullOrBlank() }) { "Configuração de assinatura incompleta; use o procedimento protegido de release" }
    if (signingEnv.all { !it.isNullOrBlank() }) signingConfigs.create("protectedRelease") {
        storeFile = file(requireNotNull(signingEnv[0]))
        storeType = "PKCS12"
        storePassword = signingEnv[1]
        keyAlias = signingEnv[2]
        keyPassword = signingEnv[3]
    }
    buildTypes {
        getByName("release") {
            isDebuggable = false
            if (signingEnv.all { !it.isNullOrBlank() }) signingConfig = signingConfigs.getByName("protectedRelease")
        }
        create("homologation") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".homologation"
            versionNameSuffix = "-homologacao"
            matchingFallbacks += "debug"
            manifestPlaceholders["stoneReturnScheme"] = "ruypos-stone-homologation"
            buildConfigField("boolean", "HOMOLOGATION", "true")
        }
    }
    testBuildType = "homologation"
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { it.systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2") }
    }
}
kapt { arguments { arg("room.schemaLocation", "$projectDir/schemas") } }
dependencies {
    implementation("com.google.zxing:core:3.5.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test:runner:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    "homologationImplementation"("androidx.compose.ui:ui-test-manifest")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
