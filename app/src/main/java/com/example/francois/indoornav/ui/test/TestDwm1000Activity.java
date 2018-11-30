package com.example.francois.indoornav.ui.test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.example.francois.indoornav.R;
import com.example.francois.indoornav.decawave.Dwm1000Master;
import com.example.francois.indoornav.location.LocationProviderAsyncTask;
import com.example.francois.indoornav.spi.FT311SPIMaster;
import com.example.francois.indoornav.util.PointD;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import java.util.Locale;

import static com.example.francois.indoornav.util.BytesUtils.byteArray4ToInt;

public class TestDwm1000Activity extends AppCompatActivity {

    private FT311SPIMaster mSpi;
    private Dwm1000Master dwm1000;
    private TextView textDWM1000ID;
    private TextView textTestBox;
    private TextView textTestBox2;
    private TextView textTestBox3;
    private TextView textTestBox4 ;
    private LocationProviderAsyncTask mytask;
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

        mytask = new LocationProviderAsyncTask(this, dwm1000, textTestBox2, textTestBox3);
    }

    // Button to test DWM1000 connection
    public void testDwm1000Connection(View view){
        if (dwm1000.initDwm1000()){
            textTestBox.setText(R.string.success_init);
        }
        else{
            textTestBox.setText(R.string.fail_init);
        }

        byte[] deviceId = dwm1000.readDeviceId();
        textDWM1000ID.setText(getString(R.string.device_id, byteArray4ToInt(deviceId)));
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
                    mytask = new LocationProviderAsyncTask(this, dwm1000, textTestBox2, textTestBox3);
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
        PointD coordinates = new PointD();
        for(int it = 0; it < itMax; ++it) {
            dwm1000.updateLocation();
            coordinates.set(dwm1000.getLastLocation());
            coordinateStats[0].addValue(coordinates.x);
            coordinateStats[1].addValue(coordinates.y);
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
