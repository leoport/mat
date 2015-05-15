/*
 * Copyright (C) 2015 Liang Jing
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.leopub.mat.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.leopub.mat.Configure;
import org.leopub.mat.HttpUtil;
import org.leopub.mat.Logger;
import org.leopub.mat.R;
import org.leopub.mat.UserManager;
import org.leopub.mat.exception.AuthException;
import org.leopub.mat.exception.NetworkDataException;
import org.leopub.mat.exception.NetworkException;
import org.leopub.mat.model.PersonalInfoItem;
import org.leopub.mat.model.User;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class PersonalInfoActivity extends ListActivity {
    public final static String PERSON_ID = "org.leopub.mat.personId";

    private PersonalInfoArrayAdapter mArrayAdapter;
    private List<PersonalInfoItem> mItemList;
    private OnClickListener mEditButtonListener;
    private OnClickListener mHelpButtonListener;
    private String mPersonId;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPersonId = getIntent().getStringExtra(PERSON_ID);
        setContentView(R.layout.activity_personal_info);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mEditButtonListener = new EditButtonListener();
        mHelpButtonListener = new HelpButtonListener();
        mItemList = new ArrayList<PersonalInfoItem>();
        mArrayAdapter = null;
        updateView();
        GetPersonalInfoTask getTask= new GetPersonalInfoTask();
        getTask.execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateView() {
        int pos = 0;
        if (mArrayAdapter != null) {
            pos = getListView().getFirstVisiblePosition();
        }
        mArrayAdapter = new PersonalInfoArrayAdapter(this, R.layout.personal_info_item, R.id.info_item_title, mItemList);
        getListView().setAdapter(mArrayAdapter);
        getListView().setSelection(pos);
    }

    private class PersonalInfoArrayAdapter extends ArrayAdapter<PersonalInfoItem> {
        public PersonalInfoArrayAdapter(Context context, int resource, int textViewId, List<PersonalInfoItem> items) {
            super(context, resource, textViewId, items);
        }

        @SuppressLint("InflateParams")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.personal_info_item, null);
            }
            PersonalInfoItem item = getItem(position);
            TextView itemContentView = (TextView) convertView.findViewById(R.id.info_item_title) ;
            itemContentView.setText(item.getTitle());

            TextView itemInfoView = (TextView) convertView.findViewById(R.id.info_item_value);
            itemInfoView.setText(item.getValue());

            Button editButton = (Button) convertView.findViewById(R.id.info_item_edit);
            editButton.setTag(item.getKey());
            if (!item.isEditable()) {
                editButton.setVisibility(View.GONE);
            } else {
                editButton.setOnClickListener(mEditButtonListener);
                editButton.setVisibility(View.VISIBLE);
            }
            Button helpButton = (Button) convertView.findViewById(R.id.info_item_help);
            helpButton.setTag(item.getKey());
            if (item.getHint() == null || item.getHint().isEmpty()) {
                helpButton.setVisibility(View.GONE);
            } else {
                helpButton.setOnClickListener(mHelpButtonListener);
                helpButton.setVisibility(View.VISIBLE);
            }

            return convertView;
        }
    } 

    private List<PersonalInfoItem> parseJSON(String s) throws NetworkDataException {
        List<PersonalInfoItem> itemList = new ArrayList<PersonalInfoItem>();
        try {
            JSONArray array = new JSONArray(s);
            int n = array.length();
            for (int i = 0; i < n; i++) {
                JSONObject obj = array.getJSONObject(i);
                PersonalInfoItem item = new PersonalInfoItem(obj);
                itemList.add(item);
            }
        } catch (JSONException e) {
            throw new NetworkDataException("Parse the JSON of personal information failed");
        }
        return itemList;
    } 

    private class GetPersonalInfoTask extends AsyncTask<String, Void, String> {
        public GetPersonalInfoTask() {
            super();
        }

        @Override
        protected String doInBackground(String... args) {
            User user = UserManager.getInstance().getCurrentUser();
            String response = "";
            try {
                String url = Configure.INFO_GET_URL;
                if (mPersonId != null) {
                    url = url + "?id=" + mPersonId;
                }
                response = HttpUtil.getUrl(url, user);
                mItemList = parseJSON(response);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        PersonalInfoActivity.this.updateView();
                    }
                 });
            } catch (NetworkException e) {
                showToastHint(getString(R.string.error_network));
            } catch (AuthException e) {
                showToastHint(getString(R.string.error_auth_fail));
            } catch (NetworkDataException e) {
                Logger.i("PERSON INFO", "GET PERSONAL INFO FAILED:" + response);
                showToastHint(getString(R.string.error_network_data));
            }
            return "";
        }
        private void showToastHint(final String message) {
            runOnUiThread(new Runnable() {
               @Override
               public void run() {
                    Toast.makeText(PersonalInfoActivity.this, message, Toast.LENGTH_LONG).show();
               }
            });
        }
    }

    private class UpdatePersonalInfoTask extends AsyncTask<String, Void, String> {
        public UpdatePersonalInfoTask() {
            super();
        }

        @Override
        protected String doInBackground(String... args) {
            User user = UserManager.getInstance().getCurrentUser();
            try {
                String res = HttpUtil.postURL(user, Configure.INFO_UPDATE_URL, args[0]);
                mItemList = parseJSON(res);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        PersonalInfoActivity.this.updateView();
                    }
                 });
            } catch (NetworkException e) {
                showToastHint(getString(R.string.error_network));
            } catch (AuthException e) {
                showToastHint(getString(R.string.error_auth_fail));
            } catch (NetworkDataException e) {
                showToastHint(getString(R.string.error_network_data));
            }
            return "";
        }

        private void showToastHint(final String message) {
            runOnUiThread(new Runnable() {
               @Override
               public void run() {
                    Toast.makeText(PersonalInfoActivity.this, message, Toast.LENGTH_LONG).show();
               }
            });
        }
    }

    private class EditButtonListener implements OnClickListener {
        private PersonalInfoItem mInfoItem;

        @Override
        public void onClick(View view) {
            String s = (String) view.getTag();
            for (PersonalInfoItem item : mItemList) {
                if (item.getKey().equals(s)) {
                    mInfoItem = item;
                    if (mInfoItem.getType() == PersonalInfoItem.InfoType.Option) {
                        buildOptionDialog();
                    } else if(mInfoItem.getType() == PersonalInfoItem.InfoType.Date){
                        String value = item.getValue();
                        if (value == null || value.length() == 0) {
                            value = new Date().toString();
                        }
                        Scanner scanner = new Scanner(value);
                        scanner.useDelimiter("-");
                        int year = scanner.nextInt();
                        int month = scanner.nextInt();
                        int day = scanner.nextInt();
                        scanner.close();
                        buildDatePickerDialog(year, month, day);
                    } else {
                        buildTextInputDialog();
                    }
                    break;
                }
            }
        }

        private void update(String newValue) {
            try {
                String params = mInfoItem.getKey() + "=" + URLEncoder.encode(newValue, "utf-8");
                if (mPersonId != null) {
                    params = params + "&id=" + mPersonId;
                }
                UpdatePersonalInfoTask updateTask = new UpdatePersonalInfoTask();
                updateTask.execute(params);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        private void buildDatePickerDialog(int year, int month, int day) {
            new DatePickerDialog(PersonalInfoActivity.this, new OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker picker, int year, int month, int day) {
                    String newValue = year + "-" + (month + 1) + "-" + day;
                    update(newValue);
                }
                
            }, year, month - 1, day).show();
        }
        private void buildOptionDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(PersonalInfoActivity.this);
            builder.setTitle(mInfoItem.getTitle());
            builder.setItems(mInfoItem.getOptions().split(";"), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String newValue = mInfoItem.getOptions().split(";")[which];
                    update(newValue);
                }
            });
            builder.create().show(); 
        }
        private void buildTextInputDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(PersonalInfoActivity.this);
            builder.setTitle(mInfoItem.getTitle());

            // Set up the input
            final EditText input = new EditText(PersonalInfoActivity.this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setText(mInfoItem.getValue());
            input.setHint(mInfoItem.getHint());
            builder.setView(input);

            // Set up the buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String newValue = input.getText().toString();
                    update(newValue);
                    //Toast.makeText(PersonalInfoActivity.this, newValue, Toast.LENGTH_LONG).show();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
        }
    };

    private class HelpButtonListener implements OnClickListener {
        @Override
        public void onClick(View view) {
            String s = (String) view.getTag();
            for (PersonalInfoItem item : mItemList) {
                if (item.getKey().equals(s)) {
                    buildTextDialog(item);
                    break;
                }
            }
        }

        private void buildTextDialog(PersonalInfoItem item) {
            AlertDialog.Builder builder = new AlertDialog.Builder(PersonalInfoActivity.this);
            builder.setTitle(item.getTitle());

            // Set up the input
            final TextView textView = new TextView(PersonalInfoActivity.this);
            textView.setText(item.getHint());
            textView.setTextSize(20);
            builder.setView(textView);
            builder.show();
        }
    };
}
