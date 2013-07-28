package com.podnoms.android.podcatcher.providers;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;
import com.podnoms.android.podcatcher.PodNomsApplication;
import com.podnoms.android.podcatcher.util.FileUtils;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class DataStore extends SQLiteOpenHelper {

    public final static String DATABASE_NAME = "podnoms.sqlite";
    public final static int DATABASE_VERSION = 4;
    private static final String TAG = "DataStore";
    private static final int TEST_RECORD_COUNT = 3;
    private Context _context;
    private static final boolean SEED_DATA = false;

    public DataStore(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        _context = context;
        _copyDatabase();
        if (PodNomsApplication.isDebug())
            _context.deleteDatabase(DATABASE_NAME);
    }

    private void _copyDatabase() {
        try {
            File sd = Environment.getExternalStorageDirectory();
            File data = Environment.getDataDirectory();

            if (sd.canWrite()) {
                String currentDBPath = "//data//com.podnoms.android.podcatcher//databases//" + DATABASE_NAME;
                File currentDB = new File(data, currentDBPath);
                File backupDB = new File(sd, DATABASE_NAME);

                if (currentDB.exists()) {
                    FileUtils.copyFile(currentDB, backupDB, true);
                }
            }
        } catch (IOException e) {
            Log.e(PodNomsApplication.PACKAGE, e.getMessage());
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            Log.i(PodNomsApplication.PACKAGE, "Creating database");
            AssetManager am = _context.getAssets();
            InputStreamReader in = new InputStreamReader(am.open("podnoms.sql"));
            BufferedReader br = new BufferedReader(in);

            ArrayList<String> statements = FileUtils.parseSqlFile(br);
            for (String sql : statements) {
                db.execSQL(sql);
            }
            if (SEED_DATA) {
                for (int i = 1; i <= TEST_RECORD_COUNT; i++) {
                    String sql = String.format(
                            "INSERT INTO podcast (title, description, date_updated) VALUES('Podcast Title %d', 'Podcast Description %d', datetime())", i, i);
                    db.execSQL(sql);
                }
            }
            //clear these fellas
            PodNomsApplication.clearCacheDirectory();
            if (PodNomsApplication.isDebug()) {
                PodNomsApplication.clearDownloadsDirectory();
            }
            onUpgrade(db, 1, DATABASE_VERSION);
        } catch (SQLException ex) {
            Log.e(TAG, "Exception creating database: " + ex.getLocalizedMessage());
            throw ex;
        } catch (Exception ex) {
            Log.e(TAG, "Exception creating database: " + ex.getLocalizedMessage());
            throw new IllegalArgumentException("Exception creating database: " + ex.getLocalizedMessage());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(PodNomsApplication.PACKAGE, "Updating database from version " + oldVersion + " to " + newVersion);
        int curVer = oldVersion;
        while (curVer <= newVersion) {
            switch (curVer) {
                case 3:
                    try {
                        db.execSQL("ALTER TABLE podcast ADD COLUMN resource_uri VARCHAR(100)");
                    } catch (SQLiteException ex) {
                    }
                    break;
                case 4:
                    db.execSQL("CREATE TABLE podcast01(\n" +
                            "  _id,\n" +
                            "  title,\n" +
                            "  description,\n" +
                            "  url,\n" +
                            "  image,\n" +
                            "  date_updated,\n" +
                            "  date_created,\n" +
                            "  resource_uri\n" +
                            ")\n");
                    db.execSQL("INSERT INTO podcast01\n" +
                            "SELECT\n" +
                            "  _id,\n" +
                            "  title,\n" +
                            "  description,\n" +
                            "  url,\n" +
                            "  image,\n" +
                            "  date_updated,\n" +
                            "  date_created,\n" +
                            "  resource_uri\n" +
                            "FROM podcast;\n");

                    db.execSQL("DROP TABLE podcast;\n");
                    db.execSQL("CREATE TABLE podcast (\n" +
                            "  _id           integer PRIMARY KEY AUTOINCREMENT,\n" +
                            "  title         varchar(255) UNIQUE,\n" +
                            "  description   varchar(1000),\n" +
                            "  url           varchar(255) NOT NULL UNIQUE,\n" +
                            "  image         varchar(255),\n" +
                            "  date_updated  date,\n" +
                            "  date_created  date,\n" +
                            "  resource_uri  varchar(100) UNIQUE\n" +
                            ");\n");
                    db.execSQL("INSERT INTO podcast\n" +
                            "  (_id,\n" +
                            "  title,\n" +
                            "  description,\n" +
                            "  url,\n" +
                            "  image,\n" +
                            "  date_updated,\n" +
                            "  date_created,\n" +
                            "  resource_uri)\n" +
                            "SELECT\n" +
                            "  _id,\n" +
                            "  title,\n" +
                            "  description,\n" +
                            "  url,\n" +
                            "  image,\n" +
                            "  date_updated,\n" +
                            "  date_created,\n" +
                            "  resource_uri\n" +
                            "FROM podcast01;\n");
                    db.execSQL("DROP TABLE podcast01;");
            }
            curVer++;
        }
    }
}
