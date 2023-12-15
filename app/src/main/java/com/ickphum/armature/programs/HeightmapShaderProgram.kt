package com.ickphum.armature.programs

import android.content.Context
import android.opengl.GLES20.glGetAttribLocation
import android.opengl.GLES20.glGetUniformLocation
import android.opengl.GLES20.glUniform3f
import android.opengl.GLES20.glUniform3fv
import android.opengl.GLES20.glUniform4fv
import android.opengl.GLES20.glUniformMatrix4fv
import com.ickphum.armature.R


class HeightmapShaderProgram( context: Context ) :
    ShaderProgram( context, R.raw.heightmap_vertex_shader, R.raw.heightmap_fragment_shader )
{

    private val uVectorToLightLocation = glGetUniformLocation(program, U_VECTOR_TO_LIGHT);
    private val uMVMatrixLocation = glGetUniformLocation(program, U_MV_MATRIX);
    private val uIT_MVMatrixLocation = glGetUniformLocation(program, U_IT_MV_MATRIX);
    private val uMVPMatrixLocation = glGetUniformLocation(program, U_MVP_MATRIX);

    private val uPointLightPositionsLocation = glGetUniformLocation(program, U_POINT_LIGHT_POSITIONS);
    private val uPointLightColorsLocation = glGetUniformLocation(program, U_POINT_LIGHT_COLORS);

//    private val uTextureUnitLocation1 = glGetUniformLocation(program, U_TEXTURE_UNIT_1);
//    private val uTextureUnitLocation2 = glGetUniformLocation(program, U_TEXTURE_UNIT_2);

    private val aPositionLocation = glGetAttribLocation(program, A_POSITION);
    private val aNormalLocation = glGetAttribLocation(program, A_NORMAL);
//    private val aTextureCoordinatesLocation = glGetAttribLocation(program, A_TEXTURE_COORDINATES);

    fun setUniforms(
        mvMatrix: FloatArray,
        it_mvMatrix: FloatArray,
        mvpMatrix: FloatArray,
        vectorToDirectionalLight: FloatArray,
        pointLightPositions: FloatArray,
        pointLightColors: FloatArray,
//        grassTextureId: Int,
//        stoneTextureId: Int
    ) {
        glUniformMatrix4fv(uMVMatrixLocation, 1, false, mvMatrix, 0)
        glUniformMatrix4fv(uIT_MVMatrixLocation, 1, false, it_mvMatrix, 0)
        glUniformMatrix4fv(uMVPMatrixLocation, 1, false, mvpMatrix, 0)

        glUniform3fv(uVectorToLightLocation, 1, vectorToDirectionalLight, 0);

        glUniform4fv(uPointLightPositionsLocation, 3, pointLightPositions, 0)
        glUniform3fv(uPointLightColorsLocation, 3, pointLightColors, 0)
//
//        // The 0 means "GL_TEXTURE0", or the first texture unit.
//        glActiveTexture(GL_TEXTURE0)
//        glBindTexture(GL_TEXTURE_2D, grassTextureId)
//        glUniform1i( uTextureUnitLocation1, 0 )
//
//        // The 1 means "GL_TEXTURE1", or the second texture unit.
//        glActiveTexture(GL_TEXTURE1)
//        glBindTexture(GL_TEXTURE_2D, stoneTextureId)
//        glUniform1i( uTextureUnitLocation2, 1 )
    }

    fun getPositionAttributeLocation(): Int {
        return aPositionLocation
    }

    fun getNormalAttributeLocation(): Int {
        return aNormalLocation
    }

//    fun getTextureCoordinatesAttributeLocation(): Int {
//        return aTextureCoordinatesLocation
//    }
}