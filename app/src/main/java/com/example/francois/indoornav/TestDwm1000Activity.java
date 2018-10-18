package com.example.francois.indoornav;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import com.ftdi.j2xx.ft4222.FT_4222_Defines;
import com.ftdi.j2xx.ft4222.FT_4222_Device;
import com.ftdi.j2xx.ft4222.FT_4222_Spi_Master;
import com.ftdi.j2xx.interfaces.SpiMaster;

import java.text.DecimalFormat;
import java.util.Arrays;


public class TestDwm1000Activity extends AppCompatActivity {

    private SpiMaster spimInterface;
    private Dwm1000Master dwm1000;
    private TextView textDWM1000ID;
    private TextView textTestBox;
    private TextView textTestBox2;
    private TextView textTestBox3;
    private TextView textTestBox4 ;
    private Location mytask;
    private Switch trackingSwitch;

    private DecimalFormat df;

    class Location extends AsyncTask<Void, Double, String> {


        @Override
        protected String doInBackground(Void... voids) {
            double it = 0;
            double[] distance = new double[3];
            while (!isCancelled()) {
                try {
                    //Thread.sleep(500);
                    distance = dwm1000.getDistances();
                } catch (Exception e) {
                    Log.v("Error:", e.toString());
                }
                publishProgress(++it, distance[0]);
            }
            return "Done";
        }

        @Override
        protected void onProgressUpdate(Double... values) {
            super.onProgressUpdate(values);
            textTestBox2.setText("Iteration: " + values[0]);
            textTestBox3.setText("Distances: " + values[1]);
        }

        @Override
        protected void onPostExecute(String values) {
            super.onPostExecute(values);
            textTestBox3.setText(values);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_dwm1000);

        try {
            D2xxManager manager = D2xxManager.getInstance(this);
            int numdev;
            do {
                numdev = manager.createDeviceInfoList(this);
            } while(numdev < 1);
            FT_4222_Device ftdev = new FT_4222_Device(manager.openByIndex(this, 0));
            int status = ftdev.init();
            status = ftdev.setClock((byte) FT_4222_Defines.FT4222_ClockRate.SYS_CLK_24);
            spimInterface = ftdev.getSpiMasterDevice();
            status = spimInterface.init(FT_4222_Defines.FT4222_SPIMode.SPI_IO_SINGLE,
                    FT_4222_Defines.FT4222_SPIClock.CLK_DIV_8,
                    FT_4222_Defines.FT4222_SPICPOL.CLK_IDLE_LOW,
                    FT_4222_Defines.FT4222_SPICPHA.CLK_LEADING, (byte)1);
            status = ((FT_4222_Spi_Master)spimInterface).setDrivingStrength(FT_4222_Defines.SPI_DrivingStrength.DS_4MA,
                    FT_4222_Defines.SPI_DrivingStrength.DS_12MA, FT_4222_Defines.SPI_DrivingStrength.DS_4MA);
            int a = 0;
        } catch (D2xxManager.D2xxException e) {
            e.printStackTrace();
        }
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
                    mytask = new Location();
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
        byte[] read = new byte[5];
        byte[] write = new byte[5];
        write[0] = (byte)0x04;
        write[1] = (byte)0xF0;
        write[2] = (byte)0xFF;
        write[3] = (byte)0x0F;
        write[4] = (byte)0xAA;
        int stat = spimInterface.singleReadWrite(read,write, 5, new int[1], true);
        int stat2 = spimInterface.singleReadWrite(read,write, 5, new int[1], true);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mytask.cancel(true);
        //spimInterface.DestroyAccessory();
        super.onDestroy();
    }


}
