package com.example.parkinsonassistant;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TimelineActivity extends AppCompatActivity implements TextToSpeech.OnInitListener{
    private Map<String, StringBuilder> notesByDay;
    private LineChart lineChart;
    private TextView txtDate;
    private int selectedSmiley = -1;

    private int axisColor;
    private int graphColor;
    private StringBuilder notes;

    private AlertDialog alertDialog;
    private TextToSpeech textToSpeech;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);
        notesByDay = new HashMap<>();

        // Initialize the TextToSpeech object if it's null
        if (textToSpeech == null) {
            textToSpeech = new TextToSpeech(this, this); // "this" refers to the current activity implementing OnInitListener
        }

        // Initialize the TextToSpeech object
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // Set the language for text-to-speech
                int result = textToSpeech.setLanguage(Locale.getDefault());

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TextToSpeech", "Language not supported");
                } else {
                    // Start reading the message after initialization
                    speakMessage("Hier sehen Sie Ihr Befinden über 7 Tage abgetragen und können es bei Bedarf dem Arzt vorzeigen!");
                }
            } else {
                Log.e("TextToSpeech", "Initialization failed");
            }
        });


        // Wiederherstellen des ausgewählten Smileys aus den SharedPreferences
        SharedPreferences preferences = getSharedPreferences("TimelinePrefs", MODE_PRIVATE);
        selectedSmiley = preferences.getInt("selectedSmiley", -1);



        // Get the current device mode (light or dark)
        int currentMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        Button backButton = findViewById(R.id.btn_back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Go back to MainActivity when the back button is clicked
                Intent intent = new Intent(TimelineActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Optional
            }
        });

        // Show AlertDialog
        AlertDialog.Builder welcomeBuilder = new AlertDialog.Builder(TimelineActivity.this);
        welcomeBuilder.setTitle("Zeitverlauf");
        welcomeBuilder.setMessage("Hier sehen Sie Ihr Befinden über 7 Tage abgetragen und können es bei Bedarf dem Arzt vorzeigen!");
        welcomeBuilder.setPositiveButton("OK", (dialog, which) -> {
            stopTextToSpeech();
        });
        alertDialog = welcomeBuilder.create();
        alertDialog.show();





        // Set colors based on the device mode (light or dark)
        if (currentMode == Configuration.UI_MODE_NIGHT_YES) {
            // Dark Mode
            axisColor = Color.WHITE;
            graphColor = Color.WHITE;
        } else {
            // Light Mode
            axisColor = Color.BLACK;
            graphColor = Color.BLACK;
        }

        lineChart = findViewById(R.id.line_chart);
        txtDate = findViewById(R.id.txt_date);

        Intent intent = getIntent();
        selectedSmiley = intent.getIntExtra("selectedSmiley", -1);
        notes = (StringBuilder) intent.getSerializableExtra("notes");

        if (selectedSmiley != -1) {
            String currentDate = getIntent().getStringExtra("currentDate");
            txtDate.setText(currentDate);

            lineChart.setTouchEnabled(false);
            lineChart.setDragEnabled(false);
            lineChart.setScaleEnabled(false);
            lineChart.setPinchZoom(false);
            lineChart.setDrawGridBackground(false);
            lineChart.getDescription().setEnabled(false);

            // Fixed values for the days before the current date
            int[] fixedValues = {4, 3, 3, 1, 2, 5};

            List<Entry> entries = new ArrayList<>();

            // Add data points for the last 7 days
            for (int i = -6; i <= 0; i++) {
                if (i == 0) {
                    entries.add(new Entry(i, selectedSmiley));
                } else {
                    int index = Math.abs(i) - 1;
                    int value = fixedValues[index];
                    entries.add(new Entry(i, value));
                }
            }

            LineDataSet lineDataSet = new LineDataSet(entries, "");
            lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
            lineDataSet.setLineWidth(3f);
            lineDataSet.setCircleRadius(5f);
            lineDataSet.setDrawCircles(true);
            lineDataSet.setCircleColor(Color.RED);
            lineDataSet.setDrawValues(false); // Do not display values

            lineDataSet.setColor(graphColor);

            ArrayList<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(lineDataSet);

            LineData data = new LineData(dataSets);

            lineChart.setData(data);
            lineChart.invalidate();

            XAxis xAxis = lineChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setValueFormatter(new IndexAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    int dayOfWeek = (int) value;
                    return getDayOfWeek(dayOfWeek);
                }
            });

            xAxis.setLabelCount(7, true);
            xAxis.setTextColor(axisColor);

            YAxis yAxis = lineChart.getAxisLeft();
            yAxis.setInverted(true);  // Invert the Y-axis
            yAxis.setGranularity(1f);
            yAxis.setDrawGridLines(true);
            yAxis.setAxisMinimum(1f);
            yAxis.setAxisMaximum(5f);
            yAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getAxisLabel(float value, AxisBase axis) {
                    return String.valueOf((int) value);
                }
            });

            yAxis.setTextColor(axisColor);

            lineChart.getAxisRight().setEnabled(false);

            lineChart.setData(data);
            lineChart.invalidate();
        } else {
            // No smiley selected
            Toast.makeText(this, "Kein Smiley ausgewählt", Toast.LENGTH_SHORT).show();

            // Select the last value if available
            int[] fixedValues = {4, 3, 3, 1, 2, 5};
            int lastIndex = fixedValues.length - 1;
            int lastValue = fixedValues[lastIndex];

            List<Entry> entries = new ArrayList<>();

            for (int i = -6; i <= 0; i++) {
                if (i == 0) {
                    entries.add(new Entry(i, lastValue));
                } else {
                    int index = Math.abs(i) - 1;
                    int value = fixedValues[index];
                    entries.add(new Entry(i, value));
                }
            }

            LineDataSet lineDataSet = new LineDataSet(entries, "");
            lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
            lineDataSet.setLineWidth(3f);
            lineDataSet.setCircleRadius(5f);
            lineDataSet.setDrawCircles(true);
            lineDataSet.setCircleColor(Color.RED);
            lineDataSet.setDrawValues(false);

            lineDataSet.setColor(graphColor);

            ArrayList<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(lineDataSet);

            LineData data = new LineData(dataSets);

            lineChart.setData(data);
            lineChart.invalidate();

            XAxis xAxis = lineChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setValueFormatter(new IndexAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    int dayOfWeek = (int) value;
                    return getDayOfWeek(dayOfWeek);
                }
            });

            xAxis.setLabelCount(7, true);
            xAxis.setTextColor(axisColor);

            YAxis yAxis = lineChart.getAxisLeft();
            yAxis.setInverted(true);
            yAxis.setGranularity(1f);
            yAxis.setDrawGridLines(true);
            yAxis.setAxisMinimum(1f);
            yAxis.setAxisMaximum(5f);
            yAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getAxisLabel(float value, AxisBase axis) {
                    return String.valueOf((int) value);
                }
            });

            yAxis.setTextColor(axisColor);

            lineChart.getAxisRight().setEnabled(false);

            lineChart.setData(data);
            lineChart.invalidate();
        }

        LinearLayout legendLayout = findViewById(R.id.legend_layout);

        // Create a TextView in the LinearLayout for each smiley
        for (int i = 1; i <= 5; i++) {
            TextView textView = new TextView(this);
            textView.setText(getSmileyText(i));
            textView.setTextSize(20);
            textView.setPadding(10, 0, 10, 0);
            legendLayout.addView(textView);
        }

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setTextSize(14f);

        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setTextSize(20f);
    }

    private String getDayOfWeek(int dayOfWeek) {
        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.DAY_OF_WEEK);
        int offset = dayOfWeek;
        calendar.add(Calendar.DAY_OF_WEEK, offset);

        if (offset == 0) {
            return "Heute";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.getDefault());
            return sdf.format(calendar.getTime());
        }
    }

    private String getSmileyText(int smileyId) {
        String text = "";
        switch (smileyId) {
            case 1:
                text = "😄 = 1";
                break;
            case 2:
                text = "😊 = 2";
                break;
            case 3:
                text = "😐 = 3";
                break;
            case 4:
                text = "😞 = 4";
                break;
            case 5:
                text = "😢 = 5";
                break;
        }
        return text;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // Sprachsynthese erfolgreich initialisiert
            speakMessage("Hier sehen Sie ihr Befinden über 7 Tage abgetragen und können es bei Bedarf dem Arzt vorzeigen!");
        } else {
            // Fehler bei der Initialisierung der Sprachsynthese
            Toast.makeText(this, "Fehler bei der Initialisierung der Text-to-Speech-Funktionalität", Toast.LENGTH_SHORT).show();
        }
    }

    private void speakMessage(String message) {
        if (textToSpeech != null && !textToSpeech.isSpeaking()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }


    private void stopTextToSpeech() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null; // Setze das TextToSpeech-Objekt auf null
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTextToSpeech();
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
    }




}