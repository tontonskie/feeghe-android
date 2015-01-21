package com.greenlemonmedia.feeghe.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.greenlemonmedia.feeghe.api.ResponseArray;
import com.greenlemonmedia.feeghe.api.RoomService;
import com.greenlemonmedia.feeghe.api.Util;
import com.greenlemonmedia.feeghe.storage.Session;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by tonton on 1/20/15.
 */
public class GetRoomsTask extends AsyncTask<Void, Void, ResponseArray> {

  private Context context;
  private ProgressDialog preloader;
  private Session session;
  private Listener listener;

  public interface Listener extends TaskListener {
    public void onSuccess(ArrayList<JSONObject> rooms);
  }

  public GetRoomsTask(Context context, Listener listener) {
    this.context = context;
    this.listener = listener;
    session = Session.getInstance(context);
    preloader = new ProgressDialog(context);
  }

  public void onPreExecute() {
    preloader.setCancelable(false);
    preloader.setMessage("Please wait...");
    preloader.show();
  }

  @Override
  protected ResponseArray doInBackground(Void... params) {
    ResponseArray rooms = null;
    RoomService roomService = new RoomService(context);
    try {
      JSONObject notNull = new JSONObject();
      JSONObject checkUser = new JSONObject();
      notNull.put("!", JSONObject.NULL);
      checkUser.put("users." + session.getUserId(), notNull);
      JSONObject request = roomService.createWhereQuery(checkUser);
      request.put("sort", "updatedAt DESC");
      rooms = roomService.query(request);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return rooms;
  }

  public void onPostExecute(ResponseArray response) {
    if (!response.isOk()) {
      listener.onFail(response.getStatusCode(), response.getErrorMessage());
    } else {
      listener.onSuccess(Util.toList(response));
    }
    preloader.dismiss();
  }
}
