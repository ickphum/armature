package com.ickphum.armature

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.setPadding

private const val TAG = "MainActivity"

class MainActivity : Activity() {

    private lateinit var surfaceView: SurfaceView

    override fun onPause() {
        Log.d( TAG, "onPause")
        super.onPause()
        surfaceView.onPause()
    }

    override fun onResume() {
        Log.d( TAG, "onResume")
        super.onResume()
        surfaceView.onResume()
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a GLSurfaceView instance and set it
        // as the ContentView for this Activity.
        surfaceView = SurfaceView(this)
        setContentView(surfaceView)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val wrappedParams = LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT )

        // Create a layout---------------
        val linearLayout = LinearLayout(this)
//        linearLayout.setPadding( 20 )

        val menuBtn = ImageButton(this)
        menuBtn.setImageDrawable( getDrawable( R.drawable.ring_selector ) )
        menuBtn.scaleType = ImageView.ScaleType.CENTER_CROP
        menuBtn.layoutParams = LinearLayout.LayoutParams( 148, 148 )
        menuBtn.setBackgroundColor(Color.TRANSPARENT);
        menuBtn.setPadding( 10 )

        val settingsBtn = ImageButton(this)
        settingsBtn.setImageDrawable( getDrawable( R.drawable.settings_selector ) )
        settingsBtn.scaleType = ImageView.ScaleType.CENTER_CROP
        settingsBtn.layoutParams = LinearLayout.LayoutParams( 148, 148 )
        settingsBtn.setBackgroundColor(Color.TRANSPARENT);
        settingsBtn.setPadding( 10 )
        settingsBtn.visibility = GONE

        menuBtn.setOnClickListener {
            settingsBtn.visibility = if ( settingsBtn.visibility == GONE ) VISIBLE else GONE
        }

        settingsBtn.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
//            intent.putExtra("SHOW_WELCOME", true)
            startActivity(intent)
            finish()
        }

        linearLayout.addView( menuBtn )
        linearLayout.addView( settingsBtn )

        addContentView(linearLayout, wrappedParams)

        /*
                val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val wrappedParams = LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT )

        // Create a layout---------------
        val linearLayout = LinearLayout(this)
        linearLayout.orientation = LinearLayout.VERTICAL

        //----Create a TextView------
        val textView = TextView(this)

        textView.text = "This TextView is dynamically created"
        textView.layoutParams = params

        //--Create A EditText------------------
        val editText = EditText(this)
        editText.layoutParams = params

        //----Create a CheckBox-------------
        val checkBox = CheckBox(this)
        checkBox.layoutParams = params

        //--- Create a RadioGroup---------------
        val radioGroup = RadioGroup(this)
        radioGroup.layoutParams = params

        //--------Create a RadioButton----------
        val radioButton = RadioButton(this)
        radioButton.layoutParams = params

        //-----Create a Button--------
        val button = Button(this)
        button.text = "This Button is dynamically created"
        button.layoutParams = params

        val b1 = Button(this)
        b1.text = getString( R.string.title_activity_main )
        b1.layoutParams = wrappedParams

        val b2 = ImageButton(this)
        b2.setImageDrawable( getDrawable( R.drawable.ring_selector ) )
        b2.scaleType = ImageView.ScaleType.CENTER_CROP
        b2.layoutParams = LinearLayout.LayoutParams( 128, 128 )
        b2.setBackgroundColor(Color.TRANSPARENT);

        b2.setOnClickListener {
            Log.d( "MAIN", "Clicked")
            if ( b1.visibility == GONE )
                b1.visibility = VISIBLE
            else
                b1.visibility = GONE
        }
        val horizontalLayout = LinearLayout(this)
        horizontalLayout.orientation = LinearLayout.HORIZONTAL
        horizontalLayout.layoutTransition = LayoutTransition()

//        val layoutTransition: LayoutTransition = horizontalLayout.layoutTransition
//        layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        horizontalLayout.addView( b2 )
        horizontalLayout.addView( b1 )

        //---Add all elements to the layout

        //---Add all elements to the layout
        linearLayout.addView(textView)
        linearLayout.addView(checkBox)
        linearLayout.addView( horizontalLayout )
        linearLayout.addView(editText)
        linearLayout.addView(radioGroup)
        linearLayout.addView(radioButton)

        linearLayout.addView(button)

        //---Create a layout param for the layout-----------------

        //---Create a layout param for the layout-----------------
        val layoutParams = LinearLayout.LayoutParams(
            ActionBar.LayoutParams.MATCH_PARENT,
            ActionBar.LayoutParams.WRAP_CONTENT
        )
        addContentView(linearLayout, layoutParams)

         */
    }
}
