package com.greenlemonmedia.feeghe;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.greenlemonmedia.feeghe.tasks.LogoutTask;

public class MainActivity extends ActionBarActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
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
