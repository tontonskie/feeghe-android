package com.greenlemonmedia.feeghe.api;

import org.apache.http.HttpStatus;

/**
 * Created by tonton on 1/5/15.
 */
public class Response {

	protected int statusCode;
	protected String error;

	public Response() {
		this.statusCode = HttpStatus.SC_OK;
	}

	/**
	 *
	 * @param statusCode
	 */
	public Response(int statusCode) {
		this.statusCode = statusCode;
		this.error = null;
	}

	/**
	 *
	 * @param statusCode
	 * @param error
	 */
	public Response(int statusCode, String error) {
		this.statusCode = statusCode;
		this.error = error;
	}

	/**
	 *
	 * @return
	 */
	public boolean hasError() {
		return error != null;
	}

	/**
	 *
	 * @return
	 */
	public String getErrorMessage() {
		return error;
	}

	/**
	 *
	 * @return
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 *
	 * @return
	 */
	public boolean isOk() {
		return statusCode == HttpStatus.SC_OK;
	}

	/**
	 *
	 * @return
	 */
	public boolean isForbidden() {
		return statusCode == HttpStatus.SC_FORBIDDEN;
	}

	/**
	 *
	 * @return
	 */
	public boolean isBadRequest() {
		return statusCode == HttpStatus.SC_BAD_REQUEST;
	}

	/**
	 *
	 * @return
	 */
	public boolean isServerError() {
		return statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR;
	}
}
