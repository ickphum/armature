package com.ickphum.armature.util

import kotlin.math.sqrt


class Geometry {

    class Point(val x: Float, val y: Float, val z: Float) {
        fun translateY(distance: Float): Point {
            return Point(x, y + distance, z)
        }

        fun translate(vector: Vector): Point {
            return Point(
                x + vector.x,
                y + vector.y,
                z + vector.z
            )
        }
    }

    class Circle(val center: Point, val radius: Float) {
        fun scale(scale: Float): Circle {
            return Circle(center, radius * scale)
        }
    }

    class Cylinder(val center: Point, val radius: Float, val height: Float)
    class Vector(val x: Float, val y: Float, val z: Float) {
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
    }
    class Ray(val point: Point, val vector: Vector)

    class Sphere(val center: Point, val radius: Float)




    companion object Helpers {
        fun vectorBetween(from: Point, to: Point): Vector {
            return Vector(
                to.x - from.x,
                to.y - from.y,
                to.z - from.z
            )
        }

        fun distanceBetween(point: Point, ray: Ray): Float {
            val p1ToPoint: Vector = vectorBetween(ray.point, point);
            val p2ToPoint: Vector = vectorBetween(ray.point.translate(ray.vector), point)

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

    }
}
