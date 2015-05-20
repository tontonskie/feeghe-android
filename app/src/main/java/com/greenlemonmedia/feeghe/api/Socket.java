package com.greenlemonmedia.feeghe.api;

import android.app.Activity;

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
public class Socket {

  private static Socket instance;

  public Activity context;
  public Session session;
  public SocketIOClient client;

  public interface SocketConnectionListener {
    void onStartConnecting(SocketIORequest request);
    void onConnect(SocketIOClient client);
  }

  /**
   *
   * @param context
   */
  private Socket(Activity context) {
    this.context = context;
    this.session = Session.getInstance(context);
  }


  /**
   *
   * @return
   */
  public static boolean isConnected() {
    return instance != null && (instance.client != null && instance.client.isConnected());
  }

  /**
   *
   * @return
   */
  public static SocketIOClient getClient() {
    if (!isConnected()) {
      return null;
    }
    return instance.client;
  }

  /**
   *
   * @param context
   * @param connectionListener
   */
  public static void connect(Activity context, final SocketConnectionListener connectionListener) {
    instance = new Socket(context);
    String qstring = "token=" + instance.session.getToken() + "&user=" + instance.session.getUserId();
    qstring += "&__sails_io_sdk_version=0.10.0&__sails_io_sdk_platform=mobile&__sails_io_sdk_language=java";
    qstring += "&t=" + new Date().getTime();

    String wsUrl = APIService.HTTP_SCHEME + "://" + APIService.HOST;
    if (!APIService.PORT.isEmpty()) {
      wsUrl += ':' + APIService.PORT;
    }
    SocketIORequest socketRequest = new SocketIORequest(wsUrl, "", qstring);
    connectionListener.onStartConnecting(socketRequest);
    SocketIOClient.connect(AsyncHttpClient.getDefaultInstance(), socketRequest, new ConnectCallback() {

      @Override
      public void onConnectCompleted(Exception ex, SocketIOClient socketClient) {
        if (ex != null) {
          ex.printStackTrace();
          return;
        }
        instance.client = socketClient;
        connectionListener.onConnect(instance.client);
      }
    });
  }

  public static void disconnect() {
    if (!isConnected()) return;
    instance.client.disconnect();
    instance.client = null;
  }

  /**
   *
   * @param event
   * @param callback
   */
  public static void on(String event, final APIService.EventCallback callback) {
    if (!isConnected()) return;
    instance.client.on(event, new EventCallback() {

      @Override
      public void onEvent(final JSONArray argument, Acknowledge acknowledge) {
        instance.context.runOnUiThread(new Runnable() {

          @Override
          public void run() {
            try {
              callback.onEvent(argument.getJSONObject(0));
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }
        });
      }
    });
  }
}
