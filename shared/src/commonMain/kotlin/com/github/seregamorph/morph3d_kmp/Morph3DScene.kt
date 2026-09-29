package com.github.seregamorph.morph3d_kmp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Full-size black "room" with the morphing figure. The original 320x200 screen is scaled
 * uniformly to fit, and each dot is drawn as a fading trail of its recent positions.
 */
@Composable
fun Morph3DScene(modifier: Modifier = Modifier) {
    val engine = remember { Morph3DEngine() }
    val frameCounter = remember { mutableLongStateOf(0L) }

    LaunchedEffect(engine) {
        var last = -1L
        while (true) {
            withFrameNanos { now ->
                if (last >= 0) engine.advance((now - last) / 1e9)
                last = now
                frameCounter.longValue++
            }
        }
    }

    Canvas(modifier.fillMaxSize().background(Color.Black)) {
        // subscribe to frame updates, it invalidates only the draw phase
        frameCounter.longValue
        drawTrails(engine)
    }
}

private fun DrawScope.drawTrails(engine: Morph3DEngine) {
    val scale = min(size.width / Morph3DEngine.SCREEN_WIDTH, size.height / Morph3DEngine.SCREEN_HEIGHT).toFloat()
    val originX = size.width / 2 - (Morph3DEngine.SCREEN_WIDTH / 2).toFloat() * scale
    val originY = size.height / 2 - (Morph3DEngine.SCREEN_HEIGHT / 2).toFloat() * scale
    // one pixel of the original screen, a bit smaller to look crisp on a modern display
    val radius = max(1f, scale * DOT_SIZE / 2)

    // oldest first, so that newer positions are drawn on top, like in the original
    for (frame in engine.frames) {
        val fade = Morph3DEngine.TRAIL_FADE.pow(engine.time - frame.time).toFloat()
        for (i in 0 until POINTS) {
            val x = frame.sx[i]
            if (x.isNaN()) continue
            drawCircle(
                color = Color(frame.r[i] * fade, frame.g[i] * fade, frame.b[i] * fade),
                radius = radius,
                center = Offset(originX + x * scale, originY + frame.sy[i] * scale),
            )
        }
    }
}

private const val DOT_SIZE = 0.75f
