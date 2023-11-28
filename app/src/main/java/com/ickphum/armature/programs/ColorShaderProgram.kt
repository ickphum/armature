package com.ickphum.armature.programs

import android.content.Context
import android.opengl.GLES20.glGetAttribLocation
import android.opengl.GLES20.glGetUniformLocation
import android.opengl.GLES20.glUniform4f
import android.opengl.GLES20.glUniformMatrix4fv


class ColorShaderProgram(context: Context)
    : ShaderProgram( context, "simple_vertex_shader.glsl", "simple_fragment_shader.glsl" ) {
    // Uniform locations
    private val uMatrixLocation = glGetUniformLocation(program, U_MATRIX)

    // Attribute locations
    private val aPositionLocation = glGetAttribLocation(program, A_POSITION)
    private val uColorLocation = glGetUniformLocation(program, U_COLOR)

    fun setUniforms(matrix: FloatArray?, r: Float, g: Float, b: Float) {
        glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0)
        glUniform4f(uColorLocation, r, g, b, 1f)
    }

    fun getPositionAttributeLocation(): Int {
        return aPositionLocation
    }

}