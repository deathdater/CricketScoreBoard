package com.example.android.cricketscoreboard;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Deathdater on 10/26/17.
 */

public class ScoreMan {

    public int battingScore, oversBowled, overBalls, runsScored,index=0;
  public   boolean wide, noball, freeHit,byes, legbyes, out, undoBall;
  public   String bowlerName, thisOver, strikerName, nonStrikerName,scoreCommentry=":";

    public ArrayList score=new ArrayList();
    public ArrayList overs=new ArrayList();

    public void goBowler(String bowlerName) {

        //Code to freeze bowler NAME for next 6 valid balls
    }

    public void recordScore(int battingScore){

        score.add(index,battingScore);
        index=index+1;

    }
    public void recordOver(int oversBowled,int overBalls ){
        String over;
        over=oversBowled+"."+overBalls;
        overs.add(index,over);

    }
    public void undoBall(){
        if(index!=0){
        index=index-1;
        score.remove(index);
            if(score.isEmpty()){
                battingScore=0;
            }else{
                battingScore=(Integer)score.get((index-1));
            }

        }else{
            Log.i("Array Empty", "onBallPress: " );
            battingScore=0;

        }


    }
 /*   public int getScore(){

        battingScore=(int)score.get(index);
        return battingScore;

    }*/

    public void recordBall() {
        if (undoBall == true) {
            if (overBalls != 0 && oversBowled != 0) {
                //Display message no ball to reverse
            } else {
                //Code to reverse last ball actions
            }
        } else {
            //UndoBall not checked
            //We will now proceed with this ball

                 if (wide==false && noball==false && overBalls!=6) {

                     overBalls = overBalls + 1;

                     if (byes == true || legbyes == true) {
                         if (byes == true) {
                             //display byes and runs scored
                             System.out.println(runsScored);
                         } else {
                             //display leg byes and runs scored
                             System.out.println(runsScored);
                         }
                     }
                 }

                else if(wide==true){

                     //**IMPORTANT we cannot have leg byes on a wide ball
                     if(byes==true){
                             //display WIDE byes and runs scored
                             System.out.println(runsScored);
                         }else{
                         //display message wide ball
                     }
                     }

                 else if(noball=true){
                     if(freeHit=true) {

                         if(byes==true||legbyes==true){
                             if(byes==true){
                                 //display byes and runs scored
                                 //display message no ball and Free hit
                                 System.out.println(runsScored);
                             }else{
                                 //display leg byes and runs scored
                                 //display message no ball and Free hit
                                 System.out.println(runsScored);
                             }
                         }
                     }
                     else{

                         if(byes==true||legbyes==true){
                             if(byes==true){
                                 //display byes and runs scored
                                 //display message No Ball
                                 System.out.println(runsScored);
                             }else{
                                 //display leg byes and runs scored
                                 //display message No Ball
                                 System.out.println(runsScored);
                             }
                         }

                     }
                 }

                 else if(out==true){
                     //allow selection of striker or non striker
                     //display out with the player name
                     //display player name with player score
                     //load the new batsmen using activity.
                 }

                     }
            } //display message "Over Complete !! Select new bowler & press Go"




        }


