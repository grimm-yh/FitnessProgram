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

    fun playThreeStageNotification(onFinished: () -> Unit = {}) {
        wasPlayingBeforeNotification = isBackgroundMusicPlaying()
        if (wasPlayingBeforeNotification) {
            pauseBackgroundMusic()
        }

        playNotificationWithVolume(0.3f, 1) {
            playNotificationWithVolume(0.6f, 2) {
                playNotificationWithVolume(1.0f, 3) {
                    if (wasPlayingBeforeNotification) {
                        startBackgroundMusic()
                    }
                    onFinished()
                }
            }
        }
    }

    private fun playNotificationWithVolume(volume: Float, stage: Int, onComplete: () -> Unit) {
        notificationPlayer?.release()
        notificationPlayer = MediaPlayer.create(context, R.raw.notification).apply {
            setVolume(volume, volume)
            setOnCompletionListener {
                onComplete()
            }
        }
        notificationPlayer?.start()
        vibrate(200 * stage)
    }

    fun playTaskFinishSound(onFinished: () -> Unit = {}) {
        val wasPlaying = isBackgroundMusicPlaying()
        if (wasPlaying) {
            pauseBackgroundMusic()
        }

        notificationPlayer?.release()
        notificationPlayer = MediaPlayer.create(context, R.raw.task_finish).apply {
            setVolume(1.0f, 1.0f)
            setOnCompletionListener {
                if (wasPlaying) {
                    startBackgroundMusic()
                }
                onFinished()
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration.toLong())
        }
    }

    fun release() {
        backgroundPlayer?.release()
        backgroundPlayer = null
        notificationPlayer?.release()
        notificationPlayer = null
    }
}
