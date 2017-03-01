package edu.wpi.alcowatch.alcowatch.Classification;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import edu.wpi.alcowatch.alcowatch.MobileDataMapActivity;
import edu.wpi.alcowatch.alcowatch.ProfileActivity;
import edu.wpi.alcowatch.alcowatch.utils.DatabaseContract;
import edu.wpi.alcowatch.alcowatch.utils.Utils;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

/**
 * Created by Jacob Watson on 2/9/17.
 */

public class ClassificationHelper extends AsyncTask<Context, Void, String> {
    Context mContext;
    private final String TAG = "Server Communication";
    private String extractedFeaturesJsonResult;
    String usersName;
    String gender;
    Double genderNumber;
    Integer weight;
    Double height;
    Double age;
    String featureExtractionResults;
    SharedPreferences bacSharedPreferences;
    String bin;
    RandomForest cls;
    SQLiteDatabase readableDB;
    SQLiteDatabase writableDatabase;


    public ClassificationHelper(Context context) {
        mContext = context;
    }

    @Override
    protected String doInBackground(Context... params) {
        //Step 1: Load up user data
        setupAndLoadUserData();
        //Step 2: Run extraction
        try {
            runCallToAPIToStartFeatureExtraction();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            throw new RuntimeException(e);
        }

        return bin;
    }

    @Override
    protected void onPostExecute(String result) {
        //Result these flags so we can give the user these various alerts again.
        resetFlagsSoWeCanSendAlertsAgain();

        Log.i("FeatureExtraction", "We have reached the onPostExecute phase for the Feature extraction and Classification task.");

        //Step 3: Classify the data
        try {
            runClassification();
        } catch (Exception e) {
            Log.e("Error", " " + e.toString());
            Log.e("Error", " " + e.getMessage());
            Log.e("Error", " " + e.getLocalizedMessage());
            for (int i = 0; i < 10; i++) {
                Log.e("Error", " " + e.getStackTrace()[i].toString());
            }
        }

        //Step 4: Add BAC reading and other info to database
        addReadingToDatabaseAndPreferences();

        bacSharedPreferences = mContext.getSharedPreferences(Utils.BAC_NOTIFICATION_SETTINGS, mContext.MODE_PRIVATE);
        Log.v("Watch Results", bacSharedPreferences.getString(Utils.FEATURE_EXTRACTION_RESULTS, ""));

//        //Step 5: Reopen the main activity
//        Intent intent = new Intent(mContext, ProfileActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        mContext.startActivity(intent);

        MobileDataMapActivity.classifying = false;
    }

    /*
  * Method to add estimated BAC reading to database
  */
    public void addReadingToDatabaseAndPreferences() {
        double latitude = Utils.DUMMY_LATITUDE_AND_LONGITUDE_VALUE;
        double longitude = Utils.DUMMY_LATITUDE_AND_LONGITUDE_VALUE;

        // Getting current year, day of week, and number week for this reading
        int dayOfWeek = Utils.getDayOfWeek();
        int currentYear = Utils.getCurrentYear();
        int currentWeek = Utils.getCurrentWeek();



        // Values to put into database:
        ContentValues content = new ContentValues();
        content.put(DatabaseContract.BACReadingsTable.COLUMN_NAME_DAY_OF_WEEK, dayOfWeek);
        content.put(DatabaseContract.BACReadingsTable.COLUMN_NAME_WEEK_OF_YEAR, currentWeek);
        content.put(DatabaseContract.BACReadingsTable.COLUMN_NAME_YEAR, currentYear);
        content.put(DatabaseContract.BACReadingsTable.COLUMN_NAME_BAC, bin);

        // Actually add to database:
        writableDatabase.insert(DatabaseContract.BACReadingsTable.TABLE_NAME, null, content);

        DateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        Date date = new Date();
        String formattedDateString = dateFormatter.format(date);

        // Lastly, let's store in preferences the last BAC estimation:
        SharedPreferences.Editor editor = bacSharedPreferences.edit();
        Log.i("Classification", "In onPostExecute of Feature Extraction and Classification Task, we have now picked a bin for the user, which is: " + bin + " at the time " + formattedDateString);
        editor.putString(Utils.LAST_BAC_ESTIMATION, bin);
        editor.putString(Utils.LAST_BAC_ESTIMATION_TIME, formattedDateString);
        editor.commit();
        Log.i("Classification", "Just put classification results into database and preferences.");
            return;

    }

    //This method will classify the extracted data
    public void runClassification() throws Exception {
        featureExtractionResults = bacSharedPreferences.getString(Utils.FEATURE_EXTRACTION_RESULTS, "");

        //Normalizing our sway areas and volume:
        List<String> normalizedAttributes = getListofGeneratedFeatures(featureExtractionResults);

        //Getting our test data from the preferences:
        Instances userData = createWekaObject(normalizedAttributes, (1.0 * weight), genderNumber, height, age);
        userData.setClassIndex(userData.numAttributes() - 1);

        ConverterUtils.DataSource dataFile = new ConverterUtils.DataSource(mContext.getAssets().open("some10mov2000rem0movsegTWOCLASSselected.arff"));
        Instances data = dataFile.getDataSet();
        if(data.classIndex() == -1) {
            data.setClassIndex(data.numAttributes() -1);
        }

        //Creating our model based on the training data
        cls = (RandomForest) weka.core.SerializationHelper.read(mContext.getAssets().open("finalModel.model"));

        //Classify each instance individually
        classifyDataInstances(userData);

        Log.i("Classifying Data", "Via Weka, the data has been successfully classified!");
    }

    /*
 * This method will classify each instance
 * @param userData is the extracted feature data for the user
 */
    public void classifyDataInstances(Instances userData) throws Exception {
            double clsLabel = cls.classifyInstance(userData.get(0));
            bin = userData.classAttribute().value((int) clsLabel);
//        }
    }

    public List<String> getListofGeneratedFeatures(String generatedFeaturesString) {
        //If normalization was to be done, it would be done in here. We don't wish to do so at this time, though.
        LinkedList<String> listOfFeatures = new LinkedList<String>(Arrays.asList(generatedFeaturesString.split(",")));
        return listOfFeatures;

    }

    //This method creates a weka object
    public static Instances createWekaObject(List<String> listOfFeatures, Double weight, Double gender, Double height, Double age) {
        Double steps = Double.parseDouble(listOfFeatures.get(0));
        Double bandpower = Double.parseDouble(listOfFeatures.get(1));
        Double harmonicDistortion = Double.parseDouble(listOfFeatures.get(2));
        Double XZSwayArea = Double.parseDouble(listOfFeatures.get(3));
        Double XYSwayArea = Double.parseDouble(listOfFeatures.get(4));
        Double YZSwayArea = Double.parseDouble(listOfFeatures.get(5));
        Double distYForward = Double.parseDouble(listOfFeatures.get(6));
        Double rollVelVarianceForward = Double.parseDouble(listOfFeatures.get(7));
        Double rollVelMedianForward = Double.parseDouble(listOfFeatures.get(8));
        Double rollVelVarianceBackward = Double.parseDouble(listOfFeatures.get(9));
        Double pitchVelMedianForward = Double.parseDouble(listOfFeatures.get(10));
        Double pitchVelVarianceForward = Double.parseDouble(listOfFeatures.get(11));
        Double pitchVelMedianBackward = Double.parseDouble(listOfFeatures.get(12));
        Double pitchVelVarianceBackward = Double.parseDouble(listOfFeatures.get(13));
        Double yawVelMedianForward = Double.parseDouble(listOfFeatures.get(14));
        Double yawVelVarianceForward = Double.parseDouble(listOfFeatures.get(15));
        Double yawVelMedianBackward = Double.parseDouble(listOfFeatures.get(16));
        Double yawVelVarianceBackward = Double.parseDouble(listOfFeatures.get(17));
        Double pitchForward = Double.parseDouble(listOfFeatures.get(18));
        Double pitchBackward = Double.parseDouble(listOfFeatures.get(19));
        Double yawForward = Double.parseDouble(listOfFeatures.get(20));
        Double xVelMedianBackward = Double.parseDouble(listOfFeatures.get(21));
        Double yVelMedianForward = Double.parseDouble(listOfFeatures.get(22));
        Double yVelVarianceBackward = Double.parseDouble(listOfFeatures.get(23));
        Double yVelMedianBackward = Double.parseDouble(listOfFeatures.get(24));

        WekaDataFormatter myFormattedDataForWeka = new WekaDataFormatter(steps, bandpower, harmonicDistortion, XZSwayArea, XYSwayArea, YZSwayArea, distYForward, rollVelVarianceForward, rollVelMedianForward, rollVelVarianceBackward, pitchVelMedianForward, pitchVelVarianceForward, pitchVelMedianBackward, pitchVelVarianceBackward, yawVelMedianForward, yawVelVarianceForward, yawVelMedianBackward, yawVelVarianceBackward, pitchForward, pitchBackward, yawForward, xVelMedianBackward, yVelMedianForward, yVelVarianceBackward, yVelMedianBackward, height, weight, gender, age);

        return myFormattedDataForWeka.getInstanceData();
    }



    //Resetting these flags so we can alert the user again of these facts.
    private void resetFlagsSoWeCanSendAlertsAgain() {
        SharedPreferences.Editor editor = bacSharedPreferences.edit();
        editor.putBoolean(Utils.HAVE_WE_SENT_A_TEXT_ALERT_YET, false);
        editor.putBoolean(Utils.HAVE_WE_TOLD_USER_THEY_SURPASSED_DRIVING_LIMIT, false);
        editor.putBoolean(Utils.HAVE_WE_TOLD_USER_THEY_SURPASSED_THEIR_PERSONAL_LIMIT, false);
        editor.commit();
    }

    public void setupAndLoadUserData(){
        writableDatabase = new DatabaseContract.DatabaseHelper(mContext).getWritableDatabase();
        //Extract user data preferences
        bacSharedPreferences = mContext.getSharedPreferences(Utils.BAC_NOTIFICATION_SETTINGS, mContext.MODE_PRIVATE);
        featureExtractionResults = bacSharedPreferences.getString(Utils.FEATURE_EXTRACTION_RESULTS, "");
        //usersName = bacSharedPreferences.getString(Utils.NAME, ""); Something to add on later, maybe.
        gender = bacSharedPreferences.getString(Utils.GENDER, "");
        weight = bacSharedPreferences.getInt(Utils.WEIGHT, 0);
        height = (1.0 *(bacSharedPreferences.getInt(Utils.HEIGHT,0)) / Utils.CONVERT_INCHES_TO_CM);
        age =  Double.parseDouble(bacSharedPreferences.getString(Utils.AGE, ""));
        //Get gender number
        if(gender.contentEquals("male"))
            genderNumber = 1.0;
        else
            genderNumber = 0.0;
    }

    //this method handles feature extraction
    public void runCallToAPIToStartFeatureExtraction() throws IOException, JSONException{
        final String endpointURL = "http://130.215.250.210/?method=alcogait&format=json";

        readableDB = new DatabaseContract.DatabaseHelper(mContext).getReadableDatabase();

        ArrayList<Float> accelX = Utils.getSensorDataFromTable(readableDB, DatabaseContract.SensorReadingsTable.TABLE_NAME, DatabaseContract.SensorReadingsTable.COLUMN_NAME_X, "accelerometer");
        ArrayList<Float> accelY = Utils.getSensorDataFromTable(readableDB, DatabaseContract.SensorReadingsTable.TABLE_NAME, DatabaseContract.SensorReadingsTable.COLUMN_NAME_Y, "accelerometer");
        ArrayList<Float> accelZ = Utils.getSensorDataFromTable(readableDB, DatabaseContract.SensorReadingsTable.TABLE_NAME, DatabaseContract.SensorReadingsTable.COLUMN_NAME_Z, "accelerometer");
        ArrayList<Float> accelT = Utils.getSensorDataFromTable(readableDB, DatabaseContract.SensorReadingsTable.TABLE_NAME, DatabaseContract.SensorReadingsTable.COLUMN_NAME_TIMESTAMP, "accelerometer");
        ArrayList<Float> gyroX = Utils.getSensorDataFromTable(readableDB, DatabaseContract.SensorReadingsTable.TABLE_NAME, DatabaseContract.SensorReadingsTable.COLUMN_NAME_X, "gyroscope");
        ArrayList<Float> gyroY = Utils.getSensorDataFromTable(readableDB, DatabaseContract.SensorReadingsTable.TABLE_NAME, DatabaseContract.SensorReadingsTable.COLUMN_NAME_Y, "gyroscope");
        ArrayList<Float> gyroZ = Utils.getSensorDataFromTable(readableDB, DatabaseContract.SensorReadingsTable.TABLE_NAME, DatabaseContract.SensorReadingsTable.COLUMN_NAME_Z, "gyroscope");

        // Connect to server
        URL url = new URL(endpointURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        Log.i(TAG, "Made connection to AlcoWatch server.");

        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestMethod("POST");

        //Putting sensor data into their own JSON objects
        JSONObject accelerometer = new JSONObject();
        accelerometer.put("x", accelX);
        accelerometer.put("y", accelY);
        accelerometer.put("z",accelZ);
        accelerometer.put("t",accelT);
        JSONObject gyroscope = new JSONObject();
        gyroscope.put("x",gyroX);
        gyroscope.put("y",gyroY);
        gyroscope.put("z",gyroZ);

        //The parent object to hold both of these
        JSONObject sensorData = new JSONObject();
        sensorData.put("accelerometer", accelerometer);
        sensorData.put("gyroscope", gyroscope);

        //Sending the data
        OutputStream os = conn.getOutputStream();
        os.write(sensorData.toString().getBytes("UTF-8"));
        os.close();

        //Analyzing response
        InputStream in = conn.getInputStream();
        Log.i("Data From Server", "Input from the server was: "+ in.toString());
        extractedFeaturesJsonResult = IOUtils.toString(in);
        //Grab just the status code;
        String statusValue = extractedFeaturesJsonResult.substring((extractedFeaturesJsonResult.indexOf("status")), (extractedFeaturesJsonResult.indexOf("data")-1));
        statusValue = statusValue.replace("\"", ""); //remove that quotation mark from this string
        //Grab just our extracted features results:
        extractedFeaturesJsonResult = extractedFeaturesJsonResult.substring((extractedFeaturesJsonResult.indexOf("data")+7), extractedFeaturesJsonResult.length()-4);
        Log.i("Data From Server", "Extracted feature data was received from server: " + extractedFeaturesJsonResult + " and status code was: " + statusValue);

        //We now have results, so let's store them in preferences for a bit:
        SharedPreferences.Editor editor = bacSharedPreferences.edit();
        editor.putString(Utils.FEATURE_EXTRACTION_RESULTS, extractedFeaturesJsonResult);
        editor.commit();

        //Checking to see if this worked:
        if (bacSharedPreferences.getString(Utils.FEATURE_EXTRACTION_RESULTS, "") != "") {
            Log.i(TAG, "Successfully logged extracted feature data! This is what was put into preferences: " + bacSharedPreferences.getString(Utils.FEATURE_EXTRACTION_RESULTS, ""));
        }

    }
}