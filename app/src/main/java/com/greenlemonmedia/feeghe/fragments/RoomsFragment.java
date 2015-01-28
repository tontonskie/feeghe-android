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
import com.greenlemonmedia.feeghe.api.Util;
import com.greenlemonmedia.feeghe.storage.Session;

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

    JSONObject request = null;
    try {
      JSONObject notNull = new JSONObject();
      JSONObject checkUser = new JSONObject();
      notNull.put("!", JSONObject.NULL);
      checkUser.put("users." + session.getUserId(), notNull);
      request = roomService.createWhereQuery(checkUser);
      request.put("sort", "updatedAt DESC");
    } catch (JSONException e) {
      e.printStackTrace();
    }
    roomCacheCollection = roomService.getCacheCollection(request);
    ResponseArray response = roomCacheCollection.getContent();
    if (response.getContent().length() == 0) {
      final ProgressDialog preloader = ProgressDialog.show(context, null, "Please wait...", true, false);
      roomService.query(request, new APIService.QueryCallback() {

        @Override
        public void onSuccess(ResponseArray response) {
          showRooms(response);
          roomCacheCollection.save(response.getContent());
          preloader.dismiss();
        }

        @Override
        public void onFail(int statusCode, String error) {

        }
      });
    } else {
      showRooms(response);
    }
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
      String info;
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
        viewHolder.info = room.toString();
        viewHolder.txtViewRoomName.setText(Util.getRoomName(room.getJSONObject("users"), session.getUserId()));
        viewHolder.txtViewRoomRecentChat.setText(room.getString("recentChat"));
      } catch (JSONException e) {
        e.printStackTrace();
      }
      return convertView;
    }
  }
}
