package com.example.parkinsonassistant;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Note.class}, version = 2)
public abstract class NotesDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "notes_db";

    private static NotesDatabase instance;

    public abstract NoteDao noteDao();

    public static NotesDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (NotesDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(), NotesDatabase.class, "notes-db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}

