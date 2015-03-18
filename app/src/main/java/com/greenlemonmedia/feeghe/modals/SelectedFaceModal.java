package com.greenlemonmedia.feeghe.modals;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.R;
import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.FaceService;
import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tonton on 2/27/15.
 */
public class SelectedFaceModal extends MainActivityModal {

  private ImageView imgViewSelectedFace;
  private TextView txtViewSelectedFaceUsage;
  private TextView txtViewSelectedFaceTitle;
  private TextView txtViewSelectedFaceTags;
  private TextView txtViewSelectedFaceTagsCount;
  private Button btnSendSelectedFace;
  private Button btnLikeFace;
  private Button btnSaveSelectedFace;
  private FaceService faceService;

  public SelectedFaceModal(MainActivity activity) {
    super(activity);
  }

  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.modal_selected_face);

    faceService = new FaceService(getContext());

    imgViewSelectedFace = (ImageView) findViewById(R.id.imgViewSelectedFace);
    txtViewSelectedFaceTitle = (TextView) findViewById(R.id.txtViewSelectedFaceTitle);
    txtViewSelectedFaceTags = (TextView) findViewById(R.id.txtViewSelectedFaceTags);
    txtViewSelectedFaceUsage = (TextView) findViewById(R.id.txtViewSelectedFaceUsage);
    txtViewSelectedFaceTagsCount = (TextView) findViewById(R.id.txtViewSelectedFaceTagsCount);
    btnSendSelectedFace = (Button) findViewById(R.id.btnSendSelectedFace);
    btnSaveSelectedFace = (Button) findViewById(R.id.btnSaveSelectedFace);
    btnLikeFace = (Button) findViewById(R.id.btnLikeFace);

    setupUIEvents();
  }

  @Override
  protected void setupUIEvents() {
    btnSendSelectedFace.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {

      }
    });

    btnLikeFace.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        JSONObject face = (JSONObject) getData();
        try {
          btnLikeFace.setText("Loading...");
          btnLikeFace.setEnabled(false);
          faceService.like(face.getString("id"), !face.getBoolean("liked"), new APIService.UpdateCallback() {

            @Override
            public void onSuccess(ResponseObject response) {
              JSONObject newData = response.getContent();
              setData(newData);
              faceService.getCacheCollection().replace(newData.optString("id", ""), newData);
              btnLikeFace.setEnabled(true);
              if (newData.optBoolean("liked", false)) {
                btnLikeFace.setText("Unlike \n" + newData.optInt("likesCount", 0));
              } else {
                btnLikeFace.setText("Like \n" + newData.optInt("likesCount", 0));
              }
            }

            @Override
            public void onFail(int statusCode, String error) {
              Toast.makeText(getContext(), statusCode + ": " + error, Toast.LENGTH_SHORT).show();
            }
          });
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    });

    btnSaveSelectedFace.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        JSONObject data = (JSONObject) getData();
        try {
          btnSaveSelectedFace.setText("Loading...");
          btnSaveSelectedFace.setEnabled(false);
          faceService.favorite(data.getString("id"), !data.getBoolean("favorite"), new APIService.UpdateCallback() {

            @Override
            public void onSuccess(ResponseObject response) {
              JSONObject newData = response.getContent();
              setData(newData);
              faceService.getCacheCollection().replace(newData.optString("id", ""), newData);
              btnSaveSelectedFace.setEnabled(true);
              if (newData.optBoolean("favorite", false)) {
                btnSaveSelectedFace.setText("Unfavorite");
              } else {
                btnSaveSelectedFace.setText("Save");
              }
            }

            @Override
            public void onFail(int statusCode, String error) {
              Toast.makeText(getContext(), statusCode + ": " + error, Toast.LENGTH_SHORT).show();
            }
          });
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    });
  }

  protected void onStart() {
    JSONObject face = (JSONObject) getData();
    try {

      txtViewSelectedFaceTitle.setText(face.getString("title"));
      txtViewSelectedFaceUsage.setText(face.getInt("usedCount") + "");
      if (face.getBoolean("liked")) {
        btnLikeFace.setText("Unlike \n" + face.getInt("likesCount"));
      } else {
        btnLikeFace.setText("Like \n" + face.getInt("likesCount"));
      }
      if (face.getBoolean("favorite")) {
        btnSaveSelectedFace.setText("Unfavorite");
      } else {
        btnSaveSelectedFace.setText("Save");
      }

      if (!face.isNull("tags")) {
        JSONArray tags = face.getJSONArray("tags");
        SpannableStringBuilder tagString = new SpannableStringBuilder();
        String spacesBetween = "  ";
        for (int i = 0; i < tags.length(); i++) {

          int tagIndexStart = tagString.length();
          String tag = tags.getString(i)
            .replaceAll("^\"+", "")
            .replaceAll("\"+$", "");
          tagString.append(tag + spacesBetween);

          tagString.setSpan(
            new BackgroundColorSpan(Color.WHITE),
            tagIndexStart,
            tagIndexStart + tag.length(),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
          );
        }

        txtViewSelectedFaceTags.setText(tagString);
        txtViewSelectedFaceTagsCount.setText(tags.length() + "");
      } else {
        txtViewSelectedFaceTags.setText("");
        txtViewSelectedFaceTagsCount.setText("0");
      }

      Util.getPicasso(getContext())
        .load(Uri.parse(Util.getStaticUrl(face.getJSONObject("photo").getString("medium"))))
        .into(imgViewSelectedFace);

    } catch (JSONException e) {
      e.printStackTrace();
    }
  }
}
