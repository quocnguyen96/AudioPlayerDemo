package com.example.audioplayerdemo

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    //    private val durationHandler = Handler(Looper.getMainLooper())
    private val handler = Handler(Looper.getMainLooper())

    //    private var timeElapsed = 0
//    private val jumpTime = 2000
    private var blockGUIUpdate = false
    private val receiver = GuiReceiver()

//    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        volumeControlStream = AudioManager.STREAM_MUSIC
//        initPlayer()
        eventListener()
    }

    private fun eventListener() {
        btn_playpause.setOnClickListener { playPause() }
//        btn_forward.setOnClickListener { forward() }
//        btn_rewind.setOnClickListener { rewind() }
//        mediaPlayer.setOnCompletionListener {
//            mediaPlayer.seekTo(0)
//            mediaPlayer.pause()
//            playPauseToggle()
//        }


        sb_music.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            var time = 0
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                time = progress
                if (fromUser) MusicPlayerService.mediaPlayer?.seekTo(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                blockGUIUpdate = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                unblockGUIUpdate()
                setTime(time)
            }

        })
    }

    private fun unblockGUIUpdate() {
        handler.postDelayed({ blockGUIUpdate = false }, 150)
    }

    private fun setTime(time: Int) {
        MusicPlayerService().startActionSeekTo(this, time * 1000)
    }

//    private fun initPlayer() {
//        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.music)
//        tv_total_time.text = mediaPlayer.duration.toTime()
//        sb_music.max = mediaPlayer.duration
//    }

    private fun playPause() {
        MusicPlayerService.mediaPlayer?.let {
            if (it.isPlaying) {
                MusicPlayerService().startActionPause(this)
                btn_playpause.setBackgroundResource(R.drawable.ic_play)
            } else {
                MusicPlayerService().startActionPlay(this)
                btn_playpause.setBackgroundResource(R.drawable.ic_pause)
            }
        }
    }

//    private fun forward() {
//        if (timeElapsed + jumpTime <= mediaPlayer.duration) {
//            timeElapsed += jumpTime
//            mediaPlayer.seekTo(timeElapsed)
//        }
//    }
//
//    private fun rewind() {
//        if (timeElapsed - jumpTime > 0) {
//            timeElapsed -= jumpTime
//            mediaPlayer.seekTo(timeElapsed)
//        }
//    }
//
//    private fun playPauseToggle() {
//        if (mediaPlayer.isPlaying){
//            btn_playpause.setBackgroundResource(R.drawable.ic_pause)
//        }
//        else {
//            btn_playpause.setBackgroundResource(R.drawable.ic_play)
//        }
//    }

    fun toTime(time: Int): String {
        val minute = TimeUnit.MILLISECONDS.toMinutes(time.toLong())
        val second = TimeUnit.MILLISECONDS.toSeconds(time.toLong()) - TimeUnit.MINUTES.toSeconds(
            TimeUnit.MILLISECONDS.toMinutes(time.toLong())
        )
        return String.format("%02d:%02d", minute, second)
    }

//    private val updateSeekBarTime: Runnable = object : Runnable {
//        override fun run() {
//
//            //get current position
//            timeElapsed = mediaPlayer.currentPosition
//
//            //set seekbar progress
//            sb_music.progress = timeElapsed
//
//            //set current time
////            tv_current_time.text = timeElapsed.toTime()
//
//            durationHandler.postDelayed(this, 100)
//        }
//    }

    private class GuiReceiver : BroadcastReceiver() {
        private var playerActivity: MainActivity? = null
        private var actualTime = 0
        fun setPlayerActivity(playerActivity: MainActivity?) {
            this.playerActivity = playerActivity
        }

        override fun onReceive(context: Context, intent: Intent) {
            Log.d("TAG", "onReceive: ${intent.action}")
            when (intent.action) {
                MusicPlayerService().GUI_UPDATE_ACTION -> {
                    if (intent.hasExtra(MusicPlayerService().ACTUAL_TIME_VALUE_EXTRA)) {
                        if (playerActivity!!.blockGUIUpdate) return
                        actualTime = intent.getIntExtra(MusicPlayerService().ACTUAL_TIME_VALUE_EXTRA, 0)
                        playerActivity?.sb_music?.progress = actualTime
                        playerActivity?.tv_current_time?.text = playerActivity?.toTime(actualTime)
                    }
                }

                MusicPlayerService().INIT_PLAYER_ACTION -> {
                    if (intent.hasExtra(MusicPlayerService().TOTAL_TIME_VALUE_EXTRA)) {
                        val totalTime = intent.getIntExtra(MusicPlayerService().TOTAL_TIME_VALUE_EXTRA, 0)
                        playerActivity?.sb_music?.max = totalTime
                        playerActivity?.tv_total_time?.text = playerActivity?.toTime(totalTime)
                    }
                }
                MusicPlayerService().PLAY_ACTION -> {
                    playerActivity?.btn_playpause?.setBackgroundResource(R.drawable.ic_pause)
                }

                MusicPlayerService().PAUSE_ACTION -> {
                    playerActivity?.btn_playpause?.setBackgroundResource(R.drawable.ic_play)
                }
            }
        }
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        receiver.setPlayerActivity(this)
        val filter = IntentFilter()
        filter.addAction(MusicPlayerService().INIT_PLAYER_ACTION)
        filter.addAction(MusicPlayerService().GUI_UPDATE_ACTION)
        filter.addAction(MusicPlayerService().PLAY_ACTION)
        filter.addAction(MusicPlayerService().PAUSE_ACTION)
        filter.addAction(MusicPlayerService().DELETE_ACTION)
        filter.addAction(MusicPlayerService().COMPLETE_ACTION)
        registerReceiver(receiver, filter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }
}