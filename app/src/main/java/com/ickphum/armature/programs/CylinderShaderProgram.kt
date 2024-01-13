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
import android.opengl.GLES20.glUniform3f
import android.opengl.GLES20.glUniform4f
import android.opengl.GLES20.glUniform4fv
import android.opengl.GLES20.glUniformMatrix4fv
import com.ickphum.armature.util.Geometry

class CylinderShaderProgram (context: Context )
    : ShaderProgram( context, R.raw.cylinder_vertex, R.raw.cylinder_fragment )
{
    private val uMatrixLocation = glGetUniformLocation(program, U_MATRIX)
    private val uVectorToLightLocation = glGetUniformLocation(program, U_VECTOR_TO_LIGHT)

    private var aPositionLocation = glGetAttribLocation(program, A_POSITION)
    private var aNormalLocation = glGetAttribLocation(program, A_NORMAL)

    fun setUniforms(matrix: FloatArray, vectorToLight: Geometry.Vector ) {
        glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0)
        glUniform3f(uVectorToLightLocation, vectorToLight.x, vectorToLight.y, vectorToLight.z);
    }

    fun getPositionAttributeLocation(): Int {
        return aPositionLocation
    }

    fun getNormalAttributeLocation(): Int {
        return aNormalLocation
    }
}