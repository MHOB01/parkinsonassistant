package com.example.parkinsonassistant;

import android.net.Uri;

public class ModelVideo {
    private String smiley;  // Represents a smiley associated with the video

    private long id;  // Unique identifier for the video
    private Uri data;  // Uri of the video file
    private String title;  // Title of the video
    private String duration;  // Duration of the video

    public ModelVideo(long id, Uri data, String title, String duration) {
        this.id = id;
        this.data = data;
        this.title = title;
        this.duration = duration;
    }

    public String getSmiley() {
        return smiley;
    }

    public void setSmiley(String smiley) {
        this.smiley = smiley;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Uri getData() {
        return data;
    }

    public void setData(Uri data) {
        this.data = data;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}

