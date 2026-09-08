package com.lunaai.music.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MediaPlayerService : Service(), MediaPlayer.OnCompletionListener {

    private var mediaPlayer: MediaPlayer? = null
    private var currentSongPath: String? = null
    private var isPaused: Boolean = false

    companion object {
        const val ACTION_PLAY = "com.lunaai.music.player.PLAY"
        const val ACTION_PAUSE = "com.lunaai.music.player.PAUSE"
        const val ACTION_STOP = "com.lunaai.music.player.STOP"
        const val EXTRA_SONG_PATH = "EXTRA_SONG_PATH"
        const val CHANNEL_ID = "LunaAIMusicPlaybackChannel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val path = intent.getStringExtra(EXTRA_SONG_PATH)
                if (path != null) {
                    playSong(path)
                } else if (isPaused) {
                    resumeSong()
                }
            }
            ACTION_PAUSE -> pauseSong()
            ACTION_STOP -> stopForeground(true).also { stopSelf() }
        }
        return START_STICKY
    }

    private fun playSong(path: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, Uri.parse(path))
                prepare()
                start()
            }
            currentSongPath = path
            isPaused = false
            mediaPlayer?.setOnCompletionListener(this)
            
            val songName = path.substringAfterLast("/")
            startForeground(NOTIFICATION_ID, createNotification(songName, true))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun pauseSong() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPaused = true
                val songName = currentSongPath?.substringAfterLast("/") ?: "Müzik"
                startForeground(NOTIFICATION_ID, createNotification(songName, false))
            }
        }
    }

    private fun resumeSong() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                isPaused = false
                val songName = currentSongPath?.substringAfterLast("/") ?: "Müzik"
                startForeground(NOTIFICATION_ID, createNotification(songName, true))
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LunaAI Müzik Oynatıcı Servisi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Arka planda kesintisiz müzik çalınmasını sağlar"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(songTitle: String, isPlaying: Boolean): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseIntent = Intent(this, MediaPlayerService::class.java).apply {
            action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        }
        val playPausePendingIntent = PendingIntent.getService(
            this, 0, playPauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, MediaPlayerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val actionTitle = if (isPlaying) "Duraklat" else "Oynat"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LunaAI Müzik")
            .setContentText(songTitle)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, actionTitle, playPausePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Kapat", stopPendingIntent)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle())
            .setOngoing(isPlaying)
            .build()
    }

    override fun onCompletion(mp: MediaPlayer?) {
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}