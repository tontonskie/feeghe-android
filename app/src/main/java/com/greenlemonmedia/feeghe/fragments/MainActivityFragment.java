package com.greenlemonmedia.feeghe.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

import com.greenlemonmedia.feeghe.MainActivity;

import org.json.JSONObject;

/**
 * Created by tonton on 1/14/15.
 */
abstract public class MainActivityFragment extends Fragment {

  protected MainActivity context;

  abstract public String getTabId();

  abstract protected void setupUIEvents();

  abstract protected void setupSocketEvents();

  abstract public void onKeyboardShow();

  abstract public void onKeyboardHide();

  abstract public String getFragmentId();

  abstract public boolean onSearchQuerySubmit(String query);

  abstract public boolean onSearchQueryChange(String query);

  abstract public void onSearchClose();

  abstract public void setActionBar();

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    ((MainActivity) activity).setCurrentFragmentId(getFragmentId());
  }

  @Override
  public void onActivityCreated(Bundle savedInstance) {
    super.onActivityCreated(savedInstance);
    context = (MainActivity) getActivity();
    setActionBar();
  }

  public MainActivity getCurrentActivity() {
    if (context == null) {
      context = (MainActivity) getActivity();
    }
    return context;
  }

  public interface AttachmentListing {
    JSONObject getAttachedItem(int position);
    int getAttachmentsCount();
  }
}
