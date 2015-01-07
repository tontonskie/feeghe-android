package com.greenlemonmedia.feeghe.storage;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by tonton on 1/6/15.
 */
public class Session {

  private SharedPreferences session;
  public SharedPreferences.Editor editor;

  private static Session instance = null;
  private static final String LOGGED_IN_KEY = "logged_in";
  private static final String TOKEN_KEY = "token";
  private static final String USER_KEY = "user";
  private static final String PREFERENCE_NAME = "feeghe_user_session";
  private static final int PREFERENCE_MODE = 0;

  /**
   *
   * @param preferences
   */
  private Session(SharedPreferences preferences) {
    session = preferences;
    editor = session.edit();
  }

  /**
   *
   * @param context
   * @return
   */
  public static Session getInstance(Context context) {
    if (instance == null) {
      instance = new Session(
        context.getApplicationContext().getSharedPreferences(PREFERENCE_NAME, PREFERENCE_MODE)
      );
    }
    return instance;
  }

  public void save() {
    editor.commit();
  }

  /**
   *
   * @param loggedIn
   */
  public void setLoggedIn(boolean loggedIn) {
    editor.putBoolean(LOGGED_IN_KEY, loggedIn);
    if (!loggedIn) {
      editor.remove(TOKEN_KEY);
      editor.remove(USER_KEY);
    }
    save();
  }

  /**
   *
   * @param token
   * @param userId
   */
  public void setCredentials(String token, String userId) {
    editor.putString(TOKEN_KEY, token);
    editor.putString(USER_KEY, userId);
    setLoggedIn(true);
  }

  /**
   *
   * @return
   */
  public String getUserId() {
    return session.getString(USER_KEY, null);
  }

  /**
   *
   * @return
   */
  public String getToken() {
    return session.getString(TOKEN_KEY, null);
  }

  /**
   *
   * @return
   */
  public boolean isLoggedIn() {
    return session.getBoolean(LOGGED_IN_KEY, false);
  }
}
