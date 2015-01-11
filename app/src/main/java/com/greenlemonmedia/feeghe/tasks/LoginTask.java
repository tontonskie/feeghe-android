package com.greenlemonmedia.feeghe.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.UserService;
import com.greenlemonmedia.feeghe.storage.Session;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tonton on 1/6/15.
 */
public class LoginTask extends AsyncTask<Void, Void, ResponseObject> {

  private ProgressDialog preloader;
  private String paramPhoneNumber;
  private String paramPassword;
  private Session session;
  private Listener listener;
  private Context context;

  public interface Listener extends TaskListener {
    public void onSuccess(String token, String userId);
  }

  public LoginTask(Context context, String phoneNumber, String password, Listener listener) {
    paramPhoneNumber = phoneNumber;
    paramPassword = password;
    session = Session.getInstance(context);
    preloader = new ProgressDialog(context);
    this.context = context;
    this.listener = listener;
  }

  public void onPreExecute() {
    preloader.setMessage("Please wait...");
    preloader.setCancelable(false);
    preloader.show();
  }

  @Override
  protected ResponseObject doInBackground(Void... params) {
    return new UserService(context).login(paramPhoneNumber, paramPassword);
  }

  public void onPostExecute(ResponseObject response) {
    if (!response.isOk()) {
      listener.onFail(response.getStatusCode(), "Invalid number and password");
      preloader.dismiss();
      return;
    }
    try {
      JSONObject user = response.getContent();
      session.setCredentials(user.getString("token"), user.getString("user"));
      listener.onSuccess(session.getToken(), session.getUserId());
      preloader.dismiss();
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }
}
