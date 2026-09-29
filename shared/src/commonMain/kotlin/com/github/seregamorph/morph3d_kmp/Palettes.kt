package com.github.seregamorph.morph3d_kmp

/**
 * Color sets of the original (`pal_sets`), VGA 6-bit RGB. Each set has 4 shades,
 * from the nearest (brightest) to the farthest (darkest) depth level.
 */
internal object Palettes {

    private val sets = arrayOf(
        intArrayOf(60, 35, 60, 52, 28, 52, 44, 22, 44, 36, 15, 36), // purple
        intArrayOf(53, 45, 19, 47, 40, 15, 41, 35, 12, 35, 30, 10), // yellow
        intArrayOf(30, 60, 60, 23, 52, 52, 17, 44, 44, 10, 36, 36), // cyan
        intArrayOf(22, 35, 60, 20, 33, 52, 18, 31, 44, 16, 29, 36), // blue
        intArrayOf(54, 54, 54, 46, 46, 46, 38, 38, 38, 30, 30, 30), // grey
        intArrayOf(60, 54, 50, 45, 42, 37, 35, 33, 29, 30, 29, 26), // dark
        intArrayOf(22, 60, 35, 20, 52, 33, 18, 44, 31, 16, 36, 29), // green
        intArrayOf(60, 40, 20, 52, 33, 13, 44, 27, 7, 36, 20, 0), // red
        intArrayOf(50, 50, 60, 43, 43, 52, 37, 37, 44, 30, 30, 36), // lavender
    )

    const val COUNT = 9
    const val SHADES = 4

    /** Copies palette [index] into [dest] as 12 floats in 0..1 (4 shades of r, g, b). */
    fun load(index: Int, dest: FloatArray) {
        val set = sets[index]
        for (i in set.indices) dest[i] = set[i] / 63f
    }
}
