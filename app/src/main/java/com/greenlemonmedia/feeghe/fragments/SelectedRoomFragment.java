package com.greenlemonmedia.feeghe.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.R;
import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.CacheCollection;
import com.greenlemonmedia.feeghe.api.CacheEntry;
import com.greenlemonmedia.feeghe.api.MessageService;
import com.greenlemonmedia.feeghe.api.ResponseArray;
import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.RoomService;
import com.greenlemonmedia.feeghe.api.Socket;
import com.greenlemonmedia.feeghe.api.Util;
import com.greenlemonmedia.feeghe.storage.Session;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class SelectedRoomFragment extends MainActivityFragment {

  private MainActivity context;
  private String currentRoomId;
  private ListView listViewMessages;
  private Button btnSendNewMessage;
  private EditText txtNewMessage;
  private RoomMessagesAdapter roomMessagesAdapter;
  private Session session;
  private MessageService messageService;
  private JSONObject typers = new JSONObject();
  private TextView txtViewTyping;
  private Boolean meTyping = false;
  private Boolean processingNewMessage = false;
  private Handler typingHandler;
  private Runnable cancelTypingTask;
  private JSONObject seenBy = new JSONObject();
  private TextView txtViewSeenBy;
  private Boolean hasNewMessage = false;
  private Boolean onEndOfList = true;
  private View listViewMessagesFooter;
  private RoomService roomService;
  private CacheCollection messageCacheCollection;
  private JSONObject currentRoom;
  private CacheEntry roomCacheEntry;
  private JSONObject currentRoomUsers;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    listViewMessagesFooter = inflater.inflate(R.layout.room_messages_footer, null, false);
    return inflater.inflate(R.layout.fragment_selected_room, container, false);
  }

  public void scrollToEnd() {
    listViewMessages.setSelection(roomMessagesAdapter.getCount() - 1);
  }

  public void addToListViewMesages(JSONObject message) {
    roomMessagesAdapter.add(message);
    clearSeenBy();
  }

  public void onActivityCreated(Bundle savedInstance) {
    super.onActivityCreated(savedInstance);
    context = getCurrentActivity();
    try {
      currentRoom = new JSONObject(getArguments().getString("roomInfo"));
      currentRoomId = currentRoom.getString("id");
      currentRoomUsers = currentRoom.getJSONObject("users");
    } catch (JSONException e) {
      e.printStackTrace();
    }

    session = Session.getInstance(context);
    messageService = new MessageService(context);
    roomService = new RoomService(context);
    roomCacheEntry = roomService.getCacheEntry(currentRoomId);

    listViewMessages = (ListView) context.findViewById(R.id.listViewMessages);
    listViewMessages.addFooterView(listViewMessagesFooter);
    txtViewTyping = (TextView) listViewMessagesFooter.findViewById(R.id.txtViewTyping);
    txtViewSeenBy = (TextView) listViewMessagesFooter.findViewById(R.id.txtViewSeenBy);
    txtNewMessage = (EditText) context.findViewById(R.id.txtNewMessage);
    btnSendNewMessage = (Button) context.findViewById(R.id.btnSendNewMessage);

    JSONObject query = new JSONObject();
    try {
      query.put("room", currentRoomId);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    query = messageService.createWhereQuery(query);
    messageCacheCollection = messageService.getCacheCollection(query);
    ResponseArray response = messageCacheCollection.getContent();
    if (response.getContent().length() == 0) {
      messageService.query(query, new APIService.QueryCallback() {

        @Override
        public void onSuccess(ResponseArray response) {
          showMessages(response);
          messageCacheCollection.save(response.getContent());
        }

        @Override
        public void onFail(int statusCode, String error) {
          Toast.makeText(context, "Code: " + statusCode + " " + error, Toast.LENGTH_LONG).show();
        }
      });
    } else {
      showMessages(response);
    }

    typingHandler = new Handler();
    cancelTypingTask = new Runnable() {

      @Override
      public void run() {
        if (meTyping) {
          meTyping = false;
          messageService.typing(currentRoomId, false);
        }
      }
    };

    txtNewMessage.addTextChangedListener(new TextWatcher() {

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {

      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (!processingNewMessage) {
          if (!meTyping) {
            meTyping = true;
            messageService.typing(currentRoomId, true);
          }
          typingHandler.removeCallbacks(cancelTypingTask);
          typingHandler.postDelayed(cancelTypingTask, 1000);
        }
      }

      @Override
      public void afterTextChanged(Editable s) {

      }
    });

    listViewMessages.setOnScrollListener(new ListView.OnScrollListener() {

      @Override
      public void onScrollStateChanged(AbsListView view, int scrollState) {

      }

      @Override
      public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if ((firstVisibleItem + visibleItemCount) >= totalItemCount) {
          onEndOfList = true;
          listViewMessages.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
          if (hasNewMessage) {
            roomService.visit(currentRoomId, null);
            hasNewMessage = false;
          }
        } else {
          onEndOfList = false;
          listViewMessages.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        }
      }
    });

    btnSendNewMessage.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        String content = txtNewMessage.getText().toString();
        if (content.isEmpty()) {
          return;
        }

        btnSendNewMessage.setEnabled(false);
        processingNewMessage = true;
        txtNewMessage.setText("");
        processingNewMessage = false;
        JSONObject newMessage = new JSONObject();

        final String tmpMessageId = "tmp-" + Util.createUniqueCode();
        final JSONObject dataForAppend = new JSONObject();
        try {

          newMessage.put("content", content);
          newMessage.put("user", session.getUserId());
          newMessage.put("room", currentRoomId);

          dataForAppend.put("content", content);
          dataForAppend.put("user", session.getCurrentUser().toJSON());
          dataForAppend.put("room", currentRoomId);
          dataForAppend.put("timestamp", "Sending...");
          dataForAppend.put("id", tmpMessageId);

        } catch (JSONException ex) {
          ex.printStackTrace();
        }

        messageService.socketSave(newMessage, new APIService.SocketCallback() {

          @Override
          public void onSuccess(final ResponseObject response) {
            final int index = roomMessagesAdapter.getPosition(dataForAppend);
            if (index < 0) {
              Toast.makeText(context, "Sent message position not found", Toast.LENGTH_LONG).show();
              return;
            }
            messageCacheCollection.save(response.getContent());
            context.runOnUiThread(new Runnable() {

              @Override
              public void run() {
                roomMessagesAdapter.remove(dataForAppend);
                roomMessagesAdapter.insert(response.getContent(), index);
              }
            });
          }

          @Override
          public void onFail(int statusCode, String error) {
            Toast.makeText(context, "Code: " + statusCode + " " + error, Toast.LENGTH_LONG).show();
          }
        });

        addToListViewMesages(dataForAppend);
        scrollToEnd();
        btnSendNewMessage.setEnabled(true);
      }
    });

    Socket.on("message", new APIService.EventCallback() {

      @Override
      public void onEvent(JSONObject evt) {
        try {
          String verb = evt.getString("verb");
          final JSONObject data = evt.getJSONObject("data");
          if (!data.getString("room").equals(currentRoomId)) {
            return;
          }
          if (verb.equals("created")) {
            messageCacheCollection.save(data);
            context.runOnUiThread(new Runnable() {

              @Override
              public void run() {
                hasNewMessage = true;
                addToListViewMesages(data);
              }
            });
          } else if (verb.equals("typing")) {
            context.runOnUiThread(new Runnable() {

              @Override
              public void run() {
                try {
                  updateTypers(data.getJSONObject("user"), data.getBoolean("typing"));
                } catch (JSONException e) {
                  e.printStackTrace();
                }
              }
            });
          }
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    });

    Socket.on("room", new APIService.EventCallback() {

      @Override
      public void onEvent(JSONObject evt) {
        try {
          String verb = evt.getString("verb");
          final JSONObject data = evt.getJSONObject("data");
          if (verb.equals("seen")) {
            final JSONObject user = data.getJSONObject("user");
            if (data.getString("room").equals(currentRoomId) && !user.getString("id").equals(session.getUserId())) {
              context.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                  updateSeenBy(user);
                }
              });
            }
          }
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    });
  }

  public void showMessages(ResponseArray response) {
    roomMessagesAdapter = new RoomMessagesAdapter(Util.toList(response));
    listViewMessages.setAdapter(roomMessagesAdapter);
    txtNewMessage.setEnabled(true);
  }

  public void setSeenBy(JSONObject users) {
    LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) txtViewSeenBy.getLayoutParams();
    layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
    txtViewSeenBy.setLayoutParams(layoutParams);
    txtViewSeenBy.setText("Seen by " + Util.getRoomName(users, session.getUserId()));
  }

  public void updateSeenBy(JSONObject user) {
    try {
      String userId = user.getString("id");
      seenBy.put(userId, user);
      currentRoomUsers.getJSONObject(userId).put("unreadCount", 0);
      roomCacheEntry.update(currentRoom);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) txtViewSeenBy.getLayoutParams();
    layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
    txtViewSeenBy.setLayoutParams(layoutParams);
    txtViewSeenBy.setText("Seen by " + Util.getRoomName(seenBy, session.getUserId()));
    if (onEndOfList) {
      scrollToEnd();
    }
  }

  public void clearSeenBy() {
    seenBy = new JSONObject();
    Iterator<String> i = currentRoomUsers.keys();
    try {
      JSONObject roomUser;
      while (i.hasNext()) {
        roomUser = currentRoomUsers.getJSONObject((String) i.next());
        roomUser.put("unreadCount", roomUser.getInt("unreadCount") + 1);
      }
    } catch (JSONException ex) {
      ex.printStackTrace();
    }
    roomCacheEntry.update(currentRoom);
    LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) txtViewSeenBy.getLayoutParams();
    layoutParams.height = 0;
    txtViewSeenBy.setLayoutParams(layoutParams);
    txtViewSeenBy.setText("");
  }

  public void updateTypers(JSONObject user, Boolean isTyping) {
    try {
      String typerUserId = user.getString("id");
      if (isTyping) {
        typers.put(typerUserId, user);
      } else {
        typers.remove(typerUserId);
      }
    } catch (JSONException ex) {
      ex.printStackTrace();
    }
    int typersCount = typers.length();
    LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) txtViewTyping.getLayoutParams();
    if (typersCount > 0) {
      String suffix = " is typing...";
      if (typersCount > 1) {
        suffix = " are typing...";
      }
      txtViewTyping.setText(Util.getRoomName(typers, session.getUserId()) + suffix);
      layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
    } else {
      txtViewTyping.setText("");
      layoutParams.height = 0;
    }
    txtViewTyping.setLayoutParams(layoutParams);
    if (onEndOfList) {
      scrollToEnd();
    }
  }

  @Override
  public String getTabId() {
    return MainActivity.TAB_MESSAGES;
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
//        if ((nextUser == null || !nextUser.getString("id").equals(userId)) && !userId.equals(session.getUserId())) {
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

        if (position == (getCount() - 1)) {
          setSeenBy(currentRoom.getJSONObject("users"));
        }

      } catch (JSONException e) {
        e.printStackTrace();
      }

      return convertView;
    }
  }
}
