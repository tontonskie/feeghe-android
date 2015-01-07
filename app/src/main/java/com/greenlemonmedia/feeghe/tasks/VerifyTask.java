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
 * Created by tonton on 1/7/15.
 */
public class VerifyTask extends AsyncTask<Void, Void, Void> {

  private ProgressDialog preloader;
  private Activity activity;
  private Session session;
  private String paramCode;
  private String paramId;
  private VerifyListener listener;

  public interface VerifyListener {
    public void onSuccess(String token, String userId);
    public void onFail(int statusCode, String error);
  }

  public VerifyTask(Activity context, String verificationId, String code, VerifyListener verifyListener) {
    paramCode = code;
    paramId = verificationId;
    preloader = new ProgressDialog(context);
    session = Session.getInstance(context);
    activity = context;
    listener = verifyListener;
  }

  public void onPreExecute() {
    preloader.setCancelable(false);
    preloader.setMessage("Please wait...");
    preloader.show();
  }

  @Override
  protected Void doInBackground(Void... params) {
    UserService userService = new UserService(session);
    final ResponseObject response = userService.verify(paramId, paramCode);
    if (!response.isOk()) {
      activity.runOnUiThread(new Runnable() {

        @Override
        public void run() {
          listener.onFail(response.getStatusCode(), "Invalid code");
        }
      });
      return null;
    }
    if (response.getContent().has("token")) {
      activity.runOnUiThread(new Runnable() {

        @Override
        public void run() {
          JSONObject result = response.getContent();
          try {
            session.setCredentials(result.getString("token"), result.getString("user"));
          } catch (JSONException e) {
            e.printStackTrace();
          }
          listener.onSuccess(session.getToken(), session.getUserId());
        }
      });
    }
    return null;
  }

  public void onPostExecute(Void unused) {
    preloader.dismiss();
  }
}
