package com.example.parkinsonassistant;

public class VideoUtils {
    public static String generateUniqueFilePath(String videoPath) {
        // Get the file name from the video path
        String fileName = videoPath.substring(videoPath.lastIndexOf("/") + 1);

        // Check if the file name meets the condition
        if (fileName.startsWith("video_")) {
            return videoPath; // Return the original video path if the condition is met
        } else {
            return null; // Return null if the condition is not met (to exclude the video from being displayed)
        }
    }
}

