package com.ickphum.armature.programs

import android.content.Context
import android.opengl.GLES20.GL_TEXTURE0
import android.opengl.GLES20.GL_TEXTURE_2D
import android.opengl.GLES20.glActiveTexture
import android.opengl.GLES20.glBindTexture
import android.opengl.GLES20.glGetAttribLocation
import android.opengl.GLES20.glGetUniformLocation
import android.opengl.GLES20.glUniform1i
import android.opengl.GLES20.glUniformMatrix4fv


class TextureShaderProgram(context: Context)
    : ShaderProgram( context, "texture_vertex_shader.glsl", "texture_fragment_shader.glsl" )
{
    // Uniform locations
    private var uMatrixLocation = glGetUniformLocation(program, U_MATRIX)
    private var uTextureUnitLocation = glGetUniformLocation(program, U_TEXTURE_UNIT)

    // Attribute locations
    private var aPositionLocation = glGetAttribLocation(program, A_POSITION)
    private var aTextureCoordinatesLocation = glGetAttribLocation(program, A_TEXTURE_COORDINATES)

    fun setUniforms(matrix: FloatArray, textureId: Int) {
        // Pass the matrix into the shader program.
        glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0)
        // Set the active texture unit to texture unit 0.
        glActiveTexture(GL_TEXTURE0)
        // Bind the texture to this unit.
        glBindTexture(GL_TEXTURE_2D, textureId)
        // Tell the texture uniform sampler to use this texture in the shader by
        // telling it to read from texture unit 0.
        glUniform1i(uTextureUnitLocation, 0)
    }

    fun getPositionAttributeLocation(): Int {
        return aPositionLocation
    }

    fun getTextureCoordinatesAttributeLocation(): Int {
        return aTextureCoordinatesLocation
    }
}