package com.example.fallenpersondetection

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat

class MainActivity : Activity(), OnCheckedChangeListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set view
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()

        // if user granted permission, make it clear
        if (gotPermissions(doRequest = false)) {
            val tw : TextView = findViewById(R.id.permissionGranted)
            tw.visibility = View.VISIBLE
        }

        // switch button status must be coherent to service status
        val detectionSwitch : Switch = findViewById(R.id.detectionSwitch)
        detectionSwitch.isChecked = AccelerometerService.running
        detectionSwitch.setOnCheckedChangeListener(this)
    }

    // switch button status change
    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        // service intent
        val intent = Intent(this, AccelerometerService::class.java)

        if (isChecked) {
            // check if user has already given permission
            if (gotPermissions(doRequest = true))
            {
                // start service
                startService(intent)
            }
            else {
                // uncheck button
                buttonView?.isChecked = false
            }
        }
        else {
            // stop service
            stopService(intent)
        }
    }

    // return true if user already granted permission
    // otherwise return false and request permissions if 'doRequest' is true
    private fun gotPermissions(doRequest : Boolean) : Boolean {
        // API < 29
        // permission.USE_FULL_SCREEN_INTENT and permission.POST_NOTIFICATIONS don't exist
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            return true

        // API < 33
        // permission.POST_NOTIFICATIONS doesn't exist
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.USE_FULL_SCREEN_INTENT)
                        == PackageManager.PERMISSION_GRANTED)
                return true

            if (doRequest)
                requestPermissions(
                    arrayOf(Manifest.permission.USE_FULL_SCREEN_INTENT),
                    0
                )

            return false
        }

        // API >= 33
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.USE_FULL_SCREEN_INTENT)
                    == PackageManager.PERMISSION_GRANTED) and
            (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED))
            return true

        if (doRequest)
            requestPermissions(
                arrayOf(
                    Manifest.permission.USE_FULL_SCREEN_INTENT,
                    Manifest.permission.POST_NOTIFICATIONS
                ),
                0
            )

        return false
    }
}