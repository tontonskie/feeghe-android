package com.greenlemonmedia.feeghe.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.Socket;
import com.greenlemonmedia.feeghe.api.UserService;
import com.greenlemonmedia.feeghe.storage.Session;

import org.json.JSONException;

/**
 * Created by tonton on 1/7/15.
 */
public class LogoutTask extends AsyncTask<Void, Void, ResponseObject> {

  private ProgressDialog preloader;
  private Listener listener;
  private Session session;
  private Context context;

  public interface Listener extends TaskListener {
    public void onSuccess();
  }

  public LogoutTask(Context context, Listener listener) {
    preloader = new ProgressDialog(context);
    session = Session.getInstance(context);
    this.listener = listener;
    this.context = context;
  }

  public void onPreExecute() {
    preloader.setCancelable(false);
    preloader.setMessage("Please wait...");
    preloader.show();
  }

  @Override
  protected ResponseObject doInBackground(Void... params) {
    return new UserService(context).logout();
  }

  public void onPostExecute(ResponseObject response) {
    try {
      if (response.isOk() && response.getContent().getBoolean("success")) {
        session.setLoggedIn(false);
        Socket.disconnect();
        listener.onSuccess();
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
    preloader.dismiss();
  }
}
