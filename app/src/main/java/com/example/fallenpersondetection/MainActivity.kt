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

class MainActivity : Activity(), OnCheckedChangeListener /*, OnClickListener*/ {
    //private lateinit var mAlarmReceiver: AlarmReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTurnScreenOn(true)
        setShowWhenLocked(true)

        /*
        // alarm broadcast listener
        mAlarmReceiver = AlarmReceiver()
        ContextCompat.registerReceiver(
            this,
            mAlarmReceiver,
            IntentFilter("FALL_DETECTED"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

         */

        // set view
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        if (gotPermissions(doRequest = false)) {
            val tw : TextView = findViewById(R.id.permissionGranted)
            tw.visibility = View.VISIBLE
        }
        val detectionSwitch : Switch = findViewById(R.id.detectionSwitch)
        detectionSwitch.isChecked = AccelerometerService.running
        detectionSwitch.setOnCheckedChangeListener(this)
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
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

                // ask for permission
                /*
                AlertDialog.Builder(this)
                    .setMessage("This feature requires over lay permission")
                    .setPositiveButton("OK", this)
                    .setNegativeButton("Cancel", this)
                    .create()
                    .show()
                 */
            }
        }
        else {
            // stop service
            stopService(intent)
        }
    }

    private fun gotPermissions(doRequest : Boolean) : Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            return true

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

    /*
    override fun onClick(dialog: DialogInterface?, which: Int) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            // go to settings
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        }
    }
     */
    /*
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mAlarmReceiver)
    }

     */

    /*
    private class AlarmReceiver () : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!AlarmActivity.active) {
                AlarmActivity.active = true

                val alarmActivity = Intent(context, AlarmActivity::class.java)
                context?.startActivity(alarmActivity)
            }
        }
    }
    */
}