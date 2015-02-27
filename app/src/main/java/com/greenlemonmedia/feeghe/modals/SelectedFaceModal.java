package com.greenlemonmedia.feeghe.modals;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.R;
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
  private Button btnShareSelectedFace;
  private Button btnSaveSelectedFace;

  public SelectedFaceModal(MainActivity activity) {
    super(activity);
  }

  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.modal_selected_face);

    imgViewSelectedFace = (ImageView) findViewById(R.id.imgViewSelectedFace);
    txtViewSelectedFaceTitle = (TextView) findViewById(R.id.txtViewSelectedFaceTitle);
    txtViewSelectedFaceTags = (TextView) findViewById(R.id.txtViewSelectedFaceTags);
    txtViewSelectedFaceUsage = (TextView) findViewById(R.id.txtViewSelectedFaceUsage);
    txtViewSelectedFaceTagsCount = (TextView) findViewById(R.id.txtViewSelectedFaceTagsCount);
    btnSendSelectedFace = (Button) findViewById(R.id.btnSendSelectedFace);
    btnShareSelectedFace = (Button) findViewById(R.id.btnShareSelectedFace);
    btnSaveSelectedFace = (Button) findViewById(R.id.btnSaveSelectedFace);

    setupUIEvents();
  }

  @Override
  protected void setupUIEvents() {
    btnSendSelectedFace.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {

      }
    });

    btnShareSelectedFace.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {

      }
    });

    btnSaveSelectedFace.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {

      }
    });
  }

  protected void onStart() {
    JSONObject face = (JSONObject) getData();
    try {

      txtViewSelectedFaceTitle.setText(face.getString("title"));
      txtViewSelectedFaceUsage.setText(face.getInt("usedCount") + "");

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
