package com.greenlemonmedia.feeghe.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.APIUtils;
import com.greenlemonmedia.feeghe.api.FailedMessageService;
import com.greenlemonmedia.feeghe.api.MessageService;
import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.Socket;
import com.koushikdutta.async.http.socketio.SocketIOClient;
import com.koushikdutta.async.http.socketio.SocketIORequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tontonskie on 5/26/15.
 */
public class NetworkChangeReceiver extends BroadcastReceiver {

  private void sendFailedMessages(Context context) {
    final FailedMessageService failedMsgService = new FailedMessageService(context);
    final MessageService messageService = new MessageService(context);
    JSONArray msgs = failedMsgService.getAllCache().getData().getContent();
    try {
      for (int i = 0; i < msgs.length(); i++) {
        final JSONObject newMessage = msgs.getJSONObject(i);
        final String tmpId = newMessage.getString("id");
        newMessage.put("user", newMessage.getJSONObject("user").getString("id"));
        newMessage.remove("id");
        newMessage.remove("timestamp");
        messageService.socketSave(newMessage, new APIService.SocketCallback() {

          @Override
          public void onSuccess(ResponseObject response) {
            try {
              JSONObject cacheQuery = messageService.getCacheQuery(newMessage.getString("room"));
              messageService.getCacheCollection(cacheQuery).save(response.getContent());
              failedMsgService.getCacheCollection(cacheQuery).delete(tmpId);
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }

          @Override
          public void onFail(int statusCode, String error, JSONObject validationError) {

          }
        });
      }
    } catch (JSONException ex) {
      ex.printStackTrace();
    }
  }

  @Override
  public void onReceive(final Context context, Intent intent) {
    if (!APIUtils.isConnected(context)) {
      return;
    }
    if (!Socket.isConnected()) {
      Socket.connect(context, new Socket.SocketConnectionListener() {

        @Override
        public void onStartConnecting(SocketIORequest request) {

        }

        @Override
        public void onConnect(SocketIOClient client) {
          sendFailedMessages(context);
        }
      });
      return;
    }
    sendFailedMessages(context);
  }
}
