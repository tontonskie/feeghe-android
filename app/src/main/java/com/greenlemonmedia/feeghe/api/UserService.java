package com.greenlemonmedia.feeghe.api;

import android.content.Context;

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

  public UserService(Context context) {
    super("user", context);
  }

  /**
   *
   * @param phoneNumber
   * @param password
   * @return
   */
  public ResponseObject login(String phoneNumber, String password) {
    HttpPost postRequest = new HttpPost(getBaseUrl("login"));
    JSONObject params = new JSONObject();
    try {
      params.put("password", password);
      params.put("phoneNumber", phoneNumber);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    setBodyParams(postRequest, params);
    return (ResponseObject) call(postRequest);
  }

  /**
   *
   * @param phoneNumber
   * @return
   */
  public ResponseObject register(String phoneNumber) {
    HttpPost postRequest = new HttpPost(getBaseUrl("register"));
    JSONObject params = new JSONObject();
    try {
      params.put("number", phoneNumber);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    setBodyParams(postRequest, params);
    return (ResponseObject) call(postRequest);
  }

  /**
   *
   * @param verificationId
   * @param code
   * @return
   */
  public ResponseObject verify(String verificationId, String code) {
    HttpPut putRequest = new HttpPut(getBaseUrl("verify/" + verificationId));
    JSONObject params = new JSONObject();
    try {
      params.put("code", code);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    setBodyParams(putRequest, params);
    return (ResponseObject) call(putRequest);
  }

  /**
   *
   * @return
   */
  public ResponseObject logout() {
    return (ResponseObject) apiCall(new HttpDelete(getBaseUrl("logout")));
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
}
