package com.greenlemonmedia.feeghe.storage;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by tonton on 1/6/15.
 */
public class UserSession {

    private SharedPreferences session;
    private SharedPreferences.Editor editor;

    private static UserSession instance = null;
    private static final String LOGGED_IN_KEY = "logged_in";
    private static final String TOKEN_KEY = "token";
    private static final String PREFERENCE_NAME = "feeghe_user_session";
    private static final int PREFERENCE_MODE = 0;

    /**
     *
     * @param preferences
     */
    private UserSession(SharedPreferences preferences) {
        session = preferences;
        editor = session.edit();
    }

    /**
     *
     * @param context
     * @return
     */
    public static UserSession getInstance(Context context) {
        if (instance == null) {
            instance = new UserSession(
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
        }
        editor.commit();
    }

    /**
     *
     * @param token
     */
    public void setToken(String token) {
        setLoggedIn(true);
        editor.putString(TOKEN_KEY, token);
        editor.commit();
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
