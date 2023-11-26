package com.ickphum.armature.data

import android.opengl.GLES20.GL_TRIANGLE_FAN
import android.opengl.GLES20.glDrawArrays
import com.ickphum.armature.Constants.BYTES_PER_FLOAT
import com.ickphum.armature.programs.TextureShaderProgram




private const val POSITION_COMPONENT_COUNT = 2
private const val TEXTURE_COORDINATES_COMPONENT_COUNT = 2
private const val STRIDE: Int = (POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT) * BYTES_PER_FLOAT

class Table {

    private val VERTEX_DATA = floatArrayOf(
        // Order of coordinates: X, Y, S, T
        // Triangle Fan
        0f,     0f,     0.5f,   0.5f,
        -0.5f,  -0.8f,  0f,     0.9f,
        0.5f,   -0.8f,  1f,     0.9f,
        0.5f,   0.8f,   1f,     0.1f,
        -0.5f,  0.8f,   0f,     0.1f,
        -0.5f, -0.8f,   0f,     0.9f
    )

    private val vertexArray = VertexArray( VERTEX_DATA )

    fun bindData(textureProgram: TextureShaderProgram) {
        vertexArray.setVertexAttribPointer(
            0,
            textureProgram.getPositionAttributeLocation(),
            POSITION_COMPONENT_COUNT,
            STRIDE
        )
        vertexArray.setVertexAttribPointer(
            POSITION_COMPONENT_COUNT,
            textureProgram.getTextureCoordinatesAttributeLocation(),
            TEXTURE_COORDINATES_COMPONENT_COUNT,
            STRIDE
        )
    }
    fun draw() {
        glDrawArrays(GL_TRIANGLE_FAN, 0, 6)
    }
}