package com.greenlemonmedia.feeghe.modals;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.R;
import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.FaceCommentService;
import com.greenlemonmedia.feeghe.api.FaceService;
import com.greenlemonmedia.feeghe.api.ResponseArray;
import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.APIUtils;
import com.greenlemonmedia.feeghe.storage.Session;
import com.greenlemonmedia.feeghe.ui.OnSwipeTouchListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by tonton on 2/27/15.
 */
public class SelectedFaceModal extends MainActivityModal {

  private ImageView imgViewSelectedFace;
  private TextView txtViewSelectedFaceUsage;
  private TextView txtViewSelectedFaceTitle;
  private Button btnSendSelectedFace;
  private Button btnLikeFace;
  private Button btnSaveSelectedFace;
  private FaceService faceService;
  private ViewAnimator switcherSelectedFace;
  private Button btnShowComments;
  private FaceCommentService faceCommentService;
  private CommentsAdapter commentsAdapter;
  private ListView listViewComments;
  private Activity context;
  private TextView txtViewCommentsLoading;
  private ImageButton btnSendComment;
  private EditText editTxtNewComment;
  private Session session;
  private Drawable iconHappyFace;
  private Drawable iconSadFace;
  private Drawable iconComments;
  private TextView txtViewSelectedFaceUser;
  private LinearLayout scrollViewTags;
  private RelativeLayout layoutSelFaceOpts;
  private ImageButton btnSelFaceBack;

  public SelectedFaceModal(MainActivity activity) {
    super(activity);
  }

  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.modal_selected_face);

    context = getActivity();
    getWindow().setBackgroundDrawable(context.getResources().getDrawable(R.drawable.modal_selected_face_bg));
    session = Session.getInstance(context);
    faceService = new FaceService(context);

    switcherSelectedFace = (ViewAnimator) findViewById(R.id.switcherSelectedFace);
    imgViewSelectedFace = (ImageView) findViewById(R.id.imgViewSelectedFace);
    txtViewSelectedFaceTitle = (TextView) findViewById(R.id.txtViewSelectedFaceTitle);
    txtViewSelectedFaceUsage = (TextView) findViewById(R.id.txtViewSelectedFaceUsage);
    btnSendSelectedFace = (Button) findViewById(R.id.btnSendSelectedFace);
    btnSaveSelectedFace = (Button) findViewById(R.id.btnSaveSelectedFace);
    btnLikeFace = (Button) findViewById(R.id.btnLikeFace);
    btnShowComments = (Button) findViewById(R.id.btnShowSelectedFaceComments);
    listViewComments = (ListView) findViewById(R.id.listViewSelectedFaceComments);
    txtViewCommentsLoading = (TextView) findViewById(R.id.txtViewSelectedFaceCommentsLoading);
    btnSendComment = (ImageButton) findViewById(R.id.btnSendSelectedFaceComment);
    editTxtNewComment = (EditText) findViewById(R.id.editTxtSelectedFaceComment);
    txtViewSelectedFaceUser = (TextView) findViewById(R.id.txtViewSelectedFaceUser);
    scrollViewTags = (LinearLayout) findViewById(R.id.selectedFaceTagsContainer);
    layoutSelFaceOpts = (RelativeLayout) findViewById(R.id.layoutSelectedFaceOpts);
    btnSelFaceBack = (ImageButton) findViewById(R.id.btnSelFaceBack);

    iconHappyFace = context.getResources().getDrawable(R.drawable.happy_face);
    iconHappyFace.setBounds(0, 0, 40, 40);
    iconSadFace = context.getResources().getDrawable(R.drawable.sad_face);
    iconSadFace.setBounds(0, 0, 40, 40);
    iconComments = context.getResources().getDrawable(R.drawable.messages);
    iconComments.setBounds(0, 0, 40, 40);

    setupUIEvents();
  }

  @Override
  protected void setupUIEvents() {
    ((RelativeLayout) findViewById(R.id.modalSelFaceRoot)).setOnTouchListener(new OnSwipeTouchListener(context) {

      @Override
      public void onSwipeTop() {
        dismiss();
      }
    });

    btnSendComment.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        String newCommentContent = editTxtNewComment.getText().toString();
        if (newCommentContent.isEmpty()) {
          return;
        }
        btnSendComment.setEnabled(false);
        JSONObject newComment = new JSONObject();
        JSONObject newCommentForAppend = new JSONObject();
        try {

          newComment.put("content", newCommentContent);
          newComment.put("user", session.getUserId());

          newCommentForAppend.put("id", "tmp-" + APIUtils.createUniqueCode());
          newCommentForAppend.put("content", newCommentContent);
          newCommentForAppend.put("user", session.getCurrentUser().toJSON());

        } catch (JSONException e) {
          e.printStackTrace();
        }

        faceCommentService.socketSave(newComment, new APIService.SocketCallback() {

          @Override
          public void onSuccess(ResponseObject response) {
            faceCommentService.getCacheCollection().save(response.getContent());
            JSONObject selectedFace = (JSONObject) getData();
            try {
              selectedFace.put("commentsCount", selectedFace.getInt("commentsCount") + 1);
              faceService.getCacheCollection().replace(selectedFace.getString("id"), selectedFace);
              btnShowComments.setText(selectedFace.getInt("commentsCount") + "");
            } catch (JSONException e) {
              e.printStackTrace();
            }
            setData(selectedFace);
          }

          @Override
          public void onFail(int statusCode, String error, JSONObject validationError) {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show();
          }
        });

        commentsAdapter.add(newCommentForAppend);
        editTxtNewComment.setText("");
        btnSendComment.setEnabled(true);
      }
    });

    btnShowComments.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        if (switcherSelectedFace.getDisplayedChild() == 1) {
          switcherSelectedFace.setDisplayedChild(0);
          btnSelFaceBack.setVisibility(View.GONE);
          layoutSelFaceOpts.setVisibility(View.VISIBLE);
          return;
        }

        final ResponseArray commentsFromCache = faceCommentService.getCacheCollection().getData();
        if (commentsFromCache.length() > 0) {
          setComments(commentsFromCache);
          setCommentsReady(true);
        }

        faceCommentService.query(null, new APIService.QueryCallback() {

          @Override
          public void onSuccess(ResponseArray response) {
            if (commentsFromCache.length() == 0) {
              setComments(response);
              setCommentsReady(true);
              faceCommentService.getCacheCollection().save(response.getContent());
            } else {
              commentsAdapter.clear();
              JSONArray newComments = faceCommentService.getCacheCollection().updateCollection(response).getContent();
              int newCommentsLength = newComments.length();
              try {
                for (int i = 0; i < newCommentsLength; i++) {
                  commentsAdapter.add(newComments.getJSONObject(i));
                }
              } catch (JSONException ex) {
                ex.printStackTrace();
              }
            }
          }

          @Override
          public void onFail(int statusCode, String error, JSONObject validationError) {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show();
          }
        });
        layoutSelFaceOpts.setVisibility(View.GONE);
        btnSelFaceBack.setVisibility(View.VISIBLE);
        switcherSelectedFace.setDisplayedChild(1);
      }
    });

//    btnHideComments.setOnClickListener(new View.OnClickListener() {
//
//      @Override
//      public void onClick(View v) {
//        switcherSelectedFace.showPrevious();
//        if (commentsAdapter != null) {
//          commentsAdapter.clear();
//        }
//        setCommentsReady(false);
//      }
//    });

    btnSendSelectedFace.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        dismiss();
        SendFaceModal sendFaceModal = new SendFaceModal(getActivity());
        sendFaceModal.setData(getData());
        sendFaceModal.show();
      }
    });

    btnLikeFace.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        final JSONObject face = (JSONObject) getData();
        try {
          btnLikeFace.setEnabled(false);
          faceService.like(face.getString("id"), !face.getBoolean("liked"), new APIService.UpdateCallback() {

            @Override
            public void onSuccess(ResponseObject response) {
              JSONObject newData = response.getContent();
              setData(newData);
              faceService.getCacheCollection().replace(newData.optString("id", ""), newData);
              btnLikeFace.setEnabled(true);
              if (newData.optBoolean("liked", false)) {
                btnLikeFace.setCompoundDrawables(iconHappyFace, null, null, null);
                btnLikeFace.setText(newData.optInt("likesCount", 0) + "");
              } else {
                btnLikeFace.setCompoundDrawables(iconSadFace, null, null, null);
                btnLikeFace.setText(newData.optInt("likesCount", 0) + "");
              }
            }

            @Override
            public void onFail(int statusCode, String error, JSONObject validationError) {
              Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
              if (face.optBoolean("liked", false)) {
                btnLikeFace.setCompoundDrawables(iconHappyFace, null, null, null);
                btnLikeFace.setText(face.optInt("likesCount", 0) + "");
              } else {
                btnLikeFace.setCompoundDrawables(iconSadFace, null, null, null);
                btnLikeFace.setText(face.optInt("likesCount", 0) + "");
              }
              btnLikeFace.setEnabled(true);
            }
          });
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    });

    btnSelFaceBack.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        btnSelFaceBack.setVisibility(View.GONE);
        switcherSelectedFace.setDisplayedChild(0);
        layoutSelFaceOpts.setVisibility(View.VISIBLE);
      }
    });

    btnSaveSelectedFace.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        final JSONObject data = (JSONObject) getData();
        try {
          btnSaveSelectedFace.setEnabled(false);
          faceService.favorite(data.getString("id"), !data.getBoolean("favorite"), new APIService.UpdateCallback() {

            @Override
            public void onSuccess(ResponseObject response) {
              JSONObject newData = response.getContent();
              setData(newData);
              faceService.getCacheCollection().replace(newData.optString("id", ""), newData);
              btnSaveSelectedFace.setEnabled(true);
              if (newData.optBoolean("favorite", false)) {
                btnSaveSelectedFace.setBackgroundResource(R.drawable.sel_face_unfav_bg);
              } else {
                btnSaveSelectedFace.setBackgroundResource(R.drawable.sel_face_fav_bg);
              }
            }

            @Override
            public void onFail(int statusCode, String error, JSONObject validationError) {
              Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
              if (data.optBoolean("favorite", false)) {
                btnSaveSelectedFace.setBackgroundResource(R.drawable.sel_face_unfav_bg);
              } else {
                btnSaveSelectedFace.setBackgroundResource(R.drawable.sel_face_fav_bg);
              }
              btnSaveSelectedFace.setEnabled(true);
            }
          });
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    });
  }

  private void setComments(ResponseArray comments) {
    commentsAdapter = new CommentsAdapter(APIUtils.toList(comments));
    listViewComments.setAdapter(commentsAdapter);
  }

  private void setCommentsReady(boolean isReady) {
    if (isReady) {
      listViewComments.setVisibility(View.VISIBLE);
      txtViewCommentsLoading.setVisibility(View.GONE);
      btnSendComment.setEnabled(true);
    } else {
      listViewComments.setVisibility(View.GONE);
      txtViewCommentsLoading.setVisibility(View.VISIBLE);
      btnSendComment.setEnabled(false);
    }
  }

  private class CommentsAdapter extends ArrayAdapter<JSONObject> {

    public CommentsAdapter(ArrayList<JSONObject> comments) {
      super(context, R.layout.per_comment, comments);
    }

    private class CommentViewHolder {
      public TextView txtViewUser;
      public TextView txtViewContent;
      public ImageView imgViewUserPic;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
      CommentViewHolder viewHolder;
      if (convertView == null) {
        LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = vi.inflate(R.layout.per_comment, null);
        viewHolder = new CommentViewHolder();
        viewHolder.txtViewUser = (TextView) convertView.findViewById(R.id.txtViewUserPerComment);
        viewHolder.txtViewContent = (TextView) convertView.findViewById(R.id.txtViewContentPerComment);
        viewHolder.imgViewUserPic = (ImageView) convertView.findViewById(R.id.imgViewUserPicPerComment);
        convertView.setTag(viewHolder);
      } else {
        viewHolder = (CommentViewHolder) convertView.getTag();
      }

      JSONObject comment = getItem(position);
      try {
        JSONObject user = comment.getJSONObject("user");
        viewHolder.txtViewUser.setText(APIUtils.getFullName(user));
        viewHolder.txtViewContent.setText(comment.getString("content"));
        APIUtils.getPicasso(context)
          .load(Uri.parse(APIUtils.getStaticUrl(user.getJSONObject("profilePic").getString("small"))))
          .placeholder(R.drawable.placeholder)
          .into(viewHolder.imgViewUserPic);
      } catch (JSONException e) {
        e.printStackTrace();
      }

      return convertView;
    }
  }

  public void render() {
    JSONObject face = (JSONObject) getData();
    try {
      faceCommentService = new FaceCommentService(context, face.getString("id"));

      btnSelFaceBack.setVisibility(View.GONE);
      layoutSelFaceOpts.setVisibility(View.VISIBLE);
      btnShowComments.setText(face.getInt("commentsCount") + "");
      btnShowComments.setCompoundDrawables(iconComments, null, null, null);

      txtViewSelectedFaceTitle.setText(face.getString("title"));
      txtViewSelectedFaceUser.setText(APIUtils.getFullName(face.getJSONObject("user")));
      txtViewSelectedFaceUsage.setText(face.getInt("usedCount") + "");

      if (face.getBoolean("liked")) {
        btnLikeFace.setCompoundDrawables(iconHappyFace, null, null, null);
        btnLikeFace.setText(face.getInt("likesCount") + "");
      } else {
        btnLikeFace.setCompoundDrawables(iconSadFace, null, null, null);
        btnLikeFace.setText(face.getInt("likesCount") + "");
      }

      if (face.getBoolean("favorite")) {
        btnSaveSelectedFace.setBackgroundResource(R.drawable.sel_face_unfav_bg);
      } else {
        btnSaveSelectedFace.setBackgroundResource(R.drawable.sel_face_fav_bg);
      }

      scrollViewTags.removeAllViews();
      if (!face.isNull("tags")) {
        JSONArray tags = face.getJSONArray("tags");
//        SpannableStringBuilder tagString = new SpannableStringBuilder();
//        String spacesBetween = "  ";
        for (int i = 0; i < tags.length(); i++) {

//          int tagIndexStart = tagString.length();
//          String tag = tags.getString(i)
//              .replaceAll("^\"+", "")
//              .replaceAll("\"+$", "");
//          tagString.append(tag + spacesBetween);
//
//          tagString.setSpan(
//              new BackgroundColorSpan(Color.WHITE),
//              tagIndexStart,
//              tagIndexStart + tag.length(),
//              Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//          );

          TextView tagView = new TextView(context);
          tagView.setText(tags.getString(i));
          tagView.setPadding(10, 10, 10, 10);
          tagView.setTextAppearance(context, android.R.style.TextAppearance_Medium);
          scrollViewTags.addView(tagView);
        }
      }

      APIUtils.getPicasso(context)
        .load(Uri.parse(APIUtils.getStaticUrl(face.getJSONObject("photo").getString("medium"))))
        .into(imgViewSelectedFace);

    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  protected void onStart() {
    switcherSelectedFace.setDisplayedChild(0);
    setCommentsReady(false);
    if (commentsAdapter != null) {
      commentsAdapter.clear();
    }
    render();
  }
}
