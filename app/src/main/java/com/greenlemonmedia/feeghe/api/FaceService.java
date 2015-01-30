package com.greenlemonmedia.feeghe.api;

import android.content.Context;

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

  @Override
  public JSONObject getCacheQuery() {
    return null;
  }
}
