package edu.wpi.alcowatch.alcowatch;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

import edu.wpi.alcowatch.alcowatch.Classification.ClassificationHelper;
import edu.wpi.alcowatch.alcowatch.utils.DatabaseContract;
import edu.wpi.alcowatch.alcowatch.utils.Utils;
import weka.core.Attribute;
import weka.core.Instances;

/**
 * The Main activity that displays on the phone. This activity has a title, and a status textview. It is able to start services that listen for messages from the wearable.
 * It also communicates with a pre-programmed matlab server that calculates the features of each time a subject starts the applicaton.
 * @author Steven Ireland, Andrew McAfee, Benjamin Bianchi
 * @version 2
 */


public class MobileDataMapActivity extends AppCompatActivity
{
    /**
     * The Shared Prefence object that holds the biommetric data for the subject, as well as his preferred callback.
     */
    static SharedPreferences bacSharedPreferences;
    /**
     * Used for permissions required by the android OS
     */
    int hasPermission = 0;

    /**
     * The service Intent that will spawn the MobileListenerService service, which implements a WearableListenerService, allowing communication from the wearable to the phone, without the mobile application
     * being in the foreground.
     */
    Intent serviceIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * Load biometric data from subject.
         */
        bacSharedPreferences = getApplicationContext().getSharedPreferences(Utils.BAC_NOTIFICATION_SETTINGS, getApplicationContext().MODE_PRIVATE);


        /**
         * Connect the application to the database, and clean out all entries so that we don't send old data.
         */
        DatabaseContract.DatabaseHelper d = new DatabaseContract.DatabaseHelper(getApplicationContext());
        SQLiteDatabase db = d.getWritableDatabase();
        db.delete("sensor_readings","",new String[0]);

        /**
         * Register the local broadcast receiver -- allows the phone to send messages TO the wearable. The MessageReciever is defined at the bottom of this document.
         */

        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);


        /**
         * Spawn the WearableListenerService that will listen for data from the wearable.
         */
        serviceIntent = new Intent(this, MobileListenerService.class);
        startService(serviceIntent);

    }


    /**
     * The Message path used by the android OS and android manifest that forwards data towards the wearable's registered listeners.
     */
    private static final String WEARABLE_DATA_PATH = "/wearable_data";

    /**
     * This function is invoked after the phone communicates with matlab server to extract features. If there is missing biometric data, then we will spawn the
     * Profile screen so that the user is forced to fill out his/her biometric data.
     */
    protected void onFullReadingComplete(){
        Intent i = new Intent(getApplicationContext(), ProfileActivity.class);

        /**
         * Load Biometric Data
         */
        String gender = bacSharedPreferences.getString(Utils.GENDER, "");
        double weight = bacSharedPreferences.getInt(Utils.WEIGHT, 0);
        double height = (1.0 *(bacSharedPreferences.getInt(Utils.HEIGHT,0)) / Utils.CONVERT_INCHES_TO_CM);
        String age =  (bacSharedPreferences.getString(Utils.AGE, ""));


        /**
         * Classify features if biometrics are correctly filled out. Must use a thread in order for the Main thread to not hang (Android error is thrown).
         */
        if (weight != 0 && gender != "" && height != 0 && age != "") {

        Runnable r = new Runnable() {
            @Override
            public void run() {

                ClassificationHelper ch = new ClassificationHelper(getApplicationContext());

                TextView tv = (TextView) findViewById(R.id.output);
                tv.setText("Sending Request to Server");
                ch.execute();
            }
        };
            r.run();

        }
        else
        {
            startActivity(i);
        }

    }


    @Override
    protected void onStart() {
        super.onStart();
    }


    public void onDestroy(){
        stopService(serviceIntent);
        super.onDestroy();
    }

    // Disconnect from the data layer when the Activity stops
    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     * Class that sends a message to the wearable. Messages contains data from the current time, accelerometer and gryroscope. x, y, z correspond to accelerometer; gx, gy, gz correspond
     * to gyroscope data.
     */
    public class MessageReceiver extends BroadcastReceiver {

        /**
         * Tell the message reciever which database we are inserting into, and how to store decimals within it.
         */
        DatabaseContract.DatabaseHelper d = new DatabaseContract.DatabaseHelper(getApplicationContext());
        SQLiteDatabase db = d.getWritableDatabase();
        public final DecimalFormat df = new DecimalFormat("#.##");


        @Override
        public void onReceive(Context context, Intent intent) {

            Bundle data = intent.getBundleExtra("datamap");

            if (data.getString("type").equals("status")) {} // Normal Ping

            else if (data.getString("type").equals("end")){ // This is sent at the end of the transmission from the wearable.
                    onFullReadingComplete();
            }
            else if (data.getString("type").equals("data")) { // THis is sent when the wearable is transferring actual data to the phone.

                // Log the data
                long[] dt = data.getLongArray("dt");
                float[] x = data.getFloatArray("x");
                float[] y = data.getFloatArray("y");
                float[] z = data.getFloatArray("z");
                float[] gx = data.getFloatArray("gx");
                float[] gy = data.getFloatArray("gy");
                float[] gz = data.getFloatArray("gz");

                /**
                 * Notify the activity that we are recieving data from the wearable.
                 */
                String output = "Receiving Data";
                TextView tv = (TextView) findViewById(R.id.output);
                tv.setText(output);

                for (int i = 0; i < x.length; i++) {

                    d.InsertIntoAcc(db,dt[i],x[i],y[i],z[i]);
                    d.InsertIntoGryo(db,dt[i],gx[i],gy[i],gz[i]);

                }
            }
        }
    }

    /**
     * The Thread that sends a message from the phone to the wearable. This dataMap contains the sobriety ruling, and the callback that the user
     * selected.
     */
    public static class SendToDataLayerThread extends Thread {
        String path;

        // Constructor for sending data objects to the data layer
        public SendToDataLayerThread(String p) {
            path = p;
        }

        public void run() {

            /**
             * Create Datamap and fill it with a classificationResult, a callback, and Time. Change the type flag to classUpdate and send.
             */
            DataMap dataMap = new DataMap();
            // Get classification result here  ..........................
            dataMap.putString("classificationResult",bacSharedPreferences.getString(Utils.LAST_BAC_ESTIMATION,""));
            dataMap.putString("callback",bacSharedPreferences.getString(Utils.CALLBACK,""));
            Calendar rightNow = Calendar.getInstance();
            dataMap.putLong("time",rightNow.getTimeInMillis());
            dataMap.putString("type","classUpdate");

            // Construct a DataRequest and send over the data layer
            PutDataMapRequest putDMR = PutDataMapRequest.create(path);
            putDMR.setUrgent();
            putDMR.getDataMap().putAll(dataMap);
            PutDataRequest request = putDMR.asPutDataRequest();
            DataApi.DataItemResult result = Wearable.DataApi.putDataItem(MobileListenerService.googleClient, request).await();
            if (result.getStatus().isSuccess()) {
                Log.v("MQP", "DataMap: " + dataMap.getString("classificationResult") + " sent successfully to watch");
            } else {
                Log.v("MQP", "Failed to send classificationResult to watch");
            }
        }
    }
}