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
 * Created by tonton on 1/9/15.
 */
public class NewUserTask extends AsyncTask<Void, Void, Void> {

  private Activity activity;
  private ProgressDialog preloader;
  private NewUserListener listener;
  private String paramPassword;
  private String paramGender;
  private Session session;

  public interface NewUserListener {
    public void onSuccess(ResponseObject updatedUser);
    public void onFail(int statusCode, String error);
  }

  public NewUserTask(Activity context, String gender, String password, NewUserListener newUserListener) {
    activity = context;
    listener = newUserListener;
    paramGender = gender.toLowerCase();
    paramPassword = password;
    session = Session.getInstance(context);
    preloader = new ProgressDialog(context);
  }

  public void onPreExecute() {
    preloader.setCancelable(false);
    preloader.setMessage("Please wait...");
    preloader.show();
  }

  @Override
  protected Void doInBackground(Void... params) {
    UserService userService = new UserService(activity);
    JSONObject data = new JSONObject();
    try {
      data.put("gender", paramGender);
      data.put("newPassword", paramPassword);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    final ResponseObject updatedUser = userService.update(session.getUserId(), data);
    session.getCurrentUser().update(updatedUser);
    if (!updatedUser.isOk()) {
      activity.runOnUiThread(new Runnable() {

        @Override
        public void run() {
          listener.onFail(updatedUser.getStatusCode(), updatedUser.getErrorMessage());
        }
      });
      return null;
    }
    activity.runOnUiThread(new Runnable() {

      @Override
      public void run() {
        listener.onSuccess(updatedUser);
      }
    });
    return null;
  }

  public void onPostExecute(Void unused) {
    preloader.dismiss();
  }
}
