package com.example.parkinsonassistant;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;


public class PermanentOvalView extends View {
    private boolean faceInside;
    private float ovalRadius;

    private int ovalCenterX;
    private int ovalCenterY;

    public PermanentOvalView(Context context) {
        super(context);
        setBackgroundResource(R.drawable.face_shape);

        // Set the oval radius manually here (replace 100.0f and 150.0f with the desired width and height)
        ovalRadius = 100.0f; // Width
    }

    public void setOvalRadius(float width, float height) {
        // Set the oval radius based on the provided width and height
        ovalRadius = Math.min(width, height) / 2.0f;
    }

    public float getOvalRadius() {
        return ovalRadius;
    }

    public void setOvalPosition(int centerX, int centerY) {
        ovalCenterX = centerX;
        ovalCenterY = centerY;
        invalidate(); // Dies bewirkt, dass die View neu gezeichnet wird, um die Änderungen anzuzeigen.
    }

    public PermanentOvalView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundResource(R.drawable.face_shape);
    }

    public PermanentOvalView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setBackgroundResource(R.drawable.face_shape);
    }

    // Set whether the face is inside the oval
    public void setFaceInside(boolean isInside) {
        this.faceInside = isInside;
        updateOvalDrawable(); // Update the oval drawable based on the faceInside state
    }

    // Update the oval drawable based on the faceInside state
    private void updateOvalDrawable() {
        if (faceInside) {
            setBackgroundResource(R.drawable.face_shape_green);
        } else {
            setBackgroundResource(R.drawable.face_shape);
        }
    }
}



