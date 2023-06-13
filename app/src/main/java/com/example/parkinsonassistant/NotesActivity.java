package com.example.parkinsonassistant;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
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

public class NotesActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION_CODE = 1;
    private static final int REQUEST_SPEECH_RECOGNIZER = 2;
    private static final int PERMISSION_REQUEST_CODE = 3;

    private Button btnSpeechToText;
    private Button btnSave;
    private DatabaseHelper databaseHelper;

    private EditText editTextNote;
    private int selectedSmiley = -1; // Variable zum Speichern des ausgewählten Smileys

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        editTextNote = findViewById(R.id.editTextNote);
        btnSave = findViewById(R.id.buttonSave);
        databaseHelper = new DatabaseHelper(this);

        // AlertDialog anzeigen
        AlertDialog.Builder welcomeBuilder = new AlertDialog.Builder(NotesActivity.this);
        welcomeBuilder.setTitle("Hallo und Willkommen zum Symptomtagebuch");
        welcomeBuilder.setMessage("Hier können Sie Ihr Befinden des Tages abtragen, verbal oder durch ein entsprechendes Gefühl. Verwenden Sie auch ruhig die Sprachfunktion!");
        welcomeBuilder.setPositiveButton("OK", null); // Verwende null als OnClickListener, um keine Aktion auszuführen
        AlertDialog welcomeDialog = welcomeBuilder.create();
        welcomeDialog.show();

        editTextNote = findViewById(R.id.editTextNote);

        Intent intent = getIntent();
        boolean isFromMainActivity = intent.getBooleanExtra("fromMainActivity", false);

        // Zeige den AlertDialog nur an, wenn die Aktivität von der MainActivity weitergeleitet wurde
        if (isFromMainActivity) {
            AlertDialog.Builder videoFinishedBuilder = new AlertDialog.Builder(NotesActivity.this);
            videoFinishedBuilder.setTitle("Vielen Dank");
            videoFinishedBuilder.setMessage("Die Aufnahme ist fertig. Wie würden Sie Ihren heutigen Zustand beschreiben? Antworten Sie verbal oder wählen Sie einen entsprechenden Button aus!");
            videoFinishedBuilder.setPositiveButton("OK", null); // Verwende null als OnClickListener, um keine Aktion auszuführen
            AlertDialog videoFinishedDialog = videoFinishedBuilder.create();
            videoFinishedDialog.show();
        }

        // Prüfen, ob die Berechtigung bereits gewährt wurde
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Berechtigung wurde nicht gewährt, fordere sie an
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
        }

        btnSpeechToText = findViewById(R.id.btn_speech_to_text);
        btnSpeechToText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionAndStartSpeechToText();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNoteToDatabase();
            }
        });

        // Klick-Listener für die vorgefertigten Textbausteine (Smileys)
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
    }

    private void saveNoteToDatabase() {
        String noteContent = editTextNote.getText().toString();

        // Öffnen der Datenbankverbindung
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        // Erstellen Sie ein ContentValues-Objekt, um die Daten einzufügen
        ContentValues values = new ContentValues();
        values.put("content", noteContent);

        // Fügen Sie die Daten in die Datenbank ein
        long newRowId = db.insert("notes", null, values);

        if (newRowId != -1) {
            // Wenn das Einfügen erfolgreich war
            Toast.makeText(NotesActivity.this, "Notiz gespeichert", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            // Wenn das Einfügen fehlgeschlagen ist
            Toast.makeText(NotesActivity.this, "Fehler beim Speichern der Notiz", Toast.LENGTH_SHORT).show();
        }

        // Schließen Sie die Datenbankverbindung
        db.close();
    }


    private void saveNote() {
        String noteContent = editTextNote.getText().toString().trim();
        if (!noteContent.isEmpty()) {
            // Öffnen der Datenbankverbindung
            SQLiteDatabase db = databaseHelper.getWritableDatabase();

            // Erstellen Sie ein ContentValues-Objekt, um die Daten einzufügen
            ContentValues values = new ContentValues();
            values.put("content", noteContent);

            // Fügen Sie den Datensatz in die Datenbank ein
            long newRowId = db.insert("notes", null, values);

            // Schließen der Datenbankverbindung
            db.close();
            Intent updateIntent = new Intent(NotesActivity.this, MainActivity.class);
            updateIntent.putExtra("updateNotes", true);
            startActivity(updateIntent);
            // Hier die Weiterleitung zur TimelineActivity hinzufügen
            Intent intent = new Intent(NotesActivity.this, TimelineActivity.class);
            intent.putExtra("selectedSmiley", selectedSmiley);
            startActivity(intent);

        } else {
            Toast.makeText(NotesActivity.this, "Bitte geben Sie eine Notiz ein", Toast.LENGTH_SHORT).show();
        }
    }





    private void checkPermissionAndStartSpeechToText() {
        // Überprüfen, ob die Berechtigung bereits erteilt wurde
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startSpeechToText();
        } else {
            // Berechtigung wurde nicht erteilt, fordere sie an
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
}