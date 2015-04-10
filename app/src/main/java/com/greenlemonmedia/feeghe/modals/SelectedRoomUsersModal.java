package com.greenlemonmedia.feeghe.modals;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.R;
import com.greenlemonmedia.feeghe.adapters.ContactsSpinnerAdapter;
import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.CacheCollection;
import com.greenlemonmedia.feeghe.api.ContactService;
import com.greenlemonmedia.feeghe.api.ResponseArray;
import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.RoomService;
import com.greenlemonmedia.feeghe.api.APIUtils;
import com.greenlemonmedia.feeghe.storage.Session;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by tontonskie on 3/31/15.
 */
public class SelectedRoomUsersModal extends MainActivityModal {

  private Spinner spinUsers;
  private ListView listViewRoomUsers;
  private ContactService contactService;
  private MainActivity context;
  private ContactsSpinnerAdapter contactsAdapter;
  private CacheCollection contactsCache;
  private RoomUsersAdapter roomUsersAdapter;
  private Button btnAdd;
  private RoomService roomService;
  private Session session;
  private CacheCollection roomsCache;

  public SelectedRoomUsersModal(MainActivity activity) {
    super(activity);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_edit_selected_room_users);
    context = getActivity();

    spinUsers = (Spinner) findViewById(R.id.spinEditSelectedRoomUsers);
    listViewRoomUsers = (ListView) findViewById(R.id.listViewSelectedRoomUsers);
    btnAdd = (Button) findViewById(R.id.btnAddSelectedRoomUser);

    session = Session.getInstance(context);
    contactService = new ContactService(context);
    roomService = new RoomService(context);

    roomsCache = roomService.getCacheCollection();
    JSONObject cacheQuery = contactService.getCacheQuery();
    contactsCache = contactService.getCacheCollection(cacheQuery);
    ResponseArray contactsFromCache = contactsCache.getData();
    if (contactsFromCache.length() > 0) {
      setContacts(contactsFromCache);
    }

    contactService.query(cacheQuery, new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        if (contactsAdapter == null) {
          setContacts(response);
          contactsCache.save(response.getContent());
        } else {
          JSONArray newContactsForCache = contactsCache.updateCollection(response).getContent();
          contactsAdapter.clear();
          try {
            for (int i = 0; i < newContactsForCache.length(); i++) {
              contactsAdapter.add(newContactsForCache.getJSONObject(i));
            }
          } catch (JSONException ex) {
            ex.printStackTrace();
          }
        }
      }

      @Override
      public void onFail(int statusCode, String error, JSONObject validationError) {

      }
    });

    setupUIEvents();
  }

  private void setContacts(ResponseArray contacts) {
    contactsAdapter = new ContactsSpinnerAdapter(context, APIUtils.toList(contacts));
    spinUsers.setAdapter(contactsAdapter);
  }

  private void setUsers(ArrayList<JSONObject> users) {
    roomUsersAdapter = new RoomUsersAdapter(users);
    listViewRoomUsers.setAdapter(roomUsersAdapter);
  }

  @Override
  protected void setupUIEvents() {
    btnAdd.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        final JSONObject currentRoom = (JSONObject) getData();
        JSONArray newRoomUsers = new JSONArray();
        try {

          newRoomUsers.put(((JSONObject) spinUsers.getSelectedItem()).getJSONObject("user").getString("id"));
          int roomUsersCount = roomUsersAdapter.getCount();
          for (int i = 0; i < roomUsersCount; i++) {
            newRoomUsers.put(roomUsersAdapter.getItem(i).getString("id"));
          }

          if (!currentRoom.getBoolean("isGroup")) {
            JSONObject updateRoomParams = new JSONObject();
            updateRoomParams.put("isGroup", true);
            updateRoomParams.put("creator", session.getUserId());
            updateRoomParams.put("users", newRoomUsers);
            roomService.socketSave(updateRoomParams, new APIService.SocketCallback() {

              @Override
              public void onSuccess(ResponseObject response) {
                updateRoomUsersList(null, response.getContent());
              }

              @Override
              public void onFail(int statusCode, String error) {

              }
            });
            return;
          }

          roomService.addUsers(currentRoom.getString("id"), newRoomUsers, new APIService.SocketCallback() {

            @Override
            public void onSuccess(ResponseObject response) {
              updateRoomUsersList(currentRoom, response.getContent());
            }

            @Override
            public void onFail(int statusCode, String error) {

            }
          });

        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    });
  }

  private void updateRoomUsersList(JSONObject currentRoom, JSONObject updatedRoom) {
    roomUsersAdapter.clear();
    try {
      if (currentRoom == null) {
        roomsCache.save(updatedRoom);
      } else {
        roomsCache.replace(currentRoom.getString("id"), updatedRoom);
      }
      JSONObject users = updatedRoom.getJSONObject("users");
      Iterator<?> i = users.keys();
      while (i.hasNext()) {
        roomUsersAdapter.add(users.getJSONObject((String) i.next()));
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
    setData(updatedRoom);
  }

  private void showGallery() {

  }

  @Override
  protected void onStart() {
    JSONObject room = (JSONObject) getData();
    try {

      if (room.getBoolean("isGroup") && !room.getJSONObject("creator").getString("id").equals(session.getUserId())) {
        btnAdd.setVisibility(View.INVISIBLE);
      } else {
        btnAdd.setVisibility(View.VISIBLE);
      }

      ArrayList<JSONObject> userList = new ArrayList<>();
      JSONObject users = room.getJSONObject("users");
      Iterator<?> i = users.keys();
      while (i.hasNext()) {
         userList.add(users.getJSONObject((String) i.next()));
      }
      setUsers(userList);
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  private class RoomUsersAdapter extends ArrayAdapter<JSONObject> implements View.OnClickListener {

    public RoomUsersAdapter(ArrayList<JSONObject> users) {
      super(context, R.layout.per_room_user, users);
    }

    @Override
    public void onClick(View v) {
      int pos = (int) v.getTag();
      JSONObject user = getItem(pos);
      remove(user);
      try {
        final String currentRoomId = ((JSONObject) getData()).getString("id");
        roomService.removeUser(currentRoomId, user.getString("id"), new APIService.SocketCallback() {

          @Override
          public void onSuccess(ResponseObject response) {
            JSONObject roomData = response.getContent();
            roomsCache.replace(currentRoomId, roomData);
            setData(roomData);
          }

          @Override
          public void onFail(int statusCode, String error) {

          }
        });
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }

    private class RoomUserViewHolder {
      public Button btnRemoveUser;
      public TextView txtViewRoomUser;
    }

    public View getView(int position, View convertView, ViewGroup viewGroup) {
      RoomUserViewHolder viewHolder;
      if (convertView == null) {
        convertView = context.getLayoutInflater().inflate(R.layout.per_room_user, null);
        viewHolder = new RoomUserViewHolder();
        viewHolder.btnRemoveUser = (Button) convertView.findViewById(R.id.btnSelectedRoomRemoveUser);
        viewHolder.txtViewRoomUser = (TextView) convertView.findViewById(R.id.txtViewSelectedRoomUser);
        viewHolder.btnRemoveUser.setOnClickListener(this);
        convertView.setTag(viewHolder);
      } else {
        viewHolder = (RoomUserViewHolder) convertView.getTag();
      }

      JSONObject roomUser = getItem(position);
      JSONObject currentRoom = (JSONObject) getData();
      try {
        if (currentRoom.getBoolean("isGroup") && !currentRoom.getJSONObject("creator").getString("id").equals(session.getUserId())) {
          viewHolder.btnRemoveUser.setVisibility(View.INVISIBLE);
        }
      } catch (JSONException e) {
        e.printStackTrace();
      }

      viewHolder.txtViewRoomUser.setText(APIUtils.getFullName(roomUser));
      viewHolder.btnRemoveUser.setTag(position);

      return convertView;
    }
  }
}
