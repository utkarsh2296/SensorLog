package h.utkarshdubey.a3_mt17063_sensorlog;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class A3_MT17063_DatabaseHelper extends SQLiteOpenHelper {

    public static final String db_name="IIITD.db";
    public static final String table_name="sensortable";
    public static final String id="ID";
    public static final String time="TIMESTAND";
    public static final String name="Name";
    public static final String val="VAL";

    public static int version=1;
    String createTable="CREATE TABLE " + table_name + "("
            + id + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
            + time + " TEXT,"
            + name + " TEXT,"
            + val + " TEXT"
            +")";

    public A3_MT17063_DatabaseHelper(Context context ) {
        super(context, db_name, null, version);
        SQLiteDatabase db= this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        Log.d("In onCreate:", String.valueOf(db.getVersion()));

        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        Log.d("In onUpgrade:", String.valueOf(db.getVersion()));
        db.execSQL("DROP TABLE IF EXISTS "+ table_name);

    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onDowngrade(db, oldVersion, newVersion);
        Log.d("In onDowngrade:", String.valueOf(db.getVersion()));
    }
}
