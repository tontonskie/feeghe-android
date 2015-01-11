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
 * Created by tonton on 1/7/15.
 */
public class VerifyTask extends AsyncTask<Void, Void, ResponseObject> {

  private ProgressDialog preloader;
  private Context context;
  private Session session;
  private String paramCode;
  private String paramId;
  private Listener listener;

  public interface Listener extends TaskListener {
    public void onSuccess(String token, String userId);
  }

  public VerifyTask(Context context, String verificationId, String code, Listener listener) {
    paramCode = code;
    paramId = verificationId;
    preloader = new ProgressDialog(context);
    session = Session.getInstance(context);
    this.context = context;
    this.listener = listener;
  }

  public void onPreExecute() {
    preloader.setCancelable(false);
    preloader.setMessage("Please wait...");
    preloader.show();
  }

  @Override
  protected ResponseObject doInBackground(Void... params) {
    return new UserService(context).verify(paramId, paramCode);
  }

  public void onPostExecute(ResponseObject response) {
    if (!response.isOk()) {
      listener.onFail(response.getStatusCode(), "Invalid code");
      preloader.dismiss();
      return;
    }
    JSONObject result = response.getContent();
    try {
      session.setCredentials(result.getString("token"), result.getString("user"));
    } catch (JSONException e) {
      e.printStackTrace();
    }
    listener.onSuccess(session.getToken(), session.getUserId());
    preloader.dismiss();
  }
}
