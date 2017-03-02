package edu.wpi.alcowatch.alcowatch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import edu.wpi.alcowatch.alcowatch.User.Profile;
import edu.wpi.alcowatch.alcowatch.utils.Utils;

import static edu.wpi.alcowatch.alcowatch.utils.Utils.PICK_CONTACT_REQUEST;

/**
 * This activity is shown when the user's biometric data is needed. It stores biometric data within a Shared Prefences Object.
 * @author Benjamin Bianchi
 * @version 1
 */
public class ProfileActivity extends AppCompatActivity {
    SharedPreferences bacPreferences;
    String gender="female";
    String cNumber;
    String callback = "";
    SharedPreferences bacSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /**
         * Open XML layout
         */
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        /**
         * Load Preferences
         */
        bacSharedPreferences = getApplicationContext().getSharedPreferences(Utils.BAC_NOTIFICATION_SETTINGS, getApplicationContext().MODE_PRIVATE);

        //usersName = bacSharedPreferences.getString(Utils.NAME, ""); Something to add on later, maybe.
        String gender = bacSharedPreferences.getString(Utils.GENDER, "");
        callback = bacSharedPreferences.getString(Utils.CALLBACK, "");
        double weight = bacSharedPreferences.getInt(Utils.WEIGHT, 0);
        Integer height = Math.round((bacSharedPreferences.getInt(Utils.HEIGHT,0)) / 2.54f);
        String age =  (bacSharedPreferences.getString(Utils.AGE, ""));


        /**
         * Load all controls on the screen
         */
        EditText profileAgeInput = (EditText) findViewById(R.id.profileAgeInput);
        EditText profileHeightInput = (EditText) findViewById(R.id.profileHeightInput);
        EditText profileWeightInput = (EditText) findViewById(R.id.profileWeightInput);

        RadioGroup radioGroupGender = (RadioGroup) findViewById(R.id.birthSexGroup);
        RadioGroup radioGroupCallback = (RadioGroup) findViewById(R.id.emergencyCallbackGroup);

        profileAgeInput.setText(String.valueOf(age));
        profileWeightInput.setText(String.valueOf(weight));
        profileHeightInput.setText(String.valueOf(height));

        /**
         * Pre-fill the form with saved values
         */
        if (gender=="female")
            ((RadioButton)radioGroupGender.findViewById(R.id.profileGenderFemale) ).setChecked(true);
        else
            ((RadioButton)radioGroupGender.findViewById(R.id.profileGenderMale) ).setChecked(true);

        if (callback.startsWith("contact-"))
            ((RadioButton)radioGroupCallback.findViewById(R.id.profileEmergencyCallFriend) ).setChecked(true);
        else if (callback.equals("uber"))
        ((RadioButton)radioGroupCallback.findViewById(R.id.profileEmergencyCallUber) ).setChecked(true);
        else if (callback.equals("killSwitch"))
            ((RadioButton)radioGroupCallback.findViewById(R.id.profileEmergencyKillSwitch) ).setChecked(true);


//        if ( ((RadioButton)radioGroupCallback.findViewById(R.id.profileEmergencyCallFriend)).isChecked())


        Button profileSetButton = (Button) findViewById(R.id.submitbutton);

        profileSetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitProfile(view);
            }
        });

    }

    //code

    /**
     * Taken from the android website. Get the number of a contact.
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_CONTACT_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // Get the URI that points to the selected contact
                Uri contactUri = data.getData();
                // We only need the NUMBER column, because there will be only one row in the result
                String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};

                // Perform the query on the contact to get the NUMBER column
                // We don't need a selection or sort order (there's only one result for the given URI)
                // CAUTION: The query() method should be called from a separate thread to avoid blocking
                // your app's UI thread. (For simplicity of the sample, this code doesn't do that.)
                // Consider using CursorLoader to perform the query.
                Cursor cursor = getContentResolver()
                        .query(contactUri, projection, null, null, null);
                cursor.moveToFirst();

                // Retrieve the phone number from the NUMBER column
                int column = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                cNumber = cursor.getString(column);
                callback= "contact-"+cNumber;


            }
            }
    }


    /**
     * When clicked, open the contacts menu so that a subject can select a person to call when they are drunk.
     * @param view
     */
    public void openEmergencyContactSelectionMenu(View view)
    {
        Intent pickContactIntent = new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"));
        pickContactIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
    }

    public void setCallBackToUber(View view)
    {
        //Just a setter
        callback = "uber";

    }

    public void setCallBackToKillSwitch(View view) {
        callback="kill";    }
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

        if (gender == "") {
            Log.v("?W",weightInput.getText().toString());
            Log.v("?H",heightInput.getText().toString());
            Log.v("?A",ageInput.getText().toString());
            Log.v("?G",gender);

            return;
        }

        Profile p = new Profile(gender, Integer.parseInt(ageInput.getText().toString()), Float.parseFloat(weightInput.getText().toString()), Float.parseFloat(heightInput.getText().toString()));

        /**
         * Store all profile data within shared preferences.
         */
        SharedPreferences bacPreferences = getSharedPreferences(Utils.BAC_NOTIFICATION_SETTINGS, MODE_PRIVATE);
        SharedPreferences.Editor editor = bacPreferences.edit();
        editor.putString(Utils.CALLBACK, callback);
        editor.putString(Utils.AGE, Integer.toString(p.getAge()));
        editor.putInt(Utils.WEIGHT, (int) p.getWeight());
        double height = p.getHeight() * Utils.CONVERT_INCHES_TO_CM;

        editor.putInt(Utils.HEIGHT, (int) Math.round(height));
        editor.putString(Utils.GENDER,gender);
        editor.commit();

        /**
         * Allow the user to start the listeners.
         */
        Intent i = new Intent(getApplicationContext(),MobileDataMapActivity.class);
        startActivity(i);
    }


}