package com.greenlemonmedia.feeghe.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.R;
import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.MessageService;
import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.Socket;
import com.greenlemonmedia.feeghe.storage.Session;
import com.greenlemonmedia.feeghe.tasks.GetMessagesTask;
import com.koushikdutta.async.http.socketio.Acknowledge;
import com.koushikdutta.async.http.socketio.EventCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class RoomFragment extends MainActivityFragment {

  private MainActivity context;
  private String currentRoomId;
  private ListView listViewMessages;
  private Button btnSendNewMessage;
  private EditText txtNewMessage;
  private ArrayList<JSONObject> messages;
  private RoomMessagesAdapter roomMessagesAdapter;
  private Session session;
  private MessageService messageService;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_room, container, false);
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
    btnSendNewMessage.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        JSONObject newMessage = new JSONObject();
        JSONObject dataForAppend = new JSONObject();
        String content = txtNewMessage.getText().toString();
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

        messageService.socketSave(dataForAppend, new APIService.Callback() {

          @Override
          public void onSuccess(ResponseObject response) {

          }

          @Override
          public void onFail(int statusCode, String error) {

          }
        });
      }
    });

    Socket.getClient().on("message", new EventCallback() {

      @Override
      public void onEvent(JSONArray argument, Acknowledge acknowledge) {
        JSONObject evt = null;
        try {
          evt = argument.getJSONObject(0);
        } catch (JSONException e) {
          e.printStackTrace();
        }
        messages.add(evt);
        roomMessagesAdapter.notifyDataSetChanged();
      }
    });

    GetMessagesTask getMessages = new GetMessagesTask(
      context,
      currentRoomId,
      new GetMessagesTask.Listener() {

        @Override
        public void onSuccess(ArrayList<JSONObject> messages) {
          roomMessagesAdapter = new RoomMessagesAdapter(messages);
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
      ImageView imgViewProfileImg;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
      MessageViewHolder viewHolder;
      if (convertView == null) {
        LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = vi.inflate(R.layout.per_chat, null);
        viewHolder = new MessageViewHolder();
        viewHolder.imgViewProfileImg = (ImageView) convertView.findViewById(R.id.imgViewProfileImg);
        viewHolder.txtViewChatMateName = (TextView) convertView.findViewById(R.id.txtViewChatMateName);
        viewHolder.txtViewPerChatContent = (TextView) convertView.findViewById(R.id.txtViewPerChatContent);
        viewHolder.txtViewPerChatContent.setOnLongClickListener(this);
        convertView.setTag(viewHolder);
      } else {
        viewHolder = (MessageViewHolder) convertView.getTag();
      }
      JSONObject message = getItem(position);
      try {
        JSONObject user = message.getJSONObject("user");
        String firstName = user.getString("firstName");
        String lastName = user.getString("lastName");
        if (!firstName.isEmpty() && !lastName.isEmpty() && !user.isNull("firstName") && !user.isNull("lastName")) {
          viewHolder.txtViewChatMateName.setText(firstName + " " + lastName);
        } else {
          viewHolder.txtViewChatMateName.setText(user.getString("phoneNumber"));
        }
        viewHolder.txtViewPerChatContent.setText(Html.fromHtml(message.getString("content")), TextView.BufferType.SPANNABLE);
        viewHolder.txtViewPerChatContent.setTag(message.getString("id"));
//        viewHolder.imgViewProfileImg.setImageURI(Util.toImageURI(user.getJSONObject("profilePic"), "small"));
      } catch (JSONException e) {
        e.printStackTrace();
      }
      return convertView;
    }
  }
}
