package com.greenlemonmedia.feeghe.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TabHost;

/**
 * Created by tontonskie on 4/16/15.
 */
public class UITabHost extends TabHost {

  private OnCLickCurrentTab listener;

  public interface OnCLickCurrentTab {
    public void onClick(String index);
  }

  /**
   *
   * @param context
   */
  public UITabHost(Context context) {
    super(context);
  }

  /**
   *
   * @param context
   * @param attrs
   */
  public UITabHost(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  /**
   *
   * @param listener
   */
  public void setOnClickCurrentTab(OnCLickCurrentTab listener) {
    this.listener = listener;
  }

  /**
   *
   * @param index
   */
  @Override
  public void setCurrentTab(int index) {
    if (index == getCurrentTab()) {
      if (listener != null) {
        listener.onClick(getCurrentTabTag());
      }
    } else {
      super.setCurrentTab(index);
    }
  }
}