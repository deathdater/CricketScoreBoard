package com.example.android.cricketscoreboard.views;

import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.RadioGroup;
import android.widget.SearchView;
import android.widget.Switch;

import com.example.android.cricketscoreboard.DatabaseHandler;
import com.example.android.cricketscoreboard.R;

import java.util.ArrayList;

public class PlayerDetailsActivity extends AppCompatActivity {


    public View newPlayerName,teamName,email;
    public String playerName,playerProfile,playerTeamName,playerEmail;
    public View viewRadioGrp;
   public int checkBatsmen=0,checkBowler=0,checkWK=0;
   public CheckBox batsmen,bowler,wk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_details);
        newPlayerName = (EditText) findViewById(R.id.playerName_edit_text);
        playerName = newPlayerName.toString();
        teamName = (SearchView) findViewById(R.id.playerTeamName_search_text);
        playerTeamName = newPlayerName.toString();
        email = (EditText) findViewById(R.id.playerEmail_edit_text);
        playerEmail = newPlayerName.toString();


        batsmen=(CheckBox)findViewById(R.id.checkbox_batsmen);

        bowler=(CheckBox)findViewById(R.id.checkbox_bowler);
        wk=(CheckBox)findViewById(R.id.checkbox_wicketKeeper);
    }


  public void saveNewPlayer(View view) {

      String message;
      EditText newPlayerName,newplayerEmail;


      newPlayerName = (EditText) findViewById(R.id.playerName_edit_text);
      playerName = newPlayerName.getText().toString();
      newplayerEmail=(EditText)findViewById(R.id.playerEmail_edit_text);
      playerEmail=newplayerEmail.getText().toString();

try {
    if (playerName.isEmpty() ||
        ! (//checkBatsmen==0&&checkBowler==0&&checkWK==0
                   batsmen.isChecked()||bowler.isChecked()||wk.isChecked()
        //  bowler.isChecked()&&batsmen.isChecked()&&wicketKeeper.isChecked()
        ))
             {
        //    playerCount = (NumberPicker) findViewById(R.id.playerCount_number_picker);
        //    numberOfPlayers = playerCount.getValue();
        if (playerName.isEmpty()) {
            message = "Update Player Name & Skills!!";
            Snackbar.make(findViewById(R.id.activity_player_details), message,
                    Snackbar.LENGTH_SHORT)
                    .show();
            Log.v("Player", message);
        } else {
            message = "Update Player skills !!";
            Snackbar.make(findViewById(R.id.activity_player_details), message,
                    Snackbar.LENGTH_SHORT)
                    .show();
            Log.v("Player", message);
        }
    } else {
       /* Player Profile Logic **** NOT  USED ****
        if(checkBatsmen==1&&checkBowler==1&&checkWK==1) {       //111
            playerProfile = "YYY";

            Log.v("Player Skill", playerProfile);
        }
        else if(checkBatsmen==0&&checkBowler==1&&checkWK==1){   //011
            playerProfile = "NYY";
            Log.v("Player Skill", playerProfile);
        }
        else if(checkBatsmen==0&&checkBowler==0&&checkWK==1){   //001
            playerProfile = "NNY";
            Log.v("Player Skill", playerProfile);
        }
        else if(checkBatsmen==1&&checkBowler==0&&checkWK==0){   //100
            playerProfile = "YNN";
            Log.v("Player Skill", playerProfile);
        }
        else if(checkBatsmen==1&&checkBowler==1&&checkWK==0){   //110
            playerProfile = "YYN";
            Log.v("Player Skill", playerProfile);
        }
        else if(checkBatsmen==1&&checkBowler==0&&checkWK==1){   //101
            playerProfile = "YNY";
            Log.v("Player Skill", playerProfile);
        }
        else if(checkBatsmen==0&&checkBowler==1&&checkWK==0){   //010
            playerProfile = "NYN";
            Log.v("Player Skill", playerProfile);
        }
    */
        // Checking if the player is already present to DB
        DatabaseHandler db = new DatabaseHandler(getApplicationContext());
        ArrayList playerList= db.viewAllPlayers();
        if(playerList.contains(playerName)){
            Log.v("Player Already Exist", playerName);
            Snackbar.make(findViewById(R.id.activity_player_details), playerName+"\'s Name Already Exist \nTry A Different Name!!",
                    Snackbar.LENGTH_SHORT)
                    .show();

        }else{
            db.addPlayerDetails("aditya.juet@gmail.com",playerName,checkBatsmen,checkBowler,checkWK);
            Log.v("Player Added", playerName);
            Snackbar.make(findViewById(R.id.activity_player_details), playerName+" Added to Player List",
                    Snackbar.LENGTH_SHORT)
                    .show();
        }

        //  playerCount = (NumberPicker) findViewById(R.id.playerCount_number_picker);
        //  numberOfPlayers = playerCount.getValue();
        //   message = "Your Team Name is " + teamName + " \n With minimum number players is " + numberOfPlayers;
        // Snackbar.make(findViewById(R.id.activity_team_details), message,
        //          Snackbar.LENGTH_SHORT)
        //        .show();
    }
}catch (Exception e){

    Log.v("ExceptionCaught",e.getMessage());
}

    }
    public void onCheckBatsmen(View view){

      // viewCheckBat = (CheckBox) findViewById(R.id.checkbox_batsmen);
           if(checkBatsmen==0){

            viewRadioGrp = findViewById(R.id.batsmenStyle_radioGrp);
            viewRadioGrp.setVisibility(View.VISIBLE);
               checkBatsmen=1;
        } else{
           viewRadioGrp = findViewById(R.id.batsmenStyle_radioGrp);
           viewRadioGrp.setVisibility(View.INVISIBLE);
               checkBatsmen=0;
        }

    }
    public void onCheckBowler(View view){

       // viewCheckBat = (CheckBox) findViewById(R.id.checkbox_batsmen);
        if(checkBowler==0){

            viewRadioGrp = findViewById(R.id.bowlerStyle_radioGrp);
            viewRadioGrp.setVisibility(View.VISIBLE);
            checkBowler=1;
        } else{
            viewRadioGrp = findViewById(R.id.bowlerStyle_radioGrp);
            viewRadioGrp.setVisibility(View.INVISIBLE);
            checkBowler=0;
        }

    }
    public void onCheckWK(View view){
    if(checkWK==0){
        checkWK=1;
    }else{
        checkWK=0;
    }
    }
}

