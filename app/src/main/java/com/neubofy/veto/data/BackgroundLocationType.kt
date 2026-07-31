package com.neubofy.veto.data

class BackgroundLocationType(inState: Int) {
    var gps: Boolean = (inState and GPS) != 0

    /**
     * Use bitmasking to encode the state into an integer that can be persisted in the settings.
     */
    fun encode(): Int {
        var state = BASE
        if (gps) state = state or GPS
        return state
    }

    fun isAll(): Boolean {
        return gps
    }

    fun isEmpty(): Boolean {
        return !gps
    }

    companion object {
        // Offset/prefix values in order to distinguish them from the old encoding
        const val BASE = 0x1000 // 4096 --> plenty of room for future extensions

        @JvmStatic
        var EMPTY = BASE // only for Java interop

        // Only GPS remains supported
        const val GPS = 0x2

        fun fromEmpty(): BackgroundLocationType {
            return BackgroundLocationType(BASE)
        }

        // OLD ENCODING:
        // 0=GPS, 1=CELL, 2=ALL, 3=NONE
        fun fromOldEncoding(inState: Int): BackgroundLocationType {
            val state = BackgroundLocationType(BASE)

            when (inState) {
                0 -> state.gps = true
                2 -> state.gps = true
                else -> {
                    // other states ignored
                }
            }

            return state
        }
    }
}
