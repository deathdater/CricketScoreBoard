package com.example.android.cricketscoreboard.views;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.cricketscoreboard.DatabaseHandler;
import com.example.android.cricketscoreboard.R;
import com.example.android.cricketscoreboard.UiSettingsDialog;
import com.example.android.cricketscoreboard.sync.CloudSyncDialog;
import com.example.android.cricketscoreboard.sync.SyncPreferences;

import java.util.List;
import java.util.Locale;

public class LandingPageActivity extends AppCompatActivity {
    Button playerDetailbtn, teamDetailbtn, groundDetailbtn, scheduleMatchesbtn;
    Button viewMatchSummaryBtn, setRulesBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing_page);

        // Initializing Menu for Team Details
        teamDetailbtn = (Button) findViewById(R.id.teamDetails_btn);
        teamPopup();

        // Initializing Menu for Player Details
        playerDetailbtn = (Button) findViewById(R.id.playerDetails_btn);
        playerPopup();

        // Initializing Menu for Ground Details
        groundDetailbtn = (Button) findViewById(R.id.groundDetails_btn);
        groundPopup();

        // Initializing Menu for Schedule Matches
        scheduleMatchesbtn = (Button) findViewById(R.id.scheduleMatch_btn);
        schedulePopup();

        // Secondary tiles: Match Summary opens Match Center tabs; Set Rules opens Preferences & Rules
        viewMatchSummaryBtn = (Button) findViewById(R.id.viewMatchSummary_btn);
        if (viewMatchSummaryBtn != null) {
            viewMatchSummaryBtn.setOnClickListener(v -> {
                Intent intent = new Intent(LandingPageActivity.this, ScorerTabActivity.class);
                startActivity(intent);
            });
        }

        setRulesBtn = (Button) findViewById(R.id.setRules_btn);
        if (setRulesBtn != null) {
            setRulesBtn.setOnClickListener(v -> UiSettingsDialog.show(LandingPageActivity.this));
        }

        View summaryCard = findViewById(R.id.dashboard_summary_card);
        if (summaryCard != null) {
            summaryCard.setOnClickListener(v ->
                    CloudSyncDialog.show(LandingPageActivity.this, this::refreshClubSummaryBanner));
        }

        refreshClubSummaryBanner();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshClubSummaryBanner();
    }

    private void refreshClubSummaryBanner() {
        TextView subtitle = findViewById(R.id.dashboard_summary_subtitle);
        if (subtitle == null) {
            return;
        }
        try {
            DatabaseHandler db = new DatabaseHandler(this);
            List<String> teams = db.getAllTeamNames();
            List<String> players = db.viewAllPlayers();
            int schedules = db.getNumberOfSchedules();
            int teamCount = teams != null ? teams.size() : 0;
            int playerCount = players != null ? players.size() : 0;
            SyncPreferences syncPrefs = SyncPreferences.get(this);
            String syncBadge = syncPrefs.isSyncEnabled()
                    ? "  •  Sync: " + syncPrefs.getClubId()
                    : "";
            subtitle.setText(String.format(Locale.US,
                    "%d Teams  •  %d Players  •  %d Scheduled Matches%s",
                    teamCount, playerCount, schedules, syncBadge));
        } catch (Exception e) {
            Log.v("LandingPageActivity", "Summary banner refresh failed: " + e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scorer_pad, menu);
        MenuItem settingsItem = menu.findItem(R.id.action_settings);
        if (settingsItem != null) {
            settingsItem.setIcon(R.drawable.ic_settings_24dp);
            settingsItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            UiSettingsDialog.show(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void teamPopup(){
        teamDetailbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                PopupMenu popup = new PopupMenu(LandingPageActivity.this,teamDetailbtn);

                popup.getMenuInflater().inflate(R.menu.landing_page_popupmenu,popup.getMenu());

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){

                    public boolean onMenuItemClick(MenuItem item) {

                        if (item.getTitle().toString().equals("New")) {

                            Toast.makeText(LandingPageActivity.this, "New Team Details", Toast.LENGTH_SHORT).show();
                            teamDetails(v);
                            return true;
                        } else if (item.getTitle().toString().equals("View/Edit")) {
                            DatabaseHandler db=new DatabaseHandler(LandingPageActivity.this);
                            List<String> val= db.getAllTeamNames();
                            if(val.isEmpty()) {
                                Log.v("TEAM LIST", "List is empty");
                                Toast.makeText(LandingPageActivity.this,"No Data Found!!",Toast.LENGTH_SHORT).show();

                            }
                            else{
                                viewTeams(v);

                            Toast.makeText(LandingPageActivity.this, "View/Edit Team Details", Toast.LENGTH_SHORT).show();

                            return true;
                            }
                        }

                        return true;

                    }
                });
                // to show icons in the popup menu Phew!!
                MenuPopupHelper menuHelper=new MenuPopupHelper(LandingPageActivity.this,(MenuBuilder)popup.getMenu(), teamDetailbtn);
                menuHelper.setForceShowIcon(true);
                menuHelper.show();

            }
        });


    }


    public void groundPopup(){
        groundDetailbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                PopupMenu popup = new PopupMenu(LandingPageActivity.this,groundDetailbtn);

                popup.getMenuInflater().inflate(R.menu.landing_page_popupmenu,popup.getMenu());

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){

                    public boolean onMenuItemClick(MenuItem item) {

                        if (item.getTitle().toString().equals("New")) {

                            Toast.makeText(LandingPageActivity.this, "New Ground Details", Toast.LENGTH_SHORT).show();
                            groundDetails(v);
                            return true;
                        } else if (item.getTitle().toString().equals("View/Edit")) {
                            Toast.makeText(LandingPageActivity.this, "View/Edit Ground Details", Toast.LENGTH_SHORT).show();
                            viewGrounds(v);
                            return true;
                        }

                        return true;

                    }
                });
                // to show icons in the popup menu Phew!!
                MenuPopupHelper menuHelper=new MenuPopupHelper(LandingPageActivity.this,(MenuBuilder)popup.getMenu(), groundDetailbtn);
                menuHelper.setForceShowIcon(true);
                menuHelper.show();
            }
        });


    }
    public void playerPopup(){
        playerDetailbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                PopupMenu popup = new PopupMenu(LandingPageActivity.this,playerDetailbtn);

                popup.getMenuInflater().inflate(R.menu.landing_page_popupmenu,popup.getMenu());

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){

                    public boolean onMenuItemClick(MenuItem item) {

                        if (item.getTitle().toString().equals("New")) {

                            Toast.makeText(LandingPageActivity.this, "New Player Details", Toast.LENGTH_SHORT).show();
                            playerDetails(v);
                            return true;
                        } else if (item.getTitle().toString().equals("View/Edit")) {

                            DatabaseHandler db=new DatabaseHandler(LandingPageActivity.this);
                            List<String> val= db.viewAllPlayers();
                            if(val.isEmpty()) {
                                Log.v("Player LIST", "List is empty");
                                Toast.makeText(LandingPageActivity.this,"No Data Found!!",Toast.LENGTH_SHORT).show();

                            }
                            else{
                                Toast.makeText(LandingPageActivity.this, "View/Edit Player Details", Toast.LENGTH_SHORT).show();
                                viewPlayers(v);
                                return true;
                            }

                        }

                        return true;

                    }
                });
                // to show icons in the popup menu Phew!!
             MenuPopupHelper menuHelper=new MenuPopupHelper(LandingPageActivity.this,(MenuBuilder)popup.getMenu(), playerDetailbtn);
               menuHelper.setForceShowIcon(true);
              menuHelper.show();

            }
        });

    }

    public void schedulePopup(){
        scheduleMatchesbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                PopupMenu popup = new PopupMenu(LandingPageActivity.this,scheduleMatchesbtn);

                popup.getMenuInflater().inflate(R.menu.landing_page_popupmenu,popup.getMenu());
                popup.setGravity(Gravity.AXIS_PULL_AFTER);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){

                    public boolean onMenuItemClick(MenuItem item) {

                        if (item.getTitle().toString().equals("New")) {

                            Toast.makeText(LandingPageActivity.this, "New Match Schedule", Toast.LENGTH_SHORT).show();
                            scheduleMatches(v);
                            return true;
                        } else if (item.getTitle().toString().equals("View/Edit")) {

                            Toast.makeText(LandingPageActivity.this, "View/Edit Match Schedule", Toast.LENGTH_SHORT).show();
                            viewScheduleMatches(v);
                            return true;
                        }

                        return true;

                    }
                });
                // to show icons in the popup menu Phew!!
                MenuPopupHelper menuHelper=new MenuPopupHelper(LandingPageActivity.this,(MenuBuilder)popup.getMenu(), scheduleMatchesbtn);
                menuHelper.setForceShowIcon(true);
                menuHelper.show();
            }
        });

    }

    public void teamDetails(View view){
            Intent intent = new Intent(LandingPageActivity.this,TeamDetails.class);
            startActivity(intent);
    }
    public void viewTeams(View view){
        Intent intent = new Intent(LandingPageActivity.this,ViewTeams.class);
        startActivity(intent);
    }
    public void viewPlayers(View view){
        Intent intent = new Intent(LandingPageActivity.this,ViewPlayers.class);
        startActivity(intent);
    }

    public void viewGrounds(View view){
        Intent intent = new Intent(LandingPageActivity.this,ViewGrounds.class);
        startActivity(intent);
    }
    public void playerDetails(View view){
        Intent intent = new Intent(LandingPageActivity.this,PlayerDetailsActivity.class);
        startActivity(intent);
    }

    public void groundDetails(View view){
        Intent intent = new Intent(LandingPageActivity.this,GroundDetailsActivity.class);
        startActivity(intent);
    }

    public void scheduleMatches(View view){
        Intent intent = new Intent(LandingPageActivity.this,ScheduleMatches.class);
        startActivity(intent);
    }
    public void viewScheduleMatches(View view){
        Intent intent = new Intent(LandingPageActivity.this,ViewSchedules.class);
        startActivity(intent);
    }
    public void startMatch(View view){
        Intent intent = new Intent(LandingPageActivity.this,ScorerPadActivity.class);
        startActivity(intent);
    }
}
