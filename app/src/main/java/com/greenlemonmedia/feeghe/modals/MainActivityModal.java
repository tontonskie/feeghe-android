package com.greenlemonmedia.feeghe.modals;

import android.app.Dialog;
import android.view.ViewGroup;
import android.view.Window;

import com.greenlemonmedia.feeghe.MainActivity;

/**
 * Created by tonton on 2/27/15.
 */
abstract public class MainActivityModal extends Dialog {

  private Object data;
  private OnDataChangedListener listener;

  public interface OnDataChangedListener {
    public void onChanged(Object oldData, Object newData);
  }

  public MainActivityModal(MainActivity activity) {
    super(activity);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setLayout(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    );
  }

  public void setOnDataChangedListener(OnDataChangedListener listener) {
    this.listener = listener;
  }

  public void setData(Object data, boolean notify) {
    Object oldData = this.data;
    this.data = data;
    if (notify && listener != null) {
      listener.onChanged(oldData, this.data);
    }
  }

  public void setData(Object data) {
    setData(data, true);
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
