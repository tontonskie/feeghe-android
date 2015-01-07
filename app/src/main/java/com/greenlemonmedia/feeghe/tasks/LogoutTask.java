package com.greenlemonmedia.feeghe.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.UserService;
import com.greenlemonmedia.feeghe.storage.Session;

import org.json.JSONException;

/**
 * Created by tonton on 1/7/15.
 */
public class LogoutTask extends AsyncTask<Void, Void, Void> {

  private ProgressDialog preloader;
  private LogoutListener listener;
  private Session session;

  public interface LogoutListener {
    public void onSuccess();
  }

  public LogoutTask(Context context, LogoutListener logoutListener) {
    preloader = new ProgressDialog(context);
    session = Session.getInstance(context);
    listener = logoutListener;
  }

  public void onPreExecute() {
    preloader.setCancelable(false);
    preloader.setMessage("Please wait...");
    preloader.show();
  }

  @Override
  protected Void doInBackground(Void... params) {
    UserService userService = new UserService(session);
    ResponseObject response = userService.logout();
    try {
      if (response.isOk() && response.getContent().getBoolean("success")) {
        session.setLoggedIn(false);
        listener.onSuccess();
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return null;
  }

  public void onPostExecute(Void unused) {
    preloader.dismiss();
  }
}
