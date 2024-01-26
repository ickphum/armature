package com.ickphum.armature.objects

import android.opengl.GLES20
import android.util.Log
import com.ickphum.armature.data.VertexArray
import com.ickphum.armature.util.Geometry
import kotlin.math.cos
import kotlin.math.sin
import com.ickphum.armature.Constants.BYTES_PER_FLOAT
import com.ickphum.armature.programs.CylinderShaderProgram


class Cylinder (val center: Geometry.Point, private val radius: Float, private var height: Float ) {
    companion object {
        private const val POSITION_COMPONENT_COUNT = 3
        private const val NORMAL_COMPONENT_COUNT = 3
        private const val TOTAL_COMPONENT_COUNT = POSITION_COMPONENT_COUNT + NORMAL_COMPONENT_COUNT
        private const val STRIDE = TOTAL_COMPONENT_COUNT * BYTES_PER_FLOAT
        private const val SEGMENTS = 12
        private const val TAG = "Cylinder"
        private const val triangleMode = true

        // the fan and the strip both go around the circle and repeat the start,
        // hence the plus 1; 1 vertex per segment for the fan, 2 for the strip.
        // Final +1 for the fan center
        private const val NUMBER_FAN_VERTICES = SEGMENTS + 2

        // strip offset into vertex data
        private const val FAN_VERTEX_OFFSET = NUMBER_FAN_VERTICES * TOTAL_COMPONENT_COUNT;
    }

    private val NUMBER_SIDE_VERTICES = if (triangleMode) SEGMENTS * 6 else (SEGMENTS + 1) * 2
    private val NUMBER_VERTICES = NUMBER_FAN_VERTICES + NUMBER_SIDE_VERTICES

    private val vertexData = FloatArray(NUMBER_VERTICES * TOTAL_COMPONENT_COUNT)
    private lateinit var vertexArray: VertexArray

    private val selectedColor = floatArrayOf(0.3f, 0.7f, 0.3f)
    private val normalColor = floatArrayOf(0.5f, 0.1f, 0.1f)

    // these planes and triangles are used for touch detection
    private lateinit var xyPlane: Geometry.Plane
    private lateinit var zyPlane: Geometry.Plane
    private lateinit var xyTriangleTop: Geometry.Triangle
    private lateinit var xyTriangleBottom: Geometry.Triangle
    private lateinit var zyTriangleTop: Geometry.Triangle
    private lateinit var zyTriangleBottom: Geometry.Triangle

    var selected = true

    init {
        generateVertices()
    }

    private fun generateVertices() {

        // fill in the float data
        var offset = 0
        var sideOffset = FAN_VERTEX_OFFSET

        // centre of top triangle fan
        vertexData[offset++] = center.x
        vertexData[offset++] = center.y + height
        vertexData[offset++] = center.z

        // needed for normal calc
        val centerPoint = Geometry.Point(vertexData, 0)

        // all top normals are straight up
        vertexData[offset++] = 0f
        vertexData[offset++] = 1f
        vertexData[offset++] = 0f


        // points around top triangle fan
        for (i in 0..SEGMENTS) {

            generateCirclePoint(vertexData, offset, i, center, radius, triangleMode)
            offset += TOTAL_COMPONENT_COUNT

            if (triangleMode) {

                // triangles don't wrap around, so ignore the last fan vertex
                if (i < SEGMENTS) {
                    generateTrianglePoints(vertexData, offset, sideOffset, centerPoint)
                    sideOffset += 6 * TOTAL_COMPONENT_COUNT
                }
            } else {
                generateStripPoints(vertexData, offset, sideOffset, centerPoint)
                sideOffset += 2 * TOTAL_COMPONENT_COUNT
            }

        }

        // create the vertex array
        vertexArray = VertexArray(vertexData)

        // touch detection
        xyPlane = Geometry.Plane( center, Geometry.Vector( 0f, 0f, 1f ))
        zyPlane = Geometry.Plane( center, Geometry.Vector( 1f, 0f, 0f ))
        xyTriangleTop = Geometry.Triangle(
            Geometry.Vector( center.x - radius, center.y + height, center.z ),
            Geometry.Vector( center.x - radius, center.y, center.z ),
            Geometry.Vector( center.x + radius, center.y + height, center.z ),
        )
        xyTriangleBottom = Geometry.Triangle(
            Geometry.Vector( center.x - radius, center.y, center.z ),
            Geometry.Vector( center.x + radius, center.y, center.z ),
            Geometry.Vector( center.x + radius, center.y + height, center.z ),
        )
        zyTriangleTop = Geometry.Triangle(
            Geometry.Vector( center.x, center.y + height, center.z - radius),
            Geometry.Vector( center.x, center.y, center.z - radius),
            Geometry.Vector( center.x, center.y + height, center.z + radius),
        )
        zyTriangleBottom = Geometry.Triangle(
            Geometry.Vector( center.x, center.y, center.z - radius),
            Geometry.Vector( center.x, center.y, center.z + radius),
            Geometry.Vector( center.x, center.y + height, center.z + radius),
        )
    }

    private fun generateCirclePoint(
        vertexData: FloatArray,
        startOffset: Int,
        i: Int,
        center: Geometry.Point,
        radius: Float,
        triangleMode: Boolean
    ) {
        val angleInRadians = (i.toFloat() / SEGMENTS.toFloat() * (Math.PI.toFloat() * 2f))
        var offset = startOffset

        vertexData[offset++] = (center.x + radius * cos(angleInRadians))
        vertexData[offset++] = center.y + height
        vertexData[offset++] = (center.z + radius * sin(angleInRadians))

        // top normal
        vertexData[offset++] = 0f
        vertexData[offset++] = 1f
        vertexData[offset++] = 0f

        // if we're generating triangles, not a strip, we need to know the next point around the circle as well.
        // We're not trying to be super-efficient here, otherwise we'd run the loop twice, once to
        // do all the circle vertices and once for the sides. This point will be overwritten next loop,
        // since we're only incrementing offset by 1 point
        if (triangleMode) {
            val nextIndex = if (i == SEGMENTS) 0 else i + 1
            val nextAngle = (nextIndex.toFloat() / SEGMENTS.toFloat() * (Math.PI.toFloat() * 2f))

            vertexData[offset++] = (center.x + radius * cos(nextAngle))
            vertexData[offset++] = center.y + height
            vertexData[offset++] = (center.z + radius * sin(nextAngle))

            // don't bother doing the normal
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
        startSideOffset: Int,
        centerPoint: Geometry.Point
    ) {

        // fill in the side strip at the same time; we know the number of points
        // in the top fan, and we just start at the end of those.
        // the top side of the strip is exactly the same as the fan points;
        // we could draw via index to save the duplication.
        var sideOffset = startSideOffset

        // we have 2 points on the circle to deal with; this is the vertexData offsets to the first point,
        // the second is just offset
        val circlePoint1 = offset - 3 - NORMAL_COMPONENT_COUNT

        val topTriangle = Geometry.Triangle(
            Geometry.Vector(vertexData, circlePoint1),
            Geometry.Vector(
                vertexData[circlePoint1],
                vertexData[circlePoint1 + 1] - height,
                vertexData[circlePoint1 + 2],
            ),
            Geometry.Vector(vertexData, offset)
        )

        val bottomTriangle = Geometry.Triangle(
            Geometry.Vector(
                vertexData[circlePoint1],
                vertexData[circlePoint1 + 1] - height,
                vertexData[circlePoint1 + 2],
            ),
            Geometry.Vector(
                vertexData[offset],
                vertexData[offset + 1] - height,
                vertexData[offset + 2],
            ),
            Geometry.Vector(vertexData, offset)
        )

        topTriangle.writeToArray(vertexData, sideOffset)
        sideOffset += 3 * TOTAL_COMPONENT_COUNT

        bottomTriangle.writeToArray(vertexData, sideOffset)
    }

    fun bindData(program: CylinderShaderProgram) {
        vertexArray.setVertexAttribPointer(
            0,
            0, // program.getPositionAttributeLocation(),
            POSITION_COMPONENT_COUNT, STRIDE
        )
        vertexArray.setVertexAttribPointer(
            POSITION_COMPONENT_COUNT,
            1, // program.getNormalAttributeLocation(),
            NORMAL_COMPONENT_COUNT, STRIDE
        )

        program.setColorUniform(if (selected) selectedColor else normalColor)

    }

    fun draw() {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, NUMBER_FAN_VERTICES)
        if (triangleMode)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, NUMBER_FAN_VERTICES, NUMBER_SIDE_VERTICES)
        else
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, NUMBER_FAN_VERTICES, NUMBER_SIDE_VERTICES)
    }

    fun changeHeight(delta: Float) {
        val safeDelta = if (height + delta <= 0f) 0.01f - height else delta

        height += safeDelta
        generateVertices()
        vertexArray.updateBuffer(vertexData, 0, NUMBER_VERTICES * TOTAL_COMPONENT_COUNT)
    }

    fun findIntersectionPoint( ray: Geometry.Ray): Geometry.Point? {
        var touchedPoint: Geometry.Point? = Geometry.intersectionPoint(ray, xyPlane)
        if ( touchedPoint != null )
        {
            if ( xyTriangleTop.pointInTriangle( touchedPoint ) || xyTriangleBottom.pointInTriangle( touchedPoint ))
                return touchedPoint
        }
        touchedPoint = Geometry.intersectionPoint(ray, zyPlane)
        if ( touchedPoint != null )
        {
            if ( zyTriangleTop.pointInTriangle( touchedPoint ) || zyTriangleBottom.pointInTriangle( touchedPoint ))
                return touchedPoint
        }
        return null
    }
}

