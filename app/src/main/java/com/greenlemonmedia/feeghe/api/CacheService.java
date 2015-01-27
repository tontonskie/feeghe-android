package com.greenlemonmedia.feeghe.api;

import com.greenlemonmedia.feeghe.storage.DbCache;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by tonton on 1/27/15.
 */
public class CacheService implements ServiceInterface {

  private DbCache cache;
  private String cacheKey;
  private String queryId;

  /**
   *
   * @param dbCache
   */
  public CacheService(String modelName, DbCache dbCache) {
    cache = dbCache;
    cacheKey = modelName;
  }

  /**
   *
   * @param query
   */
  public void setQueryId(JSONObject query) {
    queryId = DbCache.createQueryHash(query);
  }

  /**
   *
   * @param query
   * @return
   */
  @Override
  public ResponseArray query(JSONObject query) {
    return new ResponseArray(cache.getArray(cacheKey, query), true);
  }

  /**
   *
   * @param data
   * @return
   */
  @Override
  public ResponseObject save(JSONObject data) {
    cache.set(cacheKey, queryId, data);
    return new ResponseObject(data, true);
  }

  /**
   *
   * @param data
   */
  public void save(JSONArray data) {
    cache.set(cacheKey, queryId, data);
  }

  /**
   *
   * @param id
   * @return
   */
  @Override
  public ResponseObject delete(String id) {
    return null;
  }

  /**
   *
   * @param id
   * @return
   */
  @Override
  public ResponseObject get(String id) {
    return null;
  }

  /**
   *
   * @param id
   * @param data
   * @return
   */
  @Override
  public ResponseObject update(String id, JSONObject data) {
    return null;
  }
}
