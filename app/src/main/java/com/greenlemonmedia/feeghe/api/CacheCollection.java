package com.greenlemonmedia.feeghe.api;

import com.greenlemonmedia.feeghe.storage.DbCache;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by tonton on 1/27/15.
 */
public class CacheCollection {

  private DbCache cache;
  private String tableName;
  private String queryId;

  /**
   *
   * @param dbCache
   */
  public CacheCollection(String modelName, DbCache dbCache, String queryId) {
    cache = dbCache;
    tableName = modelName;
    this.queryId = queryId;
  }

  /**
   *
   * @return
   */
  public ResponseArray getContent() {
    return new ResponseArray(cache.getArray(tableName, queryId), true);
  }

  /**
   *
   * @param id
   */
  public void delete(String id) {
    cache.delete(tableName, id);
  }

  /**
   *
   * @param id
   * @param data
   */
  public void update(String id, JSONObject data) {
    cache.update(tableName, id, data);
  }

  /**
   *
   * @param data
   */
  public void save(JSONObject data) {
    cache.set(tableName, queryId, data);
  }

  /**
   *
   * @param data
   */
  public void save(JSONArray data) {
    cache.set(tableName, queryId, data);
  }

  /**
   *
   * @param id
   * @return
   */
  public ResponseObject get(String id) {
    return new ResponseObject(cache.get(tableName, id));
  }
}
