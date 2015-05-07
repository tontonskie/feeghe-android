package com.greenlemonmedia.feeghe.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
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

public class WallOfFacesFragment extends MainActivityFragment {

  private MainActivity context;
  private FaceService faceService;
  private FacesAdapter facesAdapter;
  private GridView gridViewFaces;
  private CacheCollection faceCacheCollection;
  private SelectedFaceModal selectedFaceModal;
  private TextView txtViewLoading;
  private Session session;
  private boolean isLoadingNextFaces = false;
  private int prevCount = 0;
  private JSONObject[] faceQueries;
  private int selectedFilterPos = 0;

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
    txtViewLoading = (TextView) context.findViewById(R.id.txtViewLoadingFaces);

    try {
      faceQueries = new JSONObject[]{
        faceService.getCacheQuery(),
        faceService.createWhereQuery(new JSONObject("{\"favoritedBy." + session.getUserId() + "\":{\"!\":null},\"privacy\":{\"private\":false}}")),
        (new JSONObject()).put("user", session.getUserId())
      };
    } catch (JSONException e) {
      e.printStackTrace();
    }

    selectedFaceModal = new SelectedFaceModal(context);

    loadAllFaces();
    setupUIEvents();
    setupSocketEvents();
  }

  private void loadAllFaces() {
    prevCount = 0;
    gridViewFaces.smoothScrollToPosition(0);
    JSONObject cacheQuery = faceService.getCacheQuery();
    faceCacheCollection = faceService.getCacheCollection(cacheQuery);
    final ResponseArray facesFromCache = faceCacheCollection.getData();
    if (facesFromCache.length() != 0) {
      setAllFaces(facesFromCache);
    } else {
      txtViewLoading.setVisibility(View.VISIBLE);
    }

    faceService.query(cacheQuery, new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        if (facesFromCache.length() == 0) {
          setAllFaces(response);
          faceCacheCollection.save(response.getContent());
          txtViewLoading.setVisibility(View.GONE);
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
        txtViewLoading.setVisibility(View.GONE);
        Toast.makeText(context, error, Toast.LENGTH_LONG).show();
      }
    });
  }

  private void loadFavoriteFaces() {
    prevCount = 0;
    gridViewFaces.smoothScrollToPosition(0);
    try {
      final JSONObject query = faceService.createWhereQuery(
        new JSONObject("{\"favoritedBy." + session.getUserId() + "\":{\"!\":null},\"privacy\":{\"private\":false}}")
      );
      final ResponseArray favFacesCache = faceService.getCacheCollection(query).getData();
      if (favFacesCache.length() != 0) {
        setFavFaces(favFacesCache);
      }
      faceService.query(query, new APIService.QueryCallback() {

        @Override
        public void onSuccess(ResponseArray response) {
          CacheCollection cache = faceService.getCacheCollection(query);
          if (favFacesCache.length() == 0) {
            cache.save(response.getContent());
          } else {
            response = cache.updateCollection(response);
          }
          setFavFaces(response);
        }

        @Override
        public void onFail(int statusCode, String error, JSONObject validationError) {
          Toast.makeText(context, "Error in getting favorites: " + statusCode + " " + error, Toast.LENGTH_SHORT).show();
        }
      });
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  private void loadOwnFaces() {
    prevCount = 0;
    gridViewFaces.smoothScrollToPosition(0);
    final JSONObject query = new JSONObject();
    try {
      query.put("user", session.getUserId());
    } catch (JSONException e) {
      e.printStackTrace();
    }
    final ResponseArray ownFacesCache = faceService.getCacheCollection(query).getData();
    if (ownFacesCache.length() != 0) {
      setOwnFaces(ownFacesCache);
    }
    faceService.query(query, new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        CacheCollection cache = faceService.getCacheCollection(query);
        if (ownFacesCache.length() == 0) {
          cache.save(ownFacesCache.getContent());
        } else {
          response = cache.updateCollection(response);
        }
        setOwnFaces(response);
      }

      @Override
      public void onFail(int statusCode, String error, JSONObject validationError) {

      }
    });
  }

  private void setAllFaces(ResponseArray response) {
    facesAdapter = new FacesAdapter(APIUtils.toList(response));
    gridViewFaces.setAdapter(facesAdapter);
  }

  private void setFavFaces(ResponseArray response) {
    JSONArray favFaces = response.getContent();
    facesAdapter.clear();
    try {
      for (int i = 0; i < favFaces.length(); i++) {
        facesAdapter.add(favFaces.getJSONObject(i));
      }
    } catch (JSONException ex) {
      ex.printStackTrace();
    }
  }

  private void setOwnFaces(ResponseArray response) {
    JSONArray ownFaces = response.getContent();
    facesAdapter.clear();
    try {
      for (int i = 0; i < ownFaces.length(); i++) {
        facesAdapter.add(ownFaces.getJSONObject(i));
      }
    } catch (JSONException ex) {
      ex.printStackTrace();
    }
  }

  @Override
  public String getTabId() {
    return MainActivity.TAB_WALL_OF_FACES;
  }

  private void loadSelectedFilter() {
    switch (selectedFilterPos) {
      case 0:
        loadAllFaces();
        break;
      case 1:
        loadFavoriteFaces();
        break;
      case 2:
        loadOwnFaces();
        break;
    }
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

    String[] filters = {
      "Wall of Feeghes",
      "Favorites",
      "My Faces"
    };
    context.setActionBarSpinner(filters, new AdapterView.OnItemSelectedListener() {

      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        selectedFilterPos = position;
        loadSelectedFilter();
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {

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
        txtViewLoading.setVisibility(View.GONE);
        isLoadingNextFaces = false;
      }

      @Override
      public void onFail(int statusCode, String error, JSONObject validationError) {
        txtViewLoading.setVisibility(View.GONE);
        isLoadingNextFaces = false;
      }
    };

    gridViewFaces.setOnScrollListener(new AbsListView.OnScrollListener() {

      @Override
      public void onScrollStateChanged(AbsListView view, int scrollState) {

      }

      @Override
      public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (facesAdapter != null && prevCount != facesAdapter.getCount() && (firstVisibleItem + visibleItemCount) >= totalItemCount && !isLoadingNextFaces) {
          prevCount = facesAdapter.getCount();
          isLoadingNextFaces = true;
          JSONObject nextFacesParams = createSearchQuery(context.getSearchView().getQuery().toString());
          try {
            nextFacesParams.put("skip", prevCount);
          } catch (JSONException ex) {
            ex.printStackTrace();
          }
          txtViewLoading.setVisibility(View.VISIBLE);
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

  private JSONObject createSearchQuery(String searchText) {
    JSONObject searchQuery = null;
    try {
      if (searchText.isEmpty()) {
        searchQuery = faceQueries[selectedFilterPos];
      } else {
        switch (selectedFilterPos) {
          case 0:
          case 1:
            searchQuery = faceQueries[selectedFilterPos];
            break;
          case 2:
            searchQuery = faceService.createWhereQuery(faceQueries[selectedFilterPos]);
            break;
        }
        searchQuery.getJSONObject("where")
          .put("or", new JSONArray("[{\"title\":{\"contains\":\"" + searchText + "\"}},{\"tags\":{\"contains\":\"" + searchText + "\"}}]"));
      }
    } catch (JSONException ex) {
      ex.printStackTrace();
    }
    return searchQuery;
  }

  @Override
  public boolean onSearchQuerySubmit(String searchText) {
    txtViewLoading.setVisibility(View.VISIBLE);
    gridViewFaces.smoothScrollToPosition(0);
    faceService.query(createSearchQuery(searchText), new APIService.QueryCallback() {

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
        txtViewLoading.setVisibility(View.GONE);
      }

      @Override
      public void onFail(int statusCode, String error, JSONObject validationError) {
        txtViewLoading.setVisibility(View.GONE);
      }
    });
    return true;
  }

  @Override
  public boolean onSearchQueryChange(String query) {
    return false;
  }

  @Override
  public void onSearchClose() {
    loadSelectedFilter();
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
