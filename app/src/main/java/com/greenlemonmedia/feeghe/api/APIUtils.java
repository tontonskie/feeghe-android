package com.greenlemonmedia.feeghe.api;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

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
public class APIUtils {

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
    instance.setLoggingEnabled(true);
    return instance;
  }

  /**
   *
   * @param path
   * @return
   */
  public static String generateFilename(String path) {
    String[] splitted = path.split("\\.");
    return createUniqueCode() + "." + splitted[splitted.length - 1];
  }

  /**
   *
   * @param context
   * @param uri
   * @return
   */
  @SuppressLint("NewApi")
  public static String getPath(Context context, Uri uri) {

    final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

    // DocumentProvider
    if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
      // ExternalStorageProvider
      if (isExternalStorageDocument(uri)) {
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        if ("primary".equalsIgnoreCase(type)) {
          return Environment.getExternalStorageDirectory() + "/" + split[1];
        }

        // TODO handle non-primary volumes
      }
      // DownloadsProvider
      else if (isDownloadsDocument(uri)) {

        final String id = DocumentsContract.getDocumentId(uri);
        final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

        return getDataColumn(context, contentUri, null, null);
      }
      // MediaProvider
      else if (isMediaDocument(uri)) {
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        Uri contentUri = null;
        if ("image".equals(type)) {
          contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if ("video".equals(type)) {
          contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if ("audio".equals(type)) {
          contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        final String selection = "_id=?";
        final String[] selectionArgs = new String[] {
            split[1]
        };

        return getDataColumn(context, contentUri, selection, selectionArgs);
      }
    }
    // MediaStore (and general)
    else if ("content".equalsIgnoreCase(uri.getScheme())) {
      return getDataColumn(context, uri, null, null);
    }
    // File
    else if ("file".equalsIgnoreCase(uri.getScheme())) {
      return uri.getPath();
    }

    return null;
  }

  /**
   *
   * @param context
   * @param uri
   * @param selection
   * @param selectionArgs
   * @return
   */
  public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

    Cursor cursor = null;
    final String column = "_data";
    final String[] projection = {
      column
    };

    try {
      cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
      if (cursor != null && cursor.moveToFirst()) {
        final int column_index = cursor.getColumnIndexOrThrow(column);
        return cursor.getString(column_index);
      }
    } finally {
      if (cursor != null)
        cursor.close();
    }
    return null;
  }

  /**
   *
   * @param uri
   * @return
   */
  public static boolean isExternalStorageDocument(Uri uri) {
    return "com.android.externalstorage.documents".equals(uri.getAuthority());
  }

  /**
   *
   * @param uri
   * @return
   */
  public static boolean isDownloadsDocument(Uri uri) {
    return "com.android.providers.downloads.documents".equals(uri.getAuthority());
  }

  /**
   *
   * @param uri
   * @return
   */
  public static boolean isMediaDocument(Uri uri) {
    return "com.android.providers.media.documents".equals(uri.getAuthority());
  }
}
