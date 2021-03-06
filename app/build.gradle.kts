import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-android-extensions")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    compileSdkVersion(30)
    buildToolsVersion("29.0.3")

    defaultConfig {
        applicationId = "name.lmj0011.jetpackreleasetracker"
        minSdkVersion(19)
        targetSdkVersion(30)
        versionCode(getCommitCount().toInt())
        versionName = "1.3"

        vectorDrawables {
            useSupportLibrary = true
        }

        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ref: https://stackoverflow.com/a/48674264/2445763
        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
            }
        }
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            resValue("bool", "DEBUG_MODE", "false")
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
        }

        named("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            versionNameSuffix("+debug.${getGitSha().take(8)}")
            applicationIdSuffix(".debug")
            resValue("bool", "DEBUG_MODE", "true")
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
        }
    }

    buildTypes.forEach {
        it.resValue("string", "app_build", getGitSha().take(8))
        it.resValue("string", "git_commit_count", getCommitCount())
        it.resValue("string", "git_commit_sha", getGitSha())
        it.resValue("string", "app_buildtime", getBuildTime())
    }


    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    // Big Up! to cSn: https://stackoverflow.com/a/36626584/2445763
    configurations.implementation {
        isCanBeResolved = true
    }

    configurations.androidTestImplementation {
        isCanBeResolved = true
    }

    project.gradle.addBuildListener(object : BuildListener {
        override fun buildStarted(gradle: Gradle) {}

        override fun settingsEvaluated(settings: Settings) {}

        override fun projectsLoaded(gradle: Gradle) {}

        override fun projectsEvaluated(gradle: Gradle) {}

        override fun buildFinished(result: BuildResult) {
            var str = "# auto-generated; this file should be checked into version control\n"
            val resolvedImplementationConfig = configurations.implementation.get().resolvedConfiguration
            val resolvedAndroidTestImplementationConfig = configurations.androidTestImplementation.get().resolvedConfiguration
            val fileName = "deps.list.txt"
            val depsFile = File(projectDir, fileName)

            resolvedImplementationConfig.firstLevelModuleDependencies.forEach { dep ->
                str += "${dep.moduleGroup}:${dep.moduleName}:${dep.moduleVersion}\n"
            }

            resolvedAndroidTestImplementationConfig.firstLevelModuleDependencies.forEach { dep ->
                str += "${dep.moduleGroup}:${dep.moduleName}:${dep.moduleVersion}\n"
            }

           GlobalScope.launch(Dispatchers.IO) {
               depsFile.writeText(str)
               println("\n${fileName} created.\n")
           }
        }
    })
}

dependencies {
    val fTree = fileTree("lib")
    fTree.include("*.jar")

    implementation(fTree)
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${findProperty("kotlin.version")}")
    implementation("androidx.core:core-ktx:1.3.2")
    implementation("androidx.appcompat:appcompat:1.2.0")

    implementation("com.google.android.material:material:1.2.1")
    implementation("androidx.constraintlayout:constraintlayout:2.0.1")
    implementation("androidx.vectordrawable:vectordrawable:1.1.0")
    implementation("androidx.navigation:navigation-fragment:${findProperty("fragment.version")}")
    implementation("androidx.navigation:navigation-fragment-ktx:${findProperty("fragment.version")}")
    implementation("androidx.navigation:navigation-ui:${findProperty("nav.version")}")
    implementation("androidx.navigation:navigation-ui-ktx:${findProperty("nav.version")}")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    testImplementation("junit:junit:4.13")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")

    // Room dependencies
    implementation("androidx.room:room-runtime:${findProperty("room.version")}")
    annotationProcessor("androidx.room:room-compiler:${findProperty("room.version")}")
    kapt("androidx.room:room-compiler:${findProperty("room.version")}")

    // GSON
    implementation("com.google.code.gson:gson:2.8.6")

    // Lifecycle-aware components
    // ref: https://developer.android.com/jetpack/androidx/releases/lifecycle
    implementation("androidx.lifecycle:lifecycle-extensions:${findProperty("lifecycle.version")}")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${findProperty("coroutine.version")}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${findProperty("coroutine.version")}")

    // androidx.preference
    implementation("androidx.preference:preference-ktx:1.1.1")

    // WorkManager
    val workVersion = "2.4.0"
    implementation("androidx.work:work-runtime-ktx:$workVersion")

    // https://developer.android.com/studio/write/java8-support?utm_source=android-studio-4-0&utm_medium=studio-assistant-stable#library-desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.0.10")

    // add the Firebase SDK for Google Analytics
    implementation(platform("com.google.firebase:firebase-bom:26.0.0")) // ref: https://firebase.google.com/docs/android/learn-more#bom
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics")


    // other 3rd party libs
    implementation("com.jakewharton.timber:timber:4.7.1")
    implementation("com.vdurmont:semver4j:3.1.0")
    implementation("br.com.simplepass:loading-button-android:2.2.0")
    implementation("com.github.kittinunf.fuel:fuel:2.2.3")
}

// Git is needed in your system PATH for these commands to work.
// If it's not installed, you can return a random value as a workaround
// ref: https://github.com/tachiyomiorg/tachiyomi/blob/master/app/build.gradle.kts
fun getCommitCount(): String {
    return runCommand("git rev-list --count HEAD")
}

fun getGitSha(): String {
    return runCommand("git rev-parse HEAD")
}

fun getBuildTime(): String {
    val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
    df.timeZone = TimeZone.getTimeZone("UTC")
    return df.format(Date())
}

fun runCommand(command: String): String {
    val byteOut = ByteArrayOutputStream()
    project.exec {
        commandLine = command.split(" ")
        standardOutput = byteOut
    }
    return String(byteOut.toByteArray()).trim()
}


