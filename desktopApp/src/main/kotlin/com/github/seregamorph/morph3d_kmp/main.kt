package com.github.seregamorph.morph3d_kmp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.delay
import java.awt.Cursor
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Toolkit
import java.awt.image.BufferedImage

/**
 * Runs as a full-screen screen saver (exits on any key, mouse click or mouse move),
 * or in a regular window with `--window` (exits on Esc or window close).
 */
fun main(args: Array<String>) {
    val windowed = "--window" in args || "-w" in args
    application {
        val state = rememberWindowState(
            placement = if (windowed) WindowPlacement.Floating else WindowPlacement.Fullscreen,
            width = 1280.dp,
            height = 800.dp,
        )
        Window(
            onCloseRequest = ::exitApplication,
            title = "Morph3D",
            state = state,
            onKeyEvent = {
                if (!windowed || it.key == Key.Escape) {
                    exitApplication()
                    true
                } else {
                    false
                }
            },
        ) {
            var modifier = Modifier.fillMaxSize()
            if (!windowed) {
                ScreenSaverMode(onExit = ::exitApplication)
                modifier = modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            if (awaitPointerEvent().type == PointerEventType.Press) exitApplication()
                        }
                    }
                }
            }
            Morph3DScene(modifier)
        }
    }
}

/**
 * Hides the cursor and exits when the mouse moves. The absolute pointer location is polled
 * instead of listening to window events, as those also fire while the window enters full screen.
 */
@Composable
private fun FrameWindowScope.ScreenSaverMode(onExit: () -> Unit) {
    DisposableEffect(window) {
        val blank = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val previous = window.cursor
        window.cursor = Toolkit.getDefaultToolkit().createCustomCursor(blank, Point(0, 0), "blank")
        onDispose { window.cursor = previous ?: Cursor.getDefaultCursor() }
    }
    LaunchedEffect(Unit) {
        // let the full screen transition settle, the pointer can be moved by it
        delay(1000)
        val start = MouseInfo.getPointerInfo()?.location ?: return@LaunchedEffect
        while (true) {
            delay(50)
            val location = MouseInfo.getPointerInfo()?.location ?: continue
            if (location.distance(start) > MOUSE_MOVE_THRESHOLD) onExit()
        }
    }
}

private const val MOUSE_MOVE_THRESHOLD = 10.0
