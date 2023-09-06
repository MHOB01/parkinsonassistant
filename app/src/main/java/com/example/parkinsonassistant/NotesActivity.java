package com.example.parkinsonassistant;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
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

    private TextView textViewMessage;


    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            updateSaveButtonState();
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        // Show AlertDialog
        AlertDialog.Builder welcomeBuilder = new AlertDialog.Builder(NotesActivity.this);
        welcomeBuilder.setTitle("Hallo und Willkommen zum Symptomtagebuch");
        welcomeBuilder.setMessage("Bitte bewerten Sie, wie es Ihnen heute geht und wie Sie sich während der Übung gefühlt haben, " +
                "indem Sie ein entsprechend passendes Gesicht oder Smiley auswählen. Sie können zusätzlich das ausgewählte Gesicht " +
                "schriftlich beschreiben oder die Sprachfunktion verwenden, um Ihre Gefühle mitzuteilen.");
        welcomeBuilder.setPositiveButton("OK", (dialog, which) -> stopTextToSpeech());
        alertDialog = welcomeBuilder.create();
        alertDialog.show();

        textToSpeech = new TextToSpeech(this, this);

        editTextNote = findViewById(R.id.editTextNote);

        editTextNote.addTextChangedListener(textWatcher);
        textViewMessage = findViewById(R.id.textViewMessage);

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
        btnSave.setEnabled(false);
        noteDao = NotesDatabase.getInstance(this).noteDao();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String noteText = editTextNote.getText().toString().trim();

                if (selectedSmiley != -1) {
                    // Smiley ausgewählt, speichere die Notiz
                    saveNote();
                } else if (!noteText.isEmpty()) {
                    // Kein Smiley ausgewählt, aber Text ist nicht leer, zeige eine Fehlermeldung an oder führe eine andere Aktion aus
                    Toast.makeText(NotesActivity.this, "Bitte wählen Sie einen Smiley aus.", Toast.LENGTH_SHORT).show();
                } else {
                    // Weder Smiley ausgewählt noch Text eingegeben, zeige eine Fehlermeldung an oder führe eine andere Aktion aus
                    Toast.makeText(NotesActivity.this, "Bitte wählen Sie einen Smiley aus und geben Sie einen Text ein.", Toast.LENGTH_SHORT).show();
                }
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



        editTextNote.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSaveButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {
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

            SharedPreferences preferences = getSharedPreferences("TimelinePrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("selectedSmiley", selectedSmiley);
            editor.apply();

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

            // Erhalte den aktuellen Inhalt des EditText
            String currentText = editTextNote.getText().toString();

            // Kombiniere den erkannten Text mit dem aktuellen Inhalt und setze ihn in den EditText
            editTextNote.setText(currentText + " " + recognizedSpeech);

            // Optional: Setze den Cursor ans Ende des Textes, um die Eingabe fortzusetzen
            editTextNote.setSelection(editTextNote.getText().length());

            // Aktualisiere den Zustand des Save-Buttons
            updateSaveButtonState();
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
            speakMessage("Hallo und Willkommen zum Symptomtagebuch. Bitte bewerten Sie, wie es Ihnen heute geht und wie Sie sich während der Übung gefühlt haben, indem Sie ein entsprechend passendes Gesicht oder Smiley auswählen. Sie können zusätzlich das ausgewählte Gesicht schriftlich beschreiben oder die Sprachfunktion verwenden, um Ihre Gefühle mitzuteilen.");
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
        editTextNote.removeTextChangedListener(textWatcher);
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
    }


    private void updateSaveButtonState() {
        String noteText = editTextNote.getText().toString().trim();

        // Prüfe, ob ein Smiley im EditText enthalten ist
        boolean containsSmiley = noteText.contains("😄") || noteText.contains("😊")
                || noteText.contains("😐") || noteText.contains("😞") || noteText.contains("😢");

        if (selectedSmiley != -1 && containsSmiley) {
            btnSave.setEnabled(true);
            textViewMessage.setVisibility(View.GONE); // Hide the message TextView
        } else {
            btnSave.setEnabled(false);
            textViewMessage.setVisibility(View.VISIBLE); // Show the message TextView
        }
    }


}