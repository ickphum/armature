package com.ickphum.armature.objects

import android.opengl.Matrix.multiplyMV
import android.opengl.Matrix.setRotateEulerM
import com.ickphum.armature.util.Geometry
import kotlin.random.Random


class ParticleShooter(
    private val position: Geometry.Point,
    direction: Geometry.Vector,
    private val color: Int,
    private val angleVariance: Float,
    private val speedVariance: Float
)
{

    private val rotationMatrix = FloatArray(16)
    private var directionVector = FloatArray(4)
    private val resultVector = FloatArray(4)

    init {
        directionVector[0] = direction.x;
        directionVector[1] = direction.y;
        directionVector[2] = direction.z;
    }

    fun addParticles( particleSystem: ParticleSystem, currentTime: Float, count: Int )
    {
        for (i in 0 until count) {

            setRotateEulerM(
                rotationMatrix, 0,
                (Random.nextFloat() - 0.5f) * angleVariance,
                (Random.nextFloat() - 0.5f) * angleVariance,
                (Random.nextFloat() - 0.5f) * angleVariance
            )
            multiplyMV(
                resultVector, 0,
                rotationMatrix, 0,
                directionVector, 0
            )
            val speedAdjustment: Float = 1f + Random.nextFloat() * speedVariance
            val thisDirection = Geometry.Vector(
                resultVector[0] * speedAdjustment,
                resultVector[1] * speedAdjustment,
                resultVector[2] * speedAdjustment
            )
            particleSystem.addParticle(position, color, thisDirection, currentTime)
        }
    }
}

