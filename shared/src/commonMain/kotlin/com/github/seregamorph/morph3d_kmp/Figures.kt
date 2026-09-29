package com.github.seregamorph.morph3d_kmp

/**
 * Figure generators, a port of `initmorph` (MORPHSUB.ASM) and the shape helpers of 3DTRANS.ASM.
 * Angles are in 1/10 degrees, coordinates are in the original 3D units.
 */
internal object Figures {

    const val COUNT = 19

    // frequencies of lissajous figures per axis (x, y, z)
    private val lissOmegas = arrayOf(
        intArrayOf(1, 5, 3),
        intArrayOf(5, 3, 6),
        intArrayOf(5, 5, 6),
        intArrayOf(4, 4, 7),
        intArrayOf(11, 1, 9),
        intArrayOf(2, 1, 3),
        intArrayOf(3, 2, 4),
        intArrayOf(10, 1, 9),
        intArrayOf(5, 1, 3),
    )
    private val lissOffsets = intArrayOf(0, 900, 0)

    private val cylinderProfile = arrayOf(
        intArrayOf(50, 350), intArrayOf(150, 350), intArrayOf(250, 350), intArrayOf(250, 250),
        intArrayOf(250, 150), intArrayOf(250, 50), intArrayOf(250, -50), intArrayOf(250, -150),
        intArrayOf(150, -150), intArrayOf(50, -150),
    )
    private val coneProfile = arrayOf(
        intArrayOf(0, 140), intArrayOf(54, 220), intArrayOf(108, 300), intArrayOf(162, 380),
        intArrayOf(216, 460), intArrayOf(270, 540), intArrayOf(324, 620), intArrayOf(220, 620),
        intArrayOf(120, 620), intArrayOf(81, 710),
    )
    private val diskProfile = intArrayOf(150, 180, 210, 240, 270, 300, 330, 360)

    fun generate(index: Int, p: PointCloud) {
        when (index) {
            0 -> torus(p, inner = 50.0, outer = 300.0)
            in 1..9 -> {
                val omega = lissOmegas[index - 1]
                makeLiss(p.x, omega[0], lissOffsets[0], 320.0)
                makeLiss(p.y, omega[1], lissOffsets[1], 320.0)
                makeLiss(p.z, omega[2], lissOffsets[2], 320.0)
            }

            10 -> sphere(p, radius = 450.0)
            11 -> {
                profile(p, cylinderProfile)
                revolve(p, cylinderProfile.size, 60.0, 60.0, 60.0)
            }

            12 -> { // christmas tree
                profile(p, coneProfile)
                revolve(p, coneProfile.size, 150.0, -400.0, 0.0)
            }

            13 -> {
                makeParam(p, a = 100.0, b = 200.0, count = 40)
                for (i in 0 until 40) p.x[i] -= 50f
                revolve(p, 40, 110.0, 110.0, 110.0)
            }

            14 -> {
                makeParam(p, a = 50.0, b = 300.0, count = 40)
                makeLiss(p.z, 1, 900, 200.0)
                revolve(p, 40, 110.0, 110.0, 110.0)
            }

            15 -> {
                for (i in diskProfile.indices) p.set(i, diskProfile[i].toDouble(), 0.0, 0.0)
                makeLiss(p.y, 10, 0, 200.0)
                makeLiss(p.z, 10, 0, 200.0)
                revolve(p, diskProfile.size, 60.0, 60.0, 60.0)
            }

            16 -> {
                profile(p, cylinderProfile)
                makeLiss(p.z, 24, 600, 200.0)
                revolve(p, cylinderProfile.size, 0.0, 0.0, 0.0)
            }

            17 -> box(p)
            18 -> fountain(p)
            else -> throw IllegalArgumentException("Unknown figure $index")
        }
    }

    /** `makeliss`: one axis of a lissajous figure, `coord = amplitude * sin(offset - omega * t)`. */
    private fun makeLiss(axis: FloatArray, omega: Int, offset: Int, amplitude: Double) {
        var theta = offset.toDouble()
        for (i in axis.indices) {
            axis[i] = (amplitude * sinT(theta)).toFloat()
            theta -= omega * 3600.0 / POINTS
        }
    }

    /** `makeparam`: planar curve `r = a - b*sin(T)`. */
    private fun makeParam(p: PointCloud, a: Double, b: Double, count: Int) {
        for (i in 0 until count) {
            val theta = i * 3600.0 / count
            val r = a - b * sinT(theta)
            p.set(i, r * sinT(theta), r * cosT(theta), 0.0)
        }
    }

    private fun profile(p: PointCloud, data: Array<IntArray>) {
        for (i in data.indices) p.set(i, data[i][0].toDouble(), data[i][1].toDouble(), 0.0)
    }

    private fun sphere(p: PointCloud, radius: Double) {
        // half circle of 10 dots, 18 degrees apart
        for (i in 0 until 10) {
            val theta = 90.0 + i * 180.0
            p.set(i, radius * sinT(theta), radius * cosT(theta), 0.0)
        }
        revolve(p, 10, 0.0, 0.0, 0.0)
    }

    private fun torus(p: PointCloud, inner: Double, outer: Double) {
        // small circle of 8 dots, 45 degrees apart
        for (i in 0 until 8) {
            val theta = 90.0 + i * 450.0
            p.set(i, inner * sinT(theta) + outer, inner * cosT(theta), 0.0)
        }
        revolve(p, 8, 150.0, 0.0, 0.0)
    }

    private fun box(p: PointCloud) {
        // a square frame of 50 dots at z = 255, revolved 4 times gives the cube edges
        var i = 0
        for (k in 0 until 18) p.set(i++, 255.0, 255.0 - k * 30, 255.0)
        for (k in 0 until 16) {
            val c = 225.0 - k * 30
            p.set(18 + k, c, 255.0, 255.0)
            p.set(34 + k, c, -255.0, 255.0)
        }
        revolve(p, 50, 0.0, 0.0, 0.0)
    }

    private fun fountain(p: PointCloud) {
        for (i in 0 until 40) {
            val c = 40 - i
            val theta = ((1800 * 5 + 900) * c / 40 % 3600).toDouble()
            val a = c * 8.0
            p.set(i, (320.0 - c * 8) * 2, a * sinT(theta) + a, 0.0)
        }
        revolve(p, 40, 0.0, 0.0, 0.0)
    }

    /**
     * `rotate2Dfig`: copies the first [pixels] points around the y axis (full turn) to fill all points,
     * then translates the whole figure.
     */
    private fun revolve(p: PointCloud, pixels: Int, mx: Double, my: Double, mz: Double) {
        val segments = p.size / pixels
        for (s in 1 until segments) {
            val yaw = s * 3600.0 / segments
            val c = cosT(yaw)
            val sn = sinT(yaw)
            for (j in 0 until pixels) {
                val x = p.x[j].toDouble()
                val z = p.z[j].toDouble()
                p.set(s * pixels + j, x * c - z * sn, p.y[j].toDouble(), x * sn + z * c)
            }
        }
        for (i in 0 until p.size) {
            p.x[i] += mx.toFloat()
            p.y[i] += my.toFloat()
            p.z[i] += mz.toFloat()
        }
    }
}
