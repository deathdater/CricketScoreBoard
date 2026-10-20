package com.example.android.cricketscoreboard.views;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import com.google.android.material.snackbar.Snackbar;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.example.android.cricketscoreboard.DatabaseHandler;
import com.example.android.cricketscoreboard.ImageUtility;
import com.example.android.cricketscoreboard.R;
import com.example.android.cricketscoreboard.entity.Teams;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class TeamDetails extends AppCompatActivity {

    public NumberPicker playerCount;
    public int numberOfPlayers;
    public View newTeamview;
    public String teamName;
    public int check = 0;
    private int STORAGE_PERMISSION_CODE = 23;
    ImageView imageView;
    SQLiteDatabase db;
    public static final int PICK_IMAGE=100;
    private int PICK_IMAGE_REQUEST = 100;
    public Uri imageUri;
    private static final int SELECT_PICTURE = 100;
    private static final String TAG = "MainActivity";
    DatabaseHandler dbHelper;

    // Photo Picker launcher (replaces startActivityForResult + storage permissions).
    private final ActivityResultLauncher<PickVisualMediaRequest> pickLogo =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    imageUri = uri;
                    imageView.setImageURI(imageUri);
                } else {
                    Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                }
            });

    //public int teamId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_details);
        playerCount = (NumberPicker) findViewById(R.id.playerCount_number_picker);
        playerCount.setMinValue(1);
        playerCount.setMaxValue(16);
        numberOfPlayers = playerCount.getValue();
        newTeamview = findViewById(R.id.teamName_edit_text);
        imageView=(ImageView)findViewById(R.id.team_logo);
        teamName = newTeamview.toString();
        dbHelper = new DatabaseHandler(this);

    }

    public void onSaveClick(View view) {

        String message;
        EditText newTeamview;

        newTeamview = (EditText) findViewById(R.id.teamName_edit_text);
        teamName = newTeamview.getText().toString();
        if (teamName.isEmpty()) {
          //  playerCount = (NumberPicker) findViewById(R.id.playerCount_number_picker);
         //   numberOfPlayers = playerCount.getValue();
            message = "Let's not have a team without a name !!";
            Snackbar.make(findViewById(R.id.activity_team_details), message,
                    Snackbar.LENGTH_SHORT)
                    .show();
        } else {

            // teamId=1;
            teamName = newTeamview.getText().toString();
        //    playerCount = (NumberPicker) findViewById(R.id.playerCount_number_picker);
         //   numberOfPlayers = playerCount.getValue();

            DatabaseHandler db = new DatabaseHandler(getApplicationContext());
            ArrayList<String> getTeams = db.getAllTeamNames();
            ListIterator itr = getTeams.listIterator();
            if (getTeams.isEmpty()) {
                Log.v("ABC", "Returned list is empty");

                db.insertTeamBasicDetails(teamName);//,teamLogo);    //, numberOfPlayers
              //  saveImageInDB(teamName,imageUri);

                message = "Added Team Name: " + teamName ;    //+ " \n Minimum number players: " + numberOfPlayers;
                Snackbar.make(findViewById(R.id.activity_team_details), message,
                        Snackbar.LENGTH_SHORT)
                        .show();
                db.close();

            } else {
                while (itr.hasNext()) {
                    if (itr.next().equals(teamName)) {
                        Log.v("ABC", "Team name exists in the list");
                        String errorMessage;
                        errorMessage = "Team Name:" + teamName + ". ALREADY EXISTS!!. \nTry a different name";
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                        db.close();
                        check = 1;
                        break;
                    } else {
                        Log.v("ABC", "Team name does not exists in the list");
                        check = 0;
                    }


                }


                if (check != 1) {

                   db.insertTeamBasicDetails(teamName);//.teamLogo);    //, numberOfPlayers
                   // saveImageInDB(teamName,imageUri);

                    message = "Added  Team Name: " + teamName; //+ " \n Minimum number players: " + numberOfPlayers;
                    Snackbar.make(findViewById(R.id.activity_team_details), message,
                            Snackbar.LENGTH_SHORT)
                            .show();
                    db.close();

                }
            }
        }

    }
    public void loadTeamdetails(View view){


    }


    public void openImageChooser(){
        pickLogo.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }
    /*
  @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {

                imageUri = data.getData();

                if (null != imageUri) {

                    // Saving to Database...
                    if (saveImageInDB(teamName,imageUri)) {
                        showMessage("Image Saved in Database...");
                        imageView.setImageURI(imageUri);
                    }

                    // Reading from Database after 3 seconds just to show the message

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (loadImageFromDB()) {
                                showMessage("Image Loaded from Database...");
                            }
                        }
                    }, 3000);
                }

            }
        }
    }
*/
    public  void fetchImage(View view) throws IOException {
        openImageChooser();



      /*  File folder= new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/demoImage.jpg/");
        FileInputStream fis = new FileInputStream(folder);
        byte[] image= new byte[fis.available()];
        fis.read(image);
        ContentValues values = new ContentValues();
        values.put("image",image);
        db.insert("imageTb", null,values);
        fis.close();
        */
        Toast.makeText(this,"Image Fetched", Toast.LENGTH_SHORT).show();
    }

    Boolean saveImageInDB(String teamName,Uri selectedImageUri) {

        try {

            InputStream iStream = getContentResolver().openInputStream(selectedImageUri);
            byte[] inputData = ImageUtility.getBytes(iStream);
           // dbHelper.insertTeamBasicDetails(teamName,inputData);
           // dbHelper.close();
            return true;
        } catch (IOException ioe) {
            Log.e(TAG, "<saveImageInDB> Error : " + ioe.getLocalizedMessage());
          //  dbHelper.close();
            return false;
        }

    }

    Boolean loadImageFromDB() {
        try {

            byte[] bytes = dbHelper.viewImage(teamName);
            dbHelper.close();
            // Show Image from DB in ImageView
            imageView.setImageBitmap(ImageUtility.getImage(bytes));
            return true;
        } catch (Exception e) {
            Log.e(TAG, "<loadImageFromDB> Error : " + e.getLocalizedMessage());
            dbHelper.close();
            return false;
        }
    }


    void showMessage(String message) {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.activity_team_details), message, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

}

