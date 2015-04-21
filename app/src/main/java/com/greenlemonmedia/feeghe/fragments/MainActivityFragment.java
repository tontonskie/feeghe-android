package com.greenlemonmedia.feeghe.fragments;

import android.app.Activity;
import android.app.Fragment;

import com.greenlemonmedia.feeghe.MainActivity;

import org.json.JSONObject;

/**
 * Created by tonton on 1/14/15.
 */
abstract public class MainActivityFragment extends Fragment {

  public MainActivity getCurrentActivity() {
    return (MainActivity) getActivity();
  }

  abstract public String getTabId();

  abstract protected void setupUIEvents();

  abstract protected void setupSocketEvents();

  abstract public void onKeyboardShow();

  abstract public void onKeyboardHide();

  abstract public String getFragmentId();

  abstract public boolean onSearchQuerySubmit(String query);

  abstract public boolean onSearchQueryChange(String query);

  abstract public boolean onSearchClose();

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    ((MainActivity) activity).setCurrentFragmentId(getFragmentId());
  }

  public interface AttachmentListing {
    public JSONObject getAttachedItem(int position);
    public int getAttachmentsCount();
  }
}
