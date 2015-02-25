package com.greenlemonmedia.feeghe.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.R;

public class WallOfFacesFragment extends MainActivityFragment {

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_wall_of_faces, container, false);
  }

  @Override
  public String getTabId() {
    return MainActivity.TAB_WALL_OF_FACES;
  }

  @Override
  protected void setupUIEvents() {

  }

  @Override
  protected void setupSocketEvents() {

  }

  @Override
  public String getFragmentId() {
    return MainActivity.FRAG_WALL_OF_FACES;
  }
}
