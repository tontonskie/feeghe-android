package com.greenlemonmedia.feeghe.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.R;
import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.CacheCollection;
import com.greenlemonmedia.feeghe.api.ContactService;
import com.greenlemonmedia.feeghe.api.ResponseArray;
import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.Util;
import com.greenlemonmedia.feeghe.storage.Session;
import com.greenlemonmedia.feeghe.tasks.GoToRoomTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ContactsFragment extends MainActivityFragment {

  private MainActivity context;
  private ListView listViewContacts;
  private ContactService contactService;
  private Session session;
  private CacheCollection contactCacheCollection;
  private ContactsAdapter contactsAdapter;
  private ProgressDialog contactsPreloader;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_contacts, container, false);
  }

  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    context = getCurrentActivity();
    session = Session.getInstance(context);
    contactService = new ContactService(context);
    listViewContacts = (ListView) context.findViewById(R.id.listViewContacts);

    JSONObject query = contactService.getCacheQuery();
    contactCacheCollection = contactService.getCacheCollection(query);
    final ResponseArray responseFromCache = contactCacheCollection.getContent();
    if (responseFromCache.getContent().length() != 0) {
      showContacts(responseFromCache);
    } else {
      contactsPreloader = ProgressDialog.show(context, null, "Please wait...", true, false);
    }

    contactService.query(query, new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        if (contactsAdapter == null) {
          showContacts(response);
          contactCacheCollection.save(response.getContent());
          contactsPreloader.dismiss();
        } else {
          JSONArray contactsFromServer = response.getContent();
          JSONArray contactsFromCache = responseFromCache.getContent();
          int contactsCacheLength = contactsFromCache.length();
          int contactsServerLength = contactsFromServer.length();
          try {
            for (int i = 0; i < contactsServerLength; i++) {
              boolean inCache = false;
              JSONObject contactFromServer = (JSONObject) contactsFromServer.getJSONObject(i);
              String roomId = contactFromServer.getString("id");
              for (int j = 0; j < contactsCacheLength; j++) {
                if (contactsFromCache.getJSONObject(j).getString("id").equals(roomId)) {
                  inCache = true;
                  break;
                }
              }
              if (!inCache) {
                contactCacheCollection.save(contactFromServer);
                contactsAdapter.add(contactFromServer);
              }
            }
          } catch (JSONException ex) {
            ex.printStackTrace();
          }
        }
      }

      @Override
      public void onFail(int statusCode, String error) {

      }
    });
  }

  public void showContacts(ResponseArray response) {
    contactsAdapter = new ContactsAdapter(Util.toList(response));
    listViewContacts.setAdapter(contactsAdapter);
  }

  @Override
  public String getTabId() {
    return MainActivity.TAB_CONTACTS;
  }

  private class ContactsAdapter extends ArrayAdapter<JSONObject> implements View.OnClickListener {

    public ContactsAdapter(ArrayList<JSONObject> contacts) {
      super(context, R.layout.contact_list_item, contacts);
    }

    @Override
    public void onClick(View v) {
      GoToRoomTask createRoom = new GoToRoomTask(
        context,
        (String) v.getTag(),
        new GoToRoomTask.Listener() {

          @Override
          public void onSuccess(ResponseObject response) {
            context.showRoomFragment(response.getContent());
          }

          @Override
          public void onFail(int statusCode, String error) {

          }
        }
      );
      createRoom.execute();
    }

    private class ContactViewHolder {
      TextView textView;
      Button btnShowChat;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
      ContactViewHolder viewHolder;
      if (convertView == null) {
        LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = vi.inflate(R.layout.contact_list_item, null);
        viewHolder = new ContactViewHolder();
        viewHolder.textView = (TextView) convertView.findViewById(R.id.txtViewListContactItem);
        viewHolder.btnShowChat = (Button) convertView.findViewById(R.id.btnShowChat);
        viewHolder.btnShowChat.setOnClickListener(this);
        convertView.setTag(viewHolder);
      } else {
        viewHolder = (ContactViewHolder) convertView.getTag();
      }
      JSONObject contact = getItem(position);
      try {
        JSONObject user = contact.getJSONObject("user");
        String userId = user.getString("id");
        String firstName = user.getString("firstName");
        String lastName = user.getString("lastName");
        if (!firstName.isEmpty() && !lastName.isEmpty() && !user.isNull("firstName") && !user.isNull("lastName")) {
          viewHolder.textView.setText(firstName + " " + lastName);
        } else {
          viewHolder.textView.setText(user.getString("phoneNumber"));
        }
        viewHolder.textView.setTag(userId);
        viewHolder.btnShowChat.setTag(userId);
      } catch (JSONException e) {
        e.printStackTrace();
      }
      return convertView;
    }
  }
}
