package com.greenlemonmedia.feeghe.api;

import org.json.JSONObject;

/**
 * Created by tonton on 1/27/15.
 */
public interface ServiceInterface {

  public ResponseArray query(JSONObject query);

  public ResponseObject save(JSONObject data);

  public ResponseObject delete(String id);

  public ResponseObject get(String id);

  public ResponseObject update(String id, JSONObject data);
}
