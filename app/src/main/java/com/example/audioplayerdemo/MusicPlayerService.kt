package com.example.audioplayerdemo

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver

class MusicPlayerService : MediaBrowserServiceCompat() {

    private val ACTION_PLAY = "audioplayerdemo.action.ACTION_PLAY"
    private val ACTION_PAUSE = "audioplayerdemo.action.ACTION_PAUSE"
    private val ACTION_FORWARD = "audioplayerdemo.action.ACTION_FORWARD"
    private val ACTION_REWIND = "audioplayerdemo.action.ACTION_REWIND"

    private lateinit var mediaSessionCompat: MediaSessionCompat
    private lateinit var mediaPlayer: MediaPlayer

    private var timeElapsed = 0
    private val jumpTime = 2000

    private var mediaSessionCallback = object: MediaSessionCompat.Callback() {
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
            mediaPlayer.pause()
        }
    }

    override fun onCreate() {
        super.onCreate()
        initPlayer()
        initMediaSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent != null) {

            if(intent.action!! == ACTION_PLAY) {
                mediaPlayer.start()
            }

            if(intent.action!! == ACTION_PAUSE) {
                mediaPlayer.pause()
            }

            if(intent.action!! == ACTION_REWIND) {
                rewind()
            }

            if(intent.action!! == ACTION_FORWARD) {
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
        mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

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
}