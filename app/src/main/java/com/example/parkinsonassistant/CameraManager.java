package com.example.parkinsonassistant;

import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;


public class CameraManager extends AppCompatActivity implements TextToSpeech.OnInitListener{
    ExecutorService service;
    Recording recording = null;
    VideoCapture<Recorder> videoCapture = null;
    ImageButton capture, toggleFlash, flipCamera;
    PreviewView previewView;
    int cameraFacing = CameraSelector.LENS_FACING_BACK;

    private List<String> messages = new ArrayList<>();
    private boolean isFirstMessageShown = false; // Flag zur Überprüfung, ob die Willkommensnachricht angezeigt wurde
    private boolean isShowingMessage = false;
    private AlertDialog alertDialog; // Referenz zum Dialog

    private TextToSpeech textToSpeech;

    private Handler handler;
    private boolean shouldShowSecondMessage = false;
    private boolean shouldShowThirdMessage = false;
    private View greenFrame;

    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
        if (ActivityCompat.checkSelfPermission(CameraManager.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera(cameraFacing);
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // Initialize views
        previewView = findViewById(R.id.viewFinder);
        capture = findViewById(R.id.capture);
        toggleFlash = findViewById(R.id.toggleFlash);
        flipCamera = findViewById(R.id.flipCamera);
       // greenFrame = findViewById(R.id.greenFrame);

        // Set click listener for the capture button
        capture.setOnClickListener(view -> {
            // Check camera permission
            if (ActivityCompat.checkSelfPermission(CameraManager.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                activityResultLauncher.launch(Manifest.permission.CAMERA);
            }
            // Check audio recording permission
            else if (ActivityCompat.checkSelfPermission(CameraManager.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                activityResultLauncher.launch(Manifest.permission.RECORD_AUDIO);
            }
            // Check write storage permission for Android versions <= P
            else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && ActivityCompat.checkSelfPermission(CameraManager.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                activityResultLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            // Start video capture
            else {
                captureVideo();
            }
        });



        // Check camera permission and start the camera
        if (ActivityCompat.checkSelfPermission(CameraManager.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(Manifest.permission.CAMERA);
        } else {
            startCamera(cameraFacing);
        }

        // Set click listener for flip camera button
        flipCamera.setOnClickListener(view -> {
            // Toggle between front and back camera
            if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
                cameraFacing = CameraSelector.LENS_FACING_FRONT;
            } else {
                cameraFacing = CameraSelector.LENS_FACING_BACK;
            }
            startCamera(cameraFacing);
        });

        // Initialize handler for UI updates
        handler = new Handler(Looper.getMainLooper());

        // Show welcome message and add other messages to the list
        showWelcomeMessage();
        messages.add("Kommen Sie bitte etwas näher.");
        messages.add("Nehmen Sie bitte noch etwas Abstand.");
        messages.add("Vielen Dank, jetzt sind Sie gut zu erkennen. Die Aufnahme startet nun automatisch.");
        messages.add("Wir beginnen mit einer einfachen Aufgabe. Bitte gehen Sie 3 Schritte auf das Gerät zu, drehen Sie sich um und gehen Sie wieder zurück.");
        messages.add("Sehr gut, jetzt berühren Sie in rascher Reihenfolge den Daumen mit dem Zeigefinger.");
        messages.add("Vielen Dank, Sie sind fertig mit der Übung. Stoppen Sie nun die Aufnahme.");

        // Initialize TextToSpeech engine
        textToSpeech = new TextToSpeech(this, this);
    }

    // Method to handle video capture
    public void captureVideo() {
        capture.setImageResource(R.drawable.round_stop_circle_24);
        Recording recording1 = recording;
        if (recording1 != null) {
            // Stop ongoing recording
            recording1.stop();
            recording = null;
            return;
        }

        // Generate file name for the captured video
        String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis());

        // Prepare media store output options for video capture
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video");
        MediaStoreOutputOptions options = new MediaStoreOutputOptions.Builder(getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues).build();

        // Check audio recording permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Start video recording
        recording = videoCapture.getOutput().prepareRecording(CameraManager.this, options).withAudioEnabled().start(ContextCompat.getMainExecutor(CameraManager.this), videoRecordEvent -> {
            if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                capture.setEnabled(true);
            } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                if (!((VideoRecordEvent.Finalize) videoRecordEvent).hasError()) {
                    String msg = "Video erfolgreich aufgezeichnet: " + ((VideoRecordEvent.Finalize) videoRecordEvent).getOutputResults().getOutputUri();
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                } else {
                    recording.close();
                    recording = null;
                    String msg = "Error: " + ((VideoRecordEvent.Finalize) videoRecordEvent).getError();
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                }
                capture.setImageResource(R.drawable.round_fiber_manual_record_24);
                // After stopping the recording, start the NotesActivity
                Intent intent = new Intent(CameraManager.this, NotesActivity.class);
                startActivity(intent);
            }
        });
    }

    // Method to start the camera
    public void startCamera(int cameraFacing) {
        ListenableFuture<ProcessCameraProvider> processCameraProvider = ProcessCameraProvider.getInstance(CameraManager.this);


        processCameraProvider.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = processCameraProvider.get();

                // Set up preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Set up video capture
                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                cameraProvider.unbindAll();

                // Select camera and bind it to lifecycle
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraFacing).build();
                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture);

                // Set click listener for toggle flash button
                toggleFlash.setOnClickListener(view -> toggleFlash(camera));
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(CameraManager.this));
    }

    // Method to toggle flash
    private void toggleFlash(Camera camera) {
        if (camera.getCameraInfo().hasFlashUnit()) {
            // Check current torch state and enable/disable the flash
            if (camera.getCameraInfo().getTorchState().getValue() == 0) {
                camera.getCameraControl().enableTorch(true);
                toggleFlash.setImageResource(R.drawable.round_flash_off_24);
            } else {
                camera.getCameraControl().enableTorch(false);
                toggleFlash.setImageResource(R.drawable.round_flash_on_24);
            }
        } else {
            runOnUiThread(() -> Toast.makeText(CameraManager.this, "Blitzlicht ist gerade nicht verfügbar", Toast.LENGTH_SHORT).show());
        }
    }

    // TextToSpeech initialization callback
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // TextToSpeech initialization successful
        } else {
            // TextToSpeech initialization failed
            Toast.makeText(this, "Fehler bei der Initialisierung der Text-to-Speech-Funktionalität", Toast.LENGTH_SHORT).show();
        }
    }

    // Show welcome message
    private void showWelcomeMessage() {
        showAlert("Willkommen!", "Willkommen! Bitte stellen Sie sich in den rot umrandeten Bereich. Wenn er grün wird, stehen Sie richtig. Bitte drücken Sie auf 'OK', um fortzufahren.");
        speakMessage("Willkommen! Bitte stellen Sie sich in den rot umrandeten Bereich. Wenn er grün wird, stehen Sie richtig. Drücken Sie auf OK, um fortzufahren.");
    }

    // Show next message
    // Show next message
    private void showNextMessage() {
        if (!messages.isEmpty() && !isShowingMessage) {
            String message = messages.remove(0);
            isShowingMessage = true;
            showAlert("Nachricht", message);
            speakMessage(message);

            if (messages.isEmpty()) {
                greenFrame.setVisibility(View.VISIBLE);
                capture.setEnabled(true);

            } else if (messages.size() == 3) {
                greenFrame.setVisibility(View.VISIBLE);
                captureVideo(); //start recording automatically
                capture.setEnabled(false);
            }
        }
    }





    // Show alert dialog with message
    private void showAlert(String title, String message) {
        if (!isFinishing()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", (dialog, id) -> {
                        if (!isFirstMessageShown) {
                            isFirstMessageShown = true;
                        }
                        isShowingMessage = false;
                        textToSpeech.stop();
                        handler.postDelayed(() -> showNextMessage(), 5000);
                    })
                    .setOnDismissListener(dialog -> {
                        if (isFirstMessageShown) {
                            isShowingMessage = false;
                            textToSpeech.stop();
                            handler.postDelayed(() -> showNextMessage(), 5000);
                        }
                    });
            alertDialog = builder.create();
            alertDialog.show();
        }
    }

    // Speak the given message using TextToSpeech
    private void speakMessage(String message) {
        if (textToSpeech != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop and shutdown TextToSpeech
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        // Dismiss alert dialog if it's showing
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reinitialize TextToSpeech and show welcome message on resume
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // TextToSpeech initialization successful
                showWelcomeMessage();
            } else {
                // TextToSpeech initialization failed
                Toast.makeText(this, "Fehler bei der Initialisierung der Text-to-Speech-Funktionalität", Toast.LENGTH_SHORT).show();
            }
        });
    }


}
