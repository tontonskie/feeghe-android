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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;

import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.Socket;
import com.greenlemonmedia.feeghe.api.UserService;
import com.greenlemonmedia.feeghe.fragments.ContactsFragment;
import com.greenlemonmedia.feeghe.fragments.HomeFragment;
import com.greenlemonmedia.feeghe.fragments.MainActivityFragment;
import com.greenlemonmedia.feeghe.fragments.RoomsFragment;
import com.greenlemonmedia.feeghe.fragments.NewUserFragment;
import com.greenlemonmedia.feeghe.fragments.SelectedRoomFragment;
import com.greenlemonmedia.feeghe.storage.Session;
import com.koushikdutta.async.http.socketio.SocketIOClient;
import com.koushikdutta.async.http.socketio.SocketIORequest;

import org.json.JSONObject;

public class MainActivity extends ActionBarActivity implements TabHost.OnTabChangeListener {

  private Activity context;
  private UserService userService;
  private Session session;
  private Session.User currentUser;
  private TabHost tabHost;
  private Boolean isManualTabChange = false;
  private String currentFragmentTabId;
  private SoundPool soundPool;
  private int alertSoundId;

  public static final String TAB_HOME = "home";
  public static final String TAB_MESSAGES = "messages";
  public static final String TAB_CONTACTS = "contacts";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    context = this;
    session = Session.getInstance(context);
    if (!session.isLoggedIn()) {
      backToLogin();
      return;
    }

    tabHost = (TabHost) findViewById(R.id.tabHost);
    tabHost.setup();

    TabHost.TabSpec tabHome = tabHost.newTabSpec(TAB_HOME);
    tabHome.setContent(new TabContent());
    tabHome.setIndicator("Home");

    TabHost.TabSpec tabMessages = tabHost.newTabSpec(TAB_MESSAGES);
    tabMessages.setContent(new TabContent());
    tabMessages.setIndicator("Messages");

    TabHost.TabSpec tabContacts = tabHost.newTabSpec(TAB_CONTACTS);
    tabContacts.setContent(new TabContent());
    tabContacts.setIndicator("Contacts");

    tabHost.addTab(tabHome);
    tabHost.addTab(tabMessages);
    tabHost.addTab(tabContacts);
    tabHost.setOnTabChangedListener(this);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      AudioAttributes audioAttr = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build();
      soundPool = new SoundPool.Builder().setMaxStreams(5).setAudioAttributes(audioAttr).build();
    } else {
      soundPool = new SoundPool(5, AudioManager.STREAM_NOTIFICATION, 0);
    }
    alertSoundId = soundPool.load(this, R.raw.alert, 1);

    userService = new UserService(this);
    final ProgressDialog preloader = ProgressDialog.show(context, null, "Please wait...", true, false);
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
          showHomeFragment();
        }
      }
    });
  }

  private class TabContent implements TabHost.TabContentFactory {

    @Override
    public View createTabContent(String tag) {
      return new View(context);
    }
  }

  @Override
  public void onTabChanged(String tabId) {
    if (!isManualTabChange) {
      switch(tabId) {
        case TAB_HOME:
          showHomeFragment();
          break;
        case TAB_MESSAGES:
          showMessagesFragment();
          break;
        case TAB_CONTACTS:
          showContactsFragment();
          break;
      }
    }
  }

  public void setCurrentTab(String tabId) {
    isManualTabChange = true;
    tabHost.setCurrentTabByTag(tabId);
    isManualTabChange = false;
  }

  public void playAlertSound() {
    soundPool.play(alertSoundId, 1f, 1f, 1, 0, 1);
  }

  private void showFragment(MainActivityFragment fragment) {
    showFragment(fragment, true);
  }

  private void showFragment(MainActivityFragment fragment, Boolean withBackStack) {
    FragmentManager fm = getFragmentManager();
    FragmentTransaction ft = fm.beginTransaction();
    ft.replace(android.R.id.tabcontent, fragment);
    if (withBackStack && currentFragmentTabId != null) {
      ft.addToBackStack(currentFragmentTabId);
    }
    currentFragmentTabId = fragment.getTabId();
    ft.commit();
  }

  public void showRoomFragment(String roomInfo) {
    Bundle args = new Bundle();
    args.putString("roomInfo", roomInfo);
    SelectedRoomFragment frag = new SelectedRoomFragment();
    frag.setArguments(args);
    showFragment(frag);
    setCurrentTab(TAB_MESSAGES);
  }

  public void showNewUserFragment() {
    showFragment(new NewUserFragment(), false);
  }

  public void showHomeFragment() {
    showFragment(new HomeFragment());
  }

  public void showHomeFragment(Boolean withBackStack) {
    showFragment(new HomeFragment(), withBackStack);
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
    int id = item.getItemId();
    if (id == R.id.action_logout) {
      final ProgressDialog preloader = ProgressDialog.show(context, null, "Please wait...", true, false);
      userService.logout(new APIService.DeleteCallback() {

        @Override
        public void onSuccess(ResponseObject response) {
          session.setLoggedIn(false);
          Socket.disconnect();
          preloader.dismiss();
          backToLogin();
        }

        @Override
        public void onFail(int statusCode, String error) {
          preloader.dismiss();
        }
      });
      return true;
    }
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

  public void backToLogin() {
    startActivity(new Intent(this, LoginActivity.class));
  }
}
