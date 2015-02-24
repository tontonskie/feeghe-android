package com.greenlemonmedia.feeghe.api;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

/**
 * Created by tonton on 1/14/15.
 */
public class Util {

  /**
   *
   * @param response
   * @return
   */
  public static ArrayList<JSONObject> toList(ResponseArray response) {
    return toList(response.getContent());
  }

  /**
   *
   * @param content
   * @return
   */
  public static ArrayList<JSONObject> toList(JSONArray content) {
    ArrayList<JSONObject> contentList = new ArrayList<>();
    int length = content.length();
    try {
      for (int i = 0; i < length; i++) {
        contentList.add(content.getJSONObject(i));
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return contentList;
  }

  /**
   *
   * @param images
   * @param type
   * @return
   */
  public static Uri toImageURI(JSONObject images, String type) {
    Uri uri;
    try {
      uri = Uri.parse(APIService.HTTP_SCHEME + "://" + APIService.HOST + "/uploads/" + images.getString(type));
    } catch (JSONException e) {
      uri = Uri.parse(APIService.HTTP_SCHEME + "://" + APIService.HOST + "/images/placeholder-img.png");
    }
    return uri;
  }

  /**
   *
   * @param user
   * @return
   */
  public static String getFullName(JSONObject user) {
    String fullName = "";
    try {
      String firstName = user.getString("firstName");
      String lastName = user.getString("lastName");
      if (firstName.isEmpty() || lastName.isEmpty() || user.isNull("firstName") || user.isNull("lastName")) {
        fullName = user.getString("phoneNumber");
      } else {
        fullName = firstName + " " + lastName;
      }
    } catch (JSONException ex) {
      ex.printStackTrace();
    }
    return fullName;
  }

  /**
   *
   * @param users
   * @return
   */
  public static String getRoomName(JSONObject users, String currentUserId) {
    StringBuilder sb = new StringBuilder();
    try {
      String userId;
      Iterator<?> iUser = users.keys();
      while (iUser.hasNext()) {
        userId = (String) iUser.next();
        if (!userId.equals(currentUserId)) {
          if (sb.length() > 0) {
            sb.append(", ");
          }
          sb.append(getFullName(users.getJSONObject(userId)));
        }
      }
    } catch (JSONException ex) {
      ex.printStackTrace();
    }
    return sb.toString();
  }

  /**
   *
   * @return
   */
  public static String createUniqueCode() {
    return UUID.randomUUID().toString();
  }

  /**
   *
   * @param context
   * @param message
   * @return
   */
  public static ProgressDialog showPreloader(Context context, String message) {
    return ProgressDialog.show(context, "", message, true, false);
  }

  /**
   *
   * @param context
   * @return
   */
  public static ProgressDialog showPreloader(Context context) {
    return showPreloader(context, "Please wait...");
  }

  /**
   *
   * @param path
   * @return
   */
  public static String getStaticUrl(String path) {
    String host = APIService.HTTP_SCHEME + "://" + APIService.STATIC_HOST;
    String prefix = "/uploads/";
    if (path.indexOf(prefix) == 0) {
      return host + path;
    }
    prefix = host + prefix;
    if (path.indexOf(prefix) == 0) {
      return path;
    }
    return prefix + path;
  }

  /**
   *
   * @param faceId
   * @param path
   * @return
   */
  public static String getImageTag(String faceId, String path) {
    return "<img face=\"" + faceId + "\" src=\"" + path + "\">";
  }

  /**
   *
   * @param context
   * @return
   */
  public static Picasso getPicasso(Context context) {
    Picasso instance = Picasso.with(context);
    instance.setIndicatorsEnabled(true);
    instance.setLoggingEnabled(true);
    return instance;
  }
}
