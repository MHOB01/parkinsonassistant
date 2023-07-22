package com.example.parkinsonassistant;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
import androidx.camera.view.PreviewView;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FaceDetection extends AppCompatActivity {

    private Preview preview;
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private DrawLayerAroundFace drawLayerAroundFace;
    private PreviewView previewView;
    private TextToSpeech textToSpeech; // Hier hinzufügen
    private RelativeLayout rootview; // Hier hinzufügen
    private TextView textView; // Hier hinzufügen

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);

        // Initialize the DrawLayerAroundFace with the current activity and an empty bounding box
        drawLayerAroundFace = new DrawLayerAroundFace(this);

        // Initialize the PreviewView using the correct ID
        previewView = findViewById(R.id.pv);

        // Initialize the TextToSpeech instance
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // TextToSpeech initialisierung erfolgreich
            } else {
                // TextToSpeech konnte nicht initialisiert werden
            }
        });

        // Initialize the rootview using the correct ID
        rootview = findViewById(R.id.conlay);

        // Initialize the textView using the correct ID
        textView = findViewById(R.id.textView3);


        renderImage();
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
                BaseImage baseImage = new BaseImage(this, textToSpeech, rootview, textView);

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
        // Ressourcen freigeben, wenn die App pausiert wird
        releaseCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ressourcen freigeben, wenn die App beendet wird
        releaseCamera();
    }

    private void releaseCamera() {
        // Freigabe des ImageAnalysis-Objekts
        if (imageAnalysis != null) {
            imageAnalysis.clearAnalyzer();
        }

        // Freigabe des ImageCapture-Objekts
        if (imageCapture != null) {
            imageCapture = null;
        }

        // Freigabe des Preview-Objekts
        if (preview != null) {
            preview = null;
        }
    }

}