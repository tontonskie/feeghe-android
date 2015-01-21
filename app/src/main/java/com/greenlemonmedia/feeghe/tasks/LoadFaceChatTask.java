package com.greenlemonmedia.feeghe.tasks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.Spannable;
import android.text.style.ImageSpan;

import com.greenlemonmedia.feeghe.api.APIService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;

/**
 * Created by tonton on 1/14/15.
 */
public class LoadFaceChatTask extends AsyncTask<Void, Void, Bitmap> {

  private Context context;
  private Spannable span;
  private Matcher matcher;
  private Listener listener;

  public interface Listener extends TaskListener {
    public void onSuccess(Bitmap result);
  }

  public LoadFaceChatTask(Context context, Spannable span, Matcher matcher, Listener listener) {
    this.context = context;
    this.span = span;
    this.matcher = matcher;
    this.listener = listener;
  }

  @Override
  protected Bitmap doInBackground(Void... params) {
    Bitmap bmp = null;
    try {
      URLConnection con = new URL(APIService.HTTP_SCHEME + "://" + APIService.HOST + matcher.group(2)).openConnection();
      con.setUseCaches(true);
      InputStream is = con.getInputStream();
      bmp = BitmapFactory.decodeStream(is);
      is.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return bmp;
  }

  public void onPostExecute(Bitmap result) {
    if (result != null) {
      span.setSpan(
        new ImageSpan(context, result),
        matcher.start(),
        matcher.end(),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
      );
      listener.onSuccess(result);
      return;
    }
    listener.onFail(0, "Error occured");
  }
}
