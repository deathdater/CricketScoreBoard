package com.example.android.cricketscoreboard;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

/**
 * Modal dialog allowing the user to customize theme mode, accent palette, layout density,
 * scorer haptics, and startup behavior, with a one-tap Reset to Defaults option.
 */
public final class UiSettingsDialog {

    private UiSettingsDialog() {
    }

    public static void show(@NonNull Activity activity) {
        UiPreferences prefs = UiPreferences.get(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_ui_settings, null);

        RadioGroup themeGroup = view.findViewById(R.id.pref_theme_mode_group);
        switch (prefs.getThemeMode()) {
            case LIGHT:
                themeGroup.check(R.id.pref_theme_light);
                break;
            case DARK:
                themeGroup.check(R.id.pref_theme_dark);
                break;
            case SYSTEM:
            default:
                themeGroup.check(R.id.pref_theme_system);
                break;
        }

        RadioGroup accentGroup = view.findViewById(R.id.pref_accent_group);
        switch (prefs.getAccentPalette()) {
            case STADIUM_BLUE:
                accentGroup.check(R.id.pref_accent_stadium);
                break;
            case PAVILION_GOLD:
                accentGroup.check(R.id.pref_accent_pavilion);
                break;
            case TURF_GREEN:
            default:
                accentGroup.check(R.id.pref_accent_turf);
                break;
        }

        RadioGroup densityGroup = view.findViewById(R.id.pref_density_group);
        if (prefs.getLayoutDensity() == UiPreferences.LayoutDensity.COMFORTABLE) {
            densityGroup.check(R.id.pref_density_comfortable);
        } else {
            densityGroup.check(R.id.pref_density_standard);
        }

        CheckBox hapticsBox = view.findViewById(R.id.pref_scorer_haptics);
        hapticsBox.setChecked(prefs.isScorerHapticsEnabled());

        CheckBox skipSplashBox = view.findViewById(R.id.pref_skip_splash);
        skipSplashBox.setChecked(prefs.isSkipSplashEnabled());

        new AlertDialog.Builder(activity)
                .setTitle("Appearance & Preferences")
                .setView(view)
                .setPositiveButton("Apply", (dialog, which) -> {
                    UiPreferences.AccentPalette oldAccent = prefs.getAccentPalette();
                    UiPreferences.LayoutDensity oldDensity = prefs.getLayoutDensity();

                    int checkedTheme = themeGroup.getCheckedRadioButtonId();
                    UiPreferences.ThemeMode newTheme = UiPreferences.ThemeMode.SYSTEM;
                    if (checkedTheme == R.id.pref_theme_light) {
                        newTheme = UiPreferences.ThemeMode.LIGHT;
                    } else if (checkedTheme == R.id.pref_theme_dark) {
                        newTheme = UiPreferences.ThemeMode.DARK;
                    }

                    int checkedAccent = accentGroup.getCheckedRadioButtonId();
                    UiPreferences.AccentPalette newAccent = UiPreferences.AccentPalette.TURF_GREEN;
                    if (checkedAccent == R.id.pref_accent_stadium) {
                        newAccent = UiPreferences.AccentPalette.STADIUM_BLUE;
                    } else if (checkedAccent == R.id.pref_accent_pavilion) {
                        newAccent = UiPreferences.AccentPalette.PAVILION_GOLD;
                    }

                    int checkedDensity = densityGroup.getCheckedRadioButtonId();
                    UiPreferences.LayoutDensity newDensity =
                            checkedDensity == R.id.pref_density_comfortable
                                    ? UiPreferences.LayoutDensity.COMFORTABLE
                                    : UiPreferences.LayoutDensity.STANDARD;

                    prefs.setAccentPalette(newAccent);
                    prefs.setLayoutDensity(newDensity);
                    prefs.setScorerHapticsEnabled(hapticsBox.isChecked());
                    prefs.setSkipSplashEnabled(skipSplashBox.isChecked());
                    prefs.setThemeMode(newTheme);

                    if (oldAccent != newAccent || oldDensity != newDensity) {
                        activity.recreate();
                    }
                })
                .setNeutralButton("Reset Defaults", (dialog, which) -> {
                    prefs.resetToDefaults();
                    Toast.makeText(activity, "Preferences reset to defaults", Toast.LENGTH_SHORT).show();
                    activity.recreate();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
