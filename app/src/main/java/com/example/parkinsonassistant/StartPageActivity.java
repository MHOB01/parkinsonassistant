package com.example.parkinsonassistant;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
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
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
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
    private static final int REQUEST_PERMISSION_CODE = 103;
    private static final int REQUEST_SPEECH_RECOGNIZER = 104;
    private static final int PERMISSION_REQUEST_CODE = 105;
    private Uri recordedVideoUri;

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
    private boolean isTextToSpeechInitialized = false;

    private TextToSpeech tts;
    private Button btnSpeechToText;

    private EditText editTextDestination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_page);

        // Display introductory text
        TextView txtIntro = findViewById(R.id.txt_intro);
        txtIntro.setText("Hallo und willkommen zu Ihrem digitalen Symptomtagebuch. Was möchten Sie heute machen?");
        txtIntro.setTextSize(28);

        // EditText for destination input
        editTextDestination = findViewById(R.id.editTextDestination);







        btnSpeechToText = findViewById(R.id.btn_speech_to_text);
        btnSpeechToText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopReading();
                checkPermissionAndStartSpeechToText();
            }
        });

        // Button for the diary
        btnDiary = findViewById(R.id.btn_diary);
        btnDiary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopReading(); // Stop reading aloud
                startActivity(new Intent(StartPageActivity.this, NotesActivity.class)); // Open the diary activity
            }
        });


        btnHome = findViewById(R.id.btn_home);

        // Button for recording a video
        Button videoRecordButton = findViewById(R.id.btn_record_video);
        videoRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopReading(); // Stop reading aloud

                // Show a welcome message dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(StartPageActivity.this);

                // Inflate the custom layout for the title
                View customTitleView = LayoutInflater.from(StartPageActivity.this).inflate(R.layout.custom_alert_dialog_title, null);
                TextView titleTextView = customTitleView.findViewById(R.id.alert_dialog_title);
                titleTextView.setText("Willkommensnachricht");

                // Set the text color for the title based on the contrast with black
                TypedValue typedValue = new TypedValue();
                StartPageActivity.this.getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true);
                int backgroundColor = typedValue.data;
                double contrastWithBlack = contrastRatio(backgroundColor, Color.BLACK);
                int titleTextColor;
                if (contrastWithBlack >= 4.5) {
                    titleTextColor = Color.BLACK;
                } else {
                    titleTextColor = Color.WHITE;
                }
                titleTextView.setTextColor(titleTextColor);

                // Set the title view for the AlertDialog
                builder.setCustomTitle(customTitleView);

// Inflate the custom layout for the message
                View customView = LayoutInflater.from(StartPageActivity.this).inflate(R.layout.custom_alert_dialog_message, null);
                TextView messageTextView = customView.findViewById(R.id.alert_dialog_message);
                messageTextView.setText("Hallo und willkommen zu Ihren digitalen Übungen! Vor dem Start der Kamera können Sie folgende Anweisungen befolgen:\n\n1. Stellen Sie sicher, dass Sie ausreichend Platz haben und sich in einer ruhigen Umgebung befinden.\n2. Positionieren Sie Ihr Gerät stabil und so, dass Sie gut zu sehen sind.\n3. Befolgen Sie dazu die kommenden Anweisungen.\n\nViel Spaß und gutes Gelingen!");

// Set the text color based on the contrast with black

                StartPageActivity.this.getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true);

                int textColor;
                if (contrastWithBlack >= 4.5) {
                    textColor = Color.BLACK;
                } else {
                    textColor = Color.WHITE;
                }
                messageTextView.setTextColor(textColor);

// Set the background color to black or the system's default background color
                StartPageActivity.this.getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true);
                //int backgroundColor = typedValue.data;
                customView.setBackgroundColor(backgroundColor);

                builder.setView(customView);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (tts != null) {
                            tts.stop(); // Stop text-to-speech playback
                        }
                        // Open the CameraManager
                        Intent intent = new Intent(StartPageActivity.this, FaceDetection.class);
                        startActivityForResult(intent, REQUEST_CODE_VIDEO_RECORD);
                    }
                });
                builder.show();

                // Initialize text-to-speech and read the welcome message
                tts = new TextToSpeech(StartPageActivity.this, new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            String text = "Hallo und willkommen zu Ihren digitalen Übungen! Vor dem Start der Kamera können Sie folgende Anweisungen befolgen: Stellen Sie sicher, dass Sie ausreichend Platz haben und sich in einer ruhigen Umgebung befinden. Positionieren Sie Ihr Gerät stabil und so, dass Sie gut zu sehen sind. Befolgen Sie dazu die kommenden Anweisungen. Viel Spaß und gutes Gelingen!";
                            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                    }
                });
            }
        });


        // Button for the home page
        btnHome = findViewById(R.id.btn_home);
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopReading(); // Stop reading aloud
                startActivity(new Intent(StartPageActivity.this, MainActivity.class)); // Open the home page activity
            }
        });

        // OK Button
        Button btnOk = findViewById(R.id.btn_ok);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopReading();
                // Get the destination text from the EditText
                String destination = editTextDestination.getText().toString();

                // Check if the destination is not empty
                if (!destination.isEmpty()) {
                    // Check the destination and navigate accordingly
                    if (destination.equalsIgnoreCase("Hauptseite")) {
                        startActivity(new Intent(StartPageActivity.this, MainActivity.class));
                    } else if (destination.equalsIgnoreCase("Video aufnehmen")) {
                        startActivity(new Intent(StartPageActivity.this, FaceDetection.class));
                    } else if (destination.equalsIgnoreCase("Tagebuch")) {
                        startActivity(new Intent(StartPageActivity.this, NotesActivity.class));

                    } else {
                        Toast.makeText(StartPageActivity.this, "Ungültiges Ziel.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(StartPageActivity.this, "Bitte geben Sie ein Ziel ein.", Toast.LENGTH_SHORT).show();
                }
            }
        });






        // TextView for "last online" text
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


    private void saveVideoToGallery(Uri videoUri) {
        ContentResolver contentResolver = getContentResolver();

        // Check if the video is already saved in the gallery
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

    private double contrastRatio(int foregroundColor, int backgroundColor) {
        // Calculate the luminance of the foreground color
        double foregroundLuminance = calculateLuminance(foregroundColor);

        // Calculate the luminance of the background color
        double backgroundLuminance = calculateLuminance(backgroundColor);

        // Ensure that the foreground luminance is the lighter color
        if (backgroundLuminance > foregroundLuminance) {
            double temp = foregroundLuminance;
            foregroundLuminance = backgroundLuminance;
            backgroundLuminance = temp;
        }

        // Calculate the contrast ratio
        double contrast = (foregroundLuminance + 0.05) / (backgroundLuminance + 0.05);
        return contrast;
    }

    private double calculateLuminance(int color) {
        double red = Color.red(color) / 255.0;
        double green = Color.green(color) / 255.0;
        double blue = Color.blue(color) / 255.0;

        red = (red <= 0.03928) ? red / 12.92 : Math.pow((red + 0.055) / 1.055, 2.4);
        green = (green <= 0.03928) ? green / 12.92 : Math.pow((green + 0.055) / 1.055, 2.4);
        blue = (blue <= 0.03928) ? blue / 12.92 : Math.pow((blue + 0.055) / 1.055, 2.4);

        // Calculate the relative luminance
        double luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue;
        return luminance;
    }



    private String getVideoPathFromUri(Uri videoUri) {
        // Query the MediaStore database to get the file path of a video from its content URI
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
        // Check if a video with the given content URI is already saved in the device's gallery
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
        // Check if a video with the given file path already exists in the device's gallery
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
        // Generate a unique file path by appending a counter to the original file path
        String uniqueVideoPath = videoPath;
        int counter = 1;

        // Keep incrementing the counter and adding it to the file path until a unique path is generated
        while (isDuplicatePath(uniqueVideoPath)) {
            uniqueVideoPath = videoPath.substring(0, videoPath.lastIndexOf(".")) + "_" + counter + videoPath.substring(videoPath.lastIndexOf("."));
            counter++;
        }

        return uniqueVideoPath;
    }

    private void updateLastOnlineText() {
        // Update the "last online" text displayed in the UI with the actual value for "last online"
        String lastOnline = "gestern"; //hardcoded
        String lastOnlineText = "Zuletzt online: " + lastOnline;
        txtLastOnline.setText(lastOnlineText);
    }

    private void startCamera() {
        // Start the device's default camera app for video recording
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_VIDEO_CAPTURE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted, start the camera
                startCamera();
            } else {
                // Camera permission denied
                Toast.makeText(this, "Video-Berechtigung wurde abgelehnt.", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSpeechToText();
            } else {
                Toast.makeText(StartPageActivity.this, "Die Aufnahmeberechtigung wurde nicht gewährt", Toast.LENGTH_SHORT).show();
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
                recordedVideoUri = videoUri; // Store the URI of the recorded video

                // Add code here to redirect to the NotesActivity
                Intent intent = new Intent(StartPageActivity.this, NotesActivity.class);
                intent.putExtra("fromMainActivity", true); // Pass the value true to indicate that the redirection is from MainActivity
                startActivityForResult(intent, REQUEST_CODE_NOTES_ACTIVITY);
            }
        }

        if (requestCode == REQUEST_SPEECH_RECOGNIZER && resultCode == RESULT_OK && data != null) {
            String recognizedSpeech = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
            editTextDestination.setText(recognizedSpeech);
        }
    }





    private void stopReading() {
        Log.d("StopReading", "Stop reading called");
        // Stop any ongoing audio playback using the MediaPlayer
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }


    private void checkPermissionAndStartSpeechToText() {
        // Check if the permission has already been granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startSpeechToText();
        } else {
            // Permission has not been granted, request it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSION_CODE);
        }
    }

    private void startSpeechToText() {
        Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Spracheingabe starten...");

        startActivityForResult(speechRecognizerIntent, REQUEST_SPEECH_RECOGNIZER);
    }
    @Override
    public void onBackPressed() {
        Log.d("BackButton", "Back button pressed");
        super.onBackPressed();
        stopReading();
    }
    @Override
    public void onStop() {
        super.onStop();
        stopReading();
    }

}
