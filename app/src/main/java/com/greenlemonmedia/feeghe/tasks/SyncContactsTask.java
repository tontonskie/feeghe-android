package com.greenlemonmedia.feeghe.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.greenlemonmedia.feeghe.api.ContactService;
import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.storage.Session;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tonton on 1/11/15.
 */
public class SyncContactsTask extends AsyncTask<Void, Void, ResponseObject> {

  private Context context;
  private JSONArray paramIds;
  private Listener listener;
  private ProgressDialog preloader;
  private Session session;

  public interface Listener extends TaskListener {
    public void onSuccess(ResponseObject response);
  }

  public SyncContactsTask(Context activity, JSONArray contactIds, Listener syncContactsListener) {
    context = activity;
    listener = syncContactsListener;
    paramIds = contactIds;
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
    return new ContactService(context).sync(paramIds);
  }

  public void onPostExecute(ResponseObject response) {
    if (!response.isOk()) {
      listener.onFail(response.getStatusCode(), response.getErrorMessage());
    } else {
      JSONObject userUpdate = new JSONObject();
      try {
        userUpdate.put("contactsCount", response.getContent().getInt("contactsCount"));
      } catch (JSONException e) {
        e.printStackTrace();
      }
      session.getCurrentUser().update(userUpdate);
      listener.onSuccess(response);
    }
    preloader.dismiss();
  }
}
