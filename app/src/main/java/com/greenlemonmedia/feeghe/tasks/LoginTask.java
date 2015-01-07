package com.greenlemonmedia.feeghe.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.UserService;
import com.greenlemonmedia.feeghe.storage.Session;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tonton on 1/6/15.
 */
public class LoginTask extends AsyncTask<Void, Void, Void> {

    private ProgressDialog preloader;
    private String paramPhoneNumber;
    private String paramPassword;
    private Session session;
    private LoginListener listener;

    public interface LoginListener {
        public void onSuccess(String token);
    }

    public LoginTask(Context context, String phoneNumber, String password, LoginListener loginListener) {
        paramPhoneNumber = phoneNumber;
        paramPassword = password;
        listener = loginListener;
        session = Session.getInstance(context);
        preloader = new ProgressDialog(context);
    }

    public void onPreExecute() {
        preloader.setMessage("Please wait...");
        preloader.setCancelable(false);
        preloader.show();
    }

    @Override
    protected Void doInBackground(Void... params) {
        UserService userService = new UserService(session);
        ResponseObject response = userService.login(paramPhoneNumber, paramPassword);
        if (response.isOk() && response.getContent().has("token")) {
            try {
                JSONObject user = response.getContent();
                session.setCredentials(user.getString("token"), user.getString("user"));
                listener.onSuccess(session.getToken());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void onPostExecute(Void unused) {
        preloader.dismiss();
    }
}
