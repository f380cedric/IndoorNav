package com.example.francois.indoornav;

public interface FT311SPIMasterListener {
    void onDeviceConnected();
    void onDeviceDisconnected();
    void onDataFailure(int status);
}
