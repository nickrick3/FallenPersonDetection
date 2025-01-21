package com.example.fallenpersondetection

import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity

class AlarmActivity: ComponentActivity() {
    companion object {
        var active : Boolean = false
    }

    private lateinit var alarm : MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fall_alarm)

        alarm = MediaPlayer.create(this, R.raw.alarm)
        alarm.isLooping = true
    }

    override fun onStart() {
        super.onStart()
        alarm.start()
    }

    override fun onStop() {
        super.onStop()
        if (alarm.isPlaying) {
            alarm.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        alarm.release()
        active = false
    }

    fun stopAlarm(v: View) {
        if (alarm.isPlaying) {
            alarm.stop()
            finish()
        }
    }
}