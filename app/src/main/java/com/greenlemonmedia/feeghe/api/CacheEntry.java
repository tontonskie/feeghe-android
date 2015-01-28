package com.greenlemonmedia.feeghe.api;

import com.greenlemonmedia.feeghe.storage.DbCache;

import org.json.JSONObject;

/**
 * Created by tonton on 1/28/15.
 */
public class CacheEntry {

  protected DbCache cache;
  private String objectId;
  protected String tableName;

  /**
   *
   * @param cacheTableName
   * @param id
   * @param dbCache
   */
  public CacheEntry(String cacheTableName, String id, DbCache dbCache) {
    objectId = id;
    cache = dbCache;
    tableName = cacheTableName;
  }

  /**
   *
   * @return
   */
  public void delete() {
    cache.delete(tableName, objectId);
  }

  /**
   *
   * @return
   */
  public ResponseObject getContent() {
    return new ResponseObject(cache.get(tableName, objectId));
  }

  /**
   *
   * @param data
   * @return
   */
  public void update(JSONObject data) {
    cache.update(tableName, objectId, data);
  }
}
