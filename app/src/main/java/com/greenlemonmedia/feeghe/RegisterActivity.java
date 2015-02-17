package com.greenlemonmedia.feeghe;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.greenlemonmedia.feeghe.api.APIService;
import com.greenlemonmedia.feeghe.api.ResponseObject;
import com.greenlemonmedia.feeghe.api.UserService;
import com.greenlemonmedia.feeghe.api.Util;
import com.greenlemonmedia.feeghe.storage.Session;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class RegisterActivity extends Activity {

  private Button btnRegister;
  private Spinner spinCountryCodes;
  private Context context;
  private EditText editTxtPhoneNumber;
  private UserService userService;
  private ViewFlipper viewSwitcher;
  private BroadcastReceiver verificationCodeReceiver;
  private Button btnCancelVerification;
  private Button btnVerify;
  private String verificationId;
  private EditText editTxtVerificationCode;
  private Session session;
  private TextView txtVerificationError;
  private TextView txtRegisterError;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    context = this;
    session = Session.getInstance(this);
    if (session.isLoggedIn()) {
      goToMainActivity();
      return;
    }
    setContentView(R.layout.activity_register);

    userService = new UserService(this);
    viewSwitcher = (ViewFlipper) findViewById(R.id.viewSwitcherRegister);
    btnRegister = (Button) findViewById(R.id.btnRegister);
    spinCountryCodes = (Spinner) findViewById(R.id.spinCountryCodes);
    editTxtPhoneNumber = (EditText) findViewById(R.id.editTxtPhoneNumber);
    btnCancelVerification = (Button) findViewById(R.id.btnCancelVerification);
    btnVerify = (Button) findViewById(R.id.btnVerify);
    editTxtVerificationCode = (EditText) findViewById(R.id.editTxtVerificationCode);
    txtVerificationError = (TextView) findViewById(R.id.txtVerificationError);
    txtRegisterError = (TextView) findViewById(R.id.txtRegisterError);

    verificationCodeReceiver = new BroadcastReceiver() {

      @Override
      public void onReceive(Context context, Intent intent) {

      }
    };

    verificationId = session.get(Session.VERIFICATION_KEY);
    if (verificationId != null) {
      viewSwitcher.showNext();
    }
    setupCountryCodes();
    setupUIEvents();
  }

  private void setupCountryCodes() {
    InputStream is = getResources().openRawResource(R.raw.country_codes);
    byte[] buffer = null;
    try {
      buffer = new byte[is.available()];
      is.read(buffer);
      is.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    ArrayAdapter<JSONObject> countryCodes = null;
    TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    String countryCode = telephony.getSimCountryIso().trim();
    int position = 0;
    try {
      JSONArray countries = new JSONArray(new String(buffer, "UTF-8"));
      int countriesLength = countries.length();
      for (int i = 0; i < countriesLength; i++) {
        if (countries.getJSONObject(i).getString("dialCode").equals(countryCode)) {
          position = i;
          break;
        }
      }
      countryCodes = new CountryCodesAdapter(Util.toList(countries));
    } catch (JSONException | UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    spinCountryCodes.setAdapter(countryCodes);
    spinCountryCodes.setSelection(position);
    editTxtPhoneNumber.setText(telephony.getLine1Number());
  }

  private void setupUIEvents() {
    btnRegister.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        String countryCode = ((JSONObject) spinCountryCodes.getSelectedItem()).optString("dialCode");
        String phoneNumber = editTxtPhoneNumber.getText().toString();
        if (phoneNumber.isEmpty()) {
          txtRegisterError.setText("Phone number is required");
          txtRegisterError.setVisibility(View.VISIBLE);
          return;
        }
        txtRegisterError.setVisibility(View.GONE);
        phoneNumber = countryCode + phoneNumber;
        final ProgressDialog preloader = Util.showPreloader(context);
        userService.register(phoneNumber, new APIService.SaveCallback() {

          @Override
          public void onSuccess(ResponseObject response) {
            try {
              verificationId = response.getContent().getString("id");
            } catch (JSONException e) {
              e.printStackTrace();
            }
            session.set(Session.VERIFICATION_KEY, verificationId);
            registerReceiver(verificationCodeReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
            viewSwitcher.showNext();
            preloader.dismiss();
          }

          @Override
          public void onFail(int statusCode, String error) {
            txtRegisterError.setText(error);
            txtRegisterError.setVisibility(View.VISIBLE);
            preloader.dismiss();
          }
        });
      }
    });

    btnVerify.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        String verificationCode = editTxtVerificationCode.getText().toString();
        if (verificationCode.isEmpty()) {
          txtVerificationError.setText("Verification code is required");
          txtVerificationError.setVisibility(View.VISIBLE);
          return;
        }
        txtVerificationError.setVisibility(View.GONE);
        final ProgressDialog preloader = Util.showPreloader(context);
        userService.verify(verificationId, verificationCode, new APIService.UpdateCallback() {

          @Override
          public void onSuccess(ResponseObject response) {
            unregisterReceiver(verificationCodeReceiver);
            JSONObject data = response.getContent();
            try {
              session.setCredentials(data.getString("token"), data.getString("user"));
            } catch (JSONException e) {
              e.printStackTrace();
            }
            preloader.dismiss();
            goToMainActivity();
          }

          @Override
          public void onFail(int statusCode, String error) {
            txtVerificationError.setText(error);
            txtVerificationError.setVisibility(View.VISIBLE);
            preloader.dismiss();
          }
        });
      }
    });

    btnCancelVerification.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        session.remove(Session.VERIFICATION_KEY);
        unregisterReceiver(verificationCodeReceiver);
        viewSwitcher.showPrevious();
      }
    });
  }

  public void goToMainActivity() {
    startActivity(new Intent(this, MainActivity.class));
  }

  private class CountryCodeViewHolder {
    public ImageView imgViewCountry;
    public TextView txtCountryCode;
  }

  private class CountryCodesAdapter extends ArrayAdapter<JSONObject> {

    public CountryCodesAdapter(ArrayList<JSONObject> countryCodes) {
      super(context, R.layout.per_country_code, countryCodes);
    }

    private View getCountryCodeView(int position, View convertView, ViewGroup parent, boolean forGetView) {
      CountryCodeViewHolder countryViewHolder;
      if (convertView == null) {
        convertView = getLayoutInflater().inflate(R.layout.per_country_code, parent, false);
        countryViewHolder = new CountryCodeViewHolder();
        countryViewHolder.imgViewCountry = (ImageView) convertView.findViewById(R.id.imgViewCountry);
        countryViewHolder.txtCountryCode = (TextView) convertView.findViewById(R.id.txtCountryCode);
        convertView.setTag(countryViewHolder);
      } else {
        countryViewHolder = (CountryCodeViewHolder) convertView.getTag();
      }
      JSONObject country = getItem(position);
      try {
        if (forGetView) {
          countryViewHolder.txtCountryCode.setText(country.getString("dialCode"));
        } else {
          countryViewHolder.txtCountryCode.setText(country.getString("dialCode") + ' ' + country.getString("name"));
        }
      } catch (JSONException e) {
        e.printStackTrace();
      }
      return convertView;
    }

    public View getDropDownView(int position, View convertView, ViewGroup parent) {
      return getCountryCodeView(position, convertView, parent, false);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
      return getCountryCodeView(position, convertView, parent, true);
    }
  }
}
