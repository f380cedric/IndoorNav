package com.example.francois.indoornav;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class IndoorMap {

    private Bitmap bitmap;
    private int sizeMapX;
    private int sizeMapY;
    private int screenX;
    private int screenY;
    private int mapPosX;
    private int mapPosY;
    private int previousMapPosX;
    private int previousMapPosY;

    public IndoorMap(Context context, int myScreenX, int myScreenY){
        screenX = myScreenX;
        screenY = myScreenY;
        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.map_ua5);
        sizeMapX = bitmap.getWidth();
        sizeMapY = bitmap.getHeight();
        mapPosX = 0;
        mapPosY = 0;
    }

    public void moveMap(int initX, int initY, int currentX, int currentY){
        mapPosX = previousMapPosX-(currentX-initX);
        if (mapPosX<0){
            mapPosX = 0;
        }
        if (mapPosX>sizeMapX-screenX){
            mapPosX = sizeMapX-screenX;
        }
        mapPosY = previousMapPosY-(currentY-initY);
        if (mapPosY<0){
            mapPosY = 0;
        }
        if (mapPosY>sizeMapY-screenY){
            mapPosY = sizeMapY-screenY;
        }
    }

    public void setPreviousMapXY(){
        previousMapPosX = mapPosX;
        previousMapPosY = mapPosY;
    }


    public Bitmap getBitmap(){
        return bitmap;
    }

    public int getMapPosX(){
        return mapPosX;
    }

    public int getMapPosY(){
        return mapPosY;
    }

    public int getBitMapWidth(){
        return sizeMapX;
    }

    public int getBitMapHeight(){
        return sizeMapY;
    }

}
