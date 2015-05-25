package com.greenlemonmedia.feeghe.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.R;
import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.CacheCollection;
import com.greenlemonmedia.feeghe.api.ResponseArray;
import com.greenlemonmedia.feeghe.api.RoomService;
import com.greenlemonmedia.feeghe.api.Socket;
import com.greenlemonmedia.feeghe.api.APIUtils;
import com.greenlemonmedia.feeghe.storage.Session;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class RoomsFragment extends MainActivityFragment {

  private RoomsAdapter roomsAdapter;
  private ListView listViewRooms;
  private Session session;
  private RoomService roomService;
  private CacheCollection roomCacheCollection;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_rooms, container, false);
  }

  public void onActivityCreated(Bundle savedInstance) {
    super.onActivityCreated(savedInstance);

    listViewRooms = (ListView) context.findViewById(R.id.listViewRooms);

    session = Session.getInstance(context);
    roomService = new RoomService(context);

    loadRooms();

    setupUIEvents();
    setupSocketEvents();
  }

  private void loadRooms() {
    JSONObject request = roomService.getCacheQuery();
    roomCacheCollection = roomService.getCacheCollection();
    final ResponseArray responseFromCache = roomCacheCollection.getData();
    if (responseFromCache.length() != 0) {
      showRooms(responseFromCache);
    }

    roomService.query(request, new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        if (responseFromCache.length() == 0) {
          showRooms(response);
          roomCacheCollection.save(response.getContent());
        } else {
          roomsAdapter.clear();
          JSONArray addedRooms = roomCacheCollection.updateCollection(response).getContent();
          int addedRoomsLength = addedRooms.length();
          try {
            for (int i = 0; i < addedRoomsLength; i++) {
              roomsAdapter.add(addedRooms.getJSONObject(i));
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
  }

  @Override
  protected void setupUIEvents() {

  }

  @Override
  protected void setupSocketEvents() {
    Socket.on("message", new APIService.EventCallback() {

      @Override
      public void onEvent(JSONObject evt) {
        try {
          String verb = evt.getString("verb");
          JSONObject data = evt.getJSONObject("data");
          if (verb.equals("created")) {

            String currentUserId = session.getUserId();
            int len = roomsAdapter.getCount();
            for (int i = 0; i < len; i++) {

              JSONObject room = roomsAdapter.getItem(i);
              if (room.getString("id").equals(data.getString("room"))) {

                room.put("recentChat", data.getString("content"));
                room.put("recentChatCreatedAt", data.getString("timestamp"));

                JSONObject roomUser = room.getJSONObject("users").getJSONObject(currentUserId);
                roomUser.put("unreadCount", roomUser.optInt("unreadCount", 0) + 1);

                roomsAdapter.remove(roomsAdapter.getItem(i));
                roomsAdapter.insert(room, 0);
                roomCacheCollection.update(room.getString("id"), room);
                break;
              }
            }
          }
        } catch (JSONException ex) {
          ex.printStackTrace();
        }
      }
    });
  }

  @Override
  public void onKeyboardShow() {

  }

  @Override
  public void onKeyboardHide() {

  }

  @Override
  public String getFragmentId() {
    return MainActivity.FRAG_ROOMS;
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

  }

  @Override
  public void setActionBar() {
    context.setActionBarTitle(getResources().getString(R.string.app_name));
  }

  public void showRooms(ResponseArray response) {
    roomsAdapter = new RoomsAdapter(APIUtils.toList(response));
    listViewRooms.setAdapter(roomsAdapter);
  }

  @Override
  public String getTabId() {
    return MainActivity.TAB_MESSAGES;
  }

  private class RoomsAdapter extends ArrayAdapter<JSONObject> implements View.OnClickListener {

    public RoomsAdapter(ArrayList<JSONObject> rooms) {
      super(context, R.layout.per_room, rooms);
    }

    @Override
    public void onClick(View v) {
      context.showRoomFragment(((RoomViewHolder) v.getTag()).info);
    }

    private class RoomViewHolder {
      String id;
      JSONObject info;
      View layoutRoom;
      TextView txtViewRoomRecentChat;
      TextView txtViewRoomName;
      ImageView imgViewRoomImg;
      TextView txtViewRoomUnread;
      TextView txtViewRecentTimestamp;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
      final RoomViewHolder viewHolder;
      if (convertView == null) {
        LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = vi.inflate(R.layout.per_room, null);
        viewHolder = new RoomViewHolder();
        viewHolder.layoutRoom = convertView;
        viewHolder.imgViewRoomImg = (ImageView) convertView.findViewById(R.id.imgViewRoomImg);
        viewHolder.txtViewRoomName = (TextView) convertView.findViewById(R.id.txtViewRoomName);
        viewHolder.txtViewRoomRecentChat = (TextView) convertView.findViewById(R.id.txtViewRoomRecentChat);
        viewHolder.txtViewRoomUnread = (TextView) convertView.findViewById(R.id.txtViewRoomUnread);
        viewHolder.txtViewRecentTimestamp = (TextView) convertView.findViewById(R.id.txtViewRoomTimestamp);
        convertView.setTag(viewHolder);
        convertView.setOnClickListener(this);
      } else {
        viewHolder = (RoomViewHolder) convertView.getTag();
      }

      final JSONObject room = getItem(position);
      try {
        viewHolder.id = room.getString("id");
        viewHolder.info = room;
        JSONObject usersInRoom = room.getJSONObject("users");
        viewHolder.txtViewRoomName.setText(APIUtils.getRoomName(usersInRoom, session.getUserId()));

        String currentUserId = session.getUserId();
        int unreadCount = usersInRoom.getJSONObject(currentUserId).getInt("unreadCount");
        if (unreadCount > 0) {
          viewHolder.txtViewRoomUnread.setText(unreadCount + "");
          viewHolder.txtViewRoomUnread.setVisibility(View.VISIBLE);
          viewHolder.layoutRoom.setBackgroundColor(context.getResources().getColor(R.color.perRoomUnreadBg));
        } else {
          viewHolder.txtViewRoomUnread.setText("");
          viewHolder.txtViewRoomUnread.setVisibility(View.INVISIBLE);
          viewHolder.layoutRoom.setBackgroundColor(context.getResources().getColor(R.color.perRoomBg));
        }

        Iterator<String> iUsers = usersInRoom.keys();
        while (iUsers.hasNext()) {
          String iUserKey = (String) iUsers.next();
          if (!iUserKey.equals(currentUserId)) {
            JSONObject roomUser = usersInRoom.getJSONObject(iUserKey);
            if (roomUser.isNull("profilePic")) {
              APIUtils.getPicasso(context)
                .load(R.drawable.placeholder)
                .into(viewHolder.imgViewRoomImg);
            } else {
              APIUtils.getPicasso(context)
                .load(Uri.parse(APIUtils.getStaticUrl(roomUser.getJSONObject("profilePic").getString("small"))))
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .into(viewHolder.imgViewRoomImg);
            }
          }
        }

        if (!room.isNull("recentChat")) {
          String recentChat = room.getString("recentChat");
          if (!recentChat.isEmpty() && APIUtils.messageHasFace(recentChat)) {
            APIUtils.loadFacesFromMessage(context, recentChat, viewHolder.txtViewRoomRecentChat, null, true);
          } else {
            viewHolder.txtViewRoomRecentChat.setText(APIUtils.sanitizeMessage(recentChat));
          }
          viewHolder.txtViewRecentTimestamp.setText(room.getString("recentChatTimestamp"));
        } else {
          viewHolder.txtViewRoomRecentChat.setText("");
          viewHolder.txtViewRecentTimestamp.setText("");
        }
      } catch (JSONException e) {
        e.printStackTrace();
      }

      return convertView;
    }
  }
}
