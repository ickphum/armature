package com.ickphum.armature.objects

import android.opengl.GLES20.GL_TRIANGLE_FAN
import android.opengl.GLES20.GL_TRIANGLE_STRIP
import android.opengl.GLES20.glDrawArrays
import android.util.Log
import com.ickphum.armature.util.Geometry
import com.ickphum.armature.util.Geometry.Circle
import com.ickphum.armature.util.Geometry.Cylinder
import kotlin.math.cos
import kotlin.math.sin

private const val TAG = "ObjectBuilder"
private const val FLOATS_PER_VERTEX = 3
class ObjectBuilder( sizeInVertices: Int ) {
    private val vertexData = FloatArray(sizeInVertices * FLOATS_PER_VERTEX)
    private var offset = 0

    interface DrawCommand {
        fun draw()
    }

    class GeneratedData(val vertexData: FloatArray, val drawList: List<DrawCommand>)

    private val drawList: MutableList<DrawCommand> = ArrayList()

    private fun appendCircle(circle: Circle, numPoints: Int) {
        val startVertex = offset / FLOATS_PER_VERTEX
        val numVertices = sizeOfCircleInVertices(numPoints)

//        Log.d( TAG, "appendCircle $numPoints, $circle")

        // Center point of fan
        vertexData[offset++] = circle.center.x
        vertexData[offset++] = circle.center.y
        vertexData[offset++] = circle.center.z

        // Fan around center point. <= is used because we want to generate
        // the point at the starting angle twice to complete the fan.
        for (i in 0..numPoints) {
            val angleInRadians = (i.toFloat() / numPoints.toFloat() * (Math.PI.toFloat() * 2f))
            Log.d( TAG, "angleInRadian $angleInRadians")
            vertexData[offset++] = (circle.center.x + circle.radius * cos(angleInRadians))
            vertexData[offset++] = circle.center.y
            vertexData[offset++] = (circle.center.z + circle.radius * sin(angleInRadians))

//            Log.d( TAG, "Added point %.2f, %.2f, %.2f".format( vertexData[offset-3], vertexData[offset-2], vertexData[offset-1] ) )
        }

        drawList.add(object : DrawCommand {
            override fun draw() {
//                Log.d( TAG, "Draw: startVertex $startVertex, numVertices $numVertices, vertexData size ${vertexData.size}")
//                for ( i in 0 .. 5) {
//                    Log.d( TAG, "Point $i ${vertexData[ i ]}")
//                }
                glDrawArrays(GL_TRIANGLE_FAN, startVertex, numVertices)
            }
        })
    }

    private fun appendOpenCylinder(cylinder: Cylinder, numPoints: Int) {
        val startVertex = offset / FLOATS_PER_VERTEX
        val numVertices = sizeOfOpenCylinderInVertices(numPoints)
        val yStart = cylinder.center.y - cylinder.height / 2f
        val yEnd = cylinder.center.y + cylinder.height / 2f
        for (i in 0..numPoints) {
            val angleInRadians = (i.toFloat() / numPoints.toFloat() * (Math.PI.toFloat() * 2f))
            val xPosition: Float = (cylinder.center.x + cylinder.radius * cos(angleInRadians))
            val zPosition: Float = (cylinder.center.z + cylinder.radius * sin(angleInRadians))
            vertexData[offset++] = xPosition
            vertexData[offset++] = yStart
            vertexData[offset++] = zPosition
            vertexData[offset++] = xPosition
            vertexData[offset++] = yEnd
            vertexData[offset++] = zPosition
        }

        drawList.add(object : DrawCommand {
            override fun draw() {
                glDrawArrays(GL_TRIANGLE_STRIP, startVertex, numVertices)
            }
        })
    }

    private fun build(): GeneratedData {
        return GeneratedData(vertexData, drawList)
    }

    companion object Helper {
        fun sizeOfCircleInVertices(numPoints: Int): Int {
            return 1 + (numPoints + 1)
        }

        fun sizeOfOpenCylinderInVertices(numPoints: Int): Int {
            return (numPoints + 1) * 2
        }

        fun createPuck(puck: Cylinder, numPoints: Int): GeneratedData {
            val size = (sizeOfCircleInVertices(numPoints)
                    + sizeOfOpenCylinderInVertices(numPoints)
                    )
            val builder = ObjectBuilder(size)
            val puckTop = Circle(
                puck.center.translateY(puck.height / 2f),
                puck.radius
            )
            builder.appendCircle(puckTop, numPoints)
            builder.appendOpenCylinder(puck, numPoints)
            return builder.build()
        }

        fun createMallet(
            center: Geometry.Point, radius: Float, height: Float, numPoints: Int
        ): GeneratedData {
            val size = (sizeOfCircleInVertices(numPoints) * 2 + sizeOfOpenCylinderInVertices(numPoints) * 2)
            val builder = ObjectBuilder(size)

            // First, generate the mallet base.
            val baseHeight = height * 0.25f
            val baseCircle = Circle(
                center.translateY(-baseHeight),
                radius
            )
            val baseCylinder = Cylinder(
                baseCircle.center.translateY(-baseHeight / 2f),
                radius, baseHeight
            )
            builder.appendCircle(baseCircle, numPoints)
            builder.appendOpenCylinder(baseCylinder, numPoints)

            val handleHeight = height * 0.75f
            val handleRadius = radius / 3f
            val handleCircle = Circle(
                center.translateY(height * 0.5f),
                handleRadius
            )

            builder.appendCircle(handleCircle, numPoints);
            val handleCylinder = Cylinder(
                handleCircle.center.translateY(-handleHeight / 2f),
                handleRadius, handleHeight
            )
            builder.appendOpenCylinder(handleCylinder, numPoints);

            return builder.build()
        }
    }
}