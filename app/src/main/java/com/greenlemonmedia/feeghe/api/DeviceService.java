package com.greenlemonmedia.feeghe.api;

import android.app.Activity;

import org.json.JSONObject;

/**
 * Created by tontonskie on 5/11/15.
 */
public class DeviceService extends APIService {

  /**
   *
   * @param context
   */
  public DeviceService(Activity context) {
    super("device", context);
  }

  @Override
  public JSONObject getCacheQuery() {
    return null;
  }
}
