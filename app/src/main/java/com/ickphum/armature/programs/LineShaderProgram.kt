package com.ickphum.armature.programs

import android.content.Context
import android.opengl.GLES20.glGetUniformLocation
import android.opengl.GLES20.glUniform4f
import android.opengl.GLES20.glUniformMatrix4fv
import com.ickphum.armature.R

class LineShaderProgram ( context: Context )
    : ShaderProgram( context, R.raw.line_vertex, R.raw.line_fragment )
{
    private val uMatrixLocation = glGetUniformLocation(program, U_MATRIX)
    private var uColorLocation = glGetUniformLocation(program, U_COLOR)

    fun setUniforms(matrix: FloatArray, r: Float, g: Float, b: Float ) {
        glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0)
        glUniform4f(uColorLocation, r, g, b, 1f )
    }
}