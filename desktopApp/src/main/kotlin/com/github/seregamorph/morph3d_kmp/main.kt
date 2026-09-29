package com.github.seregamorph.morph3d_kmp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "morph3d-kmp",
    ) {
        App()
    }
}