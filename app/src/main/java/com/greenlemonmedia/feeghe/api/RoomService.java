package com.greenlemonmedia.feeghe.api;

import android.app.Activity;

import org.apache.http.client.methods.HttpPut;
import org.json.JSONArray;
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
  public RoomService(Activity context) {
    super("room", context);
  }

  /**
   *
   * @param roomId
   * @param callback
   */
  public void visit(String roomId, UpdateCallback callback) {
    HttpPut putRequest = new HttpPut(getBaseUrl("visit"));
    JSONObject params = new JSONObject();
    try {
      params.put("id", roomId);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    setBodyParams(putRequest, params);
    apiAsyncCall(putRequest, callback);
  }

  /**
   *
   * @param roomId
   * @param newUsers
   * @param callback
   */
  public void addUsers(String roomId, JSONArray newUsers, SocketCallback callback) {
    JSONObject params = new JSONObject();
    try {
      params.put("users", newUsers);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    apiSocketCall("put", getBaseUri(roomId + "/user"), params, callback);
  }

  /**
   *
   * @param roomId
   * @param userId
   * @param callback
   */
  public void removeUser(String roomId, String userId, SocketCallback callback) {
    apiSocketCall("delete", getBaseUri(roomId + "/user/" + userId), new JSONObject(), callback);
  }

  @Override
  public JSONObject getCacheQuery() {
    JSONObject request = null;
    try {
      JSONObject notNull = new JSONObject();
      JSONObject checkUser = new JSONObject();
      notNull.put("!", JSONObject.NULL);
      checkUser.put("users." + session.getUserId(), notNull);
      request = createWhereQuery(checkUser);
      request.put("sort", "updatedAt DESC");
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return request;
  }
}
