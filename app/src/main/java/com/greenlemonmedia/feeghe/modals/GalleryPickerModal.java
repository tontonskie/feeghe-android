package com.greenlemonmedia.feeghe.modals;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;

import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.R;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by tontonskie on 4/1/15.
 */
public class GalleryPickerModal extends MainActivityModal {

  private Activity context;
  private GridView gridGallery;
  private Button btnSend;
  private GalleryAdapter galleryAdapter;

  public GalleryPickerModal(MainActivity activity) {
    super(activity);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.modal_gallery_picker);
    context = getActivity();

    gridGallery = (GridView) findViewById(R.id.gridViewGallery);
    btnSend = (Button) findViewById(R.id.btnAttachFromGallery);

    final String[] fields= { MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID };
    Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, fields, null, null, null);

    ArrayList<HashMap<String, String>> imgs = new ArrayList<>();
    int colDataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
    int colIdIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
    while (cursor.moveToNext()) {
      HashMap<String, String> img = new HashMap<>();
      img.put("data", cursor.getString(colDataIndex));
      img.put("id", cursor.getString(colIdIndex));
      imgs.add(img);
    }
    cursor.close();
    setImages(imgs);

    setupUIEvents();
  }

  private void setImages(ArrayList<HashMap<String, String>> images) {
    galleryAdapter = new GalleryAdapter(images);
    gridGallery.setAdapter(galleryAdapter);
  }

  @Override
  protected void setupUIEvents() {
    btnSend.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {

      }
    });
  }

  private class GalleryAdapter extends ArrayAdapter<HashMap<String, String>> {

    private BitmapFactory.Options opts = new BitmapFactory.Options();

    public GalleryAdapter(ArrayList<HashMap<String, String>> images) {
      super(context, R.layout.per_img_gallery, images);
      opts.inSampleSize = 5;
    }

    private class GalleryItemViewHolder {
      public ImageView imgView;
      public CheckBox chkBox;
    }

    public View getView(int position, View convertView, ViewGroup viewGroup) {
      GalleryItemViewHolder viewHolder;
      if (convertView == null) {
        convertView = context.getLayoutInflater().inflate(R.layout.per_img_gallery, null);
        viewHolder = new GalleryItemViewHolder();
        viewHolder.imgView = (ImageView) convertView.findViewById(R.id.imgViewGalleryItem);
        viewHolder.chkBox = (CheckBox) convertView.findViewById(R.id.chkBoxGalleryItem);
        convertView.setTag(viewHolder);
      } else {
        viewHolder = (GalleryItemViewHolder) convertView.getTag();
      }
      viewHolder.imgView.setImageBitmap(BitmapFactory.decodeFile(getItem(position).get("data"), opts));
      return convertView;
    }
  }
}
