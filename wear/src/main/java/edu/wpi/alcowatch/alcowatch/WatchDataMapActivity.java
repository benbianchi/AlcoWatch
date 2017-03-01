package edu.wpi.alcowatch.alcowatch;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.ProgressSpinner;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Date;

public class WatchDataMapActivity extends WearableActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final int CLASSIFYING_TIME = 60;
    private static final String COUNT_DOWN_COMPLETE = "Classifying";
    GoogleApiClient googleClient;

    boolean isRecording = false;
    Chronometer c;
    ProgressBar p;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        setAmbientEnabled();

        c = (Chronometer) findViewById(R.id.chronometer);
        p = (ProgressBar) findViewById(R.id.sobiretyProgressBar);

        // Register the local broadcast receiver
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SENDTO);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);

        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

//        startService(new Intent(this, WatchListenerService.class));
        startService(new Intent(this, SensorService.class));
    }


    public void onRecordButtonClick(View view)
    {
        if (isRecording == false) {
            isRecording = true;
            c.setVisibility(View.VISIBLE);
            p.setVisibility(View.VISIBLE);

            new CountDownTimer(CLASSIFYING_TIME*1000, 1000) {
                Chronometer c = (Chronometer) findViewById(R.id.chronometer);
                ProgressBar p = (ProgressBar) findViewById(R.id.sobiretyProgressBar);



                public void onTick(long millisUntilFinished) {
                    c.setText("Seconds Remaining: " + millisUntilFinished / 1000);
                    //here you can have your logic to set text to edittext
                }

                public void onFinish() {
                    c.setText(COUNT_DOWN_COMPLETE);
                    c.setVisibility(View.INVISIBLE);
                    p.setVisibility(View.INVISIBLE);
                    isRecording = false;
                }

            }.start();
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
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.v("MQP", connectionResult.getErrorMessage());
    }


    public void sendDataToPhone(Bundle data) {
        String MOBILE_DATA_PATH = "/mobile_data";

        new SendToDataLayerThread(MOBILE_DATA_PATH, data).start();
    }


    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle data = intent.getBundleExtra("datamap");

            if (data.getString("type").equals("status")) {
            } else if (data.getString("type").equals("data")) {
                sendDataToPhone(data);
            }

        }
    }


    class SendToDataLayerThread extends Thread {
        String path;
        Bundle data;

        // Constructor for sending data objects to the data layer
        SendToDataLayerThread(String p, Bundle data) {
            path = p;
            this.data = data;
        }

        public void run() {

            DataMap dataMap = new DataMap();
            dataMap.putFloatArray("x", data.getFloatArray("x"));
            dataMap.putFloatArray("y", data.getFloatArray("y"));
            dataMap.putFloatArray("z", data.getFloatArray("z"));

            dataMap.putFloatArray("gx", data.getFloatArray("gx"));
            dataMap.putFloatArray("gy", data.getFloatArray("gy"));
            dataMap.putFloatArray("gz", data.getFloatArray("gz"));

            dataMap.putLongArray("dt", data.getLongArray("dt"));

            dataMap.putString("type", "data");

            // Construct a DataRequest and send over the data layer
            PutDataMapRequest putDMR = PutDataMapRequest.create(path);
            putDMR.setUrgent();
            putDMR.getDataMap().putAll(dataMap);
            PutDataRequest request = putDMR.asPutDataRequest();
            DataApi.DataItemResult result = Wearable.DataApi.putDataItem(googleClient, request).await();
            if (result.getStatus().isSuccess()) {
                Log.v("MQP", "DataMap: " + dataMap + " sent successfully to data layer ");
            } else {
                Log.v("MQP", "Failed to send to data layer.");
            }
        }
    }

}

