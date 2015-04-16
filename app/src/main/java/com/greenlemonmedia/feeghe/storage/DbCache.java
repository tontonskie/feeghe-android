package com.greenlemonmedia.feeghe.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.greenlemonmedia.feeghe.api.APIUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * Created by tonton on 1/23/15.
 */
public class DbCache implements Serializable {

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
    return APIUtils.hash(query.toString());
  }

  /**
   *
   * @param cacheTableName
   * @param queryId
   * @param data
   */
  public void set(String cacheTableName, String queryId, JSONArray data) {
    int dataLength = data.length();
    try {
      for (int i = 0; i < dataLength; i++) {
        set(cacheTableName, queryId, data.getJSONObject(i));
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
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
   * @param queryId
   * @return
   */
  public Cursor getRows(String cacheTableName, String queryId) {
    Cursor result;
    if (queryId != null) {
      result = dbWriter.query(cacheTableName, null, "query_id = ?", new String[] { queryId }, null, null, null);
    } else {
      result = dbWriter.query(cacheTableName, null, null, null, null, null, null);
    }
    return result;
  }

  /**
   *
   * @param cacheTableName
   * @param objectId
   * @return
   */
  public DbCacheRow getRow(String cacheTableName, String objectId) {
    Cursor result = dbWriter.query(cacheTableName, null, "obj_id = ?", new String[] { objectId }, null, null, null);
    result.moveToFirst();
    DbCacheRow row = null;
    if (result.getCount() != 0) {
      row = new DbCacheRow();
      row.id = result.getInt(result.getColumnIndex("id"));
      row.objectId = result.getString(result.getColumnIndex("obj_id"));
      row.queryId = result.getString(result.getColumnIndex("query_id"));
      row.content = result.getString(result.getColumnIndex("content"));
    }
    result.close();
    return row;
  }

  /**
   *
   * @param cacheTableName
   * @param objectId
   */
  public void delete(String cacheTableName, String objectId) {
    dbWriter.delete(cacheTableName, "obj_id = ?", new String[] { objectId });
  }

  /**
   *
   * @param cacheTableName
   * @param queryId
   */
  public void deleteByQueryId(String cacheTableName, String queryId) {
    dbWriter.delete(cacheTableName, "query_id = ?", new String[] { queryId });
  }

  private class DbCacheRow {
    public int id;
    public String queryId;
    public String objectId;
    public String content;
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
      db.execSQL("create table face(" +
        "id integer primary key autoincrement, " +
        "obj_id varchar not null unique, " +
        "query_id varchar not null, " +
        "content text not null, " +
        "updated_at datetime current_timestamp);"
      );
      db.execSQL("create table faceComments(" +
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
