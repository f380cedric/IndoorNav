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
    private int previousMapPosX;
    private int previousMapPosY;

    IndoorMap(Context context, int myScreenX, int myScreenY){
        screenX = myScreenX;
        screenY = myScreenY;
        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.dessin);
        sizeMapX = bitmap.getWidth();
        sizeMapY = bitmap.getHeight();
        width = sizeMapX;
        height = sizeMapY;
        mapPosX = 0;
        mapPosY = 0;
    }

    void moveMap(int initX, int initY, int currentX, int currentY){
        mapPosX = previousMapPosX-(currentX-initX);
        if (mapPosX<0){
            mapPosX = 0;
        }
        if (width + mapPosX > sizeMapX){
            mapPosX = sizeMapX - width;
        }
        mapPosY = previousMapPosY-(currentY-initY);
        if (mapPosY<0){
            mapPosY = 0;
        }
        if (height + mapPosY > sizeMapY){
            mapPosY = sizeMapY - height;
        }
    }

    void setPreviousMapXY(){
        previousMapPosX = mapPosX;
        previousMapPosY = mapPosY;
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
}
