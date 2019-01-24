package com.example.francois.indoornav.ui;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.example.francois.indoornav.R;
import com.example.francois.indoornav.decawave.Dwm1000Master;

public class AnchorsCoordinates extends AppCompatActivity {

    private TextView error;
    private EditText anchor1x;
    private EditText anchor1y;
    private EditText anchor1z;

    private EditText anchor2x;
    private EditText anchor2y;
    private EditText anchor2z;

    private EditText anchor3x;
    private EditText anchor3y;
    private EditText anchor3z;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anchors_coordinates);

        anchor1x = findViewById(R.id.editText14);
        anchor1y = findViewById(R.id.editText15);
        anchor1z = findViewById(R.id.editText16);
        anchor2x = findViewById(R.id.editText20);
        anchor2y = findViewById(R.id.editText21);
        anchor2z = findViewById(R.id.editText22);
        anchor3x = findViewById(R.id.editText23);
        anchor3y = findViewById(R.id.editText24);
        anchor3z = findViewById(R.id.editText25);

        error = findViewById(R.id.textView27);
    }

    public void setAnchorsCoordinates(View view) {
        try {
            Dwm1000Master.setAnchorsCoordinates(Integer.parseInt(anchor1x.getText().toString()),
                    Integer.parseInt(anchor1y.getText().toString()),
                    Integer.parseInt(anchor1z.getText().toString()),
                    Integer.parseInt(anchor2x.getText().toString()),
                    Integer.parseInt(anchor2y.getText().toString()),
                    Integer.parseInt(anchor2z.getText().toString()),
                    Integer.parseInt(anchor3x.getText().toString()),
                    Integer.parseInt(anchor3y.getText().toString()),
                    Integer.parseInt(anchor3z.getText().toString()));
            Dwm1000Master.AnchorsCoordForced = true;
            error.setTextColor(Color.GREEN);
            error.setText("Done");
        } catch (NumberFormatException e) {
            error.setTextColor(Color.RED);
            error.setText("Invalid value(s)");
        }
    }
}
