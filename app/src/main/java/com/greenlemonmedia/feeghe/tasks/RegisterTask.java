package com.greenlemonmedia.feeghe.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.UserService;
import com.greenlemonmedia.feeghe.storage.Session;

import org.json.JSONException;

/**
 * Created by tonton on 1/7/15.
 */
public class RegisterTask extends AsyncTask<Void, Void, Void> {

    private String paramPhoneNumber;
    private ProgressDialog preloader;
    private RegisterListener listener;
    private Session session;

    public interface RegisterListener {
        public void onSuccess(String verificationId);
    }

    public RegisterTask(Context context, String phoneNumber, RegisterListener registerListener) {
        paramPhoneNumber = phoneNumber;
        listener = registerListener;
        preloader = new ProgressDialog(context);
        session = Session.getInstance(context);
    }

    public void onPreExecute() {
        preloader.setCancelable(false);
        preloader.setMessage("Please wait...");
        preloader.show();
    }

    @Override
    protected Void doInBackground(Void... params) {
        UserService userService = new UserService(session);
        ResponseObject registerResult = userService.register(paramPhoneNumber);
        if (registerResult.isOk() && registerResult.getContent().has("id")) {
            try {
                listener.onSuccess(registerResult.getContent().getString("id"));
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
