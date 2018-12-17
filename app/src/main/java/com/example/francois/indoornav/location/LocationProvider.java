package com.example.francois.indoornav.location;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;

public class LocationProvider extends HandlerThread{
    private static final String TAG = "Location";
    private final Handler uiHandler;
    private volatile boolean update = true;
    private  Handler handler;
    private ILocationProvider mLocationProvider;
    private Runnable task = new Runnable() {
        @Override
        public void run() {
            if (update) {
                handler.postDelayed(task, 500);
                Message.obtain(uiHandler, mLocationProvider.updateLocation(),
                        mLocationProvider.getLastLocation()).sendToTarget();
            }
        }
    };
    public LocationProvider(Handler uiHandler, ILocationProvider locationProvider) {
        super(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        this.uiHandler = uiHandler;
        this.mLocationProvider = locationProvider;
        start();
        handler = new Handler(getLooper());
        handler.post(task);
    }

    public void onResume(){
        if(!update) {
            update = true;
            handler.post(task);
        }
    }

    public void onPause() {
        update = false;
        handler.removeCallbacksAndMessages(null);
    }

    void postMessage(Message msg){
        handler.sendMessage(msg);
    }
}
