package com.ickphum.armature.programs

import android.content.Context
import android.opengl.GLES20.glGetUniformLocation
import android.opengl.GLES20.glUniform3f
import android.opengl.GLES20.glUniformMatrix4fv
import com.ickphum.armature.R
import com.ickphum.armature.util.Geometry

class CylinderShaderProgram (context: Context )
    : ShaderProgram( context, R.raw.cylinder_vertex, R.raw.cylinder_fragment )
{
    private val uMatrixLocation = glGetUniformLocation(program, U_MATRIX)
    private val uVectorToLightLocation = glGetUniformLocation(program, U_VECTOR_TO_LIGHT)
    private val uColorLocation = glGetUniformLocation(program, U_COLOR)

    fun setUniforms(matrix: FloatArray, vectorToLight: Geometry.Vector ) {
        glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0)
        glUniform3f(uVectorToLightLocation, vectorToLight.x, vectorToLight.y, vectorToLight.z)
    }

    fun setColorUniform( rgb: FloatArray) {
        glUniform3f(uColorLocation, rgb[0], rgb[1], rgb[2] );
    }

}