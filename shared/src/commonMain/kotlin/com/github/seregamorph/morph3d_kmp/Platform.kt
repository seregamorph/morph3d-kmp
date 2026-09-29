package com.github.seregamorph.morph3d_kmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform