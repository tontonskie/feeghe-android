package com.greenlemonmedia.feeghe.modals;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
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
  private TextView txtViewSelectedFaceTags;
  private TextView txtViewSelectedFaceTagsCount;
  private Button btnSendSelectedFace;
  private Button btnLikeFace;
  private Button btnSaveSelectedFace;
  private FaceService faceService;
  private ViewAnimator switcherSelectedFace;
  private Button btnShowComments;
  private FaceCommentService faceCommentService;
  private CommentsAdapter commentsAdapter;
  private Button btnHideComments;
  private ListView listViewComments;
  private Activity context;
  private TextView txtViewCommentsTitle;
  private TextView txtViewCommentsLoading;
  private Button btnSendComment;
  private EditText editTxtNewComment;
  private Session session;

  public SelectedFaceModal(MainActivity activity) {
    super(activity);
  }

  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.modal_selected_face);

    context = getActivity();
    session = Session.getInstance(context);
    faceService = new FaceService(context);

    switcherSelectedFace = (ViewAnimator) findViewById(R.id.switcherSelectedFace);
    imgViewSelectedFace = (ImageView) findViewById(R.id.imgViewSelectedFace);
    txtViewSelectedFaceTitle = (TextView) findViewById(R.id.txtViewSelectedFaceTitle);
    txtViewSelectedFaceTags = (TextView) findViewById(R.id.txtViewSelectedFaceTags);
    txtViewSelectedFaceUsage = (TextView) findViewById(R.id.txtViewSelectedFaceUsage);
    txtViewSelectedFaceTagsCount = (TextView) findViewById(R.id.txtViewSelectedFaceTagsCount);
    btnSendSelectedFace = (Button) findViewById(R.id.btnSendSelectedFace);
    btnSaveSelectedFace = (Button) findViewById(R.id.btnSaveSelectedFace);
    btnLikeFace = (Button) findViewById(R.id.btnLikeFace);
    btnShowComments = (Button) findViewById(R.id.btnShowSelectedFaceComments);
    btnHideComments = (Button) findViewById(R.id.btnHideSelectedFaceComments);
    listViewComments = (ListView) findViewById(R.id.listViewSelectedFaceComments);
    txtViewCommentsTitle = (TextView) findViewById(R.id.txtViewSelectedFaceCommentTitle);
    txtViewCommentsLoading = (TextView) findViewById(R.id.txtViewSelectedFaceCommentsLoading);
    btnSendComment = (Button) findViewById(R.id.btnSendSelectedFaceComment);
    editTxtNewComment = (EditText) findViewById(R.id.editTxtSelectedFaceComment);

    setupUIEvents();
  }

  @Override
  protected void setupUIEvents() {
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
              btnShowComments.setText(selectedFace.getInt("commentsCount") + " Comments");
            } catch (JSONException e) {
              e.printStackTrace();
            }
            setData(selectedFace);
          }

          @Override
          public void onFail(int statusCode, String error, JSONObject validationError) {

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
            Toast.makeText(context, statusCode + ": " + error, Toast.LENGTH_SHORT).show();
          }
        });
        switcherSelectedFace.showNext();
      }
    });

    btnHideComments.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        switcherSelectedFace.showPrevious();
        if (commentsAdapter != null) {
          commentsAdapter.clear();
        }
        setCommentsReady(false);
      }
    });

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
            public void onFail(int statusCode, String error, JSONObject validationError) {
              Toast.makeText(context, statusCode + ": " + error, Toast.LENGTH_SHORT).show();
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
            public void onFail(int statusCode, String error, JSONObject validationError) {
              Toast.makeText(context, statusCode + ": " + error, Toast.LENGTH_SHORT).show();
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
    }

    public View getView(int position, View convertView, ViewGroup parent) {
      CommentViewHolder viewHolder;
      if (convertView == null) {
        LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = vi.inflate(R.layout.per_comment, null);
        viewHolder = new CommentViewHolder();
        viewHolder.txtViewUser = (TextView) convertView.findViewById(R.id.txtViewUserPerComment);
        viewHolder.txtViewContent = (TextView) convertView.findViewById(R.id.txtViewContentPerComment);
        convertView.setTag(viewHolder);
      } else {
        viewHolder = (CommentViewHolder) convertView.getTag();
      }

      JSONObject comment = getItem(position);
      try {
        viewHolder.txtViewUser.setText(APIUtils.getFullName(comment.getJSONObject("user")));
        viewHolder.txtViewContent.setText(comment.getString("content"));
      } catch (JSONException e) {
        e.printStackTrace();
      }

      return convertView;
    }
  }

  protected void onStart() {
    switcherSelectedFace.setDisplayedChild(0);
    setCommentsReady(false);
    if (commentsAdapter != null) {
      commentsAdapter.clear();
    }
    JSONObject face = (JSONObject) getData();
    try {

      faceCommentService = new FaceCommentService(context, face.getString("id"));
      btnShowComments.setText(face.getInt("commentsCount") + " Comments");
      txtViewSelectedFaceTitle.setText(face.getString("title"));
      txtViewCommentsTitle.setText(face.getString("title"));
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

      APIUtils.getPicasso(context)
        .load(Uri.parse(APIUtils.getStaticUrl(face.getJSONObject("photo").getString("medium"))))
        .into(imgViewSelectedFace);

    } catch (JSONException e) {
      e.printStackTrace();
    }
  }
}
