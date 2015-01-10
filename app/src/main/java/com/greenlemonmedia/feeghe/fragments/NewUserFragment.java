package com.greenlemonmedia.feeghe.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.greenlemonmedia.feeghe.R;
import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.tasks.NewUserTask;

/**
 * A simple {@link Fragment} subclass.
 */
public class NewUserFragment extends Fragment {

  private Button btnChangePass;
  private EditText txtNewPassword;
  private EditText txtConfirmPassword;
  private TextView txtViewNewUserError;
  private ViewAnimator view;
  private Spinner selectGender;
  private Activity context;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_new_user, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    context = getActivity();
    view = (ViewAnimator) getView();
    view.setOutAnimation(AnimationUtils.loadAnimation(context, R.anim.abc_slide_out_top));
    view.setInAnimation(AnimationUtils.loadAnimation(context, R.anim.abc_slide_in_bottom));

    selectGender = (Spinner) view.findViewById(R.id.selectGender);
    txtViewNewUserError = (TextView) view.findViewById(R.id.txtViewNewUserError);
    txtNewPassword = (EditText) view.findViewById(R.id.txtNewPassword);
    txtConfirmPassword = (EditText) view.findViewById(R.id.txtConfirmPassword);

    btnChangePass = (Button) view.findViewById(R.id.btnChangePass);
    btnChangePass.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        String password = txtNewPassword.getText().toString();
        String confirmPassword = txtConfirmPassword.getText().toString();
        if (!password.equals(confirmPassword)) {
          txtViewNewUserError.setText("Password doesn't match");
          txtViewNewUserError.setVisibility(View.VISIBLE);
          return;
        } else if (password.isEmpty()) {
          txtViewNewUserError.setText("Password is required");
          txtViewNewUserError.setVisibility(View.VISIBLE);
          return;
        }

        txtViewNewUserError.setVisibility(View.GONE);
        NewUserTask newUserTask = new NewUserTask(
          context,
          selectGender.getSelectedItem().toString(),
          password,
          new NewUserTask.NewUserListener() {

            @Override
            public void onSuccess(ResponseObject updatedUser) {
              view.showNext();
            }

            @Override
            public void onFail(int statusCode, String error) {
              txtViewNewUserError.setText(error);
              txtViewNewUserError.setVisibility(View.VISIBLE);
            }
          }
        );
        newUserTask.execute();
      }
    });
  }
}
