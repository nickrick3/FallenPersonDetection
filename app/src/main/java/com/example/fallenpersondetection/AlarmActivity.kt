package com.example.fallenpersondetection

import android.app.NotificationManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity

class AlarmActivity: ComponentActivity() {
    private lateinit var alarm : MediaPlayer
    private  var oldVolume : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set view
        setContentView(R.layout.fall_alarm)

        // useful for full screen intent
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        // set maximum volume (and store old value)
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        oldVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            0
        )

        // create looping media player
        alarm = MediaPlayer.create(this, R.raw.alarm)
        alarm.isLooping = true
        alarm.start()
    }

    override fun onDestroy() {
        super.onDestroy()

        // stop media player
        alarm.stop()
        alarm.release()

        // restore old volume level
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            oldVolume,
            0
        )

        // dismiss notification from notification bar
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(Constants.NOTIFICATION_ID)

        // notify end of alarm
        AccelerometerService.alarmStarted = false
    }

    // function used by the button
    fun stopAlarm(v: View) {
        finish()
    }
}