package com.dhera.fitnessprogram

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class MusicManager(private val context: Context) {
    private var backgroundPlayer: MediaPlayer? = null
    private var notificationPlayer: MediaPlayer? = null
    private var wasPlayingBeforeNotification = false

    fun startBackgroundMusic() {
        if (backgroundPlayer == null) {
            backgroundPlayer = MediaPlayer.create(context, R.raw.background_music).apply {
                isLooping = true
            }
        }
        if (!backgroundPlayer!!.isPlaying) {
            backgroundPlayer!!.start()
        }
    }

    fun pauseBackgroundMusic() {
        backgroundPlayer?.pause()
    }

    fun isBackgroundMusicPlaying(): Boolean {
        return backgroundPlayer?.isPlaying ?: false
    }

    fun stopAllNotifications() {
        if (notificationPlayer?.isPlaying == true) {
            notificationPlayer?.stop()
            if (wasPlayingBeforeNotification) {
                startBackgroundMusic()
            }
        }
    }

    fun playRestFinishSound() {
        playSingleNotification(R.raw.rest_finish)
    }

    fun playDurationFinishSound() {
        playSingleNotification(R.raw.duration_finish)
    }

    private fun playSingleNotification(resId: Int) {
        wasPlayingBeforeNotification = isBackgroundMusicPlaying()
        if (wasPlayingBeforeNotification) {
            pauseBackgroundMusic()
        }

        notificationPlayer?.release()
        notificationPlayer = MediaPlayer.create(context, resId).apply {
            setVolume(1.0f, 1.0f)
            setOnCompletionListener {
                if (wasPlayingBeforeNotification) {
                    startBackgroundMusic()
                }
            }
        }
        notificationPlayer?.start()
        vibrate(500)
    }

    private fun vibrate(duration: Int) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        vibrator.vibrate(VibrationEffect.createOneShot(duration.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun release() {
        backgroundPlayer?.release()
        backgroundPlayer = null
        notificationPlayer?.release()
        notificationPlayer = null
    }
}
