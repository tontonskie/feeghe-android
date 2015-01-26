package com.greenlemonmedia.feeghe;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.UserService;
import com.greenlemonmedia.feeghe.storage.Session;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends Activity {

  private Button btnSwitchRegister;
  private Button btnBackToLogin;
  private Button btnRegister;
  private ViewFlipper viewFlipper;
  private EditText txtLoginPhoneNumber;
  private EditText txtRegisterPhoneNumber;
  private EditText txtLoginPassword;
  private TextView txtViewRegisterError;
  private TextView txtViewLoginError;
  private EditText txtVerificationCode;
  private Button btnLogin;
  private Activity context;
  private Session session;
  private String verifyId;
  private UserService userService;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    context = this;
    session = Session.getInstance(this);
    userService = new UserService(context);
    if (session.isLoggedIn()) {
      goToMainActivity();
      return;
    }

    TelephonyManager telManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    String phoneNumber = telManager.getLine1Number();

    setContentView(R.layout.activity_login);

    txtViewLoginError = (TextView) findViewById(R.id.txtViewLoginError);
    txtViewRegisterError = (TextView) findViewById(R.id.txtViewRegisterError);
    txtLoginPhoneNumber = (EditText) findViewById(R.id.txtLoginPhoneNumber);
    txtVerificationCode = (EditText) findViewById(R.id.txtVerificationCode);
    txtLoginPassword = (EditText) findViewById(R.id.txtLoginPassword);
    txtRegisterPhoneNumber = (EditText) findViewById(R.id.txtRegisterPhoneNumber);

    if (phoneNumber != null) {
      txtLoginPhoneNumber.setText(phoneNumber);
      txtRegisterPhoneNumber.setText(phoneNumber);
    }

    viewFlipper = (ViewFlipper) findViewById(R.id.viewSwitcherLogin);
    btnSwitchRegister = (Button) findViewById(R.id.btnSwitchRegister);
    btnSwitchRegister.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        viewFlipper.showNext();
      }
    });

    btnBackToLogin = (Button) findViewById(R.id.btnBackToLogin);
    btnBackToLogin.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        if (verifyId == null) {
          viewFlipper.showPrevious();
          return;
        }
        verifyId = null;
        btnRegister.setText("Sign Up");
        btnBackToLogin.setText("Back to Login");
        txtRegisterPhoneNumber.setVisibility(View.VISIBLE);
        txtVerificationCode.setVisibility(View.INVISIBLE);
      }
    });

    btnLogin = (Button) findViewById(R.id.btnLogin);
    btnLogin.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        String phoneNumber = txtLoginPhoneNumber.getText().toString();
        String password = txtLoginPassword.getText().toString();
        if (phoneNumber.isEmpty() || password.isEmpty()) {
          txtViewLoginError.setText("Phone number and password is required");
          txtViewLoginError.setVisibility(View.VISIBLE);
          return;
        }
        txtViewLoginError.setVisibility(View.GONE);
        final ProgressDialog preloader = ProgressDialog.show(context, null, "Please wait...", true, false);
        APIService.GetCallback callback = new APIService.GetCallback() {

          @Override
          public void onSuccess(ResponseObject response) {
            try {
              JSONObject user = response.getContent();
              session.setCredentials(user.getString("token"), user.getString("user"));
            } catch (JSONException e) {
              e.printStackTrace();
            }
            preloader.dismiss();
            goToMainActivity();
          }

          @Override
          public void onFail(int statusCode, String error) {
            txtViewLoginError.setText(error);
            txtViewLoginError.setVisibility(View.VISIBLE);
            preloader.dismiss();
          }
        };
        userService.login(phoneNumber, password, callback);
      }
    });

    btnRegister = (Button) findViewById(R.id.btnRegister);
    btnRegister.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        if (verifyId == null) {
          String phoneNumber = txtRegisterPhoneNumber.getText().toString();
          if (phoneNumber.isEmpty()) {
            txtViewRegisterError.setText("Phone number is required");
            txtViewRegisterError.setVisibility(View.VISIBLE);
            return;
          }
          txtViewRegisterError.setVisibility(View.GONE);
          final ProgressDialog preloader = ProgressDialog.show(context, null, "Please wait...", true, false);
          userService.register(phoneNumber, new APIService.SaveCallback() {

            @Override
            public void onSuccess(ResponseObject response) {
              try {
                verifyId = response.getContent().getString("id");
              } catch (JSONException e) {
                e.printStackTrace();
              }
              btnRegister.setText("Verify");
              btnBackToLogin.setText("Cancel");
              txtViewRegisterError.setVisibility(View.GONE);
              txtVerificationCode.setVisibility(View.VISIBLE);
              txtRegisterPhoneNumber.setVisibility(View.INVISIBLE);
              Toast.makeText(context, "SMS Sent", Toast.LENGTH_LONG).show();
              preloader.dismiss();
            }

            @Override
            public void onFail(int statusCode, String error) {
              txtViewRegisterError.setText(error);
              txtViewRegisterError.setVisibility(View.VISIBLE);
              preloader.dismiss();
            }
          });
          return;
        }
        String verificationCode = txtVerificationCode.getText().toString();
        if (verificationCode.isEmpty()) {
          txtViewRegisterError.setText("Verification code is required");
          txtViewRegisterError.setVisibility(View.VISIBLE);
          return;
        }
        txtViewRegisterError.setVisibility(View.GONE);
        final ProgressDialog preloader = ProgressDialog.show(context, null, "Please wait...", true, false);
        userService.verify(verifyId, verificationCode, new APIService.UpdateCallback() {

          @Override
          public void onSuccess(ResponseObject response) {
            try {
              JSONObject result = response.getContent();
              session.setCredentials(result.getString("token"), result.getString("user"));
            } catch (JSONException ex) {
              ex.printStackTrace();
            }
            preloader.dismiss();
            goToMainActivity();
          }

          @Override
          public void onFail(int statusCode, String error) {
            txtViewRegisterError.setText(error);
            txtViewRegisterError.setVisibility(View.VISIBLE);
            preloader.dismiss();
          }
        });
      }
    });
  }

  public void goToMainActivity() {
    startActivity(new Intent(this, MainActivity.class));
  }
}
