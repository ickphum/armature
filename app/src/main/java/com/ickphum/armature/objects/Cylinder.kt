package com.ickphum.armature.objects

import android.opengl.GLES20
import com.ickphum.armature.data.VertexArray
import com.ickphum.armature.programs.BaseShaderProgram
import com.ickphum.armature.util.Geometry
import kotlin.math.cos
import kotlin.math.sin

const val POSITION_COMPONENT_COUNT = 3
const val SEGMENTS = 12
const val NUMBER_VERTICES = SEGMENTS + 2

class Cylinder (private val center: Geometry.Point, private val radius: Float, val height: Float ) {

    private val vertexData = FloatArray(NUMBER_VERTICES * POSITION_COMPONENT_COUNT )

    private lateinit var vertexArray: VertexArray
    private val plane = Geometry.Plane(Geometry.Point(0f, 0f, 0f), Geometry.Vector(0f, 1f, 0f))

    init {
        // fill in the float data
        var offset = 0

        // centre of top triangle fan
        vertexData[ offset++ ] = center.x
        vertexData[ offset++ ] = center.y + 0.01f
        vertexData[ offset++ ] = center.z

        // points around top triangle fan
        for (i in 0..SEGMENTS) {
            val angleInRadians = (i.toFloat() / SEGMENTS.toFloat() * (Math.PI.toFloat() * 2f))
            vertexData[offset++] = (center.x + radius * cos(angleInRadians))
            vertexData[offset++] = center.y + 0.01f
            vertexData[offset++] = (center.z + radius * sin(angleInRadians))
        }

        // create the vertex array
        vertexArray = VertexArray( vertexData )
    }

    fun bindData(program: BaseShaderProgram) {
        vertexArray.setVertexAttribPointer(
            0,
            program.getPositionAttributeLocation(),
            POSITION_COMPONENT_COUNT, 0
        )
    }
    fun draw() {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, NUMBER_VERTICES)
    }


}