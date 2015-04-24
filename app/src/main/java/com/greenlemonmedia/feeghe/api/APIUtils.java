package com.greenlemonmedia.feeghe.api;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;

import com.greenlemonmedia.feeghe.R;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.Target;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by tonton on 1/14/15.
 */
public class APIUtils {

  private static Picasso iPicasso;
  private static Picasso.RequestTransformer iPicassoTransformer;
  private static LruCache lruImageCache;

  private static final int PICASSO_KEY_PADDING = 50; // Determined by exact science.
  private static final char PICASSO_KEY_SEPARATOR = '\n';

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
   * @param error
   * @return
   */
  public static String toValidationErrString(JSONObject error) {
    String errString = "";
    try {
      if (error != null && error.getString("error").equals("E_VALIDATION")) {
        JSONObject invalidAttributes = error.getJSONObject("invalidAttributes");
        Iterator<String> i = invalidAttributes.keys();
        while (i.hasNext()) {
          JSONArray attrErrors = invalidAttributes.getJSONArray((String) i.next());
          for (int k = 0; k < attrErrors.length(); k++) {
            errString += attrErrors.getJSONObject(k).getString("message");
          }
          if (!errString.isEmpty()) {
            errString += "\n";
          }
        }
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return errString;
  }

  /**
   *
   * @param email
   * @return
   */
  public static boolean isValidEmail(String email) {
    if (TextUtils.isEmpty(email)) {
      return false;
    }
    return Patterns.EMAIL_ADDRESS.matcher(email).matches();
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
   */
  private static void setPicasso(Context context) {
    Picasso.Builder builder = new Picasso.Builder(context);
    iPicassoTransformer = new Picasso.RequestTransformer() {

      @Override
      public Request transformRequest(Request request) {
        return request;
      }
    };
    builder.requestTransformer(iPicassoTransformer);
    lruImageCache = new LruCache(context);
    builder.memoryCache(lruImageCache);
    iPicasso = builder.build();
    iPicasso.setLoggingEnabled(true);
  }

  /**
   *
   * @param context
   * @return
   */
  public static Picasso getPicasso(Context context) {
    if (iPicasso == null) {
      setPicasso(context);
    }
    return iPicasso;
  }

  /**
   *
   * @param data
   * @param builder
   * @return
   */
  private static String createImageCacheKey(Request data, StringBuilder builder) {
    if (data.stableKey != null) {
      builder.ensureCapacity(data.stableKey.length() + PICASSO_KEY_PADDING);
      builder.append(data.stableKey);
    } else if (data.uri != null) {
      String path = data.uri.toString();
      builder.ensureCapacity(path.length() + PICASSO_KEY_PADDING);
      builder.append(path);
    } else {
      builder.ensureCapacity(PICASSO_KEY_PADDING);
      builder.append(data.resourceId);
    }
    builder.append(PICASSO_KEY_SEPARATOR);

    if (data.rotationDegrees != 0) {
      builder.append("rotation:").append(data.rotationDegrees);
      if (data.hasRotationPivot) {
        builder.append('@').append(data.rotationPivotX).append('x').append(data.rotationPivotY);
      }
      builder.append(PICASSO_KEY_SEPARATOR);
    }
    if (data.hasSize()) {
      builder.append("resize:").append(data.targetWidth).append('x').append(data.targetHeight);
      builder.append(PICASSO_KEY_SEPARATOR);
    }
    if (data.centerCrop) {
      builder.append("centerCrop").append(PICASSO_KEY_SEPARATOR);
    } else if (data.centerInside) {
      builder.append("centerInside").append(PICASSO_KEY_SEPARATOR);
    }

    if (data.transformations != null) {
      //noinspection ForLoopReplaceableByForEach
      for (int i = 0, count = data.transformations.size(); i < count; i++) {
        builder.append(data.transformations.get(i).key());
        builder.append(PICASSO_KEY_SEPARATOR);
      }
    }

    return builder.toString();
  }

  /**
   *
   * @param context
   * @param imageUrl
   * @return
   */
  public static Bitmap getCachedBitmap(Context context, String imageUrl) {
    if (iPicasso == null) {
      setPicasso(context);
    }
    Request.Builder data = new Request.Builder(Uri.parse(imageUrl));
    Request request = iPicassoTransformer.transformRequest(data.build());
    return lruImageCache.get(createImageCacheKey(request, new StringBuilder()));
  }

  /**
   *
   * @param message
   * @return
   */
  public static Matcher parseMessage(String message) {
    Pattern pattern = Pattern.compile("<img.*?face=\"([^\"][a-zA-Z0-9]*?)\".*?src=\"([^\">]*\\/([^\">]*?))\".*?>", Pattern.CASE_INSENSITIVE);
    return pattern.matcher(message);
  }

  /**
   *
   * @param sb
   * @param face
   * @param faceId
   * @param start
   * @param end
   * @param faceClickListener
   */
  private static void addFaceToMessage(SpannableStringBuilder sb, ImageSpan face, final String faceId, int start, int end,
                                       final OnFaceClickListener faceClickListener) {
    sb.setSpan(face, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    if (faceClickListener != null) {
      ClickableSpan clickableSpan = new ClickableSpan() {

        @Override
        public void onClick(View widget) {
          faceClickListener.onClick(widget, faceId);
        }

        @Override
        public void updateDrawState(TextPaint tp) {
          tp.bgColor = Color.TRANSPARENT;
          tp.setColor(Color.TRANSPARENT);
          tp.setUnderlineText(false);
        }
      };
      sb.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
  }

  /**
   *
   * @param sb
   * @param context
   * @param face
   * @param faceId
   * @param start
   * @param end
   * @param faceClickListener
   */
  public static void addFaceToMessage(SpannableStringBuilder sb, Context context, Bitmap face, String faceId, int start, int end,
                                      OnFaceClickListener faceClickListener) {
    if (sb == null || face == null) {
      return;
    }
    addFaceToMessage(sb, new ImageSpan(context, face), faceId, start, end, faceClickListener);
  }

  /**
   * @param sb
   * @param face
   * @param faceId
   * @param start
   * @param end
   * @param faceClickListener
   */
  public static void addFaceToMessage(SpannableStringBuilder sb, BitmapDrawable face, String faceId, int start, int end,
                                      OnFaceClickListener faceClickListener) {
    if (sb == null || face == null) {
      return;
    }
    addFaceToMessage(sb, new ImageSpan(face), faceId, start, end, faceClickListener);
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

  /**
   *
   * @param text
   * @return
   */
  public static String hash(String text) {
    StringBuffer sb = new StringBuffer();
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      md.reset();
      md.update(text.getBytes());
      byte[] result = md.digest();
      for (int i = 0; i < result.length; i++) {
        sb.append(String.format("%02x", result[i]));
      }
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return sb.toString();
  }

  /**
   *
   * @param text
   * @return
   */
  public static boolean messageHasFace(String text) {
    return text.indexOf("<img face=") >= 0;
  }

  /**
   *
   * @param txtView
   * @param face
   * @return
   */
  private static BitmapDrawable adjustFaceHeight(TextView txtView, Bitmap face) {
    BitmapDrawable bitmapDrawable = new BitmapDrawable(txtView.getContext().getResources(), face);
    bitmapDrawable.setBounds(0, 0, txtView.getLineHeight(), txtView.getLineHeight());
    return bitmapDrawable;
  }

  /**
   *
   * @param context
   * @param message
   * @param txtView
   * @param faceClickListener
   */
  public static void loadFacesFromMessage(Context context, String message, TextView txtView,
                                          OnFaceClickListener faceClickListener) {
    loadFacesFromMessage(context, message, txtView, faceClickListener, false);
  }

  /**
   *
   * @param context
   * @param message
   * @param txtView
   * @param faceClickListener
   * @param resize
   */
  public static void loadFacesFromMessage(final Context context, String message, final TextView txtView,
                                          final OnFaceClickListener faceClickListener, final boolean resize) {
    final SpannableStringBuilder sb = new SpannableStringBuilder(message);
    Matcher parser = parseMessage(message);
    int addedFaceToMessage = 0;
    txtView.setText("Loading...");

    while (parser.find()) {

      String faceImageUrl = getStaticUrl(parser.group(2));
      Bitmap faceBmp = getCachedBitmap(context, faceImageUrl);

      if (faceBmp != null) {

        if (resize) {
          addFaceToMessage(sb, adjustFaceHeight(txtView, faceBmp), parser.group(1), parser.start(), parser.end(), faceClickListener);
        } else {
          addFaceToMessage(sb, context, faceBmp, parser.group(1), parser.start(), parser.end(), faceClickListener);
        }
        addedFaceToMessage++;

      } else {

        final int start = parser.start();
        final int end = parser.end();
        final String faceId = parser.group(1);

        getPicasso(context)
          .load(faceImageUrl)
          .into(new Target() {

            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
              if (resize) {
                addFaceToMessage(sb, adjustFaceHeight(txtView, bitmap), faceId, start, end, faceClickListener);
              } else {
                addFaceToMessage(sb, context, bitmap, faceId, start, end, faceClickListener);
              }
              txtView.setText(sb);
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {

            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
              Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.placeholder);
              if (resize) {
                addFaceToMessage(sb, adjustFaceHeight(txtView, bitmap), faceId, start, end, faceClickListener);
              } else {
                addFaceToMessage(sb, context, bitmap, faceId, start, end, faceClickListener);
              }
            }
          });
      }
    }
    if (addedFaceToMessage > 0) {
      txtView.setText(sb);
    }
  }

  public interface OnFaceClickListener {
    public void onClick(View widget, String faceId);
  }

  /**
   *
   * @param context
   * @param id
   * @return
   */
  public static Drawable getDrawable(Context context, String id) {
    Drawable result = null;
    Class res = R.drawable.class;
    try {
      Field field = res.getField(id);
      result = context.getResources().getDrawable(field.getInt(null));
    } catch (IllegalAccessException | NoSuchFieldException e) {
      e.printStackTrace();
    }
    return result;
  }
}
