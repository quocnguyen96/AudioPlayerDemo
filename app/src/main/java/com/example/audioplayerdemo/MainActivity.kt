package com.example.audioplayerdemo

import android.media.AudioManager
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val musicPlayerService = MusicPlayerService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        volumeControlStream = AudioManager.STREAM_MUSIC
        eventListener()
        initObserver()
    }

    private fun initObserver() {
        MusicPlayerService.playStatus.observe(this, Observer {
            if (it) {
                btn_playpause.setBackgroundResource(R.drawable.ic_pause)
            } else {
                btn_playpause.setBackgroundResource(R.drawable.ic_play)
            }
        })

        MusicPlayerService.actualTime.observe(this, Observer {
            sb_music.progress = it
            tv_current_time.text = milliToTime(it)
        })

        MusicPlayerService.totalTime.observe(this, Observer {
            sb_music.max = it
            tv_total_time.text = milliToTime(it)
        })
    }

    private fun eventListener() {
        btn_playpause.setOnClickListener { playPause() }
        btn_forward.setOnClickListener { forward() }
        btn_rewind.setOnClickListener { rewind() }

        sb_music.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            var time = 0
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                time = progress
                if (fromUser) MusicPlayerService.mediaPlayer?.seekTo(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
    }

    private fun playPause() {
        MusicPlayerService.mediaPlayer?.let {
            if (it.isPlaying) {
                musicPlayerService.startActionPause(this)
                btn_playpause.setBackgroundResource(R.drawable.ic_play)
            } else {
                musicPlayerService.startActionPlay(this)
                btn_playpause.setBackgroundResource(R.drawable.ic_pause)
            }
        }
    }

    private fun forward() {
        musicPlayerService.startActionForward(this)
    }

    private fun rewind() {
        musicPlayerService.startActionRewind(this)
    }


    private fun milliToTime(time: Int): String {
        val minute = TimeUnit.MILLISECONDS.toMinutes(time.toLong())
        val second = TimeUnit.MILLISECONDS.toSeconds(time.toLong()) - TimeUnit.MINUTES.toSeconds(
            TimeUnit.MILLISECONDS.toMinutes(time.toLong())
        )
        return String.format("%02d:%02d", minute, second)
    }
}