package com.example.francois.indoornav;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

class IndoorMap {

    private Bitmap bitmap;
    private int sizeMapX;
    private int sizeMapY;
    private float width;
    private float height;
    private int screenX;
    private int screenY;
    private int mapPosX;
    private int mapPosY;
    private float scaleFactor;
    private float real2MapX;
    private float real2MapY;
    private Marker marker;

    IndoorMap(Context context, int myScreenX, int myScreenY) {
        screenX = myScreenX;
        screenY = myScreenY;
        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.map_ua5_2);
        sizeMapX = bitmap.getWidth();
        sizeMapY = bitmap.getHeight();
        real2MapX = sizeMapX/940f;
        real2MapY = sizeMapY/1290f;
        marker = new Marker(BitmapFactory.decodeResource(context.getResources(),
                R.drawable.ic_location_marker),sizeMapX-100,sizeMapY-200);
        setMarkerPos(470,645);
        width = sizeMapX;
        height = sizeMapY;
        mapPosX = 0;
        mapPosY = 0;
    }

    void panMap(float distanceX, float distanceY){
        mapPosX = (int)Math.max(0.f, Math.min(mapPosX + distanceX*width/screenX, sizeMapX - width));
        mapPosY = (int)Math.max(0.f, Math.min(mapPosY + distanceY*height/screenY, sizeMapY - height));
    }

    void moveMap(float newMapPosX, float newMapPosY){
        mapPosX = (int)Math.max(0.f, Math.min(newMapPosX, sizeMapX - width));
        mapPosY = (int)Math.max(0.f, Math.min(newMapPosY, sizeMapY - height));
    }

    void scaleAndFocusMap(float newScaleFactor, float focusX, float focusY) {
        scaleFactor = Math.max(1.f, Math.min(scaleFactor * newScaleFactor, 5.f));
        float oldWidth = width;
        float oldHeight = height;
        width = (sizeMapX / scaleFactor);
        height = (sizeMapY / scaleFactor);
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

    float getWidth() {
        return width;
    }

    float getHeight() {
        return height;
    }

    float getMapScaleX() {
        return width/screenX;
    }

    float getMapScaleY() {
        return height/screenY;
    }

    void setMarkerPos(float posX, float posY) {
        marker.setX((int)(posX*real2MapX));
        marker.setY((int)(posY*real2MapY));
    }

    Marker getMarker() {
        return marker;
    }

    class Marker {
        private Bitmap icon;
        private int x;
        private int y;
        Marker(Bitmap icon, int x, int y){
            this.icon = icon;
            this.x = x;
            this.y = y;
        }

        Bitmap getIcon() {
            return icon;
        }

        int getX() {
            return x;
        }

        int getY() {
            return y;
        }

        private void setIcon(Bitmap icon) {
            this.icon = icon;
        }

        private void setX(int x) {
            this.x = x;
        }

        private void setY(int y) {
            this.y = y;
        }
    }
}
