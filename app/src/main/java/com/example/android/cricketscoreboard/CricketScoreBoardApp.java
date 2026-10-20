package com.example.android.cricketscoreboard;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.android.cricketscoreboard.views.GroundDetailsActivity;
import com.example.android.cricketscoreboard.views.LandingPageActivity;
import com.example.android.cricketscoreboard.views.LoadAppActivity;
import com.example.android.cricketscoreboard.views.PlayerDetailsActivity;
import com.example.android.cricketscoreboard.views.ScheduleMatches;
import com.example.android.cricketscoreboard.views.ScorerPadActivity;
import com.example.android.cricketscoreboard.views.ScorerTabActivity;
import com.example.android.cricketscoreboard.views.TeamDetails;
import com.example.android.cricketscoreboard.views.ViewGrounds;
import com.example.android.cricketscoreboard.views.ViewPlayers;
import com.example.android.cricketscoreboard.views.ViewSchedules;
import com.example.android.cricketscoreboard.views.ViewTeams;

/**
 * Application entry point.
 *
 * <ul>
 *   <li>Applies saved user-interface customization preferences ({@link UiPreferences}) including
 *       DayNight theme mode, accent palette overlay, and layout density.</li>
 *   <li>Configures descriptive ActionBar titles and Up navigation on child screens.</li>
 *   <li>Pads every activity's content view for Android 15+ (API 35+) edge-to-edge system bars,
 *       display cutouts, and IME insets.</li>
 * </ul>
 */
public class CricketScoreBoardApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        UiPreferences.get(this).applySavedNightMode();

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityPreCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                applyAccentOverlay(activity);
            }

            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                applyAccentOverlay(activity);
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                configureActionBar(activity);
                applyInsetsAndDensity(activity);
            }

            @Override public void onActivityResumed(@NonNull Activity a) { }
            @Override public void onActivityPaused(@NonNull Activity a) { }
            @Override public void onActivityStopped(@NonNull Activity a) { }
            @Override public void onActivitySaveInstanceState(@NonNull Activity a, @Nullable Bundle b) { }
            @Override public void onActivityDestroyed(@NonNull Activity a) { }
        });
    }

    private static void applyAccentOverlay(@NonNull Activity activity) {
        UiPreferences.AccentPalette palette = UiPreferences.get(activity).getAccentPalette();
        activity.getTheme().applyStyle(palette.overlayResId, true);
    }

    private static void configureActionBar(@NonNull Activity activity) {
        if (!(activity instanceof AppCompatActivity)) {
            return;
        }
        AppCompatActivity compat = (AppCompatActivity) activity;
        ActionBar ab = compat.getSupportActionBar();
        if (ab == null) {
            return;
        }
        if (activity instanceof LoadAppActivity) {
            ab.hide();
            return;
        }
        String title = resolveScreenTitle(activity);
        if (title != null) {
            ab.setTitle(title);
        }
        boolean isChildScreen = !(activity instanceof LandingPageActivity)
                && !(activity instanceof ScorerPadActivity);
        if (isChildScreen) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Nullable
    private static String resolveScreenTitle(@NonNull Activity activity) {
        if (activity instanceof LandingPageActivity) {
            return "Cricket Score Board";
        } else if (activity instanceof TeamDetails) {
            return "New Team";
        } else if (activity instanceof ViewTeams) {
            return "Teams & Squads";
        } else if (activity instanceof PlayerDetailsActivity) {
            return "New Player";
        } else if (activity instanceof ViewPlayers) {
            return "Player Profiles & Stats";
        } else if (activity instanceof GroundDetailsActivity) {
            return "New Ground";
        } else if (activity instanceof ViewGrounds) {
            return "Grounds Directory";
        } else if (activity instanceof ScheduleMatches) {
            return "Schedule Match";
        } else if (activity instanceof ViewSchedules) {
            return "Match Schedules";
        } else if (activity instanceof ScorerPadActivity) {
            return "Live Scorer Pad";
        } else if (activity instanceof ScorerTabActivity) {
            return "Match Center";
        }
        return null;
    }

    private static void applyInsetsAndDensity(Activity activity) {
        final View content = activity.findViewById(android.R.id.content);
        if (content == null || content.getTag(R.id.edge_to_edge_applied) != null) {
            return;
        }
        content.setTag(R.id.edge_to_edge_applied, Boolean.TRUE);

        // Apply layout density scaling to the root child's padding if user chose Comfortable density.
        if (content instanceof ViewGroup && ((ViewGroup) content).getChildCount() > 0) {
            View rootChild = ((ViewGroup) content).getChildAt(0);
            float scale = UiPreferences.get(activity).getLayoutDensity().paddingScale;
            if (scale != 1.0f && rootChild != null) {
                rootChild.setPadding(
                        Math.round(rootChild.getPaddingLeft() * scale),
                        Math.round(rootChild.getPaddingTop() * scale),
                        Math.round(rootChild.getPaddingRight() * scale),
                        Math.round(rootChild.getPaddingBottom() * scale));
            }
        }

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
                int[] loc = new int[2];
                decorActionBar.getLocationInWindow(loc);
                top = Math.max(top, loc[1] + decorActionBar.getHeight());
            }
            v.setPadding(bars.left, top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(content);
        if (hasDecorActionBar) {
            decorActionBar.addOnLayoutChangeListener((ab, l, t, r, b, ol, ot, or, ob) -> {
                if (b - t != ob - ot || t != ot) {
                    ViewCompat.requestApplyInsets(content);
                }
            });
        }
    }
}
