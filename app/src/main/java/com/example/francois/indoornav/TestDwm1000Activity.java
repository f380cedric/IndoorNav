package com.example.francois.indoornav;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
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

    private DecimalFormat df;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_dwm1000);

        spimInterface = new FT311SPIMasterInterface(this);
        dwm1000 = new Dwm1000Master(spimInterface);

        textDWM1000ID   = findViewById(R.id.textView7);
        textTestBox     = findViewById(R.id.textView);
        textTestBox2    = findViewById(R.id.textView14);
        textTestBox3    = findViewById(R.id.textView15);
        textTestBox4    = findViewById(R.id.textView16);
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
    public void exploreDwm1000Environment(View view) {
        byte address;
        byte[] offset;
        byte dataLength;

        // Prepare messages to be sent
        byte master_first_message = (byte) 0x11;
        byte master_second_message = (byte) 0x21;
        byte slave_standard_message = (byte) 0x1a;

        // Disable Rx to save power
        //dwm1000.disableUwbRx();
        //sleep(1);

        // SEND FIRST MESSAGE
        /*byte[] txMessage = new byte[1];
        byte lengthTxMessage = (byte) 0x01;
        txMessage[0] = master_first_message;
        dwm1000.sendFrameUwb(txMessage, lengthTxMessage);
        // Read TX_TIME register
        address = (byte) 0x17;
        dataLength = (byte) 10;
        byte TxOk;
        do {
            TxOk = (byte) (dwm1000.readDataSpi(Dwm1000.SYS_STATUS, (byte) 0x01)[0] & (1 << Dwm1000.TXFRS));
        } while (TxOk != (byte) (1 << Dwm1000.TXFRS));
        byte[] tx_time = dwm1000.readDataSpi(address, dataLength);
        byte[] tx_stamp = Arrays.copyOfRange(tx_time, 0, 5);
        textTestBox2.setText("Tx - Timestamp: " + df.format(Dwm1000.TIME_UNIT * byteArray5ToLong(tx_stamp)) + " sec");

        // RECEIVE FIRST RESPONSE
        sleep(10);
        if (dwm1000.checkForFrameUwb()) {
            byte[] rx_frame = dwm1000.receiveFrameUwb();
            textTestBox3.setText("Rx - PaydeviceIdTheorload: 0x" + byteArrayToHex(rx_frame));
            // Read RX_TIME register
            address = (byte) 0x15;
            dataLength = 14;
            byte[] rx_time = dwm1000.readDataSpi(address, dataLength);
            byte[] rx_stamp = Arrays.copyOfRange(rx_time, 0, 5);
            textTestBox4.setText("Rx - Timestamp: " + df.format(Dwm1000.TIME_UNIT * byteArray5ToLong(rx_stamp)) + " sec");
        } else {
            textTestBox3.setText("FAILURE: Frame not received ! ");
        }*/

        /*Thread t = new Thread(dwm1000, "ranging_thread");
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        textTestBox4.setText("Timestamp: " + df.format(dwm1000.duration));*/
        textTestBox4.setText("Timestamp: " + df.format(dwm1000.getDistance()));


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
        spimInterface.DestroyAccessory();
        super.onDestroy();
    }


}
