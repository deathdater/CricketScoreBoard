package com.example.android.cricketscoreboard;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Centralized, typed manager for persistent user-interface customization preferences.
 *
 * <p>Stored in a dedicated {@link SharedPreferences} file ({@link #PREFS_NAME}) so UI
 * personalization remains strictly separated from operational match/team/player data in
 * {@link DatabaseHandler}.
 */
public final class UiPreferences {

    public static final String PREFS_NAME = "cricket_ui_preferences";

    public static final String KEY_THEME_MODE = "theme_mode";
    public static final String KEY_ACCENT_PALETTE = "accent_palette";
    public static final String KEY_LAYOUT_DENSITY = "layout_density";
    public static final String KEY_SCORER_HAPTICS = "scorer_haptics";
    public static final String KEY_SHOW_BALL_SNACKBAR = "show_ball_snackbar";
    public static final String KEY_SKIP_SPLASH = "skip_splash";

    public enum ThemeMode {
        SYSTEM("system", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
        LIGHT("light", AppCompatDelegate.MODE_NIGHT_NO),
        DARK("dark", AppCompatDelegate.MODE_NIGHT_YES);

        public final String id;
        public final int nightMode;

        ThemeMode(String id, int nightMode) {
            this.id = id;
            this.nightMode = nightMode;
        }

        @NonNull
        public static ThemeMode fromId(String id) {
            for (ThemeMode mode : values()) {
                if (mode.id.equalsIgnoreCase(id)) {
                    return mode;
                }
            }
            return SYSTEM;
        }
    }

    public enum AccentPalette {
        TURF_GREEN("turf_green", R.style.ThemeOverlay_Cricket_Accent_TurfGreen),
        STADIUM_BLUE("stadium_blue", R.style.ThemeOverlay_Cricket_Accent_StadiumBlue),
        PAVILION_GOLD("pavilion_gold", R.style.ThemeOverlay_Cricket_Accent_PavilionGold);

        public final String id;
        @StyleRes
        public final int overlayResId;

        AccentPalette(String id, @StyleRes int overlayResId) {
            this.id = id;
            this.overlayResId = overlayResId;
        }

        @NonNull
        public static AccentPalette fromId(String id) {
            for (AccentPalette palette : values()) {
                if (palette.id.equalsIgnoreCase(id)) {
                    return palette;
                }
            }
            return TURF_GREEN;
        }
    }

    public enum LayoutDensity {
        STANDARD("standard", 1.0f),
        COMFORTABLE("comfortable", 1.35f);

        public final String id;
        public final float paddingScale;

        LayoutDensity(String id, float paddingScale) {
            this.id = id;
            this.paddingScale = paddingScale;
        }

        @NonNull
        public static LayoutDensity fromId(String id) {
            for (LayoutDensity density : values()) {
                if (density.id.equalsIgnoreCase(id)) {
                    return density;
                }
            }
            return STANDARD;
        }
    }

    private final SharedPreferences prefs;

    private UiPreferences(@NonNull Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    public static UiPreferences get(@NonNull Context context) {
        return new UiPreferences(context);
    }

    @NonNull
    public ThemeMode getThemeMode() {
        return ThemeMode.fromId(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.id));
    }

    public void setThemeMode(@NonNull ThemeMode mode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.id).apply();
        applyNightMode(mode);
    }

    @NonNull
    public AccentPalette getAccentPalette() {
        return AccentPalette.fromId(prefs.getString(KEY_ACCENT_PALETTE, AccentPalette.TURF_GREEN.id));
    }

    public void setAccentPalette(@NonNull AccentPalette palette) {
        prefs.edit().putString(KEY_ACCENT_PALETTE, palette.id).apply();
    }

    @NonNull
    public LayoutDensity getLayoutDensity() {
        return LayoutDensity.fromId(prefs.getString(KEY_LAYOUT_DENSITY, LayoutDensity.STANDARD.id));
    }

    public void setLayoutDensity(@NonNull LayoutDensity density) {
        prefs.edit().putString(KEY_LAYOUT_DENSITY, density.id).apply();
    }

    public boolean isScorerHapticsEnabled() {
        return prefs.getBoolean(KEY_SCORER_HAPTICS, true);
    }

    public void setScorerHapticsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SCORER_HAPTICS, enabled).apply();
    }

    public boolean isShowBallSnackbarEnabled() {
        return prefs.getBoolean(KEY_SHOW_BALL_SNACKBAR, true);
    }

    public void setShowBallSnackbarEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SHOW_BALL_SNACKBAR, enabled).apply();
    }

    public boolean isSkipSplashEnabled() {
        return prefs.getBoolean(KEY_SKIP_SPLASH, false);
    }

    public void setSkipSplashEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SKIP_SPLASH, enabled).apply();
    }

    /**
     * Resets all customization preferences back to their defaults and applies the default theme.
     */
    public void resetToDefaults() {
        prefs.edit().clear().apply();
        applyNightMode(ThemeMode.SYSTEM);
    }

    /**
     * Applies the saved night-mode setting via {@link AppCompatDelegate}.
     */
    public void applySavedNightMode() {
        applyNightMode(getThemeMode());
    }

    private static void applyNightMode(@NonNull ThemeMode mode) {
        if (AppCompatDelegate.getDefaultNightMode() != mode.nightMode) {
            AppCompatDelegate.setDefaultNightMode(mode.nightMode);
        }
    }
}
