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
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.R;
import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.MessageService;
import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.Socket;
import com.greenlemonmedia.feeghe.api.Util;
import com.greenlemonmedia.feeghe.storage.Session;
import com.greenlemonmedia.feeghe.tasks.GetMessagesTask;
import com.greenlemonmedia.feeghe.tasks.ReadNewMessageTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

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

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
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
    currentRoomId = getArguments().getString("id");

    messageService = new MessageService(context);
    session = Session.getInstance(context);
    listViewMessages = (ListView) context.findViewById(R.id.listViewMessages);
    txtNewMessage = (EditText) context.findViewById(R.id.txtNewMessage);
    btnSendNewMessage = (Button) context.findViewById(R.id.btnSendNewMessage);
    txtViewTyping = (TextView) context.findViewById(R.id.txtViewTyping);
    txtViewSeenBy = (TextView) context.findViewById(R.id.txtViewSeenBy);

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
        if ((firstVisibleItem + visibleItemCount) == totalItemCount) {
          listViewMessages.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
          if (hasNewMessage) {
            ReadNewMessageTask readNewTask = new ReadNewMessageTask(
              context,
              currentRoomId,
              new ReadNewMessageTask.Listener() {

                @Override
                public void onSuccess(ResponseObject response) {

                }

                @Override
                public void onFail(int statusCode, String error) {

                }
              }
            );
            readNewTask.execute();
            hasNewMessage = false;
          }
        } else {
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

        messageService.socketSave(newMessage, new APIService.Callback() {

          @Override
          public void onSuccess(final ResponseObject response) {
            final int index = roomMessagesAdapter.getPosition(dataForAppend);
            if (index < 0) {
              Toast.makeText(context, "Sent message position not found", Toast.LENGTH_LONG).show();
              return;
            }
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

    GetMessagesTask getMessages = new GetMessagesTask(
      context,
      currentRoomId,
      new GetMessagesTask.Listener() {

        @Override
        public void onSuccess(ArrayList<JSONObject> messages) {
          roomMessagesAdapter = new RoomMessagesAdapter(messages);
          listViewMessages.setAdapter(roomMessagesAdapter);
          txtNewMessage.setEnabled(true);
        }

        @Override
        public void onFail(int statusCode, String error) {
          Toast.makeText(context, "Code: " + statusCode + " " + error, Toast.LENGTH_LONG).show();
        }
      }
    );
    getMessages.execute();
  }

  public void updateSeenBy(JSONObject user) {
    try {
      seenBy.put(user.getString("id"), user);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) txtViewSeenBy.getLayoutParams();
    layoutParams.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
    txtViewSeenBy.setLayoutParams(layoutParams);
    txtViewSeenBy.setText("Seen by " + Util.getRoomName(seenBy, session.getUserId()));
  }

  public void clearSeenBy() {
    seenBy = new JSONObject();
    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) txtViewSeenBy.getLayoutParams();
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
    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) txtViewTyping.getLayoutParams();
    if (typersCount > 0) {
      String suffix = " is typing...";
      if (typersCount > 1) {
        suffix = " are typing...";
      }
      txtViewTyping.setText(Util.getRoomName(typers, session.getUserId()) + suffix);
      layoutParams.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
    } else {
      txtViewTyping.setText("");
      layoutParams.height = 0;
    }
    txtViewTyping.setLayoutParams(layoutParams);
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

      } catch (JSONException e) {
        e.printStackTrace();
      }

      return convertView;
    }
  }
}
