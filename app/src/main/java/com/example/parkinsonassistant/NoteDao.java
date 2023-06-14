package com.example.parkinsonassistant;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NoteDao {
    @Insert
    void insert(Note note);

    @Query("SELECT * FROM notes")
    List<Note> getAllNotes();
    // Additional database operations...
}

