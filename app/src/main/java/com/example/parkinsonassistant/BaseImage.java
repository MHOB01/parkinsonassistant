package com.example.parkinsonassistant;

import android.annotation.SuppressLint;
import android.Manifest;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.Image;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.VideoCapture;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;


public class BaseImage implements ImageAnalysis.Analyzer {
    public static final int REQUEST_VIDEO_CAPTURE = 1003;
    FirebaseVisionFaceDetector firebaseVisionFaceDetector;
    private DrawLayerAroundFace drawLayerAroundFace;
    private boolean isRedirecting = false;
    private boolean isRecordingScheduled = false;
    private TextView textViewStatus;
    private boolean isRecording = false;
    private int ovalCenterX;
    private int ovalCenterY;
    private Context context;
    TextToSpeech textToSpeech;
    boolean right = true;
    boolean down = true;
    boolean left = true;
    boolean smile = true;
    private AlertDialog customAlertDialog;
    private boolean isWelcomeMessageShown = false;

    private float ovalRadius;
    boolean up = true;
    private boolean isRecordingActive = false;
    private long startTime;
    private ArrayList<String> messagesList = new ArrayList<>();
    private int currentMessageIndex = 0;
    private static final long MESSAGE_INTERVAL = 5000; // 5 seconds
    private boolean waitingForUserConfirmation = false;
    private boolean isVideoRecordingStarted = false;
    private static final long WELCOME_MESSAGE_DURATION = 12000; // 12 seconds

    private TextView timerTextView;

    RelativeLayout rootview;
    TextView textView;
    Activity activity;

    boolean isOvalPositioned = false; // Variable, um den Status des Ovals zu verfolgen
    private PermanentOvalView permanentOvalView;

    private static final long GREEN_OVAL_DURATION = 3000; // 3 Sekunden in Millisekunden
    private boolean isInGreenOval = false;
    private long greenOvalStartTime = 0;

    private ImageButton captureButton;
    private boolean isFirstMessageShown = false;
    private boolean previousFaceState = false;
    private MediaRecorder mediaRecorder;
    private Handler recordingHandler = new Handler();
    private boolean isRecordingPaused = false;
    private Handler textToSpeechHandler = new Handler();
    private Runnable startRecordingRunnable = new Runnable() {
        @Override
        public void run() {

            startVideoRecording();

        }
    };
    public BaseImage(Context context, TextToSpeech textToSpeech, RelativeLayout view, TextView textView, ImageButton captureButton) {
        this.context = context;
        this.textToSpeech = textToSpeech;
        this.rootview = view;
        this.textView = textView;
        this.captureButton = captureButton;


        textViewStatus = view.findViewById(R.id.textViewStatus);

        timerTextView = view.findViewById(R.id.timerTextView);


        permanentOvalView = rootview.findViewById(R.id.permanentOvalView);
        setupMediaRecorder();
        showWelcomeMessage();
    }

    public boolean isRecordingActive() {
        return isRecordingActive;
    }

    private void updateFaceStatus(boolean isInside) {
        if (isInside) {
            textViewStatus.setText("Sie stehen richtig"); // Set the appropriate text when face is inside the oval

        } else {
            textViewStatus.setText("Gesicht außerhalb des Ovals"); // Set the appropriate text when face is outside the oval
        }
    }


    private int mapRotation(ImageProxy imageProxy) {
        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
        int rotation;
        switch (rotationDegrees) {
            case 0:
                rotation = FirebaseVisionImageMetadata.ROTATION_0;
                break;
            case 90:
                rotation = FirebaseVisionImageMetadata.ROTATION_90;
                break;
            case 180:
                rotation = FirebaseVisionImageMetadata.ROTATION_180;
                break;
            case 270:
                rotation = FirebaseVisionImageMetadata.ROTATION_270;
                break;
            default:
                rotation = FirebaseVisionImageMetadata.ROTATION_0;
        }
        return rotation;
    }



    private double estimateHeadTilt(FirebaseVisionFace firebaseVisionFace) {
        FirebaseVisionFaceLandmark leftEye = firebaseVisionFace.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE);
        FirebaseVisionFaceLandmark rightEye = firebaseVisionFace.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE);
        FirebaseVisionFaceLandmark noseBase = firebaseVisionFace.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE);

        if (leftEye != null && rightEye != null && noseBase != null) {
            FirebaseVisionPoint leftEyePos = leftEye.getPosition();
            FirebaseVisionPoint rightEyePos = rightEye.getPosition();
            FirebaseVisionPoint noseBasePos = noseBase.getPosition();

            double eyeDistance = Math.sqrt(Math.pow(rightEyePos.getX() - leftEyePos.getX(), 2) + Math.pow(rightEyePos.getY() - leftEyePos.getY(), 2));
            double noseTilt = Math.atan2(noseBasePos.getY() - (leftEyePos.getY() + rightEyePos.getY()) / 2, noseBasePos.getX() - (leftEyePos.getX() + rightEyePos.getX()) / 2);
            double headTiltDegrees = Math.toDegrees(noseTilt);

            return headTiltDegrees;
        }

        return 0; // Return 0 if the necessary landmarks are not detected
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

    public void toggleRecording() {
        if (isRecordingActive) {
            stopVideoRecording();
        } else {
            startVideoRecording();
        }
    }


    private void cancelShowingMessages() {
        new Handler().removeCallbacks(this::showNextMessage);
        currentMessageIndex = 0;
    }

    private void setupMediaRecorder() {
        // Initialize MediaRecorder if it's not already initialized
        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
        } else {
            // Reset MediaRecorder if it's already initialized
            mediaRecorder.reset();
        }

        // Set up the video source and output format
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

        // Set the video encoder (H.264 or H.265)
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        // Set the output file path for the recorded video
        String videoFilePath = getVideoFilePath();
        if (videoFilePath != null) {
            mediaRecorder.setOutputFile(videoFilePath);
        } else {
            // Error: Failed to get a valid file path
            Toast.makeText(context, "Failed to start video recording. Storage not available.", Toast.LENGTH_SHORT).show();
            isRecordingScheduled = false;
            return;
        }

        // Set the video size (adjust the values as needed)
        mediaRecorder.setVideoSize(1280, 720); // 720p resolution
        // Set the video frame rate (adjust the value as needed)
        mediaRecorder.setVideoFrameRate(30); // 30 frames per second


        // Prepare the MediaRecorder
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            // Handle any exceptions that may occur during recording setup
            // Display an error message to the user or take appropriate action
            Toast.makeText(context, "Failed to start video recording!", Toast.LENGTH_SHORT).show();
            isRecordingScheduled = false;
            Log.e("VideoRecording", "Failed to start video recording: " + e.getMessage());
        }
    }

    private void dismissCustomAlertDialogWithDelay(long delayMillis) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isActivityRunning()) {
                    if (customAlertDialog != null && customAlertDialog.isShowing()) {
                        customAlertDialog.dismiss();
                    }
                }
            }
        }, delayMillis);
    }

    // New method to schedule showing the next message after a delay
    private void scheduleNextMessage() {
        currentMessageIndex++;
        if (currentMessageIndex < messagesList.size()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    showNextMessage();
                }
            }, currentMessageIndex == 1 ? 2000 : 8000);
        } else {
            // If there are no more messages, stop the video recording immediately
            stopVideoRecording();
        }
    }



    // Create a method to show the custom alert dialog with the message
    // Modify the showCustomAlertDialog() method like this:
    private void showCustomAlertDialog(String message) {
        if (isActivityRunning()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            // Inflate the custom layout for the title
            View customTitleView = LayoutInflater.from(context).inflate(R.layout.custom_alert_dialog_title, null);
            TextView titleTextView = customTitleView.findViewById(R.id.alert_dialog_title);
            titleTextView.setText("Anweisung");

            // Set the font size for the title
            titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);

            // Set the text color for the title based on the background color
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true);
            int backgroundColor = typedValue.data;

            double contrastWithWhite = contrastRatio(backgroundColor, Color.WHITE);
            double contrastWithBlack = contrastRatio(backgroundColor, Color.BLACK);

            int titleTextColor;
            if (contrastWithWhite >= 4.5) {
                titleTextColor = Color.WHITE;
            } else {
                titleTextColor = Color.BLACK;
            }

            titleTextView.setTextColor(titleTextColor);

            // Set the title view for the AlertDialog
            builder.setCustomTitle(customTitleView);

            // Inflate the custom layout for the message
            View customView = LayoutInflater.from(context).inflate(R.layout.custom_alert_dialog_message, null);
            TextView messageTextView = customView.findViewById(R.id.alert_dialog_message);
            messageTextView.setText(message);


            // Determine the appropriate text color based on the contrast ratio
            int textColor;
            if (contrastWithWhite >= 4.5) {
                textColor = Color.WHITE;
            } else {
                textColor = Color.BLACK;
            }

            messageTextView.setTextColor(textColor);

            // Set the background color to black or the system's default background color
            customView.setBackgroundColor(backgroundColor);

            builder.setView(customView)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Dismiss the current AlertDialog
                            if (customAlertDialog != null && customAlertDialog.isShowing()) {
                                customAlertDialog.dismiss();
                            }

                            // Schedule showing the next message after the delay
                            stopSpeaking();
                            scheduleNextMessage();
                            isWelcomeMessageShown = false;
                        }
                    })
                    .setCancelable(false);

            // Create and show the AlertDialog
            customAlertDialog = builder.create();
            customAlertDialog.show();

            // Start speaking the message
            speakMessage(message);

            // Dismiss the AlertDialog after the entire message is read
            int messageLengthInMillis = message.split("\\s+").length * 400; // Estimated reading time per word in milliseconds
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (customAlertDialog != null && customAlertDialog.isShowing()) {
                        // Dismiss the AlertDialog after the message is fully read
                        customAlertDialog.dismiss();
                        // Schedule showing the next message after the delay
                        scheduleNextMessage();
                        isWelcomeMessageShown = false;
                    }
                }
            }, messageLengthInMillis + 1000); // Add some extra delay to ensure the message is fully read before dismissing
        }
    }


    private double contrastRatio(int color1, int color2) {
        double luminance1 = (0.2126 * Color.red(color1) + 0.7152 * Color.green(color1) + 0.0722 * Color.blue(color1)) / 255;
        double luminance2 = (0.2126 * Color.red(color2) + 0.7152 * Color.green(color2) + 0.0722 * Color.blue(color2)) / 255;

        if (luminance1 > luminance2) {
            return (luminance1 + 0.05) / (luminance2 + 0.05);
        } else {
            return (luminance2 + 0.05) / (luminance1 + 0.05);
        }
    }






    // Add this method to show the next message after a delay
    private void showNextMessage() {
        if (currentMessageIndex < messagesList.size()) {
            String message = messagesList.get(currentMessageIndex);
            showCustomAlertDialog(message);
        }
    }








    // Modify your startVideoRecording() method like this:
    public void startVideoRecording() {
        if (checkStoragePermission()) {
            if (isRecordingActive) {
                // Already recording, do nothing
                return;
            }

            if (mediaRecorder == null) {
                setupMediaRecorder();
            }
            Log.d("VideoRecording", "Vor dem Starten der Aufnahme");
            try {
                // Start recording
                mediaRecorder.start();
                isRecording = true;
                isRecordingActive = true;
                isInGreenOval = false; // Reset the flag when recording starts

                captureButton.setImageResource(R.drawable.round_stop_circle_24);
                // Display a message indicating that the video recording has started
                Toast.makeText(context, "Video-Aufnahme gestartet!", Toast.LENGTH_SHORT).show();
                // Add log messages for debugging
                Log.d("VideoRecording", "Video recording started.");
                Log.d("VideoRecording", "Video recording started at " + System.currentTimeMillis());
                isVideoRecordingStarted = true;
                // Schedule the recording to stop after the specified duration

            } catch (Exception e) {
                e.printStackTrace();
                // Handle any exceptions that may occur during recording setup or start
                // Display an error message to the user or take appropriate action
                Toast.makeText(context, "Video-Aufnahme fehlgeschlagen", Toast.LENGTH_SHORT).show();
                isRecordingScheduled = false;
                Log.e("VideoRecording", "Failed to start video recording: " + e.getMessage());
            }
        } else {
            // If the permission is not granted, request the permission
            requestStoragePermission();
        }

        // Initialize currentMessageIndex to 0 before showing the first message
        currentMessageIndex = 0;
        messagesList.clear();
        messagesList.add("Vielen Dank, jetzt sind Sie gut zu erkennen. Die Aufnahme hat jetzt gestartet. Achten Sie darauf, Ihre Hände in der Kamerasicht zu halten.");

        messagesList.add("Wir beginnen mit einer einfachen Aufgabe. Bitte gehen Sie 3 Schritte rückwärts von Ihrer Position aus und kommen Sie wieder zurück. Beginnen Sie nach Ablauf dieser Nachricht.");

        messagesList.add("Sehr gut, jetzt berühren Sie in rascher Reihenfolge den Daumen mit dem Zeigefinger. Beginnen Sie nach Ablauf dieser Nachricht.");

        messagesList.add("Vielen Dank, Sie sind fertig mit der Übung. Die Aufnahme stoppt nun automatisch.");

        // Add more messages if needed

        // Show the first message immediately
        showNextMessage();

    }


    private void showWelcomeMessage() {
        isWelcomeMessageShown = true;
        showCustomAlertDialog("Willkommen! Bitte platzieren Sie ihr Gesicht in dem rot umrandeten Bereich, sobald diese Nachricht verschwunden ist, um die Aufnahme zu starten. Wenn er grün wird, stehen Sie richtig.");
        textToSpeech.speak("Willkommen! Bitte platzieren Sie ihr Gesicht in dem rot umrandeten Bereich, sobald diese Nachricht verschwunden ist, um die Aufnahme zu starten. Wenn er grün wird, stehen Sie richtig.", TextToSpeech.QUEUE_FLUSH, null, null);

       // new Handler().postDelayed(() -> isWelcomeMessageShown = false, WELCOME_MESSAGE_DURATION);
    }


    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                            != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions((Activity) context, new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                }, REQUEST_VIDEO_CAPTURE);
            } else {
                // Permissions already granted, start video capture
                startVideoRecording();
            }
        } else {
            // For devices below Android 10, you don't need additional permissions for video recording
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED) {

                // Permissions already granted, start video capture
                startVideoRecording();
            } else {
                // If the permission is not granted, request the permission
                ActivityCompat.requestPermissions((Activity) context, new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                }, REQUEST_VIDEO_CAPTURE);
            }
        }
    }


    private void stopSpeaking() {
        if (textToSpeech != null && textToSpeech.isSpeaking()) {
            textToSpeech.stop();
        }
    }


    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            try {
                // Reset the MediaRecorder to its idle state
                mediaRecorder.reset();
                // Release the MediaRecorder resources
                mediaRecorder.release();
                mediaRecorder = null;
                // Add log message for debugging
                Log.d("VideoRecording", "MediaRecorder released.");
            } catch (Exception e) {
                // Handle any exceptions that may occur during releasing the MediaRecorder
                Log.e("VideoRecording", "Failed to release MediaRecorder: " + e.getMessage());
            }
        }
    }

    public void stopVideoRecording() {
        if (isRecordingActive && isRecording) {
            // Remove the scheduled stop runnable, as we are stopping the recording manually
            recordingHandler.removeCallbacks(startRecordingRunnable);
            cancelShowingMessages();
            try {
                // Pause the recording first
                mediaRecorder.pause();
                // Release the MediaRecorder
                releaseMediaRecorder();
                // Reset the recording flags
                isRecording = false;
                isRecordingActive = false;
                // Add log message for debugging
                Log.d("VideoRecording", "Video recording stopped.");
                Log.d("VideoRecording", "Video recording stopped at " + System.currentTimeMillis());




                // Display a message indicating that the video recording has stopped
                Toast.makeText(context, "Video-Aufnahme beendet!", Toast.LENGTH_SHORT).show();
                long endTime = SystemClock.elapsedRealtime();
                long duration = endTime - startTime;
                Log.d("VideoRecording", "Video Duration: " + duration + " milliseconds");
                // Redirect to MainActivity after stopping the recording
                redirectToActivity();
            } catch (Exception e) {
                // Handle any exceptions that may occur during recording stop or release
                // Display an error message to the user or take appropriate action
                Toast.makeText(context, "Failed to stop video recording!", Toast.LENGTH_SHORT).show();
                Log.e("VideoRecording", "Failed to stop video recording: " + e.getMessage());
            }
        }
    }



    public void onBackPressed() {

        cancelShowingMessages();
        stopSpeaking();
    }




    public void startVideoRecordingManually() {
        startVideoRecording();
    }

    // Fügen Sie diese Methode hinzu, um die manuelle Videoaufnahme zu stoppen
    public void stopVideoRecordingManually() {
        stopVideoRecording();
    }


    private void redirectToActivity() {
        // Check if the context is an instance of Activity
        if (context instanceof Activity) {
            // Create an Intent to navigate to MainActivity
            Intent intent = new Intent(context, NotesActivity.class);
            context.startActivity(intent);

            // Finish the current activity (FaceDetection) to prevent returning to it when pressing the back button from MainActivity
            ((Activity) context).finish();
        }
    }



    // Method to get the file path for saving the recorded video
    private String getVideoFilePath() {
        // Get the external storage directory
        File mediaStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        if (mediaStorageDir != null) {
            // Create a subdirectory for your app if needed
            File appDir = new File(mediaStorageDir, "ParkinsonAssistant");
            if (!appDir.exists()) {
                if (!appDir.mkdirs()) {
                    return null; // Failed to create the directory
                }
            }
            // Create a media file name with timestamp
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String videoFileName = "VID_" + timeStamp + ".mp4";
            // Return the full file path
            return new File(appDir, videoFileName).getAbsolutePath();
        } else {
            return null; // External storage not available
        }
    }

    private boolean checkStoragePermission() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // External storage is available and writable, so the app has permission to write to it.
            Log.d("StoragePermission", "External storage is available and writable.");
            return true;
        } else {
            // External storage is either not available or read-only, so the app does not have permission to write to it.
            Log.d("StoragePermission", "External storage is not available or read-only.");
            return false;
        }
    }

    private boolean isActivityRunning() {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                return true;
            }
        }
        return false;
    }

    private void updateTimer() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - greenOvalStartTime;
        long remainingTime = GREEN_OVAL_DURATION - elapsedTime;

        // Ändere die Timer-Logik, um von 3 bis 1 zu zählen
        if (remainingTime > 0) {
            long seconds = (remainingTime + 999) / 1000; // Erhöhe die verbleibende Zeit um 1 Sekunde
            if (seconds > 0) {
                seconds--; // Reduziere die verbleibende Zeit um 1 Sekunde, um den Timer von 3 bis 1 laufen zu lassen
            }

            String timerText = "Aufnahme startet in: " + seconds + " Sek.";
            timerTextView.setText(timerText);

            // Führe die Methode nach 1 Sekunde erneut aus, um den Timer zu aktualisieren
            recordingHandler.postDelayed(this::updateTimer, 1000);
        } else {
            // Timer ist abgelaufen, verstecke die TextView
            timerTextView.setVisibility(View.GONE);
        }
    }






    private void stopTimer() {
        recordingHandler.removeCallbacksAndMessages(null); // Stoppt die Timer-Aktualisierungen
        timerTextView.setVisibility(View.GONE); // Blendet die Timer-TextView aus
    }


    @SuppressLint("UnsafeExperimentalUsageError")
    @Override
    public void analyze(@NonNull ImageProxy image) {


        Image mediaImage = image.getImage();
        if (mediaImage == null) {
            return;
        }

        int rotation = mapRotation(image);
        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromMediaImage(mediaImage, rotation);

        FirebaseVisionFaceDetectorOptions options = new FirebaseVisionFaceDetectorOptions.Builder()
                .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .setMinFaceSize(0.2f)
                .enableTracking()
                .build();

        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance().getVisionFaceDetector(options);

        detector.detectInImage(firebaseVisionImage)
                .addOnSuccessListener(firebaseVisionFaces -> {
                    boolean isFaceDetected = !firebaseVisionFaces.isEmpty();
                    if (isFaceDetected) {



                        textView.setText("Gesicht erkannt");


                        if (!isOvalPositioned) {
                            // Positionieren Sie das Oval nur einmal, wenn es noch nicht positioniert wurde
                            int parentWidth = rootview.getWidth();
                            int parentHeight = rootview.getHeight();

                            // Calculate the oval position (top center of the parent RelativeLayout)
                            ovalCenterX = parentWidth / 2;
                            ovalCenterY = parentHeight / 5; // Set the oval Y-coordinate to the top quarter of the parent RelativeLayout

                            // Set the oval position within its parent RelativeLayout
                            permanentOvalView.setOvalPosition(ovalCenterX, ovalCenterY);
                            float width = 100.0f;
                            float height = 150.0f;
                            permanentOvalView.setOvalRadius(width, height);

                            isOvalPositioned = true; // Setzen Sie den Status auf true, um das erneute Positionieren zu verhindern
                        }

                        FirebaseVisionFace firebaseVisionFace = firebaseVisionFaces.get(0);
                        // Get the bounding box of the face
                        Rect boundingBox = firebaseVisionFace.getBoundingBox();

                        // Calculate the face center
                        float faceCenterX = boundingBox.centerX();
                        float faceCenterY = boundingBox.centerY();

                        // Calculate the distance between the face center and the oval center
                        double distanceToOvalCenter = Math.sqrt(Math.pow(faceCenterX - ovalCenterX, 2) + Math.pow(faceCenterY - ovalCenterY, 2));

                        // Get the oval radius from the PermanentOvalView
                        float ovalRadius = permanentOvalView.getOvalRadius();

                        // Calculate the size difference (the difference between the maximum oval distance and the actual distance to oval center)
                        double sizeDifference = ovalRadius - distanceToOvalCenter;

                        if (sizeDifference >= 0) {
                            // Check if the face bounding box is smaller than the oval
                            boolean isFaceSmallerThanOval = boundingBox.width() <= permanentOvalView.getWidth() && boundingBox.height() <= permanentOvalView.getHeight();

                            if (isFaceSmallerThanOval) {
                                // Face is inside the oval
                                permanentOvalView.setFaceInside(true);
                                textViewStatus.setText("Sie stehen richtig");

                                // Start video recording after 3 seconds if not already recording
                                if (!isRecording && !isInGreenOval && !isWelcomeMessageShown) {
                                    greenOvalStartTime = System.currentTimeMillis(); // Record the time when the face enters the green oval
                                    isInGreenOval = true;
                                    // Post a delayed runnable to start recording after GREEN_OVAL_DURATION
                                    recordingHandler.postDelayed(startRecordingRunnable, GREEN_OVAL_DURATION);
                                    timerTextView.setVisibility(View.VISIBLE);
                                    updateTimer();
                                }

                                textView.setText("Sie stehen richtig");

                                // Check if the face state has changed from larger to smaller than the oval
                                if (!previousFaceState && isFaceSmallerThanOval && !isWelcomeMessageShown && !isRecording) {
                                    textToSpeech.speak("Sie stehen richtig", TextToSpeech.QUEUE_FLUSH, null, null);
                                    isWelcomeMessageShown = true; // Set a flag to remember that the welcome message has been shown

                                    // Post a delayed runnable to reset the welcome message flag after 5 seconds
                                    textToSpeechHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            isWelcomeMessageShown = false;
                                        }
                                    }, 1000); // 1000 milliseconds = 1 second
                                }

                                // Update the previousFaceState
                                previousFaceState = true;
                            } else {
                                // Face is larger than the oval
                                permanentOvalView.setFaceInside(false);
                                Log.d("Face Detection", "Face is larger than the oval.");
                                textViewStatus.setText("Nehmen Sie Abstand!");

                                // Check if the face state has changed from smaller to larger than the oval
                                if (previousFaceState && !isFaceSmallerThanOval && !isWelcomeMessageShown && !isRecording) {
                                    textToSpeech.speak("Nehmen Sie Abstand", TextToSpeech.QUEUE_FLUSH, null, null);
                                    isWelcomeMessageShown = true; // Set a flag to remember that the warning message has been shown

                                    // Post a delayed runnable to reset the welcome message flag after 5 seconds
                                    textToSpeechHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            isWelcomeMessageShown = false;
                                        }
                                    }, 1000); // 1000 milliseconds = 1 second
                                }

                                // Update the previousFaceState
                                previousFaceState = false;

                                // Set the oval to the default red color when the face is larger than the oval
                                permanentOvalView.setBackgroundResource(R.drawable.face_shape);
                                isInGreenOval = false;
                                recordingHandler.removeCallbacks(startRecordingRunnable); // Cancel video recording if the face leaves the oval
                                stopTimer(); // Ruft die Methode auf, um den Timer zu stoppen und die Timer-TextView auszublenden
                            }
                        } else {
                            // Face is outside the oval
                            permanentOvalView.setFaceInside(false);
                            Log.d("Face Detection", "Face is outside the oval.");

                            // Set the oval to the default red color when the face is outside
                            permanentOvalView.setBackgroundResource(R.drawable.face_shape);
                            isInGreenOval = false;
                            recordingHandler.removeCallbacks(startRecordingRunnable); // Cancel video recording if the face leaves the oval
                            updateFaceStatus(false);
                            stopTimer();
                        }



                        //FirebaseVisionFace firebaseVisionFace = firebaseVisionFaces.get(0);
                        if (firebaseVisionFace.getHeadEulerAngleY() < -20) {
                            textView.setText("Drehen Sie Ihren Kopf nach links in die Mitte");
                            if (right) { // Neue Überprüfung, um nur einmal zu sprechen, bis der Kopf zurückgedreht wird

                                if (!isRecording && !isWelcomeMessageShown) {
                                    textToSpeech.speak("Drehen Sie Ihren Kopf nach links in die Mitte", TextToSpeech.QUEUE_FLUSH, null, null);

                                }
                                right = false; // Setze die Variable auf false, um mehrfaches Sprechen zu verhindern
                                down = true; // Setze andere Variablen zurück
                                left = true;
                                smile = true;

                            }
                        } else if (firebaseVisionFace.getHeadEulerAngleY() > 20) {
                            textView.setText("Drehen Sie Ihren Kopf nach rechts in die Mitte");
                            if (left) { // Neue Überprüfung, um nur einmal zu sprechen, bis der Kopf zurückgedreht wird

                                if (!isRecording && !isWelcomeMessageShown) {
                                    textToSpeech.speak("Drehen Sie Ihren Kopf nach rechts in die Mitte", TextToSpeech.QUEUE_FLUSH, null, null);
                                }
                                left = false; // Setze die Variable auf false, um mehrfaches Sprechen zu verhindern
                                down = true; // Setze andere Variablen zurück
                                right = true;
                                smile = true;
                            }

                        } else if (firebaseVisionFace.getHeadEulerAngleZ() > 10) {
                            textView.setText("Drehen Sie Ihren Kopf nach unten in die Mitte");
                            if (up) { // Neue Überprüfung, um nur einmal zu sprechen, bis der Kopf zurückgedreht wird

                                if (!isRecording && !isWelcomeMessageShown) {
                                    textToSpeech.speak("Drehen Sie Ihren Kopf nach unten", TextToSpeech.QUEUE_FLUSH, null, null);
                                }
                                up = false; // Setze die Variable auf false, um mehrfaches Sprechen zu verhindern
                                down = true; // Setze andere Variablen zurück
                                right = true;
                                left = true;
                                smile = true;
                            }

                        } else if (firebaseVisionFace.getHeadEulerAngleZ()  < -10) {
                            textView.setText("Drehen Sie Ihren Kopf nach oben in die Mitte");
                            if (up) { // Neue Überprüfung, um nur einmal zu sprechen, bis der Kopf zurückgedreht wird

                                if (!isRecording && !isWelcomeMessageShown) {
                                    textToSpeech.speak("Drehen Sie Ihren Kopf nach unten", TextToSpeech.QUEUE_FLUSH, null, null);
                                }
                                up = true; // Setze die Variable auf false, um mehrfaches Sprechen zu verhindern
                                down = false; // Setze andere Variablen zurück
                                right = true;
                                left = true;
                                smile = true;
                            }


                        } else {
                            textView.setText("Gesicht erkannt"); // Reset text if no specific condition is met
                            // Setze alle Variablen zurück, wenn das Gesicht keine der Bedingungen erfüllt
                            down = true;
                            right = true;
                            left = true;
                            smile = true;

                        }

                        // Check if the frame already exists
                        if (drawLayerAroundFace == null) {
                            drawLayerAroundFace = new DrawLayerAroundFace(context);
                            rootview.addView(drawLayerAroundFace);
                        }

// Check if the frame already exists
                        if (drawLayerAroundFace == null) {
                            drawLayerAroundFace = new DrawLayerAroundFace(context);
                            rootview.addView(drawLayerAroundFace);
                        }

// Update the frame position
                        if (drawLayerAroundFace == null) {
                            drawLayerAroundFace = new DrawLayerAroundFace(context);
                            rootview.addView(drawLayerAroundFace);
                        }

                        int displayWidth = rootview.getWidth(); // Hier wird die Breite des rootview-Containers verwendet. Du kannst stattdessen die Breite des Kamerabildschirms oder eines anderen Anzeigebereichs verwenden, wenn es sich um eine andere View handelt.

// Invert the X-coordinate of the bounding box to adjust the frame tracking
                        //Rect boundingBox = firebaseVisionFace.getBoundingBox();
                        int invertedLeft = displayWidth - boundingBox.right; // <-- Change 'boundingBox' to 'faceBoundingBox'
                        int invertedRight = displayWidth - boundingBox.left;
                        boundingBox.left = invertedLeft;
                        boundingBox.right = invertedRight;

                        drawLayerAroundFace.setBoundingBox(boundingBox);
                        drawLayerAroundFace.setVisibility(View.VISIBLE);




                    } else {

                        textView.setText("Gesicht nicht erkannt");
                        //updateOvalVisibility(false);
                        permanentOvalView.setFaceInside(false); // Setzen Sie die Farbe des Ovals zurück, da kein Gesicht erkannt wurde
                        permanentOvalView.setBackgroundResource(R.drawable.face_shape); // Setze den roten Hintergrund zurück
                        stopTimer();
                        // Hide the frame if it exists
                        if (drawLayerAroundFace != null) {
                            drawLayerAroundFace.setVisibility(View.GONE);
                        }

                        // Reset all variables when face is not detected
                        down = true;
                        right = true;
                        left = true;
                        smile = true;


                    }

                }).addOnFailureListener(e -> {
                    Toast.makeText(context, "Nicht erkannt", Toast.LENGTH_LONG).show();

                    updateFaceStatus(false); // Update status to indicate that the face is outside the oval
                    textViewStatus.setText("Nehmen Sie Abstand!");
                }).addOnCompleteListener(task -> image.close());



    }

    private void updateTextView(String text) {
        textView.setText(text);
    }
}