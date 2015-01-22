package com.greenlemonmedia.feeghe.api;

import com.greenlemonmedia.feeghe.storage.Session;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.socketio.Acknowledge;
import com.koushikdutta.async.http.socketio.ConnectCallback;
import com.koushikdutta.async.http.socketio.EventCallback;
import com.koushikdutta.async.http.socketio.SocketIOClient;
import com.koushikdutta.async.http.socketio.SocketIORequest;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Date;

/**
 * Created by tonton on 1/8/15.
 */
abstract public class Socket {

  private static SocketIOClient client;

  public interface SocketConnectionListener {
    public void onStartConnecting(SocketIORequest request);
    public void onConnect(SocketIOClient client);
  }

  /**
   *
   * @return
   */
  public static boolean isConnected() {
    return client != null && client.isConnected();
  }

  /**
   *
   * @return
   */
  public static SocketIOClient getClient() {
    return client;
  }

  /**
   *
   * @param session
   * @param connectionListener
   */
  public static void connect(Session session, SocketConnectionListener connectionListener) {
    if (isConnected()) return;
    final SocketConnectionListener listener = connectionListener;
    String qstring = "token=" + session.getToken() + "&user=" + session.getUserId();
    qstring += "&__sails_io_sdk_version=0.10.0&__sails_io_sdk_platform=mobile&__sails_io_sdk_language=java";
    qstring += "&t=" + new Date().getTime();

    String wsUrl = APIService.HTTP_SCHEME + "://" + APIService.HOST;
    if (APIService.PORT != null) {
      wsUrl += ':' + APIService.PORT;
    }
    SocketIORequest socketRequest = new SocketIORequest(wsUrl, "", qstring);
    listener.onStartConnecting(socketRequest);
    SocketIOClient.connect(AsyncHttpClient.getDefaultInstance(), socketRequest, new ConnectCallback() {

      @Override
      public void onConnectCompleted(Exception ex, SocketIOClient socketClient) {
        if (ex != null) {
          ex.printStackTrace();
          return;
        }
        client = socketClient;
        listener.onConnect(client);
      }
    });
  }

  public static void disconnect() {
    if (!isConnected()) return;
    client.disconnect();
    client = null;
  }

  /**
   *
   * @param event
   * @param callback
   */
  public static void on(String event, final APIService.EventCallback callback) {
    client.on(event, new EventCallback() {

      @Override
      public void onEvent(JSONArray argument, Acknowledge acknowledge) {
        try {
          callback.onEvent(argument.getJSONObject(0));
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    });
  }
}
