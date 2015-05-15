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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.leopub.mat.Logger;
import org.leopub.mat.R;
import org.leopub.mat.UserDataManager;
import org.leopub.mat.UserManager;
import org.leopub.mat.exception.AuthException;
import org.leopub.mat.exception.NetworkDataException;
import org.leopub.mat.exception.NetworkException;
import org.leopub.mat.model.UnderlingItem;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class UnderlingActivity extends ListActivity {
    private UnderlingArrayAdapter mArrayAdapter;
    private List<UnderlingItem> mItemList;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_underling);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mItemList = new ArrayList<UnderlingItem>();
        updateView();
        GetUnderlingTask getTask= new GetUnderlingTask();
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
        mArrayAdapter = new UnderlingArrayAdapter(this, R.layout.underling_item, R.id.underling_id, mItemList);
        getListView().setAdapter(mArrayAdapter);
    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(this, PersonalInfoActivity.class);
        intent.putExtra(PersonalInfoActivity.PERSON_ID, mItemList.get(position).getStudentId());
        startActivity(intent);
    }

    private class UnderlingArrayAdapter extends ArrayAdapter<UnderlingItem> {
        private UserDataManager mUserDataManager;
        public UnderlingArrayAdapter(Context context, int resource, int textViewId, List<UnderlingItem> items) {
            super(context, resource, textViewId, items);
            mUserDataManager = UserManager.getInstance().getCurrentUserDataManager();
        }

        @SuppressLint("InflateParams")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.underling_item, null);
            }
            UnderlingItem item = getItem(position);
            String itemId = item.getStudentId();
            TextView itemContentView = (TextView) convertView.findViewById(R.id.underling_id);
            itemContentView.setText(itemId);

            TextView itemInfoView = (TextView) convertView.findViewById(R.id.underling_name);
            itemInfoView.setText(mUserDataManager.getContactTitle(itemId));

            return convertView;
        }
    } 

    private class GetUnderlingTask extends AsyncTask<String, Void, String> {
        public GetUnderlingTask() {
            super();
        }

        @Override
        protected String doInBackground(String... args) {
            try {
                //String res = HttpUtil.getUrl(Configure.INFO_UNDERLING_URL, user);
                String res = UserManager.getInstance().getCurrentUserDataManager().getUnderlingString();
                mItemList = parseJSON(res);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        UnderlingActivity.this.updateView();
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
                    Toast.makeText(UnderlingActivity.this, message, Toast.LENGTH_LONG).show();
               }
            });
        }

        private List<UnderlingItem> parseJSON(String s) throws NetworkDataException {
            List<UnderlingItem> itemList = new ArrayList<UnderlingItem>();
            try {
                JSONArray array = new JSONArray(s);
                int n = array.length();
                for (int i = 0; i < n; i++) {
                    JSONObject obj = array.getJSONObject(i);
                    UnderlingItem item = new UnderlingItem(obj);
                    itemList.add(item);
                }
            } catch (JSONException e) {
                Logger.i("UNDERLING", "Length is :" + s.length());
                Logger.i("UNDERLING", "First character is:" + Character.valueOf(s.charAt(0)));
                Logger.i("UNDERLING", "Parse JSON/Underling:" + s);
                Logger.i("UNDERLING", e.getMessage());
                throw new NetworkDataException("Parse the JSON of underling information failed");
            }
            return itemList;
        }
    }
}
