package com.greenlemonmedia.feeghe;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.CacheCollection;
import com.greenlemonmedia.feeghe.api.ContactService;
import com.greenlemonmedia.feeghe.api.MessageService;
import com.greenlemonmedia.feeghe.api.ResponseArray;
import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.RoomService;
import com.greenlemonmedia.feeghe.api.Socket;
import com.greenlemonmedia.feeghe.api.UserService;
import com.greenlemonmedia.feeghe.api.APIUtils;
import com.greenlemonmedia.feeghe.fragments.ContactsFragment;
import com.greenlemonmedia.feeghe.fragments.EditProfileFragment;
import com.greenlemonmedia.feeghe.fragments.UploadFragment;
import com.greenlemonmedia.feeghe.fragments.WallOfFacesFragment;
import com.greenlemonmedia.feeghe.fragments.MainActivityFragment;
import com.greenlemonmedia.feeghe.fragments.RoomsFragment;
import com.greenlemonmedia.feeghe.fragments.NewUserFragment;
import com.greenlemonmedia.feeghe.fragments.SelectedRoomFragment;
import com.greenlemonmedia.feeghe.storage.Session;
import com.greenlemonmedia.feeghe.tasks.GetPhoneContactsTask;
import com.greenlemonmedia.feeghe.tasks.RegisterIDTask;
import com.greenlemonmedia.feeghe.ui.UITabHost;
import com.koushikdutta.async.http.socketio.DisconnectCallback;
import com.koushikdutta.async.http.socketio.ErrorCallback;
import com.koushikdutta.async.http.socketio.ReconnectCallback;
import com.koushikdutta.async.http.socketio.SocketIOClient;
import com.koushikdutta.async.http.socketio.SocketIORequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends ActionBarActivity implements UITabHost.OnTabChangeListener {

  private MainActivity context;
  private UserService userService;
  private Session session;
  private Session.User currentUser;
  private UITabHost tabHost;
  private boolean isManualTabChange = false;
  private String currentFragmentTabId;
  private SoundPool soundPool;
  private int alertSoundId;
  private CacheCollection roomCacheCollection;
  private RoomService roomService;
  private TabWidget tabs;
  private MessageService messageService;
  private String currentFragmentId;
  private ListView listViewSettings;
  private ActionBarDrawerToggle toggleSettings;
  private DrawerLayout drawerSettings;
  private MainActivityFragment currentFragment;
  private SearchView searchView;
  private ContactService contactService;
  private PhoneContactsObserver phoneContactsObserver;
  private Toolbar toolbar;
  private Spinner spinActionBar;
  private ActionBar actionBar;
  private MenuItem menuItemSearch;
  private LinearLayout selectedRoomTitleContainer;
  private TextView txtViewActionBarTitle;
  private ViewPager viewPager;
  private String[] tabTags = {
    TAB_MESSAGES,
    TAB_CONTACTS,
    TAB_WALL_OF_FACES
  };

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
  public static final String FRAG_EDIT_PROFILE = "edit_profile_fragment";

  private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (!checkPlayServices()) {
      return;
    }

    context = this;
    session = Session.getInstance(context);
    if (!session.isLoggedIn()) {
      backToRegistration();
      return;
    }

    userService = new UserService(context);
    messageService = new MessageService(context);
    roomService = new RoomService(context);
    contactService = new ContactService(context);
    roomCacheCollection = roomService.getCacheCollection();

    if (session.get(Session.REG_ID) == null) {
      new RegisterIDTask(context).execute();
    }

    setContentView(R.layout.activity_main);

    setupViewPager();
    setupTabs();
    setupNavDrawer();
    setupSounds();
    setupSocketConnection();
    setupKeyboardDetection();

    registerObservers();
  }

  private void setupViewPager() {

  }

  @Override
  public void onNewIntent(Intent intent) {
    Bundle bundle = intent.getExtras();
    if (bundle == null) {
      return;
    }

    if (bundle.containsKey("room")) {
      JSONObject room = roomCacheCollection.get(bundle.getString("room")).getContent();
      if (room == null) {
        final ProgressDialog preloader = APIUtils.showPreloader(context);
        roomService.get(bundle.getString("room"), new APIService.GetCallback() {

          @Override
          public void onSuccess(ResponseObject response) {
            preloader.dismiss();
            showRoomFragment(response.getContent());
          }

          @Override
          public void onFail(int statusCode, String error, JSONObject validationError) {
            preloader.dismiss();
            Toast.makeText(context, error, Toast.LENGTH_LONG).show();
          }
        });
      } else {
        showRoomFragment(room);
      }
      return;
    }
  }

  public boolean checkPlayServices() {
    int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
    if (resultCode != ConnectionResult.SUCCESS) {
      if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
        GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
      } else {
        Toast.makeText(context, "Play services is not supported", Toast.LENGTH_LONG).show();
        finish();
      }
      return false;
    }
    return true;
  }

  @Override
  protected void onResume() {
    super.onResume();
    checkPlayServices();
  }

  private void registerObservers() {
    phoneContactsObserver = new PhoneContactsObserver();
    getContentResolver().registerContentObserver(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        true,
        phoneContactsObserver
    );
  }

  private class PhoneContactsObserver extends ContentObserver {

    public PhoneContactsObserver() {
      super(null);
    }

    public void onChange(boolean selfChange) {
      super.onChange(selfChange);
      updateContacts();
    }
  }

  public void updateContacts() {
    GetPhoneContactsTask.GetPhoneContactsListener listener = new GetPhoneContactsTask.GetPhoneContactsListener() {

      @Override
      public void onSuccess(JSONArray contacts) {
        JSONObject query = new JSONObject();
        try {
          query.put("phoneNumber", contacts);
        } catch (JSONException e) {
          e.printStackTrace();
        }
        userService.query(userService.createWhereQuery(query), new APIService.QueryCallback() {

          @Override
          public void onSuccess(ResponseArray response) {
            JSONArray userIds = new JSONArray();
            JSONArray users = response.getContent();
            try {
              for (int i = 0; i < users.length(); i++) {
                userIds.put(users.getJSONObject(i).getString("id"));
              }
            } catch (JSONException ex) {
              ex.printStackTrace();
            }
            contactService.sync(userIds, new APIService.SaveCallback() {

              @Override
              public void onSuccess(ResponseObject response) {
                JSONArray syncedContacts = null;
                try {
                  syncedContacts = response.getContent().getJSONArray("contacts");
                } catch (JSONException e) {
                  e.printStackTrace();
                }
                contactService.getCacheCollection().updateCollection(syncedContacts);
              }

              @Override
              public void onFail(int statusCode, String error, JSONObject validationError) {
                Toast.makeText(context, "Failed to sync contacts", Toast.LENGTH_LONG).show();
              }
            });
          }

          @Override
          public void onFail(int statusCode, String error, JSONObject validationError) {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show();
          }
        });
      }
    };
    new GetPhoneContactsTask(context, listener).execute();
  }

  private void setupNavDrawer() {
    String[] settingsList = getResources().getStringArray(R.array.settings_list);
    drawerSettings = (DrawerLayout) findViewById(R.id.drawerLayoutSettings);
    listViewSettings = (ListView) findViewById(R.id.drawerListSettings);
    listViewSettings.setAdapter(new ArrayAdapter(this, R.layout.per_settings_item, settingsList));
    listViewSettings.setOnItemClickListener(new ListView.OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
          case 0:
            break;
          case 1:
            showEditProfileFragment();
            break;
        }
        drawerSettings.closeDrawer(GravityCompat.END);
      }
    });

    toolbar = (Toolbar) findViewById(R.id.toolbarMain);
    setSupportActionBar(toolbar);
    spinActionBar = (Spinner) toolbar.findViewById(R.id.spinActionBarTitle);
    txtViewActionBarTitle = (TextView) toolbar.findViewById(R.id.txtViewActionBarTitle);
    selectedRoomTitleContainer = (LinearLayout) context.findViewById(R.id.selectedRoomTitleContainer);
    actionBar = getSupportActionBar();
    actionBar.setHomeButtonEnabled(false);
    actionBar.setDisplayHomeAsUpEnabled(false);
    actionBar.setDisplayShowTitleEnabled(false);

    toggleSettings = new ActionBarDrawerToggle(this, drawerSettings, toolbar, R.string.settings_drawer_open_desc, R.string.settings_drawer_close_desc) {

      public void onDrawerClosed(View view) {;
        actionBar.setTitle("Feeghe");
        supportInvalidateOptionsMenu();
      }

      public void onDrawerOpened(View drawerView) {
        listViewSettings.bringToFront();
        actionBar.setTitle("Settings");
        supportInvalidateOptionsMenu();
      }
    };

    toggleSettings.setDrawerIndicatorEnabled(false);
    drawerSettings.setDrawerListener(toggleSettings);
  }

  public void setActionBarSpinner(String[] selections, AdapterView.OnItemSelectedListener listener) {
    txtViewActionBarTitle.setVisibility(View.GONE);
    selectedRoomTitleContainer.setVisibility(View.GONE);
    spinActionBar.setVisibility(View.VISIBLE);
    spinActionBar.setAdapter(new ArrayAdapter<>(context, R.layout.spinner_action_bar, selections));
    if (listener != null) {
      spinActionBar.setOnItemSelectedListener(listener);
    }
  }

  public void setActionBarTitle(String title) {
    spinActionBar.setVisibility(View.GONE);
    selectedRoomTitleContainer.setVisibility(View.GONE);
    txtViewActionBarTitle.setVisibility(View.VISIBLE);
    txtViewActionBarTitle.setText(title);
  }

  public void showActionBarSelectedRoom() {
    spinActionBar.setVisibility(View.GONE);
    txtViewActionBarTitle.setVisibility(View.GONE);
    selectedRoomTitleContainer.setVisibility(View.VISIBLE);
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    toggleSettings.syncState();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    toggleSettings.onConfigurationChanged(newConfig);
  }

  private void setupTabs() {
    tabHost = (UITabHost) findViewById(R.id.tabHost);
    tabHost.setup();

    for (int i = 0; i < tabTags.length; i++) {
      View tabIndicator = getLayoutInflater().inflate(R.layout.tab_indicator_main, null);
      ImageView tabIcon = (ImageView) tabIndicator.findViewById(R.id.imgViewTabIndicatorMain);
      tabIcon.setImageDrawable(APIUtils.getDrawable(context, tabTags[i]));

      UITabHost.TabSpec tabSpec = tabHost.newTabSpec(tabTags[i]);
      tabSpec.setContent(new TabContent());
      tabSpec.setIndicator(tabIndicator);

      tabHost.addTab(tabSpec);
    }

    tabHost.setOnTabChangedListener(this);
    tabHost.setOnClickCurrentTab(new UITabHost.OnCLickCurrentTab() {

      @Override
      public void onClick(String tag) {
        onTabChanged(tag);
      }
    });
    tabs = tabHost.getTabWidget();
    setActiveTab(tabTags[0]);
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

  private void loadHome() {
    currentUser = userService.getCurrentUser();
    if (currentUser.hasStatus(Session.User.STATUS_INCOMPLETE)) {
      showNewUserFragment();
    } else {
      showMessagesFragment();
    }
    initSocketEvents();
    updateContacts();
  }

  private boolean isFromNotification() {
    Bundle bundle = getIntent().getExtras();
    if (bundle != null && bundle.containsKey("room")) {
      return true;
    }
    return false;
  }

  private void setupSocketConnection() {
    if (isFromNotification()) {
       return;
    }
    boolean isConnected = APIUtils.isConnected(context);
    if (!isConnected || (isConnected && Socket.isConnected())) {
      loadHome();
      return;
    }
    Socket.connect(context, new Socket.SocketConnectionListener() {

      @Override
      public void onStartConnecting(SocketIORequest request) {

      }

      @Override
      public void onConnect(SocketIOClient client) {
        loadHome();
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
          if (currentFragment != null) {
            currentFragment.onKeyboardShow();
          }
        } else {
          tabs.setVisibility(View.VISIBLE);
          if (currentFragment != null) {
            currentFragment.onKeyboardHide();
          }
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
    if (socketClient != null) {
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
  }

  public void setActiveTab(String tabId) {
    int inactiveColor = getResources().getColor(R.color.mainTabInactive);
    for (int i = 0; i < tabs.getChildCount(); i++) {
      View tab = tabs.getChildTabViewAt(i);
      tab.setBackgroundColor(inactiveColor);
      ((ImageView) tab.findViewById(R.id.imgViewTabIndicatorMain)).setImageDrawable(APIUtils.getDrawable(context, tabTags[i]));
    }
    View tab = tabHost.getCurrentTabView();
    tab.setBackgroundColor(getResources().getColor(R.color.mainTabActive));
    ((ImageView) tab.findViewById(R.id.imgViewTabIndicatorMain)).setImageDrawable(APIUtils.getDrawable(context, tabId + "_white"));
  }

  @Override
  public void onTabChanged(String tabId) {
    setActiveTab(tabId);
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
    currentFragment = fragment;
    FragmentManager fm = getSupportFragmentManager();
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

  public void showEditProfileFragment() {
    showFragment(new EditProfileFragment());
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

  public SearchView getSearchView() {
    return searchView;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
    menuItemSearch = menu.findItem(R.id.actionSearchEditTxt);
    searchView = (SearchView) menuItemSearch.getActionView();
    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

      @Override
      public boolean onQueryTextSubmit(String s) {
        return currentFragment.onSearchQuerySubmit(s);
      }

      @Override
      public boolean onQueryTextChange(String s) {
        return currentFragment.onSearchQueryChange(s);
      }
    });

    MenuItemCompat.setOnActionExpandListener(menuItemSearch, new MenuItemCompat.OnActionExpandListener() {

      @Override
      public boolean onMenuItemActionCollapse(MenuItem item) {
        currentFragment.onSearchClose();
        return true;
      }

      @Override
      public boolean onMenuItemActionExpand(MenuItem item) {
        return true;
      }
    });

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (toggleSettings.onOptionsItemSelected(item)) {
      return true;
    }
    switch (item.getItemId()) {
      case R.id.actionSettingsBtn:
        if (!drawerSettings.isDrawerOpen(listViewSettings)) {
          drawerSettings.openDrawer(GravityCompat.END);
        } else {
          drawerSettings.closeDrawer(GravityCompat.END);
        }
        break;
    }
    return super.onOptionsItemSelected(item);
  }

  public void onBackPressed() {
    FragmentManager fm = getSupportFragmentManager();
    if (fm.getBackStackEntryCount() > 0) {
      String fragTabId = fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1).getName();
      setCurrentTab(fragTabId);
      fm.popBackStack();
      currentFragmentTabId = fragTabId;
      return;
    }
    finish();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    getContentResolver().unregisterContentObserver(phoneContactsObserver);
  }

  public void backToRegistration() {
    startActivity(new Intent(this, RegisterActivity.class));
  }

  private class TabContent implements UITabHost.TabContentFactory {

    @Override
    public View createTabContent(String tag) {
      return new View(context);
    }
  }
}
