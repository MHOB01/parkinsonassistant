package com.example.parkinsonassistant;

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class TimelineActivity extends AppCompatActivity {


    private Map<String, StringBuilder> notesByDay;
    private LineChart lineChart;
    private TextView txtDate;
    private int selectedSmiley = -1;

    private int axisColor;
    private int graphColor;
    private StringBuilder notes;
    private DatabaseHelper databaseHelper;
    private List<String> notesList;
    private RecyclerView recyclerViewNotes;
    private NotesAdapter notesAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        databaseHelper = new DatabaseHelper(this);

        recyclerViewNotes.setLayoutManager(new LinearLayoutManager(this));
        int currentMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        notesList = new ArrayList<>();
        notesAdapter = new NotesAdapter(notesList);
        recyclerViewNotes.setAdapter(notesAdapter);

        Button buttonSaveNotes = findViewById(R.id.btn_back);
        buttonSaveNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNotes();
                goToMainActivity();
            }
        });

        // Setzen Sie die Farben basierend auf dem Gerätemodus

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

            // Festlegen der festen Werte für die Tage vor dem heutigen Datum
            int[] fixedValues = {4, 3, 3, 1, 2, 5};

            List<Entry> entries = new ArrayList<>();

            // Datenpunkte für die letzten 7 Tage hinzufügen
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
            lineDataSet.setDrawValues(false); // Keine Werte anzeigen

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
            yAxis.setInverted(true);  // Y-Achse drehen
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

            TextView textViewNotes = findViewById(R.id.textViewNotes);
            textViewNotes.setText(notes.toString());
        } else {
            Toast.makeText(this, "Kein Smiley ausgewählt", Toast.LENGTH_SHORT).show();
        }

        LinearLayout legendLayout = findViewById(R.id.legend_layout);

        // Erstelle für jeden Smiley eine TextView in LinearLayout
        for (int i = 1; i <= 5; i++) {
            TextView textView = new TextView(this);
            textView.setText(getSmileyText(i));
            textView.setTextSize(20);
            textView.setPadding(10, 0, 10, 0);
            legendLayout.addView(textView);
        }

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setTextSize(14f); // Schriftgröße anpassen

        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setTextSize(20f); // Schriftgröße anpassen

        loadNotesFromDatabase();
    }




    private String getDayOfWeek(int dayOfWeek) {
        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.DAY_OF_WEEK);
        int offset = dayOfWeek;
        //+ 2 - today;
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

    private void loadNotesFromDatabase() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.query("notes", null, null, null, null, null, null);

        notesList.clear();
        while (cursor.moveToNext()) {
            String note = cursor.getString(cursor.getColumnIndex("content"));
            notesList.add(note);
        }

        cursor.close();
        db.close();

        notesAdapter.notifyDataSetChanged();
    }

    private void saveNotes() {
        // Öffnen der Datenbankverbindung
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        // Schleife über die Notizenliste
        for (String note : notesList) {
            // Erstellen Sie ein neues ContentValues-Objekt und fügen Sie den Notizinhalt hinzu
            ContentValues values = new ContentValues();
            values.put("content", note);

            // Fügen Sie die Werte zur Datenbanktabelle hinzu
            db.insert("notes", null, values);
        }

        // Schließen der Datenbankverbindung
        db.close();

        Toast.makeText(TimelineActivity.this, "Notizen gespeichert", Toast.LENGTH_SHORT).show();
    }

    private void goToMainActivity() {
        Intent intent = new Intent(TimelineActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

}