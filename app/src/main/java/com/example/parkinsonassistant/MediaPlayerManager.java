package com.example.parkinsonassistant;

import android.content.Context;
import android.media.MediaPlayer;

public class MediaPlayerManager {
    private static MediaPlayer mediaPlayer;

    public static void playAudio(Context context, int resId) {
        stopAudio(); // Stopp und Freigabe des MediaPlayers, falls er aktiv ist
        mediaPlayer = MediaPlayer.create(context, resId);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopAudio();
            }
        });
        mediaPlayer.start();
    }

    public static void stopAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}

