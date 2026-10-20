package com.example.android.cricketscoreboard.views;

import android.content.Intent;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.example.android.cricketscoreboard.R;
import com.example.android.cricketscoreboard.ScoreMan;
import com.example.android.cricketscoreboard.UiPreferences;
import com.example.android.cricketscoreboard.UiSettingsDialog;

import java.util.ArrayList;

public class ScorerPadActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    ScoreMan scoreMan=new ScoreMan();
    ArrayList al;
    TextView batting_score,wickets,overCount,ballCount,thisOver;
    RadioGroup runs,wideNoBall,strikerNonStriker;
    RadioButton runsInBall,wideNoBallInBall,zeroRun,wideBall,noBall;
    int score=0,balls=0,overs=0,runSelection,wideNoBallSelection,count=0;
    int wicketDown=0;
    int endOfOver,a,b,c;
    String runText,wideNoBallInBallText,thisOverStr;
    char runCheck;
    CheckBox freeHit,outCheckBox,undoBall;
    Bundle savedInstanceState;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scorer_pad);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        String batting_score_editText=findViewById(R.id.Batting_score_editText).toString();


      /*  FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
*/
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Close the navigation drawer on back instead of leaving the screen.
        final OnBackPressedCallback drawerBackCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                drawer.closeDrawer(GravityCompat.START);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, drawerBackCallback);
        drawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                drawerBackCallback.setEnabled(true);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                drawerBackCallback.setEnabled(false);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.scorer_pad, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            UiSettingsDialog.show(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            startActivity(new Intent(this, ScorerTabActivity.class));
        } else if (id == R.id.nav_gallery) {
            startActivity(new Intent(this, TeamDetails.class));
        } else if (id == R.id.nav_slideshow) {
            startActivity(new Intent(this, PlayerDetailsActivity.class));
        } else if (id == R.id.nav_manage) {
            UiSettingsDialog.show(this);
        } else if (id == R.id.nav_share) {
            startActivity(new Intent(this, ScheduleMatches.class));
        } else if (id == R.id.nav_send) {
            finish();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

public void onBallPress(View view) {
    if (view != null && UiPreferences.get(this).isScorerHapticsEnabled()) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }

//Initialising all the parameter
    undoBall = (CheckBox) findViewById(R.id.undo_checkBox);
    wickets = (TextView) findViewById(R.id.Wickets_editText);

    thisOver = (TextView) findViewById(R.id.thisOverValue_text_view);

    wideBall = (RadioButton) findViewById(R.id.radioBtn_WideBall);
    noBall = (RadioButton) findViewById(R.id.radioBtn_NoBall);

    batting_score = (TextView) findViewById(R.id.Batting_score_editText);
    overCount = (TextView) findViewById(R.id.Bowling_overs_editText);
    ballCount = (TextView) findViewById(R.id.Bowling_oversBall_editText);

    runs = (RadioGroup) findViewById(R.id.runsScored_radioGrp);
    runSelection = runs.getCheckedRadioButtonId();
    runsInBall = (RadioButton) findViewById(runSelection);
    runText = (String) runsInBall.getText();
    runCheck = runText.charAt(0);

    outCheckBox = (CheckBox) findViewById(R.id.out_CheckBox);

    if (!undoBall.isChecked()) {

        switch (runCheck) {
            case '0':
                runSelection = 0;
                break;
            case '1':
                runSelection = 1;
                break;
            case '2':
                runSelection = 2;
                break;
            case '3':
                runSelection = 3;
                break;
            case '4':
                runSelection = 4;
                break;
            case '5':
                runSelection = 5;
                break;
            case '6':
                runSelection = 6;
                break;
        }
        if (endOfOver == 1) {
            thisOver.setText("");
            endOfOver = 0;
        }

//    batting_score.setInputType(InputType.TYPE_CLASS_NUMBER);
        if (wideBall.isChecked() || noBall.isChecked()) {
            wideNoBall = (RadioGroup) findViewById(R.id.wideBall_NoBall_radioGrp);
            wideNoBallSelection = wideNoBall.getCheckedRadioButtonId();
            wideNoBallInBall = (RadioButton) findViewById(wideNoBallSelection);
            wideNoBallInBallText = (String) wideNoBallInBall.getText();

            if (wideNoBallInBallText.contains("Wide")) {
                Log.i("WIDE", "onBallPress: " + runSelection);
                score = score + runSelection + 1;
                thisOverStr = "" + runSelection + "wd";
                thisOver.append(";");
                thisOver.append(thisOverStr);
               scoreMan.scoreCommentry= scoreMan.scoreCommentry.concat(";"+thisOverStr);
                if (balls != 6) {
                    count = count + 1;
                }
  /*  if (balls != 6) {

        balls = balls + 1;
        if (balls == 6) {
            overs = overs + 1;
            balls = 0;
        }
    }*/
            } else if (wideNoBallInBallText.contains("No")) {

                Log.i("NO BALL", "onBallPress: " + runSelection);
                score = score + runSelection + 1;
                thisOverStr = "" + runSelection + "nb";
                thisOver.append(";");
                thisOver.append(thisOverStr);
                scoreMan.scoreCommentry= scoreMan.scoreCommentry.concat(";"+thisOverStr);
                if (balls != 6) {
                    count = count + 1;
                }
  /*  if (balls != 6) {

        balls = balls + 1;
        if (balls == 6) {
            overs = overs + 1;
            balls = 0;
        }
    }*/
            }
        } else {
            Log.i("runs", "onBallPress: " + runSelection);
            score = score + runSelection;
            if (balls == 0 && count == 0) {
                thisOver.setText("");
            }
            if (balls != 6) {
                count = count + 1;
            }
            if (balls != 6) {

                thisOverStr = "" + runSelection;
                balls = balls + 1;
                thisOver.append(";");
                thisOver.append(thisOverStr);
                scoreMan.scoreCommentry=  scoreMan.scoreCommentry.concat(";"+thisOverStr);
            }
            if (balls == 6) {
                overs = overs + 1;
                balls = 0;
                count = 0;
                endOfOver = 1;
                thisOverStr = "||";
                //thisOver.append("||);
                thisOver.append(thisOverStr);
                scoreMan.scoreCommentry= scoreMan.scoreCommentry.concat(thisOverStr);


            }
        }

        scoreMan.recordScore(score);
        Log.i("Score Array", "onBallPress: " + scoreMan.score);
        Log.i("Score Commentary", "onBallPress: " + scoreMan.scoreCommentry);
        overCount.setText("" + overs);
        ballCount.setText("" + balls);
        batting_score.setText("" + score);


        reset(view);

        // batting_score.setTransformationMethod(null);

        Log.i("abc", "onBallPress: " + score);

        String message = "The Score is " + score;
        Snackbar.make(findViewById(R.id.activity_scorer_pad), message,
                Snackbar.LENGTH_SHORT)
                .show();
    } else {

        if (overs == 0 && balls == 0 && !(scoreMan.scoreCommentry.endsWith("wd") || scoreMan.scoreCommentry.endsWith("nb"))) {
            String message = "No Records to Undo !!";
            Snackbar.make(findViewById(R.id.activity_scorer_pad), message,
                    Snackbar.LENGTH_SHORT)
                    .show();
        } else {
            scoreMan.undoBall();
            score = scoreMan.battingScore;
            batting_score.setText("" + score);
            Log.i("Score after Undo", "onBallPress: " + score);

            if (balls != 0 && !(scoreMan.scoreCommentry.endsWith("wd") || scoreMan.scoreCommentry.endsWith("nb"))) {
                //Commentary Modification
                 b=scoreMan.scoreCommentry.lastIndexOf("||");
                 c=scoreMan.scoreCommentry.indexOf(":");
                 a =scoreMan.scoreCommentry.lastIndexOf(";");
                scoreMan.scoreCommentry=scoreMan.scoreCommentry.substring(c,a);


                //(Current)this over text modification
              //  thisOverStr = thisOver.getText().toString();
              //  a = thisOverStr.lastIndexOf(";");
                if(overs==0){
                    thisOverStr=scoreMan.scoreCommentry.substring(c,a);
                }else{

                    thisOverStr=scoreMan.scoreCommentry.substring(b,a);
                }
                thisOver.setText("");
                thisOver.append(thisOverStr);

                balls = balls - 1;

            } else {


                if (overs==0&&(thisOver.getText().toString().endsWith("wd") || thisOver.getText().toString().endsWith("nb"))) {

                    //Commentary Modification
                    b=scoreMan.scoreCommentry.lastIndexOf("||");
                    c=scoreMan.scoreCommentry.indexOf(":");
                    a =scoreMan.scoreCommentry.lastIndexOf(";");
                    scoreMan.scoreCommentry=scoreMan.scoreCommentry.substring(c,a);


                 //   thisOverStr = thisOver.getText().toString();
                 //    a = thisOverStr.lastIndexOf(";");

                    if(overs==0){
                        thisOverStr=scoreMan.scoreCommentry.substring(c,a);
                    }else{

                        thisOverStr=scoreMan.scoreCommentry.substring(b,a);
                    }
                    thisOver.setText("");

                    thisOver.append(thisOverStr);


                }

                if (overs != 0 || thisOver.getText().toString().contains("||")) {
                    if (thisOver.getText().toString().endsWith("wd") || thisOver.getText().toString().endsWith("nb")) {

                        //Commentary Modification
                        b=scoreMan.scoreCommentry.lastIndexOf("||");
                        c=scoreMan.scoreCommentry.indexOf(":");
                        a =scoreMan.scoreCommentry.lastIndexOf(";");
                        scoreMan.scoreCommentry=scoreMan.scoreCommentry.substring(c,a);


                        //   thisOverStr = thisOver.getText().toString();
                        //    a = thisOverStr.lastIndexOf(";");

                        if(overs==0){
                            thisOverStr=scoreMan.scoreCommentry.substring(c,a);
                        }else{

                            thisOverStr=scoreMan.scoreCommentry.substring(b,a);
                        }
                        thisOver.setText("");

                        thisOver.append(thisOverStr);
                    //    overs = overs - 1;
                    //    balls = 5;

                    }else {


                        //Commentary Modification
                        a = scoreMan.scoreCommentry.lastIndexOf(";");
                        c = scoreMan.scoreCommentry.indexOf(":");
                        scoreMan.scoreCommentry = scoreMan.scoreCommentry.substring(c, a);
                        b = scoreMan.scoreCommentry.lastIndexOf("||");
                        overs = overs - 1;
                        balls = 5;
                        //      thisOverStr = thisOver.getText().toString();
                        //   a = thisOverStr.lastIndexOf(";");
                        //      thisOverStr = thisOverStr.substring(0, a);
                        if (overs == 0) {
                            thisOverStr = scoreMan.scoreCommentry.substring(c, a);
                        } else {

                            thisOverStr = scoreMan.scoreCommentry.substring(b, a);
                        }


                        thisOver.setText("");
                        thisOver.append(thisOverStr);
                    }

                }

            }

            overCount.setText(("" + overs));
            ballCount.setText("" + balls);
            String message = "Undo is completed";
            Log.i("Score Commentary", "onBallPress: " + scoreMan.scoreCommentry);
            Snackbar.make(findViewById(R.id.activity_scorer_pad), message,
                    Snackbar.LENGTH_SHORT)
                    .show();
        }

    }
}



    public void onNoBallCheck(View view){
        freeHit=(CheckBox)findViewById(R.id.freeHit_checkBox);
        freeHit.setEnabled(true);
    }
    public void onWideBallCheck(View view){
        freeHit=(CheckBox)findViewById(R.id.freeHit_checkBox);
        freeHit.setChecked(false);
        freeHit.setEnabled(false);

    }
    public void onOutCheckBoxChecked(View v){

       outCheckBox=(CheckBox) findViewById(R.id.out_CheckBox);

  strikerNonStriker=(RadioGroup)findViewById(R.id.Striker_NonStriker_radioGrp);

        if(outCheckBox.isChecked()){

            strikerNonStriker.setVisibility(View.VISIBLE);

        }else{

            strikerNonStriker.setVisibility(View.INVISIBLE);
        }
    }

    public void reset(View v) {
        outCheckBox=(CheckBox) findViewById(R.id.out_CheckBox);

        wideBall=(RadioButton)findViewById(R.id.radioBtn_WideBall);
        noBall=(RadioButton)findViewById(R.id.radioBtn_NoBall);

        freeHit = (CheckBox) findViewById(R.id.freeHit_checkBox);
        zeroRun = (RadioButton) findViewById(R.id.radioBtn_ZeroRun);
        zeroRun.setChecked(true);
        if (wideBall.isChecked()||noBall.isChecked()||outCheckBox.isChecked()) {
            wideNoBall.clearCheck();
            outCheckBox.setChecked(false);

       //     wideBall.setChecked(false);
         //   noBall.setChecked(false);
        }
        if (freeHit.isEnabled()) {
                freeHit.setChecked(false);
                freeHit.setEnabled(false);

        }
    }

}

  //  System.out.print(a);
/* Just to watch and Learn
    playerCount = (NumberPicker) findViewById(R.id.playerCount_number_picker);
    playerCount.setMinValue(1);
    playerCount.setMaxValue(16);
    numberOfPlayers = playerCount.getValue();
    newTeamview = findViewById(R.id.teamName_edit_text);
    teamName = newTeamview.toString();
*/




