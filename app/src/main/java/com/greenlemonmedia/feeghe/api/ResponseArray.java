package com.greenlemonmedia.feeghe.api;

import org.json.JSONArray;

/**
 * Created by tonton on 1/5/15.
 */
public class ResponseArray extends Response {

  private JSONArray content;

  /**
   *
   * @param statusCode
   * @param data
   */
  public ResponseArray(int statusCode, JSONArray data) {
    super(statusCode);
    content = data;
  }

  /**
   *
   * @param statusCode
   * @param error
   */
  public ResponseArray(int statusCode, String error) {
    super(statusCode, error);
    content = new JSONArray();
  }

  /**
   *
   * @param data
   */
  public ResponseArray(JSONArray data) {
    super();
    content = data;
  }

  /**
   *
   * @return
   */
  public JSONArray getContent() {
    return content;
  }
}
