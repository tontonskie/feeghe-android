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
import android.widget.ViewFlipper;

import com.greenlemonmedia.feeghe.listeners.LoginListener;
import com.greenlemonmedia.feeghe.storage.UserSession;
import com.greenlemonmedia.feeghe.tasks.LoginTask;

public class LoginActivity extends Activity {

    private Button btnSwitchRegister;
    private Button btnBackToLogin;
    private ViewFlipper viewFlipper;
    private EditText txtLoginPhoneNumber;
    private EditText txtRegisterPhoneNumber;
    private EditText txtLoginPassword;
    private Button btnLogin;
    private Context context;
    private UserSession session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = UserSession.getInstance(this);
        if (session.isLoggedIn()) {
            goToMainActivity();
        }

        TelephonyManager telManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String phoneNumber = telManager.getLine1Number();

        setContentView(R.layout.activity_login);

        context = this;
        txtLoginPhoneNumber = (EditText) findViewById(R.id.txtLoginPhoneNumber);
        txtLoginPassword = (EditText) findViewById(R.id.txtLoginPassword);
        txtRegisterPhoneNumber = (EditText) findViewById(R.id.txtRegisterPhoneNumber);
        if (!phoneNumber.isEmpty()) {
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
                viewFlipper.showPrevious();
            }
        });

        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                LoginTask login = new LoginTask(
                        txtLoginPhoneNumber.getText().toString(),
                        txtLoginPassword.getText().toString(),
                        new ProgressDialog(context),
                        session,
                        new LoginListener() {

                            @Override
                            public void onSuccess(String token) {
                                goToMainActivity();
                            }
                        }
                );
                login.execute();
            }
        });
    }

    public void goToMainActivity() {
        Intent mainActivity = new Intent(this, MainActivity.class);
        startActivity(mainActivity);
    }
}
