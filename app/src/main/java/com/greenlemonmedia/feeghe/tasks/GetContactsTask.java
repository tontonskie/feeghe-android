package com.greenlemonmedia.feeghe.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.greenlemonmedia.feeghe.api.Util;
import com.greenlemonmedia.feeghe.api.ContactService;
import com.greenlemonmedia.feeghe.api.ResponseArray;
import com.greenlemonmedia.feeghe.storage.Session;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by tonton on 1/13/15.
 */
public class GetContactsTask extends AsyncTask<Void, Void, ResponseArray> {

  private Context context;
  private ProgressDialog preloader;
  private Listener listener;
  private Session session;

  public interface Listener extends TaskListener {
    public void onSuccess(ArrayList<JSONObject> contacts);
  }

  public GetContactsTask(Context context, Listener listener) {
    this.context = context;
    this.listener = listener;
    session = Session.getInstance(context);
    preloader = new ProgressDialog(context);
  }

  public void onPreExecute() {
    preloader.setCancelable(false);
    preloader.setMessage("Please wait...");
    preloader.show();
  }

  @Override
  protected ResponseArray doInBackground(Void... params) {
    JSONObject query = new JSONObject();
    try {
      query.put("owner", session.getUserId());
    } catch (JSONException e) {
      e.printStackTrace();
    }
    ContactService contactService = new ContactService(context);
    return contactService.query(contactService.createWhereQuery(query));
  }

  public void onPostExecute(ResponseArray contacts) {
    if (!contacts.isOk()) {
      listener.onFail(contacts.getStatusCode(), contacts.getErrorMessage());
      return;
    }
    listener.onSuccess(Util.toList(contacts));
    preloader.dismiss();
  }
}
