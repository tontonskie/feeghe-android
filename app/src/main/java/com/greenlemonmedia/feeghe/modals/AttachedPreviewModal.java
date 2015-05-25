package com.greenlemonmedia.feeghe.modals;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.api.APIUtils;
import com.greenlemonmedia.feeghe.R;
import com.greenlemonmedia.feeghe.fragments.MainActivityFragment;
import com.squareup.picasso.Callback;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tontonskie on 4/8/15.
 */
public class AttachedPreviewModal extends MainActivityModal implements Callback {

  private Button btnNext;
  private Button btnPrev;
  private ImageView imgViewPreview;
  private MainActivity context;
  private TextView txtViewLoader;
  private MainActivityFragment.AttachmentListing listing;

  public AttachedPreviewModal(MainActivityFragment.AttachmentListing fragment) {
    super(((MainActivityFragment) fragment).getCurrentActivity());
    listing = fragment;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.modal_attachment_preview);

    context = getActivity();
    btnNext = (Button) findViewById(R.id.btnNextAttachmentPreview);
    btnPrev = (Button) findViewById(R.id.btnPrevAttachmentPreview);
    imgViewPreview = (ImageView) findViewById(R.id.imgViewAttacmentPreview);
    txtViewLoader = (TextView) findViewById(R.id.txtViewLoadingAttachmentPreview);

    setupUIEvents();
  }

  @Override
  public void render() {
    loadAttachment();
  }

  @Override
  protected void setupUIEvents() {
    btnNext.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        setData((int) getData() + 1);
        loadAttachment();
      }
    });

    btnPrev.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        setData((int) getData() - 1);
        loadAttachment();
      }
    });
  }

  @Override
  protected void onStart() {
    render();
  }

  private void loadAttachment() {
    int position = (int) getData();
    JSONObject sizes = listing.getAttachedItem(position);
    if (sizes == null) {
      dismiss();
      return;
    }

//    btnNext.setVisibility(View.VISIBLE);
//    btnPrev.setVisibility(View.VISIBLE);
    txtViewLoader.setVisibility(View.VISIBLE);

//    if (position == 0 || listing.getAttachedItem(position + 1) == null) {
//      btnPrev.setVisibility(View.GONE);
//    }
//
//    if (position == (listing.getAttachmentsCount() - 1) || listing.getAttachedItem(position - 1) == null) {
//      btnPrev.setVisibility(View.GONE);
//    }

    try {
      APIUtils.getPicasso(context)
          .load(Uri.parse(APIUtils.getStaticUrl(sizes.getString("original"))))
          .placeholder(R.drawable.placeholder)
          .error(R.drawable.placeholder)
          .into(imgViewPreview, this);
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onSuccess() {
    txtViewLoader.setVisibility(View.GONE);
  }

  @Override
  public void onError() {

  }
}
