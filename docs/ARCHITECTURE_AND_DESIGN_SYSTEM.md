# Architecture & Design System Guide

## 1. Architectural Overview

Cricket Score Board uses an Activity-driven MVC architecture organized around a central SQLite persistence layer and a decoupled UI preferences layer:

```
com.example.android.cricketscoreboard
├── CricketScoreBoardApp.java      # Application lifecycle: theme/accent/density, ActionBar Up nav, API 35+ insets
├── DatabaseHandler.java           # SQLiteOpenHelper ("cricketScore_DB" v2) for all operational club/match data
├── ScoreMan.java                  # In-memory ball-by-ball score, over, and undo state machine
├── ImageUtility.java              # Bitmap <-> byte[] conversion utilities
├── UiPreferences.java             # Typed SharedPreferences manager for UI customization
├── UiSettingsDialog.java          # Appearance & Preferences modal dialog with Reset to Defaults
├── entity/                        # Domain POJOs (Teams, Players, Matches, Schedules, Grounds, Rules, Users)
└── views/                         # Activity controllers
    ├── LoadAppActivity.java       # Welcome screen (skippable via UiPreferences)
    ├── LandingPageActivity.java   # Central dashboard with live DB summary banner & popup menus
    ├── TeamDetails.java           # Create team + Photo Picker emblem selection
    ├── ViewTeams.java             # Inspect team record + add/remove squad players
    ├── PlayerDetailsActivity.java # Register player + batting/bowling/WK roles
    ├── ViewPlayers.java           # Inspect player bio + toggle batting/bowling career stat tables
    ├── GroundDetailsActivity.java # Register cricket ground & city
    ├── ViewGrounds.java           # Filter grounds by city
    ├── ScheduleMatches.java       # Schedule Team A vs Team B fixture with overs & date
    ├── ViewSchedules.java         # Tabular fixture list with empty-state guidance
    ├── ScorerPadActivity.java     # Portrait ball-by-ball live scorer with drawer navigation
    └── ScorerTabActivity.java     # Landscape ViewPager2 + TabLayout match center
```

### Separation of Operational Data vs. UI Preferences
- **Operational Data (`cricketScore_DB`):** Managed exclusively by [`DatabaseHandler`](../app/src/main/java/com/example/android/cricketscoreboard/DatabaseHandler.java) across 7 tables (`USER_DETAILS`, `TEAM_DETAILS`, `PLAYER_DETAILS`, `SCHEDULE_DETAILS`, `MATCH_DETAILS`, `RULES_DETAILS`, `GROUND_DETAILS`).
- **UI Preferences (`cricket_ui_preferences`):** Managed exclusively by [`UiPreferences`](../app/src/main/java/com/example/android/cricketscoreboard/UiPreferences.java) using `SharedPreferences`. Changing or resetting UI preferences never alters club, team, player, or match records.

---

## 2. Design System & Tokens

### Color System ([`values/colors.xml`](../app/src/main/res/values/colors.xml) & [`values-night/colors.xml`](../app/src/main/res/values-night/colors.xml))
The palette is inspired by cricket turf, stadium floodlights, and crimson leather balls, with full Light and Dark theme parity:

| Token | Light Mode | Dark Mode (`values-night`) | Purpose |
| :--- | :--- | :--- | :--- |
| `colorPrimary` | `#1B5E20` | `#2E7D32` | Primary app bars, primary CTA buttons |
| `colorAccent` / `colorSecondary` | `#C62828` | `#EF5350` | Selection controls, highlights, alerts |
| `Color_Background` | `#F5F8F4` | `#111511` | Screen root background |
| `colorSurface` | `#FFFFFF` | `#1B211B` | Card surfaces (`@drawable/rectangle`) |
| `colorSurfaceElevated` | `#F1F6F0` | `#242C24` | Stat chips & secondary containers (`@drawable/bg_stat_chip`) |
| `colorSurfaceHeader` | `#1B5E20` | `#1E3A20` | Scoreboard & club summary banners (`@drawable/bg_scoreboard_header`) |
| `colorTextPrimary` | `#181D18` | `#E6ECE5` | High-contrast headings and body text |
| `colorTextSecondary` | `#444F44` | `#B5C2B4` | Supporting labels and captions |

### Typography & Component Styles ([`values/styles.xml`](../app/src/main/res/values/styles.xml))
- `TextAppearance.Cricket.ScreenTitle`: `22sp` bold `sans-serif-medium` for screen headers.
- `TextAppearance.Cricket.SectionTitle`: `16sp` bold `sans-serif-medium` for card/section titles.
- `TextAppearance.Cricket.FieldLabel`: `13sp` bold secondary text for form field labels.
- `TextAppearance.Cricket.StatValue` / `StatLabel`: `20sp` bold values paired with `12sp` labels in stat tables.
- `Widget.Cricket.PrimaryButton`, `Widget.Cricket.OutlinedButton`, `Widget.Cricket.DashboardTile`: Standardized `48dp`+ minimum touch targets, `10dp`–`14dp` corner radii, and non-all-caps readable labels.

### Edge-to-Edge & Window Insets ([`CricketScoreBoardApp.java`](../app/src/main/java/com/example/android/cricketscoreboard/CricketScoreBoardApp.java))
On Android 15+ (API 35+), apps targeting SDK 36 render edge-to-edge by default. `CricketScoreBoardApp` registers an `OnApplyWindowInsetsListener` on every Activity's `android.R.id.content` view that pads for system bars, display cutouts, the IME keyboard, and the AppCompat decor ActionBar height.

---

## 3. User Customization & Extension Points

[`UiPreferences`](../app/src/main/java/com/example/android/cricketscoreboard/UiPreferences.java) exposes strongly typed enums and getters/setters:

1. **`ThemeMode` (`SYSTEM`, `LIGHT`, `DARK`):** Applied via `AppCompatDelegate.setDefaultNightMode(...)`.
2. **`AccentPalette` (`TURF_GREEN`, `STADIUM_BLUE`, `PAVILION_GOLD`):** Applied as a theme overlay (`R.style.ThemeOverlay_Cricket_Accent_*`) in `CricketScoreBoardApp.onActivityPreCreated`. To add a new palette, define a `ThemeOverlay.Cricket.Accent.<Name>` style in `styles.xml` and add an enum constant in `UiPreferences.AccentPalette`.
3. **`LayoutDensity` (`STANDARD`, `COMFORTABLE`):** Scales root container padding (`1.0f` vs. `1.35f`) in `CricketScoreBoardApp`.
4. **Scorer Haptics (`KEY_SCORER_HAPTICS`):** Controls virtual-key haptic feedback when recording or undoing a delivery in `ScorerPadActivity.onBallPress`.
5. **Skip Welcome Screen (`KEY_SKIP_SPLASH`):** When enabled, `LoadAppActivity` routes directly to `LandingPageActivity` on cold start.
