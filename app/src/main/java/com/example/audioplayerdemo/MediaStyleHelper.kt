package com.example.audioplayerdemo

import android.content.Context
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver

class MediaStyleHelper {
    fun from(context: Context, mediaSession: MediaSessionCompat): NotificationCompat.Builder =
        NotificationCompat.Builder(context, "ServiceChanelId")
        .setContentIntent(mediaSession.controller.sessionActivity)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setDeleteIntent(
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                context,
                PlaybackStateCompat.ACTION_STOP
            )
        )
}