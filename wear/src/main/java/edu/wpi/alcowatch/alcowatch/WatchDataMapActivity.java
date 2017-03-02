
package edu.wpi.alcowatch.alcowatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ProgressBar;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import static android.view.View.GONE;

public class WatchDataMapActivity extends WearableActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    /**
     * The duration of the walk trial.
     */
    private static final int CLASSIFYING_TIME = 10;
    /**
     * Message displayed after CountDown.
     */
    private static final String COUNT_DOWN_COMPLETE = "Classifying";
    /**
     * Message Path used by OS to forward data to phone.
     */
    private static final String MOBILE_DATA_PATH = "/mobile_data";

    /**
     * Google Client that allows wearable to connect to phone.
     */
    public static GoogleApiClient googleClient;

    /**
     * Boolean that is set to true when we are recording a walk.
     */
    boolean isRecording = false;
    /**
     * Chronometer that is the countdown timer.
     */
    Chronometer c;
    /**
     * ProgressBar shown when record button is hit.
     */
    ProgressBar p;

    /**
     * The thread that will send info to the phone.
     */
    Thread sendingThread;
    /**
     * Used to signal to send an "end transmission" message.
     */
    private boolean sendLastPacket = false;

    /**
     * Intent for spawning a sensor listener.
     */
    Intent watchListenerIntent;
    /**
     * Callback from user profile
     */
    private String callback;
    /**
     * Message Reciever that will be used to recieve the classificationResult, and the callback.
     */
    MessageReceiver messageReceiver;
    /**
     * IntentFilter used to forward messages to the MessageReciever.
     */
    IntentFilter messageFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);


        c = (Chronometer) findViewById(R.id.chronometer);
        p = (ProgressBar) findViewById(R.id.sobiretyProgressBar);

        // Register the local broadcast receiver
        this.messageFilter= new IntentFilter(Intent.ACTION_SENDTO);
        this.messageReceiver = new MessageReceiver();


        /**
         * Establish connection to phone.
         */
        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        googleClient.connect();

        startService(new Intent(this, SensorService.class));
    }


    public void onRecordButtonClick(View view) {

        /**
         * if button is clicked and we arent recording... put activity in recording state.
         * else do nothing
         */
        if (isRecording == false) {
            isRecording = true;

            /**
             * Show that we are recording.
             */
            c.setVisibility(View.VISIBLE);
            p.setVisibility(View.VISIBLE);
            sendLastPacket = false;

            /**
             * Allow the message Reciever to listen for data
             */
            LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
            /**
             * Create Countdown for set time.
             */
            new CountDownTimer(CLASSIFYING_TIME * 1000, 1000) {
                Chronometer c = (Chronometer) findViewById(R.id.chronometer);
                ProgressBar p = (ProgressBar) findViewById(R.id.sobiretyProgressBar);


                public void onTick(long millisUntilFinished) {
                    c.setText("Seconds Remaining: " + millisUntilFinished / 1000);
                    //here you can have your logic to set text to edittext
                }

                public void onFinish() {

                    /**
                     * Reset view to not recording mode, but set the text to say we are waiting for a result.
                     */
                    c.setText(COUNT_DOWN_COMPLETE);
                    p.setVisibility(View.INVISIBLE);
                    sendLastPacket = true;

                    /**
                     * Vibrate, as requested by professor Agu.
                     */
                    Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    long[] vibrationPattern = {0, 500, 50, 300};
                    //-1 - don't repeat
                    final int indexInPatternToRepeat = -1;
                    vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
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
        watchListenerIntent = new Intent(this,WatchListenerService.class);
        startService(watchListenerIntent);
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
    public void onConnectionSuspended(int cause) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.v("MQP", connectionResult.getErrorMessage());
    }


    public void sendDataToPhone(Bundle data) {

        if (isRecording) {
            sendingThread = new SendToDataLayerThread(MOBILE_DATA_PATH, data);
            sendingThread.start();
        }
    }


    public class MessageReceiver extends BroadcastReceiver {
        /**
         * Global. a = sober; b = drunk.
         */
        private static final String SOBER = "a";

        @Override
        public void onReceive(Context context, Intent intent) {

            /**
             * Read data recieved, if contains classUpdate, figure out if we recieved a drunk message or sober message. Vibrate. Open corresponding activity.
             */
            Bundle data = intent.getBundleExtra("datamap");

            if(data.getString("type").equals("classUpdate")) {

                LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(messageReceiver);

                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                long[] vibrationPattern = {0, 500, 50, 300};
                //-1 - don't repeat
                final int indexInPatternToRepeat = -1;
                vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);

                String classResult = data.getString("classificationResult");
                Log.i("REC", "onReceive: "+classResult);
                if (classResult.equals(SOBER)) {
                    ((ProgressBar) findViewById(R.id.sobiretyProgressBar)).setVisibility(GONE);
                    ((Chronometer) findViewById(R.id.chronometer)).setText("Sober");
                }
                else {
                    callback = data.getString("callback");
                    displayDrunkScreen();
                }
            }
            if (data.getString("type").equals("status")) {
            } else if (data.getString("type").equals("data")) {
                sendDataToPhone(data);
            }


//            else if (data.getString("type").equals("sober")) {
//                displaySoberScreen();
//            }
//            else if (data.getString("type").equals("drunk")) {
//                displayDrunkScreen();
//            }

        }
    }

    private void displayDrunkScreen() {
        Intent i = new Intent(getApplicationContext(),DrunkActivity.class);
        i.putExtra("callback",this.callback);
        startActivity(i);
    }

    private void displaySoberScreen() {
        Intent i = new Intent(getApplicationContext(),SoberActivity.class);
        startActivity(i);
    }


    /**
     * Simple thread that sends sensor data to phone.
     */
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

            /**
             * If the countdown is done, signal that this is the last message and to perform classification on the phone.
             */
            if (sendLastPacket == false)
                dataMap.putString("type", "data");
            else
                dataMap.putString("type", "end");

            // Construct a DataRequest and send over the data layer
            PutDataMapRequest putDMR = PutDataMapRequest.create(path);
            putDMR.setUrgent();
            putDMR.getDataMap().putAll(dataMap);
            PutDataRequest request = putDMR.asPutDataRequest();
            DataApi.DataItemResult result = Wearable.DataApi.putDataItem(googleClient, request).await();

            if (result.getStatus().isSuccess()) {
                Log.v("MQP", "DataMap: " + dataMap.getString("type") + " sent successfully to data layer ");
                if (sendLastPacket) {
                    isRecording = false;
                    this.interrupt();
                }
                } else {
                    Log.v("MQP", "Failed to send to data layer.");
                }


            }
        }


    }


