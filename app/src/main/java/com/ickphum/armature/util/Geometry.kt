package com.ickphum.armature.util

import android.opengl.Matrix
import com.ickphum.armature.Renderer
import com.ickphum.armature.enum.Axis
import glm_.quat.Quat
import glm_.vec3.Vec3
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


class Geometry {

    class Point(var x: Float, var y: Float, var z: Float) {

        constructor( data: FloatArray, offset: Int ) : this( data[ offset], data[ offset + 1], data[ offset + 2 ])

        fun translateY(distance: Float): Point {
            return Point(x, y + distance, z)
        }

        fun add(vector: Vector): Point {
            return Point(
                x + vector.x,
                y + vector.y,
                z + vector.z
            )
        }

        fun subtract(vector: Vector): Point {
            return Point(
                x - vector.x,
                y - vector.y,
                z - vector.z
            )
        }

        fun subtract(point: Point): Point {
            return Point(
                x - point.x,
                y - point.y,
                z - point.z
            )
        }

        fun asArray() : FloatArray {
            return floatArrayOf( x, y, z )
        }

        fun asVector() : Vector {
            return Vector( x, y, z )
        }

        fun rotate( quat: Quat ) : Point {
            val v = quat.times( Vec3( x, y, z ))
            return Point( v.x, v.y, v.z )
        }

        fun findCongruency( point : Point) : Renderer.Congruency {
            val small = 0.03
            val sameX = if ( abs( x - point.x ) < small ) 4 else 0
            val sameY = if ( abs( y - point.y ) < small ) 2 else 0
            val sameZ = if ( abs( z - point.z ) < small ) 1 else 0
            val bits = sameX or sameY or sameZ
//            Log.d( TAG, "FC $x,$y,$z against $point -> $sameX, $sameY, $sameZ, $bits")
            val data = mapOf<Int, Renderer.Congruency>(
                0b111 to Renderer.Congruency.SAME_POINT,
                0b110 to Renderer.Congruency.SAME_XY,
                0b101 to Renderer.Congruency.SAME_XZ,
                0b011 to Renderer.Congruency.SAME_YZ,
                0b100 to Renderer.Congruency.SAME_X,
                0b010 to Renderer.Congruency.SAME_Y,
                0b001 to Renderer.Congruency.SAME_Z,
                0b000 to Renderer.Congruency.NONE
            )
            return data[ bits ]!!
        }

        override fun toString(): String {
            return "Point[ $x, $y, $z ]"
        }

        // quasi-array access
        operator fun get(i: Int): Float {
            return if ( i == 0 ) x else if ( i == 1 ) y else z
        }

        companion object {
            private const val TAG = "Point"
        }
    }

    class Vector(var x: Float, var y: Float, var z: Float) {

        constructor( data: FloatArray, offset: Int ) : this( data[ offset], data[ offset + 1], data[ offset + 2 ])
        constructor( point: Point ) : this( point.x, point.y, point.z )

        fun length(): Float {
            return sqrt(
                x * x + y * y + z * z
            )
        }

        fun crossProduct(other: Vector): Vector {
            return Vector(
                y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x
            )
        }

        fun dotProduct(other: Vector): Float {
            return x * other.x + y * other.y + z * other.z
        }

        fun scale(f: Float): Vector {
            return Vector(
                x * f,
                y * f,
                z * f
            )
        }

        fun normalize(): Vector {
            return scale(1f / length())
        }

        fun subtract(point: Point): Vector {
            return Vector (
                x - point.x,
                y - point.y,
                z - point.z
            )
        }

        fun subtract(vector: Vector): Vector {
            return Vector (
                x - vector.x,
                y - vector.y,
                z - vector.z
            )
        }

        fun add(vector: Vector): Vector {
            return Vector (
                x + vector.x,
                y + vector.y,
                z + vector.z
            )
        }

        override fun toString(): String {
            return "Vector(x=$x, y=$y, z=$z)"
        }

        fun asArray() : FloatArray {
            return floatArrayOf( x, y, z )
        }

        fun fromVec3( vec3: Vec3 ) {
            x = vec3.x
            y = vec3.y
            z = vec3.z
        }

    }
    class Ray(val point: Point, val vector: Vector) {
        override fun toString(): String {
            return "Ray(point=$point, vector=$vector)"
        }
    }

    class Sphere(val center: Point, val radius: Float)

    class Plane(val point: Point, val normal: Vector)

    class Triangle(private val p1: Vector, private val p2: Vector, private val p3: Vector ) {

        fun normal() : Vector {
            val p1MinusP2 = p1.subtract( p2 )
            val p3MinusP2 = p3.subtract( p2 )
            return p1MinusP2.crossProduct( p3MinusP2 )
        }

        fun pointInTriangle( p: Point ) : Boolean {

            // https://gdbooks.gitbooks.io/3dcollisions/content/Chapter4/point_in_triangle.html

            val a = p1.subtract( p )
            val b = p2.subtract( p )
            val c = p3.subtract( p )

            val u = b.crossProduct( c )
            val v = c.crossProduct( a )
            val w = a.crossProduct( b )

            if ( u.dotProduct( v ) < 0f )
                return false

            return u.dotProduct( w ) >= 0f
        }

        // write vertex and normal data for this triangle to the supplied array+offset
        fun writeToArray(data: FloatArray, offset: Int) {
            // we're assuming flat shading, so all vertices have the same normal
            val n = normal()
//            Log.d( "Triangle", "normal $n" )

            data[ offset ] = p1.x
            data[ offset + 1] = p1.y
            data[ offset + 2] = p1.z
            data[ offset + 3] = n.x
            data[ offset + 4] = n.y
            data[ offset + 5] = n.z

            data[ offset + 6] = p2.x
            data[ offset + 7] = p2.y
            data[ offset + 8] = p2.z
            data[ offset + 9] = n.x
            data[ offset + 10] = n.y
            data[ offset + 11] = n.z

            data[ offset + 12] = p3.x
            data[ offset + 13] = p3.y
            data[ offset + 14] = p3.z
            data[ offset + 15] = n.x
            data[ offset + 16] = n.y
            data[ offset + 17] = n.z
        }

    }

    class Rectangle( tl: Vector, bl: Vector, br: Vector, tr: Vector  ) {
        val topTriangle = Triangle( tl, bl, tr )
        val bottomTriangle = Triangle( bl, br, tr )
        val plane = Plane( Point( tl.x, tl.y, tl.z ), topTriangle.normal())
    }

    companion object Helper {
        fun vectorBetween(from: Point, to: Point): Vector {
            return Vector(
                to.x - from.x,
                to.y - from.y,
                to.z - from.z
            )
        }

        fun distanceBetween(point: Point, ray: Ray): Float {
            val p1ToPoint: Vector = vectorBetween(ray.point, point);
            val p2ToPoint: Vector = vectorBetween(ray.point.add(ray.vector), point)

            // The length of the cross product gives the area of an imaginary
            // parallelogram having the two vectors as sides. A parallelogram can be
            // thought of as consisting of two triangles, so this is the same as
            // twice the area of the triangle defined by the two vectors.
            // http://en.wikipedia.org/wiki/Cross_product#Geometric_meaning
            val areaOfTriangleTimesTwo = p1ToPoint.crossProduct(p2ToPoint).length()
            val lengthOfBase = ray.vector.length()

            // The area of a triangle is also equal to (base * height) / 2. In
            // other words, the height is equal to (area * 2) / base. The height
            // of this triangle is the distance from the point to the ray.
            return areaOfTriangleTimesTwo / lengthOfBase
        }

        fun intersects(sphere: Sphere, ray: Ray): Boolean {
            return distanceBetween(sphere.center, ray) < sphere.radius
        }

        fun intersectionPoint(ray: Ray, plane: Plane): Point? {
            val rayToPlaneVector = vectorBetween(ray.point, plane.point)
            val rayDotProduct =  ray.vector.dotProduct(plane.normal)
            if ( abs( rayDotProduct ) > 0.00000001 ) {
                val scaleFactor: Float = (rayToPlaneVector.dotProduct(plane.normal)
                        / rayDotProduct)
                return ray.point.add(ray.vector.scale(scaleFactor))
            }
            return null
        }

        fun clamp(value: Float, min: Float, max: Float): Float {
            return max.coerceAtMost(value.coerceAtLeast(min))
        }

        fun intClamp(value: Int, min: Int, max: Int): Int {
            return max.coerceAtMost(value.coerceAtLeast(min))
        }

        // Generate circles parallel to any axis.
        fun addCircleData(
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

            // centre of triangle fan
            vertexData[offset++] = center.x
            vertexData[offset++] = center.y
            vertexData[offset++] = center.z

            // normal array is init to 0, just set the main axis for the center normal
            normalArray[ mainAxis ] = if ( initialOffset == 0 ) -1f else 1f // such a hack...
            vertexData[offset++] = normalArray[ 0 ]
            vertexData[offset++] = normalArray[ 1 ]
            vertexData[offset++] = normalArray[ 2 ]

            for (i in 0..segments) {
                val angleInRadians = (i.toFloat() / segments.toFloat() * (Math.PI.toFloat() * 2f))

                // generate the point as a float array using the axis; the main axis will always match
                // center, the other 2 axes will differ as cos and sin
                pointArray[ mainAxis ] = vertexData[ initialOffset + mainAxis ]
                pointArray[ otherAxes[0] ] = vertexData[ initialOffset + otherAxes[0] ] + radius * cos( angleInRadians )
                pointArray[ otherAxes[1] ] = vertexData[ initialOffset + otherAxes[1] ] + radius * sin( angleInRadians )

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

        private fun divideByW(vector: FloatArray) {
            vector[0] /= vector[3]
            vector[1] /= vector[3]
            vector[2] /= vector[3]
        }

        fun convertNormalized2DPointToRay(
            normalizedX: Float, normalizedY: Float, invertedViewProjectionMatrix : FloatArray
        ): Geometry.Ray {

            // We'll convert these normalized device coordinates into world-space
            // coordinates. We'll pick a point on the near and far planes, and draw a
            // line between them. To do this transform, we need to first multiply by
            // the inverse matrix, and then we need to undo the perspective divide.
            val nearPointNdc = floatArrayOf(normalizedX, normalizedY, -1f, 1f)
            val farPointNdc = floatArrayOf(normalizedX, normalizedY, 1f, 1f)
            val nearPointWorld = FloatArray(4)
            val farPointWorld = FloatArray(4)
            Matrix.multiplyMV(
                nearPointWorld, 0, invertedViewProjectionMatrix, 0, nearPointNdc, 0
            )
            Matrix.multiplyMV(
                farPointWorld, 0, invertedViewProjectionMatrix, 0, farPointNdc, 0
            )

            // Why are we dividing by W? We multiplied our vector by an inverse
            // matrix, so the W value that we end up is actually the *inverse* of
            // what the projection matrix would create. By dividing all 3 components
            // by W, we effectively undo the hardware perspective divide.
            divideByW(nearPointWorld)
            divideByW(farPointWorld)

            // We don't care about the W value anymore, because our points are now
            // in world coordinates.
            val nearPointRay = Point(nearPointWorld[0], nearPointWorld[1], nearPointWorld[2])
            val farPointRay = Point(farPointWorld[0], farPointWorld[1], farPointWorld[2])
            return Ray(
                nearPointRay,
                vectorBetween(nearPointRay, farPointRay)
            )
        }

        fun compareTouchedPoint( point : Geometry.Point, maxZ : Float?, matrix: FloatArray ) : Float? {
            val resultVec4 = FloatArray( 4 )

            // ok, basically I saw a post that said "to find the closest vertex, just apply the modelView transform and
            // then look at the Z values, highest is closest." Seems to work.
            val hitVec4 = floatArrayOf( *point.asArray(), 1f )
            Matrix.multiplyMV(resultVec4, 0, matrix, 0, hitVec4, 0)
            if ( maxZ == null || resultVec4[2] > maxZ ) {
                // new max Z ie closest hit
                return resultVec4[2]
            }
            return null
        }

    }
}
