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

import java.util.List;

import org.leopub.mat.ItemStatus;
import org.leopub.mat.R;
import org.leopub.mat.UserDataManager;
import org.leopub.mat.UserManager;
import org.leopub.mat.model.ConfirmItem;
import org.leopub.mat.model.SentItem;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class SentItemActivity extends Activity {
    public final static String SENT_ITEM_MSG_ID = "org.leopub.mat.sent.choosenItemMsgId";

    private UserDataManager mUserDataManager;
    private SentItem mItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sent_item);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        int msgId= intent.getIntExtra(SENT_ITEM_MSG_ID, -1);
        UserManager userManager = UserManager.getInstance();
        mUserDataManager = userManager.getCurrentUserDataManager();
        mItem = mUserDataManager.getSentItemByMsgId(msgId);

        String lineSeperator = System.getProperty("line.separator");

        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.sent_item_to) + ":" + mItem.getDstTitle());
        sb.append(lineSeperator);
        sb.append(getString(R.string.sent_item_time) + ":" + mItem.getTimestamp());
        sb.append(lineSeperator);
        sb.append(getString(R.string.sent_item_content) + ":" + mItem.getContent());
        TextView textView = (TextView) findViewById(R.id.sent_item_content);
        textView.setText(sb.toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sent_item, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_confirm_detail){
            onDetailPressed();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onBackPressed(View view) {
        onBackPressed();
    }

    public void onDetailPressed() {
        List<ConfirmItem> confirmItems = mUserDataManager.getConfirmItems(mItem.getMsgId());
        int nConfirmItems = confirmItems.size();
        String displayItems[] = new String[nConfirmItems];
        for (int i = 0; i < nConfirmItems; i++) {
            ConfirmItem confirmItem = confirmItems.get(i);
            if (confirmItem.getStatus() == ItemStatus.Init) {
                displayItems[i] = confirmItem.getDstTitle() + "\t" + getString(R.string.not_confirmed);
            } else {
                displayItems[i] = confirmItem.getDstTitle() + "\t" + getString(R.string.confirmed);
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.confirm_progress) + ":" + mItem.getProgress());
        builder.setItems(displayItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.create().show();
    }
}
