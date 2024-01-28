package com.ickphum.armature.programs

import android.content.Context
import android.opengl.GLES20.glGetUniformLocation
import android.opengl.GLES20.glUniform1f
import android.opengl.GLES20.glUniform4f
import android.opengl.GLES20.glUniformMatrix4fv
import com.ickphum.armature.R

class MeshShaderProgram ( context: Context )
    : ShaderProgram( context, R.raw.mesh_vertex, R.raw.mesh_fragment )
{
    private val uMatrixLocation = glGetUniformLocation(program, U_MATRIX)
    private val uTimeLocation = glGetUniformLocation(program, U_TIME)
    private var uColorLocation = glGetUniformLocation(program, U_COLOR)

    fun setUniforms(matrix: FloatArray, elapsedTime: Float, r: Float, g: Float, b: Float ) {
        glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0)
        glUniform1f(uTimeLocation, elapsedTime)
        glUniform4f(uColorLocation, r, g, b, 1f )
    }
}