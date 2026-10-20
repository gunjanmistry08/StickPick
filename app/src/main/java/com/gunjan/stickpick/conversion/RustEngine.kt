package com.gunjan.stickpick.conversion

object RustEngine {
    init {
        System.loadLibrary("stickpick_core")
    }

    external fun ping(): String
}
