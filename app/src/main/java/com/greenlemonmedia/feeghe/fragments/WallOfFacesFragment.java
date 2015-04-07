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
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.R;
import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.CacheCollection;
import com.greenlemonmedia.feeghe.api.FaceService;
import com.greenlemonmedia.feeghe.api.ResponseArray;
import com.greenlemonmedia.feeghe.api.APIUtils;
import com.greenlemonmedia.feeghe.modals.MainActivityModal;
import com.greenlemonmedia.feeghe.modals.SelectedFaceModal;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class WallOfFacesFragment extends MainActivityFragment {

  private MainActivity context;
  private FaceService faceService;
  private FacesAdapter facesAdapter;
  private GridView gridViewFaces;
  private EditText editTxtSearchFace;
  private ProgressDialog facesPreloader;
  private CacheCollection faceCacheCollection;
  private Button btnSearchFace;
  private SelectedFaceModal selectedFaceModal;
  private TextView txtViewLoadingNext;
  private boolean isLoadingNextFaces = false;
  private int prevCount = 0;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_wall_of_faces, container, false);
  }

  public void onActivityCreated(Bundle savedInstance) {
    super.onActivityCreated(savedInstance);
    context = getCurrentActivity();
    faceService = new FaceService(context);
    gridViewFaces = (GridView) context.findViewById(R.id.gridViewFaces);
    editTxtSearchFace = (EditText) context.findViewById(R.id.editTxtSearchFace);
    btnSearchFace = (Button) context.findViewById(R.id.btnSearchFace);
    txtViewLoadingNext = (TextView) context.findViewById(R.id.txtViewLoadingNextFaces);

    selectedFaceModal = new SelectedFaceModal(context);

    JSONObject cacheQuery = faceService.getCacheQuery();
    faceCacheCollection = faceService.getCacheCollection(cacheQuery);
    ResponseArray facesFromCache = faceCacheCollection.getData();
    if (facesFromCache.length() != 0) {
      setFaces(facesFromCache);
    } else {
      facesPreloader = APIUtils.showPreloader(context);
    }

    faceService.query(cacheQuery, new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        if (facesAdapter == null) {
          setFaces(response);
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
      public void onFail(int statusCode, String error) {

      }
    });

    setupUIEvents();
    setupSocketEvents();
  }

  public void setFaces(ResponseArray response) {
    facesAdapter = new FacesAdapter(APIUtils.toList(response));
    gridViewFaces.setAdapter(facesAdapter);
    btnSearchFace.setEnabled(true);
  }

  @Override
  public String getTabId() {
    return MainActivity.TAB_WALL_OF_FACES;
  }

  @Override
  protected void setupUIEvents() {
    final APIService.QueryCallback searchFaceCallback = new APIService.QueryCallback() {

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
      public void onFail(int statusCode, String error) {
        txtViewLoadingNext.setVisibility(View.GONE);
      }
    };

    btnSearchFace.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        txtViewLoadingNext.setVisibility(View.VISIBLE);
        JSONObject searchQuery = null;
        String searchText = editTxtSearchFace.getText().toString();
        if (!searchText.isEmpty()) {
          try {
            searchQuery = new JSONObject("{\"where\":{\"or\":[{\"title\":{\"contains\":\"" + searchText + "\"}},{\"tags\":{\"contains\":\"" + searchText + "\"}}]}}");
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }
        faceService.query(searchQuery, searchFaceCallback);
      }
    });

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
      public void onFail(int statusCode, String error) {
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
            String searchText = editTxtSearchFace.getText().toString();
            if (searchText.isEmpty()) {
              nextFacesParams = new JSONObject();
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
  public String getFragmentId() {
    return MainActivity.FRAG_WALL_OF_FACES;
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
    }

    public View getView(int position, View convertView, ViewGroup parent) {
      FaceViewHolder viewHolder;
      if (convertView == null) {
        LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = vi.inflate(R.layout.per_face, null);
        viewHolder = new FaceViewHolder();
        viewHolder.imgViewFace = (ImageView) convertView.findViewById(R.id.imgViewFace);
        viewHolder.txtViewFaceTitle = (TextView) convertView.findViewById(R.id.txtViewFaceTitle);
        convertView.setTag(viewHolder);
        convertView.setOnClickListener(this);
      } else {
        viewHolder = (FaceViewHolder) convertView.getTag();
      }

      JSONObject face = getItem(position);
      viewHolder.info = face;
      try {
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
