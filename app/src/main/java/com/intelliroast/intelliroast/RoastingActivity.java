package com.intelliroast.intelliroast;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class RoastingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roasting);


        List<Entry> entries = new ArrayList<Entry>();
        entries.add(new Entry(0,20));
        entries.add(new Entry(200,120));
        entries.add(new Entry(360,170));
        entries.add(new Entry(530,200));
        entries.add(new Entry(600,205));
        entries.add(new Entry(630,210));
        LineChart chart = (LineChart) findViewById(R.id.chart);

        LineDataSet dataSet = new LineDataSet(entries, "Detail"); // add entries to dataset

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate(); // refresh
    }


}
