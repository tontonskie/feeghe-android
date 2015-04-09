package com.greenlemonmedia.feeghe.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

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
    currentUser = session.getCurrentUser().toJSON();

    editTxtEmail = (EditText) context.findViewById(R.id.editTxtEditProfileFirstName);
    editTxtFirstName = (EditText) context.findViewById(R.id.editTxtEditProfileFirstName);
    editTxtLastName = (EditText) context.findViewById(R.id.editTxtEditProfileLastName);
    editTxtPhone = (EditText) context.findViewById(R.id.editTxtEditProfilePhoneNumber);
    imgViewProfile = (ImageView) context.findViewById(R.id.imgViewEditProfilePic);

    setUserInfo();

    userService.get(session.getUserId(), new APIService.GetCallback() {

      @Override
      public void onSuccess(ResponseObject response) {
        currentUser = response.getContent();
        session.setCurrentUser(currentUser);
        setUserInfo();
      }

      @Override
      public void onFail(int statusCode, String error) {

      }
    });
  }

  private void setUserInfo() {
    try {
      if (!currentUser.isNull("email")) {
        editTxtEmail.setText(currentUser.getString("email"));
      }
      if (!currentUser.isNull("lastName")) {
        editTxtLastName.setText(currentUser.getString("lastName"));
      }
      if (!currentUser.isNull("firstName")) {
        editTxtFirstName.setText(currentUser.getString("firstName"));
      }
      if (!currentUser.isNull("phoneNumber")) {
        editTxtPhone.setText(currentUser.getString("phoneNumber"));
      }
      if (!currentUser.isNull("profilePic")) {
        APIUtils.getPicasso(context)
          .load(Uri.parse(APIUtils.getStaticUrl(currentUser.getJSONObject("profilePic").getString("original"))))
          .error(R.drawable.placeholder)
          .placeholder(R.drawable.placeholder)
          .into(imgViewProfile);
      }
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

  }

  @Override
  protected void setupSocketEvents() {

  }

  @Override
  public String getFragmentId() {
    return MainActivity.FRAG_EDIT_PROFILE;
  }
}
