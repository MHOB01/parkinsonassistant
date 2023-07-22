package com.example.parkinsonassistant;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.SparseArray;
import android.view.SurfaceView;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.util.List;

public class FaceDetectorProcessor implements FaceDetector.Processor {

    private SurfaceView surfaceView;

    public FaceDetectorProcessor(SurfaceView surfaceView) {
        this.surfaceView = surfaceView;
    }

    @Override
    public void receiveDetections(FaceDetector.Detections detections) {
        // Get the sparse array of detected faces.
        SparseArray<Face> faces = detections.getDetectedItems();

        // Create a paint object for drawing rectangles
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);

        // Do something with the detected faces.
        for (int i = 0; i < faces.size(); i++) {
            Face face = faces.valueAt(i);

            // Get the position of the face.
            PointF position = face.getPosition();

            // Calculate the bounding box of the face.
            float width = face.getWidth();
            float height = face.getHeight();
            float left = position.x - width / 2f;
            float top = position.y - height / 2f;
            float right = position.x + width / 2f;
            float bottom = position.y + height / 2f;

            Canvas canvas = surfaceView.getHolder().lockCanvas();
            canvas.drawRect(left, top, right, bottom, paint);
            surfaceView.getHolder().unlockCanvasAndPost(canvas);
        }
    }





    @Override
    public void release() {
        // Do nothing.
    }

}