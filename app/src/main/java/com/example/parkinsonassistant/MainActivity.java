package com.example.parkinsonassistant;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

import android.speech.tts.TextToSpeech;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;


public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private static final int REQUEST_CODE_NOTES_ACTIVITY = 1;
    private static final int REQUEST_CODE_VIDEO_RECORD = 2;

    private static final int REQUEST_CODE_CAMERA_PERMISSION = 100;

    private static final int REQUEST_VIDEO_CAPTURE = 4;
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 100;
    private static final int REQUEST_CODE_TIMELINE_ACTIVITY = 102;
    private NotesDatabase notesDatabase;
    private StringBuilder notes;
    private TextView textViewNotes;
    private Map<String, StringBuilder> notesByDay;

    private TextView textViewInfoScreen;

    private Button buttonGallery;
    private Uri recordedVideoUri; // Speichert die Uri des aufgenommenen Videos

    private TextToSpeech textToSpeech;
    private boolean isTextToSpeechInitialized = false;

    private TextToSpeech tts;
    private NoteDao noteDao;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        notes = new StringBuilder();

        // Set current date in a specific format and display it in a TextView
        String currentDate = new SimpleDateFormat("EEEE, dd.MM.yyyy", Locale.GERMAN).format(new Date());
        TextView textViewCurrentDate = findViewById(R.id.textViewSelectedDate);
        textViewCurrentDate.setText(currentDate);

        // Initialize HashMap and add click listeners to buttons
        notesByDay = new HashMap<>();

        Button buttonAddNote = findViewById(R.id.buttonAddNote);
        buttonAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start NotesActivity for adding a note
                Intent intent = new Intent(MainActivity.this, NotesActivity.class);
                startActivityForResult(intent, REQUEST_CODE_NOTES_ACTIVITY);
            }
        });

        Button btnParkinsonInfo = findViewById(R.id.buttonParkinsonInfo);
        btnParkinsonInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ParkinsonInfoActivity.class);
                startActivityForResult(intent, REQUEST_CODE_NOTES_ACTIVITY);
            }
        });

        Button btnStartPage = findViewById(R.id.buttonInfo);
        btnStartPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StartPageActivity.class);
                startActivityForResult(intent, REQUEST_CODE_NOTES_ACTIVITY);
            }
        });


        Button videoRecordButton = findViewById(R.id.videoRecord);
        videoRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Display a dialog with instructions
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Willkommensnachricht");
                builder.setMessage("Hallo und willkommen zu Ihren digitalen Übungen! Vor dem Start der Kamera können Sie folgende Anweisungen befolgen:\n\n1. Stellen Sie sicher, dass Sie ausreichend Platz haben und sich in einer ruhigen Umgebung befinden.\n2. Halten Sie Ihr Gerät stabil und positionieren Sie es so, dass Sie gut zu sehen sind.\n3. Drücken Sie den Aufnahmeknopf, um die Kamera zu öffnen und mit der Übung zu beginnen.\n\nViel Spaß und gutes Gelingen!");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Stop text-to-speech playback if running
                        if (tts != null) {
                            tts.stop();
                        }

                        // Open CameraManager activity
                        Intent intent = new Intent(MainActivity.this, CameraManager.class);
                        startActivityForResult(intent, REQUEST_CODE_VIDEO_RECORD);
                    }
                });
                builder.show();

                // Initialize text-to-speech and read the welcome message
                tts = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            String text = "Hallo und willkommen zu Ihren digitalen Übungen! Vor dem Start der Kamera können Sie folgende Anweisungen befolgen: Stellen Sie sicher, dass Sie ausreichend Platz haben und sich in einer ruhigen Umgebung befinden. Halten Sie Ihr Gerät stabil und positionieren Sie es so, dass Sie gut zu sehen sind. Drücken Sie den Aufnahmeknopf, um die Kamera zu öffnen und mit der Übung zu beginnen.Viel Spaß und gutes Gelingen!";
                            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                    }
                });
            }
        });

        // Initialize Room database and retrieve notes from it
        notesDatabase = Room.databaseBuilder(getApplicationContext(), NotesDatabase.class, "notes-db")
                .fallbackToDestructiveMigration()
                .build();
        noteDao = notesDatabase.noteDao();

        // Retrieve notes from the database and display them in the RecyclerView

        // Retrieve and display notes from the database
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Retrieve all notes from the database
                List<Note> notes = notesDatabase.noteDao().getAllNotes();
                // Update the note display or perform corresponding logic on the UI thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Display notes
                    }
                });
            }
        }).start();


        // Button click listener for "buttonTimeline"
        Button buttonTimeline = findViewById(R.id.buttonTimeline);
        buttonTimeline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the TimelineActivity
                Intent intent = new Intent(MainActivity.this, TimelineActivity.class);
                startActivity(intent);
            }
        });



        // Button click listener for "buttonGallery"
        buttonGallery = findViewById(R.id.buttonGallery);
        buttonGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestStoragePermission();
            }
        });

        // Text-to-speech initialization
        textToSpeech = new TextToSpeech(this, this);
    }

    // Request storage permission if not granted
    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_CODE_STORAGE_PERMISSION);
        } else {
            // Permission granted
            openVideosActivity();
        }
    }

    // Handle permission request results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                openVideosActivity();
            } else {
                Toast.makeText(this, "Medien-Berechtigung wurde abgelehnt.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == REQUEST_CODE_VIDEO_RECORD && resultCode == RESULT_OK && data != null) {
            Uri videoUri = data.getData();

            if (videoUri != null) {
                saveVideoToGallery(videoUri); // Save the video to the gallery
                recordedVideoUri = videoUri; // Store the Uri of the recorded video

                // Code to navigate to NotesActivity
                Intent intent = new Intent(MainActivity.this, NotesActivity.class);
                intent.putExtra("fromMainActivity", true); // Pass the value true to indicate the redirection from MainActivity
                startActivityForResult(intent, REQUEST_CODE_NOTES_ACTIVITY);
            }
            // Process result from TimelineActivity
        } else if (requestCode == REQUEST_CODE_TIMELINE_ACTIVITY && resultCode == RESULT_OK && data != null) {
            String notes = data.getStringExtra("notes");
            if (notes != null) {
                textViewNotes.setText(notes);
            }
        }
        // Load notes from the database
        loadNotesFromDatabase();
    }

    private void saveVideoToGallery(Uri videoUri) {
        ContentResolver contentResolver = getContentResolver();

        // Check if the video already exists in the gallery
        if (isVideoAlreadySaved(videoUri)) {
            Toast.makeText(this, "Das Video existiert bereits in der Galerie.", Toast.LENGTH_SHORT).show();
            return;
        }

        String videoPath = getVideoPathFromUri(videoUri);
        if (videoPath == null) {
            Toast.makeText(this, "Fehler beim Extrahieren des Video-Pfades.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if an entry with the same path already exists
        if (isDuplicatePath(videoPath)) {
            // Entry with the same path already exists, overwrite the file or generate a unique name
            videoPath = generateUniqueFilePath(videoPath);
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.TITLE, "Mein aufgenommenes Video");
        values.put(MediaStore.Video.Media.DISPLAY_NAME, "Video_" + System.currentTimeMillis() + ".mp4");
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        values.put(MediaStore.Video.Media.DATA, videoPath);

        try {
            Uri newUri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
            if (newUri != null) {
                Toast.makeText(this, "Video erfolgreich gespeichert.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Fehler beim Speichern des Videos. Neue Uri ist null.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Fehler beim Speichern des Videos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Get the video path from the Uri
    private String getVideoPathFromUri(Uri videoUri) {
        String[] projection = {MediaStore.Video.Media.DATA};
        Cursor cursor = getContentResolver().query(videoUri, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATA);
            String videoPath = cursor.getString(columnIndex);
            cursor.close();
            return videoPath;
        }
        return null;
    }

    // Check if the video is already saved in the gallery
    private boolean isVideoAlreadySaved(Uri videoUri) {
        String[] projection = {MediaStore.Video.Media.DATA};
        String selection = MediaStore.Video.Media.DATA + "=?";
        String[] selectionArgs = {videoUri.getPath()};
        Cursor cursor = getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
        );

        boolean isAlreadySaved = cursor != null && cursor.getCount() > 0;
        if (cursor != null) {
            cursor.close();
        }
        return isAlreadySaved;
    }

    // Check if there is a duplicate path in the database
    private boolean isDuplicatePath(String videoPath) {
        String[] projection = {MediaStore.Video.Media.DATA};
        String selection = MediaStore.Video.Media.DATA + "=?";
        String[] selectionArgs = {videoPath};
        Cursor cursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null);
        boolean isDuplicate = cursor != null && cursor.getCount() > 0;
        if (cursor != null) {
            cursor.close();
        }
        return isDuplicate;
    }

    // Generate a unique file path to avoid duplication
    private String generateUniqueFilePath(String videoPath) {
        String uniqueVideoPath = videoPath;
        int counter = 1;

        // Keep increasing the counter and appending it to the path until no entry with the generated path exists
        while (isDuplicatePath(uniqueVideoPath)) {
            // Example: videoPath = "/external/video/video.mp4", uniqueVideoPath = "/external/video/video_1.mp4"
            uniqueVideoPath = videoPath.substring(0, videoPath.lastIndexOf(".")) + "_" + counter + videoPath.substring(videoPath.lastIndexOf("."));
            counter++;
        }

        return uniqueVideoPath;
    }


    private void openVideosActivity() {
        Intent intent = new Intent(MainActivity.this, Activity_Videos.class);
        startActivity(intent);
    }


    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.GERMAN);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("MainActivity", "Die deutsche Sprache wird nicht unterstützt.");
            } else {
                isTextToSpeechInitialized = true;
            }
        } else {
            Log.e("MainActivity", "Initialisierung des Text-to-Speech fehlgeschlagen.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Share TextToSpeech resources
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    private void loadNotesFromDatabase() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                List<Note> notes = noteDao.getAllNotes();
                Collections.reverse(notes); // Reverse the order of the notes to show the newest note on top
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        RecyclerView recyclerView = findViewById(R.id.recyclerViewNotes);
                        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                        NoteAdapter noteAdapter = new NoteAdapter(notes);
                        recyclerView.setAdapter(noteAdapter);
                    }
                });
            }
        });
    }



    @Override
    protected void onResume() {
        super.onResume();
        loadNotesFromDatabase();
    }


}
