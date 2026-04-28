plugins {
    id("com.android.application") apply false
    kotlin("android") apply false
    kotlin("jvm") apply false
    id("com.google.devtools.ksp") apply false
}

group = "com.xingyue.english"
version = "2.17.0"

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
