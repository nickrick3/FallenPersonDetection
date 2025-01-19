package com.example.fallenpersondetection

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.Switch
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity(), OnCheckedChangeListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set view
        setContentView(R.layout.activity_main)
        val detectionSwitch : Switch = findViewById(R.id.detectionSwitch)
        detectionSwitch.isChecked = Accelerometer.running
        detectionSwitch.setOnCheckedChangeListener(this)
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        val intent = Intent(this, Accelerometer::class.java)
        if (isChecked) {
            startService(intent)
        }
        else {
            stopService(intent)
        }
    }
}