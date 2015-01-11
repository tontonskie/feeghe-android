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
public class RegisterTask extends AsyncTask<Void, Void, ResponseObject> {

  private String paramPhoneNumber;
  private ProgressDialog preloader;
  private Listener listener;
  private Session session;
  private Context context;

  public interface Listener extends TaskListener {
    public void onSuccess(String verificationId);
  }

  public RegisterTask(Context context, String phoneNumber, Listener listener) {
    paramPhoneNumber = phoneNumber;
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
    return new UserService(context).register(paramPhoneNumber);
  }

  public void onPostExecute(ResponseObject registerResult) {
    if (!registerResult.isOk()) {
      listener.onFail(registerResult.getStatusCode(), registerResult.getErrorMessage());
      preloader.dismiss();
      return;
    }
    try {
      listener.onSuccess(registerResult.getContent().getString("id"));
    } catch (JSONException e) {
      e.printStackTrace();
    }
    preloader.dismiss();
  }
}
