package com.greenlemonmedia.feeghe.api;

import android.app.Activity;

import org.apache.http.client.methods.HttpPost;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tonton on 1/11/15.
 */
public class ContactService extends APIService {

  /**
   *
   * @param context
   */
  public ContactService(Activity context) {
    super("contact", context);
  }

  /**
   *
   * @param ids
   * @param callback
   */
  public void sync(JSONArray ids, SaveCallback callback) {
    HttpPost postRequest = new HttpPost(getBaseUrl("sync"));
    JSONObject params = new JSONObject();
    try {
      params.put("contacts", ids);
      params.put("owner", session.getUserId());
    } catch (JSONException e) {
      e.printStackTrace();
    }
    setBodyParams(postRequest, params);
    apiAsyncCall(postRequest, callback);
  }

  @Override
  public JSONObject getCacheQuery() {
    JSONObject query = new JSONObject();
    try {
      query.put("owner", session.getUserId());
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return createWhereQuery(query);
  }
}
