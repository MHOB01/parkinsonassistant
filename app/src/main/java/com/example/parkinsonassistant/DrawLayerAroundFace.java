package com.example.parkinsonassistant;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class DrawLayerAroundFace extends View {
    private Rect boundingBox; // Use Rect instead of RectF

    public DrawLayerAroundFace(Context context) {
        super(context);
    }

    public void setBoundingBox(Rect boundingBox) { // Use Rect instead of RectF
        this.boundingBox = boundingBox;
        invalidate(); // Request a redraw of the view
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (boundingBox != null) {
            Paint paint = new Paint();
            paint.setColor(Color.BLUE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(10.0f);

            // Convert Rect to RectF before drawing
            RectF rectF = new RectF(boundingBox);
            canvas.drawRect(rectF, paint);
        }
    }
}








