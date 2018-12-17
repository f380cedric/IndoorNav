package com.example.francois.indoornav.ui.test;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.example.francois.indoornav.R;
import com.example.francois.indoornav.decawave.Dwm1000;
import com.example.francois.indoornav.decawave.Dwm1000Master;
import com.example.francois.indoornav.spi.FT4222HSpiMaster;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.Locale;

public class CalibrationTest extends AppCompatActivity {

    private FT4222HSpiMaster mSpi;
    private Dwm1000Master dwm1000;
    private TextView init_textView;
    private TextView power_textView;
    private TextView distance_textView;
    private EditText antenna_delay;
    private TextView distance30_textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration_test);
        mSpi = new FT4222HSpiMaster(this, null);
        dwm1000 = new Dwm1000Master(mSpi);
        init_textView = findViewById(R.id.init_textView);
        power_textView = findViewById(R.id.power_textView);
        distance_textView = findViewById(R.id.distance_textView);
        distance30_textView = findViewById(R.id.distance30_textView);

        antenna_delay = findViewById(R.id.antenna_delay);
    }

    public void testDwm1000Connection(View view){

        if (dwm1000.initDwm1000()){
            init_textView.setText(R.string.success_init);
        }
        else{
            init_textView.setText(R.string.fail_init);
        }
    }

    public void getRxPower(View view) {
        power_textView.setText(String.format(Locale.getDefault(),
                "%f dBm", dwm1000.RxPower()));
    }

    public void calibrate(View view) {
        // TX_ANTD: Set the Tx antenna delay
        int antenna_delay = Integer.parseInt(0+this.antenna_delay.getText().toString())>>1;
        byte[] antennaDelay = {(byte)antenna_delay, (byte)(antenna_delay>>8)};
        dwm1000.writeDataSpi(Dwm1000.TX_ANTD, antennaDelay, (byte)0x02);
        dwm1000.writeDataSpi(Dwm1000.LDE_CTRL, (short)0x1804, antennaDelay, (byte)0x02);

        double distance = 0;
        int it;
        for( it = 0; it < 1000; ++it) {
            distance += dwm1000.getDistances()[0];
        }
        distance /= it;
        distance_textView.setText(String.format(Locale.getDefault(),"%f",distance));
    }

    public void calibrate30(View view) {
        int itmax = 1000;
        SummaryStatistics distance = new SummaryStatistics();
        int it;
        for( it = 0; it < itmax; ++it) {
            distance.addValue(dwm1000.getDistances()[0]);
        }
        distance30_textView.setText(String.format(Locale.getDefault(),"%f\n%f\n%f\n%f",
                distance.getMean(), distance.getMin(), distance.getMax(),
                distance.getStandardDeviation()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSpi.open();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mSpi.close();
        super.onDestroy();
    }

}
