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

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_rooms, container, false);
  }

  public void onActivityCreated(Bundle savedInstance) {
    super.onActivityCreated(savedInstance);
    context = getCurrentActivity();
    session = Session.getInstance(context);
    roomService = new RoomService(context);
    listViewRooms = (ListView) context.findViewById(R.id.listViewRooms);

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
    final ProgressDialog preloader = ProgressDialog.show(context, null, "Please wait...", true, false);
    roomService.query(request, new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        roomsAdapter = new RoomsAdapter(Util.toList(response));
        listViewRooms.setAdapter(roomsAdapter);
        preloader.dismiss();
      }

      @Override
      public void onFail(int statusCode, String error) {

      }
    });
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
      context.showRoomFragment(((RoomViewHolder) v.getTag()).id);
    }

    private class RoomViewHolder {
      String id;
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
        viewHolder.txtViewRoomName.setText(Util.getRoomName(room.getJSONObject("users"), session.getUserId()));
        viewHolder.txtViewRoomRecentChat.setText(room.getString("recentChat"));
      } catch (JSONException e) {
        e.printStackTrace();
      }
      return convertView;
    }
  }
}
