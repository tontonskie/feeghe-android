package com.greenlemonmedia.feeghe.api;

import android.content.Context;

import org.apache.http.client.methods.HttpPut;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tonton on 1/30/15.
 */
public class FaceService extends APIService {

  public FaceService(Context context) {
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

  @Override
  public JSONObject getCacheQuery() {
    return createWhereQuery(new JSONObject());
  }
}
