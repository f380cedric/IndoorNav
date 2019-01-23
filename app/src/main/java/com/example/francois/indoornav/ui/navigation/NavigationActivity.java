package com.example.francois.indoornav.ui.navigation;

import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.example.francois.indoornav.R;
import com.example.francois.indoornav.decawave.Dwm1000Master;
import com.example.francois.indoornav.location.ILocationProvider;
import com.example.francois.indoornav.location.LocationProvider;
import com.example.francois.indoornav.spi.FT4222HSpiMaster;
import com.example.francois.indoornav.spi.SpiMasterListener;
import com.example.francois.indoornav.util.PointD;
import com.example.francois.indoornav.util.SensorFusion;


public class NavigationActivity extends AppCompatActivity implements Handler.Callback,
        SpiMasterListener, SensorEventListener {

    private NavigationView navigationView;
    private FT4222HSpiMaster mSpi;
    private Dwm1000Master dwm1000;
    private LocationProvider location;
    private Handler handler;
    private SensorFusion sensorFusion;
    private SensorManager sensorManager;
    private TextView displayCoordinates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        navigationView = findViewById(R.id.navigationView);
        navigationView.setMapArrayId(getIntent().getIntExtra("mapId", R.array.littleRoom));
        displayCoordinates = findViewById(R.id.displayCoordinates);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        registerSensorManagerListeners();
        sensorFusion = new SensorFusion();
        sensorFusion.setMode(SensorFusion.Mode.FUSION);
        mSpi = new FT4222HSpiMaster(this, this);
        handler = new Handler(this);
    }

    private void registerSensorManagerListeners() {
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_UI);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_UI);
    }

    public void updateOrientationDisplay() {
        double azimuthValue = sensorFusion.getAzimuth();
        navigationView.setOrientation(azimuthValue-45);
    }

    @Override
    protected void onResume() {
        super.onResume();
        navigationView.setVisibility(View.VISIBLE);
        registerSensorManagerListeners();
        mSpi.open();
        if(location != null) {
            location.onResume();
        }
    }

    @Override
    protected void onPause() {
        navigationView.setVisibility(View.INVISIBLE);
        sensorManager.unregisterListener(this);
        if (location != null) {
            location.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mSpi.unregisterListener(this);
        handler.removeCallbacksAndMessages(null);
        if (location != null) {
            location.quit();
            location.interrupt();
            location = null;
        }
        mSpi.close();
        super.onDestroy();
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == ILocationProvider.SUCCESS) {
            navigationView.setPositions((PointD) msg.obj);
            displayCoordinates.setText(getString(R.string.coor_cm,
                    ((PointD) msg.obj).x, ((PointD) msg.obj).y));
        }
        return true;
    }

    @Override
    public void onDeviceConnected(){
        if(dwm1000 == null) {
            dwm1000 = new Dwm1000Master(mSpi);
            Thread t = new Thread(() -> dwm1000.initDwm1000());
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (location == null){
            location = new LocationProvider(handler, dwm1000);
        }
    }

    @Override
    public void onDeviceDisconnected() {
        if(location != null) {
            location.onPause();
            location.quit();
            location.interrupt();
            location = null;
        }
        dwm1000 = null;
    }


    @Override
    public void onDataFailure(int status) {
        Log.d("DWM1000 IOError", String.valueOf(status));
        if(location != null) {
            location.onPause();
            location.quit();
            location.interrupt();
            location = null;
        }
        dwm1000 = null;
        mSpi.open();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                sensorFusion.setAccel(event.values);
                sensorFusion.calculateAccMagOrientation();
                //myText.setText(String.valueOf(event.values[0]));
                break;

            case Sensor.TYPE_GYROSCOPE:
                sensorFusion.gyroFunction(event);
                //myText.setText(String.valueOf(event.values[0]));
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                sensorFusion.setMagnet(event.values);
                //myText.setText(String.valueOf(event.values[0]));
                break;
        }
        updateOrientationDisplay();
    }

}
