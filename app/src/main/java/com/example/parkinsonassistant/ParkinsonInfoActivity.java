package com.example.parkinsonassistant;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ParkinsonInfoActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private TextView textView;
    private float previousX;
    private float previousY;
    private float previousZ;
    private static final float THRESHOLD = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parkinson_info);

        textView = findViewById(R.id.textView);
        textView.setText("Parkinson lorem ipsum");

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Überprüfe die Veränderung der Sensorwerte im Vergleich zu den vorherigen Werten
            float deltaX = Math.abs(previousX - x);
            float deltaY = Math.abs(previousY - y);
            float deltaZ = Math.abs(previousZ - z);

            // Wenn die Veränderung der Sensorwerte den Schwellenwert überschreitet, aktualisiere die Position des Textbereichs
            if (deltaX > THRESHOLD || deltaY > THRESHOLD || deltaZ > THRESHOLD) {
                // Hier kannst du die Logik implementieren, um den Textbereich zu stabilisieren
                // Beispielsweise kannst du die Position des TextViews in der Activity anpassen
                // oder den Bildschirmbereich, der angezeigt wird, zuschneiden
            }

            // Aktualisiere die vorherigen Sensorwerte
            previousX = x;
            previousY = y;
            previousZ = z;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Hier kannst du auf Änderungen der Sensor-Genauigkeit reagieren
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}
   