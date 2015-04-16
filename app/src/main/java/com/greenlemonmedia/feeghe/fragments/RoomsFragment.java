package com.greenlemonmedia.feeghe.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
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
import com.greenlemonmedia.feeghe.tasks.LoadFaceChatTask;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class RoomsFragment extends MainActivityFragment {

  private MainActivity context;
  private RoomsAdapter roomsAdapter;
  private ListView listViewRooms;
  private Session session;
  private RoomService roomService;
  private CacheCollection roomCacheCollection;
  private ProgressDialog roomsPreloader;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_rooms, container, false);
  }

  public void onActivityCreated(Bundle savedInstance) {
    super.onActivityCreated(savedInstance);
    context = getCurrentActivity();
    listViewRooms = (ListView) context.findViewById(R.id.listViewRooms);

    session = Session.getInstance(context);
    roomService = new RoomService(context);
    JSONObject request = roomService.getCacheQuery();
    roomCacheCollection = roomService.getCacheCollection(request);

    final ResponseArray responseFromCache = roomCacheCollection.getData();
    if (responseFromCache.length() != 0) {
      showRooms(responseFromCache);
    } else {
      roomsPreloader = APIUtils.showPreloader(context);
    }

    roomService.query(request, new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        if (responseFromCache.length() == 0) {
          showRooms(response);
          roomCacheCollection.save(response.getContent());
          roomsPreloader.dismiss();
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

      }
    });

    setupUIEvents();
    setupSocketEvents();
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
          final JSONObject data = evt.getJSONObject("data");
          if (verb.equals("created")) {
            int roomsCount = roomsAdapter.getCount();
            String roomId = data.getString("room");
            String recentChat = data.getString("content");
            for (int i = 0; i < roomsCount; i++) {
              if (roomsAdapter.getItem(i).getString("id").equals(roomId)) {
                JSONObject roomUpdate = roomsAdapter.getItem(i);
                roomsAdapter.remove(roomUpdate);
                JSONObject roomUser = roomUpdate.put("recentChat", recentChat)
                  .getJSONObject("users")
                  .getJSONObject(session.getUserId());
                roomUser.put("unreadCount", roomUser.getInt("unreadCount") + 1);
                roomsAdapter.insert(roomUpdate, i);
                return;
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

  public void showRooms(ResponseArray response) {
    roomsAdapter = new RoomsAdapter(APIUtils.toList(response));
    listViewRooms.setAdapter(roomsAdapter);
  }

  @Override
  public String getTabId() {
    return MainActivity.TAB_MESSAGES;
  }

  private class RoomsAdapter extends ArrayAdapter<JSONObject> implements View.OnClickListener {

    private HashMap<String, Spanned> recentChatWithFaces;

    public RoomsAdapter(ArrayList<JSONObject> rooms) {
      super(context, R.layout.per_room, rooms);
      recentChatWithFaces = new HashMap<>();
    }

    @Override
    public void onClick(View v) {
      context.showRoomFragment(((RoomViewHolder) v.getTag()).info);
    }

    private class RoomViewHolder {
      String id;
      JSONObject info;
      TextView txtViewRoomRecentChat;
      TextView txtViewRoomName;
      ImageView imgViewRoomImg;
      TextView txtViewRoomUnread;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
      final RoomViewHolder viewHolder;
      if (convertView == null) {
        LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = vi.inflate(R.layout.per_room, null);
        viewHolder = new RoomViewHolder();
        viewHolder.imgViewRoomImg = (ImageView) convertView.findViewById(R.id.imgViewRoomImg);
        viewHolder.txtViewRoomName = (TextView) convertView.findViewById(R.id.txtViewRoomName);
        viewHolder.txtViewRoomRecentChat = (TextView) convertView.findViewById(R.id.txtViewRoomRecentChat);
        viewHolder.txtViewRoomUnread = (TextView) convertView.findViewById(R.id.txtViewRoomUnread);
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
        } else {
          viewHolder.txtViewRoomUnread.setText("");
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
          if (APIUtils.messageHasFace(room.getString("recentChat"))) {
            LoadFaceChatTask loadFaceChatTask = new LoadFaceChatTask(
              context,
              room.getString("recentChat"),
              new LoadFaceChatTask.Listener() {

                @Override
                public void onSuccess(SpannableStringBuilder text) {
                  if (position >= listViewRooms.getFirstVisiblePosition() && position <= listViewRooms.getLastVisiblePosition()) {
                    viewHolder.txtViewRoomRecentChat.setText(text);
                  }
                  recentChatWithFaces.put(room.optString("id"), text);
                }

                @Override
                public void onFail(int statusCode, String error) {
                  Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                }
              },
              new LoadFaceChatTask.OnFaceClickListener() {

                @Override
                public void onClick(View widget, String faceId) {

                }
              }
            );
            loadFaceChatTask.execute();
          } else {
            viewHolder.txtViewRoomRecentChat.setText(room.getString("recentChat"));
          }
        }
      } catch (JSONException e) {
        e.printStackTrace();
      }

      return convertView;
    }
  }
}
