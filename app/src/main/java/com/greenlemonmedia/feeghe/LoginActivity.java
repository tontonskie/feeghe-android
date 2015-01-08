package com.greenlemonmedia.feeghe;

import android.app.Activity;
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

import com.greenlemonmedia.feeghe.storage.Session;
import com.greenlemonmedia.feeghe.tasks.LoginTask;
import com.greenlemonmedia.feeghe.tasks.RegisterTask;
import com.greenlemonmedia.feeghe.tasks.VerifyTask;

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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    session = Session.getInstance(this);
    if (session.isLoggedIn()) {
      goToMainActivity();
      return;
    }

    TelephonyManager telManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    String phoneNumber = telManager.getLine1Number();

    setContentView(R.layout.activity_login);

    context = this;
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
        LoginTask login = new LoginTask(
          context,
          txtLoginPhoneNumber.getText().toString(),
          txtLoginPassword.getText().toString(),
          new LoginTask.LoginListener() {

            @Override
            public void onSuccess(String token, String userId) {
              goToMainActivity();
            }

            @Override
            public void onFail(int statusCode, String error) {
              txtViewLoginError.setText(error);
              txtViewLoginError.setVisibility(View.VISIBLE);
            }
          }
        );
        login.execute();
      }
    });

    btnRegister = (Button) findViewById(R.id.btnRegister);
    btnRegister.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        if (verifyId == null) {
          RegisterTask register = new RegisterTask(
            context,
            txtRegisterPhoneNumber.getText().toString(),
            new RegisterTask.RegisterListener() {

              @Override
              public void onSuccess(String verificationId) {
                verifyId = verificationId;
                btnRegister.setText("Verify");
                btnBackToLogin.setText("Cancel");
                txtViewRegisterError.setVisibility(View.GONE);
                txtVerificationCode.setVisibility(View.VISIBLE);
                txtRegisterPhoneNumber.setVisibility(View.INVISIBLE);
                Toast.makeText(context, "SMS Sent", Toast.LENGTH_LONG).show();
              }

              @Override
              public void onFail(int statusCode, String error) {
                txtViewRegisterError.setText(error);
                txtViewRegisterError.setVisibility(View.VISIBLE);
              }
            }
          );
          register.execute();
          return;
        }
        VerifyTask verify = new VerifyTask(
          context,
          verifyId,
          txtVerificationCode.getText().toString(),
          new VerifyTask.VerifyListener() {

            @Override
            public void onSuccess(String token, String userId) {
              goToMainActivity();
            }

            @Override
            public void onFail(int statusCode, String error) {
              txtViewRegisterError.setText(error);
              txtViewRegisterError.setVisibility(View.VISIBLE);
            }
          }
        );
        verify.execute();
      }
    });
  }

  public void goToMainActivity() {
    Intent mainActivity = new Intent(this, MainActivity.class);
    startActivity(mainActivity);
  }
}
