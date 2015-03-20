package com.greenlemonmedia.feeghe.api;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by GreenLemon on 3/20/15.
 */
public class FaceCommentService extends APIService {

  private String faceId;

  /**
   *
   * @param context
   * @param faceId
   */
  public FaceCommentService(Context context, String faceId) {
    super("faceComments", context);
    setBasePath("face/" + faceId + "/comments");
    this.faceId = faceId;
  }

  @Override
  public JSONObject getCacheQuery() {
    JSONObject cacheQuery = new JSONObject();
    try {
      cacheQuery.put("face", faceId);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return cacheQuery;
  }
}
