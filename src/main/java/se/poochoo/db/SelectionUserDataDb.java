package se.poochoo.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.StringBufferInputStream;

public class SelectionUserDataDb extends SQLiteOpenHelper {

    public static final String TABLE_NAME = "SelectionUserData";

    public static final String SID_COLUMN = "sid";
    public static final String RESOURCE_COLUMN = "resourceHash";
    @Deprecated
    public static final String METRO_COLUMN = "metro";
    public static final String SELECTION_COLUMN = "selectionType";
    public static final String MULTIPLIER_COLUMN = "multiplier";

    public static final String DATABASE_NAME = "SelectionUserDataDb";
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_CREATE =
            "CREATE TABLE " + TABLE_NAME + " (" +
            SID_COLUMN + " INTEGER NOT NULL," + // Column 0
            RESOURCE_COLUMN + " INTEGER NOT NULL," + // Column 1
            METRO_COLUMN + " INTEGER DEFAULT 0," + // Column 2
            SELECTION_COLUMN + " INTEGER NOT NULL," + // Column 3
            MULTIPLIER_COLUMN + " INTEGER DEFAULT 1," + // Column 4
            "PRIMARY KEY (sid, resourceHash, metro));";

    static final String IS_PROMOTED_SQL = "SELECT multiplier, selectionType FROM " +SelectionUserDataDb.TABLE_NAME + " WHERE " +
        SelectionUserDataDb.SID_COLUMN + "= ? AND " +
        SelectionUserDataDb.RESOURCE_COLUMN + "= ? AND "+
        SelectionUserDataDb.SELECTION_COLUMN + "= ?;";

    static final String CURRENT_MULTIPLIER_SQL = "SELECT multiplier, selectionType FROM " +SelectionUserDataDb.TABLE_NAME + " WHERE " +
        SelectionUserDataDb.SID_COLUMN + "= ? AND " +
        SelectionUserDataDb.RESOURCE_COLUMN + "= ?;";

    static final String DELETE_WHERE = SelectionUserDataDb.SID_COLUMN + "= ? AND " +
        SelectionUserDataDb.RESOURCE_COLUMN + "= ?;";

    static final String STORE_ACTION_SQL = "INSERT OR REPLACE INTO " + SelectionUserDataDb.TABLE_NAME + "(" +
        SelectionUserDataDb.SID_COLUMN + "," +
        SelectionUserDataDb.RESOURCE_COLUMN + "," +
        SelectionUserDataDb.METRO_COLUMN + "," +
        SelectionUserDataDb.SELECTION_COLUMN + "," +
        SelectionUserDataDb.MULTIPLIER_COLUMN +
        ") VALUES (?, ?, ?, ?, ?)";

    public SelectionUserDataDb(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        throw new RuntimeException("Database upgrade not implement clear app data... oops");
    }
}