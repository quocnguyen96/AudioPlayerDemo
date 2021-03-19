package com.example.audioplayerdemo

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MusicPlayerService : MediaBrowserServiceCompat() {

    private val ACTION_PLAY = "audioplayerdemo.action.ACTION_PLAY"
    private val ACTION_PAUSE = "audioplayerdemo.action.ACTION_PAUSE"
    private val ACTION_FORWARD = "audioplayerdemo.action.ACTION_FORWARD"
    private val ACTION_REWIND = "audioplayerdemo.action.ACTION_REWIND"

    val GUI_UPDATE_ACTION = "GUI_UPDATE_ACTION"
    val ACTUAL_TIME_VALUE_EXTRA = "ACTUAL_TIME_VALUE_EXTRA"
    val PLAY_ACTION = "PLAY_ACTION"
    val PAUSE_ACTION = "PAUSE_ACTION"

    private lateinit var mediaSessionCompat: MediaSessionCompat
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var mHandler: Handler

    private var timeElapsed = 0
    private val jumpTime = 2000
    private var isUpdatingThread = false

    private var mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onRewind() {
            super.onRewind()
            rewind()
        }

        override fun onFastForward() {
            super.onFastForward()
            forward()
        }

        override fun onPlay() {
            super.onPlay()
            play()
        }

        override fun onStop() {
            super.onStop()
            mediaPlayer.stop()
        }

        override fun onPause() {
            super.onPause()
            pause()
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            seekTo(pos.toInt())
        }
    }

    override fun onCreate() {
        super.onCreate()
        initPlayer()
        initMediaSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        mHandler = Handler(Looper.getMainLooper())

        if (intent != null) {

            if (intent.action!! == ACTION_PLAY) {
                play()
            }

            if (intent.action!! == ACTION_PAUSE) {
                mediaPlayer.pause()
            }

            if (intent.action!! == ACTION_REWIND) {
                rewind()
            }

            if (intent.action!! == ACTION_FORWARD) {
                forward()
            }

        }

        return START_NOT_STICKY
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        TODO("Not yet implemented")
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        TODO("Not yet implemented")
    }

    private fun initPlayer() {
        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.music)
    }

    private fun forward() {
        if (timeElapsed + jumpTime <= mediaPlayer.duration) {
            timeElapsed += jumpTime
            mediaPlayer.seekTo(timeElapsed)
        }
    }

    private fun rewind() {
        if (timeElapsed - jumpTime > 0) {
            timeElapsed -= jumpTime
            mediaPlayer.seekTo(timeElapsed)
        }
    }

    private fun play() {
        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)
        mediaSessionCompat.isActive = true
        mediaPlayer.start()
        startUiUpdate()
        val updateIntent = Intent()
        updateIntent.action = PLAY_ACTION
        sendBroadcast(updateIntent)
//        makeNotification()

    }

    private fun pause() {
        mediaPlayer.pause()
        setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED)
        isUpdatingThread = false
        val updateIntent = Intent()
        updateIntent.action = PAUSE_ACTION
        sendBroadcast(updateIntent)
//        makeNotification()
    }

    private fun seekTo(time: Int){
        mediaPlayer.seekTo(time)
        val updateIntent = Intent()
        updateIntent.action = GUI_UPDATE_ACTION
        updateIntent.putExtra(ACTUAL_TIME_VALUE_EXTRA, mediaPlayer.currentPosition)
        sendBroadcast(updateIntent)
    }

    private fun setMediaPlaybackState(state: Int) {
        val position = mediaPlayer.currentPosition.toLong()
        val playbackStateCompat = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_FAST_FORWARD or PlaybackStateCompat.ACTION_REWIND or
                        PlaybackStateCompat.ACTION_PAUSE
            )
            .setState(state, position, 1f, SystemClock.elapsedRealtime())
            .build()
        mediaSessionCompat.setPlaybackState(playbackStateCompat)
    }

    private fun initMediaSession() {
        val mediaButtonReceiver =
            ComponentName(this, MediaButtonReceiver::class.java)
        mediaSessionCompat =
            MediaSessionCompat(applicationContext, "MediaTAG", mediaButtonReceiver, null)

        mediaSessionCompat.setCallback(mediaSessionCallback)
        mediaSessionCompat.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )

        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 99 /*request code*/,
            intent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        mediaSessionCompat.setSessionActivity(pi)

        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        mediaButtonIntent.setClass(this, MediaButtonReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0)
        mediaSessionCompat.setMediaButtonReceiver(pendingIntent)

        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)
        mediaSessionCompat.isActive = true
        sessionToken = mediaSessionCompat.sessionToken
    }

    private fun startUiUpdate() {
        isUpdatingThread = true
        CoroutineScope(Dispatchers.IO).launch {
            val guiUpdateIntent = Intent()
            guiUpdateIntent.action = GUI_UPDATE_ACTION
            delay(50)
            while (isUpdatingThread) {
                guiUpdateIntent.putExtra(ACTUAL_TIME_VALUE_EXTRA, mediaPlayer.currentPosition)
                sendBroadcast(guiUpdateIntent)
                delay(200)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        isUpdatingThread = false
        stopForeground(true)
//        cancelNotification()
        mediaSessionCompat.release()
        mediaSessionCompat.isActive = false
    }
}