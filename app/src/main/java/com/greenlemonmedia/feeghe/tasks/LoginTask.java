package com.greenlemonmedia.feeghe.tasks;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.UserService;
import com.greenlemonmedia.feeghe.listeners.LoginListener;
import com.greenlemonmedia.feeghe.storage.UserSession;

import org.json.JSONException;

/**
 * Created by tonton on 1/6/15.
 */
public class LoginTask extends AsyncTask<Void, Void, Void> {

	private ProgressDialog preloader;
	private String paramPhoneNumber;
	private String paramPassword;
	private UserSession session;
	private LoginListener listener;

	public LoginTask(String phoneNumber, String password, ProgressDialog progressDialog, UserSession session,
	                 LoginListener loginListener) {
		paramPhoneNumber = phoneNumber;
		paramPassword = password;
		preloader = progressDialog;
		listener = loginListener;
		this.session = session;
	}

	public void onPreExecute() {
		preloader.setMessage("Please wait...");
		preloader.setCancelable(false);
		preloader.show();
	}

	@Override
	protected Void doInBackground(Void... params) {
		UserService userService = new UserService();
		ResponseObject response = userService.login(paramPhoneNumber, paramPassword);
		if (response.isOk() && response.getContent().has("token")) {
			try {
				session.setToken(response.getContent().getString("token"));
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
