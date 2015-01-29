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
  private boolean isDeleted = false;

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
  public boolean isDeleted() {
    return isDeleted;
  }

  /**
   *
   * @return
   */
  public void delete() {
    cache.delete(tableName, objectId);
    isDeleted = true;
  }


  /**
   *
   * @return
   */
  public ResponseObject getContent() {
    if (isDeleted) {
      return null;
    }
    return new ResponseObject(cache.get(tableName, objectId));
  }

  /**
   *
   * @param data
   * @return
   */
  public void update(JSONObject data) {
    if (isDeleted) {
      throw new CacheEntryException("CacheEntry " + objectId + " already deleted");
    }
    cache.update(tableName, objectId, data);
  }

  private class CacheEntryException extends RuntimeException {

    public CacheEntryException(String message) {
      super(message);
    }
  }
}
