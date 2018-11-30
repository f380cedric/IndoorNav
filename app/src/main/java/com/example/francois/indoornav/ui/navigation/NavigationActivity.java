package com.example.francois.indoornav.ui.navigation;

import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.TextView;

import com.example.francois.indoornav.R;
import com.example.francois.indoornav.decawave.Dwm1000Master;
import com.example.francois.indoornav.location.ILocationProvider;
import com.example.francois.indoornav.location.LocationProvider;
import com.example.francois.indoornav.spi.FT311SPIMaster;
import com.example.francois.indoornav.spi.FT311SPIMasterListener;
import com.example.francois.indoornav.util.PointD;
import com.example.francois.indoornav.util.SensorFusion;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;


public class NavigationActivity extends AppCompatActivity implements Handler.Callback,
        FT311SPIMasterListener, SensorEventListener {

    private NavigationView navigationView;
    private FT311SPIMaster mSpi;
    private Dwm1000Master dwm1000;
    private LocationProvider location;
    private Handler handler;
    private SensorFusion sensorFusion;
    private SensorManager sensorManager;
    private DescriptiveStatistics meanAzimuth;
    private TextView displayCoordinates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        navigationView = findViewById(R.id.navigationView);
        displayCoordinates = findViewById(R.id.displayCoordinates);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        registerSensorManagerListeners();
        sensorFusion = new SensorFusion();
        sensorFusion.setMode(SensorFusion.Mode.FUSION);
        meanAzimuth = new DescriptiveStatistics(30);

        mSpi = new FT311SPIMaster(this);
        mSpi.registerListener(this);
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
        //Log.i("Orientation", azimuthValue + ", " + pitchValue + ", " + rollValue);
        meanAzimuth.addValue(azimuthValue);
        Log.i("Orientation, mean", String.valueOf(meanAzimuth.getMean()));
        navigationView.setOrientation(azimuthValue);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerSensorManagerListeners();
        mSpi.ResumeAccessory();
        if(location != null) {
            location.onResume();
        }
    }

    @Override
    protected void onPause() {
        sensorManager.unregisterListener(this);
        if (location != null) {
            location.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mSpi.unregisterListener(this);
        if (location != null) {
            location.quit();
            location.interrupt();
            location = null;
        }
        mSpi.DestroyAccessory();
        super.onDestroy();
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == ILocationProvider.SUCCESS) {
            navigationView.setPositions((PointD) msg.obj);
            displayCoordinates.setText(msg.obj.toString());
        }
        return true;
    }

    @Override
    public void onDeviceConnected(){
        if(dwm1000 == null) {
            dwm1000 = new Dwm1000Master(mSpi);
            dwm1000.initDwm1000();
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
        mSpi.ResumeAccessory();
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
