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

        initializeViews();
        checkPermissions();
        loadVideos();
    }

    private void initializeViews() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView_videos);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3)); //3 = column count
        adapterVideoList = new AdapterVideoList(this, videosList);
        recyclerView.setAdapter(adapterVideoList);
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
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
                    loadVideos();
                } else {
                    Toast.makeText(this, "Berechtigung zum Zugriff auf Medien wurde abgelehnt", Toast.LENGTH_SHORT).show();
                }
            } else {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadVideos();
                } else {
                    Toast.makeText(this, "Berechtigung zum Zugriff auf Medien wurde abgelehnt", Toast.LENGTH_SHORT).show();
                }
            }
        }

    }


    private void loadVideos() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                String[] projection = {MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.DURATION};
                String selection = MediaStore.Video.Media.IS_PENDING + " = ?";
                String[] selectionArgs = {"0"};
                String sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC";

                Cursor cursor = getApplication().getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, sortOrder);
                if (cursor != null) {
                    int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                    int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                    int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);

                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idColumn);
                        String title = cursor.getString(titleColumn);
                        int duration = cursor.getInt(durationColumn);

                        Uri data = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);

                        String duration_formatted;
                        int sec = (duration / 1000) % 60;
                        int min = (duration / (1000 * 60)) % 60;
                        int hrs = duration / (1000 * 60 * 60);

                        if (hrs == 0) {
                            duration_formatted = String.valueOf(min).concat(":".concat(String.format(Locale.UK, "%02d", sec)));
                        } else {
                            duration_formatted = String.valueOf(hrs).concat(":".concat(String.format(Locale.UK, "%02d", min).concat(":".concat(String.format(Locale.UK, "%02d", sec)))));
                        }

                        videosList.add(new ModelVideo(id, data, title, duration_formatted));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapterVideoList.notifyItemInserted(videosList.size() - 1);
                            }
                        });
                    }

                    cursor.close();
                }
            }
        }.start();
    }




}
