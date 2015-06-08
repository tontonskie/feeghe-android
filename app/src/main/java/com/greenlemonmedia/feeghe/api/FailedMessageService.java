package com.greenlemonmedia.feeghe.api;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tontonskie on 5/29/15.
 */
public class FailedMessageService extends APIService {

  /**
   *
   * @param context
   */
  public FailedMessageService(Context context) {
    super("failedMessage", context);
  }

  @Override
  public JSONObject getCacheQuery() {
    return null;
  }

  /**
   *
   * @param roomId
   * @return
   */
  public JSONObject getCacheQuery(String roomId) {
    JSONObject messageQuery = new JSONObject();
    try {
      messageQuery.put("room", roomId);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return createWhereQuery(messageQuery);
  }
}
