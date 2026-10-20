package com.example.android.cricketscoreboard.views;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.example.android.cricketscoreboard.R;
import com.example.android.cricketscoreboard.UiPreferences;

/**
 * Welcome screen with one-tap entry to {@link LandingPageActivity}.
 * Respects the user's "Skip welcome screen on startup" preference in {@link UiPreferences}.
 */
public class LoadAppActivity extends AppCompatActivity {

    public void mainScreen(View view) {
        Intent intent = new Intent(LoadAppActivity.this, LandingPageActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (UiPreferences.get(this).isSkipSplashEnabled()) {
            startActivity(new Intent(this, LandingPageActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_loadapp);
    }
}
