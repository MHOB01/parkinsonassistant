package com.example.parkinsonassistant;

import android.content.ContentUris;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

public class Activity_Player extends AppCompatActivity {

    long videoId;
    private PlayerView playerView;
    private SimpleExoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // Initialize views
        initializeViews();

        // Retrieve video ID from intent extras
        videoId = getIntent().getExtras().getLong("videoId");
    }

    private void initializeViews() {
        // Find and initialize PlayerView
        playerView = findViewById(R.id.playerView);
    }

    private void initializePlayer() {
        // Create a SimpleExoPlayer instance
        player = new SimpleExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // Create a Uri for the video using the videoId
        Uri videoUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoId);

        // Build a media source for the video
        MediaSource mediaSource = buildMediaSource(videoUri);

        // Prepare the player with the media source
        player.prepare(mediaSource);
        player.setPlayWhenReady(true);
    }

    private MediaSource buildMediaSource(Uri uri) {
        // Create a DataSource.Factory using the app name as the user agent
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, getString(R.string.app_name));

        // Create a ProgressiveMediaSource.Factory and pass the dataSourceFactory to it
        ProgressiveMediaSource.Factory mediaSourceFactory = new ProgressiveMediaSource.Factory(dataSourceFactory);

        // Create a MediaSource from the Uri
        return mediaSourceFactory.createMediaSource(MediaItem.fromUri(uri));
    }

    private void releasePlayer(){
        // Release the player resources
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check the SDK version and initialize the player
        if (Util.SDK_INT >= 24) {
            initializePlayer();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check the SDK version and initialize the player
        if (Util.SDK_INT < 24 || player == null) {
            initializePlayer();
        }
    }

    @Override
    protected void onPause() {
        // Release the player resources when the activity is paused
        if(Util.SDK_INT < 24){
            releasePlayer();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        // Release the player resources when the activity is stopped
        if(Util.SDK_INT >= 24){
            releasePlayer();
        }
        super.onStop();
    }
}
