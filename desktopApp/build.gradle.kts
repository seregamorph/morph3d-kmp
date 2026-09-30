import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
}

compose.desktop {
    application {
        mainClass = "com.github.seregamorph.morph3d_kmp.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "Morph3D"
            packageVersion = "1.0.0"
            macOS {
                bundleID = "com.github.seregamorph.morph3d"
                iconFile.set(project.file("icons/Morph3D.icns"))
            }
        }
    }
}