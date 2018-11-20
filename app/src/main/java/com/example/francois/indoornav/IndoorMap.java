package com.example.francois.indoornav;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

class IndoorMap {

    private Bitmap bitmap;
    private int sizeMapX;
    private int sizeMapY;
    private int width;
    private int height;
    private int screenX;
    private int screenY;
    private int mapPosX;
    private int mapPosY;
    private float scaleFactor;
    private Marker marker;

    IndoorMap(Context context, int myScreenX, int myScreenY) {
        screenX = myScreenX;
        screenY = myScreenY;
        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.map_ua5_2);
        marker = new Marker(BitmapFactory.decodeResource(context.getResources(),
                R.drawable.map_ua5),
                0,0);
        sizeMapX = bitmap.getWidth();
        sizeMapY = bitmap.getHeight();
        width = sizeMapX;
        height = sizeMapY;
        mapPosX = 0;
        mapPosY = 0;
    }

    void panMap(int distanceX, int distanceY){
        mapPosX = Math.max(0, Math.min(mapPosX + distanceX*width/screenX, sizeMapX - width));
        mapPosY = Math.max(0, Math.min(mapPosY + distanceY*height/screenY, sizeMapY - height));
    }

    void moveMap(int newMapPosX, int newMapPosY){
        mapPosX = Math.max(0, Math.min(newMapPosX, sizeMapX - width));
        mapPosY = Math.max(0, Math.min(newMapPosY, sizeMapY - height));
    }

    void scaleAndFocusMap(float newScaleFactor, int focusX, int focusY) {
        scaleFactor = Math.max(1.f, Math.min(scaleFactor * newScaleFactor, 5.f));
        int oldWidth = width;
        int oldHeight = height;
        width = (int)(sizeMapX / scaleFactor);
        height = (int)(sizeMapY / scaleFactor);
        moveMap(mapPosX+(focusX*oldWidth-focusX*width)/screenX,
                mapPosY+(focusY*oldHeight-focusY*height)/screenY);
    }


    Bitmap getBitmap(){
        return bitmap;
    }

    int getMapPosX(){
        return mapPosX;
    }

    int getMapPosY(){
        return mapPosY;
    }

    int getBitMapWidth(){
        return sizeMapX;
    }

    int getBitMapHeight(){
        return sizeMapY;
    }

    int getWidth() {
        return width;
    }

    int getHeight() {
        return height;
    }

    Marker getMarker() {
        return marker;
    }

    class Marker {
        private Bitmap mIcon;
        private int mX;
        private int mY;
        Marker(Bitmap icon, int x, int y){
            mIcon = icon;
            mX = x;
            mY = y;
        }

        Bitmap getIcon() {
            return mIcon;
        }

        int getX() {
            return mX;
        }

        int getY() {
            return mY;
        }
    }
}
