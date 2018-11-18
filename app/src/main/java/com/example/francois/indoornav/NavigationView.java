package com.example.francois.indoornav;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class NavigationView extends SurfaceView implements Runnable
{
    volatile boolean running;
    private Thread navigationThread = null;
    private int screenX;
    private int screenY;
    private Canvas canvas;
    private SurfaceHolder surfaceHolder;
    private Paint paint;
    private IndoorMap indoorMap;
    private int initialX;
    private int initialY;
    private int currentX;
    private int currentY;


    public NavigationView(Context context, int myScreenX, int myScreenY) {
        super(context);
        screenX = myScreenX;
        screenY = myScreenY;
        surfaceHolder = getHolder();
        paint = new Paint();
        indoorMap = new IndoorMap(context, myScreenX, myScreenY);
    }

    @Override
    public void run() {
        while (running) {
            //to update the frame
            update();
            //to draw the frame
            draw();
            //to control
            control();
        }
    }

    public void update(){}

    public void draw() {
        if (surfaceHolder.getSurface().isValid()) {
            canvas = surfaceHolder.lockCanvas();
            // Draw background
            canvas.drawColor(Color.WHITE);
            // Draw map
            Rect src = new Rect(indoorMap.getMapPosX(),
                    indoorMap.getMapPosY(),
                    indoorMap.getWidth(),
                    indoorMap.getHeight());
            Rect dst = new Rect(0,0, screenX , screenY);
            canvas.drawBitmap(indoorMap.getBitmap(), src, dst, null);
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    public void control(){}

    public void pause()
    {
        running = false;
        try {
            navigationThread.join();
        } catch (InterruptedException e) {
        }
    }

    public void resume() {
        running = true;
        navigationThread = new Thread(this);
        navigationThread.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                // when screen is touched
                initialX = (int)motionEvent.getX();
                initialY = (int)motionEvent.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                currentX = (int)motionEvent.getX();
                currentY = (int)motionEvent.getY();
                indoorMap.moveMap(initialX,initialY,currentX,currentY);
                break;
            case MotionEvent.ACTION_UP:
                // when screen is released
                indoorMap.setPreviousMapXY();
                break;
        }
        return true;
    }


}
