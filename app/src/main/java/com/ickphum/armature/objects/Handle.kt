package com.ickphum.armature.objects

import android.opengl.GLES20
import com.ickphum.armature.Constants
import com.ickphum.armature.data.VertexArray
import com.ickphum.armature.enum.Axis
import com.ickphum.armature.programs.CylinderShaderProgram
import com.ickphum.armature.util.Geometry

class Handle (var center: Geometry.Point, private val radius: Float ) {

    companion object {
        private const val TOTAL_COMPONENT_COUNT = Constants.POSITION_COMPONENT_COUNT + Constants.NORMAL_COMPONENT_COUNT
        private const val STRIDE = TOTAL_COMPONENT_COUNT * Constants.BYTES_PER_FLOAT
        private const val TAG = "Handle"

        // we want to generate points for three (one per axis) sets of handles. Each set
        // has top and bottom handles for individual changes.
        // Each handle is a circle centered on the top or bottom point and perpendicular to the
        // axis it applies to; an axis handle allows adjustment of the coords for the 2 other axes.
        // We generate all 3 sets so we can change axis (and thus the displayed handles) without
        // recalculating; we just display the handles for the current axis.
        private const val HANDLE_SEGMENTS = 24
        private const val NUMBER_HANDLE_VERTICES = HANDLE_SEGMENTS + 2

        private var classIndex = 0
        fun nextIndex() = classIndex++
        fun resetIndex(index : Int ) = run { classIndex = index }
    }

    enum class HandleElement {
        X {
            override fun axis() = Axis.X
        },
        Y {
            override fun axis() = Axis.Y
        },
        Z {
            override fun axis() = Axis.Z
        };

        abstract fun axis(): Axis
    }

    data class HandleTouch(
        val handle: Handle,
        val point: Geometry.Point,
        val element: HandleElement
    )

    private var id = nextIndex()

    private val numberVertices = NUMBER_HANDLE_VERTICES * 3

    private val vertexData = FloatArray(numberVertices * TOTAL_COMPONENT_COUNT)
    private lateinit var vertexArray: VertexArray

    // handle touch detection
    data class HandlePlane(
        val plane: Geometry.Plane,
        val center: Geometry.Point,
        val element: HandleElement
    )
    private var handlePlanes = listOf<HandlePlane>()

    override fun toString() = "Handle $id"

    init {
        generateVertices()
    }

    private fun generateVertices() {

        Geometry.addCircleData( vertexData, 0, center, radius, HANDLE_SEGMENTS, Axis.X )
        Geometry.addCircleData( vertexData, NUMBER_HANDLE_VERTICES * TOTAL_COMPONENT_COUNT, center, radius, HANDLE_SEGMENTS, Axis.Y )
        Geometry.addCircleData( vertexData, NUMBER_HANDLE_VERTICES * TOTAL_COMPONENT_COUNT * 2, center, radius, HANDLE_SEGMENTS, Axis.Z )

        vertexArray = VertexArray(vertexData)

        handlePlanes = listOf(
            HandlePlane(Geometry.Plane( center, Geometry.Vector(1f, 0f, 0f )), center, HandleElement.X),
            HandlePlane(Geometry.Plane( center, Geometry.Vector(0f, 1f, 0f )), center, HandleElement.Y),
            HandlePlane(Geometry.Plane( center, Geometry.Vector(0f, 0f, 1f )), center, HandleElement.Z),
        )
    }

    fun bindData() {
        vertexArray.setVertexAttribPointer(0,0, Constants.POSITION_COMPONENT_COUNT, STRIDE )
        vertexArray.setVertexAttribPointer( Constants.POSITION_COMPONENT_COUNT,1, Constants.NORMAL_COMPONENT_COUNT, STRIDE )
    }

    fun draw(program: CylinderShaderProgram, color: FloatArray ) {

        program.setColorUniform(color)

        for (i in 0..2)
            GLES20.glDrawArrays( GLES20.GL_TRIANGLE_FAN, NUMBER_HANDLE_VERTICES * i, NUMBER_HANDLE_VERTICES )
    }

    fun findIntersectionPoint(ray: Geometry.Ray, modelViewMatrix: FloatArray): HandleTouch? {

        val intersections = mutableListOf<HandleTouch>()

        for (handlePlane in handlePlanes) {
            val point: Geometry.Point? = Geometry.intersectionPoint(ray, handlePlane.plane)
            if (point != null) {
                val vectorToCenter = Geometry.vectorBetween(point, handlePlane.center)
                if (vectorToCenter.length() < radius) {
                    intersections.add(HandleTouch(this, point, handlePlane.element))
                }
            }
        }

        var maxZ : Float? = null
        var closestIntersection : HandleTouch? = null

        for ( intersection in intersections ) {
            val newMax = Geometry.compareTouchedPoint( intersection.point, maxZ, modelViewMatrix )

            if ( newMax != null ) {
                maxZ = newMax;
                closestIntersection = intersection
            }
        }

        return closestIntersection
    }

    fun changePosition(delta: Geometry.Vector) {
        center = center.add( delta )
        generateVertices()
        vertexArray.updateBuffer(vertexData, 0, numberVertices * TOTAL_COMPONENT_COUNT)
    }

}