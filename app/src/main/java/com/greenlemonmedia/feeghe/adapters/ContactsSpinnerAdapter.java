package com.greenlemonmedia.feeghe.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.greenlemonmedia.feeghe.R;
import com.greenlemonmedia.feeghe.api.APIUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by GreenLemon on 3/19/15.
 */
public class ContactsSpinnerAdapter extends ArrayAdapter<JSONObject> {

  private int resId;

  public ContactsSpinnerAdapter(Context context, int resourceId, ArrayList<JSONObject> contacts) {
    super(context, resourceId, contacts);
    resId = resourceId;
  }

  public ContactsSpinnerAdapter(Context context, ArrayList<JSONObject> contacts) {
    this(context, R.layout.per_recipient, contacts);
  }

  private class ContactViewHolder {
    public TextView txtContactName;
  }

  private View getContactView(int position, View convertView, ViewGroup parent, boolean forGetView) {
    ContactViewHolder contactViewHolder;
    if (convertView == null) {
      LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      convertView = vi.inflate(resId, parent, false);
      contactViewHolder = new ContactViewHolder();
      contactViewHolder.txtContactName = (TextView) convertView.findViewById(R.id.txtRecipientContactName);
      convertView.setTag(contactViewHolder);
    } else {
      contactViewHolder = (ContactViewHolder) convertView.getTag();
    }
    JSONObject contact = getItem(position);
    try {
      contactViewHolder.txtContactName.setText(APIUtils.getFullName(contact.getJSONObject("user")));
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return convertView;
  }

  public View getDropDownView(int position, View convertView, ViewGroup parent) {
    return getContactView(position, convertView, parent, false);
  }

  public View getView(int position, View convertView, ViewGroup parent) {
    return getContactView(position, convertView, parent, true);
  }
}
