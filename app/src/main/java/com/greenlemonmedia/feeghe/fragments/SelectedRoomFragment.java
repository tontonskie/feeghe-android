package com.greenlemonmedia.feeghe.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Spanned;
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
import android.widget.ImageButton;
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

  private String currentRoomId;
  private ListView listViewMessages;
  private Button btnSendNewMessage;
  private EditText editTxtNewMessage;
  private RoomMessagesAdapter roomMessagesAdapter;
  private Session session;
  private MessageService messageService;
  private JSONObject typers = new JSONObject();
  private TextView txtViewTyping;
  private boolean meTyping = false;
  private boolean processingNewMessage = false;
  private Handler typingHandler;
  private Runnable cancelTypingTask;
  private boolean onEndOfList = true;
  private View listViewMessagesFooter;
  private RoomService roomService;
  private CacheCollection messageCacheCollection;
  private CacheCollection roomCacheCollection;
  private JSONObject currentRoom;
  private JSONObject currentRoomUsers;
  private Button btnShowUseFace;
  private FaceService faceService;
  private InputMethodManager newMessageManager;
  private LinearLayout faceSelectionDisplay;
  private UsableFacesAdapter facesAdapter;
  private GridView gridUsableFaces;
  private TextView txtViewRoomTitle;
  private Button btnEditMembers;
  private SelectedRoomUsersModal modalEditUsers;
  private Button btnSendAttachment;
  private GalleryPickerModal modalGallery;
  private AttachedPreviewModal modalAttachedPreview;
  private SelectedFaceModal modalSelectedFace;
  private ImageView imgViewUsers;
  private CacheCollection faceRecentCacheCollection;
  private CacheCollection faceOwnCacheCollection;
  private CacheCollection faceFavCacheCollection;
  private ImageButton btnChatShowRecentFaces;
  private ImageButton btnChatShowOwnFaces;
  private ImageButton btnChatShowFavFaces;
  private ImageButton btnChatShowSearchFaces;
  private EditText editTxtSearchFace;
  private boolean noMorePrevMessages = false;
  private Button btnLoadPrevMessages;
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

    setRoomVars();

    session = Session.getInstance(context);
    faceService = new FaceService(context);
    messageService = new MessageService(context);
    roomService = new RoomService(context);
    roomCacheCollection = roomService.getCacheCollection();

    listViewMessages = (ListView) context.findViewById(R.id.listViewMessages);
    listViewMessages.addFooterView(listViewMessagesFooter);
    txtViewTyping = (TextView) listViewMessagesFooter.findViewById(R.id.txtViewTyping);
    editTxtNewMessage = (EditText) context.findViewById(R.id.txtNewMessage);
    btnSendNewMessage = (Button) context.findViewById(R.id.btnSendNewMessage);
    btnShowUseFace = (Button) context.findViewById(R.id.btnShowUseFace);
    newMessageManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    faceSelectionDisplay = (LinearLayout) context.findViewById(R.id.newMessageOptionsContainer);
    gridUsableFaces = (GridView) context.findViewById(R.id.gridUsableFaces);
    txtViewRoomTitle = (TextView) context.findViewById(R.id.txtViewSelectedRoomTitle);
    btnEditMembers = (Button) context.findViewById(R.id.btnEditSelectedRoomMembers);
    btnSendAttachment = (Button) context.findViewById(R.id.btnSendAttachment);
    imgViewUsers = (ImageView) context.findViewById(R.id.imgViewSelectedRoomProfilePic);
    btnChatShowRecentFaces = (ImageButton) context.findViewById(R.id.btnChatShowRecentFaces);
    btnChatShowOwnFaces = (ImageButton) context.findViewById(R.id.btnChatShowOwnFaces);
    btnChatShowSearchFaces = (ImageButton) context.findViewById(R.id.btnChatSearchFaces);
    btnChatShowFavFaces = (ImageButton) context.findViewById(R.id.btnChatShowFavFaces);
    editTxtSearchFace = (EditText) context.findViewById(R.id.editTxtChatSearchFace);
    btnLoadPrevMessages = (Button) context.findViewById(R.id.btnLoadPrevMessages);

    modalGallery = new GalleryPickerModal(context);
    modalAttachedPreview = new AttachedPreviewModal(this);
    modalSelectedFace = new SelectedFaceModal(context);
    modalEditUsers = new SelectedRoomUsersModal(context);
    modalEditUsers.setData(currentRoom, false);

    setRoomTitle();
    loadMessages();
    loadRecentFaces();

    setupUIEvents();
    setupSocketEvents();
  }

  private void loadRecentFaces() {
    faceRecentCacheCollection = faceService.getCacheCollection("recent");
    final ResponseArray facesFromCache = faceRecentCacheCollection.getData();
    if (facesFromCache.length() != 0) {
      setUsableFaces(facesFromCache);
    }
    faceService.getRecent(new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        if (facesFromCache.length() == 0) {
          setUsableFaces(response);
          faceRecentCacheCollection.save(response.getContent());
        } else {
          JSONArray addedFaces = faceRecentCacheCollection.updateCollection(response).getContent();
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
        Toast.makeText(context, error, Toast.LENGTH_LONG).show();
      }
    });
  }

  private void loadFavFaces() {
    JSONObject faceQuery = null;
    String jsonParamString = "{\"or\":[{\"favoritedBy." + session.getUserId();
    jsonParamString += "\":{\"!\":null}},{\"user\":\"" + session.getUserId() + "\"}]}";
    try {
      faceQuery = faceService.createWhereQuery(new JSONObject(jsonParamString));
    } catch (JSONException e) {
      e.printStackTrace();
    }
    faceFavCacheCollection = faceService.getCacheCollection(faceQuery);
    final ResponseArray facesFromCache = faceFavCacheCollection.getData();
    if (facesFromCache.length() != 0) {
      setUsableFaces(facesFromCache);
    }

    faceService.query(faceQuery, new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        if (facesFromCache.length() == 0) {
          setUsableFaces(response);
          faceFavCacheCollection.save(response.getContent());
        } else {
          JSONArray addedFaces = faceFavCacheCollection.updateCollection(response).getContent();
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

  private void loadOwnFaces() {
    JSONObject query = new JSONObject();
    try {
      query.put("user", session.getUserId());
    } catch (JSONException e) {
      e.printStackTrace();
    }
    faceOwnCacheCollection = faceService.getCacheCollection(query);
    final ResponseArray facesFromCache = faceOwnCacheCollection.getData();
    if (facesFromCache.length() != 0) {
      setUsableFaces(facesFromCache);
    }

    faceService.query(query, new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        if (facesFromCache.length() == 0) {
          setUsableFaces(response);
          faceOwnCacheCollection.save(response.getContent());
        } else {
          JSONArray addedFaces = faceOwnCacheCollection.updateCollection(response).getContent();
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

  private void loadSearchFaces(String text) {
    if (text.isEmpty()) {
      if (facesAdapter != null) {
        facesAdapter.clear();
      }
      return;
    }
    JSONObject query = null;
    try {
      query = faceService.createWhereQuery(
        new JSONObject("{\"or\":[{\"title\":{\"contains\":\"" + text + "\"}},{\"tags\":{\"contains\":\"" + text + "\"}}]}")
      );
    } catch (JSONException e) {
      e.printStackTrace();
    }
    faceService.query(query, new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        setUsableFaces(response);
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
    sendNewMessage(new String[]{APIUtils.getPath(context, uri)});
  }

  private void loadPrevMessages() {
    if (noMorePrevMessages) {
      btnLoadPrevMessages.setVisibility(View.GONE);
      return;
    }
    if (!btnLoadPrevMessages.isEnabled()) {
      return;
    }
    btnLoadPrevMessages.setText("Loading...");
    btnLoadPrevMessages.setEnabled(false);
    JSONObject query = messageService.getCacheQuery(currentRoomId);
    try {
      query.put("skip", roomMessagesAdapter.getCount());
    } catch (JSONException e) {
      e.printStackTrace();
    }
    messageService.query(query, new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        JSONArray messages = response.getContent();
        if (messages.length() == 0) {
          noMorePrevMessages = true;
          btnLoadPrevMessages.setVisibility(View.GONE);
          return;
        }
        try {
          for (int i = 0; i < messages.length(); i++) {
            roomMessagesAdapter.insert(messages.getJSONObject(i), 0);
          }
        } catch (JSONException ex) {
          ex.printStackTrace();
        }
        btnLoadPrevMessages.setText(getResources().getString(R.string.room_show_more_btn));
        btnLoadPrevMessages.setEnabled(true);
      }

      @Override
      public void onFail(int statusCode, String error, JSONObject validationError) {
        Toast.makeText(context, error, Toast.LENGTH_LONG).show();
        btnLoadPrevMessages.setText(getResources().getString(R.string.room_show_more_btn));
        btnLoadPrevMessages.setEnabled(true);
      }
    });
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
        loadRecentFaces();
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

    editTxtNewMessage.addTextChangedListener(new TextWatcher() {

      private ArrayList<ImageSpan> facesToRemove = new ArrayList<>();

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (count > 0) {
          int end = start + count;
          Editable message = editTxtNewMessage.getEditableText();
          ImageSpan[] list = message.getSpans(start, end, ImageSpan.class);
          for (ImageSpan span : list) {
            int spanStart = message.getSpanStart(span);
            int spanEnd = message.getSpanEnd(span);
            if ((spanStart < end) && (spanEnd > start)) {
              facesToRemove.add(span);
            }
          }
        }
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
        Editable message = editTxtNewMessage.getEditableText();
        for (ImageSpan span : facesToRemove) {
          int start = message.getSpanStart(span);
          int end = message.getSpanEnd(span);
          message.removeSpan(span);
          if (start != end) {
            message.delete(start, end);
          }
        }
        facesToRemove.clear();
      }
    });

    btnLoadPrevMessages.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        loadPrevMessages();
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
          btnLoadPrevMessages.setVisibility(View.GONE);
          read();
        } else {
          onEndOfList = false;
          listViewMessages.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
          if (firstVisibleItem == 0 && !noMorePrevMessages) {
            View v = listViewMessages.getChildAt(0);
            int offset = (v == null) ? 0 : v.getTop();
            if (offset == 0) {
              btnLoadPrevMessages.setVisibility(View.VISIBLE);
            }
          } else {
            btnLoadPrevMessages.setVisibility(View.GONE);
          }
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
        if (!faceSelectionDisplay.isShown()) {
          newMessageManager.hideSoftInputFromWindow(editTxtNewMessage.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
          faceSelectionDisplay.setVisibility(View.VISIBLE);
        } else {
          faceSelectionDisplay.setVisibility(View.GONE);
        }
      }
    });

    btnChatShowRecentFaces.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        setFaceTabsInactive();
        btnChatShowRecentFaces.setBackgroundColor(getResources().getColor(R.color.faceChatTabActive));
        loadRecentFaces();
      }
    });

    btnChatShowFavFaces.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        setFaceTabsInactive();
        btnChatShowFavFaces.setBackgroundColor(getResources().getColor(R.color.faceChatTabActive));
        loadFavFaces();
      }
    });

    btnChatShowSearchFaces.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        setFaceTabsInactive();
        btnChatShowSearchFaces.setBackgroundColor(getResources().getColor(R.color.faceChatTabActive));
        editTxtSearchFace.setVisibility(View.VISIBLE);
        editTxtSearchFace.setText("");
        if (facesAdapter != null) {
          facesAdapter.clear();
        }
      }
    });

    btnChatShowOwnFaces.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        setFaceTabsInactive();
        btnChatShowOwnFaces.setBackgroundColor(getResources().getColor(R.color.faceChatTabActive));
        loadOwnFaces();
      }
    });

    final Runnable searchFace = new Runnable() {

      @Override
      public void run() {
        loadSearchFaces(editTxtSearchFace.getText().toString());
      }
    };

    editTxtSearchFace.addTextChangedListener(new TextWatcher() {

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {

      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        typingHandler.removeCallbacks(searchFace);
        typingHandler.postDelayed(searchFace, 500);
      }

      @Override
      public void afterTextChanged(Editable s) {

      }
    });
  }

  private void setFaceTabsInactive() {
    int color = getResources().getColor(R.color.faceChatTabInactive);
    btnChatShowFavFaces.setBackgroundColor(color);
    btnChatShowSearchFaces.setBackgroundColor(color);
    btnChatShowRecentFaces.setBackgroundColor(color);
    btnChatShowOwnFaces.setBackgroundColor(color);
    editTxtSearchFace.setVisibility(View.GONE);
  }

  private APIService.UpdateCallback onVisitListener = new APIService.UpdateCallback() {

    @Override
    public void onSuccess(ResponseObject response) {

    }

    @Override
    public void onFail(int statusCode, String error, JSONObject validationError) {
      Toast.makeText(context, error, Toast.LENGTH_LONG).show();
    }
  };

  private void read() {
    JSONObject room = roomService.getCacheCollection().get(currentRoomId).getContent();
    try {
      room.getJSONObject("users").getJSONObject(session.getUserId()).put("unreadCount", 0);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    roomService.getCacheCollection().replace(currentRoomId, room);
    roomService.visit(currentRoomId, onVisitListener);
  }

  @Override
  protected void setupSocketEvents() {
    Socket.on("message", new APIService.EventCallback() {

      @Override
      public void onEvent(JSONObject evt) {
        try {
          String verb = evt.getString("verb");
          JSONObject data = evt.getJSONObject("data");
          if (data.getString("room").equals(currentRoomId)) {
            if (verb.equals("created")) {

              roomMessagesAdapter.add(data);
              messageCacheCollection.save(data);

            } else if (verb.equals("typing")) {
              updateTypers(data.getJSONObject("user"), data.getBoolean("typing"));
            }
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

            String userId = data.getJSONObject("user").getString("id");
            if (data.getString("room").equals(currentRoomId)
                && !userId.equals(session.getUserId())) {

              String currentUserId = session.getUserId();
              int len = roomMessagesAdapter.getCount();
              for (int i = 0; i < len; i++) {

                JSONObject message = roomMessagesAdapter.getItem(i);
                if (!message.getJSONObject("user").getString("id").equals(currentUserId)) {
                  continue;
                }

                JSONObject statusByUsers = message.optJSONObject("statusByUsers");
                if (statusByUsers != null && statusByUsers.has(userId)) {
                  statusByUsers.put(userId, "read");
                  roomMessagesAdapter.remove(roomMessagesAdapter.getItem(i));
                  roomMessagesAdapter.insert(message, i);
                }
              }
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
  public void onSearchClose() {
    loadMessages();
  }

  @Override
  public void setActionBar() {
    context.showActionBarSelectedRoom();
  }

  private void sendNewMessage(final String[] attachments) {
    String content = editTxtNewMessage.getText().toString();
    if (content.isEmpty() && (attachments == null || attachments.length == 0)) {
      return;
    }

    btnSendNewMessage.setEnabled(false);
    processingNewMessage = true;
    editTxtNewMessage.setText("");
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
      if (APIUtils.isConnected(context)) {
        dataForAppend.put("timestamp", "sending");
      } else {
        dataForAppend.put("timestamp", "failed");
      }
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
        try {

          final String sentMsgTimestamp = sentMessage.getString("timestamp");
          sentMessage.put("user", currentRoomUsers.getJSONObject(sentMessage.getString("user")));
          messageCacheCollection.save(sentMessage);

          if (!sentMessage.isNull("files") && sentMessage.getJSONArray("files").length() > 0) {

            sentMessage.put("timestamp", "uploading...");
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

          JSONObject roomUpdate = new JSONObject();
          roomUpdate.put("recentChat", sentMessage.getString("content"));
          roomUpdate.put("recentChatCreatedAt", sentMessage.getString("timestamp"));
          roomCacheCollection.update(sentMessage.getString("room"), roomUpdate);

        } catch (JSONException ex) {
          ex.printStackTrace();
        }

        roomMessagesAdapter.remove(dataForAppend);
        roomMessagesAdapter.insert(sentMessage, index);
      }

      @Override
      public void onFail(int statusCode, String error, JSONObject validationError) {

      }
    });

    roomMessagesAdapter.add(dataForAppend);
    scrollToEnd();
    btnSendNewMessage.setEnabled(true);
  }

  public void setMessages(ResponseArray response) {
    roomMessagesAdapter = new RoomMessagesAdapter(APIUtils.toList(response));
    listViewMessages.setAdapter(roomMessagesAdapter);
    editTxtNewMessage.setEnabled(true);
  }

  public void setUsableFaces(ResponseArray response) {
    facesAdapter = new UsableFacesAdapter(APIUtils.toList(response));
    gridUsableFaces.setAdapter(facesAdapter);
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
      String faceImgTag = APIUtils.getImageTag(face.id, face.src);
      int start = editTxtNewMessage.getSelectionStart();
      ImageSpan span = new ImageSpan(context, face.img);
      Editable message = editTxtNewMessage.getEditableText();
      message.replace(start, editTxtNewMessage.getSelectionEnd(), faceImgTag);
      message.setSpan(span, start, start + faceImgTag.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
      TextView txtViewOwnMessageTimestamp;
      TextView txtViewMateMessageTimestamp;
      ImageView imgViewAttachment;
      TextView txtViewMessageStatus;
      LinearLayout layoutStatusContainer;
      LinearLayout layoutMessageContainer;
    }

    private void showFaceModal(String faceId) {
      JSONObject face = faceService.getCacheCollection().get(faceId).getContent();
      if (face != null) {
        modalSelectedFace.setData(face);
        modalSelectedFace.show();
      }
      faceService.get(faceId, new APIService.GetCallback() {

        @Override
        public void onSuccess(ResponseObject response) {
          modalSelectedFace.setData(response.getContent());
          if (!modalSelectedFace.isShowing()) {
            modalSelectedFace.show();
          } else {
            modalSelectedFace.render();
          }
        }

        @Override
        public void onFail(int statusCode, String error, JSONObject validationError) {
          Toast.makeText(context, error, Toast.LENGTH_LONG).show();
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
        viewHolder.txtViewOwnMessageTimestamp = (TextView) convertView.findViewById(R.id.txtViewOwnMessageTimestamp);
        viewHolder.txtViewMateMessageTimestamp = (TextView) convertView.findViewById(R.id.txtViewMateMessageTimestamp);
        viewHolder.imgViewAttachment = (ImageView) convertView.findViewById(R.id.imgViewPerChatAttachment);
        viewHolder.txtViewMessageStatus = (TextView) convertView.findViewById(R.id.txtViewMessageStatus);
        viewHolder.layoutMessageContainer = (LinearLayout) convertView.findViewById(R.id.perChatMessageContainer);
        viewHolder.layoutStatusContainer = (LinearLayout) convertView.findViewById(R.id.layoutMessageStatusContainer);
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
        if (position > 0) {
          previousUser = getItem(position - 1).getJSONObject("user");
        }

        JSONObject user = message.getJSONObject("user");
        String userId = user.getString("id");
        if (currentRoom.getBoolean("isGroup") && (previousUser == null || !previousUser.getString("id").equals(userId))) {
          viewHolder.txtViewChatMateName.setVisibility(View.VISIBLE);
          viewHolder.txtViewChatMateName.setText(APIUtils.getFullName(user));
        } else {
          viewHolder.txtViewChatMateName.setVisibility(View.GONE);
        }

        viewHolder.txtViewMateMessageTimestamp.setVisibility(View.GONE);
        viewHolder.txtViewOwnMessageTimestamp.setVisibility(View.GONE);

        LinearLayout.LayoutParams layoutMessageParams = (LinearLayout.LayoutParams) viewHolder.layoutMessageContainer.getLayoutParams();
        if (userId.equals(session.getUserId())) {

          viewHolder.txtViewChatMateName.setGravity(Gravity.RIGHT);
          viewHolder.layoutStatusContainer.setGravity(Gravity.RIGHT);
          viewHolder.layoutMessageContainer.setBackgroundResource(R.drawable.per_chat_own);
          viewHolder.txtViewPerChatContent.setTextColor(context.getResources().getColor(R.color.perChatContentOwn));
          viewHolder.txtViewMessageStatus.setVisibility(View.VISIBLE);
          viewHolder.txtViewOwnMessageTimestamp.setVisibility(View.VISIBLE);
          viewHolder.txtViewOwnMessageTimestamp.setText(message.getString("timestamp"));

          JSONObject statusByUsers = message.optJSONObject("statusByUsers");
          boolean isRead = true;
          if (statusByUsers != null) {
            Iterator<String> i = statusByUsers.keys();
            while (i.hasNext()) {
              if (!statusByUsers.getString((String) i.next()).equals("read")) {
                isRead = false;
                break;
              }
            }
          } else {
            isRead = false;
          }

          if (!isRead) {
            String ts = message.getString("timestamp");
            if (ts.equals("sending")) {
              viewHolder.txtViewMessageStatus.setTextColor(Color.YELLOW);
            } else if (ts.equals("failed")) {
              viewHolder.txtViewMessageStatus.setTextColor(Color.RED);
            } else {
              viewHolder.txtViewMessageStatus.setTextColor(Color.GREEN);
            }
          } else {
            viewHolder.txtViewMessageStatus.setTextColor(Color.DKGRAY);
          }

          layoutMessageParams.gravity = Gravity.RIGHT;

        } else {

          viewHolder.txtViewChatMateName.setGravity(Gravity.LEFT);
          viewHolder.layoutStatusContainer.setGravity(Gravity.LEFT);
          viewHolder.layoutMessageContainer.setBackgroundResource(R.drawable.per_chat_received);
          viewHolder.txtViewPerChatContent.setTextColor(context.getResources().getColor(R.color.perChatContentReceived));
          viewHolder.txtViewMessageStatus.setVisibility(View.GONE);
          viewHolder.txtViewMateMessageTimestamp.setVisibility(View.VISIBLE);
          viewHolder.txtViewMateMessageTimestamp.setText(message.getString("timestamp"));

          layoutMessageParams.gravity = Gravity.LEFT;
        }

        viewHolder.txtViewPerChatContent.setText("Loading...");
        viewHolder.txtViewPerChatContent.setVisibility(View.VISIBLE);
        viewHolder.imgViewAttachment.setVisibility(View.GONE);
        viewHolder.layoutMessageContainer.setLayoutParams(layoutMessageParams);

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
          viewHolder.txtViewPerChatContent.setText(APIUtils.sanitizeMessage(message.getString("content")));
        } else {
          viewHolder.txtViewPerChatContent.setText("");
        }

        if (position >= (getCount() - 1)) {
          read();
        }

      } catch (JSONException e) {
        e.printStackTrace();
      }

      return convertView;
    }
  }
}
