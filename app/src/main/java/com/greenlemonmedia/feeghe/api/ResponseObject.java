package com.greenlemonmedia.feeghe.api;

import org.apache.http.HttpStatus;
import org.json.JSONObject;

/**
 * Created by tonton on 1/5/15.
 */
public class ResponseObject extends Response {

	private JSONObject content;

	public ResponseObject(int statusCode, JSONObject data) {
		this.statusCode = statusCode;
		content = data;
	}

	public ResponseObject(JSONObject data) {
		content = data;
	}

	public JSONObject getContent() {
		return content;
	}
}
