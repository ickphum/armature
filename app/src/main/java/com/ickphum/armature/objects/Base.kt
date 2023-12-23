package com.ickphum.armature.objects

import android.opengl.GLES20
import com.ickphum.armature.data.VertexArray
import com.ickphum.armature.programs.BaseShaderProgram

class Base ( size: Float ){
    private val POSITION_COMPONENT_COUNT = 3
    private val vertexArray = VertexArray(
        floatArrayOf(
            -size, -0f, -size,    // (0) rear left
            -size, -0f, size,    // (0) front left
            size, -0f, -size,     // (1) rear right
            size, -0f, size,     // (1) front right
        )
    )

    fun bindData(program: BaseShaderProgram) {
        vertexArray.setVertexAttribPointer(
            0,
            program.getPositionAttributeLocation(),
            POSITION_COMPONENT_COUNT, 0
        )
    }
    fun draw() {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }
}