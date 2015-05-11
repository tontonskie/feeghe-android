package com.greenlemonmedia.feeghe.tasks;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.DeviceService;
import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.storage.Session;

import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by tontonskie on 5/11/15.
 */
public class RegisterIDTask extends AsyncTask<Void, Void, String> {

  private GoogleCloudMessaging gcm;
  private Session session;
  private DeviceService deviceService;
  private Activity context;

  public static final String SENDER_ID = "997031270102";

  public RegisterIDTask(Activity context) {
    this.context = context;
    gcm = GoogleCloudMessaging.getInstance(context);
    session = Session.getInstance(context);
    deviceService = new DeviceService(context);
  }

  @Override
  protected void onPreExecute() {

  }

  @Override
  protected String doInBackground(Void... params) {
    String regId = null;
    try {
      regId = gcm.register(SENDER_ID);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return regId;
  }

  @Override
  protected void onPostExecute(String result) {
    if (result != null) {
      session.set(Session.REG_ID, result);
      JSONObject params = new JSONObject();
      try {
        params.put("deviceId", result);
        params.put("platform", "android");
        params.put("user", session.getUserId());
      } catch (Exception e) {
        e.printStackTrace();
      }
      deviceService.save(params, new APIService.SaveCallback() {

        @Override
        public void onSuccess(ResponseObject response) {

        }

        @Override
        public void onFail(int statusCode, String error, JSONObject validationError) {
          Toast.makeText(context, error, Toast.LENGTH_LONG).show();
        }
      });
    }
  }
}
