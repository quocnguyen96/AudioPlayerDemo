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
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val ACTUAL_TIME_VALUE_EXTRA_1 = ""

class MusicPlayerService : MediaBrowserServiceCompat() {

    private val NOTIFICATION_ID = 345

    private val ACTION_PLAY = "audioplayerdemo.action.ACTION_PLAY"
    private val ACTION_PAUSE = "audioplayerdemo.action.ACTION_PAUSE"
    private val ACTION_FORWARD = "audioplayerdemo.action.ACTION_FORWARD"
    private val ACTION_REWIND = "audioplayerdemo.action.ACTION_REWIND"
    private val ACTION_SEEK_TO = "audioplayerdemo.action.ACTION_SEEK_TO"

    private val EXTRA_TIME = "audioplayerdemo.action.EXTRA_TIME"

    val GUI_UPDATE_ACTION = "GUI_UPDATE_ACTION"
    val INIT_PLAYER_ACTION = "INIT_PLAYER_ACTION"
    val ACTUAL_TIME_VALUE_EXTRA = "ACTUAL_TIME_VALUE_EXTRA"
    val TOTAL_TIME_VALUE_EXTRA = "TOTAL_TIME_VALUE_EXTRA"
    val PLAY_ACTION = "PLAY_ACTION"
    val PAUSE_ACTION = "PAUSE_ACTION"
    val DELETE_ACTION = "DELETE_ACTION"
    val COMPLETE_ACTION = "DELETE_ACTION"
    val FORWARD_ACTION = "FORWARD_ACTION"
    val REWIND_ACTION = "REWIND"

    private lateinit var mediaSessionCompat: MediaSessionCompat

    private lateinit var mHandler: Handler

    private var timeElapsed = 0
    private val jumpTime = 2000
    private var isUpdatingThread = false

    private val binder = Binder()

    companion object {
        var mediaPlayer: MediaPlayer? = MediaPlayer()
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

        mHandler = Handler(Looper.getMainLooper())

        if (intent != null) {

            if (intent.action!! == ACTION_PLAY) {
                play()
            }

            if (intent.action!! == ACTION_PAUSE) {
                mediaPlayer?.pause()
            }

            if (intent.action!! == ACTION_REWIND) {
                rewind()
            }

            if (intent.action!! == ACTION_FORWARD) {
                forward()

            } else {
                MediaButtonReceiver.handleIntent(mediaSessionCompat, intent)
            }

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
        val updateIntent = Intent()
        mediaPlayer?.setOnCompletionListener {
            updateIntent.action = COMPLETE_ACTION
            sendBroadcast(updateIntent)
        }
        updateIntent.action = INIT_PLAYER_ACTION
        updateIntent.putExtra(TOTAL_TIME_VALUE_EXTRA, mediaPlayer?.duration)
        sendBroadcast(updateIntent)
    }

    private fun forward() {
        mediaPlayer?.let {
            if (timeElapsed + jumpTime <= it.duration) {
                timeElapsed += jumpTime
                mediaPlayer?.seekTo(timeElapsed)
            }
        }

    }

    private fun rewind() {
        if (timeElapsed - jumpTime > 0) {
            timeElapsed -= jumpTime
            mediaPlayer?.seekTo(timeElapsed)
        }
    }

    private fun play() {
        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)
        mediaSessionCompat.isActive = true
        mediaPlayer?.start()
        startUiUpdate()
        val updateIntent = Intent()
        updateIntent.action = PLAY_ACTION
        sendBroadcast(updateIntent)
        makeNotification()

    }

    private fun pause() {
        mediaPlayer?.pause()
        setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED)
        isUpdatingThread = false
        val updateIntent = Intent()
        updateIntent.action = PAUSE_ACTION
        sendBroadcast(updateIntent)
        makeNotification()
    }

    private fun seekTo(time: Int) {
        mediaPlayer?.seekTo(time)
        val updateIntent = Intent()
        updateIntent.action = GUI_UPDATE_ACTION
        updateIntent.putExtra(ACTUAL_TIME_VALUE_EXTRA, mediaPlayer?.currentPosition)
        sendBroadcast(updateIntent)
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
        CoroutineScope(Dispatchers.Main).launch {
            val guiUpdateIntent = Intent()
            guiUpdateIntent.action = GUI_UPDATE_ACTION
            delay(50)
            while (isUpdatingThread) {
                guiUpdateIntent.putExtra(ACTUAL_TIME_VALUE_EXTRA, mediaPlayer?.currentPosition)
                sendBroadcast(guiUpdateIntent)
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

        val deleteIntent =
            Intent(this, MusicPlayerService::class.java)
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

        val mNotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        if (mediaPlayer.isPlaying) {
            startForeground(NOTIFICATION_ID, builder?.build())
//        } else {
//            stopForeground(false)
//            mNotificationManager.notify(NOTIFICATION_ID, builder?.build())
//        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startMyOwnForeground() {
        val NOTIFICATION_CHANNEL_ID = "ServiceChanelId"
        val channelName = "My Background Service"
        val chan = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager =
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(chan)

        val notificationBuilder = MediaStyleHelper().from(this, mediaSessionCompat)
        val notification: Notification = notificationBuilder!!.setOngoing(true)
            .setSmallIcon(R.drawable.ic_music_note)
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(2, notification)
    }

    private fun cancelNotification() {
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.cancel(NOTIFICATION_ID)
    }

    fun startActionPlay(context: Context) {
        val intent =
            Intent(context, MusicPlayerService::class.java)
        intent.action = ACTION_PLAY
        context.startService(intent)
    }

    fun startActionPause(context: Context) {
        val intent =
            Intent(context, MusicPlayerService::class.java)
        intent.action = ACTION_PAUSE
        context.startService(intent)
    }

    fun startActionSeekTo(context: Context, time: Int) {
        val intent =
            Intent(context, MusicPlayerService::class.java)
        intent.action = ACTION_SEEK_TO
        intent.putExtra(EXTRA_TIME, time)
        context.startService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        isUpdatingThread = false
        stopForeground(true)
        cancelNotification()
        mediaSessionCompat.release()
        mediaSessionCompat.isActive = false
    }
}