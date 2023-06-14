package com.example.parkinsonassistant;

import static android.app.PendingIntent.getActivity;

import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class Activity_Videos extends AppCompatActivity {

    private static final int REQUEST_VIDEO_CAPTURE = 1;
    private ArrayList<ModelVideo> videosList = new ArrayList<>();
    private AdapterVideoList adapterVideoList;
    private Uri videoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videos);

        // Initialize views
        initializeViews();

        // Check and request necessary permissions
        checkPermissions();

        // Load videos from external storage
        loadVideos();
    }

    private void initializeViews() {
        // Find the RecyclerView in the layout and set its layout manager and adapter
        RecyclerView recyclerView = findViewById(R.id.recyclerView_videos);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3)); // 3 = column count
        adapterVideoList = new AdapterVideoList(this, videosList);
        recyclerView.setAdapter(adapterVideoList);
    }

    private void checkPermissions() {
        // Check if the app has the necessary permission to access external storage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                // Request the permission to access all files on the device
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
                Toast.makeText(this, "Bitte gewähren Sie die Berechtigung zum Zugriff auf Medien", Toast.LENGTH_LONG).show();
                return;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 123) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    // If the permission is granted, load the videos
                    loadVideos();
                } else {
                    Toast.makeText(this, "Berechtigung zum Zugriff auf Medien wurde abgelehnt", Toast.LENGTH_SHORT).show();
                }
            } else {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // If the permission is granted, load the videos
                    loadVideos();
                } else {
                    Toast.makeText(this, "Berechtigung zum Zugriff auf Medien wurde abgelehnt", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void loadVideos() {
        // Load videos from external storage in a separate thread
        new Thread() {
            @Override
            public void run() {
                super.run();
                String[] projection = {MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.DURATION};
                String selection = MediaStore.Video.Media.IS_PENDING + " = ?";
                String[] selectionArgs = {"0"};
                String sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC";

                // Query the media store to get the videos
                Cursor cursor = getApplication().getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, sortOrder);

                if (cursor != null) {
                    int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                    int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                    int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);

                    while (cursor.moveToNext()) {
                        // Get video details from the cursor
                        long id = cursor.getLong(idColumn);
                        String title = cursor.getString(titleColumn);
                        int duration = cursor.getInt(durationColumn);

                        // Create a Uri for the video using the video ID
                        Uri data = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);

                        // Format the duration in HH:MM:SS format
                        String duration_formatted;
                        int sec = (duration / 1000) % 60;
                        int min = (duration / (1000 * 60)) % 60;
                        int hrs = duration / (1000 * 60 * 60);

                        if (hrs == 0) {
                            duration_formatted = String.valueOf(min).concat(":".concat(String.format(Locale.UK, "%02d", sec)));
                        } else {
                            duration_formatted = String.valueOf(hrs).concat(":".concat(String.format(Locale.UK, "%02d", min).concat(":".concat(String.format(Locale.UK, "%02d", sec)))));
                        }

                        // Create a ModelVideo object and add it to the videosList
                        videosList.add(new ModelVideo(id, data, title, duration_formatted));

                        // Update the adapter on the UI thread
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapterVideoList.notifyItemInserted(videosList.size() - 1);
                            }
                        });
                    }

                    // Close the cursor
                    cursor.close();
                }
            }
        }.start();
    }
}

