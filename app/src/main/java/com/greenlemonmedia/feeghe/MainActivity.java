package com.greenlemonmedia.feeghe;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabWidget;

import com.greenlemonmedia.feeghe.api.Socket;
import com.greenlemonmedia.feeghe.api.UserService;
import com.greenlemonmedia.feeghe.fragments.ContactsFragment;
import com.greenlemonmedia.feeghe.fragments.HomeFragment;
import com.greenlemonmedia.feeghe.fragments.RoomsFragment;
import com.greenlemonmedia.feeghe.fragments.NewUserFragment;
import com.greenlemonmedia.feeghe.fragments.SelectedRoomFragment;
import com.greenlemonmedia.feeghe.storage.Session;
import com.greenlemonmedia.feeghe.tasks.LogoutTask;
import com.koushikdutta.async.http.socketio.SocketIOClient;
import com.koushikdutta.async.http.socketio.SocketIORequest;

public class MainActivity extends ActionBarActivity implements TabHost.OnTabChangeListener {

  private Activity context;
  private ProgressDialog socketPreloader;
  private UserService userService;
  private Session session;
  private Session.User currentUser;
  private TabHost mainLayout;
  private TabWidget tabs;

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

    tabs = (TabWidget) findViewById(android.R.id.tabs);
    mainLayout = (TabHost) findViewById(R.id.tabHost);
    mainLayout.setup();

    TabHost.TabSpec tabHome = mainLayout.newTabSpec(TAB_HOME);
    tabHome.setContent(new TabContent());
    tabHome.setIndicator("Home");

    TabHost.TabSpec tabMessages = mainLayout.newTabSpec(TAB_MESSAGES);
    tabMessages.setContent(new TabContent());
    tabMessages.setIndicator("Messages");

    TabHost.TabSpec tabContacts = mainLayout.newTabSpec(TAB_CONTACTS);
    tabContacts.setContent(new TabContent());
    tabContacts.setIndicator("Contacts");

    mainLayout.addTab(tabHome);
    mainLayout.addTab(tabMessages);
    mainLayout.addTab(tabContacts);
    mainLayout.setOnTabChangedListener(this);

    userService = new UserService(this);
    Socket.connect(session, new Socket.SocketConnectionListener() {

      @Override
      public void onStartConnecting(SocketIORequest request) {
        socketPreloader = new ProgressDialog(context);
        socketPreloader.setCancelable(false);
        socketPreloader.setMessage("Please wait...");
        socketPreloader.show();
      }

      @Override
      public void onConnect(SocketIOClient client) {
        currentUser = userService.getCurrentUser();
        socketPreloader.dismiss();
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

  public TabHost getTabHost() {
    return mainLayout;
  }

  @Override
  public void onTabChanged(String tabId) {
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

  private void showFragment(Fragment fragment) {
    getFragmentManager()
      .beginTransaction()
      .replace(android.R.id.tabcontent, fragment)
      .addToBackStack(null)
      .commit();
  }

  public void showRoomFragment(String roomId) {
    Bundle args = new Bundle();
    args.putString("id", roomId);
    SelectedRoomFragment frag = new SelectedRoomFragment();
    frag.setArguments(args);
    showFragment(frag);
  }

  public void showNewUserFragment() {
    showFragment(new NewUserFragment());
  }

  public void showHomeFragment() {
    showFragment(new HomeFragment());
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
      LogoutTask logout = new LogoutTask(this, new LogoutTask.Listener() {

        @Override
        public void onSuccess() {
          backToLogin();
        }

        @Override
        public void onFail(int statusCode, String error) {

        }
      });
      logout.execute();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  public void backToLogin() {
    startActivity(new Intent(this, LoginActivity.class));
  }
}
