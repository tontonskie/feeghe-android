package com.greenlemonmedia.feeghe.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.ResponseArray;
import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.RoomService;
import com.greenlemonmedia.feeghe.storage.Session;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tonton on 1/14/15.
 */
public class GoToRoomTask extends AsyncTask<Void, Void, ResponseArray> {

  private ProgressDialog preloader;
  private Session session;
  private String paramChatMateId;
  private Listener listener;
  private RoomService roomService;

  public interface Listener extends TaskListener {
    public void onSuccess(ResponseObject response);
  }

  public GoToRoomTask(Context context, String chatMateId, Listener listener) {
    this.listener = listener;
    preloader = new ProgressDialog(context);
    session = Session.getInstance(context);
    paramChatMateId = chatMateId;
    roomService = new RoomService(context);
  }

  public void onPreExecute() {
    preloader.setCancelable(false);
    preloader.setMessage("Please wait...");
    preloader.show();
  }

  @Override
  protected ResponseArray doInBackground(Void... params) {
    ResponseArray checkResponse = null;
    JSONObject userIsNotNull = new JSONObject();
    JSONObject checkRoomQuery = new JSONObject();
    try {
      userIsNotNull.put("!", JSONObject.NULL);
      checkRoomQuery.put("users." + paramChatMateId, userIsNotNull);
      checkRoomQuery.put("users." + session.getUserId(), userIsNotNull);
      checkRoomQuery.put("isGroup", false);
      checkRoomQuery = roomService.createWhereQuery(checkRoomQuery);
      checkRoomQuery.put("limit", 1);
      checkResponse = roomService.query(checkRoomQuery);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return checkResponse;
  }

  public void onPostExecute(ResponseArray checkResponse) {
    try {
      if (checkResponse.isOk()) {
        if (checkResponse.getContent().length() == 0) {
          JSONObject saveData = new JSONObject();
          saveData.put("creator", session.getUserId());
          JSONArray chatMates = new JSONArray();
          chatMates.put(paramChatMateId);
          chatMates.put(session.getUserId());
          saveData.put("users", chatMates);
          roomService.socketSave(saveData, new APIService.Callback() {

            @Override
            public void onSuccess(ResponseObject response) {
              listener.onSuccess(response);
              preloader.dismiss();
            }

            @Override
            public void onFail(int statusCode, String error) {
              listener.onFail(statusCode, error);
            }
          });
        } else {
          listener.onSuccess(new ResponseObject(
            checkResponse.getStatusCode(),
            checkResponse.getContent().getJSONObject(0)
          ));
          preloader.dismiss();
        }
      } else {
        listener.onFail(checkResponse.getStatusCode(), checkResponse.getErrorMessage());
      }
    } catch (JSONException ex) {
      ex.printStackTrace();
    }
  }
}
