package com.example.parkinsonassistant;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;
@Entity(tableName = "notes")
public class Note {
    @PrimaryKey(autoGenerate = true)
    private int id;  // Unique identifier for the note

    private String date;  // Date associated with the note
    private String content;  // Content of the note
    private String noteText;  // Text of the note

    private Date timestamp;  // Timestamp of the note

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getNoteText() {
        return noteText;
    }

    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
