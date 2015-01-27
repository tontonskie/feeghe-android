package com.greenlemonmedia.feeghe.api;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.greenlemonmedia.feeghe.storage.DbCache;
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
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

/**
 * Created by tonton on 1/5/15.
 */
abstract public class APIService implements AsyncServiceInterface, ServiceInterface {

  public static final String PORT = null;
  public static final String HTTP_SCHEME = "http";
  public static final String HOST = "dev.feeghe.com";
  public static final String PATH = "api";
  public static final String URL = HTTP_SCHEME + "://" + HOST + "/" + PATH + "/";

  protected String modelName;
  protected Session session;
  protected DefaultHttpClient httpClient;
  protected DbCache dbCache;
  protected CacheService cacheService;

  /**
   *
   * @param modelName
   */
  public APIService(String modelName, Context context) {
    this.modelName = modelName;
    session = Session.getInstance(context);
    dbCache = DbCache.getInstance(context);
    httpClient = new DefaultHttpClient();
  }

  /**
   *
   * @return
   */
  public CacheService getCacheEntry() {
    if (cacheService == null) {
      cacheService = new CacheService(modelName, dbCache);
    }
    return cacheService;
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
   * @param callback
   * @param isArray
   */
  protected void asyncCall(HttpUriRequest request, Callback callback, boolean isArray) {
    setDefaultHeaders(request);
    new AsyncCall(request, isArray, callback).execute();
  }

  /**
   *
   * @param request
   * @param callback
   */
  protected void asynCall(HttpUriRequest request, Callback callback) {
    asyncCall(request, callback, false);
  }

  /**
   *
   * @param request
   * @param callback
   * @param isArray
   */
  protected void apiAsyncCall(HttpUriRequest request, Callback callback, boolean isArray) {
    setApiCredentials(request);
    asyncCall(request, callback, isArray);
  }

  /**
   *
   * @param request
   * @param callback
   */
  protected void apiAsyncCall(HttpUriRequest request, Callback callback) {
    apiAsyncCall(request, callback, false);
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
   * @param callback
   */
  public void get(String id, GetCallback callback) {
    apiAsyncCall(new HttpGet(getBaseUrl(id)), callback);
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
   * @param callback
   */
  public void query(JSONObject query, QueryCallback callback) {
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
    apiAsyncCall(new HttpGet(uriBuilder.toString()), callback, true);
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
   * @param callback
   */
  public void save(JSONObject data, SaveCallback callback) {
    HttpPost postRequest = new HttpPost(getBaseUrl());
    setBodyParams(postRequest, data);
    apiAsyncCall(postRequest, callback);
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
   * @param callback
   */
  public void update(String id, JSONObject data, UpdateCallback callback) {
    HttpPut putRequest = new HttpPut(getBaseUrl(id));
    setBodyParams(putRequest, data);
    apiAsyncCall(putRequest, callback);
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
   * @param callback
   */
  public void delete(String id, DeleteCallback callback) {
    apiAsyncCall(new HttpDelete(getBaseUrl(id)), callback);
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

  /**
   *
   * @param method
   * @param uri
   * @param params
   * @param callback
   */
  protected void apiSocketCall(String method, String uri, JSONObject params, final SocketCallback callback) {
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
  public void apiSocketCall(String method, JSONObject data, SocketCallback callback) {
    apiSocketCall(method, null, data, callback);
  }

  /**
   *
   * @param postData
   * @param callback
   */
  public void socketSave(JSONObject postData, SocketCallback callback) {
    apiSocketCall("post", postData, callback);
  }

  /**
   *
   * @param id
   * @param postData
   * @param callback
   */
  public void socketUpdate(String id, JSONObject postData, SocketCallback callback) {
    apiSocketCall("put", getBaseUri(id), postData, callback);
  }

  public interface Callback {
    public void onFail(int statusCode, String error);
  }

  public interface APICallback extends Callback {
    public void onSuccess(Response response);
  }

  public interface QueryCallback extends Callback {
    public void onSuccess(ResponseArray response);
  }

  public interface SaveCallback extends Callback {
    public void onSuccess(ResponseObject response);
  }

  public interface UpdateCallback extends Callback {
    public void onSuccess(ResponseObject response);
  }

  public interface DeleteCallback extends Callback {
    public void onSuccess(ResponseObject response);
  }

  public interface GetCallback extends Callback {
    public void onSuccess(ResponseObject response);
  }

  public interface SocketCallback {
    public void onSuccess(ResponseObject response);
    public void onFail(int statusCode, String error);
  }

  public interface EventCallback {
    public void onEvent(JSONObject evt);
  }

  private class AsyncCall extends AsyncTask<Void, Void, Response> {

    private HttpUriRequest request;
    private boolean isArray;
    private Callback callback;

    public AsyncCall(HttpUriRequest request, boolean isArray, Callback callback) {
      this.request = request;
      this.isArray = isArray;
      this.callback = callback;
    }

    public void onPreExecute() {

    }

    @Override
    protected Response doInBackground(Void... params) {
      Response result = null;
      try {
        result = parseResponse(httpClient.execute(request), isArray);
      } catch (IOException e) {
        e.printStackTrace();
      }
      return result;
    }

    public void onPostExecute(Response result) {
      if (callback != null) {
        if (!result.isOk()) {
          callback.onFail(result.getStatusCode(), result.getErrorMessage());
          return;
        }
        if (callback instanceof GetCallback) {
          ((GetCallback) callback).onSuccess((ResponseObject) result);
        } else if (callback instanceof QueryCallback) {
          ((QueryCallback) callback).onSuccess((ResponseArray) result);
        } else if (callback instanceof DeleteCallback) {
          ((DeleteCallback) callback).onSuccess((ResponseObject) result);
        } else if (callback instanceof SaveCallback) {
          ((SaveCallback) callback).onSuccess((ResponseObject) result);
        } else if (callback instanceof UpdateCallback) {
          ((UpdateCallback) callback).onSuccess((ResponseObject) result);
        } else if (callback instanceof APICallback) {
          ((APICallback) callback).onSuccess(result);
        }
      }
    }
  }
}
