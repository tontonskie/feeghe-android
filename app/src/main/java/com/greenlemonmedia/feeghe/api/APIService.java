package com.greenlemonmedia.feeghe.api;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by tonton on 1/5/15.
 */
abstract public class APIService {

	public static final String API_HOST = "http://192.168.122.1:1338/api/";

	protected String modelName;
	protected DefaultHttpClient httpClient = new DefaultHttpClient();

	/**
	 *
	 * @param append
	 * @return String
	 */
	public String getBaseUrl(String append) {
		return getBaseUrl() + '/' + append;
	}

	/**
	 *
	 * @return String
	 */
	public String getBaseUrl() {
		return API_HOST + modelName;
	}

	/**
	 *
	 * @param response
	 * @param isArray
	 * @return Response
	 */
	protected Response parseResponse(HttpResponse response, boolean isArray) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			response.getEntity().writeTo(output);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			String result = output.toString();
			int statusCode = response.getStatusLine().getStatusCode();
			if (isArray) {
				if (result.isEmpty()) {
					result = "[]";
				}
				return new ResponseArray(statusCode, new JSONArray(result));
			} else {
				if (result.isEmpty()) {
					result = "{}";
				}
				return new ResponseObject(statusCode, new JSONObject(result));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 *
	 * @param id
	 * @return ResponseObject
	 */
	public ResponseObject get(String id) {
		ResponseObject response = null;
		HttpGet request = new HttpGet(getBaseUrl(id));
		try {
			response = (ResponseObject) parseResponse(httpClient.execute(request), false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return response;
	}

	/**
	 *
	 * @param query
	 * @return JSONArray
	 */
	public ResponseArray query(JSONObject query) {
		ResponseArray response = null;
		HttpGet request = new HttpGet();
		try {
			response = (ResponseArray) parseResponse(httpClient.execute(request), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return response;
	}

	/**
	 *
	 * @param data
	 * @return JSONObject
	 */
	public JSONObject save(JSONObject data) {
		return new JSONObject();
	}

	/**
	 *
	 * @param data
	 * @return JSONObject
	 */
	public JSONObject update(JSONObject data) {
		return new JSONObject();
	}
}
