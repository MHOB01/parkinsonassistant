package com.example.parkinsonassistant;



import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.Manifest;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.media.MediaRecorder;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.TaskExecutors;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FaceDetection extends AppCompatActivity {
    static ImageButton captureButton;
    static Recording recording = null;
    static VideoCapture<Recorder> videoCapture = null;
    private ImageAnalysis imageAnalysis1;
    private Preview preview;

    private static final int REQUEST_VIDEO_CAPTURE = 1;
    private TextView textViewFaceState;
    private ImageCapture imageCapture;
    //private ImageButton captureButton;

    private ImageAnalysis imageAnalysis;
    private DrawLayerAroundFace drawLayerAroundFace;
    static PreviewView previewView;

    private TextToSpeech textToSpeech;
    private RelativeLayout rootview;
    private TextView textView;

    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;

    private int screenWidth;
    private int screenHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;



        // Initialize the DrawLayerAroundFace with the current activity and an empty bounding box
        drawLayerAroundFace = new DrawLayerAroundFace(this);
        // Initialize the PreviewView using the correct ID
        previewView = findViewById(R.id.pv);

        // Initialize the TextToSpeech instance
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // TextToSpeech initialisierung successful
            } else {
                // TextToSpeech could not be initialized
            }
        });
        textToSpeech.speak("Willkommen! Bitte platzieren Sie ihr Gesicht in dem rot umrandeten Bereich, sobald diese Nachricht verschwunden ist, um die Aufnahme zu starten. Wenn er grün wird, stehen Sie richtig.", TextToSpeech.QUEUE_FLUSH, null, null);

        // Initialize the rootview using the correct ID
        rootview = findViewById(R.id.conlay);

        // Initialize the textView using the correct ID
        textView = findViewById(R.id.textView3);
// Pass the Context reference to the BaseImage constructor
        // Initialize the imageAnalysis1 variable
        imageAnalysis1 = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(new Size(900, 1200))
                .build();
        captureButton = findViewById(R.id.capture);
        //textViewFaceState = findViewById(R.id.textViewFaceState);
        // Pass the Context reference to the BaseImage constructor
        BaseImage baseImage = new BaseImage(this, textToSpeech, rootview, textView, captureButton);

        // Set the BaseImage instance as the analyzer for the ImageAnalysis
        imageAnalysis1.setAnalyzer(ContextCompat.getMainExecutor(this), baseImage);


        renderImage();

        // click listener for the capture button
        ImageButton captureButton = findViewById(R.id.capture);
       captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If the recording is not active, start the video recording
                if (!baseImage.isRecordingActive()) {
                    baseImage.startVideoRecordingManually();
                } else {
                    // if the recording is active, stop the video recording
                    baseImage.stopVideoRecordingManually();
                }
            }
        });


    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_VIDEO_CAPTURE) {
            // check if the permission was granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                BaseImage baseImage = new BaseImage(this, textToSpeech, rootview, textView, captureButton);

                // the permission was granted, start the video recording
                baseImage.startVideoRecording();

            } else {
                // the permission was not granted, show a toast message and close the app
                Toast.makeText(this, "Speicherzugriffsberechtigung verweigert. Die Videoaufnahme ist nicht möglich.", Toast.LENGTH_SHORT).show();
            }
        }
    }




    private void renderImage() {
        ListenableFuture<ProcessCameraProvider> val = ProcessCameraProvider.getInstance(this);

        val.addListener(() -> {
            try {
                ProcessCameraProvider processCameraProvider = val.get();
                preview = new Preview.Builder().setTargetResolution(new Size(900, 1200)).build();

                CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageCapture imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();
                ImageAnalysis imageAnalysis1 = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(new Size(900, 1200)).build();

                // Create an instance of BaseImage to perform face detection

                BaseImage baseImage = new BaseImage(this, textToSpeech, rootview, textView, captureButton);

                // Set the BaseImage instance as the analyzer for the ImageAnalysis
                imageAnalysis1.setAnalyzer(ContextCompat.getMainExecutor(this), baseImage);

                // Bind all use cases to the camera lifecycle
                processCameraProvider.unbindAll();
                Camera camera = processCameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis1);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }




    private void detectFaces(ImageProxy image, FirebaseVisionFaceDetector detector) {

    }

    private Rect convertRectFToRect(RectF rectF) {
        return new Rect(
                Math.round(rectF.left),
                Math.round(rectF.top),
                Math.round(rectF.right),
                Math.round(rectF.bottom)
        );
    }

    private void updateFaceRectangles(List<RectF> faceRectangles) {
        Log.d("FaceDetection", "updateFaceRectangles() called");
        RelativeLayout rootview = findViewById(R.id.conlay);

        // Remove all previously drawn views
        rootview.removeAllViews();

        for (RectF boundingBox : faceRectangles) {
            // Convert RectF to Rect
            Rect boundingRect = convertRectFToRect(boundingBox);

            // Add the updated DrawLayerAroundFace view
            View view1 = new DrawLayerAroundFace(this); // Pass 'this' to use the context from FaceDetection class
            ((DrawLayerAroundFace) view1).setBoundingBox(boundingRect);
            rootview.addView(view1);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // free resources when the app is paused
        releaseCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // free resources when the app is destroyed
        releaseCamera();
    }

    private void releaseCamera() {
        // release imageAnalysis resources
        if (imageAnalysis != null) {
            imageAnalysis.clearAnalyzer();
        }

        // release imageCapture resources
        if (imageCapture != null) {
            imageCapture = null;
        }

        // release preview resources
        if (preview != null) {
            preview = null;
        }
    }




}