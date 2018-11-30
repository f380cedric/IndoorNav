package com.example.francois.indoornav.ui.test;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import com.example.francois.indoornav.R;
import com.example.francois.indoornav.util.SensorFusion;

import java.text.DecimalFormat;

public class TestOrientationActivity extends Activity
        implements SensorEventListener {

    private SensorFusion sensorFusion;
    private SensorManager sensorManager = null;
    private TextView azimuthText, pithText, rollText;
    private DecimalFormat d = new DecimalFormat("###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_orientation);

        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        registerSensorManagerListeners();
        sensorFusion = new SensorFusion();
        sensorFusion.setMode(SensorFusion.Mode.FUSION);

        d.setMaximumFractionDigits(0);
        d.setMinimumFractionDigits(0);

        azimuthText = findViewById(R.id.azimuth);
        pithText = findViewById(R.id.pitch);
        rollText = findViewById(R.id.roll);
    }

    private void updateOrientationDisplay() {

        double azimuthValue = sensorFusion.getAzimuth();
        double pitchValue =  sensorFusion.getPitch();
        double rollValue =  sensorFusion.getRoll();

        azimuthText.setText(String.valueOf(d.format(azimuthValue)));
        pithText.setText(String.valueOf(d.format(pitchValue)));
        rollText.setText(String.valueOf(d.format(rollValue)));

    }


    public void registerSensorManagerListeners() {
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST);
    }


    @Override
    public void onStop() {
        super.onStop();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerSensorManagerListeners();
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
