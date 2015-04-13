package com.greenlemonmedia.feeghe.tasks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.view.View;

import com.greenlemonmedia.feeghe.api.APIUtils;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by tonton on 2/23/15.
 */
public class LoadFaceChatTask extends AsyncTask<Void, Void, SpannableStringBuilder> {

  private String messageContent;
  private Context context;
  private Listener listener;
  private OnFaceClickListener clickListener;

  public interface Listener extends TaskListener {
    public void onSuccess(SpannableStringBuilder text);
  }

  public interface OnFaceClickListener {
    public void onClick(View widget, String faceId);
  }

  public LoadFaceChatTask(Context uiContext, String content, Listener faceChatListener, OnFaceClickListener onClickListener) {
    messageContent = content;
    context = uiContext;
    listener = faceChatListener;
    clickListener = onClickListener;
  }

  @Override
  protected SpannableStringBuilder doInBackground(Void... params) {
    SpannableStringBuilder sb = new SpannableStringBuilder(messageContent);
    Pattern pattern = Pattern.compile("<img.*?face=\"([^\"][a-zA-Z0-9]*?)\".*?src=\"([^\">]*\\/([^\">]*?))\".*?>", Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(messageContent);
    try {
      while (matcher.find()) {

        Bitmap bmp = APIUtils.getPicasso(context)
          .load(Uri.parse(APIUtils.getStaticUrl(matcher.group(2))))
          .get();
        sb.setSpan(new ImageSpan(context, bmp), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        final String faceId = matcher.group(1);
        ClickableSpan clickableSpan = new ClickableSpan() {

          @Override
          public void onClick(View widget) {
            clickListener.onClick(widget, faceId);
          }

          @Override
          public void updateDrawState(TextPaint tp) {
            tp.bgColor = Color.TRANSPARENT;
            tp.setColor(Color.TRANSPARENT);
            tp.setUnderlineText(false);
          }
        };
        sb.setSpan(clickableSpan, matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return sb;
  }

  @Override
  public void onPostExecute(SpannableStringBuilder parsedContent) {
    if (parsedContent == null) {
      listener.onFail(0, "Error downloading image");
      return;
    }
    listener.onSuccess(parsedContent);
  }
}
