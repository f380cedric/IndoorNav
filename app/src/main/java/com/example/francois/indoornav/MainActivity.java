package com.example.francois.indoornav;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    public void startNavigation(View view) {
        startActivity(new Intent(this, NavigationActivity.class));
    }

    public void startTestDwm1000(View view){
        startActivity(new Intent(this, TestDwm1000Activity.class));
    }

    public void startTestOrientation(View view) {
        startActivity(new Intent(this, TestOrientationActivity.class));
    }


}
