package com.ickphum.armature.objects

import android.opengl.GLES20
import android.util.Log
import com.ickphum.armature.data.VertexArray
import com.ickphum.armature.programs.BaseShaderProgram
import com.ickphum.armature.util.Geometry
import kotlin.math.cos
import kotlin.math.sin

private const val TAG = "Cylinder"

class Cylinder (private val center: Geometry.Point, private val radius: Float, private var height: Float ) {
    companion object {
        private const val POSITION_COMPONENT_COUNT = 3
        private const val SEGMENTS = 12

        // the fan and the strip both go around the circle and repeat the start,
        // hence the plus 1; 1 vertex per segment for the fan, 2 for the strip.
        // Final +1 for the fan center
        private const val NUMBER_FAN_VERTICES = SEGMENTS + 2
        private const val NUMBER_STRIP_VERTICES = ( SEGMENTS + 1 ) * 2
        private const val NUMBER_VERTICES = NUMBER_FAN_VERTICES + NUMBER_STRIP_VERTICES

        // strip offset into vertex data
        private const val FAN_VERTEX_OFFSET = NUMBER_FAN_VERTICES * POSITION_COMPONENT_COUNT;
    }

    private val vertexData = FloatArray(NUMBER_VERTICES * POSITION_COMPONENT_COUNT )

    private lateinit var vertexArray: VertexArray
    private val plane = Geometry.Plane(Geometry.Point(0f, 0f, 0f), Geometry.Vector(0f, 1f, 0f))

    init {
        // fill in the float data
        var offset = 0

        // centre of top triangle fan
        vertexData[ offset++ ] = center.x
        vertexData[ offset++ ] = center.y + height
        vertexData[ offset++ ] = center.z

        // points around top triangle fan
        for (i in 0..SEGMENTS) {
            val angleInRadians = (i.toFloat() / SEGMENTS.toFloat() * (Math.PI.toFloat() * 2f))
            vertexData[offset++] = (center.x + radius * cos(angleInRadians))
            vertexData[offset++] = center.y + height
            vertexData[offset++] = (center.z + radius * sin(angleInRadians))

            // fill in the side strip at the same time; we know the number of points
            // in the top fan, and we just start at the end of those.
            // the top side of the strip is exactly the same as the fan points;
            // we could draw via index to save the duplication.

            // top
            vertexData[offset - 6 + FAN_VERTEX_OFFSET + i * 3 ] = vertexData[ offset - 3];
            vertexData[offset - 5 + FAN_VERTEX_OFFSET + i * 3  ] = vertexData[ offset - 2];
            vertexData[offset - 4 + FAN_VERTEX_OFFSET + i * 3  ] = vertexData[ offset - 1];

            // bottom
            vertexData[offset - 3 + FAN_VERTEX_OFFSET + i * 3  ] = vertexData[ offset - 3];
            vertexData[offset - 2 + FAN_VERTEX_OFFSET + i * 3  ] = vertexData[ offset - 2] - height;
            vertexData[offset - 1 + FAN_VERTEX_OFFSET + i * 3  ] = vertexData[ offset - 1];
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
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, NUMBER_FAN_VERTICES)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, NUMBER_FAN_VERTICES, NUMBER_STRIP_VERTICES)
    }

    fun changeHeight(delta: Float) {
        val safeDelta = if ( height + delta <= 0f ) 0.01f - height else delta

        // we want to change all the vertices that rely on height, ie the fan
        // and the top of the strip. It would be more efficient to change the bottom
        // of the strip and always set the vertices with the top at 0, and then move it
        // up by the height in the shader.
        Log.d( TAG, "changeHeight $height $delta $safeDelta")
        vertexData[ 1 ] += safeDelta
        for (i in 0..SEGMENTS) {
            vertexData[ i * POSITION_COMPONENT_COUNT + 4 ] += safeDelta
            vertexData[ i * POSITION_COMPONENT_COUNT * 2 + 1 + FAN_VERTEX_OFFSET ] += safeDelta
        }
        height += safeDelta
        vertexArray.updateBuffer( vertexData, 0, NUMBER_VERTICES * POSITION_COMPONENT_COUNT)
    }

}