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
 * Created by tonton on 1/9/15.
 */
public class NewUserTask extends AsyncTask<Void, Void, ResponseObject> {

  private Context context;
  private ProgressDialog preloader;
  private Listener listener;
  private String paramPassword;
  private String paramGender;
  private Session session;

  public interface Listener extends TaskListener {
    public void onSuccess(ResponseObject updatedUser);
  }

  public NewUserTask(Context context, String gender, String password, Listener listener) {
    this.context = context;
    this.listener = listener;
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
  protected ResponseObject doInBackground(Void... params) {
    UserService userService = new UserService(context);
    JSONObject data = new JSONObject();
    try {
      data.put("gender", paramGender);
      data.put("newPassword", paramPassword);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return userService.update(session.getUserId(), data);
  }

  public void onPostExecute(ResponseObject updatedUser) {;
    session.getCurrentUser().update(updatedUser);
    if (!updatedUser.isOk()) {
      listener.onFail(updatedUser.getStatusCode(), updatedUser.getErrorMessage());
      preloader.dismiss();
      return;
    }
    listener.onSuccess(updatedUser);
    preloader.dismiss();
  }
}
