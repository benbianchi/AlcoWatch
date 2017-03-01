package edu.wpi.alcowatch.alcowatch;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Chronometer;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
//import com.google.android.gms.wearable.DataApi;
//import com.google.android.gms.wearable.DataMap;
//import com.google.android.gms.wearable.PutDataMapRequest;
//import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;


import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import android.net.Uri;

import org.w3c.dom.Attr;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.converters.ArffSaver;


public class MobileDataMapActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    GoogleApiClient googleClient;

    private Chronometer chronometer;

    private EditText labelField;

    ArrayList<Attribute> attributes;
    Instances mDataset;

    int hasPermission = 0;

    boolean serviceStarted = false;

    String mClass = "no_goggles";

    Intent serviceIntent;

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


        // Register the local broadcast receiver
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);

        // Build a new GoogleApiClient for the the Wearable API
        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();



        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    hasPermission);
        }

        serviceIntent = new Intent(this, MobileListenerService.class);
        startService(serviceIntent);

                if (serviceStarted) {
                    serviceStarted = false;
                    chronometer.stop();
                    saveToFile();
                } else {

                    serviceIntent.putExtra("class", mClass);

                    chronometer.setBase(SystemClock.elapsedRealtime());
                    chronometer.start();
                    mDataset.delete();
                    mDataset = new Instances("mqp_features", attributes, 10000);
                    mDataset.setClassIndex(mDataset.numAttributes() - 1);
                    serviceStarted = true;
                }
    }

    protected void saveToFile(){
        File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), labelField.getText() + "-" + System.currentTimeMillis() + ".arff");
        if (mDataset != null) {
            ArffSaver saver = new ArffSaver();
            saver.setInstances(mDataset);


            Log.v("MQP", "FILE " + outputFile.getAbsolutePath());
            try {
                saver.setFile(outputFile);
                saver.writeBatch();

                Intent intent =
                        new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(outputFile));
                sendBroadcast(intent);
            } catch (IOException e) {
                Log.e("MQP", "error saving");
                e.printStackTrace();
            }

        } else {
            Log.v("MQP", "Dataset NULL");
        }
    }

    // Connect to the data layer when the Activity starts
    @Override
    protected void onStart() {
        super.onStart();
        googleClient.connect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {


    }

    public void onDestroy(){
        stopService(serviceIntent);
        super.onDestroy();
    }

    // Disconnect from the data layer when the Activity stops
    @Override
    protected void onStop() {
        if (null != googleClient && googleClient.isConnected()) {
            googleClient.disconnect();
        }
        super.onStop();
    }

    // Placeholders for required connection callbacks
    @Override
    public void onConnectionSuspended(int cause) { }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) { }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

//    public void sendToWearable(String content) {
//        String WEARABLE_DATA_PATH = "/wearable_data";
//
//        DataMap dataMap = new DataMap();
//        dataMap.putString("content", content);
//        dataMap.putString("type", "status");
//        dataMap.putLong("timestamp", System.currentTimeMillis());
//        new SendToDataLayerThread(WEARABLE_DATA_PATH, dataMap).start();
//    }
//
//    class SendToDataLayerThread extends Thread {
//        String path;
//        DataMap dataMap;
//
//        // Constructor for sending data objects to the data layer
//        SendToDataLayerThread(String p, DataMap data) {
//            path = p;
//            dataMap = data;
//        }
//
//        public void run() {
//            // Construct a DataRequest and send over the data layer
//            PutDataMapRequest putDMR = PutDataMapRequest.create(path);
//            putDMR.getDataMap().putAll(dataMap);
//            PutDataRequest request = putDMR.asPutDataRequest();
//            DataApi.DataItemResult result = Wearable.DataApi.putDataItem(googleClient, request).await();
//            if (result.getStatus().isSuccess()) {
////                Log.v("MQP", "DataMap: " + dataMap + " sent successfully to data layer ");
//            } else {
//                // Log an error
//                Log.v("MQP", "ERROR: failed to send DataMap to data layer");
//            }
//        }
//    }

    public class MessageReceiver extends BroadcastReceiver {

        public final DecimalFormat df = new DecimalFormat("#.##");

        @Override
        public void onReceive(Context context, Intent intent) {
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

                if (serviceStarted == true) {
                    if (attributes == null) {
                        Log.v("MQP", "attributes null");
                        return;
                    }

                    for (int i = 0; i < x.length; i++) {
                        Instance inst = new DenseInstance(attributes.size());
                        inst.setDataset(mDataset);
                        inst.setValue(mDataset.attribute("dt"), dt[i]);
                        inst.setValue(mDataset.attribute("x"), x[i]);
                        inst.setValue(mDataset.attribute("y"), y[i]);
                        inst.setValue(mDataset.attribute("z"), z[i]);
                        inst.setValue(mDataset.attribute("gx"), gx[i]);
                        inst.setValue(mDataset.attribute("gy"), gy[i]);
                        inst.setValue(mDataset.attribute("gz"), gz[i]);
                        inst.setValue(mDataset.attribute("goggles_class"), mClass);
                        mDataset.add(inst);
                    }
                }
            }
        }
    }
}
