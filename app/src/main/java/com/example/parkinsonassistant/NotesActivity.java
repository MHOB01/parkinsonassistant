package com.example.parkinsonassistant;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import java.util.Locale;
import java.util.concurrent.Executors;

import androidx.room.Database;
import androidx.room.Room;


public class NotesActivity extends AppCompatActivity implements TextToSpeech.OnInitListener{

    private static final int REQUEST_PERMISSION_CODE = 1;
    private static final int REQUEST_SPEECH_RECOGNIZER = 2;
    private static final int PERMISSION_REQUEST_CODE = 3;

    private Button btnSpeechToText;
    private Button btnSave;

    private EditText editTextNote;
    private int selectedSmiley = -1; // Variable zum Speichern des ausgewählten Smileys
    private NotesDatabase notesDatabase;

    private TextToSpeech textToSpeech;

    Note note = new Note();
    NoteDao noteDao;
    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        // Show AlertDialog
        AlertDialog.Builder welcomeBuilder = new AlertDialog.Builder(NotesActivity.this);
        welcomeBuilder.setTitle("Hallo und Willkommen zum Symptomtagebuch");
        welcomeBuilder.setMessage("Hier können Sie Ihr Befinden des Tages und während der Übung abtragen, verbal oder durch ein entsprechendes Gefühl. Verwenden Sie auch ruhig die Sprachfunktion!");
        welcomeBuilder.setPositiveButton("OK", (dialog, which) -> stopTextToSpeech());
        alertDialog = welcomeBuilder.create();
        alertDialog.show();

        textToSpeech = new TextToSpeech(this, this);

        editTextNote = findViewById(R.id.editTextNote);


        // Check if the permission has already been granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Permission has not been granted, request it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
        }

        btnSpeechToText = findViewById(R.id.btn_speech_to_text);
        btnSpeechToText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionAndStartSpeechToText();
            }
        });

        btnSave = findViewById(R.id.buttonSave);
        noteDao = NotesDatabase.getInstance(this).noteDao();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        // Click Listeners for pre-defined text elements (smileys)
        TextView imgSmiley1 = findViewById(R.id.img_smiley_1);
        TextView imgSmiley2 = findViewById(R.id.img_smiley_2);
        TextView imgSmiley3 = findViewById(R.id.img_smiley_3);
        TextView imgSmiley4 = findViewById(R.id.img_smiley_4);
        TextView imgSmiley5 = findViewById(R.id.img_smiley_5);

        imgSmiley1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedSmiley = 1; // Smiley 1
                editTextNote.setText("😄");
            }
        });

        imgSmiley2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedSmiley = 2; // Smiley 2
                editTextNote.setText("😊");
            }
        });

        imgSmiley3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedSmiley = 3; // Smiley 3
                editTextNote.setText("😐");
            }
        });

        imgSmiley4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedSmiley = 4; // Smiley 4
                editTextNote.setText("😞");
            }
        });

        imgSmiley5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedSmiley = 5; // Smiley 5
                editTextNote.setText("😢");
            }
        });

        notesDatabase = Room.databaseBuilder(getApplicationContext(), NotesDatabase.class, "notes-db")
                .build();

        noteDao = notesDatabase.noteDao();
    }

    private void saveNote() {
        String noteText = editTextNote.getText().toString();
        Note note = new Note();
        note.setNoteText(noteText);
        if (!noteText.isEmpty()) {
            // Set the current timestamp
            note.setTimestamp(new Date());

            // Execute database operations in the background
            new SaveNoteTask().execute(note);

            // Add redirection to TimelineActivity here
            Intent intent = new Intent(NotesActivity.this, TimelineActivity.class);
            intent.putExtra("selectedSmiley", selectedSmiley);
            startActivity(intent);
        } else {
            Toast.makeText(NotesActivity.this, "Bitte geben Sie eine Notiz ein", Toast.LENGTH_SHORT).show();
        }
    }

    private class SaveNoteTask extends AsyncTask<Note, Void, Void> {
        @Override
        protected Void doInBackground(Note... notes) {
            noteDao.insert(notes[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Intent intent = new Intent(NotesActivity.this, TimelineActivity.class);
            intent.putExtra("selectedSmiley", selectedSmiley);
            startActivity(intent);
            finish();
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SPEECH_RECOGNIZER && resultCode == RESULT_OK && data != null) {
            String recognizedSpeech = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
            editTextNote.setText(recognizedSpeech);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSpeechToText();
            } else {
                Toast.makeText(NotesActivity.this, "Die Aufnahmeberechtigung wurde nicht gewährt", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // Sprachsynthese erfolgreich initialisiert
            speakMessage("Hallo und Willkommen zum Symptomtagebuch. Hier können Sie Ihr Befinden des Tages und während der Übung abtragen, verbal oder durch ein entsprechendes Gefühl. Verwenden Sie auch ruhig die Sprachfunktion!");
        } else {
            // Fehler bei der Initialisierung der Sprachsynthese
            Toast.makeText(this, "Fehler bei der Initialisierung der Text-to-Speech-Funktionalität", Toast.LENGTH_SHORT).show();
        }
    }

    private void speakMessage(String message) {
        if (textToSpeech != null) {
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
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
    }
}