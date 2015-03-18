package com.greenlemonmedia.feeghe.modals;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.R;
import com.greenlemonmedia.feeghe.adapters.ContactsSpinnerAdapter;
import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.CacheCollection;
import com.greenlemonmedia.feeghe.api.ContactService;
import com.greenlemonmedia.feeghe.api.MessageService;
import com.greenlemonmedia.feeghe.api.ResponseArray;
import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.Util;
import com.greenlemonmedia.feeghe.storage.Session;
import com.greenlemonmedia.feeghe.tasks.GoToRoomTask;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by GreenLemon on 3/19/15.
 */
public class SendFaceModal extends MainActivityModal {

  private ContactService contactService;
  private Button btnSendFace;
  private Button btnCancelSendFace;
  private EditText editTxtSendFace;
  private Spinner selectContact;
  private ContactsSpinnerAdapter contactsAdapter;
  private CacheCollection contactsCache;
  private LinearLayout layoutContent;
  private LinearLayout layoutLoading;
  private MessageService messageService;
  private Session session;
  private Context context;

  public SendFaceModal(MainActivity activity) {
    super(activity);
  }

  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.modal_send_face);

    btnSendFace = (Button) findViewById(R.id.btnSendFaceModal);
    btnCancelSendFace = (Button) findViewById(R.id.btnCancelSendFaceModal);
    editTxtSendFace = (EditText) findViewById(R.id.editTxtSendFaceModal);
    selectContact = (Spinner) findViewById(R.id.spinSendFaceModal);
    layoutContent = (LinearLayout) findViewById(R.id.layoutContentSendFaceModal);
    layoutLoading = (LinearLayout) findViewById(R.id.layoutLoadingSendFaceModal);

    context = getContext();
    session = Session.getInstance(context);
    messageService = new MessageService(context);
    contactService = new ContactService(context);
    JSONObject cacheQuery = contactService.getCacheQuery();
    contactsCache = contactService.getCacheCollection(cacheQuery);
    ResponseArray contactsFromCache = contactsCache.getData();
    if (contactsFromCache.length() > 0) {
      setContacts(contactsFromCache);
    }

    contactService.query(cacheQuery, new APIService.QueryCallback() {

      @Override
      public void onSuccess(ResponseArray response) {
        if (contactsAdapter != null) {
          setContacts(response);
          contactsCache.save(response.getContent());
        } else {
          contactsAdapter.setNotifyOnChange(false);
          contactsAdapter.clear();
          contactsAdapter.setNotifyOnChange(true);
          JSONArray contactsFromServer = contactsCache.updateCollection(response).getContent();
          int contactsFromServerLength = contactsFromServer.length();
          try {
            for (int i = 0; i < contactsFromServerLength; i++) {
              contactsAdapter.add(contactsFromServer.getJSONObject(i));
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

    JSONObject data = (JSONObject) getData();
    try {
      Util.getPicasso(context)
        .load(Uri.parse(Util.getStaticUrl(data.getJSONObject("photo").getString("small"))))
        .into(new FaceImageTarget());
    } catch (JSONException e) {
      e.printStackTrace();
    }

    setupUIEvents();
  }

  private void setContacts(ResponseArray contacts) {
    contactsAdapter = new ContactsSpinnerAdapter(context, Util.toList(contacts));
    selectContact.setAdapter(contactsAdapter);
  }

  private class FaceImageTarget implements Target {

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
      int cursorPos = editTxtSendFace.getSelectionStart();
      SpannableStringBuilder message = new SpannableStringBuilder(editTxtSendFace.getText());
      JSONObject data = (JSONObject) getData();

      try {
        String faceImgTag = Util.getImageTag(data.getString("id"), data.getJSONObject("photo").getString("small"));
        message.insert(cursorPos, faceImgTag);
        message.setSpan(
          new ImageSpan(context, bitmap),
          cursorPos,
          cursorPos + faceImgTag.length(),
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
      } catch (JSONException ex) {
        ex.printStackTrace();
      }

      editTxtSendFace.setText(message);
      layoutLoading.setVisibility(View.GONE);
      layoutContent.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {

    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {

    }
  }

  @Override
  protected void setupUIEvents() {
    btnSendFace.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        JSONObject selectedContact = (JSONObject) selectContact.getSelectedItem();
        try {
          GoToRoomTask goToRoomTask = new GoToRoomTask(
            context,
            selectedContact.getJSONObject("user").getString("id"),
            new GoToRoomTask.Listener() {

              @Override
              public void onSuccess(final ResponseObject room) {
                JSONObject newMessage = new JSONObject();
                try {
                  newMessage.put("content", editTxtSendFace.getText().toString());
                  newMessage.put("user", session.getUserId());
                  newMessage.put("room", room.getContent().getString("id"));
                } catch (JSONException e) {
                  e.printStackTrace();
                }
                messageService.socketSave(newMessage, new APIService.SocketCallback() {

                  @Override
                  public void onSuccess(ResponseObject response) {
                    dismiss();
                  }

                  @Override
                  public void onFail(int statusCode, String error) {

                  }
                });
              }

              @Override
              public void onFail(int statusCode, String error) {

              }
            }
          );
          goToRoomTask.execute();
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    });

    btnCancelSendFace.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        dismiss();
      }
    });
  }
}
