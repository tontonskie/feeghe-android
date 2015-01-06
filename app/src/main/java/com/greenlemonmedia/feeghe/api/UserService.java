package com.greenlemonmedia.feeghe.api;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Created by tonton on 1/5/15.
 */
public class UserService extends APIService {

	public UserService() {
		super("user");
	}

	/**
	 *
	 * @param phoneNumber
	 * @param password
	 * @return
	 */
	public ResponseObject login(String phoneNumber, String password) {
		HttpPost postRequest = new HttpPost(getBaseUrl("login"));
		JSONObject params = new JSONObject();
		try {
			params.put("password", password);
			params.put("phoneNumber", phoneNumber);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		try {
			postRequest.setEntity(new StringEntity(params.toString()));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return (ResponseObject) call(postRequest);
	}
}
