package com.ickphum.armature

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import kotlinx.coroutines.Runnable
import java.util.Timer
import java.util.TimerTask
import kotlin.math.pow
import kotlin.math.sqrt

private const val TAG = "SurfaceView"
private const val DRAG_TRIGGER_LENGTH = 15f
private const val LONG_PRESS_TIME = 500L

class SurfaceView(context: Context) : GLSurfaceView(context) {

    private val renderer: com.ickphum.armature.Renderer
    private var previousX: Float = 0f
    private var previousY: Float = 0f

    // touch state tracking
    private var touchTime = 0L
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var dragging = false
    private var gotLongPress = false

    private var timer :Timer? = null

    init {

        // Create an OpenGL ES 3.0 context
        setEGLContextClientVersion(3)

        renderer = Renderer( context )

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(renderer)

        // Render the view only when there is a change in the drawing data.
//        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

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
                previousX = x
                previousY = y

                touchTime = System.nanoTime()
                touchDownX = x
                touchDownY = y
                dragging = false
                gotLongPress = false

                queueEvent(Runnable {
                    val rc = renderer.handleTouchDown( normalizedX, normalizedY )
//                    Log.d( TAG, "touch down rc $rc")

                    // don't give feedback timer if we didn't tap on an item or node
                    if ( rc > 0 )
                        performHapticFeedback( HapticFeedbackConstants.VIRTUAL_KEY )

                    timer = Timer( true )
                    timer!!.schedule(object : TimerTask() {
                        override fun run() {
                            Log.d( TAG, "Timer expired")
                            queueEvent(Runnable {
                                renderer.handleLongPress()
                            })
                            performHapticFeedback( HapticFeedbackConstants.LONG_PRESS )
                            timer = null
                            gotLongPress = true
                        }
                    }, LONG_PRESS_TIME)

                })
            }

            MotionEvent.ACTION_MOVE -> {

                if ( gotLongPress ) return false

                if ( !dragging ) {
                    val displacement = sqrt( ( x - touchDownX ).pow(2) + ( y - touchDownY ).pow(2) )
//                    Log.d( TAG, "displacement $displacement")
                    if ( displacement > DRAG_TRIGGER_LENGTH ) {
                        dragging = true
                        previousX = touchDownX
                        previousY = touchDownY
                        timer?.cancel()
                        timer = null
                        queueEvent(Runnable {
                            renderer.handleDragStart()
                        })
                    }
                }

                if ( dragging ) {
                    val deltaX = previousX - x
                    val deltaY = previousY - y
                    queueEvent(Runnable {
                        renderer.handleDragMove(-deltaX, -deltaY, normalizedX, normalizedY)
                    })
                    previousX = x
                    previousY = y
                }
            }

            MotionEvent.ACTION_UP -> {

                // after a long press has been detected, the up event is ignored
                if (!gotLongPress) {

                    // timer might have been cancelled by a drag event
                    timer?.cancel()
                    timer = null

                    if (dragging)
                        queueEvent(Runnable { renderer.handleDragEnd(normalizedX, normalizedY) })
                    else
                        queueEvent(Runnable { renderer.handleShortPress(normalizedX, normalizedY) })
                }
            }

        }

        return true
    }
}
