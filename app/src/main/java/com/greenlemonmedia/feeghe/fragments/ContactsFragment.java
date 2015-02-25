package com.greenlemonmedia.feeghe.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.HashMap;

public class ContactsFragment extends MainActivityFragment {

  private MainActivity context;
  private ListView tabContentFeegheContacts;
  private ListView tabContentPhoneContacts;
  private ContactService contactService;
  private Session session;
  private CacheCollection contactCacheCollection;
  private FeegheContactsAdapter feegheContactsAdapter;
  private PhoneContactsAdapter phoneContactsAdapter;
  private ProgressDialog contactsPreloader;
  private TabHost tabHostContacts;

  public static final String TAB_PHONE_CONTACTS = "contacts";
  public static final String TAB_FEEGHE_CONTACTS = "feeghe_contacts";

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
    tabContentFeegheContacts = (ListView) context.findViewById(R.id.tabContentFeegheContacts);
    tabContentPhoneContacts = (ListView) context.findViewById(R.id.tabContentPhoneContacts);
    tabHostContacts = (TabHost) context.findViewById(R.id.tabHostContacts);
    tabHostContacts.setup();

    TabHost.TabSpec tabPhoneContacts = tabHostContacts.newTabSpec(TAB_PHONE_CONTACTS);
    tabPhoneContacts.setContent(R.id.tabContentPhoneContacts);
    tabPhoneContacts.setIndicator("Phone Contacts");

    TabHost.TabSpec tabFeegheContacts = tabHostContacts.newTabSpec(TAB_FEEGHE_CONTACTS);
    tabFeegheContacts.setContent(R.id.tabContentFeegheContacts);
    tabFeegheContacts.setIndicator("Feeghe Contacts");

    tabHostContacts.addTab(tabPhoneContacts);
    tabHostContacts.addTab(tabFeegheContacts);

    JSONObject query = contactService.getCacheQuery();
    contactCacheCollection = contactService.getCacheCollection(query);
    final ResponseArray responseFromCache = contactCacheCollection.getData();
    if (responseFromCache.getContent().length() != 0) {
      showFeegheContacts(responseFromCache);
    } else {
      contactsPreloader = ProgressDialog.show(context, null, "Please wait...", true, false);
    }

    contactService.query(query, new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        if (feegheContactsAdapter == null) {
          showFeegheContacts(response);
          contactCacheCollection.save(response.getContent());
          contactsPreloader.dismiss();
        } else {
          JSONArray addedContacts = contactCacheCollection.update(response).getContent();
          int addedContactsLength = addedContacts.length();
          try {
            for (int i = 0; i < addedContactsLength; i++) {
              feegheContactsAdapter.add(addedContacts.getJSONObject(i));
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

    Cursor contactsCursor = context.getContentResolver().query(
      ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
      null, null, null, null
    );
    ArrayList<HashMap<String, String>> phoneContacts = new ArrayList<>();
    int numberIndex = contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
    int nameIndex = contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
    while (contactsCursor.moveToNext()) {
      HashMap<String, String> phoneContactInfo = new HashMap<>();
      String phoneNumber = contactsCursor
        .getString(numberIndex)
        .replace("+", "")
        .replace("-", "")
        .replace(" ", "");
      phoneContactInfo.put("phoneNumber", phoneNumber);
      phoneContactInfo.put("name", contactsCursor.getString(nameIndex));
      phoneContacts.add(phoneContactInfo);
    }
    contactsCursor.close();
    showPhoneContacts(phoneContacts);
  }

  public void showFeegheContacts(ResponseArray response) {
    feegheContactsAdapter = new FeegheContactsAdapter(Util.toList(response));
    tabContentFeegheContacts.setAdapter(feegheContactsAdapter);
  }

  public void showPhoneContacts(ArrayList<HashMap<String, String>> phoneContacts) {
    phoneContactsAdapter = new PhoneContactsAdapter(phoneContacts);
    tabContentPhoneContacts.setAdapter(phoneContactsAdapter);
  }

  @Override
  public String getTabId() {
    return MainActivity.TAB_CONTACTS;
  }

  @Override
  protected void setupUIEvents() {

  }

  @Override
  protected void setupSocketEvents() {

  }

  @Override
  public String getFragmentId() {
    return MainActivity.FRAG_CONTACTS;
  }

  private class FeegheContactsAdapter extends ArrayAdapter<JSONObject> implements View.OnClickListener {

    public FeegheContactsAdapter(ArrayList<JSONObject> contacts) {
      super(context, R.layout.per_contact, contacts);
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

    private class FeegheContactViewHolder {
      TextView txtViewListContactName;
      TextView txtViewListContactNumber;
      Button btnShowChat;
      Button btnShowCall;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
      FeegheContactViewHolder viewHolder;
      if (convertView == null) {
        LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = vi.inflate(R.layout.per_contact, null);
        viewHolder = new FeegheContactViewHolder();
        viewHolder.txtViewListContactName = (TextView) convertView.findViewById(R.id.txtViewListContactName);
        viewHolder.txtViewListContactNumber = (TextView) convertView.findViewById(R.id.txtViewListContactNumber);
        viewHolder.btnShowChat = (Button) convertView.findViewById(R.id.btnShowChat);
        viewHolder.btnShowCall = (Button) convertView.findViewById(R.id.btnShowCall);
        viewHolder.btnShowChat.setOnClickListener(this);
        convertView.setTag(viewHolder);
      } else {
        viewHolder = (FeegheContactViewHolder) convertView.getTag();
      }

      JSONObject contact = getItem(position);
      try {
        JSONObject user = contact.getJSONObject("user");
        String userId = user.getString("id");
        String firstName = user.getString("firstName");
        String lastName = user.getString("lastName");
        if (!firstName.isEmpty() && !lastName.isEmpty() && !user.isNull("firstName") && !user.isNull("lastName")) {
          viewHolder.txtViewListContactName.setText(firstName + " " + lastName);
        } else {
          viewHolder.txtViewListContactName.setText(user.getString("phoneNumber"));
        }
        viewHolder.txtViewListContactName.setTag(userId);
        viewHolder.txtViewListContactNumber.setText(user.getString("phoneNumber"));
        viewHolder.btnShowChat.setTag(userId);
      } catch (JSONException e) {
        e.printStackTrace();
      }

      return convertView;
    }
  }

  private class PhoneContactsAdapter extends ArrayAdapter<HashMap<String, String>> implements View.OnClickListener {

    public PhoneContactsAdapter(ArrayList<HashMap<String, String>> contacts) {
      super(context, R.layout.per_contact, contacts);
    }

    @Override
    public void onClick(View v) {

    }

    private class PhoneContactViewHolder {
      TextView txtViewListContactName;
      TextView txtViewListContactNumber;
      Button btnShowChat;
      Button btnShowCall;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
      PhoneContactViewHolder viewHolder;
      if (convertView == null) {
        LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = vi.inflate(R.layout.per_contact, null);
        viewHolder = new PhoneContactViewHolder();
        viewHolder.txtViewListContactNumber = (TextView) convertView.findViewById(R.id.txtViewListContactNumber);
        viewHolder.txtViewListContactName = (TextView) convertView.findViewById(R.id.txtViewListContactName);
        viewHolder.btnShowChat = (Button) convertView.findViewById(R.id.btnShowChat);
        viewHolder.btnShowCall = (Button) convertView.findViewById(R.id.btnShowCall);
        viewHolder.btnShowChat.setOnClickListener(this);
        convertView.setTag(viewHolder);
      } else {
        viewHolder = (PhoneContactViewHolder) convertView.getTag();
      }

      HashMap<String, String> contact = getItem(position);
      viewHolder.txtViewListContactName.setText(contact.get("name"));
      viewHolder.txtViewListContactNumber.setText(contact.get("phoneNumber"));

      return convertView;
    }
  }
}
