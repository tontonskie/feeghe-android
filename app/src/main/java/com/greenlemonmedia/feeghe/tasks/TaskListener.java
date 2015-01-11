package com.greenlemonmedia.feeghe.tasks;

/**
 * Created by tonton on 1/11/15.
 */
public interface TaskListener {

  public void onFail(int statusCode, String error);
}
