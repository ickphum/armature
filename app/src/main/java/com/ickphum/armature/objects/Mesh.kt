package com.ickphum.armature.objects

import android.opengl.GLES20
import com.ickphum.armature.Constants.POSITION_COMPONENT_COUNT
import com.ickphum.armature.data.VertexArray
import com.ickphum.armature.enum.Axis
import com.ickphum.armature.programs.MeshShaderProgram
import com.ickphum.armature.util.Geometry


class Mesh (val size: Float, axis: Axis, position: Float ){
    companion object {
        private const val TAG = "Mesh"
        private const val MESH_CORNERS = 4
    }
    private lateinit var vertexArray: VertexArray
    private lateinit var plane: Geometry.Plane

    init {
        val corners = mutableListOf<IntArray>(
            intArrayOf( -1, -1),
            intArrayOf( -1, 1),
            intArrayOf( 1, -1),
            intArrayOf( 1, 1),
        )
        val vertexData = FloatArray( MESH_CORNERS * POSITION_COMPONENT_COUNT )
        val otherAxis = axis.otherAxes()
        for ( p in 0.until( MESH_CORNERS )) {
            val factors = corners[ p ]
            for ( i in 0 .. 1 )
                vertexData[ p * POSITION_COMPONENT_COUNT + otherAxis[ i ] ] = factors[ i ] * size
            vertexData[ p * POSITION_COMPONENT_COUNT + axis.axis() ] = position

            // raised axes (x and z) shift up to start at 0
            if ( axis != Axis.Y )
                vertexData[ p * POSITION_COMPONENT_COUNT + 1 ] += size
        }
        vertexArray = VertexArray( vertexData )

        val planePointData = floatArrayOf( 0f, 0f, 0f )
        planePointData[ axis.axis() ] = position
        val planeVectorData = floatArrayOf( 0f, 0f, 0f )
        planeVectorData[ axis.axis() ] = 1f
        plane = Geometry.Plane( Geometry.Point( planePointData, 0 ), Geometry.Vector(planeVectorData, 0))

    }

    fun bindData(program: MeshShaderProgram) {
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