package com.greenlemonmedia.feeghe.api;

import android.content.Context;
import android.net.Uri;

import com.greenlemonmedia.feeghe.storage.Session;
import com.koushikdutta.async.http.socketio.Acknowledge;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

/**
 * Created by tonton on 1/5/15.
 */
abstract public class APIService implements Serializable {

  public static final String PORT = null;
  public static final String HTTP_SCHEME = "http";
  public static final String HOST = "dev.feeghe.com";
  public static final String PATH = "api";
  public static final String URL = HTTP_SCHEME + "://" + HOST + "/" + PATH + "/";

  protected String modelName;
  protected Session session;
  protected DefaultHttpClient httpClient;

  /**
   *
   * @param modelName
   */
  public APIService(String modelName, Context context) {
    this.modelName = modelName;
    session = Session.getInstance(context);
    httpClient = new DefaultHttpClient();
  }

  /**
   *
   * @param append
   * @return
   */
  public String getBaseUrl(String append) {
    return getBaseUrl() + '/' + append;
  }

  /**
   *
   * @return
   */
  public String getBaseUrl() {
    return URL + modelName;
  }

  /**
   *
   * @return
   */
  public String getBaseUri() {
    return '/' + PATH + '/' + modelName;
  }

  /**
   *
   * @param append
   * @return
   */
  public String getBaseUri(String append) {
    return getBaseUri() + '/' + append;
  }

  /**
   *
   * @return
   */
  public Uri.Builder getBaseUrlBuilder() {
    Uri.Builder uriBuilder;
    if (PORT != null) {
      uriBuilder = Uri.parse(HTTP_SCHEME + "://" + HOST + ":" + PORT).buildUpon();
    } else {
      uriBuilder = new Uri.Builder()
        .scheme(HTTP_SCHEME)
        .authority(HOST);
    }
    return uriBuilder
      .appendPath(PATH)
      .appendPath(modelName);
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
   * @return
   */
  protected Response parseResponse(HttpResponse response, boolean isArray) {
    Response returnResponse;
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
   * @return
   */
  public ResponseObject get(String id) {
    return (ResponseObject) apiCall(new HttpGet(getBaseUrl(id)));
  }

  /**
   *
   * @param query
   * @return
   */
  public ResponseArray query(JSONObject query) {
    Uri.Builder uriBuilder = getBaseUrlBuilder();
    Iterator<String> keys = query.keys();
    try {
      String key;
      while (keys.hasNext()) {
        key = (String) keys.next();
        Object val = query.get(key);
        if (val instanceof JSONObject || val instanceof JSONArray) {
          uriBuilder.appendQueryParameter(key, val.toString());
        } else {
          uriBuilder.appendQueryParameter(key, String.valueOf(val));
        }
      }
    } catch (JSONException ex) {
      ex.printStackTrace();
    }
    return (ResponseArray) apiCall(new HttpGet(uriBuilder.toString()), true);
  }

  /**
   *
   * @param data
   * @return
   */
  public ResponseObject save(JSONObject data) {
    HttpPost postRequest = new HttpPost(getBaseUrl());
    setBodyParams(postRequest, data);
    return (ResponseObject) apiCall(postRequest);
  }

  /**
   *
   * @param id
   * @param data
   * @return
   */
  public ResponseObject update(String id, JSONObject data) {
    HttpPut putRequest = new HttpPut(getBaseUrl(id));
    setBodyParams(putRequest, data);
    return (ResponseObject) apiCall(putRequest);
  }

  /**
   *
   * @param id
   * @return
   */
  public ResponseObject delete(String id) {
    return (ResponseObject) apiCall(new HttpDelete(getBaseUrl(id)));
  }

  /**
   *
   * @return
   */
  public Session getSession() {
    return session;
  }

  /**
   *
   * @param whereCondition
   * @return
   */
  public JSONObject createWhereQuery(JSONObject whereCondition) {
    JSONObject whereQuery = new JSONObject();
    try {
      if (whereCondition == null) {
        whereQuery.put("where", new JSONObject());
      } else {
        whereQuery.put("where", whereCondition);
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return whereQuery;
  }

  public JSONObject createWhereQuery() {
    return createWhereQuery(null);
  }

  public interface Callback {
    public void onSuccess(ResponseObject response);
    public void onFail(int statusCode, String error);
  }

  public interface EventCallback {
    public void onEvent(JSONObject evt);
  }

  /**
   *
   * @param method
   * @param uri
   * @param params
   * @param callback
   */
  protected void apiSocketCall(String method, String uri, JSONObject params, final Callback callback) {
    JSONArray args = new JSONArray();
    try {
      JSONObject data = new JSONObject();
      data.put("method", method);
      data.put("data", params);
      if (uri == null) {
        data.put("url", getBaseUri());
      } else {
        data.put("url", uri);
      }
      JSONObject headers = new JSONObject();
      headers.put("X-Feeghe-Token", session.getToken());
      headers.put("X-Feeghe-User", session.getUserId());
      data.put("headers", headers);
      args.put(data);
    } catch (JSONException ex) {
      ex.printStackTrace();
    }
    if (callback != null) {
      Socket.getClient().emit(method, args, new Acknowledge() {

        @Override
        public void acknowledge(JSONArray arguments) {
          try {
            JSONObject result = arguments.getJSONObject(0);
            int statusCode = result.getInt("statusCode");
            if (statusCode == HttpStatus.SC_OK) {
              callback.onSuccess(new ResponseObject(statusCode, result.getJSONObject("body")));
            } else {
              callback.onFail(statusCode, result.getString("body"));
            }
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }
      });
    } else {
      Socket.getClient().emit(method, args);
    }
  }

  /**
   *
   * @param method
   * @param uri
   * @param data
   */
  public void apiSocketCall(String method, String uri, JSONObject data) {
    apiSocketCall(method, uri, data, null);
  }

  /**
   *
   * @param method
   * @param data
   * @param callback
   */
  public void apiSocketCall(String method, JSONObject data, Callback callback) {
    apiSocketCall(method, null, data, callback);
  }

  /**
   *
   * @param postData
   * @param callback
   */
  public void socketSave(JSONObject postData, Callback callback) {
    apiSocketCall("post", postData, callback);
  }

  /**
   *
   * @param id
   * @param postData
   * @param callback
   */
  public void socketUpdate(String id, JSONObject postData, Callback callback) {
    apiSocketCall("put", getBaseUri(id), postData, callback);
  }
}
