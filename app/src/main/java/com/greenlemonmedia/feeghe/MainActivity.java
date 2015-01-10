package com.greenlemonmedia.feeghe;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.greenlemonmedia.feeghe.api.Socket;
import com.greenlemonmedia.feeghe.api.UserService;
import com.greenlemonmedia.feeghe.fragments.NewUserFragment;
import com.greenlemonmedia.feeghe.storage.Session;
import com.greenlemonmedia.feeghe.tasks.LogoutTask;
import com.koushikdutta.async.http.socketio.SocketIOClient;
import com.koushikdutta.async.http.socketio.SocketIORequest;

public class MainActivity extends ActionBarActivity {

  private Activity context;
  private ProgressDialog socketPreloader;
  private UserService userService;
  private Session session;
  private Session.User currentUser;

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
        }
      }
    });
  }

  private void showFragment(Fragment fragment) {
    fragment.setArguments(getIntent().getExtras());
    getFragmentManager()
      .beginTransaction()
      .add(R.id.frameMainContent, fragment)
      .commit();
  }

  public void showNewUserFragment() {
    showFragment(new NewUserFragment());
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
      LogoutTask logout = new LogoutTask(this, new LogoutTask.LogoutListener() {

        @Override
        public void onSuccess() {
          backToLogin();
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
