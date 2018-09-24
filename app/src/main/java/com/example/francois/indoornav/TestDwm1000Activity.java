package com.example.francois.indoornav;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import java.text.DecimalFormat;

public class TestDwm1000Activity extends AppCompatActivity {

    private FT311SPIMasterInterface spimInterface;
    private Dwm1000Master dwm1000;
    private TextView textDWM1000ID;
    private TextView textTestBox;
    private TextView textTestBox2;
    private TextView textTestBox3;
    private TextView textTestBox4 ;
    private Location mytask;
    private Switch trackingSwitch;

    private DecimalFormat df;

    class Location extends AsyncTask<Void, Integer, Long> {


        @Override
        protected Long doInBackground(Void... voids) {
            int it = 0;
            while (!isCancelled()) {
                try {
                    Thread.sleep(500);
                    dwm1000.getDistance();
                } catch (Exception e) {
                    Log.v("Error:", e.toString());
                }
                publishProgress(++it);
            }
            return dwm1000.getDistance();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            textTestBox4.setText("Iteration: " + values[0]);
        }

        @Override
        protected void onPostExecute(Long aLong) {
            super.onPostExecute(aLong);
            textTestBox4.setText("Timestamp: " + aLong);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_dwm1000);

        spimInterface = new FT311SPIMasterInterface(this);
        dwm1000 = new Dwm1000Master(spimInterface);
        mytask = new Location();

        textDWM1000ID   = findViewById(R.id.textView7);
        textTestBox     = findViewById(R.id.textView);
        textTestBox2    = findViewById(R.id.textView14);
        textTestBox3    = findViewById(R.id.textView15);
        textTestBox4    = findViewById(R.id.textView16);
        trackingSwitch  = findViewById(R.id.switch1);
        df = new DecimalFormat();
        df.setMaximumFractionDigits(6);

    }

    // Button to test DWM1000 connection
    public void testDwm1000Connection(View view){
        if (dwm1000.initDwm1000()){
            textTestBox.setText("Device initialized succesfully");
        }
        else{
            textTestBox.setText("WARNING: device failed to initialize");
        }
        byte[] deviceId = dwm1000.readDeviceId();
        textDWM1000ID.setText("Device ID: 0x" + byteArrayToHex(deviceId));
        textTestBox2.setText(mytask.getStatus().toString());
    }

    // Button to explore DWM1000 Environment
    public void trackingSwitch(View view) {

        if(((Switch)view).isChecked()) {
            switch (mytask.getStatus()) {
                case PENDING:
                    mytask.execute();
                    break;
                case RUNNING:
                    break;
                case FINISHED:
                    mytask = new Location();
                    mytask.execute();
                    break;
            }
        }
        else {
            mytask.cancel(true);
        }

    }

    // Convert byte array to hex string
    private String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        //for(byte b: a)
        for (int i = a.length-1; i>=0; i--) {
            byte b = a[i];
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // Convert byte to hex string
    private static String byteToHex(byte a) {
        return String.format("%02x", a);
    }

    // Convert 5-element byte array to int


    @Override
    protected void onResume() {
        super.onResume();
        spimInterface.ResumeAccessory();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mytask.cancel(true);
        spimInterface.DestroyAccessory();
        super.onDestroy();
    }


}
