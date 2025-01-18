package com.example.fallenpersondetection

import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity

class AlarmActivity: ComponentActivity() {
    private val mRingtoneManager = RingtoneManager(this)
    private val alarm = mRingtoneManager.getRingtone(R.raw.alarm)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fall_alarm)
        alarm.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    }

    override fun onStart() {
        super.onStart()
        alarm.play()
    }

    override fun onStop() {
        super.onStop()
        if (alarm.isPlaying) {
            alarm.stop()
        }
    }

    fun stopAlarm(v: View) {
        if (alarm.isPlaying) {
            alarm.stop()
        }
    }
}