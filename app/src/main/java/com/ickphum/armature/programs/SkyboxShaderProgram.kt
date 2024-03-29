package com.ickphum.armature.programs

import com.ickphum.armature.R
import android.content.Context
import android.opengl.GLES20.GL_TEXTURE0
import android.opengl.GLES20.GL_TEXTURE_CUBE_MAP
import android.opengl.GLES20.glActiveTexture
import android.opengl.GLES20.glBindTexture
import android.opengl.GLES20.glGetUniformLocation
import android.opengl.GLES20.glUniform1i
import android.opengl.GLES20.glUniformMatrix4fv


class SkyboxShaderProgram ( context: Context )
    : ShaderProgram( context, R.raw.skybox_vertex_shader, R.raw.skybox_fragment_shader )
{

    private var uMatrixLocation = glGetUniformLocation(program, U_MATRIX)
    private var uTextureUnitLocation = glGetUniformLocation(program, U_TEXTURE_UNIT)

    fun setUniforms(matrix: FloatArray?, textureId: Int) {
        glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0)
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_CUBE_MAP, textureId)
        glUniform1i(uTextureUnitLocation, 0)
    }
}