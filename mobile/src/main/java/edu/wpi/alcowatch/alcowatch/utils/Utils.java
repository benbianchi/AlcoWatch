package edu.wpi.alcowatch.alcowatch.utils;

/**
 * Created by Jacob Watson on 2/16/2017.
 */

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class Utils {
    // Constant number values
    public static final double DUMMY_LATITUDE_AND_LONGITUDE_VALUE = 99999999.99; // This is used if we can't get a user's lat and long due to their GPS being off
    public static final int REPEATING_INTERVAL_IN_MILLIS = 900000; // This is how often the alarm repeats
    public static final int NUMBER_OF_SECONDS_IN_A_MINUTE = 60; // I wonder what this is...
    public static final Integer PICK_CONTACT_REQUEST = 99; // Lets us know when the pick-a-contact-to-text-when-I'm-too-drunk activity has completed
    public static final Integer SETUP = 98; // Lets us know when setup has been completed
    public static final Double CONVERT_INCHES_TO_CM = 2.54;
    // Constant string values used for SharedPreferences
    public static final String AGE = "age";
    public static final String PROFILE = "profile"; // Identifies part of shared preferences containing user's personal info
    public static final String BAC_NOTIFICATION_SETTINGS = "BACNotificationSettings"; // Identifies part of shared preferences with anything related to BAC
    public static final String NAME = "name";
    public static final String GENDER = "gender";
    public static final String CALLBACK = "callback";
    public static final String WEIGHT = "weight";
    public static final String HEIGHT = "height";
    public static final String BIRTHDAY = "birthday";
    public static final String BIRTHMONTH = "birthMonth";
    public static final String BIRTHYEAR = "birthYear";
    public static final String FEATURE_EXTRACTION_RESULTS = "featureExtractionResults";
    public static final String BAC_START_HOUR = "BACStartHour";
    public static final String BAC_END_HOUR = "BACEndHour";
    public static final String LAST_BAC_ESTIMATION = "lastBACEstimation";
    public static final String RATIO_OR_THD_FAILED = "RatioOrThdFailed";
    public static final String RATIO_AND_THD_FAILED = "Both ratio and THD failed.";
    public static final String JUST_RATIO_FAILED = "Just Ratio failed.";
    public static final String JUST_THD_FAILED = "Just THD failed.";
    public static final String SERVER_LOG = "serverLog";
    public static final String BAC_LIMIT = "BACLimit";
    public static final String BAC_LIMIT_TEXT_ALERT = "BACLimitForTextAlert";
    public static final String BAC_LIMIT_TEXT_ALERT_NUMBER = "BACLimitForTextAlertNumber";
    public static final String LAST_BAC_ESTIMATION_TIME = "lastBACEstimationTime";
    public static final String SOBER_WALKING_DATA_XZ_SWAY_AREA = "soberWalkingDataXZSwayArea";
    public static final String SOBER_WALKING_DATA_XY_SWAY_AREA = "soberWalkingDataXYSwayArea";
    public static final String SOBER_WALKING_DATA_YZ_SWAY_AREA = "soberWalkingDataYZSwayArea";
    public static final String SOBER_WALKING_DATA_SWAY_VOLUME = "soberWalkingDataSwayVolume";
    public static final String HAVE_WE_SENT_A_TEXT_ALERT_YET = "haveWeSentTextAlertYet";
    public static final String HAVE_WE_TOLD_USER_THEY_SURPASSED_DRIVING_LIMIT = "haveWeToldUserTheySurpassedDrivingLimit";
    public static final String HAVE_WE_TOLD_USER_THEY_SURPASSED_THEIR_PERSONAL_LIMIT = "haveWeToldUserTheySurpassedPersonalLimit";
    public static final String ALARM_SET = "alarmSet";

    /*
 * Helper to get today's current week of the year.
 */
    public static Integer getCurrentWeek(){
        String format = "yyyyMMdd";

        SimpleDateFormat df = new SimpleDateFormat(format);
        Date date = new Date();

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.WEEK_OF_YEAR);
    }

    /*
     * Helper to get today's current year.
     */
    public static Integer getCurrentYear(){
        String format = "yyyyMMdd";

        SimpleDateFormat df = new SimpleDateFormat(format);
        Date date = new Date();

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.YEAR);
    }

    /*
     * Helper to get today's day of the week.
     * 1 = Sunday
     * 2 = Monday
     * 3 = Tuesday
     * 4 = Wednesday
     * 5 = Thursday
     * 6 = Friday
     * 7 = Saturday
     */
    public static Integer getDayOfWeek(){
        String format = "yyyyMMdd";

        SimpleDateFormat df = new SimpleDateFormat(format);
        Date date = new Date();

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.DAY_OF_WEEK);
    }

    //This method will get all info from a selected column from the database.
    public static ArrayList<Float> getSensorDataFromTable(SQLiteDatabase db, String tableName, String columnName, String sensorName) {
        ArrayList<Float> values = new ArrayList<Float>();
        try {
            //Write what columns we want from the table
            String[] projection = {
                    columnName,
            };
            //Our where clause instruction, saying what column needs to match with a value
            String whereClauseInstructions = DatabaseContract.SensorReadingsTable.COLUMN_NAME_SENSOR_NAME + "=?";
            //What the value of that column needs to be
            String[] whereClauseMatchingTerm = {
                    sensorName,
            };
            //Organize company names in this order...
            String sortOrder =
                    DatabaseContract.SensorReadingsTable.COLUMN_NAME_TIMESTAMP + " ASC";
            //Our select statement
            Cursor cursor = db.query(
                    tableName,
                    projection,
                    whereClauseInstructions,
                    whereClauseMatchingTerm,
                    null,
                    null,
                    sortOrder,
                    "600"
            );

            //Get our floats from the cursor
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                values.add(cursor.getFloat(0));
                cursor.moveToNext();
            }
            cursor.close();
        } catch (Exception e) {
            Log.e("Table Error", e.getMessage());
            Log.e("Table Error", e.getStackTrace().toString());
        }
        return values;
    }
}