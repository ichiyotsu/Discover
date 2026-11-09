buildscript {
    dependencies {
        // AGP 9 ships Kotlin 2.2.10. Compose 1.13 / Material 3 Expressive need a newer compiler.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    id("com.android.application") apply false
    id("org.jetbrains.kotlin.plugin.compose") apply false
}
