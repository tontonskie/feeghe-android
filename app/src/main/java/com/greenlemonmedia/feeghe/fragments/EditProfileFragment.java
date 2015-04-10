package com.greenlemonmedia.feeghe.fragments;

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
        JSONObject newUserInfo = response.getContent();
        session.setCurrentUser(newUserInfo);
        setUserInfo(newUserInfo);

      }

      @Override
      public void onFail(int statusCode, String error) {

      }
    });

    setupUIEvents();
  }

  private void setUserInfo(JSONObject userInfo) {
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
      String profilePic = userInfo.getJSONObject("profilePic").getString("original");
      if (currentUser == null || !profilePic.equals(currentUser.getJSONObject("profilePic").getString("original"))) {
        APIUtils.getPicasso(context)
          .load(Uri.parse(APIUtils.getStaticUrl(profilePic)))
          .error(R.drawable.placeholder)
          .placeholder(R.drawable.placeholder)
          .into(imgViewProfile);
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
    currentUser = userInfo;
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
        btnSave.setEnabled(false);
        btnSave.setText("Saving...");
        JSONObject updates = new JSONObject();
        try {
          updates.put("firstName", editTxtFirstName.getText().toString());
          updates.put("lastName", editTxtLastName.getText().toString());
          updates.put("email", editTxtEmail.getText().toString());
        } catch (JSONException e) {
          e.printStackTrace();
        }
        userService.update(session.getUserId(), updates, new APIService.UpdateCallback() {

          @Override
          public void onSuccess(ResponseObject response) {
            session.setCurrentUser(response);
            btnSave.setEnabled(true);
            btnSave.setText("Save Changes");
          }

          @Override
          public void onFail(int statusCode, String error) {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show();
            btnSave.setEnabled(true);
            btnSave.setText("Save Changes");
          }
        });
      }
    });
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
}
