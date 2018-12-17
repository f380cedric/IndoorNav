package com.example.francois.indoornav.spi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import com.ftdi.j2xx.ft4222.FT_4222_Defines;
import com.ftdi.j2xx.ft4222.FT_4222_Device;
import com.ftdi.j2xx.ft4222.FT_4222_Spi_Master;
import com.ftdi.j2xx.interfaces.SpiMaster;

public class FT4222HSpiMaster implements SpiMaster {
    private Context mContext;
    private D2xxManager mManager;
    private FT_Device mDevice;
    private FT_4222_Device mFT4222Device;
    private FT_4222_Spi_Master mSpi;
    private SpiMasterListener mListener;
    private boolean listening = false;

    public FT4222HSpiMaster(Context context) {
        this(context, null);
    }

    public FT4222HSpiMaster(Context context, SpiMasterListener listener) {

        mContext = context.getApplicationContext();
        mListener = listener;
    }

    public void open() {
        if (mSpi == null && getManager()) {
            if (mManager.createDeviceInfoList(mContext) > 0) {
                mManager.setUsbRegisterBroadcast(false);
                IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
                mContext.registerReceiver(mUsbReceiver, filter);
                listening = true;
                mDevice = mManager.openByIndex(mContext, 0);
                mFT4222Device = new FT_4222_Device(mDevice);
                int status = mFT4222Device.init();
                status += mFT4222Device.setClock((byte) FT_4222_Defines.FT4222_ClockRate.SYS_CLK_24);
                mSpi = (FT_4222_Spi_Master) mFT4222Device.getSpiMasterDevice();
                if (status != FT_4222_Defines.FT4222_STATUS.FT4222_OK) {
                    mSpi = null;
                    mFT4222Device = null;
                    mDevice.close();
                    mDevice = null;
                }
                else {
                    if (mListener != null) {
                        mListener.onDeviceConnected();
                    }
                }
            }
        }
    }

    public void close() {
        if (listening) {
            mContext.unregisterReceiver(mUsbReceiver);
            listening = false;
        }
        if (mListener != null) {
            mListener.onDeviceDisconnected();
        }
        mSpi = null;
        mFT4222Device = null;
        if (mDevice != null) {
            mDevice.close();
            mDevice = null;
        }
    }

    private boolean getManager() {
        if (mManager == null) {
            try {
                mManager = D2xxManager.getInstance(mContext);
                mManager.setUsbRegisterBroadcast(false);
            } catch (D2xxManager.D2xxException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
            {
                close();
            }
        }
    };

    public void registerListener(SpiMasterListener listener) {
        mListener = listener;
    }

    public void unregisterListener(SpiMasterListener listener) {
        if(mListener == listener) {
            mListener = null;
        }
    }

    @Override
    public int init(int ioLine, int clock, int cpol, int cpha, byte ssoMap) {
        int result = FT_4222_Defines.FT4222_STATUS.FT4222_DEVICE_NOT_FOUND;
        if(mSpi != null) {
            result =  mSpi.init(ioLine, clock, cpol, cpha, ssoMap);
        }
        return result;
    }

    @Override
    public int reset() {
        int result = FT_4222_Defines.FT4222_STATUS.FT4222_DEVICE_NOT_FOUND;
        if(mSpi != null) {
            result = mSpi.reset();
        }
        return result;
    }

    @Override
    public int setLines(int spiMode) {
        int result = FT_4222_Defines.FT4222_STATUS.FT4222_DEVICE_NOT_FOUND;
        if(mSpi != null) {
            result = mSpi.setLines(spiMode);
        }
        return result;
    }

    @Override
    public int singleWrite(byte[] writeBuffer, int sizeToTransfer, int[] sizeTransferred,
                           boolean isEndTransaction) {
        int result = FT_4222_Defines.FT4222_STATUS.FT4222_DEVICE_NOT_FOUND;
        if(mSpi != null) {
            result = mSpi.singleWrite(writeBuffer, sizeToTransfer, sizeTransferred, isEndTransaction);
        }
        return result;
    }

    @Override
    public int singleRead(byte[] readBuffer, int sizeToTransfer, int[] sizeOfRead,
                          boolean isEndTransaction) {
        int result = FT_4222_Defines.FT4222_STATUS.FT4222_DEVICE_NOT_FOUND;
        if(mSpi != null) {
            result = mSpi.singleRead(readBuffer, sizeToTransfer, sizeOfRead, isEndTransaction);
        }
        return result;
    }

    @Override
    public int singleReadWrite(byte[] readBuffer, byte[] writeBuffer, int sizeToTransfer,
                               int[] sizeTransferred, boolean isEndTransaction) {
        int result = FT_4222_Defines.FT4222_STATUS.FT4222_DEVICE_NOT_FOUND;
        if(mSpi != null) {
            result = mSpi.singleReadWrite(readBuffer,  writeBuffer, sizeToTransfer, sizeTransferred,
                    isEndTransaction);
        }
        return result;
    }

    @Override
    public int multiReadWrite(byte[] readBuffer, byte[] writeBuffer, int singleWriteBytes,
                              int multiWriteBytes, int multiReadBytes, int[] sizeOfRead) {
        int result = FT_4222_Defines.FT4222_STATUS.FT4222_DEVICE_NOT_FOUND;
        if(mSpi != null) {
            result = mSpi.multiReadWrite(readBuffer, writeBuffer, singleWriteBytes, multiWriteBytes,
                    multiReadBytes, sizeOfRead);
        }
        return result;
    }

    public int setDrivingStrength(int clkStrength, int ioStrength, int ssoStregth) {
        int result = FT_4222_Defines.FT4222_STATUS.FT4222_DEVICE_NOT_FOUND;
        if(mSpi != null) {
            result = mSpi.setDrivingStrength(clkStrength, ioStrength, ssoStregth);
        }
        return result;
    }
}
