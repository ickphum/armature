package com.ickphum.armature.util

import android.opengl.GLES20
import android.util.Log

class Logging {
    companion object Helper {
        fun checkError(dbgDomain: String, dbgText: String) {

            val error = GLES20.glGetError()

            if (error != GLES20.GL_NO_ERROR) {
                Log.e(dbgDomain, dbgText)
            }
        }


        fun dumpArray( tag: String, arr: FloatArray, cols: Int ) {
            val rows = arr.size / cols - 1
            for ( r in 0 .. rows ) {
                Log.d( tag, "dumpArray: row $r: ${arr[ r * cols ]}, ${arr[ r * cols + 1]}, ${arr[ r * cols + 2]}")
            }

        }

    }
}
