package edu.wpi.alcowatch.alcowatch;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import edu.wpi.alcowatch.alcowatch.Classification.ClassificationHelper;
import edu.wpi.alcowatch.alcowatch.utils.DatabaseContract;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.pmml.Array;

//import com.google.android.gms.wearable.DataApi;
//import com.google.android.gms.wearable.DataMap;
//import com.google.android.gms.wearable.PutDataMapRequest;
//import com.google.android.gms.wearable.PutDataRequest;


public class MobileDataMapActivity extends AppCompatActivity
{

    private Chronometer chronometer;

    private EditText labelField;

    ArrayList<Attribute> attributes;
    Instances mDataset;

    int hasPermission = 0;

    String mClass = "no_goggles";

    Intent serviceIntent;

    public static Boolean classifying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        attributes = new ArrayList<>();
        attributes.add(new Attribute("dt"));
        attributes.add(new Attribute("x"));
        attributes.add(new Attribute("y"));
        attributes.add(new Attribute("z"));
        attributes.add(new Attribute("gx"));
        attributes.add(new Attribute("gy"));
        attributes.add(new Attribute("gz"));
        ArrayList class_nominal_values = new ArrayList<String>(5);
        class_nominal_values.add("no_goggles");
        class_nominal_values.add("green_goggles");
        class_nominal_values.add("black_goggles");
        class_nominal_values.add("red_goggles");
        class_nominal_values.add("orange_goggles");
        attributes.add(new Attribute("goggles_class", class_nominal_values));

        mDataset = new Instances("mqp_features", attributes, 10000);
        mDataset.setClassIndex(mDataset.numAttributes() - 1);

        chronometer = (Chronometer) findViewById(R.id.chronometer);
        labelField = (EditText) findViewById(R.id.editText);

        DatabaseContract.DatabaseHelper d = new DatabaseContract.DatabaseHelper(getApplicationContext());
        SQLiteDatabase db = d.getWritableDatabase();
        db.delete("sensor_readings","",new String[0]);

        // Register the local broadcast receiver
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);


        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    hasPermission);
        }

        serviceIntent = new Intent(this, MobileListenerService.class);
        startService(serviceIntent);

        final RadioButton radioNoGoggles = (RadioButton) findViewById(R.id.radioButton);
        final RadioButton radioGreenGoggles = (RadioButton) findViewById(R.id.radioButton2);
        final RadioButton radioBlackGoggles = (RadioButton) findViewById(R.id.radioButton3);
        final RadioButton radioRedGoggles = (RadioButton) findViewById(R.id.radioButton4);
        final RadioButton radioOrangeGoggles = (RadioButton) findViewById(R.id.radioButton5);
        radioNoGoggles.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                mClass = "no_goggles";
            }
        });
        radioGreenGoggles.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                mClass = "green_goggles";
            }
        });
        radioBlackGoggles.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                mClass = "black_goggles";
            }
        });
        radioRedGoggles.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                mClass = "red_goggles";
            }
        });
        radioOrangeGoggles.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){ mClass = "orange_goggles";    }
        });

        final Button button = (Button) findViewById(R.id.sendButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

    }

    protected void onFullReadingComplete(){

        ClassificationHelper ch = new ClassificationHelper(this.getApplicationContext());
        ch.execute();

        classifying = false;

    }

    // Connect to the data layer when the Activity starts
    @Override
    protected void onStart() {
        super.onStart();
    }

    private static final String MOBILE_DATA_PATH = "/mobile_data";

    public void onDestroy(){
        stopService(serviceIntent);
        super.onDestroy();
    }

    // Disconnect from the data layer when the Activity stops
    @Override
    protected void onStop() {
        super.onStop();
    }

    public class MessageReceiver extends BroadcastReceiver {

        DatabaseContract.DatabaseHelper d = new DatabaseContract.DatabaseHelper(getApplicationContext());
        SQLiteDatabase db = d.getWritableDatabase();
        public final DecimalFormat df = new DecimalFormat("#.##");

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v("AW","onReceive");
            if(classifying)
                return;
            classifying = true;
            Bundle data = intent.getBundleExtra("datamap");
            if (data.getString("type").equals("status")) {
                //
            } else if (data.getString("type").equals("data")) {

                // Log the data
                long[] dt = data.getLongArray("dt");
                float[] x = data.getFloatArray("x");
                float[] y = data.getFloatArray("y");
                float[] z = data.getFloatArray("z");
                float[] gx = data.getFloatArray("gx");
                float[] gy = data.getFloatArray("gy");
                float[] gz = data.getFloatArray("gz");

                String output = "Receiving Data";
                TextView tv = (TextView) findViewById(R.id.output);
                tv.setText(output);

                for (int i = 0; i < x.length; i++) {

                    d.InsertIntoAcc(db,dt[i],x[i],y[i],z[i]);
                    d.InsertIntoGryo(db,dt[i],gx[i],gy[i],gz[i]);

                }

                onFullReadingComplete();
            }
        }
    }
}