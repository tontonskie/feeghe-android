package com.greenlemonmedia.feeghe.api;

import org.apache.http.HttpStatus;

/**
 * Created by tonton on 1/5/15.
 */
public class Response {

	protected int statusCode = HttpStatus.SC_OK;

	public int getStatusCode() {
		return statusCode;
	}

	public boolean isOk() {
		return statusCode == HttpStatus.SC_OK;
	}

	public boolean isForbidden() {
		return statusCode == HttpStatus.SC_FORBIDDEN;
	}

	public boolean isBadRequest() {
		return statusCode == HttpStatus.SC_BAD_REQUEST;
	}

	public boolean isServerError() {
		return statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR;
	}
}
