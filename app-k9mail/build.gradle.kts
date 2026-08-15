import org.cyclonedx.Version
import org.cyclonedx.gradle.CyclonedxDirectTask
import org.cyclonedx.model.Component
import org.gradle.api.file.RegularFile

plugins {
    id(ThunderbirdPlugins.App.androidCompose)
    alias(libs.plugins.cyclonedx.bom)
    alias(libs.plugins.dependency.guard)
    alias(libs.plugins.tb.app.badging)
    alias(libs.plugins.tb.app.versioning)
}

val testCoverageEnabled = hasProperty("testCoverageEnabled")
val kemiVersionCode = 39040
val kemiVersionName = "20.1"

android {
    namespace = "com.fsck.k9"

    defaultConfig {
        applicationId = "com.fsck.k9"
        testApplicationId = "com.fsck.k9.tests"

        versionCode = kemiVersionCode
        versionName = kemiVersionName

        buildConfigField("String", "CLIENT_INFO_APP_NAME", "\"KEMI Mail\"")
    }

    androidResources {
        // Keep in sync with the resource string array "supported_languages"
        localeFilters += listOf(
            "ar",
            "be",
            "bg",
            "br",
            "ca",
            "co",
            "cs",
            "cy",
            "da",
            "de",
            "el",
            "en",
            "en-rGB",
            "eo",
            "es",
            "et",
            "eu",
            "fa",
            "fi",
            "fr",
            "fy",
            "ga",
            "gd",
            "gl",
            "hr",
            "hu",
            "in",
            "is",
            "it",
            "iw",
            "ja",
            "ko",
            "lt",
            "lv",
            "nb",
            "nl",
            "nn",
            "pl",
            "pt-rBR",
            "pt-rPT",
            "ro",
            "ru",
            "sk",
            "sl",
            "sq",
            "sr",
            "sv",
            "ta-rIN",
            "tr",
            "uk",
            "vi",
            "zh-rCN",
            "zh-rTW",
        )
    }

    signingConfigs {
        createSigningConfig(project, SigningType.K9_RELEASE, isUpload = false)
    }

    buildTypes {
        val isCI = project.findProperty("ci") == "true"
        release {
            signingConfig = signingConfigs.getByType(SigningType.K9_RELEASE)

            isMinifyEnabled = !isCI
            isShrinkResources = !isCI

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }

        debug {
            applicationIdSuffix = ".debug"

            enableUnitTestCoverage = testCoverageEnabled
            enableAndroidTestCoverage = testCoverageEnabled

            isMinifyEnabled = false
        }
    }

    flavorDimensions += listOf("app")
    productFlavors {
        create("foss") {
            dimension = "app"
            buildConfigField("String", "PRODUCT_FLAVOR_APP", "\"foss\"")
        }

        create("full") {
            dimension = "app"
            buildConfigField("String", "PRODUCT_FLAVOR_APP", "\"full\"")
        }
    }

    packaging {
        jniLibs {
            excludes += listOf("kotlin/**")
        }

        resources {
            excludes += listOf(
                "META-INF/*.kotlin_module",
                "META-INF/*.version",
                "kotlin/**",
                "DebugProbesKt.bin",
            )
        }
    }
}

dependencies {
    implementation(projects.appCommon)
    implementation(projects.core.ui.compose.common)
    implementation(projects.core.ui.compose.theme2)
    implementation(projects.core.ui.legacy.theme2.k9mail)
    implementation(projects.feature.launcher)
    implementation(projects.feature.mail.message.list.api)
    implementation(projects.feature.mail.message.list.internal)
    implementation(projects.feature.mail.message.reader.api)

    implementation(projects.legacy.core)
    implementation(projects.legacy.ui.legacy)

    implementation(projects.core.featureflag)

    implementation(projects.feature.account.settings.impl)

    "fossImplementation"(projects.feature.funding.noop)
    "fullImplementation"(projects.feature.funding.noop)
    implementation(projects.feature.migration.launcher.noop)
    implementation(projects.feature.onboarding.migration.noop)
    implementation(projects.feature.thundermail.api)
    implementation(projects.feature.thundermail.k9mail)
    implementation(projects.feature.telemetry.noop)
    implementation(projects.feature.widget.messageList)
    implementation(projects.feature.widget.messageListGlance)
    implementation(projects.feature.widget.shortcut)
    implementation(projects.feature.widget.unread)

    implementation(libs.androidx.work.runtime)

    implementation(projects.feature.autodiscovery.api)
    debugImplementation(projects.backend.demo)
    debugImplementation(projects.feature.autodiscovery.demo)

    // Required for DependencyInjectionTest
    testImplementation(projects.feature.account.api)
    testImplementation(projects.feature.account.common)
    testImplementation(projects.feature.thundermail.internal.common)
    testImplementation(projects.plugins.openpgpApiLib.openpgpApi)
    testImplementation(libs.appauth)
}

dependencyGuard {
    configuration("fossReleaseRuntimeClasspath")
    configuration("fullReleaseRuntimeClasspath")
}

fun CyclonedxDirectTask.configureKemiReleaseSbom(flavor: String) {
    group = "reporting"
    description = "Generates the KEMI $flavor release runtime CycloneDX SBOM."
    includeConfigs = listOf("${flavor}ReleaseRuntimeClasspath")
    skipConfigs = emptyList()
    testConfigs = emptyList()
    projectType = Component.Type.APPLICATION
    schemaVersion = Version.VERSION_17
    componentGroup = "app.k9mail"
    componentName = "KEMI Mail ($flavor)"
    componentVersion = kemiVersionName
    includeBomSerialNumber = false
    includeLicenseText = false
    includeMetadataResolution = true
    includeBuildEnvironment = false
    includeBuildSystem = false
    jsonOutput = layout.buildDirectory.file("reports/sbom/kemi-$flavor-release.cdx.json")
    xmlOutput.convention(null as RegularFile?)
}

val cyclonedxFossReleaseBom = tasks.named<CyclonedxDirectTask>("cyclonedxDirectBom") {
    configureKemiReleaseSbom("foss")
}

val cyclonedxFullReleaseBom = tasks.register<CyclonedxDirectTask>("cyclonedxFullReleaseBom") {
    configureKemiReleaseSbom("full")
}

tasks.register("kemiReleaseSboms") {
    group = "reporting"
    description = "Generates both KEMI release runtime SBOMs."
    dependsOn(cyclonedxFossReleaseBom, cyclonedxFullReleaseBom)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 24
}
