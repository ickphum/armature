package com.ickphum.armature

import android.R.attr
import android.content.Context
//import android.opengl.GLES20.GL_COLOR_BUFFER_BIT
//import android.opengl.GLES20.GL_COMPILE_STATUS
//import android.opengl.GLES20.GL_FLOAT
//import android.opengl.GLES20.GL_FRAGMENT_SHADER
//import android.opengl.GLES20.GL_LINES
//import android.opengl.GLES20.GL_LINK_STATUS
//import android.opengl.GLES20.GL_NO_ERROR
//import android.opengl.GLES20.GL_POINTS
//import android.opengl.GLES20.GL_TRIANGLE_FAN
//import android.opengl.GLES20.GL_TRUE
//import android.opengl.GLES20.GL_VERTEX_SHADER
//import android.opengl.GLES20.glAttachShader
//import android.opengl.GLES20.glClear
//import android.opengl.GLES20.glClearColor
//import android.opengl.GLES20.glCompileShader
//import android.opengl.GLES20.glCreateProgram
//import android.opengl.GLES20.glCreateShader
//import android.opengl.GLES20.glDeleteShader
//import android.opengl.GLES20.glDetachShader
//import android.opengl.GLES20.glDrawArrays
//import android.opengl.GLES20.glEnableVertexAttribArray
//import android.opengl.GLES20.glGetAttribLocation
//import android.opengl.GLES20.glGetError
//import android.opengl.GLES20.glGetProgramInfoLog
//import android.opengl.GLES20.glGetProgramiv
//import android.opengl.GLES20.glGetShaderInfoLog
//import android.opengl.GLES20.glGetShaderiv
//import android.opengl.GLES20.glGetUniformLocation
//import android.opengl.GLES20.glLinkProgram
//import android.opengl.GLES20.glShaderSource
//import android.opengl.GLES20.glUniformMatrix4fv
//import android.opengl.GLES20.glUseProgram
//import android.opengl.GLES20.glVertexAttribPointer
//import android.opengl.GLES20.glViewport
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.Matrix.orthoM
import android.opengl.Matrix.*
//import android.opengl.Matrix.translateM
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL10


private const val TAG = "ArmRenderer"

private const val BYTES_PER_FLOAT = 4
private const val POSITION_COMPONENT_COUNT = 2
private const val COLOR_COMPONENT_COUNT = 3

private const val A_POSITION = "a_Position"
private const val A_COLOR = "a_Color"
private const val U_MATRIX = "u_Matrix"
private const val STRIDE: Int = (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) * BYTES_PER_FLOAT

class Renderer(context: Context) : GLSurfaceView.Renderer {

//    private lateinit var mTriangle: Triangle
//    // vPMatrix is an abbreviation for "Model View Projection Matrix"
//    private val vPMatrix = FloatArray(16)
//    private val projectionMatrix = FloatArray(16)
//    private val viewMatrix = FloatArray(16)
//    private val rotationMatrix = FloatArray(16)
//    private val invertedVPMatrix = FloatArray(16)
//    private var malletPressed: Boolean = false

//    @Volatile
//    var angle: Float = 0f

    private var vertexData: FloatBuffer
    private val context: Context = context
    private var shaderProgramHandle: Int = 0
    private val projectionMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    private var aColorLocation = 0
    private var aPositionLocation = 0
    private var uMatrixLocation = 0

    init {
        var tableVerticesWithTriangles = floatArrayOf(
// Order of coordinates: X, Y, Z, W, R, G, B
            0f,
            0f,
            1f,
            1f,
            1f,
            -0.5f, -0.8f, 0.7f, 0.7f, 0.7f,
            0.5f, -0.8f, 0.7f, 0.7f, 0.7f,
            0.5f, 0.8f, 0.7f, 0.7f, 0.7f,
            -0.5f, 0.8f, 0.7f, 0.7f, 0.7f,
            -0.5f, -0.8f, 0.7f, 0.7f, 0.7f,
// Line 1
            -0.5f, 0f, 1f, 0f, 0f,
            0.5f, 0f, 1f, 0f, 0f,
// Mallets
            0f, -0.4f, 0f, 0f, 1f,
            0f, 0.4f, 1f, 0f, 0f
        )

        vertexData =
            // (number of coordinate values * 4 bytes per float)
            ByteBuffer.allocateDirect(tableVerticesWithTriangles.size * BYTES_PER_FLOAT).run {
                // use the device hardware's native byte order
                order(ByteOrder.nativeOrder())

                // create a floating point buffer from the ByteBuffer
                asFloatBuffer().apply {
                    // add the coordinates to the FloatBuffer
                    put(tableVerticesWithTriangles)
                    // set the buffer to read the first coordinate
                    position(0)
                }
            }
    }

    override fun onSurfaceCreated(p0: GL10?, p1: javax.microedition.khronos.egl.EGLConfig?) {
        glClearColor(0.0f, 0.5f, 0.5f, 1.0f)
        initializeShaders()
        aColorLocation = glGetAttribLocation(shaderProgramHandle, A_COLOR);
        aPositionLocation = glGetAttribLocation(shaderProgramHandle, A_POSITION)
        uMatrixLocation = glGetUniformLocation(shaderProgramHandle, U_MATRIX)

        glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT, GL_FLOAT,
            false, STRIDE, vertexData)
        glEnableVertexAttribArray(aPositionLocation)

        vertexData.position(POSITION_COMPONENT_COUNT);
        glVertexAttribPointer(aColorLocation, COLOR_COMPONENT_COUNT, GL_FLOAT,
            false, STRIDE, vertexData);
        glEnableVertexAttribArray(aColorLocation);
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

    override fun onDrawFrame(unused: GL10) {

//        val scratch = FloatArray(16)

        // Redraw background color
        glClear(GL_COLOR_BUFFER_BIT)
        glUniformMatrix4fv(uMatrixLocation, 1, false, projectionMatrix, 0);

        glDrawArrays(GL_TRIANGLE_FAN, 0, 6);

        glDrawArrays(GL_LINES, 6, 2);

        glDrawArrays(GL_POINTS, 8, 1);
        glDrawArrays(GL_POINTS, 9, 1);

//        // Set the camera position (View matrix)
//        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)
//
//        // Create a rotation transformation for the triangle
////        val time = SystemClock.uptimeMillis() % 4000L
////        val angle = 0.090f * time.toInt()
//        Matrix.setRotateM(rotationMatrix, 0, angle, 0f, 0f, -1.0f)
//
//        // Calculate the projection and view transformation
//        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
//
//        // Combine the rotation matrix with the projection and camera view
//        // Note that the vPMatrix factor *must be first* in order
//        // for the matrix multiplication product to be correct.
//        Matrix.multiplyMM(scratch, 0, vPMatrix, 0, rotationMatrix, 0)
//
//        // invert matrix for touch interpretation
//        Matrix.invertM(invertedVPMatrix, 0, scratch, 0);
//
//        mTriangle.draw( scratch )
    }



    private fun divideByW(vector: FloatArray) {
        vector[0] /= vector[3]
        vector[1] /= vector[3]
        vector[2] /= vector[3]
    }

//    private fun convertNormalized2DPointToRay(
//        normalizedX: Float, normalizedY: Float
//    ): Geometry.Ray {
//        // We'll convert these normalized device coordinates into world-space
//        // coordinates. We'll pick a point on the near and far planes, and draw a
//        // line between them. To do this transform, we need to first multiply by
//        // the inverse matrix, and then we need to undo the perspective divide.
//        val nearPointNdc = floatArrayOf(normalizedX, normalizedY, -1f, 1f)
//        val farPointNdc = floatArrayOf(normalizedX, normalizedY, 1f, 1f)
//        val nearPointWorld = FloatArray(4)
//        val farPointWorld = FloatArray(4)
//        Matrix.multiplyMV(
//            nearPointWorld, 0, invertedVPMatrix, 0, nearPointNdc, 0
//        )
//        Matrix.multiplyMV(
//            farPointWorld, 0, invertedVPMatrix, 0, farPointNdc, 0
//        )
//
//        // Why are we dividing by W? We multiplied our vector by an inverse
//        // matrix, so the W value that we end up is actually the *inverse* of
//        // what the projection matrix would create. By dividing all 3 components
//        // by W, we effectively undo the hardware perspective divide.
//        divideByW(nearPointWorld)
//        divideByW(farPointWorld)
//
//        // We don't care about the W value anymore, because our points are now
//        // in world coordinates.
//        val nearPointRay = Geometry.Point(nearPointWorld[0], nearPointWorld[1], nearPointWorld[2])
//        val farPointRay = Geometry.Point(farPointWorld[0], farPointWorld[1], farPointWorld[2])
//        return Geometry.Ray(
//            nearPointRay,
//            Geometry.vectorBetween(nearPointRay, farPointRay)
//        )
//    }

    fun handleMoveEvent(normalizedX: Float, normalizedY: Float) {
//        Log.d( tag, "handleMoveEvent %.1f, %.1f".format( normalizedX, normalizedY ))
    }

    fun handleDownEvent(normalizedX: Float, normalizedY: Float) {
        Log.d( TAG, "handleDownEvent %.1f, %.1f".format( normalizedX, normalizedY ))

//        val ray: Geometry.Ray = convertNormalized2DPointToRay(normalizedX, normalizedY)
//
//        val boundingSphere = Sphere( Geometry.Point(0.5f, 0.5f, 0f),0.2f )
//
//        malletPressed = Geometry.intersects(boundingSphere, ray);
//        if ( malletPressed )
//            Log.d( tag, "Pressed!")
    }


    private fun initializeShaders() {
        val dbgDomain = "Initializing shaders"

        //Create the vertex shader:
        val vertexShaderHandle = createShader(GL_VERTEX_SHADER, "simple_vertex_shader.glsl")

        //Create the fragment shader:
        val fragmentShaderHandle = createShader(GL_FRAGMENT_SHADER, "simple_fragment_shader.glsl")

        //Create the program:
        this.shaderProgramHandle = glCreateProgram()
        checkError(dbgDomain, "Failed to generate shader program handle")

        //Attach the shaders:
        glAttachShader(this.shaderProgramHandle, vertexShaderHandle)
        checkError(dbgDomain, "Failed to attach vertex shader")

        glAttachShader(this.shaderProgramHandle, fragmentShaderHandle)
        checkError(dbgDomain, "Failed to attach fragment shader")

        //Link the program:
        glLinkProgram(this.shaderProgramHandle)
        checkError(dbgDomain, "Failed to link shader program")

        //Check if we had success:
        val linkingSuccess = IntArray(1)

        glGetProgramiv(this.shaderProgramHandle, GL_LINK_STATUS, linkingSuccess, 0)
        checkError(dbgDomain, "Failed to retrieve shader program parameter")

        if (linkingSuccess[0] != GL_TRUE) {
            //Retrieve the error message:
            val errorMessage = glGetProgramInfoLog(this.shaderProgramHandle)
            checkError(dbgDomain, "Failed to retrieve shader program info log")

            //Print it and fail:
            Log.d(dbgDomain, errorMessage)
            assert(true)
        }

        //After we have linked the program, it's a good idea to detach the shaders from it:
        glDetachShader(this.shaderProgramHandle, vertexShaderHandle)
        checkError(dbgDomain, "Failed to detach vertex shader")

        glDetachShader(this.shaderProgramHandle, fragmentShaderHandle)
        checkError(dbgDomain, "Failed to detach fragment shader")

        //We don't need the shaders anymore, so we can delete them right here:
        glDeleteShader(vertexShaderHandle)
        checkError(dbgDomain, "Failed to devale vertex shader")

        glDeleteShader(fragmentShaderHandle)
        checkError(dbgDomain, "Failed to devale fragment shader")

        //Use our program from now on:
        glUseProgram(this.shaderProgramHandle)
        checkError(dbgDomain, "Failed to enable shader program")
    }
    
    private fun createShader(shaderType: Int, sourceFileName: String): Int {
        val dbgDomain = "Creating shader"

        //Create a shader of our type:
        val shaderHandle = glCreateShader(shaderType)
        checkError(dbgDomain, "Failed to generate shader handle")

        //Get a null-terminated raw char pointer to the source:
        val rawText = this.context.assets.open(sourceFileName)
        val shaderString = rawText.bufferedReader().use { it.readText() }

        //Pass the shader source down to GLES:
        glShaderSource(shaderHandle, shaderString)
        checkError(dbgDomain, "Failed to provide shader source code")

        //Compile the shader:
        glCompileShader(shaderHandle)
        checkError(dbgDomain, "Failed to compile shader")

        //Check if we had success:
        val compilationSuccess = IntArray(1)

        glGetShaderiv(shaderHandle, GL_COMPILE_STATUS, compilationSuccess, 0)
        checkError(dbgDomain, "Failed to retrieve shader parameter")

        if (compilationSuccess[0] != GL_TRUE) {
            //Retrieve the error message:
            val errorMessageRaw = glGetShaderInfoLog(shaderHandle)
            checkError(dbgDomain, "Failed to retrieve shader info log")

            //Print it and fail:
            Log.d(dbgDomain, errorMessageRaw)
            assert(true)
        }

        //Return the shader handle:
        return shaderHandle
    }
    companion object {
        fun checkError(dbgDomain: String, dbgText: String) {

            val error = glGetError()

            if (error != GL_NO_ERROR) {
                Log.d(dbgDomain, dbgText)
            }
        }
    }


}
