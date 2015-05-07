package com.greenlemonmedia.feeghe.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
  private View phoneContactsContainer;
  private View feegheContactsContainer;

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
    feegheContactsContainer = context.findViewById(R.id.tabContentFeegheContacts);
    phoneContactsContainer = context.findViewById(R.id.tabContentPhoneContacts);

    dialogNewFeegheContactBuilder = new AlertDialog.Builder(context);
    View dialogContent = context.getLayoutInflater().inflate(R.layout.dialog_create_feeghe_contact, null);
    dialogNewFeegheContactBuilder.setView(dialogContent);
    dialogNewFeegheContactBuilder.setPositiveButton("Save", null);
    editTxtNewFeegheContact = (EditText) dialogContent.findViewById(R.id.editTxtNewFeegheContact);
    txtViewNewFeegheContactError = (TextView) dialogContent.findViewById(R.id.txtViewNewFeegheContactError);

    dialogSelFeegheContactBuilder = new AlertDialog.Builder(context);
    dialogSelFeegheContactBuilder.setTitle("Choose Action");

    contactCacheCollection = contactService.getCacheCollection();
    final ResponseArray responseFromCache = contactCacheCollection.getData();
    if (responseFromCache.getContent().length() != 0) {
      showFeegheContacts(responseFromCache);
    }

    contactService.query(contactService.getCacheQuery(), new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        if (feegheContactsAdapter == null) {
          showFeegheContacts(response);
          contactCacheCollection.save(response.getContent());
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
      public void onFail(int statusCode, String error, JSONObject validationError) {
        Toast.makeText(context, error, Toast.LENGTH_LONG).show();
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
    String[] filters = {
      "Feeghe Contacts",
      "Phone Contacts"
    };
    context.setActionBarSpinner(filters, new AdapterView.OnItemSelectedListener() {

      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position == 0) {
          phoneContactsContainer.setVisibility(View.GONE);
          feegheContactsContainer.setVisibility(View.VISIBLE);
        } else if (position == 1) {
          phoneContactsContainer.setVisibility(View.VISIBLE);
          feegheContactsContainer.setVisibility(View.GONE);
        }
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {

      }
    });

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
                    public void onFail(int statusCode, String error, JSONObject validationError) {

                    }
                  });
                } catch (JSONException e) {
                  e.printStackTrace();
                }
              }

              @Override
              public void onFail(int statusCode, String error, JSONObject validationError) {
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
  public void onKeyboardShow() {

  }

  @Override
  public void onKeyboardHide() {

  }

  @Override
  public String getFragmentId() {
    return MainActivity.FRAG_CONTACTS;
  }

  @Override
  public boolean onSearchQuerySubmit(String query) {
    return false;
  }

  @Override
  public boolean onSearchQueryChange(String query) {
    return false;
  }

  @Override
  public void onSearchClose() {
    return;
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
            Toast.makeText(context, error, Toast.LENGTH_LONG).show();
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
      ImageView imgViewProfilePic;
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
        viewHolder.imgViewProfilePic = (ImageView) convertView.findViewById(R.id.imgViewPerContact);
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
        if (!user.isNull("profilePic")) {
          APIUtils.getPicasso(context)
            .load(Uri.parse(APIUtils.getStaticUrl(user.getJSONObject("profilePic").getString("small"))))
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.placeholder)
            .into(viewHolder.imgViewProfilePic);
        } else {
          APIUtils.getPicasso(context)
            .load(R.drawable.placeholder)
            .into(viewHolder.imgViewProfilePic);
        }
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
