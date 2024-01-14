package com.ickphum.armature.objects

import android.opengl.GLES20
import com.ickphum.armature.data.VertexArray
import com.ickphum.armature.programs.BaseShaderProgram
import com.ickphum.armature.util.Geometry


class Base ( val size: Float ){
    private val POSITION_COMPONENT_COUNT = 3
    private val vertexArray = VertexArray(
        floatArrayOf(
            -size, -0f, -size,    // (0) rear left
            -size, -0f, size,    // (0) front left
            size, -0f, -size,     // (1) rear right
            size, -0f, size,     // (1) front right
        )
    )
    private val plane = Geometry.Plane(Geometry.Point(0f, 0f, 0f), Geometry.Vector(0f, 1f, 0f))

    fun bindData(program: BaseShaderProgram) {
        vertexArray.setVertexAttribPointer(
            0,
            0,
            POSITION_COMPONENT_COUNT, 0
        )
    }
    fun draw() {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    fun findIntersectionPoint( ray: Geometry.Ray): Geometry.Point? {
        val touchedPoint: Geometry.Point? = Geometry.intersectionPoint(ray, plane)
        if ( touchedPoint != null && touchedPoint.x >= -size && touchedPoint.x <= size
            && touchedPoint.z >= -size && touchedPoint.z <= size)
        {
            return touchedPoint
        }
        return null
    }
}