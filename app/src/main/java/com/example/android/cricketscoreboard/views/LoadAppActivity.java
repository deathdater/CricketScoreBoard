package com.example.android.cricketscoreboard.views;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.example.android.cricketscoreboard.R;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class LoadAppActivity extends AppCompatActivity {



    public void mainScreen(View view)
    {
        Intent intent = new Intent(LoadAppActivity.this, LandingPageActivity.class);
        startActivity(intent);

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_loadapp);

    }}

