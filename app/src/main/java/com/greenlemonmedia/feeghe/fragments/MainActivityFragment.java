package com.greenlemonmedia.feeghe.fragments;

import android.app.Fragment;

import com.greenlemonmedia.feeghe.MainActivity;

/**
 * Created by tonton on 1/14/15.
 */
abstract public class MainActivityFragment extends Fragment {

  protected MainActivity getCurrentActivity() {
    return (MainActivity) getActivity();
  }

  abstract public String getTabId();

  abstract protected void setupUIEvents();

  abstract protected void setupSocketEvents();
}
