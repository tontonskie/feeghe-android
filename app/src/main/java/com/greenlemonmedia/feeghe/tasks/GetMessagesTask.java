package com.greenlemonmedia.feeghe.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.greenlemonmedia.feeghe.api.MessageService;
import com.greenlemonmedia.feeghe.api.ResponseArray;
import com.greenlemonmedia.feeghe.api.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by tonton on 1/14/15.
 */
public class GetMessagesTask extends AsyncTask<Void, Void, ResponseArray> {

  private Context context;
  private String paramRoomId;
  private Listener listener;

  public interface Listener extends TaskListener {
    public void onSuccess(ArrayList<JSONObject> messages);
  }

  public GetMessagesTask(Context context, String roomId, Listener listener) {
    this.context = context;
    this.listener = listener;
    paramRoomId = roomId;
  }

  @Override
  protected ResponseArray doInBackground(Void... params) {
    ResponseArray messages = null;
    JSONObject query = new JSONObject();
    try {
      query.put("room", paramRoomId);
      MessageService messageService = new MessageService(context);
      messages = messageService.query(messageService.createWhereQuery(query));
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return messages;
  }

  public void onPostExecute(ResponseArray messages) {
    if (!messages.isOk()) {
      listener.onFail(messages.getStatusCode(), messages.getErrorMessage());
    } else {
      listener.onSuccess(Util.toList(messages));
    }
  }
}
