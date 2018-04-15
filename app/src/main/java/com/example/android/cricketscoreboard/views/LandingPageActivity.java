package com.example.android.cricketscoreboard.views;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.android.cricketscoreboard.DatabaseHandler;
import com.example.android.cricketscoreboard.R;
import com.example.android.cricketscoreboard.entity.Teams;

import java.util.List;

public class LandingPageActivity extends AppCompatActivity {
    Button playerDetailbtn,teamDetailbtn,groundDetailbtn,scheduleMatchesbtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing_page);


        //Initializing Menu for Team Details
        teamDetailbtn=(Button)findViewById((R.id.teamDetails_btn)) ;
        teamPopup();

        //Initializing Menu for Player Details
        playerDetailbtn =(Button)findViewById(R.id.playerDetails_btn);
        playerPopup();

        //Initializing Menu for Ground Details
        groundDetailbtn=(Button)findViewById((R.id.groundDetails_btn)) ;
        groundPopup();


        //Initializing Menu for Schedule Matches
        scheduleMatchesbtn=(Button)findViewById((R.id.scheduleMatch_btn)) ;
        schedulePopup();

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
    public void startMatch(View view){


        Intent intent = new Intent(LandingPageActivity.this,ScorerPadActivity.class);
        startActivity(intent);


    }
}
