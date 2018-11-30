package com.example.francois.indoornav.spi;

public interface FT311SPIMasterListener {
    void onDeviceConnected();
    void onDeviceDisconnected();
    void onDataFailure(int status);
}
