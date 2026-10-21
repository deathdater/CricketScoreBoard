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
import com.example.android.cricketscoreboard.scoring.CricketRulesProfile;
import com.example.android.cricketscoreboard.scoring.CricketScoringEngine;
import com.example.android.cricketscoreboard.scoring.DeliveryEvent;
import com.example.android.cricketscoreboard.scoring.MatchState;
import com.example.android.cricketscoreboard.scoring.MatchState.BatterInningsStat;
import com.example.android.cricketscoreboard.scoring.MatchState.BowlerSpellStat;
import com.example.android.cricketscoreboard.scoring.MatchState.InningsScorecard;
import com.example.android.cricketscoreboard.scoring.MatchState.MatchLifecycleStatus;
import com.example.android.cricketscoreboard.sync.CricketSyncWorker;
import com.example.android.cricketscoreboard.sync.SyncPreferences;
import com.example.android.cricketscoreboard.sync.SyncRecordModels;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.OutboxItem;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.PlayerSyncRecord;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.ScheduleSyncRecord;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.TeamSyncRecord;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


/**
 * Created by Deathdater on 2/13/17.
 */

public class DatabaseHandler extends SQLiteOpenHelper {
    public String USERID="aditya.juet@gmail.com";
    // v2: fixed MATCH_DETAILS schema (missing USER_EMAIL_ID column type).
    // v3: added offline-first cloud sync metadata columns + SYNC_OUTBOX table.
    // v4: added configurable RULES_PROFILES, MATCH_SESSIONS, and event-sourced MATCH_DELIVERY_EVENTS.
    private static final int DATABASE_VERSION = 4;
    //Declaring Database Name
    private SQLiteDatabase db;
    private final Context appContext;
    private static final String DATABASE_NAME = "cricketScore_DB";
    Teams team = new Teams();
    Players player=new Players();

    // Common Sync Metadata Columns added in v3
    public static final String COLUMN_SYNC_UUID = "SYNC_UUID";
    public static final String COLUMN_CLUB_ID = "CLUB_ID";
    public static final String COLUMN_UPDATED_AT = "UPDATED_AT";
    public static final String COLUMN_IS_DELETED = "IS_DELETED";
    public static final String COLUMN_SYNC_STATUS = "SYNC_STATUS";
    public static final String COLUMN_REMOTE_VERSION = "REMOTE_VERSION";

    // Persistent Sync Outbox Table
    public static final String TABLE_SYNC_OUTBOX = "SYNC_OUTBOX";
    public static final String COLUMN_OUTBOX_ID = "OUTBOX_ID";
    public static final String COLUMN_OUTBOX_ENTITY_TYPE = "ENTITY_TYPE";
    public static final String COLUMN_OUTBOX_ENTITY_UUID = "ENTITY_UUID";
    public static final String COLUMN_OUTBOX_OPERATION = "OPERATION";
    public static final String COLUMN_OUTBOX_PAYLOAD = "PAYLOAD_JSON";
    public static final String COLUMN_OUTBOX_CREATED_AT = "CREATED_AT";
    public static final String COLUMN_OUTBOX_RETRY_COUNT = "RETRY_COUNT";
    public static final String COLUMN_OUTBOX_LAST_ERROR = "LAST_ERROR";

    public static final String CREATE_TABLE_SYNC_OUTBOX = "CREATE TABLE IF NOT EXISTS " + TABLE_SYNC_OUTBOX + "("
            + COLUMN_OUTBOX_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_OUTBOX_ENTITY_TYPE + " TEXT NOT NULL,"
            + COLUMN_OUTBOX_ENTITY_UUID + " TEXT NOT NULL,"
            + COLUMN_OUTBOX_OPERATION + " TEXT NOT NULL,"
            + COLUMN_OUTBOX_PAYLOAD + " TEXT NOT NULL,"
            + COLUMN_OUTBOX_CREATED_AT + " INTEGER NOT NULL,"
            + COLUMN_OUTBOX_RETRY_COUNT + " INTEGER DEFAULT 0,"
            + COLUMN_OUTBOX_LAST_ERROR + " TEXT,"
            + " UNIQUE(" + COLUMN_OUTBOX_ENTITY_TYPE + ", " + COLUMN_OUTBOX_ENTITY_UUID + "));";

    // v4: Configurable Rules Profiles Table
    public static final String TABLE_RULES_PROFILES = "RULES_PROFILES";
    public static final String COLUMN_PROFILE_ID = "PROFILE_ID";
    public static final String COLUMN_PROFILE_NAME = "PROFILE_NAME";
    public static final String COLUMN_PROFILE_VERSION = "PROFILE_VERSION";
    public static final String COLUMN_PROFILE_PAYLOAD = "RULES_PAYLOAD";

    public static final String CREATE_TABLE_RULES_PROFILES = "CREATE TABLE IF NOT EXISTS " + TABLE_RULES_PROFILES + "("
            + COLUMN_PROFILE_ID + " TEXT PRIMARY KEY,"
            + COLUMN_PROFILE_NAME + " TEXT NOT NULL,"
            + COLUMN_PROFILE_VERSION + " INTEGER DEFAULT 1,"
            + COLUMN_PROFILE_PAYLOAD + " TEXT NOT NULL,"
            + COLUMN_CLUB_ID + " TEXT,"
            + COLUMN_UPDATED_AT + " INTEGER DEFAULT 0,"
            + COLUMN_IS_DELETED + " INTEGER DEFAULT 0);";

    // v4: Event-Sourced Match Sessions Table
    public static final String TABLE_MATCH_SESSIONS = "MATCH_SESSIONS";
    public static final String COLUMN_SESSION_MATCH_UUID = "MATCH_UUID";
    public static final String COLUMN_SESSION_SCHEDULE_ID = "SCHEDULE_ID";
    public static final String COLUMN_SESSION_TEAM_A_ID = "TEAM_A_ID";
    public static final String COLUMN_SESSION_TEAM_A_NAME = "TEAM_A_NAME";
    public static final String COLUMN_SESSION_TEAM_B_ID = "TEAM_B_ID";
    public static final String COLUMN_SESSION_TEAM_B_NAME = "TEAM_B_NAME";
    public static final String COLUMN_SESSION_RULES_SNAPSHOT = "RULES_SNAPSHOT";
    public static final String COLUMN_SESSION_LIFECYCLE_STATUS = "LIFECYCLE_STATUS";
    public static final String COLUMN_SESSION_CURRENT_INNINGS = "CURRENT_INNINGS";
    public static final String COLUMN_SESSION_RESULT_TEXT = "RESULT_TEXT";

    public static final String CREATE_TABLE_MATCH_SESSIONS = "CREATE TABLE IF NOT EXISTS " + TABLE_MATCH_SESSIONS + "("
            + COLUMN_SESSION_MATCH_UUID + " TEXT PRIMARY KEY,"
            + COLUMN_SESSION_SCHEDULE_ID + " INTEGER DEFAULT 0,"
            + COLUMN_SESSION_TEAM_A_ID + " INTEGER DEFAULT 1,"
            + COLUMN_SESSION_TEAM_A_NAME + " TEXT,"
            + COLUMN_SESSION_TEAM_B_ID + " INTEGER DEFAULT 2,"
            + COLUMN_SESSION_TEAM_B_NAME + " TEXT,"
            + COLUMN_SESSION_RULES_SNAPSHOT + " TEXT NOT NULL,"
            + COLUMN_SESSION_LIFECYCLE_STATUS + " TEXT DEFAULT 'PROVISIONAL',"
            + COLUMN_SESSION_CURRENT_INNINGS + " INTEGER DEFAULT 1,"
            + COLUMN_SESSION_RESULT_TEXT + " TEXT,"
            + COLUMN_CLUB_ID + " TEXT,"
            + COLUMN_UPDATED_AT + " INTEGER DEFAULT 0,"
            + COLUMN_IS_DELETED + " INTEGER DEFAULT 0,"
            + COLUMN_SYNC_STATUS + " TEXT DEFAULT 'DIRTY');";

    // v4: Ordered Delivery Events Table
    public static final String TABLE_MATCH_DELIVERY_EVENTS = "MATCH_DELIVERY_EVENTS";
    public static final String COLUMN_EVENT_UUID = "EVENT_UUID";
    public static final String COLUMN_EVENT_MATCH_UUID = "MATCH_UUID";
    public static final String COLUMN_EVENT_INNINGS = "INNINGS_NUMBER";
    public static final String COLUMN_EVENT_SEQ = "SEQ_NUMBER";
    public static final String COLUMN_EVENT_PAYLOAD = "EVENT_PAYLOAD";
    public static final String COLUMN_EVENT_IS_UNDONE = "IS_UNDONE";

    public static final String CREATE_TABLE_MATCH_DELIVERY_EVENTS = "CREATE TABLE IF NOT EXISTS " + TABLE_MATCH_DELIVERY_EVENTS + "("
            + COLUMN_EVENT_UUID + " TEXT PRIMARY KEY,"
            + COLUMN_EVENT_MATCH_UUID + " TEXT NOT NULL,"
            + COLUMN_EVENT_INNINGS + " INTEGER NOT NULL,"
            + COLUMN_EVENT_SEQ + " INTEGER NOT NULL,"
            + COLUMN_EVENT_PAYLOAD + " TEXT NOT NULL,"
            + COLUMN_UPDATED_AT + " INTEGER NOT NULL,"
            + COLUMN_EVENT_IS_UNDONE + " INTEGER DEFAULT 0);";

    //Declaring all Tables

    //Declaring table -RULES_DETAILS
    private static final String TABLE_RULES_DETAILS = "RULES_DETAILS";

    //Declaring Columns-- RULES_DETAILS
    private static final String COLUMN_RULE_ID = "RULE_ID";
    private static final String COLUMN_RULE_DESCRIPTION = "RULE_DESCRIPTION";
    private static final String COLUMN_RULE_STATUS = "RULE_STATUS";

    //Rules
    String CREATE_TABLE_RULES_DETAILS = "CREATE TABLE IF NOT EXISTS " + TABLE_RULES_DETAILS + "("

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
    public String CREATE_TABLE_MATCH_DETAILS = "CREATE TABLE IF NOT EXISTS " + TABLE_MATCH_DETAILS + "("
            + COLUMN_MATCH_USER_ID + " TEXT,"
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
    public String CREATE_TABLE_USER_DETAILS = "CREATE TABLE IF NOT EXISTS " + TABLE_USER_DETAILS + "("
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
    public String CREATE_TABLE_PLAYER_DETAILS = "CREATE TABLE IF NOT EXISTS " + TABLE_PLAYER_DETAILS + "("
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
    public String CREATE_TABLE_TEAM_DETAILS = "CREATE TABLE IF NOT EXISTS " + TABLE_TEAM_DETAILS + "("
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

    public String CREATE_TABLE_SCHEDULE_DETAILS = "CREATE TABLE IF NOT EXISTS " + TABLE_SCHEDULE_DETAILS + "("
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
    public String CREATE_TABLE_GROUND_DETAILS = "CREATE TABLE IF NOT EXISTS " + TABLE_GROUND_DETAILS + "("
            + COLUMN_GROUND_USER_ID + " TEXT,"
            + COLUMN_GROUND_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_GROUND_NAME + " TEXT,"
            + COLUMN_GROUND_CITY + " TEXT,"
            + COLUMN_GROUND_LOC_X + " REAL,"
            + COLUMN_GROUND_LOC_Y + " REAL );";


    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.appContext = context != null ? context.getApplicationContext() : null;
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
                CREATE_TABLE_GROUND_DETAILS,
                CREATE_TABLE_SYNC_OUTBOX,
                CREATE_TABLE_RULES_PROFILES,
                CREATE_TABLE_MATCH_SESSIONS,
                CREATE_TABLE_MATCH_DELIVERY_EVENTS};

        for (String sql : createStatements) {
            db.execSQL(sql);
        }
        ensureSyncSchemaAndBackfill(db);
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
        if (oldVersion < 2) {
            // MATCH_DETAILS was created with a malformed first column in v1 and never written to,
            // so it is safe to recreate. All other tables (teams, players, schedules) are kept.
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MATCH_DETAILS);
        }
        // Create any missing tables (CREATE TABLE IF NOT EXISTS keeps existing data).
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            ensureSyncSchemaAndBackfill(db);
        }
    }

    private String getActiveClubId() {
        if (appContext != null) {
            return SyncPreferences.get(appContext).getClubId();
        }
        return SyncPreferences.DEFAULT_LOCAL_CLUB_ID;
    }

    /**
     * Ensures sync metadata columns exist on synchronizable tables and backfills
     * missing {@code SYNC_UUID} values on pre-existing local records without losing data.
     */
    private void ensureSyncSchemaAndBackfill(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SYNC_OUTBOX);
        db.execSQL(CREATE_TABLE_RULES_PROFILES);
        db.execSQL(CREATE_TABLE_MATCH_SESSIONS);
        db.execSQL(CREATE_TABLE_MATCH_DELIVERY_EVENTS);
        ensureTableSyncColumns(db, TABLE_TEAM_DETAILS, COLUMN_TEAM_ID);
        ensureTableSyncColumns(db, TABLE_PLAYER_DETAILS, COLUMN_PLAYER_ID);
        ensureTableSyncColumns(db, TABLE_SCHEDULE_DETAILS, COLUMN_SCHEDULE_MATCH_ID);
        ensureTableSyncColumns(db, TABLE_GROUND_DETAILS, COLUMN_GROUND_ID);
    }

    private void ensureTableSyncColumns(SQLiteDatabase db, String tableName, String pkColumn) {
        Set<String> existingCols = new HashSet<>();
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + tableName + ")", null)) {
            int nameIdx = c.getColumnIndex("name");
            while (c.moveToNext() && nameIdx >= 0) {
                existingCols.add(c.getString(nameIdx));
            }
        }
        if (!existingCols.contains(COLUMN_SYNC_UUID)) {
            db.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + COLUMN_SYNC_UUID + " TEXT");
        }
        if (!existingCols.contains(COLUMN_CLUB_ID)) {
            db.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + COLUMN_CLUB_ID
                    + " TEXT DEFAULT '" + SyncPreferences.DEFAULT_LOCAL_CLUB_ID + "'");
        }
        if (!existingCols.contains(COLUMN_UPDATED_AT)) {
            db.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + COLUMN_UPDATED_AT + " INTEGER DEFAULT 0");
        }
        if (!existingCols.contains(COLUMN_IS_DELETED)) {
            db.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + COLUMN_IS_DELETED + " INTEGER DEFAULT 0");
        }
        if (!existingCols.contains(COLUMN_SYNC_STATUS)) {
            db.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + COLUMN_SYNC_STATUS
                    + " TEXT DEFAULT '" + SyncRecordModels.STATUS_DIRTY + "'");
        }
        if (!existingCols.contains(COLUMN_REMOTE_VERSION)) {
            db.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + COLUMN_REMOTE_VERSION + " INTEGER DEFAULT 0");
        }

        // Backfill SYNC_UUID and UPDATED_AT on any legacy pre-existing rows
        long now = System.currentTimeMillis();
        String clubId = getActiveClubId();
        try (Cursor c = db.rawQuery("SELECT " + pkColumn + ", " + COLUMN_SYNC_UUID + ", " + COLUMN_UPDATED_AT
                + " FROM " + tableName, null)) {
            while (c.moveToNext()) {
                int id = c.getInt(0);
                String uuid = c.isNull(1) ? "" : c.getString(1);
                long updatedAt = c.isNull(2) ? 0L : c.getLong(2);
                if (uuid == null || uuid.trim().isEmpty()) {
                    ContentValues cv = new ContentValues();
                    cv.put(COLUMN_SYNC_UUID, UUID.randomUUID().toString());
                    cv.put(COLUMN_CLUB_ID, clubId);
                    cv.put(COLUMN_UPDATED_AT, updatedAt > 0 ? updatedAt : now);
                    cv.put(COLUMN_IS_DELETED, 0);
                    cv.put(COLUMN_SYNC_STATUS, SyncRecordModels.STATUS_DIRTY);
                    db.update(tableName, cv, pkColumn + " = ?", new String[]{String.valueOf(id)});
                }
            }
        }
    }

    private void enqueueOutboxInternal(SQLiteDatabase db, String entityType, String entityUuid,
                                       String operation, String payloadJson) {
        if (entityUuid == null || entityUuid.isEmpty() || payloadJson == null) {
            return;
        }
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_OUTBOX_ENTITY_TYPE, entityType);
        cv.put(COLUMN_OUTBOX_ENTITY_UUID, entityUuid);
        cv.put(COLUMN_OUTBOX_OPERATION, operation);
        cv.put(COLUMN_OUTBOX_PAYLOAD, payloadJson);
        cv.put(COLUMN_OUTBOX_CREATED_AT, System.currentTimeMillis());
        cv.put(COLUMN_OUTBOX_RETRY_COUNT, 0);
        cv.putNull(COLUMN_OUTBOX_LAST_ERROR);
        db.insertWithOnConflict(TABLE_SYNC_OUTBOX, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        try {
            CricketSyncWorker.enqueueIfEnabled(appContext);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Inserting new label into lables table
     */
    public void insertTeamBasicDetails(String teamName)//,byte[] imageBytes){//add code to insert image if selected.,int numberOfPlayers
    {
        //column name, column value
        String syncUuid = UUID.randomUUID().toString();
        String clubId = getActiveClubId();
        long now = System.currentTimeMillis();

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
        teamValues.put(COLUMN_SYNC_UUID, syncUuid);
        teamValues.put(COLUMN_CLUB_ID, clubId);
        teamValues.put(COLUMN_UPDATED_AT, now);
        teamValues.put(COLUMN_IS_DELETED, 0);
        teamValues.put(COLUMN_SYNC_STATUS, SyncRecordModels.STATUS_DIRTY);
        teamValues.put(COLUMN_REMOTE_VERSION, 0);
         /*   if(imageBytes!=null) {
                teamValues.put(COLUMN_TEAM_LOGO, imageBytes);
            }
            else{
                teamValues.putNull(COLUMN_TEAM_LOGO);
            }
            */
        // Inserting Row
        long rowId = db.insert(TABLE_TEAM_DETAILS, null, teamValues);//tableName, nullColumnHack, CotentValues
        if (rowId != -1) {
            try {
                TeamSyncRecord rec = new TeamSyncRecord();
                rec.syncUuid = syncUuid;
                rec.clubId = clubId;
                rec.userEmailId = "aditya.juet@gmail.com";
                rec.teamName = teamName;
                rec.matchWon = 0;
                rec.matchLost = 0;
                rec.matchDraw = 0;
                rec.captainId = 0;
                rec.updatedAt = now;
                rec.isDeleted = false;
                rec.remoteVersion = 0L;
                enqueueOutboxInternal(db, SyncRecordModels.ENTITY_TEAM, syncUuid,
                        SyncRecordModels.OP_UPSERT, rec.toJson().toString());
            } catch (Exception e) {
                Log.w("DatabaseHandler", "Failed to enqueue team outbox record", e);
            }
        }

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
        //code to retrieve Teamlogo from DB. (Not implemented; previously threw NumberFormatException.)
        return null;
    }

    public String getTeamName(int teamId) {
        String teamName = "";
        String selectQuery = "SELECT " + COLUMN_TEAM_NAME + " FROM " + TABLE_TEAM_DETAILS + " WHERE " + COLUMN_TEAM_ID + "=" +teamId;
        SQLiteDatabase db=this.getReadableDatabase();

        try (Cursor cursor = db.rawQuery(selectQuery, null)) {
            if (cursor.moveToFirst()) {
                teamName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEAM_NAME));
            }
        }
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
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.query(TABLE_TEAM_DETAILS, new String[]{COLUMN_TEAM_LOGO},
                COLUMN_TEAM_NAME + " = ?", new String[]{teamName}, null, null, null)) {
            if (c.moveToFirst() && c.getType(0) == Cursor.FIELD_TYPE_BLOB) {
                return c.getBlob(0);
            }
        }
        return null;
    }


    //Getting All Team Names
    public ArrayList<String> getAllTeamNames() {
        ArrayList<String> teamlist = new ArrayList<>();


        // Select All Query (excluding soft-deleted tombstones)
        String selectQuery = "SELECT " + COLUMN_TEAM_ID + ", " + COLUMN_TEAM_NAME + " FROM " + TABLE_TEAM_DETAILS
                + " WHERE (" + COLUMN_IS_DELETED + " IS NULL OR " + COLUMN_IS_DELETED + " = 0)";
        //+ COLUMN_TEAM_ID + ','

        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery(selectQuery, null);//selectQuery,selectedArguments
            if (cursor.moveToLast()) {
                do {
                    teamlist.add(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEAM_NAME)));
                    //team.teamId=cursor.getInt(1);
                   /* list.add(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TEAM_ID))+"\t\t\t"+*///teamlist.add(team);
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
        String syncUuid = UUID.randomUUID().toString();
        String clubId = getActiveClubId();
        long now = System.currentTimeMillis();

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

        // Sync Metadata
        playerValues.put(COLUMN_SYNC_UUID, syncUuid);
        playerValues.put(COLUMN_CLUB_ID, clubId);
        playerValues.put(COLUMN_UPDATED_AT, now);
        playerValues.put(COLUMN_IS_DELETED, 0);
        playerValues.put(COLUMN_SYNC_STATUS, SyncRecordModels.STATUS_DIRTY);
        playerValues.put(COLUMN_REMOTE_VERSION, 0);

        // Inserting Row
        long rowId = db.insert(TABLE_PLAYER_DETAILS, null, playerValues);//tableName, nullColumnHack, CotentValues
        if (rowId != -1) {
            try {
                PlayerSyncRecord rec = new PlayerSyncRecord();
                rec.syncUuid = syncUuid;
                rec.clubId = clubId;
                rec.userEmailId = userId;
                rec.playerName = playerName;
                rec.playerEmail = "NotSet";
                rec.playerBatsman = checkBatsman;
                rec.playerBowler = checkbowler;
                rec.playerWicketKeeper = checkWk;
                rec.playerDob = 0;
                rec.teamSyncUuid = "";
                rec.updatedAt = now;
                rec.isDeleted = false;
                rec.remoteVersion = 0L;
                enqueueOutboxInternal(db, SyncRecordModels.ENTITY_PLAYER, syncUuid,
                        SyncRecordModels.OP_UPSERT, rec.toJson().toString());
            } catch (Exception e) {
                Log.w("DatabaseHandler", "Failed to enqueue player outbox record", e);
            }
        }

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
            Log.v("getTeamDetails", "cursor Value" + cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEAM_NAME)));
            team.teamId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TEAM_ID));
            Log.v("getTeamDetails", "COLUMN_TEAM_ID Value" + cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEAM_ID)));

            team.teamName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEAM_NAME));
            Log.v("getTeamDetails", "COLUMN_TEAM_NAME Value" + cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEAM_NAME)));

            //   teams.teamLogo=cursor.getBlob(cursor.getColumnIndexOrThrow(COLUMN_TEAM_LOGO));
            team.matchDraw = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TEAM_MATCHES_DRAW));
            Log.v("getTeamDetails", "COLUMN_TEAM_MATCHES_DRAW Value" + cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEAM_MATCHES_DRAW)));

            team.matchWon = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TEAM_MATCHES_WON));
            Log.v("getTeamDetails", "COLUMN_TEAM_MATCHES_WON Value" + cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEAM_MATCHES_WON)));

            team.matchLost = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TEAM_MATCHES_LOST));
            Log.v("getTeamDetails", "COLUMN_TEAM_MATCHES_LOST Value" + cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEAM_MATCHES_LOST)));

            team.captainId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TEAM_CAPTAIN_ID));
            Log.v("getTeamDetails", "COLUMN_TEAM_CAPTAIN_ID Value" + cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEAM_CAPTAIN_ID)));

            //   team.teamPlayerCount= cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TEAM_NUMBER_OF_PLAYERS));

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
                Log.v("LetMeSee", "Hello " + cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TEAM_ID)));
                // if (cursor.) {
                //  do {
                teamId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TEAM_ID));
                //team.teamId=cursor.getInt(1);
                   /* list.add(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TEAM_ID))+"\t\t\t"+*///teamlist.add(team);
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

            player.playerName=cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_NAME));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_NAME Value: " + cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_NAME)));
            player.playerID=cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_ID));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_ID Value: " + cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_ID)));
            player.playerEmail=cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_EMAIL));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_EMAIL Value: " + cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_EMAIL)));
            player.playerBatsmen=cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_BATSMAN));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_BATSMAN Value: " + cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_BATSMAN)));
            player.playerBowler=cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_BOWLER));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_BOWLER Value: " + cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_BOWLER)));
            player.playerWK=cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_WICKETKEEPER));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_WICKETKEEPER Value: " + cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_WICKETKEEPER)));
          //  player.playerImage=cursor.getBlob(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_IMAGE));
            player.playerDOB=cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_DOB));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_DOB Value: " + cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_DOB)));
            player.playerTeamId=cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_TEAM_ID));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_TEAM_ID Value: " +cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_TEAM_ID)));
            player.playerTotalRuns=cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_TOTAL_RUNS));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_TOTAL_RUNS Value: " + cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_TOTAL_RUNS)));
            player.playerFifty=cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_FIFTY));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_FIFTY Value: " + cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_FIFTY)));
            player.playerHundred=cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_HUNDRED));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_HUNDRED Value: " + cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_HUNDRED)));
            player.playerBallFaced=cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_BALLS_FACED));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_BALLS_FACED Value: " + cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_BALLS_FACED)));
            player.playerTwoHundred=cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_TWO_HUNDRED));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_TWO_HUNDRED Value: " + cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_TWO_HUNDRED)));
            player.playerMatchPlayed=cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_MATCH_PLAYED));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_MATCH_PLAYED Value: " + cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_MATCH_PLAYED)));
            player.playerOverBowled=cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_OVERS_BOWLED));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_OVERS_BOWLED Value: " + cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_OVERS_BOWLED)));
            player.playerWicketTaken=cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_WICKETS_TAKEN));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_WICKETS_TAKEN Value: " + cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_WICKETS_TAKEN)));
            player.playerMaidenBowled=cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_MAIDEN_BOWLED));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_MAIDEN_BOWLED Value: " + cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_MAIDEN_BOWLED)));
            player.playerWidesCount=cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_WIDES_COUNT));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_WIDES_COUNT Value: " + cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_WIDES_COUNT)));
            player.playerNoBallCount=cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_NOBALL_COUNT));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_NOBALL_COUNT Value: " + cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_NOBALL_COUNT)));
            player.playerRunsGiven=cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_RUNS_GIVEN));
            Log.v("getPlayerDetails", "COLUMN_PLAYER_RUNS_GIVEN Value: " + cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_RUNS_GIVEN)));
            player.playerCatches=cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYERS_CATCHES));
            Log.v("getPlayerDetails", "COLUMN_PLAYERS_CATCHES Value: " + cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYERS_CATCHES)));


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

                   /* list.add(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TEAM_ID))+"\t\t\t"+*///list.add(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_ID)));
                playerId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_ID));//adding COLUMN_PLAYER_ID column data
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


        // Select All Query (excluding soft-deleted tombstones)
        String selectQuery = "SELECT " + COLUMN_PLAYER_ID + ", " + COLUMN_PLAYER_NAME + " FROM " + TABLE_PLAYER_DETAILS
                + " WHERE (" + COLUMN_IS_DELETED + " IS NULL OR " + COLUMN_IS_DELETED + " = 0)";
        //+ COLUMN_TEAM_ID + ','

        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery(selectQuery, null);//selectQuery,selectedArguments
            if (cursor.moveToLast()) {
                do {

                   /* list.add(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TEAM_ID))+"\t\t\t"+*///list.add(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_ID)));
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
        long now = System.currentTimeMillis();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PLAYER_TEAM_ID,""+playerteamId);
        cv.put(COLUMN_UPDATED_AT, now);
        cv.put(COLUMN_SYNC_STATUS, SyncRecordModels.STATUS_DIRTY);
        String WHERE=COLUMN_PLAYER_ID+"= ?";
       String[] WHEREARGS=new String[]{String.valueOf(playerId)};
        try{
            SQLiteDatabase db = this.getWritableDatabase();
            db.update(TABLE_PLAYER_DETAILS,cv,WHERE,WHEREARGS);
            enqueuePlayerOutboxById(db, playerId);
            Log.v("addPlayerInTeam:","Player Updated with Team");
            db.close();
        }catch (Exception e){
            Log.v("addPlayerInTeam:","Threw Exception"+e);
        }

        //String query= "UPDATE" +COLUMN_PLAYER_TEAM_ID+"AS"+playerteamId+"WHERE "+ player+

    }

    public void removePlayerInTeam(int playerId){
        int zero=0;
        long now = System.currentTimeMillis();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PLAYER_TEAM_ID,""+zero);
        cv.put(COLUMN_UPDATED_AT, now);
        cv.put(COLUMN_SYNC_STATUS, SyncRecordModels.STATUS_DIRTY);
        String WHERE=COLUMN_PLAYER_ID+"= ?";
        String[] WHEREARGS=new String[]{String.valueOf(playerId)};
        try{
            SQLiteDatabase db = this.getWritableDatabase();
            db.update(TABLE_PLAYER_DETAILS,cv,WHERE,WHEREARGS);
            enqueuePlayerOutboxById(db, playerId);
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
        String selectQuery = "SELECT " +COLUMN_PLAYER_ID+ ", " +COLUMN_PLAYER_NAME+ " FROM " +TABLE_PLAYER_DETAILS
                + " WHERE "+COLUMN_PLAYER_TEAM_ID+"=\'"+teamId+"\'"
                + " AND (" + COLUMN_IS_DELETED + " IS NULL OR " + COLUMN_IS_DELETED + " = 0)";

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(selectQuery, null);//selectQuery,selectedArguments

            if (cursor.moveToLast()) {  //cursor!=null&&
                do {

                   /* list.add(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TEAM_ID))+"\t\t\t"+*///list.add(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_ID)));
                    list.add(cursor.getString(1));//adding 2nd column data
                } while (cursor.moveToPrevious());
            }
            cursor.close();
            db.close();
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
            String syncUuid = UUID.randomUUID().toString();
            String clubId = getActiveClubId();
            long now = System.currentTimeMillis();
            String teamAUuid = getTeamSyncUuid(TeamAId);
            String teamBUuid = getTeamSyncUuid(TeamBId);

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

            // Sync Metadata
            scheduleValues.put(COLUMN_SYNC_UUID, syncUuid);
            scheduleValues.put(COLUMN_CLUB_ID, clubId);
            scheduleValues.put(COLUMN_UPDATED_AT, now);
            scheduleValues.put(COLUMN_IS_DELETED, 0);
            scheduleValues.put(COLUMN_SYNC_STATUS, SyncRecordModels.STATUS_DIRTY);
            scheduleValues.put(COLUMN_REMOTE_VERSION, 0);


            // Inserting Row
            long rowId = db.insert(TABLE_SCHEDULE_DETAILS, null, scheduleValues);//tableName, nullColumnHack, CotentValues
            if (rowId != -1) {
                ScheduleSyncRecord rec = new ScheduleSyncRecord();
                rec.syncUuid = syncUuid;
                rec.clubId = clubId;
                rec.userEmailId = userId;
                rec.matchDate = matchDate;
                rec.teamASyncUuid = teamAUuid;
                rec.teamBSyncUuid = teamBUuid;
                rec.matchOvers = matchOvers;
                rec.matchStatus = "SCHEDULED";
                rec.updatedAt = now;
                rec.isDeleted = false;
                rec.remoteVersion = 0L;
                enqueueOutboxInternal(db, SyncRecordModels.ENTITY_SCHEDULE, syncUuid,
                        SyncRecordModels.OP_UPSERT, rec.toJson().toString());
            }

            db.close(); // Closing database connection
        }catch (Exception e){
            Log.v("addSchedule","Ërror in adding basic schedule details");
        }

    }

    //Used to view all schedules
    public String[][] viewAllSchedules() {
        String selectQuery = "SELECT " + COLUMN_SCHEDULE_MATCH_DATE+ ", " +COLUMN_SCHEDULE_TEAMA_ID+ ", " +COLUMN_SCHEDULE_TEAMB_ID+ ", " +COLUMN_SCHEDULE_MATCH_TYPE+" FROM " +TABLE_SCHEDULE_DETAILS
                + " WHERE (" + COLUMN_IS_DELETED + " IS NULL OR " + COLUMN_IS_DELETED + " = 0)";

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

                   /* list.add(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TEAM_ID))+"\t\t\t"+*///list.add(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_ID)));
                        list[i][j] = cursor.getString(j);//adding 2nd column data

                    }

            }
        }catch (Exception e){
            Log.v("DatabaseHandler:","Exception thrown at viewAllSchedules  "+e);
            Log.v("DatabaseHandler:","Number of Rows "+numberOfRows);
            Log.v("DatabaseHandler:","Number of Columns "+numberOfColumns);
        } finally {
            cursor.close();
            db.close();
        }
        return list;
    }
    public int getNumberOfSchedules(){
        int numberOfRows =0;
       try {
           String selectQuery = "SELECT " + COLUMN_SCHEDULE_MATCH_DATE + ", " + COLUMN_SCHEDULE_TEAMA_ID + ", " + COLUMN_SCHEDULE_TEAMB_ID + ", " + COLUMN_SCHEDULE_MATCH_TYPE + " FROM " + TABLE_SCHEDULE_DETAILS
                   + " WHERE (" + COLUMN_IS_DELETED + " IS NULL OR " + COLUMN_IS_DELETED + " = 0)";
           SQLiteDatabase db = this.getReadableDatabase();
           Cursor cursor = db.rawQuery(selectQuery, null);//selectQuery,selectedArguments
           numberOfRows = cursor.getCount();
           cursor.close();
           db.close();
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

    // =========================================================================
    // Offline-First Cloud Synchronization Engine Helpers (Added in DB v3)
    // =========================================================================

    private void enqueuePlayerOutboxById(SQLiteDatabase db, int playerId) {
        PlayerSyncRecord rec = readPlayerSyncRecordInternal(db, COLUMN_PLAYER_ID + " = ?",
                new String[]{String.valueOf(playerId)});
        if (rec != null) {
            try {
                enqueueOutboxInternal(db, SyncRecordModels.ENTITY_PLAYER, rec.syncUuid,
                        rec.isDeleted ? SyncRecordModels.OP_DELETE : SyncRecordModels.OP_UPSERT,
                        rec.toJson().toString());
            } catch (Exception e) {
                Log.w("DatabaseHandler", "Failed to enqueue updated player outbox", e);
            }
        }
    }

    public int getPendingOutboxCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SYNC_OUTBOX, null)) {
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
        } catch (Exception e) {
            Log.w("DatabaseHandler", "getPendingOutboxCount failed", e);
        }
        return 0;
    }

    public List<OutboxItem> getPendingOutboxItems() {
        List<OutboxItem> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT " + COLUMN_OUTBOX_ID + ", "
                + COLUMN_OUTBOX_ENTITY_TYPE + ", "
                + COLUMN_OUTBOX_ENTITY_UUID + ", "
                + COLUMN_OUTBOX_OPERATION + ", "
                + COLUMN_OUTBOX_PAYLOAD + ", "
                + COLUMN_OUTBOX_CREATED_AT + ", "
                + COLUMN_OUTBOX_RETRY_COUNT + ", "
                + COLUMN_OUTBOX_LAST_ERROR
                + " FROM " + TABLE_SYNC_OUTBOX
                + " ORDER BY CASE " + COLUMN_OUTBOX_ENTITY_TYPE
                + " WHEN '" + SyncRecordModels.ENTITY_TEAM + "' THEN 1"
                + " WHEN '" + SyncRecordModels.ENTITY_GROUND + "' THEN 2"
                + " WHEN '" + SyncRecordModels.ENTITY_PLAYER + "' THEN 3"
                + " WHEN '" + SyncRecordModels.ENTITY_SCHEDULE + "' THEN 4"
                + " ELSE 5 END, " + COLUMN_OUTBOX_ID + " ASC";
        try (Cursor c = db.rawQuery(sql, null)) {
            while (c.moveToNext()) {
                OutboxItem item = new OutboxItem();
                item.outboxId = c.getLong(0);
                item.entityType = c.getString(1);
                item.entityUuid = c.getString(2);
                item.operation = c.getString(3);
                item.payloadJson = c.getString(4);
                item.createdAt = c.getLong(5);
                item.retryCount = c.getInt(6);
                item.lastError = c.isNull(7) ? "" : c.getString(7);
                items.add(item);
            }
        }
        return items;
    }

    public void markOutboxItemSynced(long outboxId, String entityType, String entityUuid, long remoteVersion) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.delete(TABLE_SYNC_OUTBOX, COLUMN_OUTBOX_ID + " = ?", new String[]{String.valueOf(outboxId)});
            String table = resolveTableForEntityType(entityType);
            if (table != null && entityUuid != null) {
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_SYNC_STATUS, SyncRecordModels.STATUS_SYNCED);
                cv.put(COLUMN_REMOTE_VERSION, remoteVersion);
                db.update(table, cv, COLUMN_SYNC_UUID + " = ?", new String[]{entityUuid});
            }
        } finally {
            db.close();
        }
    }

    public void markOutboxItemFailed(long outboxId, String error) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.execSQL("UPDATE " + TABLE_SYNC_OUTBOX
                    + " SET " + COLUMN_OUTBOX_RETRY_COUNT + " = " + COLUMN_OUTBOX_RETRY_COUNT + " + 1, "
                    + COLUMN_OUTBOX_LAST_ERROR + " = ?"
                    + " WHERE " + COLUMN_OUTBOX_ID + " = ?",
                    new Object[]{error == null ? "Unknown error" : error, outboxId});
        } finally {
            db.close();
        }
    }

    private String resolveTableForEntityType(String entityType) {
        if (SyncRecordModels.ENTITY_TEAM.equals(entityType)) {
            return TABLE_TEAM_DETAILS;
        }
        if (SyncRecordModels.ENTITY_PLAYER.equals(entityType)) {
            return TABLE_PLAYER_DETAILS;
        }
        if (SyncRecordModels.ENTITY_SCHEDULE.equals(entityType)) {
            return TABLE_SCHEDULE_DETAILS;
        }
        if (SyncRecordModels.ENTITY_GROUND.equals(entityType)) {
            return TABLE_GROUND_DETAILS;
        }
        return null;
    }

    public String getTeamSyncUuid(int teamId) {
        if (teamId <= 0) {
            return "";
        }
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.query(TABLE_TEAM_DETAILS, new String[]{COLUMN_SYNC_UUID},
                COLUMN_TEAM_ID + " = ?", new String[]{String.valueOf(teamId)},
                null, null, null)) {
            if (c.moveToFirst() && !c.isNull(0)) {
                return c.getString(0);
            }
        }
        return "";
    }

    public int getTeamIdBySyncUuid(String syncUuid) {
        if (syncUuid == null || syncUuid.isEmpty()) {
            return 0;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        return getTeamIdBySyncUuidInternal(db, syncUuid);
    }

    private int getTeamIdBySyncUuidInternal(SQLiteDatabase db, String syncUuid) {
        if (syncUuid == null || syncUuid.isEmpty()) {
            return 0;
        }
        try (Cursor c = db.query(TABLE_TEAM_DETAILS, new String[]{COLUMN_TEAM_ID},
                COLUMN_SYNC_UUID + " = ?", new String[]{syncUuid},
                null, null, null)) {
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
        }
        return 0;
    }

    public List<TeamSyncRecord> getAllTeamsForSync() {
        List<TeamSyncRecord> result = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.query(TABLE_TEAM_DETAILS, null, null, null, null, null, COLUMN_TEAM_ID + " ASC")) {
            while (c.moveToNext()) {
                result.add(cursorToTeamSyncRecord(c));
            }
        }
        return result;
    }

    public TeamSyncRecord getTeamBySyncUuid(String syncUuid) {
        if (syncUuid == null || syncUuid.isEmpty()) {
            return null;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.query(TABLE_TEAM_DETAILS, null,
                COLUMN_SYNC_UUID + " = ?", new String[]{syncUuid},
                null, null, null)) {
            if (c.moveToFirst()) {
                return cursorToTeamSyncRecord(c);
            }
        }
        return null;
    }

    public TeamSyncRecord getTeamByNormalizedName(String normalizedName) {
        if (normalizedName == null || normalizedName.isEmpty()) {
            return null;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.query(TABLE_TEAM_DETAILS, null,
                "LOWER(TRIM(" + COLUMN_TEAM_NAME + ")) = ?",
                new String[]{SyncRecordModels.normalizeName(normalizedName)},
                null, null, null)) {
            if (c.moveToFirst()) {
                return cursorToTeamSyncRecord(c);
            }
        }
        return null;
    }

    private TeamSyncRecord cursorToTeamSyncRecord(Cursor c) {
        TeamSyncRecord r = new TeamSyncRecord();
        r.localTeamId = c.getInt(c.getColumnIndexOrThrow(COLUMN_TEAM_ID));
        r.userEmailId = c.getString(c.getColumnIndexOrThrow(COLUMN_TEAM_USER_ID));
        r.teamName = c.getString(c.getColumnIndexOrThrow(COLUMN_TEAM_NAME));
        r.teamNameNormalized = SyncRecordModels.normalizeName(r.teamName);
        r.matchWon = c.getInt(c.getColumnIndexOrThrow(COLUMN_TEAM_MATCHES_WON));
        r.matchLost = c.getInt(c.getColumnIndexOrThrow(COLUMN_TEAM_MATCHES_LOST));
        r.matchDraw = c.getInt(c.getColumnIndexOrThrow(COLUMN_TEAM_MATCHES_DRAW));
        r.captainId = c.getInt(c.getColumnIndexOrThrow(COLUMN_TEAM_CAPTAIN_ID));
        int uuidIdx = c.getColumnIndex(COLUMN_SYNC_UUID);
        r.syncUuid = uuidIdx >= 0 && !c.isNull(uuidIdx) ? c.getString(uuidIdx) : "";
        int clubIdx = c.getColumnIndex(COLUMN_CLUB_ID);
        r.clubId = clubIdx >= 0 && !c.isNull(clubIdx) ? c.getString(clubIdx) : SyncPreferences.DEFAULT_LOCAL_CLUB_ID;
        int updIdx = c.getColumnIndex(COLUMN_UPDATED_AT);
        r.updatedAt = updIdx >= 0 && !c.isNull(updIdx) ? c.getLong(updIdx) : 0L;
        int delIdx = c.getColumnIndex(COLUMN_IS_DELETED);
        r.isDeleted = delIdx >= 0 && !c.isNull(delIdx) && c.getInt(delIdx) != 0;
        int verIdx = c.getColumnIndex(COLUMN_REMOTE_VERSION);
        r.remoteVersion = verIdx >= 0 && !c.isNull(verIdx) ? c.getLong(verIdx) : 0L;
        int statusIdx = c.getColumnIndex(COLUMN_SYNC_STATUS);
        r.syncStatus = statusIdx >= 0 && !c.isNull(statusIdx) ? c.getString(statusIdx) : SyncRecordModels.STATUS_DIRTY;
        return r;
    }

    /**
     * Upserts a remote {@link TeamSyncRecord} into {@code TEAM_DETAILS}, matching first by
     * {@code SYNC_UUID} and falling back to normalized {@code TEAM_NAME} for first-time natural-key deduplication.
     */
    public void upsertTeamFromRemote(TeamSyncRecord remote, boolean markSynced) {
        if (remote == null || remote.syncUuid == null || remote.syncUuid.isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            TeamSyncRecord existing = null;
            try (Cursor c = db.query(TABLE_TEAM_DETAILS, null,
                    COLUMN_SYNC_UUID + " = ?", new String[]{remote.syncUuid},
                    null, null, null)) {
                if (c.moveToFirst()) {
                    existing = cursorToTeamSyncRecord(c);
                }
            }
            if (existing == null && remote.teamName != null && !remote.teamName.isEmpty()) {
                try (Cursor c = db.query(TABLE_TEAM_DETAILS, null,
                        "LOWER(TRIM(" + COLUMN_TEAM_NAME + ")) = ?",
                        new String[]{SyncRecordModels.normalizeName(remote.teamName)},
                        null, null, null)) {
                    if (c.moveToFirst()) {
                        existing = cursorToTeamSyncRecord(c);
                    }
                }
            }

            ContentValues cv = new ContentValues();
            cv.put(COLUMN_TEAM_USER_ID, remote.userEmailId == null ? "aditya.juet@gmail.com" : remote.userEmailId);
            cv.put(COLUMN_TEAM_NAME, remote.teamName);
            cv.put(COLUMN_TEAM_MATCHES_WON, remote.matchWon);
            cv.put(COLUMN_TEAM_MATCHES_LOST, remote.matchLost);
            cv.put(COLUMN_TEAM_MATCHES_DRAW, remote.matchDraw);
            cv.put(COLUMN_TEAM_CAPTAIN_ID, remote.captainId);
            cv.put(COLUMN_SYNC_UUID, remote.syncUuid);
            cv.put(COLUMN_CLUB_ID, remote.clubId == null ? getActiveClubId() : remote.clubId);
            cv.put(COLUMN_UPDATED_AT, remote.updatedAt);
            cv.put(COLUMN_IS_DELETED, remote.isDeleted ? 1 : 0);
            cv.put(COLUMN_REMOTE_VERSION, remote.remoteVersion);
            cv.put(COLUMN_SYNC_STATUS, markSynced ? SyncRecordModels.STATUS_SYNCED : SyncRecordModels.STATUS_DIRTY);

            if (existing != null) {
                String oldUuid = existing.syncUuid;
                db.update(TABLE_TEAM_DETAILS, cv,
                        COLUMN_TEAM_ID + " = ?", new String[]{String.valueOf(existing.localTeamId)});
                if (oldUuid != null && !oldUuid.equals(remote.syncUuid)) {
                    // Remove superseded local UUID from outbox after natural-key merge
                    db.delete(TABLE_SYNC_OUTBOX,
                            COLUMN_OUTBOX_ENTITY_TYPE + " = ? AND " + COLUMN_OUTBOX_ENTITY_UUID + " = ?",
                            new String[]{SyncRecordModels.ENTITY_TEAM, oldUuid});
                }
            } else {
                cv.put(COLUMN_TEAM_LOGO, "NotSet");
                db.insert(TABLE_TEAM_DETAILS, null, cv);
            }

            if (!markSynced) {
                enqueueOutboxInternal(db, SyncRecordModels.ENTITY_TEAM, remote.syncUuid,
                        remote.isDeleted ? SyncRecordModels.OP_DELETE : SyncRecordModels.OP_UPSERT,
                        remote.toJson().toString());
            } else {
                db.delete(TABLE_SYNC_OUTBOX,
                        COLUMN_OUTBOX_ENTITY_TYPE + " = ? AND " + COLUMN_OUTBOX_ENTITY_UUID + " = ?",
                        new String[]{SyncRecordModels.ENTITY_TEAM, remote.syncUuid});
            }
        } catch (Exception e) {
            Log.w("DatabaseHandler", "upsertTeamFromRemote failed", e);
        } finally {
            db.close();
        }
    }

    public List<PlayerSyncRecord> getAllPlayersForSync() {
        List<PlayerSyncRecord> result = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.query(TABLE_PLAYER_DETAILS, null, null, null, null, null, COLUMN_PLAYER_ID + " ASC")) {
            while (c.moveToNext()) {
                result.add(cursorToPlayerSyncRecord(db, c));
            }
        }
        return result;
    }

    public PlayerSyncRecord getPlayerBySyncUuid(String syncUuid) {
        if (syncUuid == null || syncUuid.isEmpty()) {
            return null;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        return readPlayerSyncRecordInternal(db, COLUMN_SYNC_UUID + " = ?", new String[]{syncUuid});
    }

    private PlayerSyncRecord readPlayerSyncRecordInternal(SQLiteDatabase db, String selection, String[] args) {
        try (Cursor c = db.query(TABLE_PLAYER_DETAILS, null, selection, args, null, null, null)) {
            if (c.moveToFirst()) {
                return cursorToPlayerSyncRecord(db, c);
            }
        }
        return null;
    }

    private PlayerSyncRecord cursorToPlayerSyncRecord(SQLiteDatabase db, Cursor c) {
        PlayerSyncRecord r = new PlayerSyncRecord();
        r.localPlayerId = c.getInt(c.getColumnIndexOrThrow(COLUMN_PLAYER_ID));
        r.userEmailId = c.getString(c.getColumnIndexOrThrow(COLUMN_PLAYER_USER_ID));
        r.playerName = c.getString(c.getColumnIndexOrThrow(COLUMN_PLAYER_NAME));
        r.playerNameNormalized = SyncRecordModels.normalizeName(r.playerName);
        r.playerEmail = c.getString(c.getColumnIndexOrThrow(COLUMN_PLAYER_EMAIL));
        r.playerBatsman = c.getInt(c.getColumnIndexOrThrow(COLUMN_PLAYER_BATSMAN));
        r.playerBowler = c.getInt(c.getColumnIndexOrThrow(COLUMN_PLAYER_BOWLER));
        r.playerWicketKeeper = c.getInt(c.getColumnIndexOrThrow(COLUMN_PLAYER_WICKETKEEPER));
        r.playerDob = c.getInt(c.getColumnIndexOrThrow(COLUMN_PLAYER_DOB));
        r.localTeamId = c.getInt(c.getColumnIndexOrThrow(COLUMN_PLAYER_TEAM_ID));
        r.teamSyncUuid = "";
        if (r.localTeamId > 0) {
            try (Cursor tc = db.query(TABLE_TEAM_DETAILS, new String[]{COLUMN_SYNC_UUID},
                    COLUMN_TEAM_ID + " = ?", new String[]{String.valueOf(r.localTeamId)},
                    null, null, null)) {
                if (tc.moveToFirst() && !tc.isNull(0)) {
                    r.teamSyncUuid = tc.getString(0);
                }
            }
        }
        r.totalRuns = c.getInt(c.getColumnIndexOrThrow(COLUMN_PLAYER_TOTAL_RUNS));
        r.fifty = c.getInt(c.getColumnIndexOrThrow(COLUMN_PLAYER_FIFTY));
        r.hundred = c.getInt(c.getColumnIndexOrThrow(COLUMN_PLAYER_HUNDRED));
        r.twoHundred = c.getInt(c.getColumnIndexOrThrow(COLUMN_PLAYER_TWO_HUNDRED));
        r.ballsFaced = c.getInt(c.getColumnIndexOrThrow(COLUMN_PLAYER_BALLS_FACED));
        r.matchPlayed = c.getInt(c.getColumnIndexOrThrow(COLUMN_PLAYER_MATCH_PLAYED));
        r.oversBowled = c.getInt(c.getColumnIndexOrThrow(COLUMN_PLAYER_OVERS_BOWLED));
        r.wicketsTaken = c.getInt(c.getColumnIndexOrThrow(COLUMN_PLAYER_WICKETS_TAKEN));
        r.maidenBowled = c.getInt(c.getColumnIndexOrThrow(COLUMN_PLAYER_MAIDEN_BOWLED));
        r.widesCount = c.getInt(c.getColumnIndexOrThrow(COLUMN_PLAYER_WIDES_COUNT));
        r.noBallCount = c.getInt(c.getColumnIndexOrThrow(COLUMN_PLAYER_NOBALL_COUNT));
        r.runsGiven = c.getInt(c.getColumnIndexOrThrow(COLUMN_PLAYER_RUNS_GIVEN));
        r.catches = c.getInt(c.getColumnIndexOrThrow(COLUMN_PLAYERS_CATCHES));

        int uuidIdx = c.getColumnIndex(COLUMN_SYNC_UUID);
        r.syncUuid = uuidIdx >= 0 && !c.isNull(uuidIdx) ? c.getString(uuidIdx) : "";
        int clubIdx = c.getColumnIndex(COLUMN_CLUB_ID);
        r.clubId = clubIdx >= 0 && !c.isNull(clubIdx) ? c.getString(clubIdx) : SyncPreferences.DEFAULT_LOCAL_CLUB_ID;
        int updIdx = c.getColumnIndex(COLUMN_UPDATED_AT);
        r.updatedAt = updIdx >= 0 && !c.isNull(updIdx) ? c.getLong(updIdx) : 0L;
        int delIdx = c.getColumnIndex(COLUMN_IS_DELETED);
        r.isDeleted = delIdx >= 0 && !c.isNull(delIdx) && c.getInt(delIdx) != 0;
        int verIdx = c.getColumnIndex(COLUMN_REMOTE_VERSION);
        r.remoteVersion = verIdx >= 0 && !c.isNull(verIdx) ? c.getLong(verIdx) : 0L;
        int statusIdx = c.getColumnIndex(COLUMN_SYNC_STATUS);
        r.syncStatus = statusIdx >= 0 && !c.isNull(statusIdx) ? c.getString(statusIdx) : SyncRecordModels.STATUS_DIRTY;
        return r;
    }

    /**
     * Upserts a remote {@link PlayerSyncRecord} into {@code PLAYER_DETAILS}, resolving
     * {@code teamSyncUuid} to the local {@code TEAM_ID} foreign key.
     */
    public void upsertPlayerFromRemote(PlayerSyncRecord remote, boolean markSynced) {
        if (remote == null || remote.syncUuid == null || remote.syncUuid.isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            PlayerSyncRecord existing = readPlayerSyncRecordInternal(db,
                    COLUMN_SYNC_UUID + " = ?", new String[]{remote.syncUuid});
            if (existing == null && remote.playerName != null && !remote.playerName.isEmpty()) {
                existing = readPlayerSyncRecordInternal(db,
                        "LOWER(TRIM(" + COLUMN_PLAYER_NAME + ")) = ?",
                        new String[]{SyncRecordModels.normalizeName(remote.playerName)});
            }

            int resolvedTeamId = getTeamIdBySyncUuidInternal(db, remote.teamSyncUuid);

            ContentValues cv = new ContentValues();
            cv.put(COLUMN_PLAYER_USER_ID, remote.userEmailId == null ? "aditya.juet@gmail.com" : remote.userEmailId);
            cv.put(COLUMN_PLAYER_NAME, remote.playerName);
            cv.put(COLUMN_PLAYER_EMAIL, remote.playerEmail == null ? "NotSet" : remote.playerEmail);
            cv.put(COLUMN_PLAYER_BATSMAN, remote.playerBatsman);
            cv.put(COLUMN_PLAYER_BOWLER, remote.playerBowler);
            cv.put(COLUMN_PLAYER_WICKETKEEPER, remote.playerWicketKeeper);
            cv.put(COLUMN_PLAYER_DOB, remote.playerDob);
            cv.put(COLUMN_PLAYER_TEAM_ID, String.valueOf(resolvedTeamId));
            cv.put(COLUMN_PLAYER_TOTAL_RUNS, remote.totalRuns);
            cv.put(COLUMN_PLAYER_FIFTY, remote.fifty);
            cv.put(COLUMN_PLAYER_HUNDRED, remote.hundred);
            cv.put(COLUMN_PLAYER_TWO_HUNDRED, remote.twoHundred);
            cv.put(COLUMN_PLAYER_BALLS_FACED, remote.ballsFaced);
            cv.put(COLUMN_PLAYER_MATCH_PLAYED, remote.matchPlayed);
            cv.put(COLUMN_PLAYER_OVERS_BOWLED, remote.oversBowled);
            cv.put(COLUMN_PLAYER_WICKETS_TAKEN, remote.wicketsTaken);
            cv.put(COLUMN_PLAYER_MAIDEN_BOWLED, remote.maidenBowled);
            cv.put(COLUMN_PLAYER_WIDES_COUNT, remote.widesCount);
            cv.put(COLUMN_PLAYER_NOBALL_COUNT, remote.noBallCount);
            cv.put(COLUMN_PLAYER_RUNS_GIVEN, remote.runsGiven);
            cv.put(COLUMN_PLAYERS_CATCHES, remote.catches);
            cv.put(COLUMN_SYNC_UUID, remote.syncUuid);
            cv.put(COLUMN_CLUB_ID, remote.clubId == null ? getActiveClubId() : remote.clubId);
            cv.put(COLUMN_UPDATED_AT, remote.updatedAt);
            cv.put(COLUMN_IS_DELETED, remote.isDeleted ? 1 : 0);
            cv.put(COLUMN_REMOTE_VERSION, remote.remoteVersion);
            cv.put(COLUMN_SYNC_STATUS, markSynced ? SyncRecordModels.STATUS_SYNCED : SyncRecordModels.STATUS_DIRTY);

            if (existing != null) {
                String oldUuid = existing.syncUuid;
                db.update(TABLE_PLAYER_DETAILS, cv,
                        COLUMN_PLAYER_ID + " = ?", new String[]{String.valueOf(existing.localPlayerId)});
                if (oldUuid != null && !oldUuid.equals(remote.syncUuid)) {
                    db.delete(TABLE_SYNC_OUTBOX,
                            COLUMN_OUTBOX_ENTITY_TYPE + " = ? AND " + COLUMN_OUTBOX_ENTITY_UUID + " = ?",
                            new String[]{SyncRecordModels.ENTITY_PLAYER, oldUuid});
                }
            } else {
                cv.put(COLUMN_PLAYER_IMAGE, "NotSet");
                db.insert(TABLE_PLAYER_DETAILS, null, cv);
            }

            if (!markSynced) {
                enqueueOutboxInternal(db, SyncRecordModels.ENTITY_PLAYER, remote.syncUuid,
                        remote.isDeleted ? SyncRecordModels.OP_DELETE : SyncRecordModels.OP_UPSERT,
                        remote.toJson().toString());
            } else {
                db.delete(TABLE_SYNC_OUTBOX,
                        COLUMN_OUTBOX_ENTITY_TYPE + " = ? AND " + COLUMN_OUTBOX_ENTITY_UUID + " = ?",
                        new String[]{SyncRecordModels.ENTITY_PLAYER, remote.syncUuid});
            }
        } catch (Exception e) {
            Log.w("DatabaseHandler", "upsertPlayerFromRemote failed", e);
        } finally {
            db.close();
        }
    }

    public List<ScheduleSyncRecord> getAllSchedulesForSync() {
        List<ScheduleSyncRecord> result = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.query(TABLE_SCHEDULE_DETAILS, null, null, null, null, null,
                COLUMN_SCHEDULE_MATCH_ID + " ASC")) {
            while (c.moveToNext()) {
                result.add(cursorToScheduleSyncRecord(db, c));
            }
        }
        return result;
    }

    public ScheduleSyncRecord getScheduleBySyncUuid(String syncUuid) {
        if (syncUuid == null || syncUuid.isEmpty()) {
            return null;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.query(TABLE_SCHEDULE_DETAILS, null,
                COLUMN_SYNC_UUID + " = ?", new String[]{syncUuid},
                null, null, null)) {
            if (c.moveToFirst()) {
                return cursorToScheduleSyncRecord(db, c);
            }
        }
        return null;
    }

    private ScheduleSyncRecord cursorToScheduleSyncRecord(SQLiteDatabase db, Cursor c) {
        ScheduleSyncRecord r = new ScheduleSyncRecord();
        r.localScheduleId = c.getInt(c.getColumnIndexOrThrow(COLUMN_SCHEDULE_MATCH_ID));
        r.userEmailId = c.getString(c.getColumnIndexOrThrow(COLUMN_SCHEDULE_USER_ID));
        r.matchDate = c.getString(c.getColumnIndexOrThrow(COLUMN_SCHEDULE_MATCH_DATE));
        r.localTeamAId = c.getInt(c.getColumnIndexOrThrow(COLUMN_SCHEDULE_TEAMA_ID));
        r.localTeamBId = c.getInt(c.getColumnIndexOrThrow(COLUMN_SCHEDULE_TEAMB_ID));
        r.teamASyncUuid = "";
        r.teamBSyncUuid = "";
        if (r.localTeamAId > 0) {
            try (Cursor tc = db.query(TABLE_TEAM_DETAILS, new String[]{COLUMN_SYNC_UUID},
                    COLUMN_TEAM_ID + " = ?", new String[]{String.valueOf(r.localTeamAId)},
                    null, null, null)) {
                if (tc.moveToFirst() && !tc.isNull(0)) {
                    r.teamASyncUuid = tc.getString(0);
                }
            }
        }
        if (r.localTeamBId > 0) {
            try (Cursor tc = db.query(TABLE_TEAM_DETAILS, new String[]{COLUMN_SYNC_UUID},
                    COLUMN_TEAM_ID + " = ?", new String[]{String.valueOf(r.localTeamBId)},
                    null, null, null)) {
                if (tc.moveToFirst() && !tc.isNull(0)) {
                    r.teamBSyncUuid = tc.getString(0);
                }
            }
        }
        r.matchOvers = c.getInt(c.getColumnIndexOrThrow(COLUMN_SCHEDULE_MATCH_TYPE));
        int statusCol = c.getColumnIndex(COLUMN_SCHEDULE_MATCH_STATUS);
        r.matchStatus = statusCol >= 0 && !c.isNull(statusCol) ? c.getString(statusCol) : "SCHEDULED";

        int uuidIdx = c.getColumnIndex(COLUMN_SYNC_UUID);
        r.syncUuid = uuidIdx >= 0 && !c.isNull(uuidIdx) ? c.getString(uuidIdx) : "";
        int clubIdx = c.getColumnIndex(COLUMN_CLUB_ID);
        r.clubId = clubIdx >= 0 && !c.isNull(clubIdx) ? c.getString(clubIdx) : SyncPreferences.DEFAULT_LOCAL_CLUB_ID;
        int updIdx = c.getColumnIndex(COLUMN_UPDATED_AT);
        r.updatedAt = updIdx >= 0 && !c.isNull(updIdx) ? c.getLong(updIdx) : 0L;
        int delIdx = c.getColumnIndex(COLUMN_IS_DELETED);
        r.isDeleted = delIdx >= 0 && !c.isNull(delIdx) && c.getInt(delIdx) != 0;
        int verIdx = c.getColumnIndex(COLUMN_REMOTE_VERSION);
        r.remoteVersion = verIdx >= 0 && !c.isNull(verIdx) ? c.getLong(verIdx) : 0L;
        int statusIdx = c.getColumnIndex(COLUMN_SYNC_STATUS);
        r.syncStatus = statusIdx >= 0 && !c.isNull(statusIdx) ? c.getString(statusIdx) : SyncRecordModels.STATUS_DIRTY;
        return r;
    }

    /**
     * Upserts a remote {@link ScheduleSyncRecord} into {@code SCHEDULE_DETAILS}, resolving
     * {@code teamASyncUuid} and {@code teamBSyncUuid} to local {@code TEAM_ID} foreign keys.
     */
    public void upsertScheduleFromRemote(ScheduleSyncRecord remote, boolean markSynced) {
        if (remote == null || remote.syncUuid == null || remote.syncUuid.isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            int teamAId = getTeamIdBySyncUuidInternal(db, remote.teamASyncUuid);
            int teamBId = getTeamIdBySyncUuidInternal(db, remote.teamBSyncUuid);

            ScheduleSyncRecord existing = null;
            try (Cursor c = db.query(TABLE_SCHEDULE_DETAILS, null,
                    COLUMN_SYNC_UUID + " = ?", new String[]{remote.syncUuid},
                    null, null, null)) {
                if (c.moveToFirst()) {
                    existing = cursorToScheduleSyncRecord(db, c);
                }
            }
            if (existing == null && teamAId > 0 && teamBId > 0 && remote.matchDate != null) {
                try (Cursor c = db.query(TABLE_SCHEDULE_DETAILS, null,
                        COLUMN_SCHEDULE_MATCH_DATE + " = ? AND "
                                + COLUMN_SCHEDULE_TEAMA_ID + " = ? AND "
                                + COLUMN_SCHEDULE_TEAMB_ID + " = ? AND "
                                + COLUMN_SCHEDULE_MATCH_TYPE + " = ?",
                        new String[]{
                                remote.matchDate,
                                String.valueOf(teamAId),
                                String.valueOf(teamBId),
                                String.valueOf(remote.matchOvers)
                        },
                        null, null, null)) {
                    if (c.moveToFirst()) {
                        existing = cursorToScheduleSyncRecord(db, c);
                    }
                }
            }

            ContentValues cv = new ContentValues();
            cv.put(COLUMN_SCHEDULE_USER_ID, remote.userEmailId == null ? "aditya.juet@gmail.com" : remote.userEmailId);
            cv.put(COLUMN_SCHEDULE_MATCH_DATE, remote.matchDate);
            cv.put(COLUMN_SCHEDULE_TEAMA_ID, teamAId);
            cv.put(COLUMN_SCHEDULE_TEAMB_ID, teamBId);
            cv.put(COLUMN_SCHEDULE_MATCH_TYPE, remote.matchOvers);
            cv.put(COLUMN_SCHEDULE_MATCH_STATUS, remote.matchStatus == null ? "SCHEDULED" : remote.matchStatus);
            cv.put(COLUMN_SYNC_UUID, remote.syncUuid);
            cv.put(COLUMN_CLUB_ID, remote.clubId == null ? getActiveClubId() : remote.clubId);
            cv.put(COLUMN_UPDATED_AT, remote.updatedAt);
            cv.put(COLUMN_IS_DELETED, remote.isDeleted ? 1 : 0);
            cv.put(COLUMN_REMOTE_VERSION, remote.remoteVersion);
            cv.put(COLUMN_SYNC_STATUS, markSynced ? SyncRecordModels.STATUS_SYNCED : SyncRecordModels.STATUS_DIRTY);

            if (existing != null) {
                String oldUuid = existing.syncUuid;
                db.update(TABLE_SCHEDULE_DETAILS, cv,
                        COLUMN_SCHEDULE_MATCH_ID + " = ?",
                        new String[]{String.valueOf(existing.localScheduleId)});
                if (oldUuid != null && !oldUuid.equals(remote.syncUuid)) {
                    db.delete(TABLE_SYNC_OUTBOX,
                            COLUMN_OUTBOX_ENTITY_TYPE + " = ? AND " + COLUMN_OUTBOX_ENTITY_UUID + " = ?",
                            new String[]{SyncRecordModels.ENTITY_SCHEDULE, oldUuid});
                }
            } else {
                db.insert(TABLE_SCHEDULE_DETAILS, null, cv);
            }

            if (!markSynced) {
                enqueueOutboxInternal(db, SyncRecordModels.ENTITY_SCHEDULE, remote.syncUuid,
                        remote.isDeleted ? SyncRecordModels.OP_DELETE : SyncRecordModels.OP_UPSERT,
                        remote.toJson().toString());
            } else {
                db.delete(TABLE_SYNC_OUTBOX,
                        COLUMN_OUTBOX_ENTITY_TYPE + " = ? AND " + COLUMN_OUTBOX_ENTITY_UUID + " = ?",
                        new String[]{SyncRecordModels.ENTITY_SCHEDULE, remote.syncUuid});
            }
        } catch (Exception e) {
            Log.w("DatabaseHandler", "upsertScheduleFromRemote failed", e);
        } finally {
            db.close();
        }
    }

    /**
     * Soft-deletes a team using a tombstone ({@code IS_DELETED = 1}) and enqueues a DELETE operation.
     */
    public void softDeleteTeam(int teamId) {
        TeamSyncRecord rec = null;
        for (TeamSyncRecord item : getAllTeamsForSync()) {
            if (item.localTeamId == teamId) {
                rec = item;
                break;
            }
        }
        if (rec == null) {
            return;
        }
        rec.isDeleted = true;
        rec.updatedAt = System.currentTimeMillis();
        upsertTeamFromRemote(rec, false);
    }

    /**
     * Soft-deletes a player using a tombstone ({@code IS_DELETED = 1}) and enqueues a DELETE operation.
     */
    public void softDeletePlayer(int playerId) {
        PlayerSyncRecord rec = null;
        for (PlayerSyncRecord item : getAllPlayersForSync()) {
            if (item.localPlayerId == playerId) {
                rec = item;
                break;
            }
        }
        if (rec == null) {
            return;
        }
        rec.isDeleted = true;
        rec.updatedAt = System.currentTimeMillis();
        upsertPlayerFromRemote(rec, false);
    }

    /**
     * Enqueues all active local records into {@code SYNC_OUTBOX} under {@code clubId}
     * for first-time synchronization.
     */
    public void enqueueAllLocalRecordsForClub(String clubId) {
        String targetClub = SyncPreferences.sanitizeClubId(clubId);
        List<TeamSyncRecord> teams = getAllTeamsForSync();
        List<PlayerSyncRecord> players = getAllPlayersForSync();
        List<ScheduleSyncRecord> schedules = getAllSchedulesForSync();

        SQLiteDatabase db = this.getWritableDatabase();
        try {
            for (TeamSyncRecord t : teams) {
                t.clubId = targetClub;
                if (t.updatedAt <= 0L) {
                    t.updatedAt = System.currentTimeMillis();
                }
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_CLUB_ID, targetClub);
                cv.put(COLUMN_UPDATED_AT, t.updatedAt);
                db.update(TABLE_TEAM_DETAILS, cv, COLUMN_TEAM_ID + " = ?",
                        new String[]{String.valueOf(t.localTeamId)});
                enqueueOutboxInternal(db, SyncRecordModels.ENTITY_TEAM, t.syncUuid,
                        t.isDeleted ? SyncRecordModels.OP_DELETE : SyncRecordModels.OP_UPSERT,
                        t.toJson().toString());
            }
            for (PlayerSyncRecord p : players) {
                p.clubId = targetClub;
                if (p.updatedAt <= 0L) {
                    p.updatedAt = System.currentTimeMillis();
                }
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_CLUB_ID, targetClub);
                cv.put(COLUMN_UPDATED_AT, p.updatedAt);
                db.update(TABLE_PLAYER_DETAILS, cv, COLUMN_PLAYER_ID + " = ?",
                        new String[]{String.valueOf(p.localPlayerId)});
                enqueueOutboxInternal(db, SyncRecordModels.ENTITY_PLAYER, p.syncUuid,
                        p.isDeleted ? SyncRecordModels.OP_DELETE : SyncRecordModels.OP_UPSERT,
                        p.toJson().toString());
            }
            for (ScheduleSyncRecord s : schedules) {
                s.clubId = targetClub;
                if (s.updatedAt <= 0L) {
                    s.updatedAt = System.currentTimeMillis();
                }
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_CLUB_ID, targetClub);
                cv.put(COLUMN_UPDATED_AT, s.updatedAt);
                db.update(TABLE_SCHEDULE_DETAILS, cv, COLUMN_SCHEDULE_MATCH_ID + " = ?",
                        new String[]{String.valueOf(s.localScheduleId)});
                enqueueOutboxInternal(db, SyncRecordModels.ENTITY_SCHEDULE, s.syncUuid,
                        s.isDeleted ? SyncRecordModels.OP_DELETE : SyncRecordModels.OP_UPSERT,
                        s.toJson().toString());
            }
        } catch (Exception e) {
            Log.w("DatabaseHandler", "enqueueAllLocalRecordsForClub failed", e);
        } finally {
            db.close();
        }
    }

    /**
     * Clears local club tables and pending outbox when switching to a different club account
     * so records from the previous club are never leaked into the new club.
     */
    public void clearLocalDataForClubSwitch() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.delete(TABLE_SYNC_OUTBOX, null, null);
            db.delete(TABLE_MATCH_DELIVERY_EVENTS, null, null);
            db.delete(TABLE_MATCH_SESSIONS, null, null);
            db.delete(TABLE_RULES_PROFILES, null, null);
            db.delete(TABLE_SCHEDULE_DETAILS, null, null);
            db.delete(TABLE_PLAYER_DETAILS, null, null);
            db.delete(TABLE_TEAM_DETAILS, null, null);
            db.delete(TABLE_GROUND_DETAILS, null, null);
        } finally {
            db.close();
        }
    }

    // =========================================================================
    // v4 Configurable Rules Profiles & Event-Sourced Match Session Persistence
    // =========================================================================

    /**
     * Saves or updates a versioned {@link CricketRulesProfile} in {@code RULES_PROFILES}.
     */
    public void saveRulesProfile(CricketRulesProfile profile) {
        if (profile == null) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COLUMN_PROFILE_ID, profile.profileId);
            cv.put(COLUMN_PROFILE_NAME, profile.profileName);
            cv.put(COLUMN_PROFILE_VERSION, profile.version);
            cv.put(COLUMN_PROFILE_PAYLOAD, profile.serialize());
            cv.put(COLUMN_CLUB_ID, getActiveClubId());
            cv.put(COLUMN_UPDATED_AT, System.currentTimeMillis());
            cv.put(COLUMN_IS_DELETED, 0);
            db.insertWithOnConflict(TABLE_RULES_PROFILES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        } finally {
            db.close();
        }
    }

    /**
     * Returns all active {@link CricketRulesProfile} templates stored locally, seeding the
     * 7 official templates automatically if the table is empty.
     */
    public List<CricketRulesProfile> getAllRulesProfiles() {
        List<CricketRulesProfile> list = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            try (Cursor c = db.query(TABLE_RULES_PROFILES,
                    new String[]{COLUMN_PROFILE_PAYLOAD},
                    COLUMN_IS_DELETED + " = 0", null, null, null, COLUMN_PROFILE_NAME + " ASC")) {
                while (c.moveToNext()) {
                    list.add(CricketRulesProfile.deserialize(c.getString(0)));
                }
            }
            if (list.isEmpty()) {
                for (CricketRulesProfile official : CricketRulesProfile.getOfficialTemplates()) {
                    ContentValues cv = new ContentValues();
                    cv.put(COLUMN_PROFILE_ID, official.profileId);
                    cv.put(COLUMN_PROFILE_NAME, official.profileName);
                    cv.put(COLUMN_PROFILE_VERSION, official.version);
                    cv.put(COLUMN_PROFILE_PAYLOAD, official.serialize());
                    cv.put(COLUMN_CLUB_ID, getActiveClubId());
                    cv.put(COLUMN_UPDATED_AT, System.currentTimeMillis());
                    cv.put(COLUMN_IS_DELETED, 0);
                    db.insertWithOnConflict(TABLE_RULES_PROFILES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                    list.add(official);
                }
            }
        } finally {
            db.close();
        }
        return list;
    }

    /**
     * Creates or updates a persistent {@code MATCH_SESSIONS} row with an immutable snapshot
     * of the match's {@link CricketRulesProfile}.
     */
    public void saveMatchSessionHeader(MatchState state, int scheduleId) {
        if (state == null || state.matchUuid == null || state.matchUuid.isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            saveMatchSessionHeaderInternal(db, state, scheduleId);
        } finally {
            db.close();
        }
    }

    private void saveMatchSessionHeaderInternal(SQLiteDatabase db, MatchState state, int scheduleId) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_SESSION_MATCH_UUID, state.matchUuid);
        if (scheduleId > 0) {
            cv.put(COLUMN_SESSION_SCHEDULE_ID, scheduleId);
        }
        cv.put(COLUMN_SESSION_TEAM_A_ID, state.teamAId);
        cv.put(COLUMN_SESSION_TEAM_A_NAME, state.teamAName);
        cv.put(COLUMN_SESSION_TEAM_B_ID, state.teamBId);
        cv.put(COLUMN_SESSION_TEAM_B_NAME, state.teamBName);
        cv.put(COLUMN_SESSION_RULES_SNAPSHOT, state.rulesProfile.serialize());
        cv.put(COLUMN_SESSION_LIFECYCLE_STATUS, state.lifecycleStatus.name());
        cv.put(COLUMN_SESSION_CURRENT_INNINGS, state.currentInningsNumber);
        cv.put(COLUMN_SESSION_RESULT_TEXT, state.resultText);
        cv.put(COLUMN_CLUB_ID, getActiveClubId());
        cv.put(COLUMN_UPDATED_AT, System.currentTimeMillis());
        cv.put(COLUMN_IS_DELETED, 0);
        cv.put(COLUMN_SYNC_STATUS, SyncRecordModels.STATUS_DIRTY);
        db.insertWithOnConflict(TABLE_MATCH_SESSIONS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Validates, persists, and applies a new {@link DeliveryEvent} to the match session,
     * returning the deterministically updated {@link MatchState}.
     */
    public MatchState recordMatchDelivery(MatchState currentState, DeliveryEvent newEvent) {
        MatchState updated = CricketScoringEngine.appendDeliveryOrThrow(currentState, newEvent);
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COLUMN_EVENT_UUID, newEvent.eventUuid);
            cv.put(COLUMN_EVENT_MATCH_UUID, updated.matchUuid);
            cv.put(COLUMN_EVENT_INNINGS, newEvent.inningsNumber);
            cv.put(COLUMN_EVENT_SEQ, updated.appliedEvents.size());
            cv.put(COLUMN_EVENT_PAYLOAD, newEvent.serialize());
            cv.put(COLUMN_UPDATED_AT, newEvent.timestampMillis);
            cv.put(COLUMN_EVENT_IS_UNDONE, 0);
            db.insertWithOnConflict(TABLE_MATCH_DELIVERY_EVENTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            saveMatchSessionHeaderInternal(db, updated, 0);
        } finally {
            db.close();
        }
        return updated;
    }

    /**
     * Undoes the last non-undone delivery event for {@code matchUuid} (marking it {@code IS_UNDONE = 1}
     * for auditability) and deterministically replays the remaining event history.
     */
    public MatchState undoLastMatchDelivery(String matchUuid) {
        if (matchUuid == null || matchUuid.isEmpty()) {
            return null;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            String lastEventUuid = null;
            try (Cursor c = db.query(TABLE_MATCH_DELIVERY_EVENTS,
                    new String[]{COLUMN_EVENT_UUID},
                    COLUMN_EVENT_MATCH_UUID + " = ? AND " + COLUMN_EVENT_IS_UNDONE + " = 0",
                    new String[]{matchUuid},
                    null, null, COLUMN_EVENT_SEQ + " DESC", "1")) {
                if (c.moveToFirst()) {
                    lastEventUuid = c.getString(0);
                }
            }
            if (lastEventUuid != null) {
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_EVENT_IS_UNDONE, 1);
                cv.put(COLUMN_UPDATED_AT, System.currentTimeMillis());
                db.update(TABLE_MATCH_DELIVERY_EVENTS, cv,
                        COLUMN_EVENT_UUID + " = ?", new String[]{lastEventUuid});
            }
        } finally {
            db.close();
        }
        return loadMatchState(matchUuid);
    }

    /**
     * Loads and deterministically replays a match session from {@code MATCH_SESSIONS} and
     * {@code MATCH_DELIVERY_EVENTS}.
     */
    public MatchState loadMatchState(String matchUuid) {
        if (matchUuid == null || matchUuid.isEmpty()) {
            return null;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        CricketRulesProfile rules = CricketRulesProfile.createStandardT20();
        int teamAId = 1;
        String teamAName = "Team A";
        int teamBId = 2;
        String teamBName = "Team B";
        MatchLifecycleStatus status = MatchLifecycleStatus.PROVISIONAL;
        int savedInnings = 1;
        boolean foundHeader = false;

        try (Cursor c = db.query(TABLE_MATCH_SESSIONS, null,
                COLUMN_SESSION_MATCH_UUID + " = ? AND " + COLUMN_IS_DELETED + " = 0",
                new String[]{matchUuid}, null, null, null)) {
            if (c.moveToFirst()) {
                foundHeader = true;
                teamAId = c.getInt(c.getColumnIndexOrThrow(COLUMN_SESSION_TEAM_A_ID));
                teamAName = c.getString(c.getColumnIndexOrThrow(COLUMN_SESSION_TEAM_A_NAME));
                teamBId = c.getInt(c.getColumnIndexOrThrow(COLUMN_SESSION_TEAM_B_ID));
                teamBName = c.getString(c.getColumnIndexOrThrow(COLUMN_SESSION_TEAM_B_NAME));
                rules = CricketRulesProfile.deserialize(
                        c.getString(c.getColumnIndexOrThrow(COLUMN_SESSION_RULES_SNAPSHOT)));
                savedInnings = c.getInt(c.getColumnIndexOrThrow(COLUMN_SESSION_CURRENT_INNINGS));
                try {
                    status = MatchLifecycleStatus.valueOf(
                            c.getString(c.getColumnIndexOrThrow(COLUMN_SESSION_LIFECYCLE_STATUS)));
                } catch (Exception ignored) {
                }
            }
        }
        if (!foundHeader) {
            return null;
        }

        List<DeliveryEvent> events = new ArrayList<>();
        try (Cursor ec = db.query(TABLE_MATCH_DELIVERY_EVENTS,
                new String[]{COLUMN_EVENT_PAYLOAD},
                COLUMN_EVENT_MATCH_UUID + " = ? AND " + COLUMN_EVENT_IS_UNDONE + " = 0",
                new String[]{matchUuid},
                null, null, COLUMN_EVENT_SEQ + " ASC")) {
            while (ec.moveToNext()) {
                events.add(DeliveryEvent.deserialize(ec.getString(0)));
            }
        }

        MatchState replayed = CricketScoringEngine.replay(
                matchUuid, rules, teamAId, teamAName, teamBId, teamBName, events, status);
        if (savedInnings > replayed.currentInningsNumber) {
            replayed.currentInningsNumber = savedInnings;
            replayed.getOrCreateInnings(savedInnings);
        }
        return replayed;
    }

    /**
     * Loads the most recently updated match session, or {@code null} if none exists.
     */
    public MatchState getLatestActiveMatchState() {
        String latestUuid = null;
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.query(TABLE_MATCH_SESSIONS,
                new String[]{COLUMN_SESSION_MATCH_UUID},
                COLUMN_IS_DELETED + " = 0", null,
                null, null, COLUMN_UPDATED_AT + " DESC", "1")) {
            if (c.moveToFirst()) {
                latestUuid = c.getString(0);
            }
        }
        return latestUuid != null ? loadMatchState(latestUuid) : null;
    }

    /**
     * Confirms or finalizes a match session and commits player batting/bowling career statistics
     * and team win/loss/draw totals into {@code PLAYER_DETAILS} and {@code TEAM_DETAILS}.
     */
    public MatchState confirmAndCommitMatchStats(String matchUuid, boolean finalizeLock) {
        MatchState state = loadMatchState(matchUuid);
        if (state == null) {
            return null;
        }
        boolean wasProvisional = (state.lifecycleStatus == MatchLifecycleStatus.PROVISIONAL);
        state.lifecycleStatus = finalizeLock ? MatchLifecycleStatus.FINALIZED : MatchLifecycleStatus.CONFIRMED;
        saveMatchSessionHeader(state, 0);

        if (wasProvisional) {
            // Commit batting & bowling statistics for any matching local players
            List<PlayerSyncRecord> localPlayers = getAllPlayersForSync();
            for (InningsScorecard inn : state.inningsList) {
                for (BatterInningsStat b : inn.batterStats.values()) {
                    for (PlayerSyncRecord p : localPlayers) {
                        if (p.localPlayerId == b.playerId
                                || SyncRecordModels.normalizeName(p.playerName)
                                .equals(SyncRecordModels.normalizeName(b.playerName))) {
                            p.totalRuns += b.runs;
                            p.ballsFaced += b.ballsFaced;
                            p.matchPlayed += 1;
                            if (b.runs >= 200) {
                                p.twoHundred += 1;
                            } else if (b.runs >= 100) {
                                p.hundred += 1;
                            } else if (b.runs >= 50) {
                                p.fifty += 1;
                            }
                            p.updatedAt = System.currentTimeMillis();
                            upsertPlayerFromRemote(p, false);
                            break;
                        }
                    }
                }
                for (BowlerSpellStat bw : inn.bowlerStats.values()) {
                    for (PlayerSyncRecord p : localPlayers) {
                        if (p.localPlayerId == bw.playerId
                                || SyncRecordModels.normalizeName(p.playerName)
                                .equals(SyncRecordModels.normalizeName(bw.playerName))) {
                            p.oversBowled += bw.getCompletedOvers();
                            p.wicketsTaken += bw.wickets;
                            p.maidenBowled += bw.maidens;
                            p.widesCount += bw.wides;
                            p.noBallCount += bw.noBalls;
                            p.runsGiven += bw.runsConceded;
                            p.updatedAt = System.currentTimeMillis();
                            upsertPlayerFromRemote(p, false);
                            break;
                        }
                    }
                }
            }
            // Commit team win/loss/draw if teams exist in TEAM_DETAILS
            if (state.matchCompleted) {
                for (TeamSyncRecord t : getAllTeamsForSync()) {
                    boolean isTeamA = (t.localTeamId == state.teamAId)
                            || SyncRecordModels.normalizeName(t.teamName)
                            .equals(SyncRecordModels.normalizeName(state.teamAName));
                    boolean isTeamB = (t.localTeamId == state.teamBId)
                            || SyncRecordModels.normalizeName(t.teamName)
                            .equals(SyncRecordModels.normalizeName(state.teamBName));
                    if (isTeamA || isTeamB) {
                        if (state.isTied) {
                            t.matchDraw += 1;
                        } else if ((isTeamA && state.winnerTeamId == state.teamAId)
                                || (isTeamB && state.winnerTeamId == state.teamBId)) {
                            t.matchWon += 1;
                        } else {
                            t.matchLost += 1;
                        }
                        t.updatedAt = System.currentTimeMillis();
                        upsertTeamFromRemote(t, false);
                    }
                }
            }
        }
        return state;
    }
}






