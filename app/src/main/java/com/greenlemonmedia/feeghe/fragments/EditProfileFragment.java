package com.greenlemonmedia.feeghe.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.R;
import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.APIUtils;
import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.UserService;
import com.greenlemonmedia.feeghe.storage.Session;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tontonskie on 4/9/15.
 */
public class EditProfileFragment extends MainActivityFragment {

  private Session session;
  private MainActivity context;
  private JSONObject currentUser;
  private EditText editTxtFirstName;
  private EditText editTxtLastName;
  private EditText editTxtEmail;
  private UserService userService;
  private EditText editTxtPhone;
  private ImageView imgViewProfile;
  private Button btnSave;
  private AlertDialog diagEditPic;
  private Uri forUploadFromGallery;

  private static final int UPLOAD_FILE = 1;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_edit_profile, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    context = getCurrentActivity();
    session = Session.getInstance(context);
    userService = new UserService(context);

    editTxtEmail = (EditText) context.findViewById(R.id.editTxtEditProfileEmail);
    editTxtFirstName = (EditText) context.findViewById(R.id.editTxtEditProfileFirstName);
    editTxtLastName = (EditText) context.findViewById(R.id.editTxtEditProfileLastName);
    editTxtPhone = (EditText) context.findViewById(R.id.editTxtEditProfilePhoneNumber);
    imgViewProfile = (ImageView) context.findViewById(R.id.imgViewEditProfilePic);
    btnSave = (Button) context.findViewById(R.id.btnEditProfileSave);

    setUserInfo(session.getCurrentUser().toJSON());
    userService.get(session.getUserId(), new APIService.GetCallback() {

      @Override
      public void onSuccess(ResponseObject response) {
        setUserInfo(response.getContent());
      }

      @Override
      public void onFail(int statusCode, String error, JSONObject validationError) {
        Toast.makeText(context, error, Toast.LENGTH_LONG).show();
      }
    });

    setupUIEvents();
  }

  private void setUserInfo(JSONObject userInfo) {
    session.setCurrentUser(userInfo);
    try {
      if (!userInfo.isNull("email")) {
        editTxtEmail.setText(userInfo.getString("email"));
      }
      if (!userInfo.isNull("lastName")) {
        editTxtLastName.setText(userInfo.getString("lastName"));
      }
      if (!userInfo.isNull("firstName")) {
        editTxtFirstName.setText(userInfo.getString("firstName"));
      }
      if (!userInfo.isNull("phoneNumber")) {
        editTxtPhone.setText(userInfo.getString("phoneNumber"));
      }
      if (userInfo.isNull("profilePic")) {
        APIUtils.getPicasso(context)
          .load(R.drawable.placeholder)
          .into(imgViewProfile);
      } else {
        String profilePic = userInfo.getJSONObject("profilePic").getString("original");
        if (currentUser == null || !profilePic.equals(currentUser.getJSONObject("profilePic").getString("original"))) {
          APIUtils.getPicasso(context)
            .load(Uri.parse(APIUtils.getStaticUrl(profilePic)))
            .error(R.drawable.placeholder)
            .placeholder(R.drawable.placeholder)
            .into(imgViewProfile);
        }
      }
      currentUser = userInfo;
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  @Override
  public String getTabId() {
    return null;
  }

  @Override
  protected void setupUIEvents() {
    btnSave.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        String email = editTxtEmail.getText().toString();
        String firstName = editTxtFirstName.getText().toString();
        String lastName = editTxtLastName.getText().toString();

        if (firstName.isEmpty()) {
          Toast.makeText(context, "Invalid first name", Toast.LENGTH_LONG).show();
          return;
        }

        if (lastName.isEmpty()) {
          Toast.makeText(context, "Invalid last name", Toast.LENGTH_LONG).show();
          return;
        }

        if (!APIUtils.isValidEmail(email)) {
          Toast.makeText(context, "Invalid email address", Toast.LENGTH_LONG).show();
          return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        final Uri forUpload = forUploadFromGallery;
        JSONObject updates = new JSONObject();
        try {
          updates.put("firstName", firstName);
          updates.put("lastName", lastName);
          updates.put("email", email);
        } catch (JSONException e) {
          e.printStackTrace();
        }
        userService.update(session.getUserId(), updates, new APIService.UpdateCallback() {

          @Override
          public void onSuccess(ResponseObject response) {
            if (forUpload != null) {
              uploadFile(forUpload);
              return;
            }
            session.setCurrentUser(response);
            btnSave.setEnabled(true);
            btnSave.setText("Save Changes");
          }

          @Override
          public void onFail(int statusCode, String error, JSONObject validationError) {
            Toast.makeText(context, APIUtils.toValidationErrString(validationError), Toast.LENGTH_LONG).show();
            btnSave.setEnabled(true);
            btnSave.setText("Save Changes");
          }
        });
      }
    });

    AlertDialog.Builder diagEditPicBuilder = new AlertDialog.Builder(context);
    String[] actions = {
        "Take a Photo",
        "Select from Gallery"
    };
    diagEditPicBuilder.setItems(actions, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        switch (which) {
          case 0:
            break;
          case 1:
            selectPicture();
            break;
        }
      }
    });
    diagEditPic = diagEditPicBuilder.create();

    imgViewProfile.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        diagEditPic.show();
      }
    });
  }

  private void selectPicture() {
    Intent intent = new Intent();
    intent.setType("image/*");
    intent.setAction(Intent.ACTION_GET_CONTENT);
    startActivityForResult(Intent.createChooser(intent, "Select Picture"), UPLOAD_FILE);
  }

  @Override
  public void onActivityResult(int reqCode, int resCode, Intent data) {
    super.onActivityResult(reqCode, resCode, data);
    if (resCode == Activity.RESULT_OK && reqCode == UPLOAD_FILE) {
      forUploadFromGallery = data.getData();
      APIUtils.getPicasso(context)
        .load(forUploadFromGallery)
        .placeholder(R.drawable.placeholder)
        .error(R.drawable.placeholder)
        .into(imgViewProfile);
    }
  }

  private void uploadFile(Uri uri) {
    APIService.UploadProgressListener progressCb = new APIService.UploadProgressListener() {

      @Override
      public void onProgress(int completed) {

      }
    };

    APIService.UpdateCallback doneCb = new APIService.UpdateCallback() {

      @Override
      public void onSuccess(ResponseObject response) {
        session.setCurrentUser(response);
        btnSave.setEnabled(true);
        btnSave.setText("Save Changes");
      }

      @Override
      public void onFail(int statusCode, String error, JSONObject validationError) {
        Toast.makeText(context, error, Toast.LENGTH_LONG).show();
        btnSave.setEnabled(true);
        btnSave.setText("Save Changes");
      }
    };

    userService.upload(APIUtils.getPath(context, uri), progressCb, doneCb);
  }

  @Override
  protected void setupSocketEvents() {

  }

  @Override
  public void onKeyboardShow() {
    btnSave.setVisibility(View.GONE);
  }

  @Override
  public void onKeyboardHide() {
    btnSave.setVisibility(View.VISIBLE);
  }

  @Override
  public String getFragmentId() {
    return MainActivity.FRAG_EDIT_PROFILE;
  }

  @Override
  public boolean onSearchQuerySubmit(String query) {
    return false;
  }

  @Override
  public boolean onSearchQueryChange(String query) {
    return false;
  }

  @Override
  public boolean onSearchClose() {
    return false;
  }
}
