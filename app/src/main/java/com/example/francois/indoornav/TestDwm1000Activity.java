package com.example.francois.indoornav;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import java.util.Locale;

public class TestDwm1000Activity extends AppCompatActivity {

    private FT311SPIMaster mSpi;
    private Dwm1000Master dwm1000;
    private TextView textDWM1000ID;
    private TextView textTestBox;
    private TextView textTestBox2;
    private TextView textTestBox3;
    private TextView textTestBox4 ;
    private LocationAsyncTask mytask;
    private Switch trackingSwitch;
    private TextView statTextView;

    /*class Location extends AsyncTask<Void, Double, String> {


        @Override
        protected String doInBackground(Void... voids) {
            double it = 0;
            double[] coordinates = new double[2];
            while (!isCancelled()) {
                try {
                    //Thread.sleep(500);
                    coordinates = dwm1000.updateCoordinates();
                } catch (Exception e) {
                    Log.v("Error:", e.toString());
                }
                publishProgress(++it, coordinates[0], coordinates[1]);
            }
            return "Done";
        }

        @Override
        protected void onProgressUpdate(Double... values) {
            super.onProgressUpdate(values);
            textTestBox2.setText(getString(R.string.it, values[0]));
            textTestBox3.setText(getString(R.string.coor,values[1], values[2]));
        }

        @Override
        protected void onPostExecute(String values) {
            super.onPostExecute(values);
            textTestBox3.setText(values);
        }


    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_dwm1000);

        mSpi = new FT311SPIMaster(this);
        dwm1000 = new Dwm1000Master(mSpi);

        textDWM1000ID   = findViewById(R.id.textView7);
        textTestBox     = findViewById(R.id.textView);
        textTestBox2    = findViewById(R.id.textView14);
        textTestBox3    = findViewById(R.id.textView15);
        textTestBox4    = findViewById(R.id.textView16);
        trackingSwitch  = findViewById(R.id.switch1);
        statTextView   = findViewById(R.id.statTextView);

        mytask = new LocationAsyncTask(this, dwm1000, textTestBox2, textTestBox3);
    }

    // Button to test DWM1000 connection
    public void testDwm1000Connection(View view){
        if (dwm1000.initDwm1000()){
            textTestBox.setText(R.string.success_init);
        }
        else{
            textTestBox.setText(R.string.warning + R.string.fail_init);
        }

        byte[] deviceId = dwm1000.readDeviceId();
        textDWM1000ID.setText(getString(R.string.device_id, Dwm1000.byteArray4ToInt(deviceId)));
    }

    // Button to explore DWM1000 Environment
    public void trackingSwitch(View view) {

        if(trackingSwitch.isChecked()) {
            switch (mytask.getStatus()) {
                case PENDING:
                    mytask.execute();
                    break;
                case RUNNING:
                    break;
                case FINISHED:
                    mytask = new LocationAsyncTask(this, dwm1000, textTestBox2, textTestBox3);
                    mytask.execute();
                    break;
            }
        }
        else {
            mytask.cancel(true);
        }

    }

    public void taskStatus(View view) {
        textTestBox4.setText(mytask.getStatus().toString());
    }


    public void variation(View view){
        int itMax = 1000;
        SummaryStatistics coordinateStats[] = {new SummaryStatistics(),new SummaryStatistics()};
        double[] coordinates;
        for(int it = 0; it < itMax; ++it) {
            coordinates = dwm1000.updateCoordinates();
            coordinateStats[0].addValue(coordinates[0]);
            coordinateStats[1].addValue(coordinates[1]);
        }
        statTextView.setText(String.format(Locale.getDefault(),
                "%f\n%f\n%f\n%f\n\n%f\n%f\n%f\n%f", coordinateStats[0].getMean(),
                coordinateStats[0].getMin(), coordinateStats[0].getMax(),
                coordinateStats[0].getStandardDeviation(), coordinateStats[1].getMean(),
                coordinateStats[1].getMin(), coordinateStats[1].getMax(),
                coordinateStats[1].getStandardDeviation()));
    }
    @Override
    protected void onResume() {
        super.onResume();
        mSpi.ResumeAccessory();
    }

    @Override
    protected void onDestroy() {
        mytask.cancel(true);
        mSpi.DestroyAccessory();
        super.onDestroy();
    }
}
