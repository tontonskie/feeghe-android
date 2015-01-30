package com.greenlemonmedia.feeghe.api;

import android.database.Cursor;
import android.os.Build;

import com.greenlemonmedia.feeghe.storage.DbCache;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by tonton on 1/27/15.
 */
public class CacheCollection {

  private DbCache cache;
  private String tableName;
  private String queryId;
  private ResponseArray cacheData;
  private ArrayList<String> objectIds;

  /**
   *
   * @param modelName
   * @param dbCache
   * @param queryId
   */
  public CacheCollection(String modelName, DbCache dbCache, String queryId) {
    cache = dbCache;
    tableName = modelName;
    this.queryId = queryId;
    setData();
  }

  /**
   *
   * @return
   */
  public int length() {
    return cacheData.getContent().length();
  }

  public void setData() {
    objectIds = new ArrayList<>();
    JSONArray jsonData = new JSONArray();
    Cursor cacheData = cache.getRows(tableName, queryId);
    try {
      cacheData.moveToFirst();
      while (!cacheData.isAfterLast()) {
        objectIds.add(cacheData.getString(cacheData.getColumnIndex("obj_id")));
        jsonData.put(new JSONObject(cacheData.getString(cacheData.getColumnIndex("content"))));
        cacheData.moveToNext();
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
    cacheData.close();
    this.cacheData = new ResponseArray(jsonData, true);
  }

  /**
   *
   * @return
   */
  public ResponseArray getData() {
    return cacheData;
  }

  /**
   *
   * @param id
   */
  public void delete(String id) {
    cache.delete(tableName, id);
    int position = objectIds.indexOf(id);
    if (position >= 0) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        cacheData.getContent().remove(position);
      } else {
        JSONArray tmpCache = new JSONArray();
        JSONArray jsonCache = cacheData.getContent();
        int jsonCacheLength = jsonCache.length();
        try {
          for (int i = 0; i < jsonCacheLength; i++) {
            if (i != position) {
              tmpCache.put(jsonCache.getJSONObject(i));
            }
          }
        } catch (JSONException ex) {
          ex.printStackTrace();
        }
        cacheData = new ResponseArray(tmpCache, true);
      }
      objectIds.remove(position);
    }
  }

  /**
   *
   * @param id
   * @param data
   */
  public void replace(String id, JSONObject data) {
    cache.update(tableName, id, data);
    int position = objectIds.indexOf(id);
    if (position >= 0) {
      try {
        cacheData.getContent().put(position, data);
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   *
   * @param id
   * @param values
   */
  public void update(String id, JSONObject values) {
    JSONObject content = get(id).getContent();
    Iterator<String> i = values.keys();
    try {
      while (i.hasNext()) {
        String key = i.next();
        content.put(key, values.get(key));
      }
    } catch (JSONException ex) {
      ex.printStackTrace();
    }
    replace(id, content);
  }

  /**
   *
   * @param data
   */
  public void save(JSONObject data) {
    cache.set(tableName, queryId, data);
    add(data);
  }

  /**
   *
   * @param data
   */
  public void save(JSONArray data) {
    cache.set(tableName, queryId, data);
    int dataLength = data.length();
    try {
      for (int i = 0; i < dataLength; i++) {
        add(data.getJSONObject(i));
      }
    } catch (JSONException ex) {
      ex.printStackTrace();
    }
  }

  /**
   *
   * @param data
   */
  private void add(JSONObject data) {
    cacheData.getContent().put(data);
    try {
      objectIds.add(data.getString("id"));
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  /**
   *
   * @param id
   * @return
   */
  public ResponseObject get(String id) {
    JSONObject data = null;
    try {
      data = cacheData.getContent().getJSONObject(objectIds.indexOf(id));
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return new ResponseObject(data, true);
  }

  /**
   *
   * @param responseFromServer
   * @return
   */
  public ResponseArray update(ResponseArray responseFromServer) {
    JSONArray addedData = new JSONArray();
    JSONArray dataFromServer = responseFromServer.getContent();
    int dataFromServerLength = dataFromServer.length();
    try {
      for (int i = 0; i < dataFromServerLength; i++) {
        JSONObject dataEntry = (JSONObject) dataFromServer.getJSONObject(i);
        if (objectIds.indexOf(dataEntry.getString("id")) < 0) {
          save(dataEntry);
          addedData.put(dataEntry);
        }
      }
    } catch (JSONException ex) {
      ex.printStackTrace();
    }
    return new ResponseArray(addedData, true);
  }
}
