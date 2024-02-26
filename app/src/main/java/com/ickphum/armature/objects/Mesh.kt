package com.ickphum.armature.objects

import android.opengl.GLES20
import com.ickphum.armature.Constants.POSITION_COMPONENT_COUNT
import com.ickphum.armature.data.VertexArray
import com.ickphum.armature.enum.Axis
import com.ickphum.armature.programs.MeshShaderProgram
import com.ickphum.armature.util.Geometry
import kotlin.math.floor

class Mesh (private val size: Float, private val axis: Axis, private var position: Float ){
    companion object {
        private const val TAG = "Mesh"
        private const val MESH_CORNERS = 4
    }

    private var snapGridSize = 0.5f
    private var snapGridDim = 4
    var snapToGrid = true

    private val vertexData = FloatArray( ( MESH_CORNERS + snapGridDim * snapGridDim ) * POSITION_COMPONENT_COUNT )
    private lateinit var vertexArray: VertexArray
    private lateinit var plane: Geometry.Plane
    private var snapPoints = mutableListOf<Geometry.Point>( )

    init {
        val corners = mutableListOf<IntArray>(
            intArrayOf( -1, -1),
            intArrayOf( -1, 1),
            intArrayOf( 1, -1),
            intArrayOf( 1, 1),
        )
        val otherAxis = axis.otherAxes()
        for ( p in 0 until MESH_CORNERS ) {
            val factors = corners[ p ]
            for ( i in 0 .. 1 )
                vertexData[ p * POSITION_COMPONENT_COUNT + otherAxis[ i ] ] = factors[ i ] * size
            vertexData[ p * POSITION_COMPONENT_COUNT + axis.axis() ] = position

            // raised axes (x and z) shift up to start at 0
            if ( axis != Axis.Y )
                vertexData[ p * POSITION_COMPONENT_COUNT + 1 ] += size
        }

        calcSnapPoints( Geometry.Point(1.2f, 0f, 0.6f ))

        vertexArray = VertexArray( vertexData )

        val planePointData = floatArrayOf( 0f, 0f, 0f )
        planePointData[ axis.axis() ] = position
        val planeVectorData = floatArrayOf( 0f, 0f, 0f )
        planeVectorData[ axis.axis() ] = 1f
        plane = Geometry.Plane( Geometry.Point( planePointData, 0 ), Geometry.Vector(planeVectorData, 0))

    }

    fun calcSnapPoints( reference: Geometry.Point ) {
        snapPoints.clear()

        // we want to find the grid point immediately toward the origin from the reference point.
        // we'll convert to a scale where the grid size is 1, then we can use floor()
        // to find the correct grid line, then reverse the scale conversion
        val otherAxis = axis.otherAxes()
        val gridScale = 1.0f / snapGridSize

        // we take one grid span off the floor so we can show a 3x3 grid around the reference point
        val axis1Floor = floor( reference.asArray()[ otherAxis[ 0 ]] * gridScale ) / gridScale - snapGridSize
        val axis2Floor = floor( reference.asArray()[ otherAxis[ 1 ]] * gridScale ) / gridScale - snapGridSize
//        Log.d( TAG, "axis1Floor $axis1Floor")
//        Log.d( TAG, "axis2Floor $axis2Floor")

        val pointData = floatArrayOf( 0f, 0f, 0f )
        pointData[ axis.axis() ] = position

        var offset = MESH_CORNERS * POSITION_COMPONENT_COUNT
        for ( axis1 in 0 until snapGridDim ) {
            pointData[ otherAxis[ 0 ]] = axis1Floor + axis1 * snapGridSize
            for ( axis2 in 0 until snapGridDim ) {
                pointData[ otherAxis[ 1 ]] = axis2Floor + axis2 * snapGridSize

                snapPoints.add(Geometry.Point( pointData , 0))

                vertexData[ offset++ ] = pointData[ 0 ]
                vertexData[ offset++ ] = pointData[ 1 ]
                vertexData[ offset++ ] = pointData[ 2 ]
            }
        }

    }

    private fun recalcSnapPoints(point: Geometry.Point ) {
        calcSnapPoints( point )
        vertexArray.updateBuffer(vertexData, MESH_CORNERS * POSITION_COMPONENT_COUNT,
            snapGridDim * snapGridDim * POSITION_COMPONENT_COUNT, )
    }

    fun bindData(program: MeshShaderProgram) {
        vertexArray.setVertexAttribPointer(
            0,
            0,
            POSITION_COMPONENT_COUNT, 0
        )
    }
    fun draw() {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, MESH_CORNERS)
        GLES20.glDrawArrays(GLES20.GL_POINTS, MESH_CORNERS, snapGridDim * snapGridDim )
    }

    fun findIntersectionPoint( ray: Geometry.Ray): Geometry.Point? {
        val touchedPoint: Geometry.Point? = Geometry.intersectionPoint(ray, plane)
        if ( touchedPoint != null && touchedPoint.x >= -size && touchedPoint.x <= size
            && touchedPoint.z >= -size && touchedPoint.z <= size)
        {
            recalcSnapPoints( touchedPoint )
            return touchedPoint
        }
        return null
    }

    fun nearestSnapPoint(effectivePoint: Geometry.Point): Geometry.Point? {
        if ( !snapToGrid )
            return effectivePoint

        var closestIndex : Int = -1
        var closestDistance = 0f;
        for ( i in 0 until snapPoints.size) {
            val d = snapPoints[ i ].subtract( effectivePoint ).asVector().length()
            if ( closestIndex < 0 || d < closestDistance) {
                closestIndex = i
                closestDistance = d
            }
        }
        return if ( closestIndex >= 0 ) snapPoints[ closestIndex ] else null
    }
}