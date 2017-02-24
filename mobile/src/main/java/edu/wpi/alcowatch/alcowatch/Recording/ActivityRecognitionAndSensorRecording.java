package edu.wpi.alcowatch.alcowatch.activitydedtectionandsensors;

/**
 * Created by Jacob Watson on 2/16/2017.
 */

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.wpi.alcowatch.alcowatch.utils.Utils;
import edu.wpi.alcowatch.alcowatch.utils.DatabaseContract;


public class ActivityRecognitionAndSensorRecording extends BroadcastReceiver{

    Context appContext;
    String broadcastTag = "ARASR: Broadcast";
    String activityRecognitionTag = "Activity Recognition";
    String detectMotionTag = "detectMotion";
    String mostProbableActivityString = "Most Probable Activity";
    String secondaryActivityString = "Secondary Activity";
    GoogleApiClient mGoogleApiClient;
    BroadcastReceiver actionDetectionReceiver;
    LocalBroadcastManager bManager;
    SQLiteDatabase db;
    SharedPreferences bacSharedPreferences;
    Integer startTime;

    @Override
    public void onReceive(Context context, Intent intent) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        Log.i("onReceive", "onReceive was called at " + dateFormat.format(date));

        appContext = context;
        bacSharedPreferences = appContext.getSharedPreferences(Utils.BAC_NOTIFICATION_SETTINGS, appContext.MODE_PRIVATE);
        db = new DatabaseContract.DatabaseHelper(appContext).getWritableDatabase();

        //Service start up

    }
}