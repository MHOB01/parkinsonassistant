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
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
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
    private static boolean isFirstTime = true;
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 100;
    private static final int REQUEST_CODE_VIDEO_RECORD = 101;
    private TextView txtLastOnline;

    private TextToSpeech textToSpeech;

    private static final int REQUEST_VIDEO_CAPTURE = 4;

    private Button buttonCamera;


    private Button btnDiary;
    private Button btnRecordVideo;
    private Button btnHome;

    private boolean isAlertDialogDisplayed = false;

    private static final String TAG = "StartPageActivity";
    private MediaPlayer mediaPlayer;

    private boolean isTextToSpeechInitialized = false;

    private TextToSpeech tts;
    private Button btnSpeechToText;

    private EditText editTextDestination;

    private AlertDialog alertDialog;







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_page);

        // Display introductory text
        TextView txtIntro = findViewById(R.id.txt_intro);
        txtIntro.setText("Hallo und willkommen zu Ihrem digitalen Symptomtagebuch. Was möchten Sie heute machen?");

        // EditText for destination input
        editTextDestination = findViewById(R.id.editTextDestination);


        btnSpeechToText = findViewById(R.id.btn_speech_to_text);
        btnSpeechToText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopReading();
                MediaPlayerManager.stopAudio();
                checkPermissionAndStartSpeechToText();
            }
        });

        // Button for the diary
        btnDiary = findViewById(R.id.btn_diary);
        btnDiary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopReading(); // Stop reading aloud
                MediaPlayerManager.stopAudio();
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
                MediaPlayerManager.stopAudio();
                showWelcomeAlertDialog(); // AlertDialog wird angezeigt, wenn der Button geklickt wird

                // Willkommensnachricht vorgelesen, nachdem der Button geklickt wurde
                if (isTextToSpeechInitialized) {
                    speakWelcomeMessage();
                }
            }
        });


        // Button for the home page
        btnHome = findViewById(R.id.btn_home);
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopReading(); // Stop reading aloud
                MediaPlayerManager.stopAudio();
                startActivity(new Intent(StartPageActivity.this, MainActivity.class)); // Open the home page activity
            }
        });



        editTextDestination = findViewById(R.id.editTextDestination);

        // Add a TextWatcher to the editTextDestination
        editTextDestination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // This method is not used in this case
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // This method is not used in this case
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Get the destination text from the EditText
                String destination = editable.toString().trim(); // Trim to remove leading/trailing spaces

                // Check if the destination is not empty
                if (!destination.isEmpty()) {
                    // Check the destination and navigate accordingly
                    if (destination.equalsIgnoreCase("Menü") || destination.equalsIgnoreCase("Menü ")) {
                        startActivity(new Intent(StartPageActivity.this, MainActivity.class));
                    } else if (destination.equalsIgnoreCase("Video aufnehmen") || destination.equalsIgnoreCase("Video aufnehmen ")) {
                        showVideoRecordingAlertDialog();
                    } else if (destination.equalsIgnoreCase("Tagebuch") || destination.equalsIgnoreCase("Tagebuch ")) {
                        startActivity(new Intent(StartPageActivity.this, NotesActivity.class));
                    } else {
                        // If the destination doesn't match any of the predefined values, show an error message
                        Toast.makeText(StartPageActivity.this, "Ungültiges Ziel.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // If the input is empty, show an error message
                    Toast.makeText(StartPageActivity.this, "Bitte geben Sie ein Ziel ein.", Toast.LENGTH_SHORT).show();
                }
            }
        });



// Initialisierung der Text-to-Speech-Funktionalität
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {

                    isTextToSpeechInitialized = true;
                } else {
                    // Fehler bei der Initialisierung der Sprachsynthese
                    Toast.makeText(StartPageActivity.this, "Fehler bei der Initialisierung der Text-to-Speech-Funktionalität", Toast.LENGTH_SHORT).show();
                }
            }
        });



        // TextView for "last online" text
        txtLastOnline = findViewById(R.id.txt_last_online);
        updateLastOnlineText();

        if (isFirstTime) {
            MediaPlayerManager.playAudio(this, R.raw.symptomtagebuch);
            isFirstTime = false;
        }
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
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
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
        if (!isAlertDialogDisplayed) {
            stopReading();
            MediaPlayerManager.stopAudio();
            stopTextToSpeech();
            if (tts != null) {
                tts.stop(); // Stop text-to-speech playback
            }
        }
        super.onBackPressed();
    }





    @Override
    public void onStop() {
        super.onStop();
        stopReading();
        MediaPlayerManager.stopAudio();
        stopTextToSpeech();
    }




    private void showVideoRecordingAlertDialog() {
        // Show the welcome message dialog
        isAlertDialogDisplayed = true; // Set the flag to true when the AlertDialog is displayed
        speakWelcomeMessage();
        AlertDialog.Builder builder = new AlertDialog.Builder(StartPageActivity.this);
        // Rest of your AlertDialog code

        // Set the title and message for the AlertDialog
        builder.setTitle("Willkommensnachricht");
        builder.setMessage("Hallo und willkommen zu Ihren digitalen Übungen! Vor dem Start der Kamera folgen Sie bitte der folgenden Anleitung:\n\n" +
                "1. Stellen Sie sicher, dass Sie ausreichend Platz haben und sich in einer ruhigen Umgebung befinden.\n" +
                "2. Bitte positionieren Sie Ihr Gerät in einer stabilen Position vor sich.\n" +
                "3. Stellen Sie sich vor die Kamera, bis Sie Ihren gesamten Körper sehen können.\n" +
                "4. Bitte befolgen Sie dazu die kommenden Anweisungen.\n\nViel Spaß und gutes Gelingen!");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss(); // Dismiss the AlertDialog when the "OK" button is clicked
                stopTextToSpeech();
                // Open the CameraManager
                Intent intent = new Intent(StartPageActivity.this, FaceDetection.class);
                startActivityForResult(intent, REQUEST_CODE_VIDEO_RECORD);
            }
        });

        alertDialog = builder.create(); // Create the AlertDialog instance
        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                isAlertDialogDisplayed = false; // Set the flag back to false when the AlertDialog is dismissed
                stopTextToSpeech(); // Stop text-to-speech playback when the AlertDialog is dismissed
            }
        });

        alertDialog.show(); // Show the AlertDialog
    }



    private void speakWelcomeMessage() {
        String welcomeMessage = "Hallo und willkommen zu Ihren digitalen Übungen! Vor dem Start der Kamera folgen Sie bitte der folgenden Anleitung:\n\n" +
                "erstens: Stellen Sie sicher, dass Sie ausreichend Platz haben und sich in einer ruhigen Umgebung befinden.\n" +
                "zweitens: Bitte positionieren Sie Ihr Gerät in einer stabilen Position vor sich.\n" +
                "drittens: Stellen Sie sich vor die Kamera, bis Sie Ihren gesamten Körper sehen können.\n" +
                "viertens: Bitte befolgen Sie dazu die kommenden Anweisungen.\n\nViel Spaß und gutes Gelingen!";
        speakMessage(welcomeMessage);
    }

    private void showWelcomeAlertDialog() {
        speakWelcomeMessage();
        AlertDialog.Builder builder = new AlertDialog.Builder(StartPageActivity.this);
        builder.setTitle("Willkommensnachricht");
        builder.setMessage("Hallo und willkommen zu Ihren digitalen Übungen! Vor dem Start der Kamera folgen Sie bitte der folgenden Anleitung:\n\n" +
                "1. Stellen Sie sicher, dass Sie ausreichend Platz haben und sich in einer ruhigen Umgebung befinden.\n" +
                "2. Bitte positionieren Sie Ihr Gerät in einer stabilen Position vor sich.\n" +
                "3. Stellen Sie sich vor die Kamera, bis Sie Ihren gesamten Körper sehen können.\n" +
                "4. Bitte befolgen Sie dazu die kommenden Anweisungen.\n\nViel Spaß und gutes Gelingen!");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Öffnen des CameraManagers nachdem die Willkommensnachricht gesprochen wurde
                Intent intent = new Intent(StartPageActivity.this, FaceDetection.class);
                startActivityForResult(intent, REQUEST_CODE_VIDEO_RECORD);
            }
        });

        alertDialog = builder.create();
        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                isAlertDialogDisplayed = false; // Set the flag back to false when the AlertDialog is dismissed
                stopTextToSpeech(); // Stop text-to-speech playback when the AlertDialog is dismissed
            }
        });

        // Show the AlertDialog
        alertDialog.show();

    }


    private void speakMessage(String message) {
        if (isTextToSpeechInitialized && textToSpeech != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    private void stopTextToSpeech() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

    }



}
