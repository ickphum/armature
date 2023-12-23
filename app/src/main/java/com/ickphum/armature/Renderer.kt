package com.ickphum.armature

import android.content.Context
import android.graphics.Color
import android.opengl.GLES20.GL_BLEND
import android.opengl.GLES20.GL_COLOR_BUFFER_BIT
import android.opengl.GLES20.GL_DEPTH_BUFFER_BIT
import android.opengl.GLES20.GL_DEPTH_TEST
import android.opengl.GLES20.GL_LEQUAL
import android.opengl.GLES20.GL_LESS
import android.opengl.GLES20.GL_ONE
import android.opengl.GLES20.glBlendFunc
import android.opengl.GLES20.glClear
import android.opengl.GLES20.glClearColor
import android.opengl.GLES20.glDepthFunc
import android.opengl.GLES20.glDepthMask
import android.opengl.GLES20.glDisable
import android.opengl.GLES20.glEnable
import android.opengl.GLES20.glViewport
import android.opengl.GLSurfaceView
import android.opengl.Matrix.invertM
import android.opengl.Matrix.multiplyMM
import android.opengl.Matrix.rotateM
import android.opengl.Matrix.setIdentityM
import android.opengl.Matrix.translateM
import android.opengl.Matrix.transposeM
import android.util.Log
import com.ickphum.armature.objects.Base
import com.ickphum.armature.objects.ParticleShooter
import com.ickphum.armature.objects.ParticleSystem
import com.ickphum.armature.objects.Skybox
import com.ickphum.armature.programs.BaseShaderProgram
import com.ickphum.armature.programs.ParticleShaderProgram
import com.ickphum.armature.programs.SkyboxShaderProgram
import com.ickphum.armature.util.Geometry
import com.ickphum.armature.util.TextureHelper
import javax.microedition.khronos.opengles.GL10
import kotlin.math.floor


private const val TAG = "3DRenderer"

class Renderer(context: Context) : GLSurfaceView.Renderer {

    private val context = context;

    private val projectionMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val viewProjectionMatrix = FloatArray(16)
    private val viewMatrixForSkybox = FloatArray(16)
    private val modelViewProjectionMatrix = FloatArray(16)
    private val invertedViewProjectionMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)
    private val modelViewMatrix = FloatArray(16)
    private val itModelViewMatrix = FloatArray(16)

    private lateinit var particleProgram: ParticleShaderProgram
    private lateinit var particleSystem: ParticleSystem
    private lateinit var redParticleShooter: ParticleShooter
    private lateinit var greenParticleShooter: ParticleShooter
    private lateinit var blueParticleShooter: ParticleShooter
    private var globalStartTime: Long = 0

    private lateinit var particleDirection: Geometry.Vector

    private var particleTexture = 0

    private lateinit var skyboxProgram: SkyboxShaderProgram
    private lateinit var skybox: Skybox
    private var skyboxTexture = 0

    private lateinit var baseProgram: BaseShaderProgram
    private lateinit var base: Base

    private var xRotation = 0f
    private var yRotation = 0f

    val vectorToLight = floatArrayOf(0.30f, 0.35f, -0.89f, 0f)

    private val pointLightPositions = floatArrayOf(
        -1f, 1f, 0f, 1f,
        0f, 1f, 0f, 1f,
        1f, 1f, 0f, 1f
    )
    private val pointLightColors = floatArrayOf(
        1.00f, 0.20f, 0.02f,
        0.02f, 0.15f, 0.02f,
        0.02f, 0.20f, 1.00f
    )

    override fun onSurfaceCreated(glUnused: GL10?, p1: javax.microedition.khronos.egl.EGLConfig?) {
        glClearColor(0.2f, 0.0f, 0.0f, 0.0f)
        glEnable(GL_DEPTH_TEST)
//        glEnable(GL_CULL_FACE)

        particleProgram = ParticleShaderProgram(context)
        particleSystem = ParticleSystem(1000);
        globalStartTime = System.nanoTime();

        particleDirection = Geometry.Vector(0f, 0.5f, 0f)

        val angleVarianceInDegrees = 5f;
        val speedVariance = 1f;

        redParticleShooter = ParticleShooter(
            Geometry.Point(-0.8f, 0f, 0f),
            particleDirection,
            Color.rgb(255, 50, 5),
            angleVarianceInDegrees,
            speedVariance
        )
        greenParticleShooter = ParticleShooter(
            Geometry.Point(0f, 0f, 0f),
            particleDirection,
            Color.rgb(25, 255, 25),
            angleVarianceInDegrees,
            speedVariance
        )
        blueParticleShooter = ParticleShooter(
            Geometry.Point(0.8f, 0f, 0f),
            particleDirection,
            Color.rgb(5, 50, 255),
            angleVarianceInDegrees,
            speedVariance
        )

        particleTexture = TextureHelper.loadTexture(context, R.drawable.particle_texture)

        skyboxProgram = SkyboxShaderProgram( context )
        skybox = Skybox()
        skyboxTexture = TextureHelper.loadCubeMap( context,
            intArrayOf(R.drawable.left, R.drawable.right,
                R.drawable.bottom, R.drawable.top,
                R.drawable.front, R.drawable.back))

        baseProgram = BaseShaderProgram( context )
        base = Base( 2.5f )

    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        glViewport(0, 0, width, height)

        MatrixHelper.perspectiveM(
            projectionMatrix, 45f,
            width.toFloat() / height.toFloat(), 1f, 100f
        )
        updateViewMatrices()
    }

    override fun onDrawFrame(glUnused: GL10?) {
        // Clear the rendering surface.
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT )

        drawSkybox()
        drawBase()
//        drawParticles()
    }


    private fun updateViewMatrices() {
        setIdentityM(viewMatrix, 0)
        rotateM(viewMatrix, 0, yRotation, 1f, 0f, 0f)
        rotateM(viewMatrix, 0, xRotation, 0f, 1f, 0f)
        System.arraycopy(viewMatrix, 0, viewMatrixForSkybox, 0, viewMatrix.size )

        val yFactor =

        setIdentityM(viewMatrix, 0)
        translateM(viewMatrix, 0, 0f, -3f, -15f)
        rotateM(viewMatrix, 0, xRotation, 0f, 1f, 0f)

//        =(MAX(MOD(B2,360), 360-MOD(B2,360)) - 270)/90
        val xRotMod360 = xRotation - (floor( xRotation / 360f ) * 360f);
        val zRotMod360 = (xRotation - 90f) - (floor( (xRotation - 90f) / 360f ) * 360f);

        val xFactor = ( xRotMod360.coerceAtLeast(360f - xRotMod360) - 270f ) / 90f
        val zFactor = ( zRotMod360.coerceAtLeast(360f - zRotMod360) - 270f ) / 90f;

//        Log.d( TAG, "xRotMod360 $xRotMod360, xFactor $xFactor   zRotMod360 $zRotMod360, zFactor $zFactor")

        rotateM(viewMatrix, 0, yRotation, xFactor, 0f, zFactor )

        // xr 0     x 1     z 0
        // xr 45    x 0.5   z 0.5
        // xr 90    x 0     z 1
        // xr 135   x -0.5  z 0.5
        // xr 180   x -1    z 0
        // xr 225   x -0.5  z -0.5      also xr -135
        // xr 270   x 0     z -1        also xr -90
        // xr 315   x 0.5   z -0.5      also xr -45
        // xr 360   x 1     z 0         also x = 0

//        setLookAtM( viewMatrix, 0, 5f, 2f, 0f, 0f, 0f, 0f, 5f, 3f, 0f )

        // We want the translation to apply to the regular view matrix, and not the skybox.
//        translateM(viewMatrix, 0, 0f, 1.5f, 5f)
//        rotateM(viewMatrix, 0, 5f, 0f, 1f, 0f)
    }

    private fun updateMvpMatrix() {
        multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        invertM(tempMatrix, 0, modelViewMatrix, 0)
        transposeM(itModelViewMatrix, 0, tempMatrix, 0)
        multiplyMM(
            modelViewProjectionMatrix, 0,
            projectionMatrix, 0,
            modelViewMatrix, 0
        )
    }

    private fun updateMvpMatrixForSkybox() {
        multiplyMM(tempMatrix, 0, viewMatrixForSkybox, 0, modelMatrix, 0)
        multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
    }

    private fun drawSkybox() {
        setIdentityM(modelMatrix, 0);
        updateMvpMatrixForSkybox();

        skyboxProgram.useProgram()
        skyboxProgram.setUniforms(modelViewProjectionMatrix, skyboxTexture)
        skybox.bindData(skyboxProgram)
        glDepthFunc(GL_LEQUAL);
        skybox.draw()
        glDepthFunc(GL_LESS);
    }

    private fun drawBase() {

        val currentTime = (System.nanoTime() - globalStartTime) / 1000000000f
//        Log.d( TAG, "t = $currentTime" )

        setIdentityM(modelMatrix, 0);
        updateMvpMatrix();

        glEnable(GL_BLEND)
        glBlendFunc(GL_ONE, GL_ONE)

        baseProgram.useProgram()
        baseProgram.setUniforms( modelViewProjectionMatrix, currentTime )
        base.bindData(baseProgram)
        base.draw()

        glDisable(GL_BLEND)
    }

    private fun drawParticles() {
        val currentTime = (System.nanoTime() - globalStartTime) / 1000000000f
        redParticleShooter.addParticles(particleSystem, currentTime, 1)
        greenParticleShooter.addParticles(particleSystem, currentTime, 1)
        blueParticleShooter.addParticles(particleSystem, currentTime, 1)

        setIdentityM(modelMatrix, 0);
        updateMvpMatrix();

        glEnable(GL_BLEND)
        glBlendFunc(GL_ONE, GL_ONE)

        glDepthMask( false )

        particleProgram.useProgram()
        particleProgram.setUniforms(modelViewProjectionMatrix, currentTime, particleTexture)
        particleSystem.bindData(particleProgram)
        particleSystem.draw()

        glDepthMask( true )
        glDisable(GL_BLEND)
    }

    fun handleTouchDrag(deltaX: Float, deltaY: Float) {
        xRotation += deltaX / 16f;
        yRotation += deltaY / 16f;
//        Log.d( TAG, "Move by $deltaX $deltaY, xRotation = $xRotation, yRotation = $yRotation")
        if (yRotation < -90)
            yRotation = -90f;
        else if (yRotation > 90)
            yRotation = 90f;
        updateViewMatrices()

    }

}
