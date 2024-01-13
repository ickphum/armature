package com.ickphum.armature.objects

import android.opengl.GLES20
import android.util.Log
import com.ickphum.armature.data.VertexArray
import com.ickphum.armature.util.Geometry
import kotlin.math.cos
import kotlin.math.sin
import com.ickphum.armature.Constants.BYTES_PER_FLOAT
import com.ickphum.armature.programs.CylinderShaderProgram
import com.ickphum.armature.util.Logging.Helper.dumpArray


class Cylinder (private val center: Geometry.Point, private val radius: Float, private var height: Float ) {
    companion object {
        private const val POSITION_COMPONENT_COUNT = 3
        private const val NORMAL_COMPONENT_COUNT = 3
        private const val TOTAL_COMPONENT_COUNT = POSITION_COMPONENT_COUNT + NORMAL_COMPONENT_COUNT
        private const val STRIDE = TOTAL_COMPONENT_COUNT * BYTES_PER_FLOAT
        private const val SEGMENTS = 12
        private const val TAG = "Cylinder"

        // the fan and the strip both go around the circle and repeat the start,
        // hence the plus 1; 1 vertex per segment for the fan, 2 for the strip.
        // Final +1 for the fan center
        private const val NUMBER_FAN_VERTICES = SEGMENTS + 2
        private const val NUMBER_STRIP_VERTICES = ( SEGMENTS + 1 ) * 2
        private const val NUMBER_VERTICES = NUMBER_FAN_VERTICES + NUMBER_STRIP_VERTICES

        // strip offset into vertex data
        private const val FAN_VERTEX_OFFSET = NUMBER_FAN_VERTICES * TOTAL_COMPONENT_COUNT;
    }

    private val vertexData = FloatArray(NUMBER_VERTICES * TOTAL_COMPONENT_COUNT )
    private var vertexArray: VertexArray

    private val plane = Geometry.Plane(Geometry.Point(0f, 0f, 0f), Geometry.Vector(0f, 1f, 0f))

    private val triangleData = floatArrayOf(

        0f, 1f, 0.0f,   0.5f, 0.5f, -0.5f,
        1f, 0f, 0.0f,   0.5f, 0.5f, -0.5f,
        0f, 0f, -1f,    0.5f, 0.5f, -0.5f,

        0f, 1f, 0f,     -0.5f, 0.5f, -0.5f,
        0f, 0f, -1f,    -0.5f, 0.5f, -0.5f,
        -1f, 0f, 0f,    -0.5f, 0.5f, -0.5f,

        0f, 1f, 0f,     -0.5f, 0.5f, 0.5f,
        -1f, 0f, 0f,    -0.5f, 0.5f, 0.5f,
        0f, 0f, 1f,     -0.5f, 0.5f, 0.5f,

        0f, 1f, 0f,     0.5f, 0.5f, 0.5f,
        0f, 0f, 1f,     0.5f, 0.5f, 0.5f,
        1f, 0f, 0f,     0.5f, 0.5f, 0.5f,
//
//        0f, 1f, 0.0f,   0f, 0f, -1f,
//        1f, 0f, 0.0f,   0f, 0f, 1f,
//        0f, 0f, -1f,    0f, 0f, -1f,
//
//        0f, 1f, 0f,     -1f, 0f, 0f,
//        0f, 0f, -1f,    -1f, 0f, 0f,
//        -1f, 0f, 0f,    -1f, 0f, 0f,
//
//        0f, 1f, 0f,     1f, 0f, 0f,
//        -1f, 0f, 0f,    1f, 0f, 0f,
//        0f, 0f, 1f,     1f, 0f, 0f,
//
//        0f, 1f, 0f,     0f, 0f, 1f,
//        0f, 0f, 1f,     0f, 0f, 1f,
//        1f, 0f, 0f,     0f, 0f, 1f,

        )
    private var triangleArray = VertexArray( triangleData )

    init {
        // fill in the float data
        var offset = 0
        var stripOffset = FAN_VERTEX_OFFSET

        // centre of top triangle fan
        vertexData[ offset++ ] = center.x
        vertexData[ offset++ ] = center.y + height
        vertexData[ offset++ ] = center.z

        // needed for normal calc
        val centerPoint = Geometry.Point(vertexData, 0)

        // all top normals are straight up
        vertexData[ offset++ ] = 0f
        vertexData[ offset++ ] = 1f
        vertexData[ offset++ ] = 0f

        // points around top triangle fan
        for (i in 0..SEGMENTS) {
            val angleInRadians = (i.toFloat() / SEGMENTS.toFloat() * (Math.PI.toFloat() * 2f))
            vertexData[offset++] = (center.x + radius * cos(angleInRadians))
            vertexData[offset++] = center.y + height
            vertexData[offset++] = (center.z + radius * sin(angleInRadians))

            // top normal
            vertexData[ offset++ ] = 0f
            vertexData[ offset++ ] = 1f
            vertexData[ offset++ ] = 0f

            // fill in the side strip at the same time; we know the number of points
            // in the top fan, and we just start at the end of those.
            // the top side of the strip is exactly the same as the fan points;
            // we could draw via index to save the duplication.

            // top
            vertexData[ stripOffset++ ] = vertexData[ offset - 3 - NORMAL_COMPONENT_COUNT ];
            vertexData[ stripOffset++ ] = vertexData[ offset - 2 - NORMAL_COMPONENT_COUNT ];
            vertexData[ stripOffset++ ] = vertexData[ offset - 1 - NORMAL_COMPONENT_COUNT ];

            // top strip normal; unit vector from center to vertex
            val vertexPoint = Geometry.Point( vertexData, offset - 6 )
            val centerToVertex = Geometry.vectorBetween( centerPoint, vertexPoint ).normalize()
//            Log.d( TAG, "centerToVertex $centerPoint -> $vertexPoint = $centerToVertex")

            vertexData[ stripOffset++ ] = centerToVertex.x
            vertexData[ stripOffset++ ] = centerToVertex.y
            vertexData[ stripOffset++ ] = centerToVertex.z

            // bottom
            vertexData[ stripOffset++ ] = vertexData[ offset - 3 - NORMAL_COMPONENT_COUNT ];
            vertexData[ stripOffset++ ] = vertexData[ offset - 2 - NORMAL_COMPONENT_COUNT ] - height;
            vertexData[ stripOffset++ ] = vertexData[ offset - 1 - NORMAL_COMPONENT_COUNT ];

            // bottom normals (same as top)
            vertexData[ stripOffset++ ] = centerToVertex.x
            vertexData[ stripOffset++ ] = centerToVertex.y
            vertexData[ stripOffset++ ] = centerToVertex.z
        }

        Log.d( TAG, "dump")
        dumpArray( TAG, vertexData, 3)
        Log.d( TAG, "dump2")
        // create the vertex array
        vertexArray = VertexArray( vertexData )

    }

    fun bindData(program: CylinderShaderProgram) {
        vertexArray.setVertexAttribPointer(
            0,
            program.getPositionAttributeLocation(),
            POSITION_COMPONENT_COUNT, STRIDE
        )
        vertexArray.setVertexAttribPointer(
            POSITION_COMPONENT_COUNT,
            program.getNormalAttributeLocation(),
            NORMAL_COMPONENT_COUNT, STRIDE
        )
//        triangleArray.setVertexAttribPointer(
//            0,
//            program.getPositionAttributeLocation(),
//            POSITION_COMPONENT_COUNT, STRIDE
//        )
//        triangleArray.setVertexAttribPointer(
//            POSITION_COMPONENT_COUNT,
//            program.getNormalAttributeLocation(),
//            NORMAL_COMPONENT_COUNT, STRIDE
//        )
    }

    fun draw() {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, NUMBER_FAN_VERTICES)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, NUMBER_FAN_VERTICES, NUMBER_STRIP_VERTICES)
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 12 )
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
            vertexData[ ( i + 1 ) * TOTAL_COMPONENT_COUNT + 1 ] += safeDelta
            vertexData[ FAN_VERTEX_OFFSET + i * TOTAL_COMPONENT_COUNT * 2 + 1 ] += safeDelta
        }
        height += safeDelta
        vertexArray.updateBuffer( vertexData, 0, NUMBER_VERTICES * TOTAL_COMPONENT_COUNT)
    }

}