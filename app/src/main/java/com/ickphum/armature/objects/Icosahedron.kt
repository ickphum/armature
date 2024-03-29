package com.ickphum.armature.objects

import android.opengl.GLES20
import android.util.Log
import com.ickphum.armature.Constants
import com.ickphum.armature.data.VertexArray
import com.ickphum.armature.programs.CylinderShaderProgram
import com.ickphum.armature.util.Geometry
import glm_.glm.PIf
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin

class Icosahedron (private var center: Geometry.Vector, private val radius : Float ) {

    data class IcosahedronTouch(
        val cylinder: Icosahedron,
        val point: Geometry.Point,
    )

    companion object {
        private val H_ANGLE = PIf / 180f * 72f;    // 72 degree = 360 / 5
        private val V_ANGLE = atan(1.0f / 2);  // elevation = 26.565 degree
        private const val TOTAL_COMPONENT_COUNT =
            Constants.POSITION_COMPONENT_COUNT + Constants.NORMAL_COMPONENT_COUNT
        private const val STRIDE = TOTAL_COMPONENT_COUNT * Constants.BYTES_PER_FLOAT
        private const val TAG = "Icosahedron"
        private const val NUMBER_VERTICES = 20 * 3
    }

    private var previousBottomOffset = Geometry.Vector( 0f, 0f, 0f )
    private var previousTopOffset = Geometry.Vector( 0f, 0f, 0f )

    private val handleRadius = radius * 3f

    // 20 triangular sides

    private val vertexData = FloatArray(NUMBER_VERTICES * Icosahedron.TOTAL_COMPONENT_COUNT)

    private lateinit var vertexArray: VertexArray

    data class HandlePlane(
        val plane: Geometry.Plane,
        val center: Geometry.Point,
    )

    private var handlePlanes = listOf<HandlePlane>()

    var selected = true

    init {
        generateVertices()
    }

    private fun dumpArrayTriplets(array: FloatArray, offset: Int, stride: Int, count: Int, label: String = "dump" )
    {
        for ( i in 0 until count ) {
            Log.d(Icosahedron.TAG, "$label ${offset + i} ${array[ offset + (i * stride)]}, ${array[ offset + (i * stride) + 1 ]}, ${array[ offset + (i * stride) + 2]}")
        }
    }

    private fun generateVertices() {
        // constants


        var hAngle1 = -PIf / 2 - H_ANGLE / 2;  // start from -126 deg at 1st row
        var hAngle2 = -PIf / 2;                // start from -90 deg at 2nd row

        val top = Geometry.Vector(0f, 0f, radius).add( center )
        val topRow = mutableListOf<Geometry.Vector>( )
        val bottomRow = mutableListOf<Geometry.Vector>( )

        // compute 10 vertexData at 1st and 2nd rows
        for( i in 0 .. 4 )
        {
            val z = radius * sin(V_ANGLE);              // elevation
            val xy = radius * cos(V_ANGLE);             // length on XY plane

            topRow.add( Geometry.Vector(
                xy * cos(hAngle1),
                xy * sin(hAngle1),
                z
            ).add( center ))

            bottomRow.add( Geometry.Vector(
                xy * cos(hAngle2),
                xy * sin(hAngle2),
                -z
            ).add( center ))

            // next horizontal angles
            hAngle1 += H_ANGLE;
            hAngle2 += H_ANGLE;
        }

        // the last bottom vertex at (0, 0, -r)
        val bottom = Geometry.Vector(0f, 0f, -radius).add( center )

        val triangleFloats = 3 * TOTAL_COMPONENT_COUNT
        var offset = 0

        for (i in 0 until 5 ) {

            val nextI = if ( i < 4 ) i+1 else 0

            Geometry.Triangle(top, topRow[ nextI ], topRow[i] )
                .writeToArray(vertexData, offset++ * triangleFloats )

            Geometry.Triangle(topRow[i], topRow[ nextI ], bottomRow[i] )
                .writeToArray(vertexData, offset++ * triangleFloats)

            Geometry.Triangle(bottomRow[i], topRow[ nextI ], bottomRow[ nextI ] )
                .writeToArray(vertexData, offset++ * triangleFloats )

            Geometry.Triangle(bottom, bottomRow[ i ], bottomRow[ nextI ])
                .writeToArray(vertexData, offset++ * triangleFloats )
        }

        vertexArray = VertexArray(vertexData)

    }

    fun bindData() {
        vertexArray.setVertexAttribPointer(
            0,
            0,
            Constants.POSITION_COMPONENT_COUNT, Icosahedron.STRIDE
        )
        vertexArray.setVertexAttribPointer(
            Constants.POSITION_COMPONENT_COUNT,
            1,
            Constants.NORMAL_COMPONENT_COUNT, Icosahedron.STRIDE
        )
    }

    fun draw(cylinderProgram: CylinderShaderProgram, color: FloatArray) {
        cylinderProgram.setColorUniform( color )
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 20 * 3 )
    }

    fun changePosition(delta: Geometry.Vector) {
        center = center.add( delta )
        generateVertices()
        vertexArray.updateBuffer(vertexData, 0, NUMBER_VERTICES * TOTAL_COMPONENT_COUNT)
    }

}