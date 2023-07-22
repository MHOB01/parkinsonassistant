package com.example.parkinsonassistant;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.Image;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;

import java.util.List;


public class BaseImage implements ImageAnalysis.Analyzer {
    FirebaseVisionFaceDetector firebaseVisionFaceDetector;
    private DrawLayerAroundFace drawLayerAroundFace;
    private int ovalCenterX;
    private int ovalCenterY;
    Context context;
    TextToSpeech textToSpeech;
    boolean right = true;
    boolean down = true;
    boolean left = true;
    boolean smile = true;
    private float ovalRadius;
    boolean up = true;
    RelativeLayout rootview;
    TextView textView;
    Activity activity;
    boolean isOvalPositioned = false; // Variable, um den Status des Ovals zu verfolgen
    private PermanentOvalView permanentOvalView;
    public BaseImage(Context context, TextToSpeech textToSpeech, RelativeLayout view, TextView textView) {
        this.context = context;
        this.textToSpeech = textToSpeech;
        this.rootview = view;
        this.textView = textView;

        permanentOvalView = rootview.findViewById(R.id.permanentOvalView);

        // Set the oval radius (adjust the value as needed)
        //ovalRadius = permanentOvalView.getOvalRadius();
    }

    private void updateFaceStatus(boolean isInside) {
        textView.setText(isInside ? "Gesicht erkannt" : "Gesicht nicht erkannt");
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

    private void updateOvalVisibility(boolean isVisible) {
        if (isVisible) {
            permanentOvalView.setVisibility(View.VISIBLE);
        } else {
            permanentOvalView.setVisibility(View.INVISIBLE);
        }
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

        Log.d("Ran analyzer — — — — — — — — — ->>>>>>>>>>", "Ran");

        FirebaseVisionFaceDetectorOptions a = new FirebaseVisionFaceDetectorOptions.Builder()
                .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .setMinFaceSize(0.2f)
                .enableTracking()
                .build();

        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance().getVisionFaceDetector(a);

        detector.detectInImage(firebaseVisionImage).addOnSuccessListener(firebaseVisionFaces -> {
            Log.d("Got values — — — — — — — — — ->>>>>>>>>>", firebaseVisionFaces.toString());

            boolean isFaceDetected = !firebaseVisionFaces.isEmpty();

            if (isFaceDetected) {
                textView.setText("Gesicht erkannt");
                updateOvalVisibility(true);

                if (!isOvalPositioned) {
                    // Positionieren Sie das Oval nur einmal, wenn es noch nicht positioniert wurde
                    int parentWidth = rootview.getWidth();
                    int parentHeight = rootview.getHeight();

                    // Calculate the oval position (top center of the parent RelativeLayout)
                    ovalCenterX = parentWidth / 2;
                    ovalCenterY = parentHeight / 6; // Set the oval Y-coordinate to the top quarter of the parent RelativeLayout

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

                Log.d("Face Detection", "Oval Radius: " + ovalRadius);
                Log.d("Face Detection", "Distance to Oval Center: " + distanceToOvalCenter);
                Log.d("Face Detection", "Size Difference: " + sizeDifference);

                if (sizeDifference >= 0) {
                    // Check if the face bounding box is smaller than the oval
                    boolean isFaceSmallerThanOval = boundingBox.width() <= permanentOvalView.getWidth() && boundingBox.height() <= permanentOvalView.getHeight();

                    if (isFaceSmallerThanOval) {
                        permanentOvalView.setFaceInside(true);
                        Log.d("Face Detection", "Face is inside the oval.");

                        // Set the oval to green when the face is inside
                        permanentOvalView.setBackgroundResource(R.drawable.face_shape_green);
                    } else {
                        permanentOvalView.setFaceInside(false);
                        Log.d("Face Detection", "Face is larger than the oval.");

                        // Set the oval to the default red color when the face is larger than the oval
                        permanentOvalView.setBackgroundResource(R.drawable.face_shape);
                    }
                } else {
                    permanentOvalView.setFaceInside(false);
                    Log.d("Face Detection", "Face is outside the oval.");

                    // Set the oval to the default red color when the face is outside
                    permanentOvalView.setBackgroundResource(R.drawable.face_shape);
                }





                //FirebaseVisionFace firebaseVisionFace = firebaseVisionFaces.get(0);
                if (firebaseVisionFace.getHeadEulerAngleY() < -20) {
                    textView.setText("Drehen Sie Ihren Kopf nach links");
                    if (right) { // Neue Überprüfung, um nur einmal zu sprechen, bis der Kopf zurückgedreht wird
                        textToSpeech.speak("Drehen Sie Ihren Kopf nach links", TextToSpeech.QUEUE_FLUSH, null, null);
                        right = false; // Setze die Variable auf false, um mehrfaches Sprechen zu verhindern
                        down = true; // Setze andere Variablen zurück
                        left = true;
                        smile = true;
                    }
                } else if (firebaseVisionFace.getHeadEulerAngleY() > 20) {
                    textView.setText("Drehen Sie Ihren Kopf nach rechts");
                    if (left) { // Neue Überprüfung, um nur einmal zu sprechen, bis der Kopf zurückgedreht wird
                        textToSpeech.speak("Drehen Sie Ihren Kopf nach rechts", TextToSpeech.QUEUE_FLUSH, null, null);
                        left = false; // Setze die Variable auf false, um mehrfaches Sprechen zu verhindern
                        down = true; // Setze andere Variablen zurück
                        right = true;
                        smile = true;
                    }
                } else if (firebaseVisionFace.getSmilingProbability() > 0.8) {
                    textView.setText("Nicht lächeln");
                    if (smile) { // Neue Überprüfung, um nur einmal zu sprechen, bis das Lächeln beendet ist
                        textToSpeech.speak("Nicht lächeln", TextToSpeech.QUEUE_FLUSH, null, null);
                        smile = false; // Setze die Variable auf false, um mehrfaches Sprechen zu verhindern
                        down = true; // Setze andere Variablen zurück
                        right = true;
                        left = true;
                    }

                } else if (firebaseVisionFace.getHeadEulerAngleZ() > 10) {
                    textView.setText("Drehen Sie Ihren Kopf nach unten");
                    if (up) { // Neue Überprüfung, um nur einmal zu sprechen, bis der Kopf zurückgedreht wird
                        textToSpeech.speak("Drehen Sie Ihren Kopf nach unten", TextToSpeech.QUEUE_FLUSH, null, null);
                        up = false; // Setze die Variable auf false, um mehrfaches Sprechen zu verhindern
                        down = true; // Setze andere Variablen zurück
                        right = true;
                        left = true;
                        smile = true;
                    }

                } else if (firebaseVisionFace.getHeadEulerAngleZ()  < -10) {
                    textView.setText("Drehen Sie Ihren Kopf nach unten");
                    if (up) { // Neue Überprüfung, um nur einmal zu sprechen, bis der Kopf zurückgedreht wird
                        textToSpeech.speak("Drehen Sie Ihren Kopf nach unten", TextToSpeech.QUEUE_FLUSH, null, null);
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
                permanentOvalView.setFaceInside(false); // Setzen Sie die Farbe des Ovals zurück, da kein Gesicht erkannt wurde
                permanentOvalView.setBackgroundResource(R.drawable.face_shape); // Setze den roten Hintergrund zurück
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
            updateOvalVisibility(false);
        }).addOnCompleteListener(task -> image.close());
    }

    private void updateTextView(String text) {
        textView.setText(text);
    }
}
