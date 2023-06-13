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
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import android.speech.tts.TextToSpeech;


public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private static final int REQUEST_CODE_NOTES_ACTIVITY = 1;
    private static final int REQUEST_CODE_VIDEO_RECORD = 2;

    private static final int REQUEST_CODE_CAMERA_PERMISSION = 100;

    private static final int REQUEST_VIDEO_CAPTURE = 4;
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 100;
    private static final int REQUEST_CODE_TIMELINE_ACTIVITY = 102;

    private StringBuilder notes;
    private TextView textViewNotes;
    private Map<String, StringBuilder> notesByDay;

    private TextView textViewInfoScreen;

    private Button buttonGallery;
    private Uri recordedVideoUri; // Speichert die Uri des aufgenommenen Videos

    private TextToSpeech textToSpeech;
    private boolean isTextToSpeechInitialized = false;

    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        notes = new StringBuilder();
        textViewNotes = findViewById(R.id.textViewNotes);

        String currentDate = new SimpleDateFormat("EEEE, dd.MM.yyyy", Locale.GERMAN).format(new Date());
        TextView textViewCurrentDate = findViewById(R.id.textViewSelectedDate);
        textViewCurrentDate.setText(currentDate);

        notesByDay = new HashMap<>();

        Button buttonAddNote = findViewById(R.id.buttonAddNote);
        buttonAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NotesActivity.class);
                startActivityForResult(intent, REQUEST_CODE_NOTES_ACTIVITY);
            }
        });

        Button videoRecordButton = findViewById(R.id.videoRecord);

        videoRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Willkommensnachricht");
                builder.setMessage("Hallo und willkommen zu Ihrem digitalen Übungen! Vor dem Start der Kamera können Sie folgende Anweisungen befolgen:\n\n1. Stellen Sie sicher, dass Sie ausreichend Platz haben und sich in einer ruhigen Umgebung befinden.\n2. Halten Sie Ihr Gerät stabil und positionieren Sie es so, dass Sie gut zu sehen sind.\n3. Drücken Sie den Aufnahmeknopf, um die Kamera zu öffnen und mit der Übung zu beginnen.\n\nViel Spaß und gutes Gelingen!");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Stoppen der Text-to-Speech-Wiedergabe
                        if (tts != null) {
                            tts.stop();
                        }

                        // CameraManager öffnen
                        Intent intent = new Intent(MainActivity.this, CameraManager.class);
                        startActivityForResult(intent, REQUEST_CODE_VIDEO_RECORD);
                    }
                });
                builder.show();

                // Text-to-Speech initialisieren und Willkommensnachricht vorlesen
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






    buttonGallery = findViewById(R.id.buttonGallery);
        buttonGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestStoragePermission();
            }
        });


        textToSpeech = new TextToSpeech(this, this);
    }

    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_CODE_STORAGE_PERMISSION);
        } else {
            // Berechtigung wurde bereits gewährt
            openVideosActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Berechtigung wurde gewährt
                openVideosActivity();
            } else {
                Toast.makeText(this, "Medien-Berechtigung wurde abgelehnt.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_NOTES_ACTIVITY && resultCode == RESULT_OK && data != null) {
            String noteEntry = data.getStringExtra("note");
            if (noteEntry != null) {
                StringBuilder notesForDay = getNotesForCurrentDay();
                String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.GERMAN).format(new Date());
                notesForDay.append(currentTime).append(" - ").append(noteEntry).append("\n");
                updateNotesTextView();
                Log.d("MainActivity", "Notiz hinzugefügt");
            }

        } else if (requestCode == REQUEST_CODE_VIDEO_RECORD && resultCode == RESULT_OK && data != null) {
            Uri videoUri = data.getData();

            if (videoUri != null) {
                saveVideoToGallery(videoUri); // Speichere das Video in der Galerie
                recordedVideoUri = videoUri; // Speichere die Uri des aufgenommenen Videos

                // Hier Code einfügen, um zur NotesActivity weitergeleitet zu werden
                Intent intent = new Intent(MainActivity.this, NotesActivity.class);
                intent.putExtra("fromMainActivity", true); // Übergebe den Wert true, um anzuzeigen, dass die Weiterleitung von der MainActivity erfolgt ist
                startActivityForResult(intent, REQUEST_CODE_NOTES_ACTIVITY);
            }
        } else if (requestCode == REQUEST_CODE_TIMELINE_ACTIVITY && resultCode == RESULT_OK && data != null) {
            String notes = data.getStringExtra("notes");
            if (notes != null) {
                textViewNotes.setText(notes);
            }
        }
    }

    private void saveVideoToGallery(Uri videoUri) {
        ContentResolver contentResolver = getContentResolver();

        // Überprüfen, ob das Video bereits in der Galerie vorhanden ist
        if (isVideoAlreadySaved(videoUri)) {
            Toast.makeText(this, "Das Video existiert bereits in der Galerie.", Toast.LENGTH_SHORT).show();
            return;
        }

        String videoPath = getVideoPathFromUri(videoUri);
        if (videoPath == null) {
            Toast.makeText(this, "Fehler beim Extrahieren des Video-Pfades.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Überprüfen, ob ein Eintrag mit dem gleichen Pfad bereits vorhanden ist
        if (isDuplicatePath(videoPath)) {
            // Eintrag mit dem gleichen Pfad existiert bereits, Datei überschreiben oder eindeutigen Namen generieren
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

    // Die folgenden Methoden bleiben unverändert

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

    private String generateUniqueFilePath(String videoPath) {
        String uniqueVideoPath = videoPath;
        int counter = 1;

        // Solange ein Eintrag mit dem generierten Pfad bereits existiert, erhöhe den Zähler und füge ihn dem Pfad hinzu
        while (isDuplicatePath(uniqueVideoPath)) {
            // Beispiel: videoPath = "/external/video/video.mp4", uniqueVideoPath = "/external/video/video_1.mp4"
            uniqueVideoPath = videoPath.substring(0, videoPath.lastIndexOf(".")) + "_" + counter + videoPath.substring(videoPath.lastIndexOf("."));
            counter++;
        }

        return uniqueVideoPath;
    }

    private StringBuilder getNotesForCurrentDay() {
        String currentDate = new SimpleDateFormat("EEEE, dd.MM.yyyy", Locale.GERMAN).format(new Date());
        StringBuilder notesForDay = notesByDay.get(currentDate);
        if (notesForDay == null) {
            notesForDay = new StringBuilder();
            notesByDay.put(currentDate, notesForDay);
        }
        return notesForDay;
    }

    private void updateNotesTextView() {
        StringBuilder allNotes = new StringBuilder();

        // Iteriere über die Notizen nach Datum
        for (Map.Entry<String, StringBuilder> entry : notesByDay.entrySet()) {
            StringBuilder notesForDay = entry.getValue();

            // Überprüfen, ob Notizen für den Tag vorhanden sind
            if (notesForDay.length() > 0) {
                allNotes.append(entry.getKey()).append("\n");
                allNotes.append(notesForDay.toString()).append("\n");
            }
        }

        textViewNotes.setText(allNotes.toString());
    }

    private void openVideosActivity() {
        Intent intent = new Intent(MainActivity.this, CameraManager.class);
        startActivity(intent);
    }

    private void speakText(String text) {
        if (isTextToSpeechInitialized) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "welcome");
        }
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

        // TextToSpeech-Ressourcen freigeben
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
