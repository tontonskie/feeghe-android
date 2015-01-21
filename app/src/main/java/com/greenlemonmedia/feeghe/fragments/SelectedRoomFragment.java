package com.greenlemonmedia.feeghe.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.R;
import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.MessageService;
import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.Socket;
import com.greenlemonmedia.feeghe.api.Util;
import com.greenlemonmedia.feeghe.storage.Session;
import com.greenlemonmedia.feeghe.tasks.GetMessagesTask;
import com.koushikdutta.async.http.socketio.Acknowledge;
import com.koushikdutta.async.http.socketio.EventCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class SelectedRoomFragment extends MainActivityFragment {

  private MainActivity context;
  private String currentRoomId;
  private ListView listViewMessages;
  private Button btnSendNewMessage;
  private EditText txtNewMessage;
  private ArrayList<JSONObject> listMessages;
  private RoomMessagesAdapter roomMessagesAdapter;
  private Session session;
  private MessageService messageService;
  private Boolean scrollEnd = true;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_selected_room, container, false);
  }

  public void scrollToEnd() {
    if (scrollEnd) {
      Log.d("test", "scrolled to end");
      listViewMessages.setSelection(roomMessagesAdapter.getCount() - 1);
    }
  }

  public void addToListViewMesages(JSONObject message) {
    listMessages.add(message);
    roomMessagesAdapter.notifyDataSetChanged();
    scrollToEnd();
  }

  public void onActivityCreated(Bundle savedInstance) {
    super.onActivityCreated(savedInstance);
    context = getCurrentActivity();
    currentRoomId = getArguments().getString("id");

    messageService = new MessageService(context);
    session = Session.getInstance(context);
    listViewMessages = (ListView) context.findViewById(R.id.listViewMessages);
    txtNewMessage = (EditText) context.findViewById(R.id.txtNewMessage);
    btnSendNewMessage = (Button) context.findViewById(R.id.btnSendNewMessage);

    listViewMessages.setOnScrollListener(new ListView.OnScrollListener() {

      @Override
      public void onScrollStateChanged(AbsListView view, int scrollState) {

      }

      @Override
      public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if ((firstVisibleItem + visibleItemCount) == totalItemCount) {
          scrollEnd = true;
        } else {
          scrollEnd = false;
        }
        Log.d("test", scrollEnd.toString());
      }
    });

    final APIService.Callback sendNewMessageCb = new APIService.Callback() {

      @Override
      public void onSuccess(ResponseObject response) {
        
      }

      @Override
      public void onFail(int statusCode, String error) {

      }
    };

    btnSendNewMessage.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        JSONObject newMessage = new JSONObject();
        JSONObject dataForAppend = new JSONObject();
        String content = txtNewMessage.getText().toString();
        txtNewMessage.setText("");
        try {

          newMessage.put("content", content);
          newMessage.put("user", session.getUserId());
          newMessage.put("room", currentRoomId);

          dataForAppend.put("content", content);
          dataForAppend.put("user", session.getCurrentUser().toJSON());
          dataForAppend.put("room", currentRoomId);

        } catch (JSONException ex) {
          ex.printStackTrace();
        }

        messageService.socketSave(newMessage, sendNewMessageCb);
        addToListViewMesages(dataForAppend);
      }
    });

    Socket.getClient().on("message", new EventCallback() {

      @Override
      public void onEvent(JSONArray argument, Acknowledge acknowledge) {
        String verb = "";
        JSONObject data = null;
        try {
          JSONObject evt = argument.getJSONObject(0);
          verb = evt.getString("verb");
          data = evt.getJSONObject("data");
        } catch (JSONException e) {
          e.printStackTrace();
        }
        if (verb.equals("created")) {
          addToListViewMesages(data);
        } else if (verb.equals("typing")) {

        }
      }
    });

    GetMessagesTask getMessages = new GetMessagesTask(
      context,
      currentRoomId,
      new GetMessagesTask.Listener() {

        @Override
        public void onSuccess(ArrayList<JSONObject> messages) {
          listMessages = messages;
          roomMessagesAdapter = new RoomMessagesAdapter(listMessages);
          listViewMessages.setAdapter(roomMessagesAdapter);
        }

        @Override
        public void onFail(int statusCode, String error) {

        }
      }
    );
    getMessages.execute();
  }

  private class RoomMessagesAdapter extends ArrayAdapter<JSONObject> implements View.OnLongClickListener {

    public RoomMessagesAdapter(ArrayList<JSONObject> messages) {
      super(context, R.layout.per_chat, messages);
    }

    @Override
    public boolean onLongClick(View v) {
      return false;
    }

    private class MessageViewHolder {
      TextView txtViewPerChatContent;
      TextView txtViewChatMateName;
      TextView txtViewMessageTimestamp;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
      MessageViewHolder viewHolder;
      if (convertView == null) {
        LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = vi.inflate(R.layout.per_chat, null);
        viewHolder = new MessageViewHolder();
        viewHolder.txtViewChatMateName = (TextView) convertView.findViewById(R.id.txtViewChatMateName);
        viewHolder.txtViewPerChatContent = (TextView) convertView.findViewById(R.id.txtViewPerChatContent);
        viewHolder.txtViewMessageTimestamp = (TextView) convertView.findViewById(R.id.txtViewMessageTimestamp);
        viewHolder.txtViewPerChatContent.setOnLongClickListener(this);
        convertView.setTag(viewHolder);
      } else {
        viewHolder = (MessageViewHolder) convertView.getTag();
      }

      JSONObject message = getItem(position);
      try {

        JSONObject previousUser = null;
        JSONObject nextUser = null;
        if (position > 0) {
          previousUser = getItem(position - 1).getJSONObject("user");
        }
        if (position < (getCount() - 1)) {
          nextUser = getItem(position + 1).getJSONObject("user");
        }

        JSONObject user = message.getJSONObject("user");
        String userId = user.getString("id");
        if (previousUser == null || !previousUser.getString("id").equals(userId)) {
          viewHolder.txtViewChatMateName.setVisibility(View.VISIBLE);
          viewHolder.txtViewChatMateName.setText(Util.getFullName(user));
        } else {
          viewHolder.txtViewChatMateName.setVisibility(View.GONE);
        }

        if (nextUser == null || !nextUser.getString("id").equals(userId)) {
          viewHolder.txtViewMessageTimestamp.setVisibility(View.VISIBLE);
          viewHolder.txtViewMessageTimestamp.setText(message.getString("timestamp"));
        } else {
          viewHolder.txtViewMessageTimestamp.setVisibility(View.GONE);
        }

        if (userId.equals(session.getUserId())) {
          viewHolder.txtViewChatMateName.setGravity(Gravity.RIGHT);
          viewHolder.txtViewPerChatContent.setGravity(Gravity.RIGHT);
          viewHolder.txtViewMessageTimestamp.setGravity(Gravity.RIGHT);
        } else {
          viewHolder.txtViewChatMateName.setGravity(Gravity.LEFT);
          viewHolder.txtViewPerChatContent.setGravity(Gravity.LEFT);
          viewHolder.txtViewMessageTimestamp.setGravity(Gravity.LEFT);
        }

        viewHolder.txtViewPerChatContent.setText(Html.fromHtml(message.getString("content")), TextView.BufferType.SPANNABLE);
        viewHolder.txtViewPerChatContent.setTag(message.getString("id"));

      } catch (JSONException e) {
        e.printStackTrace();
      }

      return convertView;
    }
  }
}
