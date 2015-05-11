package com.greenlemonmedia.feeghe.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.R;
import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.ContactService;
import com.greenlemonmedia.feeghe.api.ResponseArray;
import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.UserService;
import com.greenlemonmedia.feeghe.api.APIUtils;
import com.greenlemonmedia.feeghe.storage.Session;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class NewUserFragment extends MainActivityFragment {

  private Button btnChangePass;
  private EditText txtNewPassword;
  private EditText txtConfirmPassword;
  private TextView txtViewNewUserError;
  private ViewAnimator view;
  private Spinner selectGender;
  private ListView listViewContacts;
  private CheckBox chkSyncContacts;
  private Button btnAddContacts;
  private Button btnSkipContacts;
  private ContactService contactService;
  private UserService userService;
  private Session session;
  private TabWidget tabs;
  private ProgressDialog preloader;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_new_user, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    session = Session.getInstance(context);
    contactService = new ContactService(context);
    userService = new UserService(context);
    preloader = new ProgressDialog(context);
    preloader.setCancelable(false);
    preloader.setMessage("Please wait...");

    view = (ViewAnimator) getView();
    view.setOutAnimation(AnimationUtils.loadAnimation(context, R.anim.abc_slide_out_top));
    view.setInAnimation(AnimationUtils.loadAnimation(context, R.anim.abc_slide_in_bottom));

    tabs = (TabWidget) context.findViewById(android.R.id.tabs);
    tabs.setVisibility(View.GONE);

    listViewContacts = (ListView) view.findViewById(R.id.tabContentFeegheContacts);
    selectGender = (Spinner) view.findViewById(R.id.selectGender);
    txtViewNewUserError = (TextView) view.findViewById(R.id.txtViewNewUserError);
    txtNewPassword = (EditText) view.findViewById(R.id.txtNewPassword);
    txtConfirmPassword = (EditText) view.findViewById(R.id.txtConfirmPassword);
    chkSyncContacts = (CheckBox) view.findViewById(R.id.chkSyncContacts);
    btnChangePass = (Button) view.findViewById(R.id.btnChangePass);
    btnAddContacts = (Button) view.findViewById(R.id.btnAddContacts);
    btnSkipContacts = (Button) view.findViewById(R.id.btnSkipContacts);

    setupUIEvents();
    setupSocketEvents();
  }

  @Override
  protected void setupUIEvents() {
    btnChangePass.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        String password = txtNewPassword.getText().toString();
        String confirmPassword = txtConfirmPassword.getText().toString();
        if (!password.equals(confirmPassword)) {
          txtViewNewUserError.setText("Password doesn't match");
          txtViewNewUserError.setVisibility(View.VISIBLE);
          return;
        } else if (password.isEmpty()) {
          txtViewNewUserError.setText("Password is required");
          txtViewNewUserError.setVisibility(View.VISIBLE);
          return;
        }
        txtViewNewUserError.setVisibility(View.INVISIBLE);
        JSONObject data = new JSONObject();
        try {
          data.put("gender", selectGender.getSelectedItem().toString().toLowerCase());
          data.put("newPassword", password);
        } catch (JSONException e) {
          e.printStackTrace();
        }
        preloader.show();
        userService.update(session.getUserId(), data, new APIService.UpdateCallback() {

          @Override
          public void onSuccess(ResponseObject updatedUser) {
            session.getCurrentUser().update(updatedUser);
            showSyncContactsForm();
          }

          @Override
          public void onFail(int statusCode, String error, JSONObject validationError) {
            txtViewNewUserError.setText(error);
            txtViewNewUserError.setVisibility(View.VISIBLE);
            preloader.dismiss();
          }
        });
      }
    });

    chkSyncContacts.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        int childCount = listViewContacts.getChildCount();
        if (childCount > 0) {
          if (((CheckBox) v).isChecked()) {
            for (int i = 0; i < childCount; i++) {
              ((CheckBox) listViewContacts.getChildAt(i).findViewById(R.id.chkContact)).setChecked(true);
            }
            return;
          }
          for (int i = 0; i < childCount; i++) {
            ((CheckBox) listViewContacts.getChildAt(i).findViewById(R.id.chkContact)).setChecked(false);
          }
        }
      }
    });

    btnAddContacts.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        CheckBox chkBox;
        JSONArray contactIds = new JSONArray();
        int childCount = listViewContacts.getChildCount();
        for (int i = 0; i < childCount; i++) {
          chkBox = (CheckBox) listViewContacts.getChildAt(i).findViewById(R.id.chkContact);
          if (chkBox.isChecked()) {
            contactIds.put((String) chkBox.getTag());
          }
        }
        if (contactIds.length() == 0) {
          goToHome(false);
          return;
        }
        preloader.show();
        contactService.sync(contactIds, new APIService.SaveCallback() {

          @Override
          public void onSuccess(ResponseObject response) {
            JSONObject userUpdate = new JSONObject();
            try {
              userUpdate.put("contactsCount", response.getContent().getInt("contactsCount"));
            } catch (JSONException e) {
              e.printStackTrace();
            }
            session.getCurrentUser().update(userUpdate);
            preloader.dismiss();
            goToHome();
          }

          @Override
          public void onFail(int statusCode, String error, JSONObject validationError) {
            preloader.dismiss();
          }
        });
      }
    });

    btnSkipContacts.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        goToHome();
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
    return MainActivity.FRAG_NEW_USER;
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

  @Override
  public void setActionBar() {

  }

  public void showSyncContactsForm() {
    view.showNext();
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
    userService.query(userService.createWhereQuery(userQuery), new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        listViewContacts.setAdapter(new ContactsAdapter(APIUtils.toList(response)));
        btnAddContacts.setEnabled(true);
        chkSyncContacts.setEnabled(true);
        chkSyncContacts.setText("Sync Contacts");
        preloader.dismiss();
      }

      @Override
      public void onFail(int statusCode, String error, JSONObject validationError) {
        preloader.dismiss();
      }
    });
  }

  @Override
  public String getTabId() {
    return null;
  }

  public void goToHome() {
    tabs.setVisibility(View.VISIBLE);
    context.showWallOfFacesFragment();
  }

  public void goToHome(boolean withBackStack) {
    tabs.setVisibility(View.VISIBLE);
    context.showMessagesFragment(withBackStack);
  }

  private class ContactsAdapter extends ArrayAdapter<JSONObject> {

    public ContactsAdapter(ArrayList<JSONObject> contacts) {
      super(context, R.layout.per_refer_contact, contacts);
    }

    private class ContactViewHolder {
      CheckBox checkBox;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
      ContactViewHolder viewHolder;
      if (convertView == null) {
        LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = vi.inflate(R.layout.per_refer_contact, null);
        viewHolder = new ContactViewHolder();
        viewHolder.checkBox = (CheckBox) convertView.findViewById(R.id.chkContact);
        convertView.setTag(viewHolder);
      } else {
        viewHolder = (ContactViewHolder) convertView.getTag();
      }
      JSONObject contact = getItem(position);
      try {
        viewHolder.checkBox.setText(APIUtils.getFullName(contact));
        viewHolder.checkBox.setTag(contact.getString("id"));
      } catch (JSONException e) {
        e.printStackTrace();
      }
      return convertView;
    }
  }
}
