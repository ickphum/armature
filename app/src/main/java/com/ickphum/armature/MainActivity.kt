package com.ickphum.armature

import android.app.Activity
import android.os.Bundle

class MainActivity : Activity() {

    private lateinit var surfaceView: SurfaceView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a GLSurfaceView instance and set it
        // as the ContentView for this Activity.
        surfaceView = SurfaceView(this)
        setContentView(surfaceView)
    }
}
