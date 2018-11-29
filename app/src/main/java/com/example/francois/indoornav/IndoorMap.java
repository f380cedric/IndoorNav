package com.example.francois.indoornav;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

class IndoorMap {

    private final Bitmap bitmap;
    private final int sizeMapX;
    private final int sizeMapY;
    private float width;
    private float height;
    private final int screenX;
    private final int screenY;
    private final int maxWidth;
    private final int maxHeight;
    private int mapPosX;
    private int mapPosY;
    private float scaleFactor;
    private final float real2MapX;
    private final float real2MapY;
    private Marker marker;

    IndoorMap(Context context, int myScreenX, int myScreenY) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.little_room2, options);
        screenX = myScreenX;
        screenY = myScreenY;
        sizeMapX = bitmap.getWidth();
        sizeMapY = bitmap.getHeight();
        real2MapX = sizeMapX/1292f;//1290f;
        real2MapY = sizeMapY/1429f;//1347f;
        marker = new Marker(BitmapFactory.decodeResource(context.getResources(),
                R.drawable.ic_location_arrow),-50,-50, 0);
        width = sizeMapX;
        height = sizeMapY;
        float ratioScreen = myScreenX/(float)myScreenY;
        float ratioBitmap = sizeMapX/(float)sizeMapY;
        if(ratioScreen > ratioBitmap) {
            maxHeight = screenY;
            maxWidth = (int)(screenY*ratioBitmap);
        } else {
            maxWidth = screenX;
            maxHeight = (int)(screenX/ratioBitmap);
        }
        mapPosX = 0;
        mapPosY = 0;
        setMarkerPos(0,0);
    }

    void panMap(float distanceX, float distanceY){
        mapPosX = (int)Math.max(0.f, Math.min(mapPosX + distanceX*width/maxWidth, sizeMapX - width));
        mapPosY = (int)Math.max(0.f, Math.min(mapPosY + distanceY*height/maxHeight, sizeMapY - height));
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
        moveMap(mapPosX+(focusX*oldWidth-focusX*width)/maxWidth,
                mapPosY+(focusY*oldHeight-focusY*height)/maxHeight);
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

    int getMaxHeight() {
        return maxHeight;
    }

    int getMaxWidth() {
        return maxWidth;
    }

    float getWidth() {
        return width;
    }

    float getHeight() {
        return height;
    }

    float getMapScaleX() {
        return width/maxWidth;
    }

    float getMapScaleY() {
        return height/maxHeight;
    }

    void setMarkerPos(float posX, float posY) {
        marker.setX((posX+70)*real2MapX);
        marker.setY(sizeMapY - (posY+102)*real2MapY);
    }

    void setMarkerOrientation(float theta) {
        marker.setTheta((int)theta);
    }

    Marker getMarker() {
        return marker;
    }

    class Marker {
        private final Bitmap icon;
        private float x;
        private float y;
        private float theta;
        private final float centerX;
        private final float centerY;
        Marker(Bitmap icon, int x, int y, int theta){
            this.icon = icon;
            this.x = x;
            this.y = y;
            this.theta = theta;
            centerX = icon.getWidth()/2;
            centerY = icon.getHeight()/2;
        }

        Bitmap getIcon() {
            return icon;
        }

        float getX() {
            return x;
        }

        float getY() {
            return y;
        }

        float getTheta() {
            return theta;
        }

        float getCenterX() {
            return centerX;
        }

        float getCenterY() {
            return centerY;
        }

        private void setX(float x) {
            this.x = x;
        }

        private void setY(float y) {
            this.y = y;
        }

        private void setTheta(float theta) {
            this.theta = theta;
        }
    }
}
