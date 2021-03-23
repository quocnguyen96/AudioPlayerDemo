package com.example.audioplayerdemo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.*
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MusicPlayerService : MediaBrowserServiceCompat() {

    private lateinit var mediaSessionCompat: MediaSessionCompat

    private var timeElapsed = 0
    private val jumpTime = 2000
    private var isUpdatingThread = false

    private val binder = Binder()

    companion object {
        var mediaPlayer: MediaPlayer? = MediaPlayer()
        val playStatus: MutableLiveData<Boolean> = MutableLiveData()
        val totalTime: MutableLiveData<Int> = MutableLiveData()
        val actualTime: MutableLiveData<Int> = MutableLiveData()
    }

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
            mediaPlayer?.stop()
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

        when (intent?.action) {
            ACTION_PLAY -> play()
            ACTION_PAUSE -> pause()
            ACTION_REWIND -> rewind()
            ACTION_FORWARD -> forward()
            else -> MediaButtonReceiver.handleIntent(mediaSessionCompat, intent)
        }

        return START_NOT_STICKY
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        TODO("Not yet implemented")
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        TODO("Not yet implemented")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return if (SERVICE_INTERFACE == intent!!.action) {
            super.onBind(intent)
        } else binder
    }

    private fun initPlayer() {
        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.music)
        mediaPlayer?.setOnCompletionListener {
//            playStatus.postValue(false)
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED)
//            mediaPlayer?.reset()
        }
        totalTime.postValue(mediaPlayer?.duration)
    }

    private fun forward() {
        mediaPlayer?.let {
            timeElapsed = it.currentPosition
            if (timeElapsed + jumpTime <= it.duration) {
                timeElapsed += jumpTime
                seekTo(timeElapsed)
            }
        }

    }

    private fun rewind() {
        mediaPlayer?.let {
            timeElapsed = it.currentPosition
            if (timeElapsed - jumpTime > 0) {
                timeElapsed -= jumpTime
                seekTo(timeElapsed)
            }
        }
    }

    private fun play() {
        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)
        mediaSessionCompat.isActive = true
        mediaPlayer?.start()
        startUiUpdate()
        playStatus.postValue(true)
        makeNotification()
    }

    private fun pause() {
        mediaPlayer?.pause()
        setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED)
        isUpdatingThread = false
        makeNotification()
        playStatus.postValue(false)
    }

    private fun seekTo(time: Int) {
        mediaPlayer?.seekTo(time)
        actualTime.postValue(mediaPlayer?.currentPosition)
    }

    private fun setMediaPlaybackState(state: Int) {
        val position = mediaPlayer?.currentPosition?.toLong()
        val playbackStateCompat = position?.let {
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE or
                            PlaybackStateCompat.ACTION_FAST_FORWARD or PlaybackStateCompat.ACTION_REWIND or
                            PlaybackStateCompat.ACTION_PAUSE
                )
                .setState(state, it, 1f, SystemClock.elapsedRealtime())
                .build()
        }
        mediaSessionCompat.setPlaybackState(playbackStateCompat)
    }

    private fun initMediaSession() {
        val mediaButtonReceiver = ComponentName(this, MediaButtonReceiver::class.java)
        mediaSessionCompat = MediaSessionCompat(applicationContext, "MediaTAG", mediaButtonReceiver, null)
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
        CoroutineScope(Dispatchers.Main).launch {
            delay(50)
            while (isUpdatingThread) {
                actualTime.postValue(mediaPlayer?.currentPosition)
                delay(200)
            }
        }
    }

    private fun makeNotification() {
        val builder: NotificationCompat.Builder? = MediaStyleHelper().from(this, mediaSessionCompat)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val NOTIFICATION_CHANNEL_ID = "ServiceChanelId"
            val channelName = "My Background Service"
            val chan = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_NONE
            )
            chan.lightColor = Color.BLUE
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val manager = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            manager.createNotificationChannel(chan)
        }
        builder?.setSmallIcon(R.drawable.ic_music_note)

        val pplayIntent: PendingIntent? =  mediaPlayer?.let {
            if (it.isPlaying) {
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PAUSE)
            } else {
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY)
            }
        }

        builder?.addAction(R.drawable.ic_rewind, "Rewind", MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_REWIND
            )
        )

        mediaPlayer?.let {
            if (it.isPlaying) {
                builder?.addAction(R.drawable.ic_pause, "Pause", pplayIntent)
            } else {
                builder?.addAction(R.drawable.ic_play, "Play", pplayIntent)
            }
        }

        builder?.addAction(R.drawable.ic_forward, "Forward", MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_FAST_FORWARD
            )
        )

        val deleteIntent = Intent(this, MusicPlayerService::class.java)
        deleteIntent.action = DELETE_ACTION
        val pdeleteIntent = PendingIntent.getService(
            this, 0,
            deleteIntent, 0
        )

        builder?.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
                .setMediaSession(mediaSessionCompat.sessionToken)
                .setCancelButtonIntent(
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_STOP
                    )
                )
        )
        builder?.setDeleteIntent(pdeleteIntent)

        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.flags = (Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val intent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, 0
        )
        builder?.setContentIntent(intent)

        startForeground(NOTIFICATION_ID, builder?.build())
//        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        mediaPlayer?.let {
//            if (!it.isPlaying) {
//                stopForeground(false)
//                mNotificationManager.notify(NOTIFICATION_ID, builder?.build())
//            }
//        }
    }

    private fun cancelNotification() {
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.cancel(NOTIFICATION_ID)
    }

    fun startActionPlay(context: Context) {
        val intent = Intent(context, MusicPlayerService::class.java)
        intent.action = ACTION_PLAY
        context.startService(intent)
    }

    fun startActionPause(context: Context) {
        val intent = Intent(context, MusicPlayerService::class.java)
        intent.action = ACTION_PAUSE
        context.startService(intent)
    }

    fun startActionRewind(context: Context) {
        val intent = Intent(context, MusicPlayerService::class.java)
        intent.action = ACTION_REWIND
        context.startService(intent)
    }

    fun startActionForward(context: Context) {
        val intent = Intent(context, MusicPlayerService::class.java)
        intent.action = ACTION_FORWARD
        context.startService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        isUpdatingThread = false
        stopForeground(true)
//        cancelNotification()
        mediaSessionCompat.release()
        mediaSessionCompat.isActive = false
    }
}