package com.example.fallenpersondetection

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.Switch
import android.widget.TextView
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity(), OnCheckedChangeListener, OnClickListener{

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set view
        setContentView(R.layout.activity_main)
        val detectionSwitch : Switch = findViewById(R.id.detectionSwitch)
        detectionSwitch.isChecked = Accelerometer.running
        detectionSwitch.setOnCheckedChangeListener(this)
    }

    override fun onResume() {
        super.onResume()
        if (Settings.canDrawOverlays(this)) {
            val tw : TextView = findViewById(R.id.permissionGranted)
            tw.visibility = View.VISIBLE
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        val intent = Intent(this, Accelerometer::class.java)
        if (isChecked) {
            // check if user has already given permission
            if (Settings.canDrawOverlays(this)) {
                // start service
                startService(intent)
            }
            else {
                // uncheck button
                buttonView?.isChecked = false

                // ask for permission
                AlertDialog.Builder(this)
                    .setMessage("This feature requires over lay permission")
                    .setPositiveButton("OK", this)
                    .setNegativeButton("Cancel", this)
                    .create()
                    .show()
            }
        }
        else {
            // stop service
            stopService(intent)
        }
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            // go to settings
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        }
    }

}