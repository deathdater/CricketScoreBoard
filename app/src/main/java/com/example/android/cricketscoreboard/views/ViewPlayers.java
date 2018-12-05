package com.example.android.cricketscoreboard.views;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.cricketscoreboard.DatabaseHandler;
import com.example.android.cricketscoreboard.R;
import com.example.android.cricketscoreboard.entity.Players;
import com.example.android.cricketscoreboard.entity.Teams;

import java.util.HashMap;
import java.util.List;

public class ViewPlayers extends AppCompatActivity {
    public String playerName, playerProfile, playerTeamName, playerEmail;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_players);

        //  final ListView listview = (ListView) findViewById(R.id.teams_list);
        final Spinner playerListSpinner = (Spinner) findViewById(R.id.playerNameSpinner);

        RadioGroup playerStats_radioGrp_edit = (RadioGroup) findViewById(R.id.playerStats_radioGrp_edit);
        RadioButton radioBtn_Batsmen_edit = (RadioButton) playerStats_radioGrp_edit.findViewById(R.id.radioBtn_Batsmen_edit);
        // RadioButton radioBtn_Bowler_edit=(RadioButton)playerStats_radioGrp_edit.findViewById(radioBtn_Bowler_edit);
        playerStats_radioGrp_edit.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                RadioButton radioBtnBatBowl_edit = (RadioButton) radioGroup.findViewById(i);

                boolean isChecked = radioBtnBatBowl_edit.isChecked();
                if (radioBtnBatBowl_edit.getText().equals("Batsmen")) {
                    ScrollView player_details_Batsmen_Table = (ScrollView) findViewById((R.id.player_details_Batsmen_Table));
                    player_details_Batsmen_Table.setVisibility(View.VISIBLE);
                    ScrollView player_details_Bowler_Table = (ScrollView) findViewById(R.id.player_details_Bowler_Table);
                    player_details_Bowler_Table.setVisibility(View.INVISIBLE);

                } else {
                    ScrollView player_details_Batsmen_Table = (ScrollView) findViewById((R.id.player_details_Batsmen_Table));
                    player_details_Batsmen_Table.setVisibility(View.INVISIBLE);
                    ScrollView player_details_Bowler_Table = (ScrollView) findViewById(R.id.player_details_Bowler_Table);
                    player_details_Bowler_Table.setVisibility(View.VISIBLE);

                }

            }
        });


        CheckBox checkbox_batsmen_edit = (CheckBox) findViewById(R.id.checkbox_batsmen_edit);
        checkbox_batsmen_edit.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                RadioGroup batsmenStyle_radioGrp_edit = (RadioGroup) findViewById(R.id.batsmenStyle_radioGrp_edit);
                if(isChecked){
                batsmenStyle_radioGrp_edit.setVisibility(View.VISIBLE);
            }else{
                    batsmenStyle_radioGrp_edit.setVisibility(View.INVISIBLE);
                }
            }
        });
        CheckBox checkbox_bowler_edit = (CheckBox) findViewById(R.id.checkbox_bowler_edit);
        checkbox_bowler_edit.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                RadioGroup bowlerStyle_radioGrp_edit = (RadioGroup) findViewById(R.id.bowlerStyle_radioGrp_edit);
                if(isChecked){
                bowlerStyle_radioGrp_edit.setVisibility(View.VISIBLE);}
                else{
                    bowlerStyle_radioGrp_edit.setVisibility(View.INVISIBLE);
                }
            }
        });


            //String[] values = new String[] { "Android", "iPhone", "WindowsMobile",
            //       "Blackberry", "WebOS", "Ubuntu", "Windows7", "Max OS X",
            //      "Linux", "OS/2", "Ubuntu", "Windows7", "Max OS X", "Linux",
            //     "OS/2", "Ubuntu", "Windows7", "Max OS X", "Linux", "OS/2",
            //    "Android", "iPhone", "WindowsMobile" };
            DatabaseHandler db = new DatabaseHandler(this);
            List<String> val = db.viewAllPlayers();

        for(
            int i = 0;db.viewAllPlayers().

            get(i).

            isEmpty();

            i++)

            {

                val.add(db.viewAllPlayers().get(i));
                Log.v("Value of Player element " + i, val.get(i));
            }


            //  while(db.getAllTeamNames().contains('a')) {
            //   val.add(db.getAllTeamNames().get(i));
            //  Log.i("Value of element "+i,val.get(i));
            // i++;

            //System.out.println("lemme check"+val);
            // final ArrayList<String> list = new ArrayList<>();
            // for (int i = 0; i < values.length; ++i) {
            //     list.add(values[i]);
            // }
            final StableArrayAdapter adapter = new StableArrayAdapter(this,
                    android.R.layout.preference_category, val);
        playerListSpinner.setAdapter(adapter);

        playerListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()

            {
                public void onItemSelected (AdapterView < ? > parent, View view,int pos, long id){
                // Object item = parent.getItemAtPosition(pos);
                TextView playerNameText = (TextView) findViewById(R.id.playerNameText);
                playerNameText.setText(playerListSpinner.getSelectedItem().toString());
                TextView playerEmailText = (TextView) findViewById(R.id.playerEmailText);
                TextView playerDOBText = (TextView) findViewById(R.id.playerDOBText);
                TextView teamNameText = (TextView) findViewById(R.id.teamNameText);
                TextView playerMatchPlayedValue=(TextView)findViewById(R.id.playerMatchPlayedValue);
                TextView playerWicketsValue=(TextView)findViewById(R.id.playerWicketsValue);
                TextView playerBallsFacedValue=(TextView)findViewById(R.id.playerBallsFacedValue);
                TextView bowlerEconomyValue=(TextView)findViewById(R.id.bowlerEconomyValue);
                TextView playerWidesValue=(TextView)findViewById(R.id.playerWidesValue);
                TextView playerNoBallValue=(TextView)findViewById(R.id.playerNoBallValue);
                TextView playerTeamTwoHundred=(TextView)findViewById(R.id.playerTeamTwoHundred);
                TextView playerTeamHundred=(TextView)findViewById(R.id.playerTeamHundred);
                TextView playerTeamFifty=(TextView)findViewById(R.id.playerTeamFifty);
                TextView playerTeamTotalBalls=(TextView)findViewById(R.id.playerTeamTotalBalls);
                TextView playerTeamTotalRun=(TextView)findViewById(R.id.playerTeamTotalRun);
                TextView playerTeamMatchPlayedValue=(TextView)findViewById(R.id.playerTeamMatchPlayedValue);
                CheckBox checkbox_wicketKeeper_edit=(CheckBox)findViewById(R.id.checkbox_wicketKeeper_edit) ;
                    checkbox_wicketKeeper_edit.setChecked(false);

                CheckBox checkbox_batsmen_edit=(CheckBox)findViewById(R.id.checkbox_batsmen_edit);
                    checkbox_batsmen_edit.setChecked(false);
                CheckBox checkbox_bowler_edit=(CheckBox)findViewById(R.id.checkbox_bowler_edit);
                    checkbox_bowler_edit.setChecked(false);
                DatabaseHandler db=new DatabaseHandler(ViewPlayers.this);

                    int playerId=(db.getPlayerId(parent.getSelectedItem().toString()));
                    Log.v("Player Id","Requested: "+playerId);
                    Players player=db.getPlayerDetails(playerId);
                    Log.v("PlayerData:", " Player.playerName: "+player.playerName);

                    playerNameText.setText(""+player.playerName);
                    playerDOBText.setText(""+player.playerDOB);
                    playerEmailText.setText(""+player.playerEmail);
                    if(player.playerTeamId!=0){
                        teamNameText.setText(""+db.getTeamName(player.playerTeamId));
                    }else{
                        teamNameText.setText("");
                    }
                    playerMatchPlayedValue.setText(""+player.playerMatchPlayed);
                    playerWicketsValue.setText(""+player.playerWicketTaken);
                    playerBallsFacedValue.setText(""+player.playerBallFaced);
                    if(player.playerBallFaced==0){
                        player.bowlingEconomy=0;
                    }else {
                        int economyValue = player.playerRunsGiven / player.playerBallFaced;
                        player.bowlingEconomy = economyValue;
                    }
                    bowlerEconomyValue.setText(""+player.bowlingEconomy);
                    playerWidesValue.setText(""+player.playerWidesCount);
                    playerNoBallValue.setText(""+player.playerNoBallCount);
                    playerTeamTwoHundred.setText(""+player.playerTwoHundred);
                    playerTeamHundred.setText(""+player.playerHundred);
                    playerTeamFifty.setText(""+player.playerFifty);
                    playerTeamTotalBalls.setText(""+player.playerTotalRuns);
                    playerTeamTotalRun.setText(""+player.playerTotalRuns);
                    playerTeamMatchPlayedValue.setText(""+player.playerMatchPlayed);

                    if(player.playerWK==1){

                      checkbox_wicketKeeper_edit.setChecked(player.playerWK==1);

                    }
                    if(player.playerBatsmen==1){

                        checkbox_batsmen_edit.setChecked(player.playerBatsmen==1);

                    }
                    if(player.playerBowler==1){

                        checkbox_bowler_edit.setChecked(player.playerBowler==1);

                    }





                    Log.v("TeamData:", " Loaded from DB");
                    Toast.makeText(ViewPlayers.this,"Data Loaded from DB",Toast.LENGTH_SHORT).show();

            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

    }


    }

