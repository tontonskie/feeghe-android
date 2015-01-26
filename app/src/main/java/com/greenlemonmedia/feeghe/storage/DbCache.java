package com.greenlemonmedia.feeghe.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * Created by tonton on 1/23/15.
 */
public class DbCache {

  private DbHelper dbHelper;
  private SQLiteDatabase dbWriter;

  private static DbCache instance;

  /**
   *
   * @param context
   */
  private DbCache(Context context) {
    dbHelper = new DbHelper(context);
    dbWriter = dbHelper.getWritableDatabase();
  }

  /**
   *
   * @param context
   * @return
   */
  public static DbCache getInstance(Context context) {
    if (instance == null) {
      instance = new DbCache(context);
    }
    return instance;
  }

  /**
   *
   * @param query
   * @return
   */
  public static String createQueryHash(JSONObject query) {
    MessageDigest md = null;
    try {
      md = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    md.reset();
    md.update(query.toString().getBytes());
    byte[] result = md.digest();
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < result.length; i++) {
      sb.append(String.format("%02x", result[i]));
    }
    return sb.toString();
  }

  /**
   *
   * @param cacheTableName
   * @param data
   * @return
   */
  public void set(String cacheTableName, JSONObject query, JSONObject data) {
    set(cacheTableName, createQueryHash(query), data);
  }

  /**
   *
   * @param cacheTableName
   * @param queryId
   * @param data
   */
  public void set(String cacheTableName, String queryId, JSONObject data) {
    ContentValues contentValues = new ContentValues();
    try {
      contentValues.put("obj_id", data.getString("id"));
      contentValues.put("query_id", queryId);
      contentValues.put("content", data.toString());
    } catch (JSONException e) {
      e.printStackTrace();
    }
    dbWriter.insert(cacheTableName, null, contentValues);
  }

  /**
   *
   * @param cacheTableName
   * @param id
   * @param data
   * @return
   */
  public void update(String cacheTableName, String id, JSONObject data) {
    ContentValues contentValues = new ContentValues();
    contentValues.put("content", data.toString());
    contentValues.put("updated_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
    dbWriter.update(cacheTableName, contentValues, "obj_id = ?", new String[]{id});
  }

  /**
   *
   * @param cacheTableName
   * @param query
   * @return
   */
  public JSONArray getArray(String cacheTableName, JSONObject query) {
    return getArray(cacheTableName, createQueryHash(query));
  }

  /**
   *
   * @param cacheTableName
   * @param queryId
   * @return
   */
  public JSONArray getArray(String cacheTableName, String queryId) {
    Cursor result;
    if (queryId != null) {
      result = dbWriter.query(cacheTableName, null, "query_id = ?", new String[] { queryId }, null, null, null);
    } else {
      result = dbWriter.query(cacheTableName, null, null, null, null, null, null);
    }
    JSONArray fromCache = new JSONArray();
    try {
      result.moveToFirst();
      while (!result.isAfterLast()) {
        fromCache.put(new JSONObject(result.getString(result.getColumnIndex("content"))));
        result.moveToNext();
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
    result.close();
    return fromCache;
  }

  /**
   *
   * @param cacheTableName
   * @param objectId
   * @return
   */
  public JSONObject get(String cacheTableName, String objectId) {
    Cursor result = dbWriter.query(cacheTableName, null, "obj_id = ?", new String[] { objectId }, null, null, null);
    result.moveToFirst();
    JSONObject fromCache = null;
    try {
      fromCache = new JSONObject(result.getString(result.getColumnIndex("content")));
    } catch (JSONException e) {
      e.printStackTrace();
    }
    result.close();
    return fromCache;
  }

  public class DbHelper extends SQLiteOpenHelper {

    public static final int DB_VERSION = 1;
    public static final String DB_NAME = "feeghe_cache";

    /**
     *
     * @param context
     */
    public DbHelper(Context context) {
      super(context, DB_NAME, null, DB_VERSION);
    }

    /**
     *
     * @param db
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL("create table message(" +
        "id integer primary key autoincrement, " +
        "obj_id varchar not null unique, " +
        "query_id varchar not null, " +
        "content text not null, " +
        "updated_at datetime current_timestamp);"
      );
      db.execSQL("create table contact(" +
        "id integer primary key autoincrement, " +
        "obj_id varchar not null unique, " +
        "query_id varchar not null, " +
        "content text not null, " +
        "updated_at datetime current_timestamp);"
      );
      db.execSQL("create table room(" +
        "id integer primary key autoincrement, " +
        "obj_id varchar not null unique, " +
        "query_id varchar not null, " +
        "content text not null, " +
        "updated_at datetime current_timestamp);"
      );
    }

    /**
     *
     * @param db
     * @param oldVersion
     * @param newVersion
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
  }
}
