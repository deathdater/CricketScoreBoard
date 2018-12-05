package com.example.android.cricketscoreboard.views;

import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.android.cricketscoreboard.DatabaseHandler;
import com.example.android.cricketscoreboard.R;

public class ViewSchedules extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_schedules);
        init();


    }

    public void init(){
        DatabaseHandler db=new DatabaseHandler(this);
        String [][] list =db.viewAllSchedules();
        TableLayout ll = (TableLayout) findViewById(R.id.scheduleMatchTableView);
        TableRow firstRow= new TableRow(this);
        TableRow.LayoutParams lp1 = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
        firstRow.setLayoutParams(lp1);
        TextView matchDateTextView= new TextView(this);
        matchDateTextView.setText("Match Date");
        matchDateTextView.setTypeface(matchDateTextView.getTypeface(), Typeface.BOLD);

        matchDateTextView.setPadding(15,0,0,0);
        TextView teamATextView= new TextView(this);
        teamATextView.setText("Team A");
        teamATextView.setTypeface(teamATextView.getTypeface(), Typeface.BOLD);
        teamATextView.setPadding(15,0,0,0);

        TextView teamBTextView= new TextView(this);
        teamBTextView.setText("Team B");
        teamBTextView.setTypeface(teamBTextView.getTypeface(), Typeface.BOLD);
        teamBTextView.setPadding(15,0,0,0);

        TextView OversTextView= new TextView(this);
        OversTextView.setText("Overs");
        OversTextView.setTypeface(OversTextView.getTypeface(), Typeface.BOLD);
        OversTextView.setPadding(15,0,0,0);

        TextView selectScheduleTextView= new TextView(this);
        selectScheduleTextView.setText("Select \nSchedule");
        selectScheduleTextView.setTypeface(selectScheduleTextView.getTypeface(), Typeface.BOLD);
        selectScheduleTextView.setPadding(15,0,0,0);
        firstRow.addView(matchDateTextView);
        firstRow.addView(teamATextView);
        firstRow.addView(teamBTextView);
        firstRow.addView(OversTextView);
        firstRow.addView(selectScheduleTextView);
        ll.addView(firstRow,0);
try {
    RadioGroup selectScheduleRadioGroupRow=new RadioGroup(this);
    RadioButton selectScheduleRadioButtonRow[]=new RadioButton[db.getNumberOfSchedules()];
    EditText matchDateTextViewRow[]=new EditText[db.getNumberOfSchedules()];
    EditText teamATextViewRow[]=new EditText[db.getNumberOfSchedules()];
    EditText teamBTextViewRow[]=new EditText[db.getNumberOfSchedules()];
    EditText OversTextViewRow[]=new EditText[db.getNumberOfSchedules()];
    for (int i = 0; i < db.getNumberOfSchedules(); i++) {

        TableRow row = new TableRow(this);
        TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
        row.setLayoutParams(lp);
        matchDateTextViewRow[i] = new EditText(this);
        matchDateTextViewRow[i].setPadding(15, 0, 0, 0);
        matchDateTextViewRow[i].setText(list[i][0]);
        matchDateTextViewRow[i].setFocusable(false);

        teamATextViewRow[i] = new EditText(this);
        teamATextViewRow[i].setPadding(15, 0, 0, 0);
        teamATextViewRow[i].setText(db.getTeamName(Integer.parseInt(list[i][1])));
        teamATextViewRow[i].setFocusable(false);

        teamBTextViewRow[i] = new EditText(this);
        teamBTextViewRow[i].setPadding(15, 0, 0, 0);
        teamBTextViewRow[i].setText(db.getTeamName(Integer.parseInt(list[i][2])));
        teamBTextViewRow[i].setFocusable(false);

        OversTextViewRow[i] = new EditText(this);
        OversTextViewRow[i].setPadding(15, 0, 0, 0);
        OversTextViewRow[i].setText(list[i][3]);
        OversTextViewRow[i].setFocusable(false);
        selectScheduleRadioButtonRow[i] = new RadioButton(this);
        selectScheduleRadioButtonRow[i].setPadding(15, 0, 0, 0);

        row.addView(matchDateTextViewRow[i]);
        row.addView(teamATextViewRow[i]);
        row.addView(teamBTextViewRow[i]);
        row.addView(OversTextViewRow[i]);
        row.addView(selectScheduleRadioButtonRow[i]);
        ll.addView(row, i+1);

        //selectScheduleRadioGroupRow.addView(selectScheduleCheckBoxRow[i]);
    }

}catch (Exception e){
    Log.v("ViewSchedules:","Exeception Thrown at init function"+e);
}
    }

}
