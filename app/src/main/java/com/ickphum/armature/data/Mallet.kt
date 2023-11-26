package com.ickphum.armature.data

import android.opengl.GLES20.GL_POINTS
import android.opengl.GLES20.glDrawArrays
import com.ickphum.armature.Constants
import com.ickphum.armature.programs.ColorShaderProgram




private const val POSITION_COMPONENT_COUNT = 2
private const val COLOR_COMPONENT_COUNT = 3
private const val STRIDE: Int = (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) * Constants.BYTES_PER_FLOAT

class Mallet {

    // Order of coordinates: X, Y, R, G, B
    private val VERTEX_DATA = floatArrayOf(
        0f, -0.4f, 0f, 0f, 1f,
        0f, 0.4f, 1f, 0f, 0f
    )

    private val vertexArray = VertexArray( VERTEX_DATA )

    fun bindData(colorProgram: ColorShaderProgram) {
        vertexArray.setVertexAttribPointer(
            0,
            colorProgram.getPositionAttributeLocation(),
            POSITION_COMPONENT_COUNT,
            STRIDE
        )
        vertexArray.setVertexAttribPointer(
            POSITION_COMPONENT_COUNT,
            colorProgram.getColorAttributeLocation(),
            COLOR_COMPONENT_COUNT,
            STRIDE
        )
    }

    fun draw() {
        glDrawArrays(GL_POINTS, 0, 2)
    }
}