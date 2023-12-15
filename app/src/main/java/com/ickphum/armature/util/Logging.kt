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
    }
}
