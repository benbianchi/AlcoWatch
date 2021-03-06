package edu.wpi.alcowatch.alcowatch;


import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.wearable.DataMap;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class SensorService extends Service implements SensorEventListener {

    private LocalBroadcastManager localBroadcastManager;

    private static int ACCELEROMETER_BLOCK_CAPACITY = 64;
    private static int ACCELEROMETER_BUFFER_CAPACITY = 2048;

    private static int mFeatLen = ACCELEROMETER_BLOCK_CAPACITY + 2;


    private static int SERVICE_TASK_TYPE_COLLECT = 0;
    private static int SERVICE_TASK_TYPE_CLASSIFY = 1;



    private File mFeatureFile;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private Sensor mRotationVector;

    private int mServiceTaskType;
    private OnSensorChangedTask mAsyncTask;

    private static ArrayBlockingQueue<Float> mAccBufferX;
    private static ArrayBlockingQueue<Float> mAccBufferY;
    private static ArrayBlockingQueue<Float> mAccBufferZ;


    private static ArrayBlockingQueue<Float> mGyroBufferX;
    private static ArrayBlockingQueue<Float> mGyroBufferY;
    private static ArrayBlockingQueue<Float> mGyroBufferZ;


    public static final DecimalFormat df = new DecimalFormat("#.##");

    @Override
    public void onCreate() {
        super.onCreate();



        Log.v("MQP", "Sensor Service Started");

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        mAccBufferX = new ArrayBlockingQueue<Float>(ACCELEROMETER_BUFFER_CAPACITY);
        mAccBufferY = new ArrayBlockingQueue<Float>(ACCELEROMETER_BUFFER_CAPACITY);
        mAccBufferZ = new ArrayBlockingQueue<Float>(ACCELEROMETER_BUFFER_CAPACITY);


        mGyroBufferX = new ArrayBlockingQueue<Float>(ACCELEROMETER_BUFFER_CAPACITY);
        mGyroBufferY = new ArrayBlockingQueue<Float>(ACCELEROMETER_BUFFER_CAPACITY);
        mGyroBufferZ = new ArrayBlockingQueue<Float>(ACCELEROMETER_BUFFER_CAPACITY);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        List<Sensor> sensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor s : sensorList) {
            Log.v("MQP", "Sensor "+s.getName()+", "+s.getType());
        }

        mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        mGyroscope = mSensorManager
                .getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        mSensorManager.registerListener(this, mAccelerometer,
                SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGyroscope,
                SensorManager.SENSOR_DELAY_FASTEST);

        mServiceTaskType = SERVICE_TASK_TYPE_COLLECT;

        mAsyncTask = new OnSensorChangedTask();
        mAsyncTask.execute();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mAsyncTask.cancel(true);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mSensorManager.unregisterListener(this);
        super.onDestroy();

    }

    private class OnSensorChangedTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... arg0) {

            Log.v("MQP", "Start sensorChangedTask");
            int blockSize = 0;
            float[] accBlockX = new float[ACCELEROMETER_BLOCK_CAPACITY];
            float[] accBlockY = new float[ACCELEROMETER_BLOCK_CAPACITY];
            float[] accBlockZ = new float[ACCELEROMETER_BLOCK_CAPACITY];
            long[] times = new long[ACCELEROMETER_BLOCK_CAPACITY];


            float[] gyroBlockX = new float[ACCELEROMETER_BLOCK_CAPACITY];
            float[] gyroBlockY = new float[ACCELEROMETER_BLOCK_CAPACITY];
            float[] gyroBlockZ = new float[ACCELEROMETER_BLOCK_CAPACITY];

            while (true) {
                try {
//                    Log.v("MQP","Send block");
                    // need to check if the AsyncTask is cancelled or not in the while loop
                    if (isCancelled () == true)
                    {
                        return null;
                    }

                    // Dumping buffer
                    accBlockX[blockSize] = mAccBufferX.take().floatValue();
                    accBlockY[blockSize] = mAccBufferY.take().floatValue();
                    accBlockZ[blockSize] = mAccBufferZ.take().floatValue();

                    gyroBlockX[blockSize] = mGyroBufferX.take().floatValue();
                    gyroBlockY[blockSize] = mGyroBufferY.take().floatValue();
                    gyroBlockZ[blockSize] = mGyroBufferZ.take().floatValue();

                    times[blockSize++] = System.currentTimeMillis();

   /*               rotationVectorX[blockSize] = mRotationBufferX.take().floatValue();
                    rotationVectorY[blockSize] = mRotationBufferY.take().floatValue();
                    rotationVectorZ[blockSize] = mRotationBufferZ.take().floatValue();
                    rotationVectorW[blockSize++] = mRotationBufferW.take().floatValue();
*/

                    if (blockSize == ACCELEROMETER_BLOCK_CAPACITY) {
                        blockSize = 0;

                        Intent messageIntent = new Intent();
                        messageIntent.setAction(Intent.ACTION_SENDTO);
                        DataMap dataMap = new DataMap();
                        dataMap.putFloatArray("x", accBlockX);
                        dataMap.putFloatArray("y", accBlockY);
                        dataMap.putFloatArray("z", accBlockZ);

                        dataMap.putFloatArray("gx", gyroBlockX);
                        dataMap.putFloatArray("gy", gyroBlockY);
                        dataMap.putFloatArray("gz", gyroBlockZ);

                        dataMap.putLongArray("dt", times);

                        dataMap.putString("type", "data");

                        messageIntent.putExtra("datamap", dataMap.toBundle());
                        localBroadcastManager.sendBroadcast(messageIntent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onCancelled() {
            Log.v("MQP", "Cancelling SensorService");
            super.onCancelled();
        }

    }

    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {

            try {
                mAccBufferX.add(new Float(event.values[0]));
                mAccBufferY.add(new Float(event.values[1]));
                mAccBufferZ.add(new Float(event.values[2]));
            } catch (IllegalStateException e) {

            }
        }
        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {

            try {
                mGyroBufferX.add(new Float(event.values[0]));
                mGyroBufferY.add(new Float(event.values[1]));
                mGyroBufferZ.add(new Float(event.values[2]));
            } catch (IllegalStateException e) {

            }
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}