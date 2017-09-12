package com.deselmo.android.tambourine

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import java.util.*
import android.support.v4.content.LocalBroadcastManager
import java.io.File
import java.text.SimpleDateFormat


class MediaRecorderService : Service() {
    companion object {
        val START: String = "com.deselmo.android.tambourine.MediaRecorderService.START"
        val STOP: String = "com.deselmo.android.MediaRecorderService.STOP"
        val GET_RECORDING_STATE: String =
                "com.deselmo.android.MediaRecorderService.GET_RECORDING_STATE"
        val OPEN_ACTIVITY: String = "com.deselmo.android.MediaRecorderService.OPEN_ACTIVITY"
    }

    private val extension = ".m4a"

    private val dateFormat = SimpleDateFormat("'/'yyyy-MM-dd_HH-mm-ss-SSS' $extension'",
            java.util.Locale.getDefault())

    private var mRecorder: MediaRecorder? = null

    private var cacheFilePath: String? = null

    private var timer: Timer? = null
    private val dateFormatNotification = SimpleDateFormat(": mm:ss",
            java.util.Locale.getDefault())

    private var tempFileName: String? = null

    override fun onCreate() {
        super.onCreate()
        cacheFilePath = externalCacheDir.absolutePath + "/temp" + extension
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        cancelRecording()
        cancelNotification()

        super.onTaskRemoved(rootIntent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent != null) {
            when (intent.action) {
                START -> {
                    if (startRecording()) {
                        showNotification()

                        LocalBroadcastManager.getInstance(this)
                                .sendBroadcast(Intent(TambourineActivity.UPDATE_UI_FILTER)
                                        .putExtra(TambourineActivity.ACTION,
                                                TambourineActivity.START_RECORDING))
                    }
                }

                STOP -> {
                    if (stopRecording()) {
                        cancelNotification()

                        LocalBroadcastManager.getInstance(this)
                                .sendBroadcast(Intent(TambourineActivity.UPDATE_UI_FILTER)
                                        .putExtra(TambourineActivity.ACTION,
                                                TambourineActivity.STOP_RECORDING))

                        LocalBroadcastManager.getInstance(this)
                                .sendBroadcast(Intent(PlayActivity.UPDATE_UI_FILTER)
                                        .putExtra(PlayActivity.ACTION, PlayActivity.FILE_ADDED)
                                        .putExtra(PlayActivity.FILE_ADDED_NAME, tempFileName))
                    }
                }

                GET_RECORDING_STATE -> {
                    LocalBroadcastManager.getInstance(this)
                        .sendBroadcast(Intent(TambourineActivity.UPDATE_UI_FILTER)
                                .putExtra(TambourineActivity.ACTION,
                                        TambourineActivity.UPDATE_RECORDING_STATE)
                                .putExtra(TambourineActivity.UPDATE_RECORDING_STATE_VALUE,
                                        when(mRecorder) { null -> false; else -> true }))
                }

                OPEN_ACTIVITY -> {
                    startActivity(Intent(this,
                            TambourineActivity::class.java))
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }


    private fun startRecording(): Boolean {
        try {
            val tempRecorder = MediaRecorder()
            tempRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
            tempRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            tempRecorder.setOutputFile(cacheFilePath)
            tempRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            tempRecorder.setAudioEncodingBitRate(16)
            tempRecorder.setAudioSamplingRate(44100)
            tempRecorder.prepare()
            tempRecorder.start()

            mRecorder = tempRecorder
            return true
        } catch(e: IllegalStateException) {
            return false
        }
    }

    private fun stopRecording(): Boolean {
        return try {
            mRecorder?.stop()
            mRecorder?.release()
            mRecorder = null

            tempFileName = getExternalFilesDir("Records").absolutePath +
                    dateFormat.format(Calendar.getInstance().time)

            File(cacheFilePath).renameTo(File(tempFileName))

            true
        } catch(e: RuntimeException) {
            mRecorder?.release()
            mRecorder = null

            true
        } catch(e: IllegalStateException) {
            false
        }
    }

    private fun cancelRecording(): Boolean {
        return try {
            mRecorder?.release()
            mRecorder = null

            File(cacheFilePath).delete()

            true
        } catch(e: IllegalStateException) {
            false
        }
    }


    private fun showNotification() {
        val pendingIntentOpen = PendingIntent.getService(this, 0,
                Intent(this, MediaRecorderService::class.java)
                        .setAction(MediaRecorderService.OPEN_ACTIVITY),
                PendingIntent.FLAG_UPDATE_CURRENT)

        val pendingIntentStop = PendingIntent.getService(this, 0,
                Intent(this, MediaRecorderService::class.java)
                        .setAction(MediaRecorderService.STOP),
                PendingIntent.FLAG_UPDATE_CURRENT)

        @Suppress("DEPRECATION")
        val notificationBuilder = NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.audio_length)
                        + dateFormatNotification.format(Date(0)))
                .setSmallIcon(R.drawable.ic_mic_white)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setContentIntent(pendingIntentOpen)
                .addAction(R.drawable.ic_mic_white, getString(R.string.stop),
                        pendingIntentStop)


        NotificationManagerCompat.from(this)
                .notify(0, notificationBuilder.build())

        val timeNotificationStart: Long = Date().time
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                val timeNow: Long = Date().time
                notificationBuilder.setContentText(getString(R.string.audio_length)
                        + dateFormatNotification.format(Date(timeNow - timeNotificationStart)))
                NotificationManagerCompat.from(this@MediaRecorderService)
                        .notify(0, notificationBuilder.build())
            }
        }, 1000, 1000)
    }

    private fun cancelNotification() {
        timer?.cancel()
        timer = null
        NotificationManagerCompat.from(this).cancelAll()
    }
}