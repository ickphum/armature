package com.ickphum.armature

import android.content.Context
import android.opengl.GLES20.GL_COLOR_BUFFER_BIT
import android.opengl.GLES20.glClear
import android.opengl.GLES20.glClearColor
import android.opengl.GLES20.glViewport
import android.opengl.GLSurfaceView
import android.opengl.Matrix.multiplyMM
import android.opengl.Matrix.rotateM
import android.opengl.Matrix.setIdentityM
import android.opengl.Matrix.translateM
import com.ickphum.armature.data.Mallet
import com.ickphum.armature.data.Table
import com.ickphum.armature.programs.ColorShaderProgram
import com.ickphum.armature.programs.TextureShaderProgram
import com.ickphum.armature.util.TextureHelper
import javax.microedition.khronos.opengles.GL10


private const val TAG = "ArmRenderer"

class Renderer(context: Context) : GLSurfaceView.Renderer {

    private val context = context;

    private val projectionMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private var table: Table? = null
    private var mallet: Mallet? = null
    private var textureProgram: TextureShaderProgram? = null
    private var colorProgram: ColorShaderProgram? = null
    private var texture = 0

    override fun onSurfaceCreated(glUnused: GL10?, p1: javax.microedition.khronos.egl.EGLConfig?) {
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        table = Table()
        mallet = Mallet()
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

        setIdentityM(modelMatrix, 0);
        translateM(modelMatrix, 0, 0f, 0f, -3f)
        rotateM(modelMatrix, 0, -60f, 1f, 0f, 0f)

        val temp = FloatArray(16)
        multiplyMM(temp, 0, projectionMatrix, 0, modelMatrix, 0);
        System.arraycopy(temp, 0, projectionMatrix, 0, temp.size);
    }
    override fun onDrawFrame(glUnused: GL10?) {
// Clear the rendering surface.
        glClear(GL_COLOR_BUFFER_BIT)

        // Draw the table.
        textureProgram!!.useProgram()
        textureProgram!!.setUniforms(projectionMatrix, texture)
        table!!.bindData(textureProgram!!)
        table!!.draw()

        // Draw the mallets.
        colorProgram!!.useProgram()
        colorProgram!!.setUniforms(projectionMatrix)
        mallet!!.bindData(colorProgram!!)
        mallet!!.draw()
    }

}
