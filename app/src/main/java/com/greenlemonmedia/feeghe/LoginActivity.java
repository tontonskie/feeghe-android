package com.greenlemonmedia.feeghe;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ViewFlipper;

public class LoginActivity extends Activity {

	private Button btnSwitchRegister;
	private Button btnBackToLogin;
	private ViewFlipper viewSwitcherLogin;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		viewSwitcherLogin = (ViewFlipper) findViewById(R.id.viewSwitcherLogin);
		btnSwitchRegister = (Button) findViewById(R.id.btnSwitchRegister);
		btnSwitchRegister.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				viewSwitcherLogin.showNext();
			}
		});

		btnBackToLogin = (Button) findViewById(R.id.btnBackToLogin);
		btnBackToLogin.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				viewSwitcherLogin.showPrevious();
			}
		});
	}
}
