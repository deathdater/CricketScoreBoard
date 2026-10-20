package com.example.android.cricketscoreboard;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Application entry point.
 *
 * Android 15 (API 35) and later force apps that target SDK 35+ to draw edge-to-edge, behind the
 * status and navigation bars. These legacy screens were designed with opaque system bars, so this
 * class pads every activity's content view by the system-bar, display-cutout and IME insets.
 * Screens that keep the decor action bar are additionally padded so their content starts below
 * the action bar, because AppCompat lays the content out underneath it when edge-to-edge.
 */
public class CricketScoreBoardApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                applyInsets(activity);
            }

            @Override public void onActivityCreated(@NonNull Activity a, @Nullable Bundle b) { }
            @Override public void onActivityResumed(@NonNull Activity a) { }
            @Override public void onActivityPaused(@NonNull Activity a) { }
            @Override public void onActivityStopped(@NonNull Activity a) { }
            @Override public void onActivitySaveInstanceState(@NonNull Activity a, @NonNull Bundle b) { }
            @Override public void onActivityDestroyed(@NonNull Activity a) { }
        });
    }

    private static void applyInsets(Activity activity) {
        final View content = activity.findViewById(android.R.id.content);
        if (content == null || content.getTag(R.id.edge_to_edge_applied) != null) {
            return;
        }
        content.setTag(R.id.edge_to_edge_applied, Boolean.TRUE);

        View decorActionBar = activity.getWindow().getDecorView()
                .findViewById(androidx.appcompat.R.id.action_bar);
        final boolean hasDecorActionBar = decorActionBar != null
                && decorActionBar.getVisibility() == View.VISIBLE;

        ViewCompat.setOnApplyWindowInsetsListener(content, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout()
                    | WindowInsetsCompat.Type.ime());
            int top = bars.top;
            if (hasDecorActionBar && decorActionBar.getHeight() > 0) {
                // AppCompat lays the content out full-screen beneath the decor action bar, so
                // make sure the content starts below it (status bar + action bar).
                int[] loc = new int[2];
                decorActionBar.getLocationInWindow(loc);
                top = Math.max(top, loc[1] + decorActionBar.getHeight());
            }
            v.setPadding(bars.left, top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(content);
        if (hasDecorActionBar) {
            // Re-apply once the action bar has been measured/positioned.
            decorActionBar.addOnLayoutChangeListener((ab, l, t, r, b, ol, ot, or, ob) -> {
                if (b - t != ob - ot || t != ot) {
                    ViewCompat.requestApplyInsets(content);
                }
            });
        }
    }
}
