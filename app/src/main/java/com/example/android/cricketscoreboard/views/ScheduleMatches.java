package com.example.android.cricketscoreboard.views;

import android.database.sqlite.SQLiteDatabase;
import android.icu.util.Calendar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;

import com.example.android.cricketscoreboard.R;

import java.util.Date;

public class ScheduleMatches extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_matches);

        //SQLiteDatabase db = new SQLiteDatabase(savedInstanceState);

        //db.
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
