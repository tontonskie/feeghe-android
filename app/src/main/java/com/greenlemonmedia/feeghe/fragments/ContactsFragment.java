package com.greenlemonmedia.feeghe.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;

import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.R;
import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.CacheCollection;
import com.greenlemonmedia.feeghe.api.ContactService;
import com.greenlemonmedia.feeghe.api.ResponseArray;
import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.UserService;
import com.greenlemonmedia.feeghe.api.APIUtils;
import com.greenlemonmedia.feeghe.storage.Session;
import com.greenlemonmedia.feeghe.tasks.GoToRoomTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class ContactsFragment extends MainActivityFragment {

  private MainActivity context;
  private ListView listViewFeegheContacts;
  private ListView listViewPhoneContacts;
  private ContactService contactService;
  private Session session;
  private CacheCollection contactCacheCollection;
  private FeegheContactsAdapter feegheContactsAdapter;
  private PhoneContactsAdapter phoneContactsAdapter;
  private ProgressDialog contactsPreloader;
  private TabHost tabHostContacts;
  private Button btnFeegheContactsCreate;
  private Button btnPhoneContactsSave;
  private EditText editTxtNewFeegheContact;
  private AlertDialog.Builder dialogNewFeegheContactBuilder;
  private TextView txtViewNewFeegheContactError;
  private AlertDialog dialogNewFeegheContact;
  private AlertDialog.Builder dialogSelFeegheContactBuilder;
  private AlertDialog dialogSelFeegheContact;
  private JSONObject selectedFeegheContact;
  private UserService userService;

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
    userService = new UserService(context);
    listViewFeegheContacts = (ListView) context.findViewById(R.id.listViewFeegheContacts);
    listViewPhoneContacts = (ListView) context.findViewById(R.id.listViewPhoneContacts);
    btnFeegheContactsCreate = (Button) context.findViewById(R.id.btnFeegheContactsCreate);
    btnPhoneContactsSave = (Button) context.findViewById(R.id.btnPhoneContactsSave);

    dialogNewFeegheContactBuilder = new AlertDialog.Builder(context);
    View dialogContent = context.getLayoutInflater().inflate(R.layout.dialog_create_feeghe_contact, null);
    dialogNewFeegheContactBuilder.setView(dialogContent);
    dialogNewFeegheContactBuilder.setPositiveButton("Save", null);
    editTxtNewFeegheContact = (EditText) dialogContent.findViewById(R.id.editTxtNewFeegheContact);
    txtViewNewFeegheContactError = (TextView) dialogContent.findViewById(R.id.txtViewNewFeegheContactError);

    dialogSelFeegheContactBuilder = new AlertDialog.Builder(context);
    dialogSelFeegheContactBuilder.setTitle("Choose Action");

    tabHostContacts = (TabHost) context.findViewById(R.id.tabHostContacts);
    tabHostContacts.setup();

    TabHost.TabSpec tabFeegheContacts = tabHostContacts.newTabSpec(TAB_FEEGHE_CONTACTS);
    tabFeegheContacts.setContent(R.id.tabContentFeegheContacts);
    tabFeegheContacts.setIndicator("Feeghe Contacts");

    TabHost.TabSpec tabPhoneContacts = tabHostContacts.newTabSpec(TAB_PHONE_CONTACTS);
    tabPhoneContacts.setContent(R.id.tabContentPhoneContacts);
    tabPhoneContacts.setIndicator("Phone Contacts");

    tabHostContacts.addTab(tabFeegheContacts);
    tabHostContacts.addTab(tabPhoneContacts);

    JSONObject query = contactService.getCacheQuery();
    contactCacheCollection = contactService.getCacheCollection(query);
    final ResponseArray responseFromCache = contactCacheCollection.getData();
    if (responseFromCache.getContent().length() != 0) {
      showFeegheContacts(responseFromCache);
    } else {
      contactsPreloader = APIUtils.showPreloader(context);
    }

    contactService.query(query, new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        if (feegheContactsAdapter == null) {
          showFeegheContacts(response);
          contactCacheCollection.save(response.getContent());
          contactsPreloader.dismiss();
        } else {
          feegheContactsAdapter.clear();
          JSONArray addedContacts = contactCacheCollection.updateCollection(response).getContent();
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
        contactsPreloader.dismiss();
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

    setupUIEvents();
  }

  public void showFeegheContacts(ResponseArray response) {
    feegheContactsAdapter = new FeegheContactsAdapter(APIUtils.toList(response));
    listViewFeegheContacts.setAdapter(feegheContactsAdapter);
  }

  public void showPhoneContacts(ArrayList<HashMap<String, String>> phoneContacts) {
    phoneContactsAdapter = new PhoneContactsAdapter(phoneContacts);
    listViewPhoneContacts.setAdapter(phoneContactsAdapter);
  }

  @Override
  public String getTabId() {
    return MainActivity.TAB_CONTACTS;
  }

  @Override
  protected void setupUIEvents() {
    String[] feegheContactActions = new String[] {
      "Delete"
    };
    dialogSelFeegheContactBuilder.setItems(feegheContactActions, new DialogInterface.OnClickListener() {

      @Override
      public void onClick(DialogInterface dialog, int which) {
        if (which == 0) {
          feegheContactsAdapter.remove(selectedFeegheContact);
          try {
            contactService.delete(selectedFeegheContact.getString("id"), null);
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }
      }
    });

    dialogNewFeegheContactBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

      @Override
      public void onClick(DialogInterface dialog, int which) {
        dialog.cancel();
      }
    });

    btnFeegheContactsCreate.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        if (dialogNewFeegheContact == null) {
          dialogNewFeegheContact = dialogNewFeegheContactBuilder.create();
        }
        dialogNewFeegheContact.show();
        dialogNewFeegheContact.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

          @Override
          public void onClick(View v) {
            String newContactNum = editTxtNewFeegheContact.getText().toString();
            if (newContactNum.isEmpty()) {
              txtViewNewFeegheContactError.setVisibility(View.VISIBLE);
              txtViewNewFeegheContactError.setText("Phone number is required");
              return;
            } else if (newContactNum.equals(session.getCurrentUser().getString("phoneNumber"))) {
              txtViewNewFeegheContactError.setVisibility(View.VISIBLE);
              txtViewNewFeegheContactError.setText("New Feeghe contact can't be your number");
              return;
            }
            txtViewNewFeegheContactError.setVisibility(View.GONE);
            JSONObject newFeegheContactParams = new JSONObject();
            try {
              newFeegheContactParams.put("owner", session.getUserId());
              newFeegheContactParams.put("user", newContactNum);
            } catch (JSONException e) {
              e.printStackTrace();
            }

            final Button btnPositive = (Button) v;
            btnPositive.setText("Saving...");
            btnPositive.setEnabled(false);
            editTxtNewFeegheContact.setEnabled(false);

            contactService.save(newFeegheContactParams, new APIService.SaveCallback() {

              @Override
              public void onSuccess(ResponseObject response) {
                final JSONObject newContact = response.getContent();
                try {
                  userService.get(newContact.getString("user"), new APIService.GetCallback() {

                    @Override
                    public void onSuccess(ResponseObject user) {
                      try {
                        newContact.put("user", user.getContent());
                      } catch (JSONException e) {
                        e.printStackTrace();
                      }
                      feegheContactsAdapter.add(newContact);
                      contactCacheCollection.save(newContact);
                      btnPositive.setText("Save");
                      btnPositive.setEnabled(true);
                      editTxtNewFeegheContact.setEnabled(true);
                      editTxtNewFeegheContact.setText("");
                      dialogNewFeegheContact.dismiss();
                    }

                    @Override
                    public void onFail(int statusCode, String error) {

                    }
                  });
                } catch (JSONException e) {
                  e.printStackTrace();
                }
              }

              @Override
              public void onFail(int statusCode, String error) {
                editTxtNewFeegheContact.setEnabled(true);
                txtViewNewFeegheContactError.setVisibility(View.VISIBLE);
                txtViewNewFeegheContactError.setText(error);
                btnPositive.setText("Save");
                btnPositive.setEnabled(true);
              }
            });
          }
        });
      }
    });

    btnPhoneContactsSave.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {

      }
    });
  }

  @Override
  protected void setupSocketEvents() {

  }

  @Override
  public String getFragmentId() {
    return MainActivity.FRAG_CONTACTS;
  }

  private class FeegheContactsAdapter extends ArrayAdapter<JSONObject> implements View.OnClickListener, View.OnLongClickListener {

    public FeegheContactsAdapter(ArrayList<JSONObject> contacts) {
      super(context, R.layout.per_contact, contacts);
    }

    @Override
    public void onClick(View v) {
      switch (v.getId()) {
        case R.id.btnShowChat:
          showChat((Button) v);
          break;
        case R.id.btnShowCall:
          break;
      }
    }

    private void showChat(Button v) {
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

    @Override
    public boolean onLongClick(View v) {
      selectedFeegheContact = getItem((int) v.getTag());
      if (dialogSelFeegheContact == null) {
        dialogSelFeegheContact = dialogSelFeegheContactBuilder.create();
      }
      try {
        dialogSelFeegheContact.setTitle(APIUtils.getFullName(selectedFeegheContact.getJSONObject("user")));
      } catch (JSONException e) {
        e.printStackTrace();
      }
      dialogSelFeegheContact.show();
      return true;
    }

    private class FeegheContactViewHolder {
      TextView txtViewListContactName;
      TextView txtViewListContactNumber;
      Button btnShowChat;
      Button btnShowCall;
      View nameContainer;
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
        viewHolder.nameContainer = convertView.findViewById(R.id.perContactNameContainer);
        viewHolder.btnShowChat.setOnClickListener(this);
        viewHolder.nameContainer.setOnLongClickListener(this);
        convertView.setTag(viewHolder);
      } else {
        viewHolder = (FeegheContactViewHolder) convertView.getTag();
      }

      JSONObject contact = getItem(position);
      try {
        JSONObject user = contact.getJSONObject("user");
        viewHolder.txtViewListContactName.setText(APIUtils.getFullName(user));
        viewHolder.txtViewListContactName.setTag(user.getString("id"));
        viewHolder.txtViewListContactNumber.setText(user.getString("phoneNumber"));
        viewHolder.nameContainer.setTag(position);
        viewHolder.btnShowChat.setTag(user.getString("id"));
        viewHolder.btnShowCall.setVisibility(View.INVISIBLE);
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

    private void showChat(Button v) {

    }

    private void showCall(Button v) {

    }

    @Override
    public void onClick(View v) {
      switch (v.getId()) {
        case R.id.btnShowChat:
          showChat((Button) v);
          break;
        case R.id.btnShowCall:
          showCall((Button) v);
          break;
      }
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
        viewHolder.btnShowCall.setOnClickListener(this);
        viewHolder.btnShowCall.setVisibility(View.INVISIBLE);
        viewHolder.btnShowChat.setVisibility(View.INVISIBLE);
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
