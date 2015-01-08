package com.greenlemonmedia.feeghe;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.greenlemonmedia.feeghe.api.Socket;
import com.greenlemonmedia.feeghe.storage.Session;
import com.greenlemonmedia.feeghe.tasks.LogoutTask;
import com.koushikdutta.async.http.socketio.SocketIOClient;
import com.koushikdutta.async.http.socketio.SocketIORequest;

public class MainActivity extends ActionBarActivity {

  private Context context;
  private ProgressDialog socketPreloader;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    context = this;
    Session session = Session.getInstance(context);
    if (!session.isLoggedIn()) {
      backToLogin();
      return;
    }
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
        socketPreloader.dismiss();
      }
    });
    setContentView(R.layout.activity_main);
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
    Intent loginActivity = new Intent(this, LoginActivity.class);
    startActivity(loginActivity);
  }
}
