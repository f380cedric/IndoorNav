package com.example.francois.indoornav.ui.navigation;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.example.francois.indoornav.R;
import com.example.francois.indoornav.decawave.Dwm1000Master;

import java.util.ArrayList;

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
    private ArrayList<Marker> markerList = new ArrayList<>(7);
    private Marker user;
    private Bitmap anchorBitmap;

    IndoorMap(Context context ,int myScreenX, int myScreenY, int mapArrayId,
              int userIconId, int anchorIconId) {
        TypedArray mapArray = context.getResources().obtainTypedArray(mapArrayId);
        bitmap = BitmapFactory.decodeResource(context.getResources(),
                mapArray.getResourceId(0, R.drawable.blank_room));
        sizeMapX = bitmap.getWidth();
        sizeMapY = bitmap.getHeight();
        real2MapX = sizeMapX/mapArray.getFloat(1, 0);//1486.25;//1292f;//1290f;
        real2MapY = sizeMapY/mapArray.getFloat(2, 0);///1643.6;//1429f;//1347f;
        offsetX = mapArray.getInt(3, 0);
        offsetY = mapArray.getInt(4,0);
        Dwm1000Master.setAnchorsCoordinates(mapArray.getInt(5,0),
                mapArray.getInt(6,0),
                mapArray.getInt(7,0),
                mapArray.getInt(8,0),
                mapArray.getInt(9,0),
                mapArray.getInt(10,0),
                mapArray.getInt(11,0),
                mapArray.getInt(12,0),
                mapArray.getInt(13,0));
        mapArray.recycle();
        user = new Marker(BitmapFactory.decodeResource(context.getResources(),
                userIconId),-50,-50, 0);
        markerList.add(user);
        anchorBitmap = BitmapFactory.decodeResource(context.getResources(),
                anchorIconId);
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
        setUserPos(0,0);
        setAnchorsPositions(Dwm1000Master.getAnchorsCoordinates());
    }

    void panMap(double distanceX, double distanceY){
        mapPosX = (int)Math.max(0.f, Math.min(mapPosX + distanceX*width/maxWidth, sizeMapX - width));
        mapPosY = (int)Math.max(0.f, Math.min(mapPosY + distanceY*height/maxHeight, sizeMapY - height));
    }

    private double cmX2Pixel(double x){
        return (x+offsetX)*real2MapX;
    }

    private double cmY2Pixel(double y) {
        return sizeMapY - (y+offsetY)*real2MapY;
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

    private void setAnchorsPositions(int[] positions) {
        Marker marker;
        int i = 0;
        for(;i < markerList.size()-1; ++i) {
            marker = markerList.get(i+1);
            marker.setX(cmX2Pixel(positions[i*2]));
            marker.setY(cmY2Pixel(positions[i*2+1]));
        }
        for(; i*2 < positions.length; ++i) {
            markerList.add(new Marker(anchorBitmap, cmX2Pixel(positions[i*2]),
                    cmY2Pixel(positions[i*2+1]), 0));
        }
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

    void setUserPos(double posX, double posY) {
        user.setX(cmX2Pixel(posX));
        user.setY(cmY2Pixel(posY));
    }

    void setUserOrientation(double theta) {
        user.setTheta((int)theta);
    }

    ArrayList<Marker> getMarkerList() {
        return markerList;
    }

    class Marker {
        private final Bitmap icon;
        private double x;
        private double y;
        private double theta;
        private final double centerX;
        private final double centerY;
        Marker(Bitmap icon, double x, double y, double theta){
            this.icon = icon;
            this.x = x;
            this.y = y;
            this.theta = theta;
            centerX = icon.getWidth()/2.;
            centerY = icon.getHeight()/2.;
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
