package com.greenlemonmedia.feeghe.fragments;

import android.content.Context;
import android.os.Bundle;
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
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.R;
import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.tasks.SyncContactsTask;
import com.greenlemonmedia.feeghe.tasks.NewUserTask;
import com.greenlemonmedia.feeghe.tasks.ReferContactsTask;

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
  private MainActivity context;
  private ListView listViewContacts;
  private CheckBox chkSyncContacts;
  private Button btnAddContacts;
  private Button btnSkipContacts;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_new_user, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    context = getCurrentActivity();
    view = (ViewAnimator) getView();
    view.setOutAnimation(AnimationUtils.loadAnimation(context, R.anim.abc_slide_out_top));
    view.setInAnimation(AnimationUtils.loadAnimation(context, R.anim.abc_slide_in_bottom));

    listViewContacts = (ListView) view.findViewById(R.id.listViewContacts);
    selectGender = (Spinner) view.findViewById(R.id.selectGender);
    txtViewNewUserError = (TextView) view.findViewById(R.id.txtViewNewUserError);
    txtNewPassword = (EditText) view.findViewById(R.id.txtNewPassword);
    txtConfirmPassword = (EditText) view.findViewById(R.id.txtConfirmPassword);
    chkSyncContacts = (CheckBox) view.findViewById(R.id.chkSyncContacts);

    btnChangePass = (Button) view.findViewById(R.id.btnChangePass);
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
        NewUserTask newUserTask = new NewUserTask(
          context,
          selectGender.getSelectedItem().toString(),
          password,
          new NewUserTask.Listener() {

            @Override
            public void onSuccess(ResponseObject updatedUser) {
              showSyncContactsForm();
            }

            @Override
            public void onFail(int statusCode, String error) {
              txtViewNewUserError.setText(error);
              txtViewNewUserError.setVisibility(View.VISIBLE);
            }
          }
        );
        newUserTask.execute();
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

    btnAddContacts = (Button) view.findViewById(R.id.btnAddContacts);
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
          ((MainActivity) context).showHomeFragment(false);
          return;
        }
        SyncContactsTask addContacts = new SyncContactsTask(
          context,
          contactIds,
          new SyncContactsTask.Listener() {

            @Override
            public void onSuccess(ResponseObject response) {
              ((MainActivity) context).showHomeFragment();
            }

            @Override
            public void onFail(int statusCode, String error) {

            }
          }
        );
        addContacts.execute();
      }
    });

    btnSkipContacts = (Button) view.findViewById(R.id.btnSkipContacts);
    btnSkipContacts.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        ((MainActivity) context).showHomeFragment();
      }
    });
  }

  public void showSyncContactsForm() {
    view.showNext();
    ReferContactsTask referContacts = new ReferContactsTask(
      context,
      new ReferContactsTask.Listener() {

        @Override
        public void onSuccess(ArrayList<JSONObject> contacts) {
          listViewContacts.setAdapter(new ContactsAdapter(contacts));
          btnAddContacts.setEnabled(true);
          chkSyncContacts.setEnabled(true);
          chkSyncContacts.setText("Sync Contacts");
        }

        @Override
        public void onFail(int statusCode, String error) {

        }
      }
    );
    referContacts.execute();
  }

  @Override
  public String getTabId() {
    return null;
  }

  private class ContactsAdapter extends ArrayAdapter<JSONObject> {

    public ContactsAdapter(ArrayList<JSONObject> contacts) {
      super(context, R.layout.refer_contact, contacts);
    }

    private class ContactViewHolder {
      CheckBox checkBox;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
      ContactViewHolder viewHolder;
      if (convertView == null) {
        LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = vi.inflate(R.layout.refer_contact, null);
        viewHolder = new ContactViewHolder();
        viewHolder.checkBox = (CheckBox) convertView.findViewById(R.id.chkContact);
        convertView.setTag(viewHolder);
      } else {
        viewHolder = (ContactViewHolder) convertView.getTag();
      }
      JSONObject contact = getItem(position);
      try {
        viewHolder.checkBox.setText(contact.getString("firstName") + " " + contact.getString("lastName"));
        viewHolder.checkBox.setTag(contact.getString("id"));
      } catch (JSONException e) {
        e.printStackTrace();
      }
      return convertView;
    }
  }
}
