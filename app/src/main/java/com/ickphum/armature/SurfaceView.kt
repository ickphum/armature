package com.ickphum.armature

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.MotionEvent
import kotlinx.coroutines.Runnable

private const val TOUCH_SCALE_FACTOR: Float = 180.0f / 320f

class SurfaceView(context: Context) : GLSurfaceView(context) {

    private val renderer: com.ickphum.armature.Renderer
    private var previousX: Float = 0f
    private var previousY: Float = 0f

    init {

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2)

        renderer = Renderer()

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(renderer)

        // Render the view only when there is a change in the drawing data.
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        val x: Float = e.x
        val y: Float = e.y

        val normalizedX: Float = x / width * 2 - 1
        val normalizedY: Float = -(y / height * 2 - 1)

        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                queueEvent(Runnable {
                    renderer.handleDownEvent( normalizedX, normalizedY)
                })
            }
            MotionEvent.ACTION_MOVE -> {

//                Log.d( "TOUCH", "move to %.1f, %.1f".format( x, y ))
                queueEvent(Runnable {
                    renderer.handleMoveEvent( normalizedX, normalizedY)
                })

                var dx: Float = x - previousX
                var dy: Float = y - previousY

                // reverse direction of rotation above the mid-line
                if (y > height / 2) {
                    dx *= -1
                }

                // reverse direction of rotation to left of the mid-line
                if (x < width / 2) {
                    dy *= -1
                }

                renderer.angle += (dx + dy) * TOUCH_SCALE_FACTOR
                requestRender()
            }
        }

        previousX = x
        previousY = y
        return true
    }

}