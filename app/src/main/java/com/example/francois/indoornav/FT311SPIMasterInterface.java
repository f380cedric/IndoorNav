package com.example.francois.indoornav;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;

import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

/******************************FT311 GPIO interface class******************************************/
class FT311SPIMasterInterface
{

    private static final String ACTION_USB_PERMISSION =    "com.SPIMasterDemo.USB_PERMISSION";
    private UsbManager usbmanager;
    private PendingIntent mPermissionIntent;
    private ParcelFileDescriptor filedescriptor;
    private FileInputStream inputstream;
    private FileOutputStream outputstream;
    private boolean mPermissionRequestPending = false;
    //private volatile boolean READ_ENABLE = true;
    //private boolean accessory_attached = false;
    //public handler_thread handlerThread;

    private byte [] usbdata;
    private byte []	writeusbdata;
    private int readcount;
    private final byte  maxnumbytes = (byte) 63; /*maximum data bytes, except command*/
    //public volatile boolean datareceived = true;

    private Context global_context;

    private static final String ManufacturerString = "FTDI";
    private static final String ModelString = "FTDISPIMasterDemo";
    private static final String VersionString = "1.0";


    /*constructor*/
    FT311SPIMasterInterface(Context context){
        super();
        global_context = context;
        /*shall we start a thread here or what*/
        usbdata = new byte[64];
        writeusbdata = new byte[64];

        usbmanager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        // Log.d("LED", "usbmanager" +usbmanager);
        mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        context.registerReceiver(mUsbReceiver, filter);

        inputstream = null;
        outputstream = null;
    }

    /*reset method*/
    synchronized int Reset()
    {
        /*create the packet*/
        writeusbdata[0] = (byte) 0x64;
        /*send the packet over the USB*/
        return SendPacket(1);
    }


    synchronized int SetConfig(byte clockPhase, byte dataOrder, int clockFreq)
    {
        /*check for maximum clock freq, 24Mhz*/
        if(clockFreq > 24000000){
            clockFreq = 24000000;
        }

        writeusbdata[0] = (byte) 0x61;
        writeusbdata[1] = clockPhase;
        writeusbdata[2] = dataOrder;
        writeusbdata[3] = (byte)(clockFreq & 0xff);
        writeusbdata[4] = (byte)((clockFreq >> 8) & 0xff);
        writeusbdata[5] = (byte)((clockFreq >> 16)& 0xff);
        writeusbdata[6] = (byte)((clockFreq >> 24)& 0xff);

        return SendPacket(7);

    }


    /*write data*/
    synchronized int SendData(byte numBytes, byte[] buffer)
    {
        int status;
        //status = 0x02; /*error by default*/
        /*
         * if num bytes are more than maximum limit
         */
        if(numBytes < 1){
            /*return the status with the error in the command*/
            return 0x01;
        }

        /*check for maximum limit*/
        if(numBytes > maxnumbytes){
            return 0x02;
        }

        /*prepare the packet to be sent*/
        System.arraycopy(buffer, 0, writeusbdata, 1, numBytes);

        /*prepare the usbpacket*/
        writeusbdata[0] = (byte) 0x62;

        //do {
        if ((status = SendPacket(++numBytes)) != 0) {
            return status;
        }
        if ((status = ReadPacket(numBytes)) != 0) {
            return status;
        }
          /*  datareceived = false;

            /*wait while the data is received*/
            /*FIXME, may be create a thread to wait on , but any
             * way has to wait in while loop
             */
            /*long start = SystemClock.currentThreadTimeMillis();
            while(!datareceived && (SystemClock.currentThreadTimeMillis() - start)<10000){
                Log.d("SPI", "Writing");
                }
        } while(!datareceived);*/

        //status = 0x01;
        if(usbdata[0] != 0x62) {
            return 0x06;
        }
            /*copy the received data into the buffer*/
        System.arraycopy(usbdata, 1, buffer, 0, readcount - 1);
        /*update the received length*/
        /*read the next usb data*/
        //datareceived = false;
        return status;
    }


    /*read data*/
    synchronized int ReadData(byte numBytes, byte[] buffer)
    {
        int status;
        /*
         * if num bytes are more than maximum limit
         */
        if(numBytes < 1){
            /*return the status with the error in the command*/
            return 0x01;
        }

        /*check for maximum limit*/
        if(numBytes > maxnumbytes){
            return 0x02;
        }

        /*prepare the packet to be sent*/
        System.arraycopy(buffer, 0, writeusbdata, 1, numBytes);

        /*prepare the usbpacket*/
        writeusbdata[0] = (byte) 0x63;
        //do {
        if ((status = SendPacket(numBytes + 1)) != 0) {
            return status;
        }
        if ((status = ReadPacket(numBytes+1)) != 0) {
            return status;
        }
            /*datareceived = false;
            /*wait while the data is received*/
            /*FIXME, may be create a thread to wait on , but any
             * way has to wait in while loop
             */
            /*long start = SystemClock.currentThreadTimeMillis();
            while (!datareceived && (SystemClock.currentThreadTimeMillis() - start) < 10000) {
                Log.d("SPI", "Reading");
            }
        } while(!datareceived);*/

        /*success by default*/
        if(usbdata[0] !=  0x63) {
            return 0x06;
        }
            /*copy the received data into the buffer*/
        System.arraycopy(usbdata, 1, buffer, 0, readcount - 1);

        /*update the received length*/
        /*read the next usb data*/
        //datareceived = false;

        return status;
    }

    synchronized private int ReadPacket(int numBytes) {
        readcount = 0;
        int status = 0x00;
        while(readcount < numBytes) {
            try {
                readcount += inputstream.read(usbdata, readcount, numBytes - readcount);
            } catch (IOException e) {
                e.printStackTrace();
                status = 0x05;
            }
        }
        return status;
    }
    /*method to send on USB*/

    synchronized private int SendPacket(int numBytes)
    {

        int status = 0x03;
        try {
            if(outputstream != null){
                outputstream.write(writeusbdata, 0,numBytes);
                status = 0x00;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            status = 0x04;
            e.printStackTrace();
        }
        return status;

    }

    /*resume accessory*/
    void ResumeAccessory()
    {
        /*int i = 0;
        HashMap<String, UsbDevice> deviceList = usbmanager.getDeviceList();
        boolean empty = deviceList.isEmpty();
        UsbAccessory[] accessories = usbmanager.getAccessoryList();
        boolean aempty = (accessories.length == 0);
        ++i;*/
        // Intent intent = getIntent();
        if (inputstream != null && outputstream != null) {
            return;
        }

        UsbAccessory[] accessories = usbmanager.getAccessoryList();
        if(accessories != null)
        {
            Toast.makeText(global_context, "Accessory Attached", Toast.LENGTH_SHORT).show();
        }
        else
        {
            return;
        }

        UsbAccessory accessory = accessories[0];
        if (accessory != null) {
            if(!ManufacturerString.equals(accessory.getManufacturer()))
            {
                Toast.makeText(global_context, "Manufacturer is not matched!", Toast.LENGTH_SHORT).show();
                return;
            }

                if(!ModelString.equals(accessory.getModel()))
            {
                Toast.makeText(global_context, "Model is not matched!", Toast.LENGTH_SHORT).show();
                return;
            }

            if(!VersionString.equals(accessory.getVersion()))
            {
                Toast.makeText(global_context, "Version is not matched!", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(global_context, "Manufacturer, Model & Version are matched!", Toast.LENGTH_SHORT).show();

            if (usbmanager.hasPermission(accessory)) {
                OpenAccessory(accessory);
            }
            else
            {
                synchronized (mUsbReceiver) {
                    if (!mPermissionRequestPending) {
                        Toast.makeText(global_context, "Request USB Permission", Toast.LENGTH_SHORT).show();
                        usbmanager.requestPermission(accessory,
                                mPermissionIntent);
                        mPermissionRequestPending = true;
                    }
                }
            }
        }

    }

    /*destroy accessory*/
    void DestroyAccessory(){
        global_context.unregisterReceiver(mUsbReceiver);
        /*if(accessory_attached)
        {
            //READ_ENABLE = false;  // set false condition for handler_thread to exit waiting data loop
            //byte [] tmpnRBytes = {0};
            byte [] temp2 = new byte[64];
            ReadData((byte)3,temp2); // send dummy data for instream.read going
            try{Thread.sleep(10);}
            catch(Exception e){}
        }*/
        CloseAccessory();
    }



    /*********************helper routines*************************************************/

    private void OpenAccessory(UsbAccessory accessory)
    {
        filedescriptor = usbmanager.openAccessory(accessory);
        if(filedescriptor != null){
            FileDescriptor fd = filedescriptor.getFileDescriptor();
            inputstream = new FileInputStream(fd);
            outputstream = new FileOutputStream(fd);
        }

        //handlerThread = new handler_thread(inputstream);
        //handlerThread.start();
    }

    private void CloseAccessory()
    {
        try{
            if(filedescriptor != null)
                filedescriptor.close();

        }catch (IOException ignored){}

        try {
            if(inputstream != null)
                inputstream.close();
        } catch(IOException ignored){}

        try {
            if(outputstream != null)
                outputstream.close();

        }catch(IOException ignored){}
        /*FIXME, add the notfication also to close the application*/

        filedescriptor = null;
        inputstream = null;
        outputstream = null;

        //System.exit(0);

    }


    /***********USB broadcast receiver*******************************************/
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action))
            {
                synchronized (this)
                {
                    UsbAccessory accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                    {
                        Toast.makeText(global_context, "Allow USB Permission", Toast.LENGTH_SHORT).show();
                        OpenAccessory(accessory);
                    }
                    else
                    {
                        Toast.makeText(global_context, "Deny USB Permission", Toast.LENGTH_SHORT).show();
                        Log.d("LED", "permission denied for accessory "+ accessory);

                    }
                    mPermissionRequestPending = false;
                }
            }
            else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action))
            {
                CloseAccessory();
            }else
            {
                Log.d("LED", "....");
            }
        }
    };





    /*usb input data handler*/
    /*private class handler_thread  extends Thread {
        FileInputStream instream;

        handler_thread(FileInputStream stream ){
            instream = stream;
        }

        public void run()
        {

            while(READ_ENABLE == true)
            {

                try
                {
                    *//*dont overwrite the previous buffer*//*
                    if((instream != null) && (datareceived==false))
                    {
                        readcount = instream.read(usbdata,0,64);
                        if(readcount > 0)
                        {
                            datareceived = true;
                            *//*send only when you find something*//*
                        }
                    }
                }catch (IOException e){}
            }
        }
    }*/
}