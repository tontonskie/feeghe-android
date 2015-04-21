package com.greenlemonmedia.feeghe.fragments;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class UploadFragment extends MainActivityFragment {

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_upload, container, false);
  }


  @Override
  public String getTabId() {
    return MainActivity.TAB_UPLOAD;
  }

  @Override
  protected void setupUIEvents() {

  }

  @Override
  protected void setupSocketEvents() {

  }

  @Override
  public void onKeyboardShow() {

  }

  @Override
  public void onKeyboardHide() {

  }

  @Override
  public String getFragmentId() {
    return MainActivity.FRAG_UPLOAD;
  }

  @Override
  public boolean onSearchQuerySubmit(String query) {
    return false;
  }

  @Override
  public boolean onSearchQueryChange(String query) {
    return false;
  }

  @Override
  public boolean onSearchClose() {
    return false;
  }
}
