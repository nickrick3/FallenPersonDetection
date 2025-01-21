package com.example.fallenpersondetection

import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity

class AlarmActivity: ComponentActivity() {
    companion object {
        var active : Boolean = false
    }

    private lateinit var alarm : MediaPlayer
    private  var oldVolume : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fall_alarm)
        setShowWhenLocked(true)

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        oldVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            0
        )

        alarm = MediaPlayer.create(this, R.raw.alarm)
        alarm.isLooping = true
        alarm.start()
    }

    override fun onDestroy() {
        super.onDestroy()

        alarm.stop()
        alarm.release()

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            oldVolume,
            0
        )

        active = false
    }

    fun stopAlarm(v: View) {
        finish()
    }
}