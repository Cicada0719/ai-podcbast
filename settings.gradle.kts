pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        kotlin("jvm").version(extra["kotlin.version"] as String)
        kotlin("android").version(extra["kotlin.version"] as String)
        id("com.android.application").version(extra["agp.version"] as String)
        id("com.google.devtools.ksp").version(extra["ksp.version"] as String)
    }
}

rootProject.name = "XingYueEnglish"

include(":core")
include(":androidApp")

