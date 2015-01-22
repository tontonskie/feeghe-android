package com.greenlemonmedia.feeghe.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.RoomService;

/**
 * Created by tonton on 1/22/15.
 */
public class ReadNewMessageTask extends AsyncTask<Void, Void, ResponseObject> {

  private Context context;
  private Listener listener;
  private String paramRoomId;

  public interface Listener extends TaskListener {
    public void onSuccess(ResponseObject response);
  }

  public ReadNewMessageTask(Context context, String roomId, Listener listener) {
    this.context = context;
    this.listener = listener;
    paramRoomId = roomId;
  }

  @Override
  protected ResponseObject doInBackground(Void... params) {
    return new RoomService(context).visit(paramRoomId);
  }

  public void onPostExecute(ResponseObject response) {
    if (response.isOk()) {
      listener.onSuccess(response);
    } else {
      listener.onFail(response.getStatusCode(), response.getErrorMessage());
    }
  }
}
