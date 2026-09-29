package com.github.seregamorph.morph3d_kmp

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

@Suppress("FunctionName", "unused") // called from Swift
fun MainViewController(): UIViewController = ComposeUIViewController { Morph3DScene() }
