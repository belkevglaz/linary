import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    id("io.gitlab.arturbosch.detekt")
}

kotlin {
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
            implementation("com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-rc13")
            // Та же библиотека, которой редактор кодирует HTML-сущности; ею же декодируем
            implementation("com.mohamedrejeb.ksoup:ksoup-entities:0.5.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
                implementation("org.xerial:sqlite-jdbc:3.49.1.0")
            }
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "ru.b3lka.linary.resources"
    generateResClass = always
}

compose.desktop {
    application {
        mainClass = "ru.b3lka.linary.desktop.MainKt"
        nativeDistributions {
            // Для Manjaro/Arch — packaging/arch/PKGBUILD, для установки без root — packaging/install.sh.
            // Deb и Rpm собираются на системах с dpkg-deb / rpmbuild: ./gradlew packageDeb, packageRpm
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "linary"
            packageVersion = providers.gradleProperty("appVersion").get()
            description = "Journal app for Linux that works with a Diarium sync folder"
            vendor = "belkevglaz"
            modules("java.sql", "java.prefs", "java.naming", "jdk.unsupported")
            linux {
                iconFile.set(rootProject.file("packaging/icons/linary-512.png"))
                shortcut = true
                menuGroup = "Office"
                appCategory = "Office"
            }
        }
        buildTypes.release.proguard {
            // ProGuard ломает sqlite-jdbc (JNI) и рефлексию; размер здесь не важен
            isEnabled.set(false)
        }
    }
}

/*
 * Статический анализ: ./gradlew detekt
 * Форматирование (ktlint через detekt-formatting) исправляется автоматически (autoCorrect).
 */
detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    source.setFrom(
        "src/commonMain/kotlin",
        "src/commonTest/kotlin",
        "src/desktopMain/kotlin",
        "src/desktopTest/kotlin",
    )
    autoCorrect = true
    parallel = true
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
}

/*
 * detekt 1.23.8 собран под Kotlin 2.0.21 и не должен получать более новый компилятор
 * из проекта (иначе падает с "detekt was compiled with Kotlin ...").
 */
configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("2.0.21")
        }
    }
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "17"
    reports {
        html.required.set(true)
        xml.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
        txt.required.set(true)
    }
}
