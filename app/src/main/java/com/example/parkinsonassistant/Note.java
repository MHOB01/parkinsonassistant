package com.example.parkinsonassistant;

public class Note {
    private String date;
    private String content;

    public Note(String date, String content) {
        this.date = date;
        this.content = content;
    }

    public String getDate() {
        return date;
    }

    public String getContent() {
        return content;
    }
}


