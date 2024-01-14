package com.ickphum.armature.programs

import android.content.Context
import android.opengl.GLES20.glUseProgram
import com.ickphum.armature.util.ShaderHelper

open class ShaderProgram( context: Context, vertexShaderResourceId: Int, fragmentShaderResourceId: Int) {

    // Uniform constants
    protected val U_MATRIX = "u_Matrix"
    protected val U_COLOR = "u_Color"
    protected val U_TEXTURE_UNIT = "u_TextureUnit"
    protected val U_TIME = "u_Time"
    protected val U_VECTOR_TO_LIGHT = "u_VectorToLight"

    protected var program =
        ShaderHelper.buildProgram(context, vertexShaderResourceId, fragmentShaderResourceId)

    open fun useProgram() {
        // Set the current OpenGL shader program to this program.
        glUseProgram(program)
    }
}
