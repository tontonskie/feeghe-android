package com.greenlemonmedia.feeghe.api;

import com.greenlemonmedia.feeghe.storage.Session;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tonton on 1/5/15.
 */
public class UserService extends APIService {

  public UserService(Session session) {
    super("user", session);
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
   * @return
   */
  public ResponseObject logout() {
    return (ResponseObject) apiCall(new HttpDelete(getBaseUrl("logout")));
  }
}
