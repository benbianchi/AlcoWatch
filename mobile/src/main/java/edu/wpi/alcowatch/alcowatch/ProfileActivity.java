package edu.wpi.alcowatch.alcowatch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.RadioGroup;
import android.widget.Toast;

import edu.wpi.alcowatch.alcowatch.User.Profile;
import edu.wpi.alcowatch.alcowatch.utils.Utils;

public class ProfileActivity extends AppCompatActivity {
    SharedPreferences bacPreferences;
    String gender = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);


    }

    public void openEmergencyContactSelectionMenu(View view)
    {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent,1 );
    }

    public void setMale(View view)
    {
        gender = "male";
    }

    public void setFemale(View view)
    {
        gender = "female";
    }

    public void submitProfile(View view)
    {
        //Validate that all form inputs have acceptable inputs;

        EditText heightInput = (EditText) findViewById(R.id.profileHeightInput);
        EditText weightInput = (EditText) findViewById(R.id.profileWeightInput);
        EditText ageInput = (EditText) findViewById(R.id.profileAgeInput);

        if (heightInput.getText().toString() == "" || weightInput.getText().toString() == "" || ageInput.getText().toString() == "" || gender == "") {
            Log.v("?W",weightInput.getText().toString());
            Log.v("?H",heightInput.getText().toString());
            Log.v("?A",ageInput.getText().toString());
            Log.v("?G",gender.toString());
            return;
        }

        Profile p = new Profile(gender, Integer.parseInt(ageInput.getText().toString()), Integer.parseInt(weightInput.getText().toString()), Integer.parseInt(heightInput.getText().toString()));
        SharedPreferences bacPreferences = getSharedPreferences(Utils.BAC_NOTIFICATION_SETTINGS, MODE_PRIVATE);
        SharedPreferences.Editor editor = bacPreferences.edit();
        editor.putString(Utils.AGE, Integer.toString(p.getAge()));
        editor.putInt(Utils.WEIGHT, p.getWeight());
        editor.putInt(Utils.HEIGHT, p.getHeight());
        editor.putString(Utils.GENDER, p.getGender());
        editor.commit();
        Intent i = new Intent(getApplicationContext(),MobileDataMapActivity.class);
        startActivity(i);
    }


}