package com.greenlemonmedia.feeghe.tasks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.UserService;
import com.greenlemonmedia.feeghe.storage.Session;

import org.json.JSONException;

/**
 * Created by tonton on 1/7/15.
 */
public class RegisterTask extends AsyncTask<Void, Void, Void> {

  private String paramPhoneNumber;
  private ProgressDialog preloader;
  private RegisterListener listener;
  private Session session;
  private Activity activity;

  public interface RegisterListener {
    public void onSuccess(String verificationId);
    public void onFail(int statusCode, String error);
  }

  public RegisterTask(Activity context, String phoneNumber, RegisterListener registerListener) {
    paramPhoneNumber = phoneNumber;
    listener = registerListener;
    preloader = new ProgressDialog(context);
    session = Session.getInstance(context);
    activity = context;
  }

  public void onPreExecute() {
    preloader.setCancelable(false);
    preloader.setMessage("Please wait...");
    preloader.show();
  }

  @Override
  protected Void doInBackground(Void... params) {
    UserService userService = new UserService(session);
    final ResponseObject registerResult = userService.register(paramPhoneNumber);
    if (!registerResult.isOk()) {
      activity.runOnUiThread(new Runnable() {

        @Override
        public void run() {
          listener.onFail(registerResult.getStatusCode(), registerResult.getErrorMessage());
        }
      });
      return null;
    }
    if (registerResult.getContent().has("id")) {
      activity.runOnUiThread(new Runnable() {

        @Override
        public void run() {
          try {
            listener.onSuccess(registerResult.getContent().getString("id"));
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }
      });
    }
    return null;
  }

  public void onPostExecute(Void unused) {
    preloader.dismiss();
  }
}
