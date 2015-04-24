package com.greenlemonmedia.feeghe.api;

import android.app.Activity;

import org.apache.http.client.methods.HttpPut;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tonton on 1/30/15.
 */
public class FaceService extends APIService {

  public FaceService(Activity context) {
    super("face", context);
  }

  /**
   *
   * @param callback
   */
  public void getUsableFaces(QueryCallback callback) {
    JSONObject query = null;
    String jsonParamString = "{\"or\":[{\"favoritedBy." + session.getUserId();
    jsonParamString += "\":{\"!\":null}},{\"user\":\"" + session.getUserId() + "\"}]}";
    try {
      query = createWhereQuery(new JSONObject(jsonParamString));
    } catch (JSONException e) {
      e.printStackTrace();
    }
    query(query, callback);
  }

  /**
   *
   * @param faceId
   * @param like
   * @param callback
   */
  public void like(String faceId, boolean like, UpdateCallback callback) {
    HttpPut putRequest = new HttpPut(getBaseUrl(faceId + "/like"));
    JSONObject params = new JSONObject();
    try {
      params.put("user", session.getUserId());
      params.put("like", like);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    setBodyParams(putRequest, params);
    apiAsyncCall(putRequest, callback);
  }

  /**
   *
   * @param faceId
   * @param favorite
   * @param callback
   */
  public void favorite(String faceId, boolean favorite, UpdateCallback callback) {
    HttpPut putRequest = new HttpPut(getBaseUrl(faceId + "/favorite"));
    JSONObject params = new JSONObject();
    try {
      params.put("user", session.getUserId());
      params.put("favorite", favorite);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    setBodyParams(putRequest, params);
    apiAsyncCall(putRequest, callback);
  }

  @Override
  public JSONObject getCacheQuery() {
    JSONObject cacheQuery = null;
    try {
      cacheQuery = createWhereQuery(new JSONObject("{\"or\":[{\"privacy\":{\"private\":false}},{\"user\":\"" + session.getUserId()+ "\"}]}"));
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return cacheQuery;
  }
}
