package com.example.android.cricketscoreboard.views;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.android.cricketscoreboard.DatabaseHandler;
import com.example.android.cricketscoreboard.R;
import com.example.android.cricketscoreboard.UiSettingsDialog;
import com.example.android.cricketscoreboard.scoring.CricketAnalyticsCalculator;
import com.example.android.cricketscoreboard.scoring.CricketRulesProfile;
import com.example.android.cricketscoreboard.scoring.DeliveryEvent;
import com.example.android.cricketscoreboard.scoring.DeliveryEvent.DismissalType;
import com.example.android.cricketscoreboard.scoring.DeliveryEvent.ExtraType;
import com.example.android.cricketscoreboard.scoring.MatchState;
import com.example.android.cricketscoreboard.scoring.MatchState.InningsScorecard;
import com.example.android.cricketscoreboard.scoring.MatchState.MatchLifecycleStatus;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;
import java.util.UUID;

/**
 * 4-Tab Live Match Center hosting:
 * <ul>
 *   <li>Tab 1: Landscape Quick Scorer Pad</li>
 *   <li>Tab 2: Live Batting, Bowling &amp; Fall-of-Wickets Scorecard</li>
 *   <li>Tab 3: Over-by-Over Manhattan &amp; Worm Analytics + Model Projections</li>
 *   <li>Tab 4: Match Summary, Rules Snapshot, Result Confirmation &amp; One-Tap Share</li>
 * </ul>
 */
public class ScorerTabActivity extends AppCompatActivity {

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager2 mViewPager;
    private DatabaseHandler dbHandler;
    private String activeMatchUuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scorer_tab);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dbHandler = new DatabaseHandler(this);
        ensureActiveMatchUuid();

        mSectionsPagerAdapter = new SectionsPagerAdapter(this);
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = findViewById(R.id.tabs);
        new TabLayoutMediator(tabLayout, mViewPager,
                (tab, position) -> tab.setText(mSectionsPagerAdapter.getPageTitle(position)))
                .attach();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            MatchState current = getOrLoadMatchState();
            if (current == null || current.appliedEvents.isEmpty()) {
                Snackbar.make(view, "No delivery events to undo", Snackbar.LENGTH_SHORT).show();
            } else {
                dbHandler.undoLastMatchDelivery(current.matchUuid);
                refreshAllTabs();
                Snackbar.make(view, "Last delivery undone", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void ensureActiveMatchUuid() {
        if (getIntent() != null) {
            activeMatchUuid = getIntent().getStringExtra(ScorerPadActivity.EXTRA_MATCH_UUID);
        }
        if (activeMatchUuid == null || activeMatchUuid.isEmpty()) {
            MatchState latest = dbHandler.getLatestActiveMatchState();
            if (latest != null) {
                activeMatchUuid = latest.matchUuid;
            } else {
                MatchState fresh = new MatchState(
                        UUID.randomUUID().toString(),
                        CricketRulesProfile.createStandardT20(),
                        1, "Home XI", 2, "Visitors XI",
                        MatchLifecycleStatus.PROVISIONAL);
                dbHandler.saveMatchSessionHeader(fresh, 0);
                activeMatchUuid = fresh.matchUuid;
            }
        }
    }

    public MatchState getOrLoadMatchState() {
        ensureActiveMatchUuid();
        MatchState loaded = dbHandler.loadMatchState(activeMatchUuid);
        if (loaded == null) {
            loaded = new MatchState(
                    activeMatchUuid,
                    CricketRulesProfile.createStandardT20(),
                    1, "Home XI", 2, "Visitors XI",
                    MatchLifecycleStatus.PROVISIONAL);
            dbHandler.saveMatchSessionHeader(loaded, 0);
        }
        return loaded;
    }

    public DatabaseHandler getDbHandler() {
        return dbHandler;
    }

    public void refreshAllTabs() {
        if (mSectionsPagerAdapter != null) {
            mSectionsPagerAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_scorer_tab, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            UiSettingsDialog.show(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class PlaceholderFragment extends Fragment {

        private static final String ARG_SECTION_NUMBER = "section_number";
        private TextView dynamicReportTextView;

        public PlaceholderFragment() {
        }

        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            int section = getArguments() != null ? getArguments().getInt(ARG_SECTION_NUMBER, 1) : 1;
            if (section == 1) {
                View rootView = inflater.inflate(R.layout.fragment_scorer_tab, container, false);
                bindScorerPadTab(rootView);
                return rootView;
            }
            return buildReportTabView(section);
        }

        @Override
        public void onResume() {
            super.onResume();
            int section = getArguments() != null ? getArguments().getInt(ARG_SECTION_NUMBER, 1) : 1;
            if (section > 1 && dynamicReportTextView != null) {
                updateReportText(section);
            }
        }

        private void bindScorerPadTab(View rootView) {
            if (!(getActivity() instanceof ScorerTabActivity)) {
                return;
            }
            ScorerTabActivity host = (ScorerTabActivity) getActivity();
            MatchState state = host.getOrLoadMatchState();
            InningsScorecard inn = state.getCurrentInnings();

            ProgressBar overProgress = rootView.findViewById(R.id.status_CurrentOver);
            TextView oversField = rootView.findViewById(R.id.oversBowled_field);
            RadioGroup runsGroup = rootView.findViewById(R.id.runsScored_radioGrp);
            Button nextBallBtn = rootView.findViewById(R.id.nxtBall_btn);
            Button wideBtn = rootView.findViewById(R.id.wideBowl_btn);
            Button noBallBtn = rootView.findViewById(R.id.noBowl_btn);
            Button byesBtn = rootView.findViewById(R.id.byes_btn);
            Button legByesBtn = rootView.findViewById(R.id.legByes_btn);
            Button outBtn = rootView.findViewById(R.id.out_btn);
            Button retdBtn = rootView.findViewById(R.id.retd_btn);

            if (overProgress != null) {
                overProgress.setMax(state.rulesProfile.ballsPerOver);
                overProgress.setProgress(inn.ballsInCurrentOver);
            }
            if (oversField != null) {
                oversField.setText(inn.battingTeamName + " " + inn.getScoreSlashWickets()
                        + " (" + inn.getOversNotation() + " Overs)");
            }

            View.OnClickListener recordHelper = v -> {
                MatchState current = host.getOrLoadMatchState();
                InningsScorecard curInn = current.getCurrentInnings();
                int selectedRuns = 0;
                if (runsGroup != null) {
                    int checkedId = runsGroup.getCheckedRadioButtonId();
                    RadioButton rb = rootView.findViewById(checkedId);
                    if (rb != null && rb.getText() != null && rb.getText().length() > 0) {
                        char c = rb.getText().charAt(0);
                        if (c >= '0' && c <= '6') {
                            selectedRuns = c - '0';
                        }
                    }
                }
                ExtraType extraType = ExtraType.NONE;
                int runsOffBat = selectedRuns;
                int extraRuns = 0;
                DismissalType dismissalType = DismissalType.NONE;

                int vid = v.getId();
                if (vid == R.id.wideBowl_btn) {
                    extraType = ExtraType.WIDE;
                    runsOffBat = 0;
                    extraRuns = selectedRuns;
                } else if (vid == R.id.noBowl_btn) {
                    extraType = ExtraType.NO_BALL;
                } else if (vid == R.id.byes_btn) {
                    extraType = ExtraType.BYE;
                    runsOffBat = 0;
                    extraRuns = Math.max(1, selectedRuns);
                } else if (vid == R.id.legByes_btn) {
                    extraType = ExtraType.LEG_BYE;
                    runsOffBat = 0;
                    extraRuns = Math.max(1, selectedRuns);
                } else if (vid == R.id.out_btn) {
                    dismissalType = DismissalType.BOWLED;
                } else if (vid == R.id.retd_btn) {
                    dismissalType = DismissalType.RETIRED_HURT;
                }

                DeliveryEvent ev = new DeliveryEvent.Builder()
                        .setMatchUuid(current.matchUuid)
                        .setInningsNumber(current.currentInningsNumber)
                        .setSequenceNumber(current.appliedEvents.size() + 1)
                        .setStriker(curInn.currentStrikerId, curInn.currentStrikerName)
                        .setNonStriker(curInn.currentNonStrikerId, curInn.currentNonStrikerName)
                        .setBowler(curInn.currentBowlerId, curInn.currentBowlerName)
                        .setRunsOffBat(runsOffBat)
                        .setExtraType(extraType)
                        .setExtraRuns(extraRuns)
                        .setDismissal(dismissalType, curInn.currentStrikerId, curInn.currentStrikerName, "", 0, "")
                        .setWasFreeHit(curInn.freeHitArmed)
                        .build();

                try {
                    MatchState updated = host.getDbHandler().recordMatchDelivery(current, ev);
                    InningsScorecard upInn = updated.getCurrentInnings();
                    if (overProgress != null) {
                        overProgress.setMax(updated.rulesProfile.ballsPerOver);
                        overProgress.setProgress(upInn.ballsInCurrentOver);
                    }
                    if (oversField != null) {
                        oversField.setText(upInn.battingTeamName + " " + upInn.getScoreSlashWickets()
                                + " (" + upInn.getOversNotation() + " Overs)");
                    }
                } catch (IllegalArgumentException ex) {
                    Snackbar.make(rootView, ex.getMessage(), Snackbar.LENGTH_SHORT).show();
                }
            };

            if (nextBallBtn != null) {
                nextBallBtn.setOnClickListener(recordHelper);
            }
            if (wideBtn != null) {
                wideBtn.setOnClickListener(recordHelper);
            }
            if (noBallBtn != null) {
                noBallBtn.setOnClickListener(recordHelper);
            }
            if (byesBtn != null) {
                byesBtn.setOnClickListener(recordHelper);
            }
            if (legByesBtn != null) {
                legByesBtn.setOnClickListener(recordHelper);
            }
            if (outBtn != null) {
                outBtn.setOnClickListener(recordHelper);
            }
            if (retdBtn != null) {
                retdBtn.setOnClickListener(recordHelper);
            }
        }

        private View buildReportTabView(int section) {
            ScrollView scrollView = new ScrollView(requireContext());
            scrollView.setFillViewport(true);
            int pad = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
            scrollView.setPadding(pad, pad, pad, pad);

            LinearLayout container = new LinearLayout(requireContext());
            container.setOrientation(LinearLayout.VERTICAL);

            LinearLayout actionRow = new LinearLayout(requireContext());
            actionRow.setOrientation(LinearLayout.HORIZONTAL);
            actionRow.setGravity(Gravity.END);

            Button shareBtn = new Button(requireContext());
            shareBtn.setText("Share Live Score");
            shareBtn.setOnClickListener(v -> {
                if (getActivity() instanceof ScorerTabActivity) {
                    ScorerTabActivity host = (ScorerTabActivity) getActivity();
                    MatchState state = host.getOrLoadMatchState();
                    Intent sendIntent = new Intent(Intent.ACTION_SEND);
                    sendIntent.setType("text/plain");
                    sendIntent.putExtra(Intent.EXTRA_TEXT,
                            CricketAnalyticsCalculator.buildShareableScorecardText(state));
                    startActivity(Intent.createChooser(sendIntent, "Share Live Cricket Scorecard"));
                }
            });
            actionRow.addView(shareBtn);

            if (section == 4) {
                Button confirmBtn = new Button(requireContext());
                confirmBtn.setText("Confirm Official Result");
                confirmBtn.setOnClickListener(v -> {
                    if (getActivity() instanceof ScorerTabActivity) {
                        ScorerTabActivity host = (ScorerTabActivity) getActivity();
                        MatchState current = host.getOrLoadMatchState();
                        host.getDbHandler().confirmAndCommitMatchStats(current.matchUuid, false);
                        updateReportText(section);
                        Snackbar.make(scrollView, "Match Confirmed & Career Stats Updated",
                                Snackbar.LENGTH_SHORT).show();
                    }
                });
                actionRow.addView(confirmBtn);
            }

            container.addView(actionRow);

            dynamicReportTextView = new TextView(requireContext());
            dynamicReportTextView.setTypeface(Typeface.MONOSPACE);
            dynamicReportTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            dynamicReportTextView.setPadding(0, pad / 2, 0, pad);
            container.addView(dynamicReportTextView);

            scrollView.addView(container);
            updateReportText(section);
            return scrollView;
        }

        private void updateReportText(int section) {
            if (!(getActivity() instanceof ScorerTabActivity) || dynamicReportTextView == null) {
                return;
            }
            ScorerTabActivity host = (ScorerTabActivity) getActivity();
            MatchState state = host.getOrLoadMatchState();
            if (section == 2) {
                dynamicReportTextView.setText(CricketAnalyticsCalculator.buildFullScorecardReport(state));
            } else if (section == 3) {
                dynamicReportTextView.setText(CricketAnalyticsCalculator.buildOverByOverChartReport(state));
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append(CricketAnalyticsCalculator.buildShareableScorecardText(state)).append("\n\n");
                sb.append("ACTIVE RULES SNAPSHOT (").append(state.rulesProfile.profileId)
                        .append(" v").append(state.rulesProfile.version).append("):\n");
                List<String> rulesNotes = state.rulesProfile.describeActiveRules();
                for (String note : rulesNotes) {
                    sb.append("  • ").append(note).append("\n");
                }
                sb.append("\nRECENT BALL-BY-BALL COMMENTARY:\n");
                InningsScorecard inn = state.getCurrentInnings();
                if (inn.commentaryLog.isEmpty()) {
                    sb.append("  No balls bowled yet in Innings ").append(inn.inningsNumber).append(".\n");
                } else {
                    int start = Math.max(0, inn.commentaryLog.size() - 15);
                    for (int i = inn.commentaryLog.size() - 1; i >= start; i--) {
                        sb.append("  ").append(inn.commentaryLog.get(i)).append("\n");
                    }
                }
                dynamicReportTextView.setText(sb.toString());
            }
        }
    }

    public static class SectionsPagerAdapter extends FragmentStateAdapter {

        public SectionsPagerAdapter(@NonNull FragmentActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getItemCount() {
            return 4;
        }

        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Scorer Pad";
                case 1:
                    return "Scorecard";
                case 2:
                    return "Match Charts";
                case 3:
                    return "Match Summary";
            }
            return null;
        }
    }
}
