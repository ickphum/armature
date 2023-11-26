package com.ickphum.armature.programs

import android.content.Context
import android.opengl.GLES20.glGetAttribLocation
import android.opengl.GLES20.glGetUniformLocation
import android.opengl.GLES20.glUniformMatrix4fv

class ColorShaderProgram(context: Context)
    : ShaderProgram( context, "simple_vertex_shader.glsl", "simple_fragment_shader.glsl" )
{
    // Uniform locations
    private val uMatrixLocation = glGetUniformLocation(program, U_MATRIX)

    // Attribute locations
    private val aPositionLocation = glGetAttribLocation(program, A_POSITION)
    private val aColorLocation = glGetAttribLocation(program, A_COLOR)

    fun setUniforms(matrix: FloatArray?) {
        // Pass the matrix into the shader program.
        glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0)
    }

    fun getPositionAttributeLocation(): Int {
        return aPositionLocation
    }

    fun getColorAttributeLocation(): Int {
        return aColorLocation
    }
}