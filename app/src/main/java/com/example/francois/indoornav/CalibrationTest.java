package com.example.francois.indoornav;

import android.content.res.TypedArray;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;

public class CalibrationTest extends AppCompatActivity {

    private FT311SPIMasterInterface spimInterface;
    private Dwm1000Master dwm1000;
    private Spinner prf_spinner;
    private Spinner sfd_spinner;
    private TextView init_textView;
    private TextView power_textView;
    TypedArray a_values;
    int[] to_remove_values;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration_test);
        spimInterface = new FT311SPIMasterInterface(this);
        dwm1000 = new Dwm1000Master(spimInterface);
        prf_spinner = findViewById(R.id.prf_spinner);
        sfd_spinner = findViewById(R.id.sfd_spinner);
        init_textView = findViewById(R.id.init_textView);
        power_textView = findViewById(R.id.power_textView);
        a_values = getResources().obtainTypedArray(R.array.prf_to_A);
        to_remove_values = getResources().getIntArray(R.array.sdf_to_remove);
    }

    public void testDwm1000Connection(View view){
        if (dwm1000.initDwm1000()){
            init_textView.setText("Device initialized succesfully");
        }
        else{
            init_textView.setText("WARNING: device failed to initialize");
        }
    }

    public void getRxPower(View view) {
        double A = a_values.getFloat(prf_spinner.getSelectedItemPosition(), 0);
        int to_remove = to_remove_values[sfd_spinner.getSelectedItemPosition()];
        dwm1000.getDistances();
        byte[] cir_pwr = dwm1000.readDataSpi(Dwm1000.RX_FQUAL, (byte)6, (byte)2);
        int C = (cir_pwr[0] & 0xFF) | (cir_pwr[1] & 0xFF) << 8;
        byte[] rxpacc = dwm1000.readDataSpi(Dwm1000.RX_FINFO, (byte)2, (byte) 2);
        byte [] rxpacc_nosat = dwm1000.readDataSpi(Dwm1000.DRX_CONF,(byte) 0x2C, (byte)2);
        int N = (rxpacc[0] & 0xF0)>>>4 | (rxpacc[1] & 0xFF)<< 4;
        int correct = (rxpacc_nosat[0] & 0xFF) | (rxpacc_nosat[1] & 0xFF)<<8;
        if (N == correct) {
            N -= to_remove;
        }
        double rx_level = 10 * Math.log10(C * (1<<17) / (N * N)) - A;
        power_textView.setText(rx_level + " dBm");
    }

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
        spimInterface.DestroyAccessory();
        super.onDestroy();
    }

}
