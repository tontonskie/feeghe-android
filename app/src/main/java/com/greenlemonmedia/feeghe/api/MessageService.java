package com.greenlemonmedia.feeghe.api;

import android.app.Activity;

import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Created by tonton on 1/14/15.
 */
public class MessageService extends APIService {

  /**
   *
   * @param context
   */
  public MessageService(Activity context) {
    super("message", context);
  }

  /**
   *
   * @param currentRoomId
   * @param typing
   */
  public void typing(String currentRoomId, Boolean typing) {
    JSONObject data = new JSONObject();
    try {
      data.put("typing", typing);
      data.put("room", currentRoomId);
      data.put("user", session.getUserId());
    } catch (JSONException e) {
      e.printStackTrace();
    }
    apiSocketCall("post", getBaseUri("typing"), data);
  }

  @Override
  public JSONObject getCacheQuery() {
    return null;
  }

  /**
   *
   * @param roomId
   * @return
   */
  public JSONObject getCacheQuery(String roomId) {
    JSONObject messageQuery = new JSONObject();
    try {
      messageQuery.put("room", roomId);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return createWhereQuery(messageQuery);
  }

  /**
   *
   * @param messageId
   * @param filename
   * @param path
   * @param callback
   */
  public void upload(String messageId, String filename, String path, UploadProgressListener progressListener, SaveCallback callback) {
    HttpPut httpPut = new HttpPut(getBaseUrl(messageId + "/upload"));
    FileBody fileUploadBody = new FileBody(new File(path));
    UploadEntity entity = new UploadEntity(fileUploadBody.getContentLength(), progressListener);
    try {
      entity.addPart("filename", new StringBody(filename));
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    entity.addPart("file", fileUploadBody);
    httpPut.setEntity(entity);
    enableAutoHeaders = false;
    apiAsyncCall(httpPut, callback);
    enableAutoHeaders = true;
  }
}
