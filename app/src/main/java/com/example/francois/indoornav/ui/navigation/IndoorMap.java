package com.example.francois.indoornav.ui.navigation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;

import com.example.francois.indoornav.R;

class IndoorMap {
    private final Bitmap  bitmap;
    private final int sizeMapX;
    private final int sizeMapY;
    private double width;
    private double height;
    private final int maxWidth;
    private final int maxHeight;
    private int mapPosX;
    private int mapPosY;
    private double scaleFactor;
    private final double real2MapX;
    private final double real2MapY;
    private final double offsetX;
    private final double offsetY;
    private Marker marker;

    IndoorMap(Context context, int myScreenX, int myScreenY) {
        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.little_room);
        sizeMapX = bitmap.getWidth();
        sizeMapY = bitmap.getHeight();
        real2MapX = sizeMapX/1486.25;//1292f;//1290f;
        real2MapY = sizeMapY/1643.6;//1429f;//1347f;
        offsetX = 63;
        offsetY = 118;
        marker = new Marker(BitmapFactory.decodeResource(context.getResources(),
                R.drawable.ic_location_arrow),-50,-50, 0);
        width = sizeMapX;
        height = sizeMapY;
        double ratioScreen = myScreenX/(double)myScreenY;
        double ratioBitmap = sizeMapX/(double)sizeMapY;
        if(ratioScreen > ratioBitmap) {
            maxHeight = myScreenY;
            maxWidth = (int)(myScreenY *ratioBitmap);
        } else {
            maxWidth = myScreenX;
            maxHeight = (int)(myScreenX /ratioBitmap);
        }
        mapPosX = 0;
        mapPosY = 0;
        setMarkerPos(0,0);
    }

    void panMap(double distanceX, double distanceY){
        mapPosX = (int)Math.max(0.f, Math.min(mapPosX + distanceX*width/maxWidth, sizeMapX - width));
        mapPosY = (int)Math.max(0.f, Math.min(mapPosY + distanceY*height/maxHeight, sizeMapY - height));
    }

    private void moveMap(double newMapPosX, double newMapPosY){
        mapPosX = (int)Math.max(0.f, Math.min(newMapPosX, sizeMapX - width));
        mapPosY = (int)Math.max(0.f, Math.min(newMapPosY, sizeMapY - height));
    }

    void scaleAndFocusMap(double newScaleFactor, double focusX, double focusY) {
        scaleFactor = Math.max(1.f, Math.min(scaleFactor * newScaleFactor, 5.f));
        double oldWidth = width;
        double oldHeight = height;
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

    int getMaxHeight() {
        return maxHeight;
    }

    int getMaxWidth() {
        return maxWidth;
    }

    double getWidth() {
        return width;
    }

    double getHeight() {
        return height;
    }

    double getMapScaleX() {
        return width/maxWidth;
    }

    double getMapScaleY() {
        return height/maxHeight;
    }

    void setMarkerPos(double posX, double posY) {
        marker.setX((posX+offsetX)*real2MapX);
        marker.setY(sizeMapY - (posY+offsetY)*real2MapY);
    }

    void setMarkerOrientation(double theta) {
        marker.setTheta((int)theta);
    }

    Marker getMarker() {
        return marker;
    }

    class Marker {
        private final Bitmap icon;
        private double x;
        private double y;
        private double theta;
        private final double centerX;
        private final double centerY;
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

        double getX() {
            return x;
        }

        double getY() {
            return y;
        }

        double getTheta() {
            return theta;
        }

        double getCenterX() {
            return centerX;
        }

        double getCenterY() {
            return centerY;
        }

        private void setX(double x) {
            this.x = x;
        }

        private void setY(double y) {
            this.y = y;
        }

        private void setTheta(double theta) {
            this.theta = theta;
        }
    }
}
