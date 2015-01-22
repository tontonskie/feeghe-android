package com.greenlemonmedia.feeghe.api;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tonton on 1/14/15.
 */
public class MessageService extends APIService {

  /**
   *
   * @param context
   */
  public MessageService(Context context) {
    super("message", context);
  }

  /**
   *
   * @param currentRoomId
   * @param typing
   */
  public void typing(String currentRoomId, Boolean typing) {
    JSONObject data = new JSONObject();
    try {
      data.put("typing", typing);
      data.put("room", currentRoomId);
      data.put("user", session.getUserId());
    } catch (JSONException e) {
      e.printStackTrace();
    }
    apiSocketCall("post", getBaseUri("typing"), data);
  }
}
