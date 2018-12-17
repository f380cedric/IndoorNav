package com.example.francois.indoornav.spi;

public interface SpiMasterListener {
    void onDeviceConnected();
    void onDeviceDisconnected();
    void onDataFailure(int status);
}
