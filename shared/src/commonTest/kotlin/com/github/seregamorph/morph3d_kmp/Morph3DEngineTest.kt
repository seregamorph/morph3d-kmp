package com.github.seregamorph.morph3d_kmp

import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

class Morph3DEngineTest {

    @Test
    fun allFiguresAreFiniteAndFitTheRoom() {
        for (index in 0 until Figures.COUNT) {
            val p = PointCloud()
            p.x.fill(Float.NaN)
            Figures.generate(index, p)
            for (i in 0 until POINTS) {
                assertTrue(p.x[i].isFinite() && p.y[i].isFinite() && p.z[i].isFinite(), "figure $index point $i")
                assertTrue(abs(p.x[i]) < 1000 && abs(p.y[i]) < 1000 && abs(p.z[i]) < 1000, "figure $index point $i")
            }
        }
    }

    @Test
    fun trailsKeepRecentFramesOnly() {
        val engine = Morph3DEngine(Random(42))
        // several full morph cycles at 60 fps
        repeat(3000) {
            engine.advance(1.0 / 60)
            assertTrue(engine.frames.size <= Morph3DEngine.TRAIL_TICKS.toInt())
        }
        val visible = engine.frames.last().sx.count { !it.isNaN() }
        assertTrue(visible > POINTS / 2, "visible $visible")
    }
}
