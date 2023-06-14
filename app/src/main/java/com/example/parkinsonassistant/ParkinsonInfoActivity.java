package com.example.parkinsonassistant;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ParkinsonInfoActivity extends AppCompatActivity {


    private TextView textView; // TextView for displaying text
    private static final float THRESHOLD = 1.0f; // Sensitivity threshold for motion changes

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parkinson_info);

        textView = findViewById(R.id.textView);
        textView.setText(Html.fromHtml("Für zuverlässige Informationen schauen Sie hier vorbei:\n\n " +
                "<a href=\"https://www.parkinson-vereinigung.de\">Deutsche Parkinson Vereinigung (DPV)</a>, \n\n" +
                "<a href=\"https://parkinson-gesellschaft.de/fuer-betroffene/die-parkinson-krankheit?dpg/spende\">Deutsche Gesellschaft für Parkinson </a>,\n\n " +
                "<a href=\"https://www.apotheken-umschau.de/krankheiten-symptome/neurologische-erkrankungen/parkinson-krankheit-symptome-ursachen-therapie-733737.html\">Apotheken-Umschau</a>"));
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        // Increase font size
        float textSizeInSp = 30; // font size in SP
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeInSp);
    }



}
   