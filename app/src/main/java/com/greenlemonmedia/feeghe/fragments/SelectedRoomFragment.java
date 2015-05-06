package com.greenlemonmedia.feeghe.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.R;
import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.APIUtils;
import com.greenlemonmedia.feeghe.api.CacheCollection;
import com.greenlemonmedia.feeghe.api.FaceService;
import com.greenlemonmedia.feeghe.api.MessageService;
import com.greenlemonmedia.feeghe.api.ResponseArray;
import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.RoomService;
import com.greenlemonmedia.feeghe.api.Socket;
import com.greenlemonmedia.feeghe.modals.AttachedPreviewModal;
import com.greenlemonmedia.feeghe.modals.GalleryPickerModal;
import com.greenlemonmedia.feeghe.modals.MainActivityModal;
import com.greenlemonmedia.feeghe.modals.SelectedFaceModal;
import com.greenlemonmedia.feeghe.modals.SelectedRoomUsersModal;
import com.greenlemonmedia.feeghe.storage.Session;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class SelectedRoomFragment extends MainActivityFragment implements MainActivityFragment.AttachmentListing {

  private static final int UPLOAD_FILE = 1;

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
  private boolean meTyping = false;
  private boolean processingNewMessage = false;
  private Handler typingHandler;
  private Runnable cancelTypingTask;
  private JSONObject seenBy = new JSONObject();
  private TextView txtViewSeenBy;
  private boolean hasNewMessage = false;
  private boolean onEndOfList = true;
  private View listViewMessagesFooter;
  private RoomService roomService;
  private CacheCollection messageCacheCollection;
  private JSONObject currentRoom;
  private CacheCollection roomCacheCollection;
  private JSONObject currentRoomUsers;
  private Button btnShowUseFace;
  private FaceService faceService;
  private InputMethodManager newMessageManager;
  private LinearLayout newMessageOptionDisplay;
  private CacheCollection faceCacheCollection;
  private UsableFacesAdapter facesAdapter;
  private GridView gridUsableFaces;
  private Button btnCloseOptionDisplay;
  private TextView txtViewRoomTitle;
  private Button btnEditMembers;
  private SelectedRoomUsersModal modalEditUsers;
  private Button btnSendAttachment;
  private GalleryPickerModal modalGallery;
  private AttachedPreviewModal modalAttachedPreview;
  private SelectedFaceModal modalSelectedFace;
  private ImageView imgViewUsers;
  private HashMap<Integer, JSONObject> roomAttachments = new HashMap<>();

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

  private void setRoomTitle() {
    String roomName = currentRoom.optString("name");
    if (currentRoom.isNull("name") || roomName.isEmpty()) {
      roomName = APIUtils.getRoomName(currentRoomUsers, session.getUserId());
    }
    txtViewRoomTitle.setText(roomName);
    try {
      Iterator<String> i = currentRoomUsers.keys();
      while (i.hasNext()) {
        String userId = (String) i.next();
        if (!userId.equals(session.getUserId())) {
          APIUtils.getPicasso(context)
            .load(Uri.parse(APIUtils.getStaticUrl(currentRoomUsers.getJSONObject(userId).getJSONObject("profilePic").getString("small"))))
            .placeholder(R.drawable.placeholder)
            .into(imgViewUsers);
        }
      }
    } catch (JSONException e) {
      APIUtils.getPicasso(context)
        .load(R.drawable.placeholder)
        .into(imgViewUsers);
      e.printStackTrace();
    }
  }

  private void setRoomVars(JSONObject room) {
    try {
      currentRoom = room;
      currentRoomId = currentRoom.getString("id");
      currentRoomUsers = currentRoom.getJSONObject("users");
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  private void setRoomVars() {
    try {
      setRoomVars(new JSONObject(getArguments().getString("roomInfo")));
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  public void onActivityCreated(Bundle savedInstance) {
    super.onActivityCreated(savedInstance);
    context = getCurrentActivity();
    setRoomVars();

    session = Session.getInstance(context);
    faceService = new FaceService(context);
    messageService = new MessageService(context);
    roomService = new RoomService(context);
    roomCacheCollection = roomService.getCacheCollection();

    listViewMessages = (ListView) context.findViewById(R.id.listViewMessages);
    listViewMessages.addFooterView(listViewMessagesFooter);
    txtViewTyping = (TextView) listViewMessagesFooter.findViewById(R.id.txtViewTyping);
    txtViewSeenBy = (TextView) listViewMessagesFooter.findViewById(R.id.txtViewSeenBy);
    txtNewMessage = (EditText) context.findViewById(R.id.txtNewMessage);
    btnSendNewMessage = (Button) context.findViewById(R.id.btnSendNewMessage);
    btnShowUseFace = (Button) context.findViewById(R.id.btnShowUseFace);
    newMessageManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    newMessageOptionDisplay = (LinearLayout) context.findViewById(R.id.newMessageOptionDisplay);
    gridUsableFaces = (GridView) context.findViewById(R.id.gridUsableFaces);
    btnCloseOptionDisplay = (Button) context.findViewById(R.id.btnCloseOptionDisplay);
    txtViewRoomTitle = (TextView) context.findViewById(R.id.txtViewSelectedRoomTitle);
    btnEditMembers = (Button) context.findViewById(R.id.btnEditSelectedRoomMembers);
    btnSendAttachment = (Button) context.findViewById(R.id.btnSendAttachment);
    imgViewUsers = (ImageView) context.findViewById(R.id.imgViewSelectedRoomProfilePic);

    modalGallery = new GalleryPickerModal(context);
    modalAttachedPreview = new AttachedPreviewModal(this);
    modalSelectedFace = new SelectedFaceModal(context);
    modalEditUsers = new SelectedRoomUsersModal(context);
    modalEditUsers.setData(currentRoom, false);

    setRoomTitle();
    loadMessages();
    loadUsableFaces();

    setupUIEvents();
    setupSocketEvents();
  }

  private void loadUsableFaces() {
    JSONObject faceQuery = null;
    String jsonParamString = "{\"or\":[{\"favoritedBy." + session.getUserId();
    jsonParamString += "\":{\"!\":null}},{\"user\":\"" + session.getUserId() + "\"}]}";
    try {
      faceQuery = faceService.createWhereQuery(new JSONObject(jsonParamString));
    } catch (JSONException e) {
      e.printStackTrace();
    }
    faceCacheCollection = faceService.getCacheCollection(faceQuery);
    final ResponseArray facesFromCache = faceCacheCollection.getData();
    if (facesFromCache.length() != 0) {
      setUsableFaces(facesFromCache);
    }

    faceService.query(faceQuery, new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        if (facesFromCache.length() == 0) {
          setUsableFaces(response);
          faceCacheCollection.save(response.getContent());
        } else {
          JSONArray addedFaces = faceCacheCollection.updateCollection(response).getContent();
          facesAdapter.clear();
          int addedFacesLength = addedFaces.length();
          try {
            for (int i = 0; i < addedFacesLength; i++) {
              facesAdapter.add(addedFaces.getJSONObject(i));
            }
          } catch (JSONException ex) {
            ex.printStackTrace();
          }
        }
      }

      @Override
      public void onFail(int statusCode, String error, JSONObject validationError) {
        Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
      }
    });
  }

  private void loadMessages() {
    JSONObject messageQuery = messageService.getCacheQuery(currentRoomId);
    messageCacheCollection = messageService.getCacheCollection(messageQuery);
    final ResponseArray responseFromCache = messageCacheCollection.getData();
    if (responseFromCache.length() > 0) {
      setMessages(responseFromCache);
    }

    messageService.query(messageQuery, new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        if (responseFromCache.length() == 0) {
          setMessages(response);
          messageCacheCollection.save(response.getContent());
        } else {
          JSONArray newMessages = response.getContent();
          messageCacheCollection.updateCollection(newMessages);
          roomMessagesAdapter.clear();
          try {
            for (int i = 0; i < newMessages.length(); i++) {
              roomMessagesAdapter.add(newMessages.getJSONObject(i));
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
  public void onActivityResult(int reqCode, int resCode, Intent data) {
    super.onActivityResult(reqCode, resCode, data);
    if (resCode == Activity.RESULT_OK && reqCode == UPLOAD_FILE) {
      uploadFile(data.getData());
    }
    btnSendAttachment.setEnabled(true);
  }

  private void uploadFile(Uri uri) {
//    String[] fields= { MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID };
//    Cursor cursor = context.getContentResolver().query(uri, fields, null, null, null);
//    cursor.moveToFirst();
//    cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
    sendNewMessage(new String[] { APIUtils.getPath(context, uri) });
  }

  @Override
  protected void setupUIEvents() {
    btnSendAttachment.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
//        modalGallery.show();
        btnSendAttachment.setEnabled(false);
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), UPLOAD_FILE);
      }
    });

    modalEditUsers.setOnDataChangedListener(new MainActivityModal.OnDataChangedListener() {

      @Override
      public void onChanged(Object oldRoomData, Object newRoomData) {
        JSONObject newRoom = (JSONObject) newRoomData;
        setRoomVars(newRoom);
        setRoomTitle();
        try {
          if (((JSONObject) oldRoomData).getBoolean("isGroup") != newRoom.getBoolean("isGroup")) {
            loadMessages();
          }
        } catch (JSONException e) {
          e.printStackTrace();
        }
        loadUsableFaces();
      }
    });

    btnEditMembers.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        modalEditUsers.show();
      }
    });

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
        sendNewMessage(null);
      }
    });

    btnShowUseFace.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        newMessageManager.hideSoftInputFromWindow(txtNewMessage.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        newMessageOptionDisplay.setVisibility(View.VISIBLE);
      }
    });

    btnCloseOptionDisplay.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        newMessageOptionDisplay.setVisibility(View.GONE);
      }
    });
  }

  @Override
  protected void setupSocketEvents() {
    Socket.on("message", new APIService.EventCallback() {

      @Override
      public void onEvent(JSONObject evt) {
        try {
          String verb = evt.getString("verb");
          JSONObject data = evt.getJSONObject("data");
          if (!data.getString("room").equals(currentRoomId)) {
            return;
          }
          if (verb.equals("created")) {
            hasNewMessage = true;
            addToListViewMesages(data);
          } else if (verb.equals("typing")) {
            updateTypers(data.getJSONObject("user"), data.getBoolean("typing"));
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
          JSONObject data = evt.getJSONObject("data");
          if (verb.equals("seen")) {
            JSONObject user = data.getJSONObject("user");
            if (data.getString("room").equals(currentRoomId) && !user.getString("id").equals(session.getUserId())) {
              updateSeenBy(user);
            }
          }
        } catch (JSONException e) {
          e.printStackTrace();
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
    return MainActivity.FRAG_SELECTED_ROOM;
  }

  @Override
  public boolean onSearchQuerySubmit(String q) {
    JSONObject query = new JSONObject();
    try {
      query.put("room", currentRoomId);
      query.put("content", new JSONObject("{\"contains\":\"" + q + "\"}"));
    } catch (JSONException e) {
      e.printStackTrace();
    }
    final ProgressDialog searchPreloader = APIUtils.showPreloader(context);
    messageService.query(messageService.createWhereQuery(query), new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        JSONArray msgs = response.getContent();
        roomMessagesAdapter.clear();
        try {
          for (int i = 0; i < msgs.length(); i++) {
            roomMessagesAdapter.add(msgs.getJSONObject(i));
          }
        } catch (JSONException ex) {
          ex.printStackTrace();
        }
        searchPreloader.dismiss();
      }

      @Override
      public void onFail(int statusCode, String error, JSONObject validationError) {

      }
    });
    return true;
  }

  @Override
  public boolean onSearchQueryChange(String query) {
    return false;
  }

  @Override
  public boolean onSearchClose() {
    loadMessages();
    return true;
  }

  private void sendNewMessage(final String[] attachments) {
    String content = txtNewMessage.getText().toString();
    if (content.isEmpty() && (attachments == null || attachments.length == 0)) {
      return;
    }

    btnSendNewMessage.setEnabled(false);
    processingNewMessage = true;
    txtNewMessage.setText("");
    processingNewMessage = false;
    JSONObject newMessage = new JSONObject();

    final JSONArray newMsgAttachments = new JSONArray();
    final String tmpMessageId = "tmp-" + APIUtils.createUniqueCode();
    final JSONObject dataForAppend = new JSONObject();
    try {

      newMessage.put("user", session.getUserId());
      newMessage.put("room", currentRoomId);

      dataForAppend.put("user", session.getCurrentUser().toJSON());
      dataForAppend.put("room", currentRoomId);
      dataForAppend.put("timestamp", "Sending...");
      dataForAppend.put("id", tmpMessageId);

      if (attachments != null && attachments.length > 0) {
        for (int i = 0; i < attachments.length; i++) {
          newMsgAttachments.put(APIUtils.generateFilename(attachments[i]));
        }
        newMessage.put("files", newMsgAttachments);
        dataForAppend.put("files", newMsgAttachments);
        newMessage.put("content", null);
        dataForAppend.put("content", null);
      } else {
        newMessage.put("content", content);
        dataForAppend.put("content", content);
      }

    } catch (JSONException ex) {
      ex.printStackTrace();
    }

    messageService.socketSave(newMessage, new APIService.SocketCallback() {

      @Override
      public void onSuccess(ResponseObject response) {
        int index = roomMessagesAdapter.getPosition(dataForAppend);
        if (index < 0) {
          return;
        }
        final JSONObject sentMessage = response.getContent();
        JSONObject roomUpdate = new JSONObject();
        try {

          final String sentMsgTimestamp = sentMessage.getString("timestamp");
          sentMessage.put("user", currentRoomUsers.getJSONObject(sentMessage.getString("user")));
          messageCacheCollection.save(sentMessage);
          roomUpdate.put("recentChat", sentMessage.getString("content"));

          if (!sentMessage.isNull("files") && sentMessage.getJSONArray("files").length() > 0) {

            sentMessage.put("timestamp", "Uploading...");
            APIService.SaveCallback onUploadComplete = new APIService.SaveCallback() {

              @Override
              public void onSuccess(ResponseObject response) {
                int pos = roomMessagesAdapter.getPosition(sentMessage);
                if (pos < 0) {
                  return;
                }
                roomMessagesAdapter.remove(sentMessage);
                try {
                  sentMessage.put("timestamp", sentMsgTimestamp);
                } catch (JSONException e) {
                  e.printStackTrace();
                }
                roomMessagesAdapter.insert(sentMessage, pos);
              }

              @Override
              public void onFail(int statusCode, String error, JSONObject validationError) {
                Toast.makeText(context, statusCode + ": " + error, Toast.LENGTH_LONG).show();
              }
            };

            APIService.UploadProgressListener onProgress = new APIService.UploadProgressListener() {

              @Override
              public void onProgress(int completed) {
//                int pos = roomMessagesAdapter.getPosition(sentMessage);
//                if (pos < 0) {
//                  return;
//                }
//                roomMessagesAdapter.remove(sentMessage);
//                try {
//                  sentMessage.put("progress", completed);
//                } catch (JSONException e) {
//                  e.printStackTrace();
//                }
//                roomMessagesAdapter.insert(sentMessage, pos);
              }
            };

            JSONArray sentMsgAttachments = sentMessage.getJSONArray("files");
            for (int i = 0; i < sentMsgAttachments.length(); i++) {
              messageService.upload(sentMessage.getString("id"), newMsgAttachments.getString(i), attachments[i], onProgress, onUploadComplete);
            }
          }
        } catch (JSONException ex) {
          ex.printStackTrace();
        }

        roomCacheCollection.update(currentRoomId, roomUpdate);
        roomMessagesAdapter.remove(dataForAppend);
        roomMessagesAdapter.insert(sentMessage, index);
      }

      @Override
      public void onFail(int statusCode, String error, JSONObject validationError) {

      }
    });

    addToListViewMesages(dataForAppend);
    scrollToEnd();
    btnSendNewMessage.setEnabled(true);
  }

  public void setMessages(ResponseArray response) {
    roomMessagesAdapter = new RoomMessagesAdapter(APIUtils.toList(response));
    listViewMessages.setAdapter(roomMessagesAdapter);
    txtNewMessage.setEnabled(true);
  }

  public void setUsableFaces(ResponseArray response) {
    facesAdapter = new UsableFacesAdapter(APIUtils.toList(response));
    gridUsableFaces.setAdapter(facesAdapter);
  }

  public void setSeenBy(JSONObject users) {
    LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) txtViewSeenBy.getLayoutParams();
    layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
    txtViewSeenBy.setLayoutParams(layoutParams);
    txtViewSeenBy.setText("Seen by " + APIUtils.getRoomName(users, session.getUserId()));
  }

  public void updateSeenBy(JSONObject user) {
    try {
      String userId = user.getString("id");
      seenBy.put(userId, user);
      currentRoomUsers.getJSONObject(userId).put("unreadCount", 0);
      roomCacheCollection.replace(currentRoomId, currentRoom);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) txtViewSeenBy.getLayoutParams();
    layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
    txtViewSeenBy.setLayoutParams(layoutParams);
    txtViewSeenBy.setText("Seen by " + APIUtils.getRoomName(seenBy, session.getUserId()));
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
    roomCacheCollection.replace(currentRoomId, currentRoom);
    LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) txtViewSeenBy.getLayoutParams();
    layoutParams.height = 0;
    txtViewSeenBy.setLayoutParams(layoutParams);
    txtViewSeenBy.setText("");
  }

  public void updateTypers(JSONObject user, boolean isTyping) {
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
      txtViewTyping.setText(APIUtils.getRoomName(typers, session.getUserId()) + suffix);
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

  @Override
  public JSONObject getAttachedItem(int position) {
    return roomAttachments.get(position);
  }

  @Override
  public int getAttachmentsCount() {
    return roomAttachments.size();
  }

  private class UsableFacesAdapter extends ArrayAdapter<JSONObject> implements View.OnClickListener {

    public UsableFacesAdapter(ArrayList<JSONObject> faces) {
      super(context, R.layout.per_usable_face, faces);
    }

    @Override
    public void onClick(View v) {
      FaceImageTag face = (FaceImageTag) v.getTag();
      int cursorPos = txtNewMessage.getSelectionStart();
      SpannableStringBuilder message = new SpannableStringBuilder(txtNewMessage.getText());

      String faceImgTag = APIUtils.getImageTag(face.id, face.src);
      message.insert(cursorPos, faceImgTag);
      message.setSpan(
        new ImageSpan(context, face.img),
        cursorPos,
        cursorPos + faceImgTag.length(),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
      );

      txtNewMessage.setText(message);
    }

    private class UsableFaceViewHolder {
      public ImageView imgViewUsableFace;
    }

    private class FaceImageTag {
      public String id;
      public Bitmap img;
      public String src;
    }

    private class FaceImageTarget implements Target {

      private ImageView faceImgView;
      private FaceImageTag faceImgTag;

      public FaceImageTarget(ImageView imgView, FaceImageTag imgTag) {
        faceImgView = imgView;
        faceImgTag = imgTag;
      }

      @Override
      public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        faceImgTag.img = bitmap;
        faceImgView.setTag(faceImgTag);
        faceImgView.setImageBitmap(bitmap);
      }

      @Override
      public void onBitmapFailed(Drawable errorDrawable) {

      }

      @Override
      public void onPrepareLoad(Drawable placeHolderDrawable) {

      }
    }

    public View getView(int position, View convertView, ViewGroup parent) {
      UsableFaceViewHolder viewHolder;
      if (convertView == null) {
        LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = vi.inflate(R.layout.per_usable_face, null);
        viewHolder = new UsableFaceViewHolder();
        viewHolder.imgViewUsableFace = (ImageView) convertView.findViewById(R.id.imgViewUsableFace);
        viewHolder.imgViewUsableFace.setOnClickListener(this);
        convertView.setTag(viewHolder);
      } else {
        viewHolder = (UsableFaceViewHolder) convertView.getTag();
      }

      JSONObject faceInfo = getItem(position);
      FaceImageTag faceImgTag = new FaceImageTag();
      try {
        faceImgTag.id = faceInfo.getString("id");
        faceImgTag.src = faceInfo.getJSONObject("photo").getString("small");
      } catch (JSONException e) {
        e.printStackTrace();
      }

      APIUtils.getPicasso(context)
        .load(Uri.parse(APIUtils.getStaticUrl(faceImgTag.src)))
        .into(new FaceImageTarget(viewHolder.imgViewUsableFace, faceImgTag));

      return convertView;
    }
  }

  private class RoomMessagesAdapter extends ArrayAdapter<JSONObject> implements View.OnClickListener {

    public RoomMessagesAdapter(ArrayList<JSONObject> messages) {
      super(context, R.layout.per_chat, messages);

    }

    @Override
    public void onClick(View v) {
      switch (v.getId()) {
        case R.id.imgViewPerChatAttachment:
          showAttachment((ImageView) v);
          break;
      }
    }

    public void showAttachment(ImageView v) {
      modalAttachedPreview.setData(v.getTag());
      modalAttachedPreview.show();
    }

    private class MessageViewHolder {
      TextView txtViewPerChatContent;
      TextView txtViewChatMateName;
      TextView txtViewMessageTimestamp;
      ImageView imgViewAttachment;
    }

    private void showFaceModal(String faceId) {
      final ProgressDialog faceLoading = APIUtils.showPreloader(context);
      faceService.get(faceId, new APIService.GetCallback() {

        @Override
        public void onSuccess(ResponseObject response) {
          faceLoading.dismiss();
          modalSelectedFace.setData(response.getContent());
          modalSelectedFace.show();
        }

        @Override
        public void onFail(int statusCode, String error, JSONObject validationError) {
          Toast.makeText(context, error, Toast.LENGTH_LONG).show();
          faceLoading.dismiss();
        }
      });
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
      final MessageViewHolder viewHolder;
      if (convertView == null) {
        LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = vi.inflate(R.layout.per_chat, null);
        viewHolder = new MessageViewHolder();
        viewHolder.txtViewChatMateName = (TextView) convertView.findViewById(R.id.txtViewChatMateName);
        viewHolder.txtViewPerChatContent = (TextView) convertView.findViewById(R.id.txtViewPerChatContent);
        viewHolder.txtViewMessageTimestamp = (TextView) convertView.findViewById(R.id.txtViewMessageTimestamp);
        viewHolder.imgViewAttachment = (ImageView) convertView.findViewById(R.id.imgViewPerChatAttachment);
        viewHolder.imgViewAttachment.setOnClickListener(this);
        viewHolder.txtViewPerChatContent.setMovementMethod(LinkMovementMethod.getInstance());
        convertView.setTag(viewHolder);
      } else {
        viewHolder = (MessageViewHolder) convertView.getTag();
      }

      final JSONObject message = getItem(position);
      boolean isTmpMsg = false;
      try {

        if (message.getString("id").indexOf("tmp-") == 0) {
          isTmpMsg = true;
        }

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
          viewHolder.txtViewChatMateName.setText(APIUtils.getFullName(user));
        } else {
          viewHolder.txtViewChatMateName.setVisibility(View.GONE);
        }

//        if (nextUser == null || !nextUser.getString("id").equals(userId)) {
//          viewHolder.txtViewMessageTimestamp.setVisibility(View.VISIBLE);
          viewHolder.txtViewMessageTimestamp.setText(message.getString("timestamp"));
//        } else {
//          viewHolder.txtViewMessageTimestamp.setVisibility(View.GONE);
//        }

        LinearLayout.LayoutParams imgLayoutParams = (LinearLayout.LayoutParams) viewHolder.imgViewAttachment.getLayoutParams();
        LinearLayout.LayoutParams txtViewLayoutParams = (LinearLayout.LayoutParams) viewHolder.txtViewPerChatContent.getLayoutParams();
        if (userId.equals(session.getUserId())) {
          viewHolder.txtViewChatMateName.setGravity(Gravity.RIGHT);
          viewHolder.txtViewPerChatContent.setGravity(Gravity.RIGHT);
          viewHolder.txtViewPerChatContent.setBackgroundResource(R.drawable.per_chat_own);
          viewHolder.txtViewPerChatContent.setTextColor(context.getResources().getColor(R.color.perChatContentOwn));
          viewHolder.txtViewMessageTimestamp.setGravity(Gravity.RIGHT);
          txtViewLayoutParams.gravity = Gravity.RIGHT;
          imgLayoutParams.gravity = Gravity.RIGHT;
        } else {
          viewHolder.txtViewChatMateName.setGravity(Gravity.LEFT);
          viewHolder.txtViewPerChatContent.setGravity(Gravity.LEFT);
          viewHolder.txtViewPerChatContent.setBackgroundResource(R.drawable.per_chat_received);
          viewHolder.txtViewPerChatContent.setTextColor(context.getResources().getColor(R.color.perChatContentReceived));
          viewHolder.txtViewMessageTimestamp.setGravity(Gravity.LEFT);
          txtViewLayoutParams.gravity = Gravity.LEFT;
          imgLayoutParams.gravity = Gravity.LEFT;
        }

        viewHolder.txtViewPerChatContent.setText("Loading...");
        viewHolder.txtViewPerChatContent.setVisibility(View.VISIBLE);
        viewHolder.imgViewAttachment.setVisibility(View.GONE);
        viewHolder.imgViewAttachment.setLayoutParams(imgLayoutParams);
        viewHolder.txtViewPerChatContent.setLayoutParams(txtViewLayoutParams);

        if (!message.isNull("files") && message.getJSONArray("files").length() > 0) {

          viewHolder.txtViewPerChatContent.setVisibility(View.GONE);
          viewHolder.imgViewAttachment.setVisibility(View.VISIBLE);

          if (!isTmpMsg) {
            JSONArray attached = message.getJSONArray("files");
            for (int i = 0; i < attached.length(); i++) {
              JSONObject sizes = attached.getJSONObject(i);
              viewHolder.imgViewAttachment.setTag(position + i);
              APIUtils.getPicasso(context)
                .load(Uri.parse(APIUtils.getStaticUrl(sizes.getString("small"))))
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .into(viewHolder.imgViewAttachment);
              roomAttachments.put(position + i, sizes);
            }
          }

        } else if (!message.isNull("faces") && message.getJSONArray("faces").length() > 0) {

          APIUtils.loadFacesFromMessage(
            context,
            message.getString("content"),
            viewHolder.txtViewPerChatContent,
            new APIUtils.OnFaceClickListener() {

              @Override
              public void onClick(View widget, String faceId) {
                showFaceModal(faceId);
              }
            }
          );

        } else if (!message.isNull("content")) {
          viewHolder.txtViewPerChatContent.setText(message.getString("content"));
        } else {
          viewHolder.txtViewPerChatContent.setText("");
        }

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
