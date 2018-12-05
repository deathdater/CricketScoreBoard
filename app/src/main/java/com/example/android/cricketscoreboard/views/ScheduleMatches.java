package com.example.android.cricketscoreboard.views;

import android.database.sqlite.SQLiteDatabase;
import android.icu.util.Calendar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.android.cricketscoreboard.DatabaseHandler;
import com.example.android.cricketscoreboard.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ScheduleMatches extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_matches);

        Spinner teamListA=(Spinner)findViewById(R.id.teamA_spinner) ;
        Spinner teamListB=(Spinner)findViewById(R.id.teamB_spinner) ;


        try{
            DatabaseHandler db=new DatabaseHandler(this);
            List valTeam=new ArrayList();
            if(db.getAllTeamNames().size()>0) {

                for (int i = 0; i < db.getAllTeamNames().size(); i++) {

                    valTeam.add(db.getAllTeamNames().get(i));
                    Log.i("Value of teams list item " + i, db.getAllTeamNames().get(i));
                }
            }

            if(valTeam.size()>0) {
                final StableArrayAdapter teamListAdapter = new StableArrayAdapter(this,
                        android.R.layout.preference_category, valTeam);
                teamListA.setAdapter(teamListAdapter);
                teamListB.setAdapter(teamListAdapter);
            }else{
                Toast.makeText(ScheduleMatches.this, "No Teams available", Toast.LENGTH_SHORT).show();

            }

        }catch (Exception e){


        }
        //SQLiteDatabase db = new SQLiteDatabase(savedInstanceState);

        //db.
    }
    public void onSaveScheduleClick(View view){
        Spinner teamListA=(Spinner)findViewById(R.id.teamA_spinner) ;
        Spinner teamListB=(Spinner)findViewById(R.id.teamB_spinner) ;
        DatePicker matchDate=(DatePicker)findViewById(R.id.matchDatePicker);
        EditText matchOvers=(EditText)findViewById(R.id.matchOverEditText);
        String matchDateString=""+matchDate.getYear()+"/"+matchDate.getMonth()+"/"+matchDate.getDayOfMonth();


        if(teamListA.getSelectedItem().toString().equals(teamListB.getSelectedItem().toString())){
    Toast.makeText(ScheduleMatches.this, "Teams Cannot be same", Toast.LENGTH_SHORT).show();

}else{
    Toast.makeText(ScheduleMatches.this, "Now the data is safe", Toast.LENGTH_SHORT).show();

    try {
                DatabaseHandler db = new DatabaseHandler(this);
                db.addSchedule("aditya.juet@gmail.com",matchDateString,db.getTeamId(teamListA.getSelectedItem().toString()),db.getTeamId(teamListB.getSelectedItem().toString()),Integer.parseInt(matchOvers.getText().toString()));
                Log.v("onSaveScheduleClick", "Schedule Added" );
                // customsavedIntanceState.putString("TeamName",""+teamAddstr);
                finish();
                startActivity(getIntent());


            } catch (Exception e) {
                Log.v("onSaveScheduleClick", "Threw Exception " + e);

            }

}

    }

    public void onCalendar(View view){

        DatePicker calendar=(DatePicker) findViewById(R.id.matchDatePicker);



      /* Date date=new Date();
        Long time=date.getTime();
        int dd=date.getDay();
        int mm =date.getMonth();
        int yy=date.getYear();
        System.out.println("Date is"+dd+mm+"20"+yy);
        System.out.println(time.intValue());
        //calendar.setMinDate(date.);*/
    }
}
