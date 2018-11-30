package com.example.francois.indoornav.spi;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.example.francois.indoornav.ui.MainActivity;

public class USBIntentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isTaskRoot()) {
            startActivity(new Intent(this,MainActivity.class));
        }
        finish();
    }
}

