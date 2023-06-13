package com.example.parkinsonassistant;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StartPageActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_NOTES_ACTIVITY = 102;
    private Uri recordedVideoUri; // Speichert die Uri des aufgenommenen Videos

    private static final int REQUEST_CODE_CAMERA_PERMISSION = 100;
    private static final int REQUEST_CODE_VIDEO_RECORD = 101;
    private TextView txtLastOnline;

    private static final int REQUEST_VIDEO_CAPTURE = 4;

    private Button buttonCamera;

    private TextToSpeech textToSpeech;
    private Button btnDiary;
    private Button btnRecordVideo;
    private Button btnHome;

    private static final String TAG = "StartPageActivity";
    private MediaPlayer mediaPlayer;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_page);

        // Einleitungstext anzeigen
        TextView txtIntro = findViewById(R.id.txt_intro);
        txtIntro.setText("Hallo und willkommen zu Ihrem digitalen Symptomtagebuch. Was möchten Sie heute machen?");
        txtIntro.setTextSize(28);
        // Text-to-Speech initialisieren
       /* textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(Locale.getDefault());
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // Sprache wird nicht unterstützt
                        // Hier können Sie eine entsprechende Fehlerbehandlung durchführen
                    } else {
                        // Text vorlesen
                        speakText(txtIntro.getText().toString());
                    }
                } else {
                    // Text-to-Speech konnte nicht initialisiert werden
                    // Hier können Sie eine entsprechende Fehlerbehandlung durchführen
                }
            }
        });*/

        // Button für das Tagebuch
        btnDiary = findViewById(R.id.btn_diary);
        btnDiary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Öffne das Tagebuch
                stopReading(); // Vorlesen stoppen
                startActivity(new Intent(StartPageActivity.this, NotesActivity.class));
            }
        });



        // Button für das Aufnehmen eines Videos
        btnRecordVideo = findViewById(R.id.btn_record_video);
        btnRecordVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInstructionsDialog();
                stopReading(); // Vorlesen stoppen
            }
        });



        // Button für die Hauptseite
        btnHome = findViewById(R.id.btn_home);
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Öffne die Hauptseite
                stopReading(); // Vorlesen stoppen
                startActivity(new Intent(StartPageActivity.this, MainActivity.class));
            }
        });

        // Textview für "zuletzt online"
        txtLastOnline = findViewById(R.id.txt_last_online);
        updateLastOnlineText();


        mediaPlayer = MediaPlayer.create(this, R.raw.symptomtagebuch);
        mediaPlayer.start();


    }



    private void startVideoRecording() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60); // Set the maximum duration for the video in seconds
        startActivityForResult(intent, REQUEST_CODE_VIDEO_RECORD);
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA_PERMISSION);
        } else {
            startVideoRecording();
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

    // Methode zum Aktualisieren des "zuletzt online"-Texts
    private void updateLastOnlineText () {
        // Hier sollten Sie den tatsächlichen Wert für "zuletzt online" festlegen
        String lastOnline = "gestern";

        String lastOnlineText = "Zuletzt online: " + lastOnline;
        txtLastOnline.setText(lastOnlineText);
    }

    private void startCamera () {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_VIDEO_CAPTURE);
        }
    }

    @Override
    public void onRequestPermissionsResult ( int requestCode, String[] permissions,
                                             int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Berechtigung erteilt, Kamera starten
                startCamera();
            } else {
                // Berechtigung verweigert
                Toast.makeText(this, "Video-Berechtigung wurde abgelehnt.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_VIDEO_RECORD && resultCode == RESULT_OK && data != null) {
            Uri videoUri = data.getData();

            if (videoUri != null) {
                saveVideoToGallery(videoUri); // Speichere das Video in der Galerie
                recordedVideoUri = videoUri; // Speichere die Uri des aufgenommenen Videos

                // Hier Code einfügen, um zur NotesActivity weitergeleitet zu werden
                Intent intent = new Intent(StartPageActivity.this, NotesActivity.class);
                intent.putExtra("fromMainActivity", true); // Übergebe den Wert true, um anzuzeigen, dass die Weiterleitung von der MainActivity erfolgt ist
                startActivityForResult(intent, REQUEST_CODE_NOTES_ACTIVITY);
            }
        }
    }


    private void showInstructionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Anleitung");
        builder.setMessage("Hallo und willkommen zu Ihrem digitalen Übungen! Vor dem Start der Kamera können Sie folgende Anweisungen befolgen:\n\n1. Stellen Sie sicher, dass Sie ausreichend Platz haben und sich in einer ruhigen Umgebung befinden.\n2. Halten Sie Ihr Gerät stabil und positionieren Sie es so, dass Sie gut zu sehen sind.\n3. Drücken Sie den Aufnahmeknopf, um die Kamera zu öffnen und mit der Übung zu beginnen.\n\nViel Spaß und gutes Gelingen!");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                requestCameraPermission();
            }
        });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /*private void speakText(String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    } */

    private void stopReading() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

   /* @Override
    protected void onDestroy() {
        super.onDestroy();
        // Text-to-Speech-Ressourcen freigeben
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }





}
