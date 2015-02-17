package com.greenlemonmedia.feeghe.api;

import android.content.Context;
import android.telephony.TelephonyManager;

import com.greenlemonmedia.feeghe.storage.Session;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tonton on 1/5/15.
 */
public class UserService extends APIService {

  /**
   *
   * @param context
   */
  public UserService(Context context) {
    super("user", context);
  }

  /**
   *
   * @param phoneNumber
   * @param password
   * @param callback
   */
  public void login(String phoneNumber, String password, GetCallback callback) {
    HttpPost postRequest = new HttpPost(getBaseUrl("login"));
    JSONObject params = new JSONObject();
    try {
      params.put("password", password);
      params.put("phoneNumber", phoneNumber);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    setBodyParams(postRequest, params);
    asynCall(postRequest, callback);
  }

  /**
   *
   * @param phoneNumber
   * @param callback
   */
  public void register(String phoneNumber, SaveCallback callback) {
    HttpPost postRequest = new HttpPost(getBaseUrl("register"));
    JSONObject params = new JSONObject();
    TelephonyManager tel = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    try {
      params.put("number", phoneNumber);
      params.put("platform", "android");
      params.put("deviceId", tel.getDeviceId());
    } catch (JSONException e) {
      e.printStackTrace();
    }
    setBodyParams(postRequest, params);
    apiAsyncCall(postRequest, callback);
  }

  /**
   *
   * @param verificationId
   * @param code
   * @param callback
   */
  public void verify(String verificationId, String code, UpdateCallback callback) {
    HttpPut putRequest = new HttpPut(getBaseUrl("verify/" + verificationId));
    JSONObject params = new JSONObject();
    try {
      params.put("code", code);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    setBodyParams(putRequest, params);
    asynCall(putRequest, callback);
  }

  /**
   *
   * @param callback
   */
  public void logout(DeleteCallback callback) {
    apiAsyncCall(new HttpDelete(getBaseUrl("logout")), callback);
  }

  public void updateCurrentUser() {
    session.setCurrentUser(get(session.getUserId()));
  }

  /**
   *
   * @return
   */
  public Session.User getCurrentUser() {
    Session.User currentUser = session.getCurrentUser();
    if (currentUser == null) {
      updateCurrentUser();
      currentUser = session.getCurrentUser();
    }
    return currentUser;
  }

  @Override
  public JSONObject getCacheQuery() {
    return null;
  }
}
