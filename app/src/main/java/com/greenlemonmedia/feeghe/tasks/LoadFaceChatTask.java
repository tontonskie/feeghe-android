package com.greenlemonmedia.feeghe.tasks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.Html;
import android.text.Spanned;
import android.widget.TextView;

import com.greenlemonmedia.feeghe.api.Util;
import com.squareup.picasso.Picasso;

import java.io.IOException;

/**
 * Created by tonton on 2/23/15.
 */
public class LoadFaceChatTask extends AsyncTask<Void, Void, Spanned> {

  private String messageContent;
  private TextView faceMessageView;
  private Context context;

  public LoadFaceChatTask(TextView txtView, String content) {
    messageContent = content;
    faceMessageView = txtView;
    context = txtView.getContext();
  }

  @Override
  protected Spanned doInBackground(Void... params) {
    return Html.fromHtml(
      messageContent,
      new Html.ImageGetter() {

        @Override
        public Drawable getDrawable(String source) {
          Drawable bmpDrawable = null;
          try {
            Bitmap bmp = Picasso.with(context)
              .load(Uri.parse(Util.getStaticUrl(source)))
              .get();
            bmpDrawable = new BitmapDrawable(context.getResources(), bmp);
            bmpDrawable.setBounds(0, 0, bmpDrawable.getIntrinsicWidth(), bmpDrawable.getIntrinsicHeight());
          } catch (IOException e) {
            e.printStackTrace();
          }
          return bmpDrawable;
        }
      },
      null
    );
  }

  public void onPostExecute(Spanned parsedContent) {
    faceMessageView.setText(parsedContent);
  }
}
