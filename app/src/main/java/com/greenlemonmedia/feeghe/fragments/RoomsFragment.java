package com.greenlemonmedia.feeghe.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.R;
import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.CacheCollection;
import com.greenlemonmedia.feeghe.api.ResponseArray;
import com.greenlemonmedia.feeghe.api.RoomService;
import com.greenlemonmedia.feeghe.api.Socket;
import com.greenlemonmedia.feeghe.api.Util;
import com.greenlemonmedia.feeghe.storage.Session;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

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

    final ResponseArray responseFromCache = roomCacheCollection.getContent();
    if (responseFromCache.getContent().length() != 0) {
      showRooms(responseFromCache);
    } else {
      roomsPreloader = ProgressDialog.show(context, null, "Please wait...", true, false);
    }

    roomService.query(request, new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        if (roomsAdapter == null) {
          showRooms(response);
          roomCacheCollection.save(response.getContent());
          roomsPreloader.dismiss();
        } else {
          JSONArray roomsFromServer = response.getContent();
          JSONArray roomsFromCache = responseFromCache.getContent();
          int roomsCacheLength = roomsFromCache.length();
          int roomsServerLength = roomsFromServer.length();
          try {
            for (int i = 0; i < roomsServerLength; i++) {
              boolean inCache = false;
              JSONObject roomFromServer = (JSONObject) roomsFromServer.getJSONObject(i);
              String roomId = roomFromServer.getString("id");
              for (int j = 0; j < roomsCacheLength; j++) {
                if (roomsFromCache.getJSONObject(j).getString("id").equals(roomId)) {
                  inCache = true;
                  break;
                }
              }
              if (!inCache) {
                roomCacheCollection.save(roomFromServer);
                roomsAdapter.add(roomFromServer);
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

            context.runOnUiThread(new Runnable() {

              @Override
              public void run() {
                int roomsCount = roomsAdapter.getCount();
                try {
                  String roomId = data.getString("room");
                  String recentChat = data.getString("content");
                  for (int i = 0; i < roomsCount; i++) {
                    if (roomsAdapter.getItem(i).getString("id").equals(roomId)) {
                      JSONObject roomUpdate = roomsAdapter.getItem(i);
                      roomUpdate.put("recentChat", recentChat);
                      roomsAdapter.remove(roomUpdate);
                      roomsAdapter.insert(roomUpdate, i);
                      break;
                    }
                  }
                } catch (JSONException ex) {
                  ex.printStackTrace();
                }
              }
            });
          }
        } catch (JSONException ex) {
          ex.printStackTrace();
        }
      }
    });
  }

  public void showRooms(ResponseArray response) {
    roomsAdapter = new RoomsAdapter(Util.toList(response));
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
      TextView txtViewRoomRecentChat;
      TextView txtViewRoomName;
      ImageView imgViewRoomImg;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
      RoomViewHolder viewHolder;
      if (convertView == null) {
        LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = vi.inflate(R.layout.per_room, null);
        viewHolder = new RoomViewHolder();
        viewHolder.imgViewRoomImg = (ImageView) convertView.findViewById(R.id.imgViewRoomImg);
        viewHolder.txtViewRoomName = (TextView) convertView.findViewById(R.id.txtViewRoomName);
        viewHolder.txtViewRoomRecentChat = (TextView) convertView.findViewById(R.id.txtViewRoomRecentChat );
        convertView.setTag(viewHolder);
        convertView.setOnClickListener(this);
      } else {
        viewHolder = (RoomViewHolder) convertView.getTag();
      }
      JSONObject room = getItem(position);
      try {
        viewHolder.id = room.getString("id");
        viewHolder.info = room;
        viewHolder.txtViewRoomName.setText(Util.getRoomName(room.getJSONObject("users"), session.getUserId()));
        viewHolder.txtViewRoomRecentChat.setText(room.getString("recentChat"));
      } catch (JSONException e) {
        e.printStackTrace();
      }
      return convertView;
    }
  }
}
