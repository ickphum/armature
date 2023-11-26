package com.ickphum.armature.programs

import android.content.Context
import android.opengl.GLES20.glUseProgram
import com.ickphum.armature.util.ShaderHelper

open class ShaderProgram( context: Context, vertexShaderSourceFileName: String, fragmentShaderSourceFileName: String) {
    protected val U_MATRIX = "u_Matrix"
    protected val U_TEXTURE_UNIT = "u_TextureUnit"

    // Attribute constants
    protected val A_POSITION = "a_Position"
    protected val A_COLOR = "a_Color"
    protected val A_TEXTURE_COORDINATES = "a_TextureCoordinates"

    protected var program =
        ShaderHelper.buildProgram(context, vertexShaderSourceFileName, fragmentShaderSourceFileName)

    open fun useProgram() {
        // Set the current OpenGL shader program to this program.
        glUseProgram(program)
    }
}