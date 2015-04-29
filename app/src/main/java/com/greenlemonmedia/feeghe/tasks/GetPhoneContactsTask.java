package com.greenlemonmedia.feeghe.tasks;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;

import com.greenlemonmedia.feeghe.storage.Session;

import org.json.JSONArray;

/**
 * Created by tontonskie on 4/29/15.
 */
public class GetPhoneContactsTask extends AsyncTask<Void, Void, JSONArray> {

  private Context context;
  private GetPhoneContactsListener listener;
  private Session session;

  public interface GetPhoneContactsListener {
    public void onSuccess(JSONArray contacts);
  }

  public GetPhoneContactsTask(Context context, GetPhoneContactsListener listener) {
    this.context = context;
    this.listener = listener;
    this.session = Session.getInstance(context);
  }

  @Override
  protected JSONArray doInBackground(Void... params) {
    Cursor contactsCursor = context.getContentResolver().query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        null, null, null, null
    );
    JSONArray phoneContacts = new JSONArray();
    int numberIndex = contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
    while (contactsCursor.moveToNext()) {
      String phoneNumber = contactsCursor
        .getString(numberIndex)
        .replace("-", "")
        .replace(" ", "");
      if (phoneNumber.indexOf("+") != 0){
        if (phoneNumber.indexOf("0") == 0) {
          phoneNumber = phoneNumber.substring(1);
        }
        phoneNumber = session.get(Session.DIAL_CODE) + phoneNumber;
      }
      phoneNumber = phoneNumber.replace("+", "");
      phoneContacts.put(phoneNumber);
    }
    contactsCursor.close();
    return phoneContacts;
  }

  @Override
  protected void onPostExecute(JSONArray contacts) {
    listener.onSuccess(contacts);
  }
}
