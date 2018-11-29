package com.example.francois.indoornav;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;

class Location extends HandlerThread {
    private static final String TAG = "Location";
    static final int SUCCESS = 1;
    static final int IOERROR = 2;
    private final Handler uiHandler;
    private volatile boolean update = true;
    private  Handler handler;
    private Dwm1000Master device;
    private Runnable task = new Runnable() {
        @Override
        public void run() {
            if (update) {
                handler.postDelayed(task, 500);
                Message.obtain(uiHandler, SUCCESS, device.updateCoordinates()).sendToTarget();
            }
        }
    };
    Location(Handler uiHandler, Dwm1000Master device) {
        super(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        this.uiHandler = uiHandler;
        this.device = device;
        start();
        handler = new Handler(getLooper());
        handler.post(task);
    }

    void onResume(){
        if(!update) {
            update = true;
            handler.post(task);
        }
    }

    void onPause() {
        update = false;
    }

    void postMessage(Message msg){
        handler.sendMessage(msg);
    }
}
