package edu.wpi.alcowatch.alcowatch;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

import org.w3c.dom.Attr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils.DataSource;

/**
 * Created by michaelHahn on 1/16/15.
 * Listener service or data events on the data layer
 */
public class MobileListenerService extends WearableListenerService{

    private static final String MOBILE_DATA_PATH = "/mobile_data";

    Activity thisActivity;

    File outputFile;
    ArrayList<Attribute> attributes;
    ArrayList<String> labelItems;

    Attribute mClassAttribute;
    Instances mDataset;


    String mClass;

    DataMap[] gestureSeries; // 6 long for about 2s of data
    int gestureNum = 0;

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        int result = super.onStartCommand(i, flags, startId);

        if (i.getExtras()!= null) {

            Log.v("MQP", "PHONE SERVICE STARTED");

            attributes = new ArrayList<>();
            attributes.add(new Attribute("avg_speed"));
            attributes.add(new Attribute("median_speed"));
            attributes.add(new Attribute("max_speed"));
            attributes.add(new Attribute("variance_speed"));
            attributes.add(new Attribute("net_roll_velocity"));
            attributes.add(new Attribute("median_roll_velocity"));
            attributes.add(new Attribute("max_roll_velocity"));

            labelItems = new ArrayList<>();
            labelItems.add("drinking");
            labelItems.add("crossing_arms");
            labelItems.add("sitting_smoking");
            labelItems.add("on_phone");

            mClassAttribute = new Attribute("label", labelItems);
            attributes.add(mClassAttribute);


            mDataset = new Instances("mqp_features", attributes, 1000);
            mDataset.setClassIndex(mDataset.numAttributes() - 1);

            //attributes.add(new Attribute("vertical_displacement"));


            mClass = i.getExtras().getString("class");

            gestureSeries = new DataMap[6];

            outputFile = new File(getApplicationContext().getFilesDir(), "mqpdata.arff");
            if (outputFile.exists()) {
                try {
                    DataSource dataSource = new DataSource(new FileInputStream(outputFile));
                    Instances oldData = dataSource.getDataSet();
                    mDataset.setClassIndex(mDataset.numAttributes() - 1);

                    int a = 0;
                    for (Instance inst : oldData) {
                        Log.v("MQP", "Loaded inst " + a + ": " + inst.toString());
                        a++;

                        mDataset.add(inst);
                    }

                    outputFile.delete();

                    Log.v("MQP", "File loaded");
                } catch (Exception e) {
                    Log.v("MQP", "Exception", e);
                }

            } else {
                Log.v("MQP", "File not Found, making new");

                try {
                    outputFile.createNewFile();
                } catch (Exception e) {

                }
            }
        }

        return result;
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        DataMap dataMap;
        for (DataEvent event : dataEvents) {
            //Log.v("MQP", "DataMap received on phone: " + DataMapItem.fromDataItem(event.getDataItem()).getDataMap());
            // Check the data type
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // Check the data path
                String path = event.getDataItem().getUri().getPath();
                if (path.equals(MOBILE_DATA_PATH)) {
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();


                    // Broadcast DataMap contents to wearable activity for display
                    // The content has the golf hole number and distances to the front,
                    // middle and back pin placements.

                    Intent messageIntent = new Intent();
                    messageIntent.setAction(Intent.ACTION_SEND);
                    messageIntent.putExtra("datamap", dataMap.toBundle());
                    LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);

                    logData(dataMap);

                }

            }
        }
    }

    public void logData(DataMap dataMap) {

        if (gestureNum < 0) {
            // skip this set, as the last one was just gesture end. Unlikely they take puffs in such quick succession.
            gestureNum++;
            Log.v("MQP", "Skipping set");
        }
        else if (gestureNum == 0) {
            // check whether this segment starts a gesture

            float[] x = dataMap.getFloatArray("x");
            float[] y = dataMap.getFloatArray("y");
            float[] z = dataMap.getFloatArray("z");

            String sp = "";

            for (int i = 0; i< x.length; i++) {

                float speed = (float) Math.sqrt(Math.pow(x[i],2)+Math.pow(y[i],2)+Math.pow(z[i],2));
                sp = sp+""+speed+",";

                if (speed > 1.f) {
                    gestureSeries[0] = dataMap;
                    gestureNum = 1;

                    Log.v("MQP", "Gesture begin with speed" + speed);

                }

            }
            Log.v("MQP", sp);

        }
        else if (gestureNum >= 1 && gestureNum < 6) {
            // add the intermediate datamap to the list

            gestureSeries[gestureNum] = dataMap;
            gestureNum++;
        }
        else {
            // the gesture is complete. Calculate features.

            float[] max_speeds = new float[6];
            float[] median_speeds = new float[6];
            float[] mean_speeds = new float[6];
            float[] variance_speeds = new float[6];
            float[] max_roll_velocitys = new float[6];
            float[] median_roll_velocitys = new float[6];
            float[] net_roll_velocitys = new float[6];

            Log.v("MQP", "Whole gesture: ");
            String gesture = "";

            for (int i=0; i<6; i++) {
                DataMap data = gestureSeries[i];

                float[] x = data.getFloatArray("x");
                float[] y = data.getFloatArray("y");
                float[] z = data.getFloatArray("z");

                float[] speeds = new float[x.length];
                float totalSpeed = 0;

                for (int v = 0; v < x.length; v++) {
                    speeds[v] = (float) Math.sqrt(Math.pow(x[v],2) + Math.pow(y[v],2) + Math.pow(z[v],2));
                    totalSpeed+=speeds[v];
                    gesture+=speeds[v]+", ";
                }



                Arrays.sort(speeds);

                max_speeds[i] = speeds[speeds.length-1];
                median_speeds[i] = speeds[speeds.length/2];
                mean_speeds[i] = totalSpeed / speeds.length;
                variance_speeds[i] = 0;

                for (float a : speeds) {
                    variance_speeds[i] += (mean_speeds[i]-a) * (mean_speeds[i]-a);
                }
                variance_speeds[i] = variance_speeds[i] / speeds.length;

                float[] rx = data.getFloatArray("rx");
                float[] ry = data.getFloatArray("ry");
                float[] rz = data.getFloatArray("rz");
                float[] rw = data.getFloatArray("rw");


                float[] gx = data.getFloatArray("gx");
                float[] gy = data.getFloatArray("gy");
                float[] gz = data.getFloatArray("gz");

                float[] roll_velocities = new float[gx.length];
                float net_roll_velocity = 0;


                for (int v = 0; v < gx.length; v++) {
                    float a =  gx[v];
                    float b = gy[v];
                    float c = gz[v];


                    roll_velocities[v] = (float) Math.sqrt(Math.pow(a,2) + Math.pow(b,2) + Math.pow(c,2));
                    net_roll_velocity+= roll_velocities[v];
                }

                Arrays.sort(roll_velocities);

                max_roll_velocitys[i] = roll_velocities[roll_velocities.length - 1];
                median_roll_velocitys[i] = roll_velocities[roll_velocities.length/2];
                net_roll_velocitys[i] = net_roll_velocity;


            }

            // calculate net stats

            float mean_speed = 0;
            float median_speed = 0;
            float max_speed = 0;
            float variance_speed = 0;
            float net_roll_velocity = 0;
            float median_roll_velocity = 0;
            float max_roll_velocity = 0;

            for (int i = 0; i < 6; i++) {
                mean_speed+=mean_speeds[i];
                median_speed+=median_speeds[i];

                if (max_speed < max_speeds[i])
                    max_speed = max_speeds[i];

                net_roll_velocity+=net_roll_velocitys[i];
                median_roll_velocity+=median_roll_velocitys[i];

                if (max_roll_velocity < max_roll_velocitys[i])
                    max_roll_velocity = max_roll_velocitys[i];
            }

            mean_speed = mean_speed / 6;
            median_speed = median_speed / 6;
            median_roll_velocity = median_roll_velocity / 6;

            for (int i = 0; i< 6; i++) {
                variance_speed += (variance_speeds[i] + Math.pow(mean_speeds[i] - mean_speed, 2));
            }
            variance_speed = variance_speed / 6;


            Instance inst = new DenseInstance(attributes.size());
            inst.setDataset(mDataset);

            inst.setValue(mDataset.attribute("avg_speed"), mean_speed);
            inst.setValue(mDataset.attribute("median_speed"), median_speed);
            inst.setValue(mDataset.attribute("max_speed"), max_speed);
            inst.setValue(mDataset.attribute("variance_speed"), variance_speed);

            inst.setValue(mDataset.attribute("net_roll_velocity"), net_roll_velocity);
            inst.setValue(mDataset.attribute("median_roll_velocity"), median_roll_velocity);
            inst.setValue(mDataset.attribute("max_roll_velocity"), max_roll_velocity);
            inst.setValue(mClassAttribute, mClass);

            mDataset.add(inst);

            gestureNum = -2; // end gesture.

            Log.v("MQP", gesture);
            Log.v("MQP", "Gesture end");


        }

    }


    @Override
    public void onDestroy() {

        if (mDataset != null) {
            ArffSaver saver = new ArffSaver();
            saver.setInstances(mDataset);


            Log.v("MQP", "FILE " + outputFile.getAbsolutePath());
            try {
                saver.setFile(outputFile);
                saver.writeBatch();


                File newFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "mqpdata" + System.currentTimeMillis()+".arff");
                copyFile(outputFile, newFile);


                Intent intent =
                        new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(newFile));
                sendBroadcast(intent);
            } catch (IOException e) {
                Log.e("MQP", "error saving");
                e.printStackTrace();
            }

        } else {
            Log.v("MQP", "Dataset NULL");
        }
    }

    public static void copyFile(File src, File dst) throws IOException
    {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try
        {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
        finally
        {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }
    }

}
