package com.ickphum.armature.util

import kotlin.math.abs
import kotlin.math.sqrt


class Geometry {

    class Point(val x: Float, val y: Float, val z: Float) {

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

        override fun toString(): String {
            return "Point[ $x, $y, $z ]"
        }
    }

    class Circle(val center: Point, val radius: Float) {
        fun scale(scale: Float): Circle {
            return Circle(center, radius * scale)
        }

        override fun toString(): String {
            return "Circle[ c $center, r $radius ]"
        }
    }

    class Cylinder(val center: Point, val radius: Float, val height: Float) {
        override fun toString(): String {
            return "Cylinder(center=$center, radius=$radius, height=$height)"
        }
    }

    class Vector(val x: Float, val y: Float, val z: Float) {

        constructor( data: FloatArray, offset: Int ) : this( data[ offset], data[ offset + 1], data[ offset + 2 ])

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

        override fun toString(): String {
            return "Vector(x=$x, y=$y, z=$z)"
        }

    }
    class Ray(val point: Point, val vector: Vector) {
        override fun toString(): String {
            return "Ray(point=$point, vector=$vector)"
        }
    }

    class Sphere(val center: Point, val radius: Float)

    class Plane(val point: Point, val normal: Vector)

    class Triangle( val p1: Vector, val p2: Vector, val p3: Vector ) {

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

    }
}
