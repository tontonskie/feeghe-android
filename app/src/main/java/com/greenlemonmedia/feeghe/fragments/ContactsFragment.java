package com.greenlemonmedia.feeghe.fragments;

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
import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.tasks.GoToRoomTask;
import com.greenlemonmedia.feeghe.tasks.GetContactsTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ContactsFragment extends MainActivityFragment {

  private MainActivity context;
  private ListView listViewContacts;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_contacts, container, false);
  }

  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    context = getCurrentActivity();
    listViewContacts = (ListView) context.findViewById(R.id.listViewContacts);

    GetContactsTask getContacts = new GetContactsTask(
      context,
      new GetContactsTask.Listener() {

        @Override
        public void onSuccess(ArrayList<JSONObject> contacts) {
          listViewContacts.setAdapter(new ContactsAdapter(contacts));
        }

        @Override
        public void onFail(int statusCode, String error) {

        }
      }
    );
    getContacts.execute();
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
            context.getTabHost().setCurrentTabByTag(MainActivity.TAB_MESSAGES);
            try {
              context.showRoomFragment(response.getContent().getString("id"));
            } catch (JSONException e) {
              e.printStackTrace();
            }
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
