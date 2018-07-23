package com.example.francois.indoornav;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.Arrays;

import static android.os.SystemClock.sleep;

public class TestDwm1000Activity extends AppCompatActivity {

    public FT311SPIMasterInterface spimInterface;
    public Dwm1000 dwm1000;
    TextView textDWM1000ID, textTestBox, textTestBox2, textTestBox3, textTestBox4 ;
    private double time_unit = 0.00000000001565;
    DecimalFormat df;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_dwm1000);

        spimInterface = new FT311SPIMasterInterface(this);
        dwm1000 = new Dwm1000(spimInterface);

        textDWM1000ID   = (TextView) findViewById(R.id.textView7);
        textTestBox     = (TextView) findViewById(R.id.textView);
        textTestBox2    = (TextView) findViewById(R.id.textView14);
        textTestBox3    = (TextView) findViewById(R.id.textView15);
        textTestBox4    = (TextView) findViewById(R.id.textView16);
        df = new DecimalFormat();
        df.setMaximumFractionDigits(4);

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
    public void exploreDwm1000Environment(View view){
        byte address;
        byte[] offset;
        byte dataLength;

        // Prepare messages to be sent
        byte master_first_message = (byte)0x12;
        byte master_second_message = (byte)0x22;
        byte slave_standard_message = (byte)0x2a;

        // Disable Rx to save power
        dwm1000.disableUwbRx();
        sleep(1);

        // Send message
        byte[] txMessage = new byte[1];
        byte lengthTxMessage = (byte)0x01;
        txMessage[0] =  master_first_message;
        dwm1000.sendFrameUwb(txMessage, lengthTxMessage);

        // Read TX_TIME register
        address    = (byte)0x17;
        offset     = new byte[1]; offset[0]  = (byte)0xff;
        dataLength = 10;
        byte[] tx_time = dwm1000.readDataSpi(address,offset,dataLength);
        byte[] tx_stamp = Arrays.copyOfRange(tx_time, 0, 5);
        textTestBox2.setText("Tx Time stamp: " + df.format(time_unit*byteArray5ToLong(tx_stamp)) + " sec");

        // Read SYS_STATUS register
        sleep(10);
        byte[] sys_status = dwm1000.checkForFrameUwb();
        textTestBox3.setText("SYS_STATUS: 0x" + byteArrayToHex(sys_status));



        /*
        byte[] sys_status;
        for (int i=0; i<1000; i++) {
            sys_status = dwm1000.checkForFrameUwb();
            textTestBox3.setText("SYS_STATUS: 0x" + byteArrayToHex(sys_status));
            byte rxdfr = (byte)(sys_status[1] & 0x20);
            if (rxdfr == (byte)0x20){
                textTestBox4.setText("SUCCESS: Frame received ! ");
                break;
            }
            else{
                textTestBox4.setText("FAILURE: Frame not received ! ");
            }
            sleep(1);
        }
        */

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
        StringBuilder sb = new StringBuilder(1 * 2);
        sb.append(String.format("%02x", a));
        return sb.toString();
    }

    // Convert 5-element byte array to int
    private long byteArray5ToLong(byte[] bytes) {
        return (long)(bytes[0] & 0xFF) |
                (long)(bytes[1] & 0xFF) << 8 |
                (long)(bytes[2] & 0xFF) << 16 |
                (long)(bytes[3] & 0xFF) << 24 |
                (long)(bytes[4] & 0xFF) << 32;
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


}
