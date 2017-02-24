package edu.wpi.alcowatch.alcowatch.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Created by Jacob Watson on 2/16/2017.
 */

public class DatabaseContract {
    private static final String REAL_TYPE = " REAL";
    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    // Creates the tables
    private static final String CREATE_BAC_READINGS_TABLE =
            "CREATE TABLE " + BACReadingsTable.TABLE_NAME + " (" +
                    BACReadingsTable._ID + " INTEGER PRIMARY KEY," +
                    BACReadingsTable.COLUMN_NAME_WEEK_OF_YEAR + TEXT_TYPE + COMMA_SEP +
                    BACReadingsTable.COLUMN_NAME_YEAR + TEXT_TYPE + COMMA_SEP +
                    BACReadingsTable.COLUMN_NAME_DAY_OF_WEEK + TEXT_TYPE + COMMA_SEP +
                    BACReadingsTable.COLUMN_NAME_BAC + TEXT_TYPE + COMMA_SEP +
                    BACReadingsTable.COLUMN_NAME_LOCATION_LATITUDE + TEXT_TYPE + COMMA_SEP +
                    BACReadingsTable.COLUMN_NAME_LOCATION_LONGITUDE + TEXT_TYPE +
                    " )";

    private static final String CREATE_SENSOR_READINGS_TABLE =
            "CREATE TABLE " + SensorReadingsTable.TABLE_NAME + " (" +
                    SensorReadingsTable._ID + " INTEGER PRIMARY KEY," +
                    SensorReadingsTable.COLUMN_NAME_TIMESTAMP + REAL_TYPE + COMMA_SEP +
                    SensorReadingsTable.COLUMN_NAME_SENSOR_NAME + TEXT_TYPE + COMMA_SEP +
                    SensorReadingsTable.COLUMN_NAME_X + REAL_TYPE + COMMA_SEP +
                    SensorReadingsTable.COLUMN_NAME_Y + REAL_TYPE + COMMA_SEP +
                    SensorReadingsTable.COLUMN_NAME_Z + REAL_TYPE +
                    " )";

    private static final String CREATE_INITIAL_SENSOR_READINGS_TABLE =
            "CREATE TABLE " + InitialSensorReadingsTable.TABLE_NAME + " (" +
                    InitialSensorReadingsTable._ID + " INTEGER PRIMARY KEY," +
                    InitialSensorReadingsTable.COLUMN_NAME_TIMESTAMP + REAL_TYPE + COMMA_SEP +
                    InitialSensorReadingsTable.COLUMN_NAME_SENSOR_NAME + TEXT_TYPE + COMMA_SEP +
                    InitialSensorReadingsTable.COLUMN_NAME_X + REAL_TYPE + COMMA_SEP +
                    InitialSensorReadingsTable.COLUMN_NAME_Y + REAL_TYPE + COMMA_SEP +
                    InitialSensorReadingsTable.COLUMN_NAME_Z + REAL_TYPE +
                    " )";


    // Used for deleting the tables
    private static final String SQL_DELETE_BAC_READINGS_TABLE =
            "DROP TABLE IF EXISTS " + BACReadingsTable.TABLE_NAME;
    private static final String SQL_DELETE_SENSOR_READINGS_TABLE =
            "DROP TABLE IF EXISTS " + SensorReadingsTable.TABLE_NAME;
    private static final String SQL_DELETE_INITIAL_SENSOR_READINGS_TABLE =
            "DROP TABLE IF EXISTS " + InitialSensorReadingsTable.TABLE_NAME;

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public DatabaseContract() {
    }

    // Inner class that defines the table contents
    public static abstract class BACReadingsTable implements BaseColumns {
        public static final String TABLE_NAME = "bac_readings";
        public static final String COLUMN_NAME_WEEK_OF_YEAR = "timestamp";
        public static final String COLUMN_NAME_DAY_OF_WEEK = "dayofweek";
        public static final String COLUMN_NAME_YEAR = "year";
        public static final String COLUMN_NAME_BAC = "bac";
        public static final String COLUMN_NAME_LOCATION_LATITUDE = "latitude";
        public static final String COLUMN_NAME_LOCATION_LONGITUDE = "longitude";
    }

    public static abstract class SensorReadingsTable implements BaseColumns {
        public static final String TABLE_NAME = "sensor_readings";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_SENSOR_NAME = "sensor_name";
        public static final String COLUMN_NAME_X = "x";
        public static final String COLUMN_NAME_Y = "y";
        public static final String COLUMN_NAME_Z = "z";
    }

    public static abstract class InitialSensorReadingsTable implements BaseColumns {
        public static final String TABLE_NAME = "initial_sensor_readings";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_SENSOR_NAME = "sensor_name";
        public static final String COLUMN_NAME_X = "x";
        public static final String COLUMN_NAME_Y = "y";
        public static final String COLUMN_NAME_Z = "z";
    }

    public static class DatabaseHelper extends SQLiteOpenHelper {
        //If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION = 1;
        public static final String DATABASE_NAME = "Database.db";

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        //Creating each of these tables
        public void onCreate(SQLiteDatabase db) {
            Log.i("Initial DB Setup", "Database tables have been created!");
            db.execSQL(CREATE_SENSOR_READINGS_TABLE);
            db.execSQL(CREATE_INITIAL_SENSOR_READINGS_TABLE);
            db.execSQL(CREATE_BAC_READINGS_TABLE);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(CREATE_BAC_READINGS_TABLE);
            db.execSQL(CREATE_SENSOR_READINGS_TABLE);
            db.execSQL(CREATE_INITIAL_SENSOR_READINGS_TABLE);
            onCreate(db);
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }

    }
}
