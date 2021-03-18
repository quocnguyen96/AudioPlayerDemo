package com.example.audioplayerdemo

import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val durationHandler = Handler(Looper.getMainLooper())
    private var timeElapsed = 0
    private val jumpTime = 2000

    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        volumeControlStream = AudioManager.STREAM_MUSIC
        initPlayer()
        eventListener()

    }

    private fun eventListener() {
        btn_playpause.setOnClickListener { playPause() }
        btn_forward.setOnClickListener { forward() }
        btn_rewind.setOnClickListener { rewind() }
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.seekTo(0)
            mediaPlayer.pause()
            playPauseToggle()
        }

        sb_music.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer.seekTo(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })
    }

    private fun initPlayer() {
        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.music)
        tv_total_time.text = mediaPlayer.duration.toTime()
        sb_music.max = mediaPlayer.duration
    }

    private fun playPause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            btn_playpause.setBackgroundResource(R.drawable.ic_play)
        } else {
            mediaPlayer.start()
            btn_playpause.setBackgroundResource(R.drawable.ic_pause)
            timeElapsed = mediaPlayer.currentPosition
            sb_music.progress = timeElapsed
            durationHandler.postDelayed(updateSeekBarTime, 100)
        }
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

    private fun playPauseToggle() {
        if (mediaPlayer.isPlaying){
            btn_playpause.setBackgroundResource(R.drawable.ic_pause)
        }
        else {
            btn_playpause.setBackgroundResource(R.drawable.ic_play)
        }
    }

    private fun Int.toTime(): String {
        val time = this.toLong()
        val minute = TimeUnit.MILLISECONDS.toMinutes(time)
        val second = TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(
            TimeUnit.MILLISECONDS.toMinutes(time)
        )
        return String.format("%02d:%02d", minute, second)
    }

    private val updateSeekBarTime: Runnable = object : Runnable {
        override fun run() {

            //get current position
            timeElapsed = mediaPlayer.currentPosition

            //set seekbar progress
            sb_music.progress = timeElapsed

            //set current time
            tv_current_time.text = timeElapsed.toTime()

            durationHandler.postDelayed(this, 100)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer.pause()
        playPauseToggle()
    }

    override fun onResume() {
        super.onResume()
        playPauseToggle()
    }
}