package com.greenlemonmedia.feeghe.api;

import org.json.JSONObject;

/**
 * Created by tonton on 1/5/15.
 */
public class ResponseObject extends Response {

  private JSONObject content;

  /**
   *
   * @param statusCode
   * @param data
   */
  public ResponseObject(int statusCode, JSONObject data) {
    super(statusCode);
    content = data;
  }

  /**
   *
   * @param statusCode
   * @param error
   */
  public ResponseObject(int statusCode, String error) {
    super(statusCode, error);
    content = new JSONObject();
  }

  /**
   *
   * @param data
   */
  public ResponseObject(JSONObject data) {
    super();
    content = data;
  }

  /**
   *
   * @return
   */
  public JSONObject getContent() {
    return content;
  }
}
