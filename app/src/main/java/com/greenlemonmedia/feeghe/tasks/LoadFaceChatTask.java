package com.greenlemonmedia.feeghe.tasks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.Html;
import android.text.Spanned;

import com.greenlemonmedia.feeghe.api.APIUtils;

import java.io.IOException;

/**
 * Created by tonton on 2/23/15.
 */
public class LoadFaceChatTask extends AsyncTask<Void, Void, Spanned> {

  private String messageContent;
  private Context context;
  private Listener listener;

  public interface Listener extends TaskListener {
    public void onSuccess(Spanned text);
  }

  public LoadFaceChatTask(Context uiContext, String content, Listener faceChatListener) {
    messageContent = content;
    context = uiContext;
    listener = faceChatListener;
  }

  @Override
  protected Spanned doInBackground(Void... params) {
    return Html.fromHtml(
      messageContent,
      new Html.ImageGetter() {

        @Override
        public Drawable getDrawable(String source) {
          Drawable bmpDrawable;
          try {
            Bitmap bmp = APIUtils.getPicasso(context)
              .load(Uri.parse(APIUtils.getStaticUrl(source)))
              .get();
            bmpDrawable = new BitmapDrawable(context.getResources(), bmp);
            bmpDrawable.setBounds(0, 0, bmpDrawable.getIntrinsicWidth(), bmpDrawable.getIntrinsicHeight());
          } catch (IOException e) {
            return null;
          }
          return bmpDrawable;
        }
      },
      null
    );
  }

  public void onPostExecute(Spanned parsedContent) {
    if (parsedContent == null) {
      listener.onFail(0, "Error downloading image");
    } else {
      listener.onSuccess(parsedContent);
    }
  }
}
