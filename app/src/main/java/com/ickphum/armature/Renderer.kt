package com.ickphum.armature

import android.content.Context
import android.opengl.GLES20.GL_BLEND
import android.opengl.GLES20.GL_COLOR_BUFFER_BIT
import android.opengl.GLES20.GL_DEPTH_BUFFER_BIT
import android.opengl.GLES20.GL_DEPTH_TEST
import android.opengl.GLES20.GL_LEQUAL
import android.opengl.GLES20.GL_LESS
import android.opengl.GLES20.GL_ONE
import android.opengl.GLES20.GL_VERSION
import android.opengl.GLES20.glBlendFunc
import android.opengl.GLES20.glClear
import android.opengl.GLES20.glClearColor
import android.opengl.GLES20.glDepthFunc
import android.opengl.GLES20.glDisable
import android.opengl.GLES20.glEnable
import android.opengl.GLES20.glGetString
import android.opengl.GLES20.glViewport
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.opengl.Matrix.invertM
import android.opengl.Matrix.multiplyMM
import android.opengl.Matrix.multiplyMV
import android.opengl.Matrix.rotateM
import android.opengl.Matrix.setIdentityM
import android.opengl.Matrix.translateM
import android.opengl.Matrix.transposeM
import android.util.Log
import com.ickphum.armature.objects.Base
import com.ickphum.armature.objects.Cylinder
import com.ickphum.armature.objects.Skybox
import com.ickphum.armature.programs.BaseShaderProgram
import com.ickphum.armature.programs.CylinderShaderProgram
import com.ickphum.armature.programs.SkyboxShaderProgram
import com.ickphum.armature.util.Geometry
import com.ickphum.armature.util.TextureHelper
import javax.microedition.khronos.opengles.GL10
import kotlin.math.floor
import kotlin.math.sin


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

    private var globalStartTime: Long = 0

    private lateinit var skyboxProgram: SkyboxShaderProgram
    private lateinit var skybox: Skybox
    private var skyboxTexture = 0

    private lateinit var baseProgram: BaseShaderProgram
    private lateinit var base: Base

    enum class PreviousTouch {
        NOTHING, BASE, ITEM, NODE
    }
    private var previousTouch = PreviousTouch.NOTHING

    // intersection of touch down with base
    private var basePoint: Geometry.Point? = null
    private var touchedCylinder: Cylinder? = null

    private lateinit var cylinderProgram: CylinderShaderProgram
    private var cylinders = mutableListOf<Cylinder>( )

    private var xRotation = 0f
    private var yRotation = 25f

    private val vectorToLight = Geometry.Vector(2f, 5f, 4f).normalize()

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

    private var state = State.SELECT
    private var preDragState = State.SELECT

    override fun onSurfaceCreated(glUnused: GL10?, p1: javax.microedition.khronos.egl.EGLConfig?) {

        val version = glGetString(GL_VERSION)

        glClearColor(0.2f, 0.0f, 0.0f, 0.0f)
        glEnable(GL_DEPTH_TEST)
//        glEnable(GL_CULL_FACE)

        Log.d( TAG, "************** version $version ******************")

        globalStartTime = System.nanoTime();

        val angleVarianceInDegrees = 5f;
        val speedVariance = 1f;

        skyboxProgram = SkyboxShaderProgram( context )
        skybox = Skybox()
        skyboxTexture = TextureHelper.loadCubeMap( context,
            intArrayOf(R.drawable.left, R.drawable.right,
                R.drawable.bottom, R.drawable.top,
                R.drawable.front, R.drawable.back))

        baseProgram = BaseShaderProgram( context )
        base = Base( 2.5f )

        cylinderProgram = CylinderShaderProgram( context )

//        cylinder = Cylinder(Geometry.Point(1f, 0f, -2f), 0.2f, 0.5f)

        // triangle tests
        val t1 = Geometry.Triangle( Geometry.Vector( 0f, 0f, 0f ), Geometry.Vector( 4f, 0f, 0f ), Geometry.Vector( 0f, 0f, 4f ))
        Log.d( TAG, "Normal to flat triangle ${t1.normal()}")

        val points = arrayOf(
            Geometry.Point(0f, 0f, 0f),
            Geometry.Point(-0.01f, 0f, 0f),
            Geometry.Point(0f, 0f, -0.01f),
            Geometry.Point(4f, 0f, 0f),
            Geometry.Point(0f, 0f, 4f),
            Geometry.Point(4.01f, 0f, 0f),
            Geometry.Point(0f, 0f, 4.01f),
            Geometry.Point(1f, 0f, 1f),
            Geometry.Point(5f, 0f, 0f),
        )
        for ( p in points ) {
            val rc = t1.pointInTriangle( p )
            Log.d( TAG, "Check point $p $rc")
        }

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
        drawCylinders()
        drawBase()
    }


    private fun updateViewMatrices() {
        setIdentityM(viewMatrix, 0)
        rotateM(viewMatrix, 0, yRotation, 1f, 0f, 0f)
        rotateM(viewMatrix, 0, xRotation, 0f, 1f, 0f)
        System.arraycopy(viewMatrix, 0, viewMatrixForSkybox, 0, viewMatrix.size )

        setIdentityM(viewMatrix, 0)
        translateM(viewMatrix, 0, 0f, -4f, -15f)
        rotateM(viewMatrix, 0, xRotation, 0f, 1f, 0f)

        // xFactor formula
        // =(MAX(MOD(B2,360), 360-MOD(B2,360)) - 270)/90
        val xRotMod360 = xRotation - (floor( xRotation / 360f ) * 360f);
        val zRotMod360 = (xRotation - 90f) - (floor( (xRotation - 90f) / 360f ) * 360f);

        val xFactor = ( xRotMod360.coerceAtLeast(360f - xRotMod360) - 270f ) / 90f
        val zFactor = ( zRotMod360.coerceAtLeast(360f - zRotMod360) - 270f ) / 90f;

//        Log.d( TAG, "xRotMod360 $xRotMod360, xFactor $xFactor   zRotMod360 $zRotMod360, zFactor $zFactor")

        rotateM(viewMatrix, 0, yRotation, xFactor, 0f, zFactor )

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
        invertM(invertedViewProjectionMatrix, 0, modelViewProjectionMatrix, 0);
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

        // pct is in range 0-1
        val pct = if (state == State.SELECT) ((sin(currentTime * 3f) + 1f) / 2f)  else 0.5f

        setIdentityM(modelMatrix, 0);
        updateMvpMatrix();

        glEnable(GL_BLEND)
        glBlendFunc(GL_ONE, GL_ONE)

        baseProgram.useProgram()
        baseProgram.setUniforms( modelViewProjectionMatrix, currentTime, 0f, 0.2f + 0.2f * pct, 0.1f )
        base.bindData(baseProgram)
        base.draw()

        glDisable(GL_BLEND)
    }

    private fun drawCylinders() {

        if ( cylinders.size == 0 ) return

        setIdentityM(modelMatrix, 0);
        updateMvpMatrix();

        cylinderProgram.useProgram()
        cylinderProgram.setUniforms( modelViewProjectionMatrix, vectorToLight )

        for ( cyl in cylinders ) {
            cyl.bindData(cylinderProgram, state)
            cyl.draw()
        }
    }

    private fun divideByW(vector: FloatArray) {
        vector[0] /= vector[3]
        vector[1] /= vector[3]
        vector[2] /= vector[3]
    }
    private fun convertNormalized2DPointToRay(
        normalizedX: Float, normalizedY: Float
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
        val nearPointRay = Geometry.Point(nearPointWorld[0], nearPointWorld[1], nearPointWorld[2])
        val farPointRay = Geometry.Point(farPointWorld[0], farPointWorld[1], farPointWorld[2])
        return Geometry.Ray(
            nearPointRay,
            Geometry.vectorBetween(nearPointRay, farPointRay)
        )
    }

    private fun clearCylinderSelections() {
        for (cyl in cylinders) {
            cyl.selected = false;
        }
    }

    fun handleTouchDown(normalizedX: Float, normalizedY: Float) : Int {
        val ray = convertNormalized2DPointToRay(normalizedX, normalizedY)

        var maxZ : Float? = null
        touchedCylinder = null
        for (cyl in cylinders) {
            var hitVec4 = FloatArray( 4 )
            var resultVec4 = FloatArray( 4 )
            val cylinderHit = cyl.findIntersectionPoint( ray )
            if ( cylinderHit != null ) {

                // ok, basically I saw a post that said "to find the closest vertex, just apply the modelView transform and
                // then look at the Z values, highest is closest." Seems to work.
                hitVec4 = floatArrayOf( *cylinderHit.asArray(), 1f )
                multiplyMV( resultVec4, 0, modelViewMatrix, 0, hitVec4, 0 )
                Log.d( TAG, "Cylinder ${cyl.center.x} hit $cylinderHit -> ${resultVec4[0]},${resultVec4[1]},${resultVec4[2]},${resultVec4[3]}")
                if ( maxZ == null || resultVec4[2] > maxZ ) {
                    // new min Z ie closest hit
                    Log.d( TAG, "new min")
                    maxZ = resultVec4[2];
                    touchedCylinder = cyl
                }
            }
        }
        if ( touchedCylinder != null ) {
            basePoint = null
            previousTouch = PreviousTouch.ITEM
        }
        else {
            basePoint = base.findIntersectionPoint(ray)
            previousTouch = if (basePoint != null) PreviousTouch.BASE else PreviousTouch.NOTHING
        }
        return previousTouch.ordinal
    }

    fun handleShortPress(normalizedX: Float, normalizedY: Float) {
        Log.d( TAG, "Short press at $normalizedX $normalizedY")

        if ( previousTouch == PreviousTouch.ITEM ) {
            if ( state == State.SINGLE ) {
                val selectedCyl = cylinders.find { cylinder -> cylinder.selected  }
                if ( selectedCyl == touchedCylinder ) {
                    Log.d( TAG, "Touch on selected cylinder, cycle plane")
                }
                else {
                    Log.d( TAG, "Touch on new cylinder; ignore or switch?")
                    selectedCyl!!.selected = false
                    touchedCylinder!!.selected = true
                }
            }
            else if ( state == State.GROUP ) {
                touchedCylinder!!.selected = !touchedCylinder!!.selected
                val count = cylinders.filter { cyl -> cyl.selected }.size
                if ( count == 0 )
                    state = State.SELECT
            }
            else if ( state == State.SELECT ) {
                touchedCylinder!!.selected = true
                state = State.SINGLE
            }
        }
        else if ( previousTouch == PreviousTouch.NOTHING ) {
            clearCylinderSelections()
            state = State.SELECT
        }
    }

    fun handleDragStart() {

        //  Log.d( TAG, "Touch at $normalizedX $normalizedY, ray $ray, base point $basePoint")
        preDragState = state

        if ( basePoint == null ) {
            state = State.PANNING
        }
        else if ( state == State.SELECT ) {

            // clear selection from existing cylinders
            clearCylinderSelections()

            // add a new point
            val cylinder = Cylinder(basePoint!!, 0.3f, 0.5f)
            cylinders.add(cylinder)

            preDragState = State.SINGLE
            state = State.MOVE
        }

    }

    fun handleDragMove(deltaX: Float, deltaY: Float) {

        if ( state == State.PANNING ) {
            xRotation += deltaX / 16f;
            yRotation += deltaY / 16f;
//        Log.d( TAG, "Move by $deltaX $deltaY, xRotation = $xRotation, yRotation = $yRotation")
            if (yRotation < -90)
                yRotation = -90f;
            else if (yRotation > 90)
                yRotation = 90f;
            updateViewMatrices()
        }
        else if ( state == State.MOVE ){
            Log.d(TAG, "Move by $deltaX $deltaY, xRotation = $xRotation, yRotation = $yRotation")
            val cylinder = cylinders.get( cylinders.size - 1 )
            cylinder.changeHeight( -deltaY / 100 )
        }
    }

    fun handleDragEnd(normalizedX: Float, normalizedY: Float) {
        Log.d( TAG, "Drag end at $normalizedX $normalizedY")
        state = preDragState
    }

    fun handleLongPress() {
        Log.d( TAG, "Long press")
        if ( previousTouch == PreviousTouch.ITEM ) {
            if ( state == State.SINGLE || state == State.GROUP || state == State.SELECT ) {
                touchedCylinder!!.selected = true
                state = State.GROUP
            }
        }

    }

}
