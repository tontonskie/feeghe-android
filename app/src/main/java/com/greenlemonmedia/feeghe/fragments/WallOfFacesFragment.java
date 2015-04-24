package com.greenlemonmedia.feeghe.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.R;
import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.CacheCollection;
import com.greenlemonmedia.feeghe.api.FaceService;
import com.greenlemonmedia.feeghe.api.ResponseArray;
import com.greenlemonmedia.feeghe.api.APIUtils;
import com.greenlemonmedia.feeghe.modals.MainActivityModal;
import com.greenlemonmedia.feeghe.modals.SelectedFaceModal;
import com.greenlemonmedia.feeghe.storage.Session;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class WallOfFacesFragment extends MainActivityFragment implements TabHost.OnTabChangeListener {

  private MainActivity context;
  private FaceService faceService;
  private FacesAdapter facesAdapter;
  private GridView gridViewFaces;
  private ProgressDialog facesPreloader;
  private CacheCollection faceCacheCollection;
  private SelectedFaceModal selectedFaceModal;
  private TextView txtViewLoadingNext;
  private TabHost tabHostFaces;
  private TabWidget tabs;
  private FacesAdapter favFacesAdapter;
  private GridView gridViewFavFaces;
  private Session session;
  private boolean isLoadingNextFaces = false;
  private int prevCount = 0;
  private String[] tabTags = {
      TAB_ALL_FACES,
      TAB_FAV_FACES
  };
  private String[] tabIcons = {
      "Wall of Faces",
      "Favorites"
  };
  private int[] tabContents = {
      R.id.tabContentAllFaces,
      R.id.tabContentFavFaces
  };

  public static final String TAB_ALL_FACES = "all_faces";
  public static final String TAB_FAV_FACES = "favorite_faces";

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_wall_of_faces, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstance) {
    super.onActivityCreated(savedInstance);
    context = getCurrentActivity();
    session = Session.getInstance(context);
    faceService = new FaceService(context);
    gridViewFaces = (GridView) context.findViewById(R.id.gridViewFaces);
    gridViewFavFaces = (GridView) context.findViewById(R.id.gridViewFavFaces);
    txtViewLoadingNext = (TextView) context.findViewById(R.id.txtViewLoadingNextFaces);

    selectedFaceModal = new SelectedFaceModal(context);

    tabHostFaces = (TabHost) context.findViewById(R.id.tabHostWallOfFaces);
    tabHostFaces.setup();

    for (int i = 0; i < tabTags.length; i++) {
      View tabIndicator = context.getLayoutInflater().inflate(R.layout.tab_indicator_wall_of_faces, null);
      ((TextView) tabIndicator.findViewById(R.id.txtViewTabIndicatorWallOfFaces)).setText(tabIcons[i]);
      TabHost.TabSpec tabSpec = tabHostFaces.newTabSpec(tabTags[i]);
      tabSpec.setContent(tabContents[i]);
      tabSpec.setIndicator(tabIndicator);
      tabHostFaces.addTab(tabSpec);
    }

    tabHostFaces.setOnTabChangedListener(this);
    tabs = tabHostFaces.getTabWidget();
    setActiveTab();

    loadAllFaces();
    setupUIEvents();
    setupSocketEvents();
  }

  private void loadAllFaces() {
    JSONObject cacheQuery = faceService.getCacheQuery();
    faceCacheCollection = faceService.getCacheCollection(cacheQuery);
    final ResponseArray facesFromCache = faceCacheCollection.getData();
    if (facesFromCache.length() != 0) {
      setAllFaces(facesFromCache);
    } else {
      facesPreloader = APIUtils.showPreloader(context);
    }

    faceService.query(cacheQuery, new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        if (facesFromCache.length() == 0) {
          setAllFaces(response);
          faceCacheCollection.save(response.getContent());
          facesPreloader.dismiss();
        } else {
          facesAdapter.clear();
          JSONArray addedFaces = faceCacheCollection.updateCollection(response).getContent();
          int addedFacesLength = addedFaces.length();
          try {
            for (int i = 0; i < addedFacesLength; i++) {
              facesAdapter.add(addedFaces.getJSONObject(i));
            }
          } catch (JSONException ex) {
            ex.printStackTrace();
          }
        }
      }

      @Override
      public void onFail(int statusCode, String error, JSONObject validationError) {

      }
    });
  }

  private void loadFavoriteFaces() {
    JSONObject query = null;
    try {
      query = faceService.createWhereQuery(new JSONObject("{\"favoritedBy." + session.getUserId() + "\":{\"!\":null},\"privacy\":{\"private\":false}}"));
    } catch (JSONException e) {
      e.printStackTrace();
    }
    final ResponseArray favFacesCache = faceService.getCacheCollection(query).getData();
    if (favFacesCache.length() != 0) {
      setFavoriteFaces(favFacesCache);
    }
    final JSONObject finalQuery = query;
    faceService.query(query, new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        CacheCollection cache = faceService.getCacheCollection(finalQuery);
        if (favFacesCache.length() == 0) {
          cache.save(response.getContent());
          setFavoriteFaces(favFacesCache);
        } else {
          favFacesAdapter.clear();
          JSONArray newFavs = cache.updateCollection(response).getContent();
          try {
            for (int i = 0; i < newFavs.length(); i++) {
              favFacesAdapter.add(newFavs.getJSONObject(i));
            }
          } catch (JSONException ex) {
            ex.printStackTrace();
          }
        }
      }

      @Override
      public void onFail(int statusCode, String error, JSONObject validationError) {
        Toast.makeText(context, "Error in getting favorites: " + statusCode + " " + error, Toast.LENGTH_SHORT).show();
      }
    });
  }

  private void setFavoriteFaces(ResponseArray response) {
    favFacesAdapter = new FacesAdapter(APIUtils.toList(response));
    gridViewFavFaces.setAdapter(favFacesAdapter);
  }

  private void setAllFaces(ResponseArray response) {
    facesAdapter = new FacesAdapter(APIUtils.toList(response));
    gridViewFaces.setAdapter(facesAdapter);
  }

  @Override
  public String getTabId() {
    return MainActivity.TAB_WALL_OF_FACES;
  }

  @Override
  protected void setupUIEvents() {
    selectedFaceModal.setOnDataChangedListener(new MainActivityModal.OnDataChangedListener() {

      @Override
      public void onChanged(Object oldData, Object newData) {
        JSONObject oldFaceData = (JSONObject) oldData;
        JSONObject newFaceData = (JSONObject) newData;
        int position = facesAdapter.getPosition(oldFaceData);
        if (position >= 0) {
          facesAdapter.remove(oldFaceData);
          facesAdapter.insert(newFaceData, position);
        }
      }
    });

    final APIService.QueryCallback nextFacesCallback = new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        JSONArray newFaces = response.getContent();
        try {
          for (int i = 0; i < newFaces.length(); i++) {
            facesAdapter.add(newFaces.getJSONObject(i));
          }
        } catch (JSONException ex) {
          ex.printStackTrace();
        }
        txtViewLoadingNext.setVisibility(View.GONE);
        isLoadingNextFaces = false;
      }

      @Override
      public void onFail(int statusCode, String error, JSONObject validationError) {
        txtViewLoadingNext.setVisibility(View.GONE);
        isLoadingNextFaces = false;
      }
    };

    gridViewFaces.setOnScrollListener(new AbsListView.OnScrollListener() {

      @Override
      public void onScrollStateChanged(AbsListView view, int scrollState) {

      }

      @Override
      public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (facesAdapter != null && prevCount != facesAdapter.getCount() && (firstVisibleItem + visibleItemCount) >= totalItemCount  && !isLoadingNextFaces) {
          prevCount = facesAdapter.getCount();
          isLoadingNextFaces = true;
          JSONObject nextFacesParams = null;
          try {
            String searchText = context.getSearchView().getQuery().toString();
            if (searchText.isEmpty()) {
              nextFacesParams = faceService.getCacheQuery();
            } else {
              nextFacesParams = new JSONObject("{\"where\":{\"or\":[{\"title\":{\"contains\":\"" + searchText + "\"}},{\"tags\":{\"contains\":\"" + searchText + "\"}}]}}");
            }
            nextFacesParams.put("skip", prevCount);
          } catch (JSONException ex) {
            ex.printStackTrace();
          }
          txtViewLoadingNext.setVisibility(View.VISIBLE);
          faceService.query(nextFacesParams, nextFacesCallback);
        }
      }
    });
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
    return MainActivity.FRAG_WALL_OF_FACES;
  }

  @Override
  public boolean onSearchQuerySubmit(String searchText) {
    txtViewLoadingNext.setVisibility(View.VISIBLE);
    JSONObject searchQuery = null;
    if (!searchText.isEmpty()) {
      try {
        searchQuery = new JSONObject("{\"where\":{\"or\":[{\"title\":{\"contains\":\"" + searchText + "\"}},{\"tags\":{\"contains\":\"" + searchText + "\"}}]}}");
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
    faceService.query(searchQuery, new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        facesAdapter.clear();
        JSONArray searchResults = response.getContent();
        try {
          for (int i = 0; i < searchResults.length(); i++) {
            facesAdapter.add(searchResults.getJSONObject(i));
          }
        } catch (JSONException ex) {
          ex.printStackTrace();
        }
        txtViewLoadingNext.setVisibility(View.GONE);
      }

      @Override
      public void onFail(int statusCode, String error, JSONObject validationError) {
        txtViewLoadingNext.setVisibility(View.GONE);
      }
    });
    return true;
  }

  @Override
  public boolean onSearchQueryChange(String query) {
    return false;
  }

  @Override
  public boolean onSearchClose() {
    loadAllFaces();
    return true;
  }

  @Override
  public void onTabChanged(String tabId) {
    setActiveTab();
    if (tabId.equals(TAB_FAV_FACES)) {
      loadFavoriteFaces();
    } else {
      loadAllFaces();
    }
  }

  private void setActiveTab() {
    int inactiveColor = getResources().getColor(R.color.wallOfFacesTabInactive);
    for (int i = 0; i < tabs.getChildCount(); i++) {
      tabs.getChildTabViewAt(i).setBackgroundColor(inactiveColor);
    }
    tabHostFaces.getCurrentTabView().setBackgroundColor(getResources().getColor(R.color.wallOfFacesTabActive));
  }

  private class FacesAdapter extends ArrayAdapter<JSONObject> implements View.OnClickListener {

    public FacesAdapter(ArrayList<JSONObject> faces) {
      super(context, R.layout.per_face, faces);
    }

    @Override
    public void onClick(View v) {
      selectedFaceModal.setData(((FaceViewHolder) v.getTag()).info, false);
      selectedFaceModal.show();
    }

    private class FaceViewHolder {
      public JSONObject info;
      public ImageView imgViewFace;
      public TextView txtViewFaceTitle;
      public TextView txtViewFaceUsage;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
      FaceViewHolder viewHolder;
      if (convertView == null) {
        LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = vi.inflate(R.layout.per_face, null);
        viewHolder = new FaceViewHolder();
        viewHolder.imgViewFace = (ImageView) convertView.findViewById(R.id.imgViewFace);
        viewHolder.txtViewFaceTitle = (TextView) convertView.findViewById(R.id.txtViewFaceTitle);
        viewHolder.txtViewFaceUsage = (TextView) convertView.findViewById(R.id.txtViewFaceUsage);
        convertView.setTag(viewHolder);
        convertView.setOnClickListener(this);
      } else {
        viewHolder = (FaceViewHolder) convertView.getTag();
      }

      JSONObject face = getItem(position);
      viewHolder.info = face;
      try {
        if (face.getInt("usedCount") == 0) {
          viewHolder.txtViewFaceUsage.setVisibility(View.GONE);
        } else {
          viewHolder.txtViewFaceUsage.setVisibility(View.VISIBLE);
          viewHolder.txtViewFaceUsage.setText(face.getString("usedCount"));
        }
        APIUtils.getPicasso(context)
          .load(Uri.parse(APIUtils.getStaticUrl(face.getJSONObject("photo").getString("small"))))
          .into(viewHolder.imgViewFace);
        viewHolder.txtViewFaceTitle.setText(face.getString("title"));
        viewHolder.imgViewFace.setTag(face.getString("id"));
      } catch (JSONException e) {
        e.printStackTrace();
      }

      return convertView;
    }
  }
}
