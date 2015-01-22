package com.greenlemonmedia.feeghe.api;

import android.content.Context;

import org.apache.http.client.methods.HttpPut;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tonton on 1/14/15.
 */
public class RoomService extends APIService {

  /**
   *
   * @param context
   */
  public RoomService(Context context) {
    super("room", context);
  }

  /**
   *
   * @param roomId
   * @return
   */
  public ResponseObject visit(String roomId) {
    HttpPut putRequest = new HttpPut(getBaseUrl("visit"));
    JSONObject params = new JSONObject();
    try {
      params.put("id", roomId);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    setBodyParams(putRequest, params);
    return (ResponseObject) apiCall(putRequest);
  }
}
