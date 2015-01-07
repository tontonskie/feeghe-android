package com.greenlemonmedia.feeghe.api;

import com.greenlemonmedia.feeghe.storage.Session;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by tonton on 1/5/15.
 */
abstract public class APIService {

    public static final String API_HOST = "http://192.168.254.101:1338/api/";

    protected String modelName;
    protected Session session;
    protected DefaultHttpClient httpClient;

    /**
     *
     * @param modelName
     */
    public APIService(String modelName, Session sessionStorage) {
        this.modelName = modelName;
        session = sessionStorage;
        httpClient = new DefaultHttpClient();
    }

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
     * @param request
     */
    protected void setApiCredentials(HttpUriRequest request) {
        request.addHeader("X-Feeghe-Token", session.getToken());
        request.addHeader("X-Feeghe-User", session.getUserId());
    }

    /**
     *
     * @param request
     */
    protected void setDefaultHeaders(HttpUriRequest request) {
        request.addHeader("Content-Type", "application/json");
    }

    /**
     *
     * @param request
     * @param params
     */
    protected void setBodyParams(HttpEntityEnclosingRequestBase request, JSONObject params) {
        try {
            request.setEntity(new StringEntity(params.toString()));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param request
     * @param isArray
     * @return
     */
    protected Response call(HttpUriRequest request, boolean isArray) {
        Response result = null;
        setDefaultHeaders(request);
        try {
            result = parseResponse(httpClient.execute(request), isArray);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     *
     * @param request
     * @return
     */
    protected Response call(HttpUriRequest request) {
        return call(request, false);
    }

    /**
     *
     * @param request
     * @param isArray
     * @return
     */
    protected Response apiCall(HttpUriRequest request, boolean isArray) {
        setApiCredentials(request);
        return call(request, isArray);
    }

    /**
     *
     * @param request
     * @return
     */
    protected Response apiCall(HttpUriRequest request) {
        return apiCall(request, false);
    }

    /**
     *
     * @param response
     * @param isArray
     * @return Response
     */
    protected Response parseResponse(HttpResponse response, boolean isArray) {

        Response returnResponse = null;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            response.getEntity().writeTo(output);
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String result = output.toString();
        int statusCode = response.getStatusLine().getStatusCode();
        try {
            if (isArray) {
                if (result.isEmpty()) {
                    result = "[]";
                }
                returnResponse = new ResponseArray(statusCode, new JSONArray(result));
            } else {
                if (result.isEmpty()) {
                    result = "{}";
                }
                returnResponse = new ResponseObject(statusCode, new JSONObject(result));
            }
        } catch (JSONException e) {
            if (isArray) {
                returnResponse = new ResponseArray(statusCode, result);
            } else {
                returnResponse = new ResponseObject(statusCode, result);
            }
        }
        return returnResponse;
    }

    /**
     *
     * @param id
     * @return ResponseObject
     */
    public ResponseObject get(String id) {
        return (ResponseObject) call(new HttpGet(getBaseUrl(id)));
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
