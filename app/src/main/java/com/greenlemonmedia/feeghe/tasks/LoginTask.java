package com.greenlemonmedia.feeghe.tasks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;

import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.UserService;
import com.greenlemonmedia.feeghe.storage.Session;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tonton on 1/6/15.
 */
public class LoginTask extends AsyncTask<Void, Void, Void> {

  private ProgressDialog preloader;
  private String paramPhoneNumber;
  private String paramPassword;
  private Session session;
  private LoginListener listener;
  private Activity activity;

  public interface LoginListener {
    public void onSuccess(String token, String userId);
    public void onFail(int statusCode, String error);
  }

  public LoginTask(Activity context, String phoneNumber, String password, LoginListener loginListener) {
    paramPhoneNumber = phoneNumber;
    paramPassword = password;
    listener = loginListener;
    session = Session.getInstance(context);
    preloader = new ProgressDialog(context);
    activity = context;
  }

  public void onPreExecute() {
    preloader.setMessage("Please wait...");
    preloader.setCancelable(false);
    preloader.show();
  }

  @Override
  protected Void doInBackground(Void... params) {
    UserService userService = new UserService(activity);
    final ResponseObject response = userService.login(paramPhoneNumber, paramPassword);
    if (!response.isOk()) {
      activity.runOnUiThread(new Runnable() {

        @Override
        public void run() {
          listener.onFail(response.getStatusCode(), "Invalid number and password");
        }
      });
      return null;
    }
    if (response.getContent().has("token")) {
      try {
        JSONObject user = response.getContent();
        session.setCredentials(user.getString("token"), user.getString("user"));
        activity.runOnUiThread(new Runnable() {

          @Override
          public void run() {
            listener.onSuccess(session.getToken(), session.getUserId());
          }
        });
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  public void onPostExecute(Void unused) {
    preloader.dismiss();
  }
}
