package com.greenlemonmedia.feeghe.tasks;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;

import com.greenlemonmedia.feeghe.api.ResponseArray;
import com.greenlemonmedia.feeghe.api.UserService;
import com.greenlemonmedia.feeghe.api.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by tonton on 1/11/15.
 */
public class ReferContactsTask extends AsyncTask<Void, Void, ResponseArray> {

  private Context context;
  private Listener listener;

  public interface Listener extends TaskListener {
    public void onSuccess(ArrayList<JSONObject> contacts);
  }

  public ReferContactsTask(Context context, Listener listener) {
    this.context = context;
    this.listener = listener;
  }

  @Override
  protected ResponseArray doInBackground(Void... params) {
    Cursor contactsCursor = context.getContentResolver().query(
      ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
      null, null, null, null
    );
    String phoneNumber;
    JSONArray contactsToCheck = new JSONArray();
    int numberIndex = contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
    while (contactsCursor.moveToNext()) {
      phoneNumber = contactsCursor
        .getString(numberIndex)
        .replace("+", "")
        .replace("-", "")
        .replace(" ", "");
      contactsToCheck.put(phoneNumber);
    }
    contactsCursor.close();
    JSONObject userQuery = new JSONObject();
    try {
      userQuery.put("phoneNumber", contactsToCheck);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    UserService userService = new UserService(context);
    return userService.query(userService.createWhereQuery(userQuery));
  }

  public void onPostExecute(ResponseArray response) {
    if (!response.isOk()) {
      listener.onFail(response.getStatusCode(), response.getErrorMessage());
      return;
    }
    listener.onSuccess(Util.toList(response));
  }
}
