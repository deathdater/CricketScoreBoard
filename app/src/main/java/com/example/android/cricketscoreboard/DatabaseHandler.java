package com.example.android.cricketscoreboard;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.Image;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.android.cricketscoreboard.entity.Grounds;
import com.example.android.cricketscoreboard.entity.Matches;
import com.example.android.cricketscoreboard.entity.Players;
import com.example.android.cricketscoreboard.entity.Rules;
import com.example.android.cricketscoreboard.entity.Schedules;
import com.example.android.cricketscoreboard.entity.Teams;
import com.example.android.cricketscoreboard.entity.Users;


/**
 * Created by Deathdater on 2/13/17.
 */

public class DatabaseHandler extends SQLiteOpenHelper {
    public String USERID="aditya.juet@gmail.com";
    private static final int DATABASE_VERSION = 1;
    //Declaring Database Name
    private SQLiteDatabase db;
    private static final String DATABASE_NAME = "cricketScore_DB";
    Teams team = new Teams();
    Players player=new Players();

    //Declaring all Tables

    //Declaring table -RULES_DETAILS
    private static final String TABLE_RULES_DETAILS = "RULES_DETAILS";

    //Declaring Columns-- RULES_DETAILS
    private static final String COLUMN_RULE_ID = "RULE_ID";
    private static final String COLUMN_RULE_DESCRIPTION = "RULE_DESCRIPTION";
    private static final String COLUMN_RULE_STATUS = "RULE_STATUS";

    //Rules
    String CREATE_TABLE_RULES_DETAILS = "CREATE TABLE " + TABLE_RULES_DETAILS + "("

            + COLUMN_RULE_ID + " INTEGER,"
            + COLUMN_RULE_DESCRIPTION + " TEXT,"
            + COLUMN_RULE_STATUS + " INTEGER );";


    //Declaring table -MATCH_DETAILS
    public static final String TABLE_MATCH_DETAILS = "MATCH_DETAILS";

    //Declaring Columns-- MATCH_SUMMARY_DETAILS
    private static final String COLUMN_MATCH_USER_ID = "USER_EMAIL_ID";
    private static final String COLUMN_MATCH_ID = "MATCH_ID";
    private static final String COLUMN_MATCH_PLAYER_ID = "MATCH_PLAYER_ID";
    private static final String COLUMN_MATCH_PLAYER_TOTAL_RUNS = "MATCH_PLAYER_TOTAL_RUNS";
    private static final String COLUMN_MATCH_PLAYER_ONES = "MATCH_PLAYER_ONES";
    private static final String COLUMN_MATCH_PLAYER_TWOS = "MATCH_PLAYER_TWOS";
    private static final String COLUMN_MATCH_PLAYER_THREES = "MATCH_PLAYER_THREES";
    private static final String COLUMN_MATCH_PLAYER_FOURS = "MATCH_PLAYER_FOURS";
    private static final String COLUMN_MATCH_PLAYER_FIVES = "MATCH_PLAYER_FIVES";
    private static final String COLUMN_MATCH_PLAYER_SIXES = "MATCH_PLAYER_SIXES";
    private static final String COLUMN_MATCH_PLAYER_BALLS_FACED = "MATCH_PLAYER_BALLS_FACED";
    private static final String COLUMN_MATCH_PLAYER_OVERS_BOWLED = "MATCH_PLAYER_OVERS_BOWLED";
    private static final String COLUMN_MATCH_PLAYER_WICKETS = "MATCH_PLAYER_WICKETS";
    private static final String COLUMN_MATCH_PLAYER_MAIDEN = "MATCH_PLAYER_MAIDEN";
    private static final String COLUMN_MATCH_PLAYER_NOBALL = "MATCH_PLAYER_NOBALL";
    private static final String COLUMN_MATCH_PLAYER_WIDES = "MATCH_PLAYER_WIDES";
    private static final String COLUMN_MATCH_PLAYER_RUNS_GIVEN = "MATCH_PLAYER_RUNS_GIVEN";
    private static final String COLUMN_MATCH_PLAYER_CATCHES = "MATCH_RUNS_GIVEN";
    private static final String COLUMN_MATCH_PLAYER_STATUS = "MATCH_PLAYER_STATUS";


    //Match Details
    public String CREATE_TABLE_MATCH_DETAILS = "CREATE TABLE " + TABLE_MATCH_DETAILS + "("
            + COLUMN_MATCH_USER_ID
            + COLUMN_MATCH_ID + " INTEGER,"
            + COLUMN_MATCH_PLAYER_ID + " INTEGER,"
            + COLUMN_MATCH_PLAYER_TOTAL_RUNS + " INTEGER,"
            + COLUMN_MATCH_PLAYER_ONES + " INTEGER,"
            + COLUMN_MATCH_PLAYER_TWOS + " INTEGER,"
            + COLUMN_MATCH_PLAYER_THREES + " INTEGER,"
            + COLUMN_MATCH_PLAYER_FOURS + " INTEGER,"
            + COLUMN_MATCH_PLAYER_FIVES + " INTEGER,"
            + COLUMN_MATCH_PLAYER_SIXES + " INTEGER,"
            + COLUMN_MATCH_PLAYER_BALLS_FACED + " INTEGER,"
            + COLUMN_MATCH_PLAYER_OVERS_BOWLED + " REAL,"
            + COLUMN_MATCH_PLAYER_WICKETS + " INTEGER,"
            + COLUMN_MATCH_PLAYER_MAIDEN + " INTEGER,"
            + COLUMN_MATCH_PLAYER_NOBALL + " INTEGER,"
            + COLUMN_MATCH_PLAYER_WIDES + " INTEGER,"
            + COLUMN_MATCH_PLAYER_RUNS_GIVEN + " INTEGER,"
            + COLUMN_MATCH_PLAYER_CATCHES + " INTEGER,"
            + COLUMN_MATCH_PLAYER_STATUS + " TEXT );";

    //Declaring table -USER_DETAILS

    public static final String TABLE_USER_DETAILS = "USER_DETAILS";

    //Declaring Columns-- USER_DETAILS
    public static final String COLUMN_USER_ID = "USER_EMAIL_ID";
    public static final String COLUMN_USER_PASSWORD = "USER_PASSWORD";
    public static final String COLUMN_USER_DOB = "USER_DOB";
    public static final String USERID_UNIQUE = "CONSTRAINT_USER_EMAIL_ID";

    //User Details
    public String CREATE_TABLE_USER_DETAILS = "CREATE TABLE " + TABLE_USER_DETAILS + "("
            + COLUMN_USER_ID + " TEXT,"
            + COLUMN_USER_PASSWORD + " TEXT,"
            + COLUMN_USER_DOB + " TEXT,"
            + " CONSTRAINT " + USERID_UNIQUE + " UNIQUE ( " + COLUMN_USER_ID + "));";


    //Declaring table -PLAYER_DETAILS
    private static final String TABLE_PLAYER_DETAILS = "PLAYER_DETAILS";

    //Declaring Columns-- PLAYER_DETAILS
    private static final String COLUMN_PLAYER_USER_ID = "USER_EMAIL_ID";
    private static final String COLUMN_PLAYER_ID = "PLAYER_ID";
    private static final String COLUMN_PLAYER_NAME = "PLAYER_NAME";
    private static final String COLUMN_PLAYER_EMAIL = "PLAYER_EMAIL";
    private static final String COLUMN_PLAYER_BATSMAN = "PLAYER_BATSMAN";
    private static final String COLUMN_PLAYER_BOWLER = "PLAYER_BOWLER";
    private static final String COLUMN_PLAYER_WICKETKEEPER = "PLAYER_WICKETKEEPER";
    private static final String COLUMN_PLAYER_IMAGE = "PLAYER_IMAGE";
    private static final String COLUMN_PLAYER_DOB = "PLAYER_DOB";
    private static final String COLUMN_PLAYER_TOTAL_RUNS = "PLAYER_TOTAL_RUNS";
    private static final String COLUMN_PLAYER_FIFTY = "PLAYER_FIFTY";
    private static final String COLUMN_PLAYER_HUNDRED = "PLAYER_HUNDRED";
    private static final String COLUMN_PLAYER_TWO_HUNDRED = "PLAYER_TWO_HUNDRED";
    private static final String COLUMN_PLAYER_BALLS_FACED = "PLAYER_BALLS_FACED";
    private static final String COLUMN_PLAYER_MATCH_PLAYED = "PLAYER_MATCH_PLAYED";
    private static final String COLUMN_PLAYER_OVERS_BOWLED = "PLAYER_OVERS_BOWLED";
    private static final String COLUMN_PLAYER_WICKETS_TAKEN = "PLAYER_WICKETS_TAKEN";
    private static final String COLUMN_PLAYER_MAIDEN_BOWLED = "PLAYER_MAIDEN_BOWLED";
    private static final String COLUMN_PLAYER_WIDES_COUNT = "PLAYER_WIDES_COUNT";
    private static final String COLUMN_PLAYER_NOBALL_COUNT = "PLAYER_NOBALL_COUNT";
    private static final String COLUMN_PLAYER_RUNS_GIVEN = "PLAYER_RUNS_GIVEN";
    private static final String COLUMN_PLAYERS_CATCHES = "PLAYERS_CATCHES";
    private static final String COLUMN_PLAYER_TEAM_ID = "PLAYER_TEAM_ID";

    //Player Details
    public String CREATE_TABLE_PLAYER_DETAILS = "CREATE TABLE " + TABLE_PLAYER_DETAILS + "("
            + COLUMN_PLAYER_USER_ID + " TEXT,"
            + COLUMN_PLAYER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_PLAYER_NAME + " TEXT,"
            + COLUMN_PLAYER_EMAIL + " TEXT,"
            + COLUMN_PLAYER_BATSMAN + " INTEGER,"
            + COLUMN_PLAYER_BOWLER + " INTEGER,"
            + COLUMN_PLAYER_WICKETKEEPER + " INTEGER,"
            + COLUMN_PLAYER_IMAGE + " BLOB,"
            + COLUMN_PLAYER_DOB + " INTEGER,"
            + COLUMN_PLAYER_TOTAL_RUNS + " INTEGER,"
            + COLUMN_PLAYER_FIFTY + " INTEGER,"
            + COLUMN_PLAYER_HUNDRED + " INTEGER,"
            + COLUMN_PLAYER_TWO_HUNDRED + " INTEGER,"
            + COLUMN_PLAYER_BALLS_FACED + " INTEGER,"
            + COLUMN_PLAYER_MATCH_PLAYED + " INTEGER,"
            + COLUMN_PLAYER_OVERS_BOWLED + " INTEGER,"
            + COLUMN_PLAYER_WICKETS_TAKEN + " INTEGER,"
            + COLUMN_PLAYER_MAIDEN_BOWLED + " INTEGER,"
            + COLUMN_PLAYER_WIDES_COUNT + " INTEGER,"
            + COLUMN_PLAYER_NOBALL_COUNT + " INTEGER,"
            + COLUMN_PLAYER_RUNS_GIVEN + " INTEGER,"
            + COLUMN_PLAYERS_CATCHES + " INTEGER,"
            + COLUMN_PLAYER_TEAM_ID + " INTEGER );";


    //Declaring table -TEAM_DETAILS
    private static final String TABLE_TEAM_DETAILS = "TEAM_DETAILS";

    //Declaring Columns-- TEAM_DETAILS
    private static final String COLUMN_TEAM_USER_ID = "USER_EMAIL_ID";
    private static final String COLUMN_TEAM_ID = "TEAM_ID";
    private static final String COLUMN_TEAM_NAME = "TEAM_NAME";
    private static final String COLUMN_TEAM_NUMBER_OF_PLAYERS = "TEAM_NUMBER_OF_PLAYERS";
    private static final String COLUMN_TEAM_LOGO = "TEAM_LOGO";
    private static final String COLUMN_TEAM_MATCHES_WON = "TEAM_MATCHES_WON";
    private static final String COLUMN_TEAM_MATCHES_LOST = "TEAM_MATCHES_LOST";
    private static final String COLUMN_TEAM_MATCHES_DRAW = "TEAM_MATCHES_DRAW";
    private static final String COLUMN_TEAM_CAPTAIN_ID = "TEAM_CAPTAIN_ID";
    private static final String TEAMNAME_UNIQUE = "CONSTRAINT_TEAM_NAME";


    //Team Details
    public String CREATE_TABLE_TEAM_DETAILS = "CREATE TABLE " + TABLE_TEAM_DETAILS + "("
            + COLUMN_TEAM_USER_ID + " TEXT,"
            + COLUMN_TEAM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_TEAM_NAME + " TEXT,"
            //     + COLUMN_TEAM_NUMBER_OF_PLAYERS + " INTEGER,"
            + COLUMN_TEAM_LOGO + " BLOB,"
            + COLUMN_TEAM_MATCHES_WON + " INTEGER,"
            + COLUMN_TEAM_MATCHES_LOST + " INTEGER,"
            + COLUMN_TEAM_MATCHES_DRAW + " INTEGER,"
            + COLUMN_TEAM_CAPTAIN_ID + " INTEGER, "
            + " CONSTRAINT " + TEAMNAME_UNIQUE + " UNIQUE ( " + COLUMN_TEAM_NAME + " ));";


    //Declaring table -SCHEDULE_DETAILS
    public static final String TABLE_SCHEDULE_DETAILS = "SCHEDULE_DETAILS";

    //Declaring Columns-- SCHEDULE_DETAILS
    private static final String COLUMN_SCHEDULE_USER_ID = "USER_EMAIL_ID";
    private static final String COLUMN_SCHEDULE_MATCH_ID = "SCHEDULE_MATCH_ID";
    private static final String COLUMN_SCHEDULE_MATCH_DATE = "SCHEDULE_MATCH_DATE";
    private static final String COLUMN_SCHEDULE_TEAMA_ID = "SCHEDULE_TEAMA_ID";
    private static final String COLUMN_SCHEDULE_TEAMB_ID = "SCHEDULE_TEAMB_ID";
    private static final String COLUMN_SCHEDULE_GROUND_ID = "SCHEDULE_GROUND_ID";
    private static final String COLUMN_SCHEDULE_MATCH_TYPE = "SCHEDULE_MATCH_TYPE";
    private static final String COLUMN_SCHEDULE_TEAMA_SCORE = "SCHEDULE_TEAMA_SCORE";
    private static final String COLUMN_SCHEDULE_TEAMB_SCORE = "SCHEDULE_TEAMB_SCORE";
    private static final String COLUMN_SCHEDULE_TEAMA_WICKETS = "SCHEDULE_TEAMA_WICKETS";
    private static final String COLUMN_SCHEDULE_TEAMB_WICKETS = "SCHEDULE_TEAMB_WICKETS";
    private static final String COLUMN_SCHEDULE_TEAMA_OVERS = "SCHEDULE_TEAMA_OVERS";
    private static final String COLUMN_SCHEDULE_TEAMB_OVERS = "SCHEDULE_TEAMB_OVERS";
    private static final String COLUMN_SCHEDULE_TOSS_WON_TEAM = "SCHEDULE_TOSS_WON_TEAM";
    private static final String COLUMN_SCHEDULE_TOSS_DECISION = "SCHEDULE_TOSS_DECISION";
    private static final String COLUMN_SCHEDULE_WINNING_TEAM_ID = "SCHEDULE_WINNING_TEAM_ID";
    private static final String COLUMN_SCHEDULE_MATCH_STATUS = "SCHEDULE_MATCH_STATUS";
    private static final String COLUMN_SCHEDULE_MATCH_OVERS = "SCHEDULE_MATCH_OVERS";

    //Schedules

    public String CREATE_TABLE_SCHEDULE_DETAILS = "CREATE TABLE " + TABLE_SCHEDULE_DETAILS + "("
            + COLUMN_SCHEDULE_USER_ID + " TEXT,"
            + COLUMN_SCHEDULE_MATCH_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_SCHEDULE_MATCH_DATE + " TEXT,"
            + COLUMN_SCHEDULE_TEAMA_ID + " INTEGER,"
            + COLUMN_SCHEDULE_TEAMB_ID + " INTEGER,"
            + COLUMN_SCHEDULE_GROUND_ID + " INTEGER,"
            + COLUMN_SCHEDULE_MATCH_TYPE + " INTEGER,"
            + COLUMN_SCHEDULE_TEAMA_SCORE + " INTEGER,"
            + COLUMN_SCHEDULE_TEAMB_SCORE + " INTEGER,"
            + COLUMN_SCHEDULE_TEAMA_WICKETS + " INTEGER,"
            + COLUMN_SCHEDULE_TEAMB_WICKETS + " INTEGER,"
            + COLUMN_SCHEDULE_TEAMA_OVERS + " REAL,"
            + COLUMN_SCHEDULE_TEAMB_OVERS + " REAL,"
            + COLUMN_SCHEDULE_TOSS_WON_TEAM + " INTEGER,"
            + COLUMN_SCHEDULE_TOSS_DECISION + " INTEGER,"
            + COLUMN_SCHEDULE_WINNING_TEAM_ID + " INTEGER,"
            + COLUMN_SCHEDULE_MATCH_STATUS + " INTEGER,"
            + COLUMN_SCHEDULE_MATCH_OVERS + " INTEGER );";


    //Declaring table -GROUND_DETAILS
    public static final String TABLE_GROUND_DETAILS = "GROUND_DETAILS";


    //Declaring Columns-- GROUND_DETAILS
    private static final String COLUMN_GROUND_USER_ID = "USER_EMAIL_ID";
    private static final String COLUMN_GROUND_ID = "GROUND_ID";
    private static final String COLUMN_GROUND_NAME = "GROUND_NAME";
    private static final String COLUMN_GROUND_CITY = "GROUND_CITY";
    private static final String COLUMN_GROUND_LOC_X = "GROUND_LOC_X";
    private static final String COLUMN_GROUND_LOC_Y = "GROUND_LOC_Y";


    //Ground Details
    public String CREATE_TABLE_GROUND_DETAILS = "CREATE TABLE " + TABLE_GROUND_DETAILS + "("
            + COLUMN_GROUND_USER_ID + " TEXT,"
            + COLUMN_GROUND_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_GROUND_NAME + " TEXT,"
            + COLUMN_GROUND_CITY + " TEXT,"
            + COLUMN_GROUND_LOC_X + " REAL,"
            + COLUMN_GROUND_LOC_Y + " REAL );";


    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Category table create query

        String[] createStatements = new String[]{CREATE_TABLE_USER_DETAILS,
                CREATE_TABLE_TEAM_DETAILS,
                CREATE_TABLE_SCHEDULE_DETAILS,
                CREATE_TABLE_PLAYER_DETAILS,
                CREATE_TABLE_MATCH_DETAILS,
                CREATE_TABLE_RULES_DETAILS,
                CREATE_TABLE_GROUND_DETAILS};

        for (String sql : createStatements) {
            db.execSQL(sql);
        }
        //   db.execSQL(CREATE_TABLE_USER_DETAILS);
        //  db.execSQL(CREATE_TABLE_TEAM_DETAILS);
        // db.execSQL(CREATE_TABLE_SCHEDULE_DETAILS);
        //db.execSQL(CREATE_TABLE_PLAYER_DETAILS);
        //db.execSQL(CREATE_TABLE_MATCH_DETAILS);
        //db.execSQL(CREATE_TABLE_RULES_DETAILS);
        //db.execSQL(CREATE_TABLE_GROUND_DETAILS);

    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        //   db.execSQL("DROP TABLE IF EXISTS " + TABLE_TEAM_DETAILS);

        // Create tables again
        onCreate(db);
    }

    /**
     * Inserting new label into lables table
     */
    public void insertTeamBasicDetails(String teamName)//,byte[] imageBytes){//add code to insert image if selected.,int numberOfPlayers
    {
        //column name, column value

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues teamValues = new ContentValues();
        teamValues.put(COLUMN_TEAM_USER_ID, "aditya.juet@gmail.com");
        teamValues.put(COLUMN_TEAM_NAME, teamName);
        //     teamValues.put(COLUMN_TEAM_ID,teamId);
        //     teamValues.put(COLUMN_TEAM_NUMBER_OF_PLAYERS, numberOfPlayers);
        teamValues.put(COLUMN_TEAM_CAPTAIN_ID, 0);
        teamValues.put(COLUMN_TEAM_MATCHES_DRAW, 0);
        teamValues.put(COLUMN_TEAM_MATCHES_WON, 0);
        teamValues.put(COLUMN_TEAM_MATCHES_LOST, 0);
        teamValues.put(COLUMN_TEAM_LOGO, "NotSet");
         /*   if(imageBytes!=null) {
                teamValues.put(COLUMN_TEAM_LOGO, imageBytes);
            }
            else{
                teamValues.putNull(COLUMN_TEAM_LOGO);
            }
            */
        // Inserting Row
        db.insert(TABLE_TEAM_DETAILS, null, teamValues);//tableName, nullColumnHack, CotentValues

        db.close(); // Closing database connection

    }


    //   for(String name:nameCheck) {
    //     if (name.contains(teamName)) {

    // }


      /*  else {

            teamValues.put(COLUMN_TEAM_NAME, teamName);
            // teamValues.put(COLUMN_TEAM_ID,teamId);
            teamValues.put(COLUMN_NUMBER_OF_PLAYERS, numberOfPlayers);
            teamValues.putNull(COLUMN_CAPTAIN_ID);
            teamValues.putNull(COLUMN_MATCHES_DRAW);
            teamValues.putNull(COLUMN_MATCHES_WON);
            teamValues.putNull(COLUMN_MATCHES_LOST);
            teamValues.putNull(COLUMN_TEAM_LOGO);
            // Inserting Row
            db.insert(TABLE_TEAM_DETAILS, null, teamValues);//tableName, nullColumnHack, CotentValues

            db.close(); // Closing database connection

                }*/


    /**
     * Getting all labels
     * returns list of labels
     * */

    /**
     * Team Management using TEAM ID
     */
    public Byte getTeamImage(int teamId) {
        //code to retrieve Teamlogo from DB.
        Byte image = new Byte("äaaaa");
        return image;
    }

    public String getTeamName(int teamId) {
        String teamName = "";
        String selectQuery = "SELECT " + COLUMN_TEAM_NAME + " FROM " + TABLE_TEAM_DETAILS + " WHERE " + COLUMN_TEAM_ID + "=" +teamId;
        SQLiteDatabase db=this.getReadableDatabase();

        Cursor cursor=db.rawQuery(selectQuery,null);
        cursor.moveToFirst();
       teamName=cursor.getString(cursor.getColumnIndex(COLUMN_TEAM_NAME));
        return teamName;
    }

    public String getCaptainName(int teamId) {

        String captainName = "";
        return captainName;

    }

    public int[] getMatchData(int teamId) {
        int matchData[] = {0, 0, 0, 0};
        return matchData;
    }


//Update Team Details

    public void updateTeamDetails(int teamId, int[] teamPlayerIds, File teamImage) {

    }

    //
    public byte[] viewImage(String teamName) {
        String loadImageQuery = "select TEAM_LOGO from TEAM_DETAILS where TeamName = " + teamName;
        Cursor c = db.rawQuery(loadImageQuery, null);
        if (c.moveToNext()) {
            byte[] image = c.getBlob(0);
            //   Bitmap bmp= BitmapFactory.decodeByteArray(image, 0 , image.length);
            //   imageView.setImageBitmap(bmp);

            // Toast.makeText(this,"Done", Toast.LENGTH_SHORT).show();
            return image;
        }
        c.close();
        return null;
    }


    //Getting All Team Names
    public ArrayList<String> getAllTeamNames() {
        ArrayList<String> teamlist = new ArrayList<>();


        // Select All Query
        String selectQuery = "SELECT " + COLUMN_TEAM_ID + ", " + COLUMN_TEAM_NAME + " FROM " + TABLE_TEAM_DETAILS;
        //+ COLUMN_TEAM_ID + ','

        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery(selectQuery, null);//selectQuery,selectedArguments
            if (cursor.moveToLast()) {
                do {
                    teamlist.add(cursor.getString(cursor.getColumnIndex(COLUMN_TEAM_NAME)));
                    //team.teamId=cursor.getInt(1);
                   /* list.add(cursor.getInt(cursor.getColumnIndex(COLUMN_TEAM_ID))+"\t\t\t"+*///teamlist.add(team);
                    //  list.add(cursor.getString(1));//adding 2nd column data
                } while (cursor.moveToPrevious());
            }
            // closing connection
            cursor.close();
            db.close();

            // returning Team Names with TeamId
            return teamlist;
        } catch (Exception e) {
            Log.v("getAllTeamNames", "Function threw exception " + e);
            //System.out.println("Error");


            // looping through all rows and adding to list

            return null;
        }
    }

    //Adding player details to DB
    //playerProfile parameter will tell batsmen,bowler,wicketkeeper
    public void addPlayerDetails(String userId, String playerName, int checkBatsman, int checkbowler, int checkWk) {   //args: File playerImage

        //code to add player
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues playerValues = new ContentValues();

        //Key Values- Auto Generated
        playerValues.putNull(COLUMN_PLAYER_ID);

        //Basic Player Details
        playerValues.put(COLUMN_PLAYER_USER_ID, userId);
        playerValues.put(COLUMN_PLAYER_NAME, playerName);
        playerValues.put(COLUMN_PLAYER_EMAIL, "NotSet");
        playerValues.put(COLUMN_PLAYER_BATSMAN, checkBatsman);
        playerValues.put(COLUMN_PLAYER_BOWLER, checkbowler);
        playerValues.put(COLUMN_PLAYER_WICKETKEEPER, checkWk);
        playerValues.put(COLUMN_PLAYER_IMAGE, "NotSet");
        playerValues.put(COLUMN_PLAYER_DOB, "000000");
        playerValues.put(COLUMN_PLAYER_TEAM_ID, "0");


        //Null Values

        playerValues.put(COLUMN_PLAYER_TOTAL_RUNS, 0);
        playerValues.put(COLUMN_PLAYER_FIFTY, 0);
        playerValues.put(COLUMN_PLAYER_HUNDRED, 0);
        playerValues.put(COLUMN_PLAYER_TWO_HUNDRED, 0);
        playerValues.put(COLUMN_PLAYER_BALLS_FACED, 0);
        playerValues.put(COLUMN_PLAYER_MATCH_PLAYED, 0);
        playerValues.put(COLUMN_PLAYER_OVERS_BOWLED, 0);
        playerValues.put(COLUMN_PLAYER_WICKETS_TAKEN, 0);
        playerValues.put(COLUMN_PLAYER_MAIDEN_BOWLED, 0);
        playerValues.put(COLUMN_PLAYER_WIDES_COUNT, 0);
        playerValues.put(COLUMN_PLAYER_NOBALL_COUNT, 0);
        playerValues.put(COLUMN_PLAYER_RUNS_GIVEN, 0);
        playerValues.put(COLUMN_PLAYERS_CATCHES, 0);

        // Inserting Row
        db.insert(TABLE_PLAYER_DETAILS, null, playerValues);//tableName, nullColumnHack, CotentValues

        db.close(); // Closing database connection


    }

    //Updating Player Details to DB

    public void updatePlayerDetails(int playerID, int playerDOB, int teamId, int playerProfile, File playerImage, String email) {

        //code to update player

    }

    //loading team details from DB
    public Teams getTeamDetails(int teamId) {
        //write the code to retrieve all the team details from db where team ID=teamID
        Teams team = new Teams();
        ArrayList<String> teamlist = new ArrayList<>();

        // Select All Query
        //   String selectQuery = "SELECT " + COLUMN_TEAM_ID + ", " + COLUMN_TEAM_NAME + ", " + COLUMN_TEAM_MATCHES_WON + ", " + COLUMN_TEAM_MATCHES_LOST + ", " + COLUMN_TEAM_MATCHES_DRAW + ", " + COLUMN_TEAM_CAPTAIN_ID + " FROM " + TABLE_TEAM_DETAILS + " WHERE " + COLUMN_TEAM_ID + "= ?";
        //+ COLUMN_TEAM_ID + ','
        String[] columns = {COLUMN_TEAM_ID, COLUMN_TEAM_NAME, COLUMN_TEAM_MATCHES_WON, COLUMN_TEAM_MATCHES_LOST, COLUMN_TEAM_MATCHES_DRAW, COLUMN_TEAM_CAPTAIN_ID};
        String WHERE = COLUMN_TEAM_ID + " = ?";
        String[] WHEREARGS = new String[]{String.valueOf(teamId)};
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Log.v("getTeamDetails", "team Id Value" + teamId);

            // Cursor cursor = db.rawQuery(selectQuery,String.valueOf(teamId));//WHERE,new String[]{String.valueOf(teamId)});;//selectQuery,selectedArguments

            Cursor cursor = db.query(TABLE_TEAM_DETAILS, columns, WHERE, WHEREARGS, null, null, null, null);
            cursor.moveToFirst();
            Log.v("getTeamDetails", "cursor Value" + cursor.getString(cursor.getColumnIndex(COLUMN_TEAM_NAME)));
            team.teamId = cursor.getInt(cursor.getColumnIndex(COLUMN_TEAM_ID));
            Log.v("getTeamDetails", "COLUMN_TEAM_ID Value" + cursor.getString(cursor.getColumnIndex(COLUMN_TEAM_ID)));

            team.teamName = cursor.getString(cursor.getColumnIndex(COLUMN_TEAM_NAME));
            Log.v("getTeamDetails", "COLUMN_TEAM_NAME Value" + cursor.getString(cursor.getColumnIndex(COLUMN_TEAM_NAME)));

            //   teams.teamLogo=cursor.getBlob(cursor.getColumnIndex(COLUMN_TEAM_LOGO));
            team.matchDraw = cursor.getInt(cursor.getColumnIndex(COLUMN_TEAM_MATCHES_DRAW));
            Log.v("getTeamDetails", "COLUMN_TEAM_MATCHES_DRAW Value" + cursor.getString(cursor.getColumnIndex(COLUMN_TEAM_MATCHES_DRAW)));

            team.matchWon = cursor.getInt(cursor.getColumnIndex(COLUMN_TEAM_MATCHES_WON));
            Log.v("getTeamDetails", "COLUMN_TEAM_MATCHES_WON Value" + cursor.getString(cursor.getColumnIndex(COLUMN_TEAM_MATCHES_WON)));

            team.matchLost = cursor.getInt(cursor.getColumnIndex(COLUMN_TEAM_MATCHES_LOST));
            Log.v("getTeamDetails", "COLUMN_TEAM_MATCHES_LOST Value" + cursor.getString(cursor.getColumnIndex(COLUMN_TEAM_MATCHES_LOST)));

            team.captainId = cursor.getInt(cursor.getColumnIndex(COLUMN_TEAM_CAPTAIN_ID));
            Log.v("getTeamDetails", "COLUMN_TEAM_CAPTAIN_ID Value" + cursor.getString(cursor.getColumnIndex(COLUMN_TEAM_CAPTAIN_ID)));

            //   team.teamPlayerCount= cursor.getInt(cursor.getColumnIndex(COLUMN_TEAM_NUMBER_OF_PLAYERS));

            Log.v("getTeamDetails", "Retrieved Team details for Team id:" + teamId);
            cursor.close();
            db.close();
            // returning Team details with TeamId
            return team;
        } catch (Exception e) {
            Log.v("getTeamDetails", "Threw Exception" + e);

            return null;
        }
    }


    public int getTeamId(String teamName) {
        //code to get teamId
        int teamId = 0;

        // Select All Query
        String selectQuery = "SELECT " + COLUMN_TEAM_ID + ", " + COLUMN_TEAM_NAME + " FROM " + TABLE_TEAM_DETAILS + " WHERE " + COLUMN_TEAM_NAME + "='" + teamName + "'";
        //+ COLUMN_TEAM_ID + ','
        try {
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor cursor = db.rawQuery(selectQuery, null);//selectQuery,selectedArguments
            if (cursor.moveToFirst()) {
                Log.v("LetMeSee", "Hello " + cursor.getInt(cursor.getColumnIndex(COLUMN_TEAM_ID)));
                // if (cursor.) {
                //  do {
                teamId = cursor.getInt(cursor.getColumnIndex(COLUMN_TEAM_ID));
                //team.teamId=cursor.getInt(1);
                   /* list.add(cursor.getInt(cursor.getColumnIndex(COLUMN_TEAM_ID))+"\t\t\t"+*///teamlist.add(team);
                //  list.add(cursor.getString(1));//adding 2nd column data
                //  } while (cursor.moveToPrevious());
                //   }
                // closing connection
                cursor.close();
                db.close();

                // returning  TeamId for Team Name
                return teamId;
            }

        } catch (Exception e) {
            Log.v("getTeamId ", "Function threw exception " + e);
            //System.out.println("Error");


            // looping through all rows and adding to list

            return 0;
        }
        return 0;
    }

    //Getting Player details from DB
    public Players getPlayerDetails(int playerId) {
        //write the code to retrieve all the team details from db where team ID=teamID
        Players player = new Players();
        ArrayList<String> teamlist = new ArrayList<>();

        // Select All Query
        //   String selectQuery = "SELECT " + COLUMN_TEAM_ID + ", " + COLUMN_TEAM_NAME + ", " + COLUMN_TEAM_MATCHES_WON + ", " + COLUMN_TEAM_MATCHES_LOST + ", " + COLUMN_TEAM_MATCHES_DRAW + ", " + COLUMN_TEAM_CAPTAIN_ID + " FROM " + TABLE_TEAM_DETAILS + " WHERE " + COLUMN_TEAM_ID + "= ?";
        //+ COLUMN_TEAM_ID + ','
        String[] columns = {COLUMN_PLAYER_ID,
                COLUMN_PLAYER_NAME,
                COLUMN_PLAYER_EMAIL,
                COLUMN_PLAYER_BATSMAN,
                COLUMN_PLAYER_BOWLER,
                COLUMN_PLAYER_WICKETKEEPER,
                COLUMN_PLAYER_DOB,
                COLUMN_PLAYER_TEAM_ID,
                COLUMN_PLAYER_TOTAL_RUNS,
                COLUMN_PLAYER_FIFTY,
                COLUMN_PLAYER_HUNDRED,
                COLUMN_PLAYER_TWO_HUNDRED,
                COLUMN_PLAYER_BALLS_FACED,
                COLUMN_PLAYER_MATCH_PLAYED,
                COLUMN_PLAYER_OVERS_BOWLED,
                COLUMN_PLAYER_WICKETS_TAKEN,
                COLUMN_PLAYER_MAIDEN_BOWLED,
                COLUMN_PLAYER_WIDES_COUNT,
                COLUMN_PLAYER_NOBALL_COUNT,
                COLUMN_PLAYER_RUNS_GIVEN,
                COLUMN_PLAYERS_CATCHES};
        //COLUMN_PLAYER_USER_ID,COLUMN_PLAYER_IMAGE
        String WHERE = COLUMN_PLAYER_ID + " = ?";
        String[] WHEREARGS = new String[]{String.valueOf(playerId)};
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Log.v("getPlayerDetails", "player Id Value: " + playerId);

            // Cursor cursor = db.rawQuery(selectQuery,String.valueOf(teamId));//WHERE,new String[]{String.valueOf(teamId)});;//selectQuery,selectedArguments

            Cursor cursor = db.query(TABLE_PLAYER_DETAILS, columns, WHERE, WHEREARGS, null, null, null, null);
            cursor.moveToFirst();
            //cursor.moveToLast();

            player.playerName=cursor.getString(cursor.getColumnIndex(COLUMN_PLAYER_NAME));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_NAME Value: " + cursor.getString(cursor.getColumnIndex(COLUMN_PLAYER_NAME)));
            player.playerID=cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_ID));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_ID Value: " + cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_ID)));
            player.playerEmail=cursor.getString(cursor.getColumnIndex(COLUMN_PLAYER_EMAIL));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_EMAIL Value: " + cursor.getString(cursor.getColumnIndex(COLUMN_PLAYER_EMAIL)));
            player.playerBatsmen=cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_BATSMAN));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_BATSMAN Value: " + cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_BATSMAN)));
            player.playerBowler=cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_BOWLER));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_BOWLER Value: " + cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_BOWLER)));
            player.playerWK=cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_WICKETKEEPER));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_WICKETKEEPER Value: " + cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_WICKETKEEPER)));
          //  player.playerImage=cursor.getBlob(cursor.getColumnIndex(COLUMN_PLAYER_IMAGE));
            player.playerDOB=cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_DOB));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_DOB Value: " + cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_DOB)));
            player.playerTeamId=cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_TEAM_ID));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_TEAM_ID Value: " +cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_TEAM_ID)));
            player.playerTotalRuns=cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_TOTAL_RUNS));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_TOTAL_RUNS Value: " + cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_TOTAL_RUNS)));
            player.playerFifty=cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_FIFTY));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_FIFTY Value: " + cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_FIFTY)));
            player.playerHundred=cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_HUNDRED));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_HUNDRED Value: " + cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_HUNDRED)));
            player.playerBallFaced=cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_BALLS_FACED));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_BALLS_FACED Value: " + cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_BALLS_FACED)));
            player.playerTwoHundred=cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_TWO_HUNDRED));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_TWO_HUNDRED Value: " + cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_TWO_HUNDRED)));
            player.playerMatchPlayed=cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_MATCH_PLAYED));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_MATCH_PLAYED Value: " + cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_MATCH_PLAYED)));
            player.playerOverBowled=cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_OVERS_BOWLED));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_OVERS_BOWLED Value: " + cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_OVERS_BOWLED)));
            player.playerWicketTaken=cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_WICKETS_TAKEN));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_WICKETS_TAKEN Value: " + cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_WICKETS_TAKEN)));
            player.playerMaidenBowled=cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_MAIDEN_BOWLED));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_MAIDEN_BOWLED Value: " + cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_MAIDEN_BOWLED)));
            player.playerWidesCount=cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_WIDES_COUNT));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_WIDES_COUNT Value: " + cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_WIDES_COUNT)));
            player.playerNoBallCount=cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_NOBALL_COUNT));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_NOBALL_COUNT Value: " + cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_NOBALL_COUNT)));
            player.playerRunsGiven=cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_RUNS_GIVEN));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_RUNS_GIVEN Value: " + cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_RUNS_GIVEN)));
            player.playerCatches=cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYERS_CATCHES));
            Log.v("getPlayerDetails", "COLUMN_PLAYERS_CATCHES Value: " + cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYERS_CATCHES)));


            Log.v("getPlayerDetails", "Retrieved Player details for Player id: " + playerId);
            cursor.close();
            db.close();
            // returning Player details with PlayerId
            return player;
        } catch (Exception e) {
            Log.v("getPlayerDetails", "Threw Exception" + e);

            return null;
        }
    }


    public int getPlayerId(String playerName) {
        //code to get playerId
        int playerId=0;

        // Select All Query
        String selectQuery = "SELECT " +COLUMN_PLAYER_ID + ", " +COLUMN_PLAYER_NAME + " FROM " +TABLE_PLAYER_DETAILS + " WHERE " +COLUMN_PLAYER_NAME + "=\'" + playerName +"\'";
        //+ COLUMN_TEAM_ID + ',''

        try {
        SQLiteDatabase db = this.getReadableDatabase();

            Cursor cursor = db.rawQuery(selectQuery, null);//selectQuery,selectedArguments
           if (cursor.moveToFirst()) {
                //    do {

                   /* list.add(cursor.getInt(cursor.getColumnIndex(COLUMN_TEAM_ID))+"\t\t\t"+*///list.add(cursor.getString(cursor.getColumnIndex(COLUMN_PLAYER_ID)));
                playerId = cursor.getInt(cursor.getColumnIndex(COLUMN_PLAYER_ID));//adding COLUMN_PLAYER_ID column data
                //  } while (cursor.moveToPrevious());

                // closing connection
                cursor.close();
                db.close();

                // returning Player Id where Player Name is found!
                return playerId;
           }

        } catch (Exception e) {
            Log.v("getPlayerId", "Function threw exception " + e);
            //System.out.println("Error");


            // looping through all rows and adding to list

            return playerId;
        }
       return playerId;
    }

    //used to find all the players in the DB
    public ArrayList<String> viewAllPlayers() {
        ArrayList<String> list = new ArrayList<>();


        // Select All Query
        String selectQuery = "SELECT " + COLUMN_PLAYER_ID + ", " + COLUMN_PLAYER_NAME + " FROM " + TABLE_PLAYER_DETAILS;
        //+ COLUMN_TEAM_ID + ','

        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery(selectQuery, null);//selectQuery,selectedArguments
            if (cursor.moveToLast()) {
                do {

                   /* list.add(cursor.getInt(cursor.getColumnIndex(COLUMN_TEAM_ID))+"\t\t\t"+*///list.add(cursor.getString(cursor.getColumnIndex(COLUMN_PLAYER_ID)));
                    list.add(cursor.getString(1));//adding 2nd column data
                } while (cursor.moveToPrevious());
            }
            // closing connection
            cursor.close();
            db.close();

            // returning Player Names with Player Id
            return list;

        } catch (Exception e) {
            Log.v("viewAllPlayers", "Function threw exception " + e);
            //System.out.println("Error");


            // looping through all rows and adding to list

            return null;
        }
    }

    //Add Player to Team
    public void addPlayerInTeam(int playerId,int playerteamId){
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PLAYER_TEAM_ID,""+playerteamId);
        String WHERE=COLUMN_PLAYER_ID+"= ?";
       String[] WHEREARGS=new String[]{String.valueOf(playerId)};
        try{
            SQLiteDatabase db = this.getReadableDatabase();
            db.update(TABLE_PLAYER_DETAILS,cv,WHERE,WHEREARGS);
            Log.v("addPlayerInTeam:","Player Updated with Team");
            db.close();
        }catch (Exception e){
            Log.v("addPlayerInTeam:","Threw Exception"+e);
        }

        //String query= "UPDATE" +COLUMN_PLAYER_TEAM_ID+"AS"+playerteamId+"WHERE "+ player+

    }

    public void removePlayerInTeam(int playerId){
        int zero=0;
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PLAYER_TEAM_ID,""+zero);
        String WHERE=COLUMN_PLAYER_ID+"= ?";
        String[] WHEREARGS=new String[]{String.valueOf(playerId)};
        try{
            SQLiteDatabase db = this.getReadableDatabase();
            db.update(TABLE_PLAYER_DETAILS,cv,WHERE,WHEREARGS);
            Log.v("removePlayerInTeam:","Player Updated with Team");
            db.close();
        }catch (Exception e){
            Log.v("removePlayerInTeam:","Threw Exception"+e);
        }

        //String query= "UPDATE" +COLUMN_PLAYER_TEAM_ID+"AS"+playerteamId+"WHERE "+ player+

    }
    //Used to find all players of a team
    public ArrayList<String> viewAllPlayers(int teamId) {
        ArrayList<String> list = new ArrayList<>();
        // Select All Query
        String selectQuery = "SELECT " +COLUMN_PLAYER_ID+ ", " +COLUMN_PLAYER_NAME+ " FROM " +TABLE_PLAYER_DETAILS+ " WHERE "+COLUMN_PLAYER_TEAM_ID+"=\'"+teamId+"\'";

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(selectQuery, null);//selectQuery,selectedArguments

            if (cursor.moveToLast()) {  //cursor!=null&&
                do {

                   /* list.add(cursor.getInt(cursor.getColumnIndex(COLUMN_TEAM_ID))+"\t\t\t"+*///list.add(cursor.getString(cursor.getColumnIndex(COLUMN_PLAYER_ID)));
                    list.add(cursor.getString(1));//adding 2nd column data
                } while (cursor.moveToPrevious());
            }
        return list;
    }catch (Exception e){

            Log.v("viewAllPlayers:","Threw Exception"+e);
            return null;
        }
    }

    //Used to find players of a team with specific status i.e. OUT, NOT OUT, Retd.
    public ArrayList<String> viewAllPlayers(int teamId, int playerStatus) {
        ArrayList<String> list = new ArrayList<>();

        return list;
    }

    //used to add ground details in DB
    public void addGround(String groundName, String groundCity, Float groundLocationX, Float groundLocationY) {

    }


    //used to view all the grounds in DB
    public ArrayList<String> viewAllGrounds() {
        ArrayList<String> list = new ArrayList<>();

        return list;
    }

    //Used to add schedule details in DB
    public void addSchedule(String userId,String matchDate, int TeamAId, int TeamBId, int matchOvers) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues scheduleValues = new ContentValues();

            //Key Values- Auto Generated
            scheduleValues.putNull(COLUMN_SCHEDULE_MATCH_ID);

            //Basic Schedule Details
            scheduleValues.put(COLUMN_SCHEDULE_USER_ID, userId);
            scheduleValues.put(COLUMN_SCHEDULE_MATCH_DATE, matchDate);
            scheduleValues.put(COLUMN_SCHEDULE_TEAMA_ID, TeamAId);
            scheduleValues.put(COLUMN_SCHEDULE_TEAMB_ID, TeamBId);
            scheduleValues.put(COLUMN_SCHEDULE_MATCH_TYPE, matchOvers);

            //Null Values
            scheduleValues.putNull(COLUMN_SCHEDULE_GROUND_ID);
            scheduleValues.putNull(COLUMN_SCHEDULE_TEAMA_SCORE);
            scheduleValues.putNull(COLUMN_SCHEDULE_TEAMB_SCORE);
            scheduleValues.putNull(COLUMN_SCHEDULE_TEAMA_WICKETS);
            scheduleValues.putNull(COLUMN_SCHEDULE_TEAMB_WICKETS);
            scheduleValues.putNull(COLUMN_SCHEDULE_TEAMA_OVERS);
            scheduleValues.putNull(COLUMN_SCHEDULE_TEAMB_OVERS);
            scheduleValues.putNull(COLUMN_SCHEDULE_TOSS_WON_TEAM);
            scheduleValues.putNull(COLUMN_SCHEDULE_TOSS_DECISION);
            scheduleValues.putNull(COLUMN_SCHEDULE_WINNING_TEAM_ID);
            scheduleValues.put(COLUMN_SCHEDULE_MATCH_STATUS,"SCHEDULED");
            scheduleValues.putNull(COLUMN_SCHEDULE_MATCH_OVERS);


            // Inserting Row
            db.insert(TABLE_SCHEDULE_DETAILS, null, scheduleValues);//tableName, nullColumnHack, CotentValues

            db.close(); // Closing database connection
        }catch (Exception e){
            Log.v("addSchedule","Ërror in adding basic schedule details");
        }

    }

    //Used to view all schedules
    public String[][] viewAllSchedules() {
        String selectQuery = "SELECT " + COLUMN_SCHEDULE_MATCH_DATE+ ", " +COLUMN_SCHEDULE_TEAMA_ID+ ", " +COLUMN_SCHEDULE_TEAMB_ID+ ", " +COLUMN_SCHEDULE_MATCH_TYPE+" FROM " +TABLE_SCHEDULE_DETAILS;

        SQLiteDatabase db=this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);//selectQuery,selectedArguments
        int numberOfRows=cursor.getCount();
        int numberOfColumns=cursor.getColumnCount();
        String[][] list = new String[numberOfRows][numberOfColumns];
        try {
            cursor.moveToFirst();

            for (int i = 0; i < numberOfRows; i++) {
                if(i!=0){
                    cursor.moveToNext();
                }

                    for (int j = 0; j < numberOfColumns; j++) {

                   /* list.add(cursor.getInt(cursor.getColumnIndex(COLUMN_TEAM_ID))+"\t\t\t"+*///list.add(cursor.getString(cursor.getColumnIndex(COLUMN_PLAYER_ID)));
                        list[i][j] = cursor.getString(j);//adding 2nd column data

                    }

            }
        }catch (Exception e){
            Log.v("DatabaseHandler:","Exception thrown at viewAllSchedules  "+e);
            Log.v("DatabaseHandler:","Number of Rows "+numberOfRows);
            Log.v("DatabaseHandler:","Number of Columns "+numberOfColumns);
        }
        return list;
    }
    public int getNumberOfSchedules(){
        int numberOfRows =0;
       try {
           String selectQuery = "SELECT " + COLUMN_SCHEDULE_MATCH_DATE + ", " + COLUMN_SCHEDULE_TEAMA_ID + ", " + COLUMN_SCHEDULE_TEAMB_ID + ", " + COLUMN_SCHEDULE_MATCH_TYPE + " FROM " + TABLE_SCHEDULE_DETAILS;
           SQLiteDatabase db = this.getReadableDatabase();
           Cursor cursor = db.rawQuery(selectQuery, null);//selectQuery,selectedArguments
           numberOfRows = cursor.getCount();
       }catch (Exception e){
           Log.v("DatabaseHandler:","Error in function getNumberOfSchedules"+e);
       }
        return numberOfRows;
    }

    //used to update schedules
    public void updateSchedule(int matchDate, int TeamAId, int TeamBId, int matchOvers) {

    }

    //used to view all completed match summaries
    public ArrayList<String> viewAllMatchSummary() {
        ArrayList<String> list = new ArrayList<>();

        return list;

    }

    //used to view on going match summary
    public ArrayList<String> viewAllMatchSummary(int matchId) {
        ArrayList<String> list = new ArrayList<>();

        return list;

    }
    //update ongoing match details on ball clicks

    //updating batsman stats for ongoing match
    public void updateBatsmanStats(int matchId, int playerId, int runsScoredOnBall, int playerStatus, int undoStatus) {

    }

    //updating bowler stats for ongoing match
    public void updateBowlerStats(int matchId, int playerId, int wickets, int maidenOver, int wideBall, int noBall, int runsGiven, int undoStatus) {

    }

    //updating fielder stats for on going match
    public void updateFielderStats(int matchId, int playerId, int catches, int undoStatus) {

    }

    //updating batting and bowling teams after end of innings. target scores in match summaries. player details.
    public void endOfInningsUpdate(int matchId) {

    }

    //updating match results and match status in match summaries.
    public void endOfMatchUpdate(int matchId) {

    }
}





