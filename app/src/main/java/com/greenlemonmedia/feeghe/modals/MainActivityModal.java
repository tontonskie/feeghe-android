package com.greenlemonmedia.feeghe.modals;

import android.app.Dialog;
import android.view.ViewGroup;
import android.view.Window;

import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.R;

/**
 * Created by tonton on 2/27/15.
 */
abstract public class MainActivityModal extends Dialog {

  private Object data;

  public MainActivityModal(MainActivity activity) {
    super(activity);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setLayout(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    );
  }

  public void setData(Object data) {
    this.data = data;
  }

  public Object getData() {
    return data;
  }

  public void setContentView(int resId) {
    super.setContentView(resId);
    getWindow().setLayout(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    );
  }

  abstract protected void setupUIEvents();
}
