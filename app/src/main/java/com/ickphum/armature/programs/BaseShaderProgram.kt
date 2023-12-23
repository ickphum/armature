package com.ickphum.armature.programs

import com.ickphum.armature.R
import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES20.GL_TEXTURE0
import android.opengl.GLES20.GL_TEXTURE_CUBE_MAP
import android.opengl.GLES20.glActiveTexture
import android.opengl.GLES20.glBindTexture
import android.opengl.GLES20.glGetAttribLocation
import android.opengl.GLES20.glGetUniformLocation
import android.opengl.GLES20.glUniform1f
import android.opengl.GLES20.glUniform1i
import android.opengl.GLES20.glUniform4f
import android.opengl.GLES20.glUniform4fv
import android.opengl.GLES20.glUniformMatrix4fv


class BaseShaderProgram ( context: Context )
    : ShaderProgram( context, R.raw.base_vertex, R.raw.base_fragment )
{
    private val uMatrixLocation = glGetUniformLocation(program, U_MATRIX)
    private val uTimeLocation = glGetUniformLocation(program, U_TIME)
    private var aPositionLocation = glGetAttribLocation(program, A_POSITION)

    fun setUniforms(matrix: FloatArray, elapsedTime: Float ) {
        glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0)
        glUniform1f(uTimeLocation, elapsedTime)
    }

    fun getPositionAttributeLocation(): Int {
        return aPositionLocation
    }
}