package com.ickphum.armature

import android.app.Activity
import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import javax.microedition.khronos.opengles.GL10

class MainActivity : Activity() {

    private lateinit var gLView: SurfaceView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a GLSurfaceView instance and set it
        // as the ContentView for this Activity.
        gLView = SurfaceView(this)
        setContentView(gLView)
    }
}
