package com.ickphum.armature.objects

import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.ickphum.armature.Constants.BYTES_PER_FLOAT
import com.ickphum.armature.Constants.NORMAL_COMPONENT_COUNT
import com.ickphum.armature.Constants.POSITION_COMPONENT_COUNT
import com.ickphum.armature.Renderer
import com.ickphum.armature.Renderer.State
import com.ickphum.armature.data.VertexArray
import com.ickphum.armature.enum.Axis
import com.ickphum.armature.programs.CylinderShaderProgram
import com.ickphum.armature.util.Geometry
import com.ickphum.armature.util.Geometry.Helper.vectorBetween
import glm_.glm.angle
import glm_.glm.angleAxis
import glm_.vec3.Vec3
import kotlin.math.cos
import kotlin.math.sin

class Cylinder (var bottomCenter: Geometry.Point, var topCenter: Geometry.Point, private val radius: Float ) {

    enum class CylinderElement {
        BODY {
            override fun axis() = null
            override fun top() = true
            override fun bottom() = true
        },
        BOTTOM_X {
            override fun axis() = Axis.X
            override fun top() = false
            override fun bottom() = true
        },
        BOTTOM_Y {
            override fun axis() = Axis.Y
            override fun top() = false
            override fun bottom() = true
        },
        BOTTOM_Z {
            override fun axis() = Axis.Z
            override fun top() = false
            override fun bottom() = true
        },
        TOP_X {
            override fun axis() = Axis.X
            override fun top() = true
            override fun bottom() = false
        },
        TOP_Y {
            override fun axis() = Axis.Y
            override fun top() = true
            override fun bottom() = false
        },
        TOP_Z {
            override fun axis() = Axis.Z
            override fun top() = true
            override fun bottom() = false
        };

        abstract fun axis(): Axis?
        abstract fun top(): Boolean
        abstract fun bottom(): Boolean
    }

    data class CylinderTouch(
        val cylinder: Cylinder,
        val point: Geometry.Point,
        val element: CylinderElement
    )

    companion object {
        private const val TOTAL_COMPONENT_COUNT = POSITION_COMPONENT_COUNT + NORMAL_COMPONENT_COUNT
        private const val STRIDE = TOTAL_COMPONENT_COUNT * BYTES_PER_FLOAT
        private const val SEGMENTS = 12
        private const val TAG = "Cylinder"
        private const val triangleMode = true

        // the fan and the strip both go around the circle and repeat the start,
        // hence the plus 1; 1 vertex per segment for the fan, 2 for the strip.
        // Final +1 for the fan center
        private const val NUMBER_END_FAN_VERTICES = SEGMENTS + 2
        private const val NUMBER_FAN_VERTICES = NUMBER_END_FAN_VERTICES * 2

        // cylinder side offset into vertex data
        private const val SIDE_VERTEX_OFFSET = NUMBER_FAN_VERTICES * TOTAL_COMPONENT_COUNT;

        // handle offset into vertex data
        private const val HANDLE_VERTEX_OFFSET =
            SIDE_VERTEX_OFFSET + SEGMENTS * 2 * 3 * TOTAL_COMPONENT_COUNT

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

    var index = Cylinder.nextIndex()

    private var previousBottomOffset = Geometry.Vector( 0f, 0f, 0f )
    private var previousTopOffset = Geometry.Vector( 0f, 0f, 0f )

    // we draw the cylinder vertically to the height matching the distance between top
    // bottom, then rotate it so that lines up with the actual topCenter
    private var height = 0f
    private lateinit var verticalCenter: Geometry.Point

    private val handleRadius = radius * 3f

    private val numberSideVertices = if (triangleMode) SEGMENTS * 6 else (SEGMENTS + 1) * 2
    private val numberVertices = NUMBER_FAN_VERTICES + numberSideVertices + NUMBER_HANDLE_VERTICES * 6

    private val vertexData = FloatArray(numberVertices * TOTAL_COMPONENT_COUNT)
    private lateinit var vertexArray: VertexArray

    private val singleColor = floatArrayOf(0.5f, 0.9f, 0.5f, 1f)
    private val groupColor = floatArrayOf(0.97f, 0.53f, 0.08f, 1f)
    private val normalColor = floatArrayOf(0.7f, 0.1f, 0.1f, 1f)
    private val handleColor = floatArrayOf(0.5f, 0.1f, 0.5f, 1f)

    // these rectangles are used for touch detection
    private lateinit var xyRectangle: Geometry.Rectangle
    private lateinit var zyRectangle: Geometry.Rectangle

    // handle touch detection
    private lateinit var centerXPlane: Geometry.Plane
    private lateinit var centerYPlane: Geometry.Plane
    private lateinit var centerZPlane: Geometry.Plane
    private lateinit var topCenterYPlane: Geometry.Plane
    private lateinit var topCenterZPlane: Geometry.Plane
    private lateinit var topCenterXPlane: Geometry.Plane

    data class HandlePlane(
        val plane: Geometry.Plane,
        val center: Geometry.Point,
        val element: CylinderElement
    )

    override fun toString() = "Cylinder $index"

    private var handlePlanes = listOf<HandlePlane>()

    var selected = true

    init {
        generateVertices()
    }

    private fun dumpArrayTriplets(array: FloatArray, offset: Int, stride: Int, count: Int, label: String )
    {
        for ( i in 0 until count ) {
            Log.d( TAG, "$label ${offset + i} ${array[ offset + (i * stride)]}, ${array[ offset + (i * stride) + 1 ]}, ${array[ offset + (i * stride) + 2]}")
        }
    }
    private fun generateVertices() {

        height = vectorBetween( bottomCenter, topCenter).length()
        verticalCenter = bottomCenter.translateY( height )

        // top circle follows the bottom circle and we do the sides at the same time
        var offset = NUMBER_END_FAN_VERTICES * TOTAL_COMPONENT_COUNT
        var sideOffset = SIDE_VERTEX_OFFSET

        // fill in the bottom fan first so both end fans are calculated before we do the sides
        addCircleData( vertexData, 0, bottomCenter, radius, SEGMENTS, Axis.Y )

        addCircleData( vertexData, offset, verticalCenter, radius, SEGMENTS, Axis.Y )

        // needed for normal calc
        val centerPoint = Geometry.Point(vertexData, 0)

        // we skip the first vertex (and normal) in the top circle since it's the center point
        offset += TOTAL_COMPONENT_COUNT

        // points around top triangle fan
        for (i in 0..SEGMENTS) {

            offset += TOTAL_COMPONENT_COUNT

            if (triangleMode) {

                // triangles don't wrap around, so ignore the last fan vertex
                if (i < SEGMENTS) {
                    generateTrianglePoints(vertexData, offset, sideOffset )
                    sideOffset += 6 * TOTAL_COMPONENT_COUNT
                }
            } else {
                generateStripPoints(vertexData, offset, sideOffset, centerPoint)
                sideOffset += 2 * TOTAL_COMPONENT_COUNT
            }

        }

        // if the cylinder is meant to be vertical, we can skip all the rotation stuff
        if ( bottomCenter.findCongruency( topCenter ) != Renderer.Congruency.SAME_XZ ) {

            // we've generated vertices for a vertical cylinder based at bottomCenter which has
            // a height to reach topCenter; what we need to do is rotate the cylinder so the top of
            // it actually matches topCenter.
            // We're doing this now because the following points (handles) we don't actually want
            // rotated; we want the handles to remain on the true x/y/z axes.
            // Steps;
            // 1. Find the axis of rotation; this is the cross product of a vertical vector and the vector
            //      from the base to the top translated back to the base.
            // 2. Find the angle between these vectors
            // 3. Construct a quaternion based on the 2 above items
            // 4. For each 3-float component (vertex or normal) in the vertex data, apply the rotation
            //      using the quaternion and write it back into the vertex data. Vertex points need to
            //      be translated back relative to bottomCenter, normals do not.

            // 1
            val vertical = Geometry.Vector(0f, 1f, 0f)
            val shiftedTop = Geometry.Vector(topCenter).subtract(bottomCenter).normalize()
            val rotationAxis = vertical.crossProduct(shiftedTop).normalize()

            // 2
            val rotationAngle = angle(
                Vec3(vertical.x, vertical.y, vertical.z),
                Vec3(shiftedTop.x, shiftedTop.y, shiftedTop.z)
            )

            // 3
            val rotationQuat = angleAxis(rotationAngle, rotationAxis.x, rotationAxis.y, rotationAxis.z)

            // 4
            // each vertex in the object has a position and a normal; they both need rotation, but
            // the position needs to be shifted back by the base position, rotated, then shifted forward again.
            var vertexOffset = 0
            val baseVec3 = Vec3(bottomCenter.x, bottomCenter.y, bottomCenter.z)
            for (vertex in 0 until NUMBER_FAN_VERTICES + numberSideVertices) {

                val positionR = rotationQuat.times(Vec3(vertexData, vertexOffset).minus(baseVec3))
                    .plus(baseVec3)
                positionR.to(vertexData, vertexOffset)

                val normalR =
                    rotationQuat.times(Vec3(vertexData, vertexOffset + POSITION_COMPONENT_COUNT))
                normalR.to(vertexData, vertexOffset + POSITION_COMPONENT_COUNT)

                vertexOffset += TOTAL_COMPONENT_COUNT
            }
        }

        // add the handle data for all 6 handles

        addCircleData( vertexData, HANDLE_VERTEX_OFFSET, bottomCenter, handleRadius, HANDLE_SEGMENTS, Axis.X )
        addCircleData( vertexData, HANDLE_VERTEX_OFFSET + NUMBER_HANDLE_VERTICES * TOTAL_COMPONENT_COUNT, bottomCenter, handleRadius, HANDLE_SEGMENTS, Axis.Y )
        addCircleData( vertexData, HANDLE_VERTEX_OFFSET + NUMBER_HANDLE_VERTICES * TOTAL_COMPONENT_COUNT * 2, bottomCenter, handleRadius, HANDLE_SEGMENTS, Axis.Z )

        addCircleData( vertexData, HANDLE_VERTEX_OFFSET + NUMBER_HANDLE_VERTICES * TOTAL_COMPONENT_COUNT * 3, topCenter, handleRadius, HANDLE_SEGMENTS, Axis.X )
        addCircleData( vertexData, HANDLE_VERTEX_OFFSET + NUMBER_HANDLE_VERTICES * TOTAL_COMPONENT_COUNT * 4, topCenter, handleRadius, HANDLE_SEGMENTS, Axis.Y )
        addCircleData( vertexData, HANDLE_VERTEX_OFFSET + NUMBER_HANDLE_VERTICES * TOTAL_COMPONENT_COUNT * 5, topCenter, handleRadius, HANDLE_SEGMENTS, Axis.Z )

        // create the vertex array
        vertexArray = VertexArray(vertexData)

        // touch detection; we're creating 2 rectangles (each made up 2 triangles) running
        // along the axis of the cylinder at right angles to each other; that will intercept a majority
        // of clicks on the cylinder (very short cylinders will be slightly tricky).
        xyRectangle = Geometry.Rectangle(
            Geometry.Vector( topCenter.x - radius, topCenter.y, topCenter.z ),
            Geometry.Vector( bottomCenter.x - radius, bottomCenter.y, bottomCenter.z ),
            Geometry.Vector( bottomCenter.x + radius, bottomCenter.y, bottomCenter.z ),
            Geometry.Vector( topCenter.x + radius, topCenter.y, topCenter.z )
        )

        zyRectangle = Geometry.Rectangle(
            Geometry.Vector( topCenter.x, topCenter.y, topCenter.z - radius),
            Geometry.Vector( bottomCenter.x, bottomCenter.y, bottomCenter.z - radius),
            Geometry.Vector( bottomCenter.x, bottomCenter.y, bottomCenter.z + radius),
            Geometry.Vector( topCenter.x, topCenter.y, topCenter.z + radius),
        )

        centerXPlane = Geometry.Plane( bottomCenter, Geometry.Vector(1f, 0f, 0f ))
        centerYPlane = Geometry.Plane( bottomCenter, Geometry.Vector(0f, 1f, 0f ))
        centerZPlane = Geometry.Plane( bottomCenter, Geometry.Vector(0f, 0f, 1f ))
        topCenterXPlane = Geometry.Plane( topCenter, Geometry.Vector(1f, 0f, 0f ))
        topCenterYPlane = Geometry.Plane( topCenter, Geometry.Vector(0f, 1f, 0f ))
        topCenterZPlane = Geometry.Plane( topCenter, Geometry.Vector(0f, 0f, 1f ))

        handlePlanes = listOf(
            HandlePlane(centerXPlane, bottomCenter, CylinderElement.BOTTOM_X),
            HandlePlane(centerYPlane, bottomCenter, CylinderElement.BOTTOM_Y),
            HandlePlane(centerZPlane, bottomCenter, CylinderElement.BOTTOM_Z),
            HandlePlane(topCenterXPlane, topCenter, CylinderElement.TOP_X),
            HandlePlane(topCenterYPlane, topCenter, CylinderElement.TOP_Y),
            HandlePlane(topCenterZPlane, topCenter, CylinderElement.TOP_Z),
        )
    }

    // this is obviously similar to generateCirclePoint but is capable of generating circles
    // parallel to any axis, and we don't do anything about the sides. Also, the segment loop
    // is contained in here rather than the calling environment.
    private fun addCircleData(
        vertexData: FloatArray,
        initialOffset: Int,
        center: Geometry.Point,
        radius: Float,
        segments: Int,
        axis: Axis
    )
    {
        var offset = initialOffset
        val pointArray = FloatArray( 3 )
        val normalArray = floatArrayOf( 0f, 0f, 0f )
        val mainAxis = axis.axis()
        val otherAxes = axis.otherAxes()

        //        Log.d( TAG, "axis $mainAxis, otherAxes[0] ${otherAxes[0]}, otherAxes[1] ${otherAxes[1]}")

        // centre of triangle fan
        vertexData[offset++] = center.x
        vertexData[offset++] = center.y
        vertexData[offset++] = center.z

//        Log.d( TAG, "center fan ${vertexData[initialOffset + 0]}, ${vertexData[initialOffset + 1]}, ${vertexData[initialOffset + 2]}")

        // normal array is init to 0, just set the main axis for the center normal
        normalArray[ mainAxis ] = if ( initialOffset == 0 ) -1f else 1f // such a hack...
        vertexData[offset++] = normalArray[ 0 ]
        vertexData[offset++] = normalArray[ 1 ]
        vertexData[offset++] = normalArray[ 2 ]

//        Log.d( TAG, "normal array ${normalArray[ 0 ]}, ${normalArray[ 1 ]}, ${normalArray[ 2 ]}")
//        Log.d( TAG, "center normal ${vertexData[initialOffset + 3]}, ${vertexData[initialOffset + 4]}, ${vertexData[initialOffset + 5]}")

        for (i in 0..segments) {
            val angleInRadians = (i.toFloat() / segments.toFloat() * (Math.PI.toFloat() * 2f))

//            Log.d( TAG, "i $i, angleInRadians $angleInRadians")

            // generate the point as a float array using the axis; the main axis will always match
            // center, the other 2 axes will differ as cos and sin
            pointArray[ mainAxis ] = vertexData[ initialOffset + mainAxis ]
            pointArray[ otherAxes[0] ] = vertexData[ initialOffset + otherAxes[0] ] + radius * cos( angleInRadians )
            pointArray[ otherAxes[1] ] = vertexData[ initialOffset + otherAxes[1] ] + radius * sin( angleInRadians )
//            Log.d( TAG, "normal array ${pointArray[ 0 ]}, ${pointArray[ 1 ]}, ${pointArray[ 2 ]}")

            // add the point in xyz order, which is required of us by OpenGL (by default)
            vertexData[offset++] = pointArray[ 0 ]
            vertexData[offset++] = pointArray[ 1 ]
            vertexData[offset++] = pointArray[ 2 ]

            // all normals are the same
            vertexData[offset++] = normalArray[ 0 ]
            vertexData[offset++] = normalArray[ 1 ]
            vertexData[offset++] = normalArray[ 2 ]
        }
    }

    private fun generateStripPoints(
        vertexData: FloatArray,
        offset: Int,
        startSideOffset: Int,
        centerPoint: Geometry.Point
    ) {

        // fill in the side strip at the same time; we know the number of points
        // in the top fan, and we just start at the end of those.
        // the top side of the strip is exactly the same as the fan points;
        // we could draw via index to save the duplication.
        var sideOffset = startSideOffset

        // top
        vertexData[sideOffset++] = vertexData[offset - 3 - NORMAL_COMPONENT_COUNT];
        vertexData[sideOffset++] = vertexData[offset - 2 - NORMAL_COMPONENT_COUNT];
        vertexData[sideOffset++] = vertexData[offset - 1 - NORMAL_COMPONENT_COUNT];

        // top strip normal; unit vector from center to vertex
        val vertexPoint = Geometry.Point(vertexData, offset - 6)
        val centerToVertex = Geometry.vectorBetween(centerPoint, vertexPoint).normalize()

        vertexData[sideOffset++] = centerToVertex.x
        vertexData[sideOffset++] = centerToVertex.y
        vertexData[sideOffset++] = centerToVertex.z

        // bottom
        val height = 1f
        vertexData[sideOffset++] = vertexData[offset - 3 - NORMAL_COMPONENT_COUNT];
        vertexData[sideOffset++] = vertexData[offset - 2 - NORMAL_COMPONENT_COUNT] - height;
        vertexData[sideOffset++] = vertexData[offset - 1 - NORMAL_COMPONENT_COUNT];

        // bottom normals (same as top)
        vertexData[sideOffset++] = centerToVertex.x
        vertexData[sideOffset++] = centerToVertex.y
        vertexData[sideOffset++] = centerToVertex.z

    }

    private fun generateTrianglePoints(
        vertexData: FloatArray,
        offset: Int,
        startSideOffset: Int
    )
    {

        // instead of a triangle strip, we're generating separate triangles for each facet,
        // so that each facet can have its own normals and have flat shading.

        var sideOffset = startSideOffset

        val topCirclePoint1 = offset - 3 - NORMAL_COMPONENT_COUNT
        val bottomCirclePoint1 = topCirclePoint1 - NUMBER_END_FAN_VERTICES * TOTAL_COMPONENT_COUNT

        val rectangle = Geometry.Rectangle(
            Geometry.Vector(vertexData, topCirclePoint1),
            Geometry.Vector(vertexData, bottomCirclePoint1),
            Geometry.Vector(vertexData, bottomCirclePoint1 + TOTAL_COMPONENT_COUNT),
            Geometry.Vector(vertexData, offset)
        )

        rectangle.topTriangle.writeToArray(vertexData, sideOffset)
        sideOffset += 3 * TOTAL_COMPONENT_COUNT

        rectangle.bottomTriangle.writeToArray(vertexData, sideOffset)
    }

    fun bindData() {
        vertexArray.setVertexAttribPointer(
            0,
            0,
            POSITION_COMPONENT_COUNT, STRIDE
        )
        vertexArray.setVertexAttribPointer(
            POSITION_COMPONENT_COUNT,
            1,
            NORMAL_COMPONENT_COUNT, STRIDE
        )

    }

    fun draw(program: CylinderShaderProgram, state : State, preDragState: State) {

        program.setColorUniform(if (selected)
            if ( state == State.GROUP || ( state == State.PANNING && preDragState == State.GROUP ))
                groupColor
            else
                singleColor
        else normalColor)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, NUMBER_END_FAN_VERTICES)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, NUMBER_END_FAN_VERTICES, NUMBER_END_FAN_VERTICES)

        if (triangleMode)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, NUMBER_FAN_VERTICES, numberSideVertices)
        else
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, NUMBER_FAN_VERTICES, numberSideVertices)

        if ( selected && state != State.MOVE ) {
//            GLES20.glEnable(GLES20.GL_BLEND)
//            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE)

            program.setColorUniform(handleColor)

            for (i in 0..5)
                GLES20.glDrawArrays(
                    GLES20.GL_TRIANGLE_FAN,
                    NUMBER_FAN_VERTICES + numberSideVertices + NUMBER_HANDLE_VERTICES * i,
                    NUMBER_HANDLE_VERTICES
                )

            GLES20.glDisable(GLES20.GL_BLEND)
        }

    }

    fun findIntersectionPoint(ray: Geometry.Ray, modelViewMatrix: FloatArray): CylinderTouch? {

        val intersections = mutableListOf<CylinderTouch>()

        // we can register a touch to any visible handle or to the cylinder; closest point takes preference.
        var touchedPoint: Geometry.Point? = Geometry.intersectionPoint(ray, xyRectangle.plane)
        if ( touchedPoint != null )
        {
            if ( xyRectangle.topTriangle.pointInTriangle( touchedPoint ) || xyRectangle.bottomTriangle.pointInTriangle( touchedPoint ))
                intersections.add( CylinderTouch( this, touchedPoint, CylinderElement.BODY ) )
        }
        touchedPoint = Geometry.intersectionPoint(ray, zyRectangle.plane)
        if ( touchedPoint != null )
        {
            if ( zyRectangle.topTriangle.pointInTriangle( touchedPoint ) || zyRectangle.bottomTriangle.pointInTriangle( touchedPoint ))
                intersections.add( CylinderTouch( this, touchedPoint, CylinderElement.BODY ) )
        }

        if ( selected ) {

            for (handlePlane in handlePlanes) {
                val point: Geometry.Point? = Geometry.intersectionPoint(ray, handlePlane.plane)
                if (point != null) {
                    val vectorToCenter = vectorBetween(point, handlePlane.center)
                    if (vectorToCenter.length() < handleRadius) {
//                    Log.d(TAG, "Touched handle ${handlePlane.element}")
                        intersections.add(CylinderTouch(this, point, handlePlane.element))
                    }
                }
            }
        }

        var hitVec4: FloatArray
        val resultVec4 = FloatArray( 4 )
        var maxZ : Float? = null
        var closestIntersection : CylinderTouch? = null

        for ( intersection in intersections ) {
            hitVec4 = floatArrayOf( *intersection.point.asArray(), 1f )
            Matrix.multiplyMV(resultVec4, 0, modelViewMatrix, 0, hitVec4, 0)
            if ( maxZ == null || resultVec4[2] > maxZ ) {
                maxZ = resultVec4[2];
                closestIntersection = intersection
            }

        }

        return closestIntersection
    }

    fun changePosition(topOffset: Geometry.Vector?, bottomOffset: Geometry.Vector?) {
        if ( topOffset !== null && topOffset.length() > 0f )
            topCenter = topCenter.add( topOffset )
        if ( bottomOffset !== null && bottomOffset.length() > 0f )
            bottomCenter = bottomCenter.add( bottomOffset )

        generateVertices()
        vertexArray.updateBuffer(vertexData, 0, numberVertices * TOTAL_COMPONENT_COUNT)
    }

    fun startPositionChange() {
        previousTopOffset = Geometry.Vector(0f, 0f ,0f)
        previousBottomOffset = Geometry.Vector(0f, 0f ,0f)
    }
}

