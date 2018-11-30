package com.example.francois.indoornav.ui.navigation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.francois.indoornav.util.PointD;

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


    public NavigationView(Context context) {
        super(context);
        initSurface();
    }

    public NavigationView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initSurface();
    }

    public NavigationView(Context context, AttributeSet attributeSet, int integer) {
        super(context, attributeSet, integer);
        initSurface();

    }

    private void initSurface() {
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
    }

    private void initMap(Context context, int width, int height) {
        indoorMap = new IndoorMap(context, width, height);
        maxWidth = indoorMap.getMaxWidth();
        maxHeight = indoorMap.getMaxHeight();
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mDetector = new GestureDetector(context, new GestureListener());
    }

    void setPositions(PointD positions) {
        Log.d("Location received", positions.x + ", " + positions.y);
        if (indoorMap != null) {
            indoorMap.setMarkerPos(positions.x, positions.y);
        }
    }

    void setOrientation(double orientation) {
        if (indoorMap != null) {
            indoorMap.setMarkerOrientation(orientation);
        }
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
            double sX = indoorMap.getMapScaleX();
            double sY = indoorMap.getMapScaleY();
            IndoorMap.Marker marker = indoorMap.getMarker();
            double markerCenterX = marker.getCenterX();
            double markerCenterY = marker.getCenterY();
            double markerOrientation = marker.getTheta()-45;
            double markerX = (marker.getX()-x)/sX- markerCenterX;
            double markerY = (marker.getY()-y)/sY- markerCenterY;
            src.set(x, y,w + x,h + y);
            dst.set(0,0, maxWidth , maxHeight);
            canvas.drawBitmap(indoorMap.getBitmap(), src, dst, null);

            if (dst.contains((float)markerX, (float)markerY)) {
                Matrix matrix = new Matrix();
                matrix.setRotate((float)markerOrientation, (float)markerCenterX,
                        (float)markerCenterY);
                matrix.postTranslate((float)markerX, (float)markerY);
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
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        initMap(getContext(), MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec));
        setMeasuredDimension(maxWidth, maxHeight);
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
