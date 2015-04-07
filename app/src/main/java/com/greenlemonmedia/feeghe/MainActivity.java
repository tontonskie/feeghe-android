package com.greenlemonmedia.feeghe;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabWidget;

import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.CacheCollection;
import com.greenlemonmedia.feeghe.api.MessageService;
import com.greenlemonmedia.feeghe.api.RoomService;
import com.greenlemonmedia.feeghe.api.Socket;
import com.greenlemonmedia.feeghe.api.UserService;
import com.greenlemonmedia.feeghe.api.APIUtils;
import com.greenlemonmedia.feeghe.fragments.ContactsFragment;
import com.greenlemonmedia.feeghe.fragments.UploadFragment;
import com.greenlemonmedia.feeghe.fragments.WallOfFacesFragment;
import com.greenlemonmedia.feeghe.fragments.MainActivityFragment;
import com.greenlemonmedia.feeghe.fragments.RoomsFragment;
import com.greenlemonmedia.feeghe.fragments.NewUserFragment;
import com.greenlemonmedia.feeghe.fragments.SelectedRoomFragment;
import com.greenlemonmedia.feeghe.storage.Session;
import com.koushikdutta.async.http.socketio.DisconnectCallback;
import com.koushikdutta.async.http.socketio.ErrorCallback;
import com.koushikdutta.async.http.socketio.ReconnectCallback;
import com.koushikdutta.async.http.socketio.SocketIOClient;
import com.koushikdutta.async.http.socketio.SocketIORequest;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends ActionBarActivity implements TabHost.OnTabChangeListener {

  private Activity context;
  private UserService userService;
  private Session session;
  private Session.User currentUser;
  private TabHost tabHost;
  private boolean isManualTabChange = false;
  private String currentFragmentTabId;
  private SoundPool soundPool;
  private int alertSoundId;
  private CacheCollection roomCacheCollection;
  private RoomService roomService;
  private TabWidget tabs;
  private MessageService messageService;
  private String currentFragmentId;

  public static final String TAB_WALL_OF_FACES = "wall_of_faces";
  public static final String TAB_MESSAGES = "messages";
  public static final String TAB_CONTACTS = "contacts";
  public static final String TAB_UPLOAD = "upload";

  public static final String FRAG_SELECTED_ROOM = "selected_room_fragment";
  public static final String FRAG_ROOMS = "rooms_fragment";
  public static final String FRAG_CONTACTS = "contacts_fragment";
  public static final String FRAG_UPLOAD = "upload_fragment";
  public static final String FRAG_NEW_USER = "new_user_fragment";
  public static final String FRAG_WALL_OF_FACES = "wall_of_faces_fragment";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    context = this;
    session = Session.getInstance(context);
    userService = new UserService(context);
    messageService = new MessageService(context);
    roomService = new RoomService(context);
    roomCacheCollection = roomService.getCacheCollection();

    if (!session.isLoggedIn()) {
      backToRegistration();
      return;
    }

    setupTabs();
    setupSounds();
    setupSocketConnection();
    setupKeyboardDetection();
  }

  private void setupTabs() {
    tabHost = (TabHost) findViewById(R.id.tabHost);
    tabHost.setup();

    TabHost.TabSpec tabMessages = tabHost.newTabSpec(TAB_MESSAGES);
    tabMessages.setContent(new TabContent());
    tabMessages.setIndicator("Messages");

    TabHost.TabSpec tabContacts = tabHost.newTabSpec(TAB_CONTACTS);
    tabContacts.setContent(new TabContent());
    tabContacts.setIndicator("Contacts");

//    TabHost.TabSpec tabUpload = tabHost.newTabSpec(TAB_UPLOAD);
//    tabUpload.setContent(new TabContent());
//    tabUpload.setIndicator("Upload");

    TabHost.TabSpec tabWallOfFaces = tabHost.newTabSpec(TAB_WALL_OF_FACES);
    tabWallOfFaces.setContent(new TabContent());
    tabWallOfFaces.setIndicator("Faces");

    tabHost.addTab(tabMessages);
    tabHost.addTab(tabContacts);
//    tabHost.addTab(tabUpload);
    tabHost.addTab(tabWallOfFaces);
    tabHost.setOnTabChangedListener(this);
    tabs = tabHost.getTabWidget();
  }

  private void setupSounds() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      AudioAttributes audioAttr = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build();
      soundPool = new SoundPool.Builder().setMaxStreams(5).setAudioAttributes(audioAttr).build();
    } else {
      soundPool = new SoundPool(5, AudioManager.STREAM_NOTIFICATION, 0);
    }
    alertSoundId = soundPool.load(this, R.raw.alert, 1);
  }

  private void setupSocketConnection() {
    final ProgressDialog preloader = APIUtils.showPreloader(this);
    Socket.connect(session, new Socket.SocketConnectionListener() {

      @Override
      public void onStartConnecting(SocketIORequest request) {

      }

      @Override
      public void onConnect(SocketIOClient client) {
        currentUser = userService.getCurrentUser();
        preloader.dismiss();
        if (currentUser.hasStatus(Session.User.STATUS_INCOMPLETE)) {
          showNewUserFragment();
        } else {
          showMessagesFragment();
        }
        initSocketEvents();
      }
    });
  }

  private void setupKeyboardDetection() {
    final LinearLayout container = (LinearLayout) findViewById(R.id.container);
    container.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

      @Override
      public void onGlobalLayout() {
        if ((container.getRootView().getHeight() - container.getHeight()) > container.getRootView().getHeight() / 3) {
          tabs.setVisibility(View.GONE);
        } else {
          tabs.setVisibility(View.VISIBLE);
        }
      }
    });
  }

  private void initSocketEvents() {
    Socket.on("room", new APIService.EventCallback() {

      @Override
      public void onEvent(JSONObject evt) {
        try {
          String verb = evt.getString("verb");
          JSONObject data = evt.getJSONObject("data");
          if (verb.equals("created")) {
            roomCacheCollection.save(data);
          }
        } catch (JSONException ex) {
          ex.printStackTrace();
        }
      }
    });

    Socket.on("message", new APIService.EventCallback() {

      @Override
      public void onEvent(JSONObject evt) {
        try {
          String verb = evt.getString("verb");
          JSONObject message = evt.getJSONObject("data");
          if (verb.equals("created")) {
            playAlertSound();
            JSONObject roomUpdate = new JSONObject();
            roomUpdate.put("recentChat", message.getString("content"));
            roomCacheCollection.update(message.getString("room"), roomUpdate);
            messageService.getCacheCollection(messageService.getCacheQuery(message.getString("room"))).save(message);
          }
        } catch (JSONException ex) {
          ex.printStackTrace();
        }
      }
    });

    SocketIOClient socketClient = Socket.getClient();
    socketClient.setDisconnectCallback(new DisconnectCallback() {

      @Override
      public void onDisconnect(Exception e) {
        if (e != null) {
          Log.d("socket disconnect", e.getMessage());
        }
      }
    });

    socketClient.setErrorCallback(new ErrorCallback() {

      @Override
      public void onError(String error) {
        Log.d("socket error", error);
      }
    });

    socketClient.setReconnectCallback(new ReconnectCallback() {

      @Override
      public void onReconnect() {
        Log.d("socket reconnect", "reconnect");
      }
    });
  }

  @Override
  public void onTabChanged(String tabId) {
    if (!isManualTabChange) {
      switch (tabId) {
        case TAB_WALL_OF_FACES:
          showWallOfFacesFragment();
          break;
        case TAB_MESSAGES:
          showMessagesFragment();
          break;
        case TAB_CONTACTS:
          showContactsFragment();
          break;
        case TAB_UPLOAD:
          showUploadFragment();
          break;
      }
    }
  }

  public void setCurrentTab(String tabId) {
    isManualTabChange = true;
    tabHost.setCurrentTabByTag(tabId);
    isManualTabChange = false;
  }

  public void setCurrentFragmentId(String id) {
    currentFragmentId = id;
  }

  public void playAlertSound() {
    soundPool.play(alertSoundId, 1f, 1f, 1, 0, 1);
  }

  private void showFragment(MainActivityFragment fragment) {
    showFragment(fragment, true);
  }

  private void showFragment(MainActivityFragment fragment, boolean withBackStack) {
    FragmentManager fm = getFragmentManager();
    FragmentTransaction ft = fm.beginTransaction();
    ft.replace(android.R.id.tabcontent, fragment);
    if (withBackStack && currentFragmentTabId != null) {
      ft.addToBackStack(currentFragmentTabId);
    }
    currentFragmentTabId = fragment.getTabId();
    ft.commit();
  }

  public void showUploadFragment() {
    showFragment(new UploadFragment());
  }

  public void showRoomFragment(JSONObject roomInfo) {
    Bundle args = new Bundle();
    args.putString("roomInfo", roomInfo.toString());
    SelectedRoomFragment frag = new SelectedRoomFragment();
    frag.setArguments(args);
    showFragment(frag);
    setCurrentTab(TAB_MESSAGES);
  }

  public void showNewUserFragment() {
    showFragment(new NewUserFragment(), false);
  }

  public void showWallOfFacesFragment() {
    showFragment(new WallOfFacesFragment());
  }

  public void showMessagesFragment(boolean withBackStack) {
    showFragment(new RoomsFragment(), withBackStack);
  }

  public void showMessagesFragment() {
    showFragment(new RoomsFragment());
  }

  public void showContactsFragment() {
    showFragment(new ContactsFragment());
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    return super.onOptionsItemSelected(item);
  }

  public void onBackPressed() {
    FragmentManager fm = getFragmentManager();
    if (fm.getBackStackEntryCount() > 0) {
      String fragTabId = fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1).getName();
      setCurrentTab(fragTabId);
      fm.popBackStack();
      currentFragmentTabId = fragTabId;
      return;
    }
    finish();
  }

  public void backToRegistration() {
    startActivity(new Intent(this, RegisterActivity.class));
  }

  private class TabContent implements TabHost.TabContentFactory {

    @Override
    public View createTabContent(String tag) {
      return new View(context);
    }
  }
}
