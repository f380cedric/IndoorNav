package com.example.francois.indoornav;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

class NavigationView extends SurfaceView implements Runnable, SurfaceHolder.Callback
{
    private volatile boolean running;
    private Thread navigationThread = null;
    private int maxWidth;
    private int maxHeight;
    private SurfaceHolder surfaceHolder;
    private IndoorMap indoorMap;

    private Rect src = new Rect();
    private RectF dst = new RectF();

    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mDetector;

    public NavigationView(Context context, int mScreenX, int mScreenY) {
        super(context);
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        indoorMap = new IndoorMap(context, mScreenX, mScreenY);
        maxWidth = indoorMap.getMaxWidth();
        maxHeight = indoorMap.getMaxHeight();
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mDetector = new GestureDetector(context, new GestureListener());
    }

    void setPositions(double[] positions) {
        Log.d("Location received", positions[0] + ", " + positions[1]);
        indoorMap.setMarkerPos((float)positions[0], (float)positions[1]);
    }

    void setOrientation(double orientation) {
        indoorMap.setMarkerOrientation((float)orientation);
    }

    @Override
    public void run() {
        while (running) {
            //to draw the frame
            draw();
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                running = false;
            }
        }
    }

    private void draw() {
        Canvas canvas = surfaceHolder.lockCanvas();
        if(canvas != null) {
            canvas.drawColor(Color.WHITE);
            // Draw map
            int x = indoorMap.getMapPosX();
            int y = indoorMap.getMapPosY();
            int w = (int)indoorMap.getWidth();
            int h = (int)indoorMap.getHeight() ;
            float sX = indoorMap.getMapScaleX();
            float sY = indoorMap.getMapScaleY();
            IndoorMap.Marker marker = indoorMap.getMarker();
            float markerCenterX = marker.getCenterX();
            float markerCenterY = marker.getCenterY();
            float markerOrientation = marker.getTheta()-45;
            float markerX = (marker.getX()-x)/sX- markerCenterX;
            float markerY = (marker.getY()-y)/sY- markerCenterY;
            src.set(x, y,w + x,h + y);
            dst.set(0,0, maxWidth , maxHeight);
            canvas.drawBitmap(indoorMap.getBitmap(), src, dst, null);

            if (dst.contains(markerX, markerY)) {
                Matrix matrix = new Matrix();
                matrix.setRotate(markerOrientation, markerCenterX, markerCenterY);
                matrix.postTranslate(markerX, markerY);
                canvas.drawBitmap(marker.getIcon(), matrix, null);
            }
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean retVal = mScaleDetector.onTouchEvent(motionEvent);
        retVal = mDetector.onTouchEvent(motionEvent) || retVal;
        return retVal || super.onTouchEvent(motionEvent);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        navigationThread = new Thread(this);
        navigationThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        try {
            navigationThread.join();
        } catch (InterruptedException e) {
            navigationThread.interrupt();
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            indoorMap.panMap(distanceX, distanceY);
            return true;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            indoorMap.scaleAndFocusMap(detector.getScaleFactor(), detector.getFocusX(),
                    detector.getFocusY());
            return true;
        }
    }
}
