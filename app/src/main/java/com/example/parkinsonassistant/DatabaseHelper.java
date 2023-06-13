package com.example.parkinsonassistant;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "notes.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Erstellen Sie die benötigten Tabellen in der Datenbank
        String createTableQuery = "CREATE TABLE notes (_id INTEGER PRIMARY KEY AUTOINCREMENT, content TEXT)";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Aktualisieren Sie die Datenbank, falls eine neue Version verfügbar ist
        // Hier können Sie den Code zum Aktualisieren der Datenbankschema hinzufügen
    }
}

