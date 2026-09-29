package com.github.seregamorph.morph3d_kmp

import kotlin.math.floor
import kotlin.math.min
import kotlin.random.Random

/**
 * Simulation of the MORPH3D scene. Time is measured in "ticks", one tick is one frame of the
 * original program, so all the constants below match the assembler source. Unlike the original,
 * ticks may be fractional, which makes the animation independent of the display refresh rate.
 *
 * Coordinates of [frames] are in the original 320x200 screen space; the renderer scales them.
 */
class Morph3DEngine(private val random: Random = Random.Default) {

    internal class Frame {
        var time = 0.0
        val sx = FloatArray(POINTS)
        val sy = FloatArray(POINTS)
        val r = FloatArray(POINTS)
        val g = FloatArray(POINTS)
        val b = FloatArray(POINTS)
    }

    private var src1 = PointCloud()
    private var src2 = PointCloud()
    private val morphed = PointCloud()

    private var figure = -1
    private var morphing = false
    private var morphProgress = 0.0
    private var holdLeft = 0.0

    // 4 shades x (r, g, b)
    private val paletteFrom = FloatArray(Palettes.SHADES * 3)
    private val paletteTo = FloatArray(Palettes.SHADES * 3)
    private val palette = FloatArray(Palettes.SHADES * 3)
    private var paletteIndex = -1

    // flow of the dots along the figure, fraction of the way to the next point
    private var motionPhase = 0.0

    private var yaw = 0.0
    private var roll = 0.0
    private var pitch = 0.0
    private var yawTheta = 0.0
    private var rollTheta = 0.0
    private var pitchTheta = 0.0
    private var depthTheta = 0.0
    private var moveXTheta = 0.0
    private var moveYTheta = 0.0
    private var mx = 0.0
    private var my = 0.0
    private var mz = -1550.0

    /** Current time in ticks. */
    internal var time = 0.0
        private set

    /** Recent frames, oldest first, forming the fading trails. */
    internal val frames = ArrayDeque<Frame>()
    private val pool = ArrayList<Frame>()

    init {
        // the very first morph is from a collapsed point to a figure, fading in from black
        src1.clear()
        startMorph(paletteIndex = 0)
        capture()
    }

    /** Advances the animation by the real time passed since the previous call. */
    fun advance(seconds: Double) {
        // after a long stall (e.g. hidden window) don't try to catch up
        var ticks = (seconds * TICKS_PER_SECOND).coerceIn(0.0, MAX_TICKS_PER_FRAME)
        while (ticks > 0.0) {
            val dt = min(ticks, 1.0)
            step(dt)
            ticks -= dt
        }
        capture()
    }

    private fun startMorph(paletteIndex: Int = randomOther(Palettes.COUNT, this.paletteIndex)) {
        figure = randomOther(Figures.COUNT, figure)
        Figures.generate(figure, src2)
        palette.copyInto(paletteFrom)
        Palettes.load(paletteIndex, paletteTo)
        this.paletteIndex = paletteIndex
        morphing = true
        morphProgress = 0.0
    }

    private fun randomOther(count: Int, current: Int): Int {
        if (current < 0) return random.nextInt(count)
        return (current + 1 + random.nextInt(count - 1)) % count
    }

    private fun step(dt: Double) {
        time += dt

        if (morphing) {
            morphProgress += dt / MORPH_TICKS
            if (morphProgress >= 1.0) {
                morphing = false
                val t = src1
                src1 = src2
                src2 = t
                holdLeft = HOLD_TICKS
            }
        } else {
            holdLeft -= dt
            if (holdLeft <= 0.0) startMorph()
        }

        motionPhase += dt / MOTION_TICKS
        while (motionPhase >= 1.0) {
            motionPhase -= 1.0
            src1.rotateLeft()
            src2.rotateLeft()
        }

        // sinusoidal morph (domorph), the palette cross-fade uses the same sin^2 curve
        val w = if (morphing) sinT(morphProgress * 900.0).let { it * it } else 0.0
        if (morphing) {
            for (i in palette.indices) palette[i] = (paletteFrom[i] * (1 - w) + paletteTo[i] * w).toFloat()
        } else {
            paletteTo.copyInto(palette)
        }
        val wf = w.toFloat()
        for (i in 0 until POINTS) {
            morphed.x[i] = src1.x[i] + (src2.x[i] - src1.x[i]) * wf
            morphed.y[i] = src1.y[i] + (src2.y[i] - src1.y[i]) * wf
            morphed.z[i] = src1.z[i] + (src2.z[i] - src1.z[i]) * wf
        }

        // incrotation: rotation speeds themselves vary sinusoidally
        yawTheta -= YAW_INC / 3 * dt
        rollTheta -= ROLL_INC / 3 * dt
        pitchTheta -= PITCH_INC / 3 * dt
        depthTheta -= DEPTH_INC * dt
        moveXTheta -= MOVE_X_INC * dt
        moveYTheta -= MOVE_Y_INC * dt
        yaw += angularSpeed(yawTheta, YAW_INC) * dt
        roll += angularSpeed(rollTheta, ROLL_INC) * dt
        pitch += angularSpeed(pitchTheta, PITCH_INC) * dt
        mx = 150.0 * sinT(moveXTheta)
        my = 150.0 * sinT(moveYTheta)
        mz = 800.0 * sinT(depthTheta) - 1500.0
    }

    private fun angularSpeed(theta: Double, inc: Int): Double =
        inc * sinT(1800.0 * sinT(theta) + 1800.0) + inc / 2

    private fun capture() {
        val frame = obtainFrame()
        val phase = motionPhase.toFloat()
        val cy = cosT(yaw)
        val sy = sinT(yaw)
        val cr = cosT(roll)
        val sr = sinT(roll)
        val cp = cosT(pitch)
        val sp = sinT(pitch)
        for (i in 0 until POINTS) {
            // motion: each dot moves linearly towards the position of the next one
            val j = if (i == POINTS - 1) 0 else i + 1
            val x = (morphed.x[i] + (morphed.x[j] - morphed.x[i]) * phase).toDouble()
            val y = (morphed.y[i] + (morphed.y[j] - morphed.y[i]) * phase).toDouble()
            val z = (morphed.z[i] + (morphed.z[j] - morphed.z[i]) * phase).toDouble()

            // rotate: yaw around y, roll around z, pitch around x
            val xa = x * cy - z * sy
            val za = x * sy + z * cy
            val rx = xa * cr + y * sr
            val ya = y * cr - xa * sr
            val rz = za * cp - ya * sp
            val ry = za * sp + ya * cp

            val depth = rz + mz
            if (depth > NEAR_PLANE) {
                frame.sx[i] = Float.NaN
                continue
            }
            frame.sx[i] = ((rx + mx) * MAG / depth + SCREEN_WIDTH / 2).toFloat()
            frame.sy[i] = ((ry + my) * MAG / depth + SCREEN_HEIGHT / 2).toFloat()

            // the original had 4 discrete depth shades, here they are blended smoothly
            val t = ((240.0 - rz) / 160.0).coerceIn(0.0, Palettes.SHADES - 1.0)
            val shade = min(floor(t).toInt(), Palettes.SHADES - 2)
            val f = (t - shade).toFloat()
            val o = shade * 3
            frame.r[i] = palette[o] + (palette[o + 3] - palette[o]) * f
            frame.g[i] = palette[o + 1] + (palette[o + 4] - palette[o + 1]) * f
            frame.b[i] = palette[o + 2] + (palette[o + 5] - palette[o + 2]) * f
        }
        frames.addLast(frame)
        while (time - frames.first().time >= TRAIL_TICKS) {
            pool.add(frames.removeFirst())
        }
    }

    private fun obtainFrame(): Frame = (pool.removeLastOrNull() ?: Frame()).also { it.time = time }

    internal companion object {
        const val TICKS_PER_SECOND = 60.0
        const val MAX_TICKS_PER_FRAME = 4.0

        const val SCREEN_WIDTH = 320.0
        const val SCREEN_HEIGHT = 200.0
        const val MAG = 300.0
        const val NEAR_PLANE = -100.0

        const val MORPH_TICKS = 1800.0 / 8 // 1800 / MORPHLENINC
        const val HOLD_TICKS = 500.0 // MORPHDELAY
        const val MOTION_TICKS = 57.0

        /** Trail length, SHADOW in the original. */
        const val TRAIL_TICKS = 10.0

        /** Palette fade per tick, FADELVL in the original. */
        const val TRAIL_FADE = 232.0 / 256.0

        const val YAW_INC = 11
        const val ROLL_INC = 17
        const val PITCH_INC = 5
        const val DEPTH_INC = 3
        const val MOVE_X_INC = 2
        const val MOVE_Y_INC = 1
    }
}
