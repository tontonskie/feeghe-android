package com.greenlemonmedia.feeghe.fragments;

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
import com.greenlemonmedia.feeghe.api.Util;
import com.greenlemonmedia.feeghe.storage.Session;
import com.greenlemonmedia.feeghe.tasks.GetRoomsTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class RoomsFragment extends MainActivityFragment {

  private MainActivity context;
  private ArrayList<JSONObject> roomsList;
  private RoomsAdapter roomsAdapter;
  private ListView listViewRooms;
  private Session session;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_rooms, container, false);
  }

  public void onActivityCreated(Bundle savedInstance) {
    super.onActivityCreated(savedInstance);
    context = getCurrentActivity();
    session = Session.getInstance(context);
    listViewRooms = (ListView) context.findViewById(R.id.listViewRooms);

    GetRoomsTask getRooms = new GetRoomsTask(
      context,
      new GetRoomsTask.Listener() {

        @Override
        public void onSuccess(ArrayList<JSONObject> rooms) {
          roomsList = rooms;
          roomsAdapter = new RoomsAdapter(roomsList);
          listViewRooms.setAdapter(roomsAdapter);
        }

        @Override
        public void onFail(int statusCode, String error) {

        }
      }
    );
    getRooms.execute();
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
