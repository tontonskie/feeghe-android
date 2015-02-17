package com.greenlemonmedia.feeghe.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.greenlemonmedia.feeghe.api.ResponseObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by tonton on 1/6/15.
 */
public class Session {

  private SharedPreferences session;
  private SharedPreferences.Editor editor;
  private static Session instance;
  private static User currentUser;

  public static final String LOGGED_IN_KEY = "logged_in";
  public static final String TOKEN_KEY = "token";
  public static final String VERIFICATION_KEY = "verification";
  public static final String USER_KEY = "user";
  public static final String PREFERENCE_NAME = "feeghe_user_session";
  public static final int PREFERENCE_MODE = 0;

  public class User extends HashMap<String, Object> {

    public static final String STATUS_INCOMPLETE = "incomplete";
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_BANNED = "banned";

    /**
     *
     * @param info
     */
    public User(JSONObject info) {
      super();
      update(info);
    }

    /**
     *
     * @param key
     * @return
     */
    public JSONObject getJSONObject(String key) {
      return (JSONObject) get(key);
    }

    /**
     *
     * @param key
     * @return
     */
    public JSONArray getJSONArray(String key) {
      return (JSONArray) get(key);
    }

    /**
     *
     * @param key
     * @return
     */
    public String getString(String key) {
      return (String) get(key);
    }

    /**
     *
     * @param key
     * @return
     */
    public int getInt(String key) {
      return (int) get(key);
    }

    /**
     *
     * @param key
     * @return
     */
    public boolean getBoolean(String key) {
      return (boolean) get(key);
    }

    /**
     *
     * @param info
     */
    public void update(JSONObject info) {
      Iterator<String> keys = info.keys();
      try {
        String key;
        while (keys.hasNext()) {
          key = (String) keys.next();
          put(key, info.get(key));
        }
      } catch (JSONException ex) {
        ex.printStackTrace();
      }
    }

    /**
     *
     * @param info
     */
    public void update(ResponseObject info) {
      update(info.getContent());
    }

    /**
     *
     * @return
     */
    public boolean hasStatus(String status) {
      return getString("status").equals(status);
    }

    public JSONObject toJSON()  {
      return new JSONObject(this);
    }
  }

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
   * @param user
   */
  public void setCurrentUser(ResponseObject user) {
    setCurrentUser(user.getContent());
  }

  /**
   *
   * @param user
   */
  public void setCurrentUser(JSONObject user) {
    currentUser = new User(user);
  }

  /**
   *
   * @return
   */
  public User getCurrentUser() {
    return currentUser;
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

  /**
   *
   * @param loggedIn
   */
  public void setLoggedIn(boolean loggedIn) {
    editor.putBoolean(LOGGED_IN_KEY, loggedIn);
    if (!loggedIn) {
      editor.remove(TOKEN_KEY);
      editor.remove(USER_KEY);
      currentUser = null;
    }
    editor.commit();
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

  /**
   *
   * @param key
   * @return
   */
  public String get(String key) {
    return session.getString(key, null);
  }

  /**
   *
   * @param key
   * @param value
   */
  public void set(String key, String value) {
    editor.putString(key, value);
    editor.commit();
  }

  /**
   *
   * @param key
   */
  public void remove(String key) {
    editor.remove(key);
    editor.commit();
  }
}
