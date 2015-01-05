package com.greenlemonmedia.feeghe.api;

import org.json.JSONArray;

/**
 * Created by tonton on 1/5/15.
 */
public class ResponseArray extends Response {

	private JSONArray content;

	public ResponseArray(int statusCode, JSONArray data) {
		this.statusCode = statusCode;
		content = data;
	}

	public ResponseArray(JSONArray data) {
		content = data;
	}

	public JSONArray getContent() {
		return content;
	}
}
