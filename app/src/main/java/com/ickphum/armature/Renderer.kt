package com.ickphum.armature

import android.content.Context
import android.opengl.GLES20.GL_COLOR_BUFFER_BIT
import android.opengl.GLES20.glClear
import android.opengl.GLES20.glClearColor
import android.opengl.GLES20.glViewport
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.opengl.Matrix.invertM
import android.opengl.Matrix.multiplyMM
import android.opengl.Matrix.rotateM
import android.opengl.Matrix.setIdentityM
import android.opengl.Matrix.setLookAtM
import android.opengl.Matrix.translateM
import android.util.Log
import com.ickphum.armature.objects.Mallet
import com.ickphum.armature.objects.Puck
import com.ickphum.armature.objects.Table
import com.ickphum.armature.programs.ColorShaderProgram
import com.ickphum.armature.programs.TextureShaderProgram
import com.ickphum.armature.util.Geometry
import com.ickphum.armature.util.Geometry.Helper.clamp
import com.ickphum.armature.util.TextureHelper
import javax.microedition.khronos.opengles.GL10


private const val TAG = "ArmRenderer"
private const val leftBound = -0.5f
private const val rightBound = 0.5f
private const val farBound = -0.8f
private const val nearBound = 0.8f

class Renderer(context: Context) : GLSurfaceView.Renderer {

    private val context = context;

    private val projectionMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val viewProjectionMatrix = FloatArray(16)
    private val modelViewProjectionMatrix = FloatArray(16)
    private val invertedViewProjectionMatrix = FloatArray(16)

    private lateinit var table: Table
    private lateinit var mallet: Mallet
    private lateinit var puck: Puck

    private lateinit var textureProgram: TextureShaderProgram
    private lateinit var colorProgram: ColorShaderProgram
    private var texture = 0

    private var malletPressed = false
    private lateinit var blueMalletPosition: Geometry.Point
    private lateinit var previousBlueMalletPosition: Geometry.Point
    private lateinit var puckPosition: Geometry.Point
    private lateinit var puckVector: Geometry.Vector

    override fun onSurfaceCreated(glUnused: GL10?, p1: javax.microedition.khronos.egl.EGLConfig?) {
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        table = Table()
        mallet = Mallet( 0.08f, 0.15f, 32 )
        puck = Puck(0.06f, 0.02f, 32)

        blueMalletPosition = Geometry.Point(0f, mallet.height / 2f, 0.4f)
        previousBlueMalletPosition = blueMalletPosition

        puckPosition = Geometry.Point(0f, puck.height / 2f, 0f)
        puckVector = Geometry.Vector(0f, 0f, 0f)

        textureProgram = TextureShaderProgram(context)
        colorProgram = ColorShaderProgram(context)
        texture = TextureHelper.loadTexture(context, context.resources.getIdentifier(
            "air_hockey_surface",
            "drawable",
            context.packageName
        ))
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        glViewport(0, 0, width, height)

        MatrixHelper.perspectiveM(
            projectionMatrix, 45f,
            width.toFloat() / height.toFloat(), 1f, 10f
        )
        setLookAtM(viewMatrix, 0, 0f, 1.2f, 2.2f, 0f, 0f, 0f, 0f, 1f, 0f);
    }

    override fun onDrawFrame(glUnused: GL10?) {
// Clear the rendering surface.
        glClear(GL_COLOR_BUFFER_BIT)

        multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        invertM(invertedViewProjectionMatrix, 0, viewProjectionMatrix, 0)

        positionTableInScene();
        textureProgram.useProgram();
        textureProgram.setUniforms(modelViewProjectionMatrix, texture);
        table.bindData(textureProgram);
        table.draw();

        // Draw the mallets.
        positionObjectInScene(0f, mallet.height / 2f, -0.4f);
        colorProgram.useProgram();
        colorProgram.setUniforms(modelViewProjectionMatrix, 1f, 0f, 0f);
        mallet.bindData(colorProgram);
        mallet.draw();

        positionObjectInScene(blueMalletPosition.x, blueMalletPosition.y, blueMalletPosition.z)
//        positionObjectInScene(0f, mallet.height / 2f, 0.4f);
        colorProgram.setUniforms(modelViewProjectionMatrix, 0f, 0f, 1f);

        // Note that we don't have to define the object data twice -- we just
        // draw the same mallet again but in a different position and with a
        // different color.
        mallet.draw();

        puckPosition = puckPosition.translate(puckVector)
        if (puckPosition.x < leftBound + puck.radius || puckPosition.x > rightBound - puck.radius )
        {
            puckVector = Geometry.Vector(-puckVector.x, puckVector.y, puckVector.z)
            puckVector = puckVector.scale(0.99f);
        }
        if (puckPosition.z < farBound + puck.radius || puckPosition.z > nearBound - puck.radius )
        {
            puckVector = Geometry.Vector(puckVector.x, puckVector.y, -puckVector.z)
            puckVector = puckVector.scale(0.99f);

        }

        // Clamp the puck position.
        puckPosition = Geometry.Point(
            clamp(puckPosition.x, leftBound + puck.radius, rightBound - puck.radius),
            puckPosition.y,
            clamp(puckPosition.z, farBound + puck.radius, nearBound - puck.radius)
        )

        // Draw the puck.
        positionObjectInScene(puckPosition.x, puckPosition.y, puckPosition.z);
        colorProgram.setUniforms(modelViewProjectionMatrix, 0.8f, 0.8f, 1f);
        puck.bindData(colorProgram);
        puck.draw();
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

    fun handleTouchDrag(normalizedX: Float, normalizedY: Float) {
//        Log.d( TAG, "handleTouchDrag %.1f, %.1f".format( normalizedX, normalizedY ))
        if (malletPressed) {
            val ray = convertNormalized2DPointToRay(normalizedX, normalizedY)
            // Define a plane representing our air hockey table.
            val plane = Geometry.Plane( Geometry.Point(0f, 0f, 0f), Geometry.Vector(0f, 1f, 0f))
            // Find out where the touched point intersects the plane
// representing our table. We'll move the mallet along this plane.
            val touchedPoint: Geometry.Point = Geometry.intersectionPoint(ray, plane)
            blueMalletPosition = Geometry.Point(
                Geometry.clamp(touchedPoint.x,leftBound + mallet.radius,rightBound - mallet.radius),
                mallet.height / 2f,
                Geometry.clamp(touchedPoint.z,0f + mallet.radius,nearBound - mallet.radius))

            val distance = Geometry.vectorBetween(blueMalletPosition, puckPosition).length()
            if (distance < puck.radius + mallet.radius) {
                // The mallet has struck the puck. Now send the puck flying based on the mallet velocity.
                puckVector = Geometry.vectorBetween( previousBlueMalletPosition, blueMalletPosition )
            }
        }
    }

    fun handleTouchPress(normalizedX: Float, normalizedY: Float) {
//        Log.d( TAG, "handleTouchPress %.1f, %.1f".format( normalizedX, normalizedY ))

        val ray: Geometry.Ray = convertNormalized2DPointToRay(normalizedX, normalizedY)

        val boundingSphere = Geometry.Sphere(
            Geometry.Point( blueMalletPosition.x, blueMalletPosition.y, blueMalletPosition.z ),
            mallet.height / 2f)

        malletPressed = Geometry.intersects(boundingSphere, ray);
        if ( malletPressed )
            Log.d( TAG, "Pressed!" )
        else
            Log.d( TAG, "Unpressed" )
    }

    private fun positionTableInScene() {
        // The table is defined in terms of X & Y coordinates, so we rotate it
        // 90 degrees to lie flat on the XZ plane.
        setIdentityM(modelMatrix, 0)
        rotateM(modelMatrix, 0, -90f, 1f, 0f, 0f)
        multiplyMM(
            modelViewProjectionMatrix, 0, viewProjectionMatrix,
            0, modelMatrix, 0
        )
    }

    private fun positionObjectInScene(x: Float, y: Float, z: Float) {
        setIdentityM(modelMatrix, 0)
        translateM(modelMatrix, 0, x, y, z)
        multiplyMM(
            modelViewProjectionMatrix, 0, viewProjectionMatrix,
            0, modelMatrix, 0
        )
    }
}
