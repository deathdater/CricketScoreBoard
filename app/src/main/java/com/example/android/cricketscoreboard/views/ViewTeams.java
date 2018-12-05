package com.example.android.cricketscoreboard.views;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.cricketscoreboard.DatabaseHandler;
import com.example.android.cricketscoreboard.R;
import com.example.android.cricketscoreboard.entity.Teams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ViewTeams extends AppCompatActivity {
    Bundle customsavedIntanceState;
    List valTeam=new ArrayList();
    List valAddPlayer=new ArrayList();
    List valRemovePlayer=new ArrayList();
    Spinner teamList;
    Spinner addPlayer_spinner;
    Spinner removePlayer_spinner;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_teams);
        teamList=(Spinner)findViewById(R.id.teamNameSpinner) ;
        addPlayer_spinner=(Spinner)findViewById(R.id.addPlayer_spinner);
        removePlayer_spinner=(Spinner)findViewById(R.id.removePlayer_spinner);
       // final ListView listview = (ListView) findViewById(R.id.teams_list);

        //String[] values = new String[] { "Android", "iPhone", "WindowsMobile",
         //       "Blackberry", "WebOS", "Ubuntu", "Windows7", "Max OS X",
          //      "Linux", "OS/2", "Ubuntu", "Windows7", "Max OS X", "Linux",
           //     "OS/2", "Ubuntu", "Windows7", "Max OS X", "Linux", "OS/2",
            //    "Android", "iPhone", "WindowsMobile" };
        try{
        DatabaseHandler db=new DatabaseHandler(this);
        /*
        List valTeam=new ArrayList();
        List valAddPlayer=new ArrayList();
        List valRemovePlayer=new ArrayList();
        */

            Log.v("View Teams","Checking if the teams exist in DB");

//Preparing list of team Names
        if(db.getAllTeamNames().size()>0) {

            for (int i = 0; i < db.getAllTeamNames().size(); i++) {

                valTeam.add(db.getAllTeamNames().get(i));
                Log.i("Value of teams list item " + i, db.getAllTeamNames().get(i));
            }
        }

            Log.v("View Teams","Check 2 is"+db.viewAllPlayers(0).size());
//Preparing list of PlayerNames to be added
            int zero=0;
            if(db.viewAllPlayers(zero).size()>0){

                for(int i=0;i<db.viewAllPlayers(zero).size();i++){

                valAddPlayer.add(db.viewAllPlayers(zero).get(i));
                Log.i("Value of add player list "+i,db.viewAllPlayers(zero).get(i));
                }
            }


            Log.v("View Teams","Check 3");






            //  while(db.getAllTeamNames().contains('a')) {
        //   val.add(db.getAllTeamNames().get(i));
          //  Log.i("Value of element "+i,val.get(i));
           // i++;

        //System.out.println("lemme check"+val);
       // final ArrayList<String> list = new ArrayList<>();
       // for (int i = 0; i < values.length; ++i) {
       //     list.add(values[i]);
       // }
        final StableArrayAdapter teamListAdapter = new StableArrayAdapter(this,
                android.R.layout.preference_category, valTeam);
        teamList.setAdapter(teamListAdapter);
        teamList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                // Object item = parent.getItemAtPosition(pos);


                TextView teamNameText = (TextView)findViewById(R.id.teamNameText);
                TextView teamMatchPlayedValue=(TextView)findViewById(R.id.teamMatchPlayedValue);
                TextView teamMatchWonValue=(TextView)findViewById(R.id.teamMatchWonValue);
                TextView teamMatchLostValue=(TextView)findViewById(R.id.teamMatchLostValue);
                TextView teamMatchDrawnValue=(TextView)findViewById(R.id.teamMatchDrawnValue);
                Spinner teamCaptainValue=(Spinner)findViewById(R.id.teamCaptainValue);
                DatabaseHandler db=new DatabaseHandler(ViewTeams.this);

                int teamId=(db.getTeamId(parent.getSelectedItem().toString()));
                Log.v("Team Id","Requested"+teamId);
                Teams team=db.getTeamDetails(teamId);
                Log.v("TeamData:", " team.teamName"+team.teamName);

                teamNameText.setText(""+team.teamName);

                teamMatchWonValue.setText(""+team.matchWon);
                Log.v("TeamData:", " team.matchLost"+team.matchLost);
                teamMatchLostValue.setText(""+team.matchLost);
                Log.v("TeamData:", " team.matchDraw"+team.matchDraw);
                teamMatchDrawnValue.setText(""+team.matchDraw);
                int playedMatches=team.matchDraw+team.matchWon+team.matchLost; // Calculating total matches played
                Log.v("TeamData:", " team.matchPlayed"+playedMatches);
                teamMatchPlayedValue.setText(""+playedMatches);

                Log.v("TeamData:", " Loaded from DB");
                Toast.makeText(ViewTeams.this,"Data Loaded from DB",Toast.LENGTH_SHORT).show();

//              newSelection();
               //TextView playerNameText= (TextView)findViewById(R.id.playerNameText);
               // playerNameText.setText(teamList.getSelectedItem().toString());

            }
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

// Preparing list of PlayerNames to be removed

            String teamName=teamList.getSelectedItem().toString();
            Log.v("teamNameCheck","Checking Team Name"+teamList.getSelectedItem().toString());
            int teamId=db.getTeamId(teamName);
            if(db.viewAllPlayers(teamId).size()>0) {


                for (int i = 0; i < db.viewAllPlayers(teamId).size(); i++) {

                    valRemovePlayer.add(db.viewAllPlayers(teamId).get(i));
                    Log.i("Value of remove player list " + i, db.viewAllPlayers(teamId).get(i));
                }
            }

        final StableArrayAdapter addPlayerListAdapter = new StableArrayAdapter(this,
                    android.R.layout.preference_category, valAddPlayer);

            if(valAddPlayer.size()>0) {
                addPlayer_spinner.setAdapter(addPlayerListAdapter);
                addPlayer_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

                        //Code to add update player' Team Id will go here

                        Log.v("AddPlayerData:", " Loaded from DB");
                        Toast.makeText(ViewTeams.this, "Data Loaded from DB", Toast.LENGTH_SHORT).show();


                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            }else{
                Toast.makeText(ViewTeams.this, "No Players Available !!", Toast.LENGTH_SHORT).show();

            }

            final StableArrayAdapter removelayerListAdapter = new StableArrayAdapter(this,
                    android.R.layout.preference_category, valRemovePlayer);

            if(valRemovePlayer.size()>0) {

                removePlayer_spinner.setAdapter(removelayerListAdapter);
                removePlayer_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {


                        Log.v("removePlayerData:", " Loaded from DB");
                        Toast.makeText(ViewTeams.this, "Data Loaded from DB", Toast.LENGTH_SHORT).show();


                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            }else{
                Toast.makeText(ViewTeams.this, "No Players Available !!", Toast.LENGTH_SHORT).show();

            }




            }catch (Exception e){
            Log.v("TeamData:", " Cannot be Loaded from DB"+e);
        }

    }

/*
            public void newSelection(){
                Teams team;

                TextView teamName = (TextView)findViewById(R.id.teamNameText);
                TextView matchPlayed=(TextView)findViewById(R.id.teamMatchPlayedValue);
                TextView matchWon=(TextView)findViewById(R.id.teamMatchWonValue);
                TextView matchLost=(TextView)findViewById(R.id.teamMatchLostValue);
                TextView matchDrawn=(TextView)findViewById(R.id.teamMatchDrawnValue);
                Spinner teamCaptain=(Spinner)findViewById(R.id.teamCaptainValue);
                DatabaseHandler db=new DatabaseHandler(this);

                int teamId=db.getTeamId(teamList.getSelectedItem().toString());
                team=db.getTeamDetails(teamId);

                teamName.setText(team.teamName);
                matchWon.setText(team.matchWon);
                matchLost.setText(team.matchLost);
                matchDrawn.setText(team.matchDraw);
                matchPlayed.setText(team.matchDraw+team.matchWon+team.matchLost);

                Log.v("TeamData:", " Loaded from DB");
                Toast.makeText(ViewTeams.this,"Data Loaded from DB",Toast.LENGTH_SHORT).show();

            }

*/
public void addPlayerButtonClick(View view) {
    Spinner addPlayer=(Spinner)findViewById(R.id.addPlayer_spinner);
    if(addPlayer.getSelectedItemPosition()!=-1) {

        String addPlayerStr = addPlayer.getItemAtPosition(addPlayer.getSelectedItemPosition()).toString();

        //Spinner teamAdd = (Spinner) findViewById(R.id.teamNameSpinner);
       // String teamAddstr = teamAdd.getItemAtPosition(teamAdd.getSelectedItemPosition()).toString();
       // int teamPosition = teamAdd.getSelectedItemPosition();
        String teamAddstr= this.teamList.getSelectedItem().toString();
        int teamPosition=teamList.getSelectedItemPosition();

        Log.v("Checking Team String", "" + teamAddstr);
        Log.v("Checking Player String", "" + addPlayerStr);
        try {
            DatabaseHandler db = new DatabaseHandler(this);
            db.addPlayerInTeam(db.getPlayerId(addPlayerStr), db.getTeamId(teamAddstr));
            Log.v("addPlayerButtonClick", "Player Added to the Team: " + addPlayerStr);
            // customsavedIntanceState.putString("TeamName",""+teamAddstr);
            finish();
            startActivity(getIntent());

            teamList.setVerticalScrollbarPosition(teamPosition);

        } catch (Exception e) {
            Log.v("addPlayerButtonClick", "Threw Exception " + e);

        }
    }else{
        Toast.makeText(ViewTeams.this, "No Players Available !!", Toast.LENGTH_SHORT).show();

    }

}
    public void removePlayerButtonClick(View view) {
        Spinner removePlayer=(Spinner)findViewById(R.id.removePlayer_spinner);
      if(removePlayer.getSelectedItemPosition()!=-1) {
          String removePlayerStr = removePlayer.getItemAtPosition(removePlayer.getSelectedItemPosition()).toString();

          // Spinner teamAdd=(Spinner)findViewById(R.id.teamNameSpinner);
          //String teamAddstr=teamAdd.getItemAtPosition(0).toString();

          //   Log.v("Checking Team String",""+teamAddstr);
          Log.v("Checking Player String", "" + removePlayerStr);
          try {
              DatabaseHandler db = new DatabaseHandler(this);
              db.removePlayerInTeam(db.getPlayerId(removePlayerStr)); //db.getTeamId(teamAddstr));
              Log.v("removePlayerButtonClick", "Player removed to the Team: " + removePlayerStr);

              finish();
              startActivity(getIntent());

          } catch (Exception e) {
              Log.v("removePlayerButtonClick", "Threw Exception " + e);

          }

      }else{
          Toast.makeText(ViewTeams.this, "No Players Available !!", Toast.LENGTH_SHORT).show();

      }
}

        }


     class StableArrayAdapter extends ArrayAdapter<String> {

         HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

         public StableArrayAdapter(Context context, int textViewResourceId,
                                   List<String> objects) {
             super(context, textViewResourceId, objects);
             for (int i = 0; i < objects.size(); ++i) {
                 mIdMap.put(objects.get(i), i);
             }
         }

         @Override
         public long getItemId(int position) {
             String item = getItem(position);
             return mIdMap.get(item);
         }

         @Override
         public boolean hasStableIds() {
             return true;
         }



     }

