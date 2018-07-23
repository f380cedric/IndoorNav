package com.example.francois.indoornav;

import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;

public class NavigationActivity extends AppCompatActivity {

    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        navigationView = new NavigationView(this, size.x, size.y);
        setContentView(navigationView);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onPause(){
        super.onPause();
        navigationView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        navigationView.resume();
    }

}
