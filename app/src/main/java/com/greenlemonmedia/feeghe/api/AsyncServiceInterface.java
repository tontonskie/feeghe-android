package com.greenlemonmedia.feeghe.api;

import org.json.JSONObject;

/**
 * Created by tonton on 1/27/15.
 */
public interface AsyncServiceInterface {

  public void query(JSONObject query, APIService.QueryCallback callback);

  public void save(JSONObject data, APIService.SaveCallback callback);

  public void delete(String id, APIService.DeleteCallback callback);

  public void get(String id, APIService.GetCallback callback);

  public void update(String id, JSONObject data, APIService.UpdateCallback callback);
}
