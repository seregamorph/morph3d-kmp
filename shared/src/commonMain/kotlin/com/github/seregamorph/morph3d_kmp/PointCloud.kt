package com.github.seregamorph.morph3d_kmp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Number of dots in a figure, as in the original. */
const val POINTS = 200

/** Sine of an angle given in 1/10 degrees (3600 = full period), like the original SINTAB. */
internal fun sinT(theta: Double): Double = sin(theta * PI / 1800.0)

internal fun cosT(theta: Double): Double = cos(theta * PI / 1800.0)

internal class PointCloud(val size: Int = POINTS) {
    val x = FloatArray(size)
    val y = FloatArray(size)
    val z = FloatArray(size)

    fun set(i: Int, x: Double, y: Double, z: Double) {
        this.x[i] = x.toFloat()
        this.y[i] = y.toFloat()
        this.z[i] = z.toFloat()
    }

    fun clear() {
        x.fill(0f)
        y.fill(0f)
        z.fill(0f)
    }

    /** Shifts all points one position towards the start, the first point becomes the last. */
    fun rotateLeft() {
        rotateLeft(x)
        rotateLeft(y)
        rotateLeft(z)
    }

    private fun rotateLeft(a: FloatArray) {
        val first = a[0]
        a.copyInto(a, destinationOffset = 0, startIndex = 1)
        a[a.size - 1] = first
    }
}
