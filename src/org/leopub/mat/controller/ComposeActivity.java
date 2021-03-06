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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.leopub.mat.Configure;
import org.leopub.mat.Contact;
import org.leopub.mat.MyApplication;
import org.leopub.mat.R;
import org.leopub.mat.UserDataManager;
import org.leopub.mat.UserManager;
import org.leopub.mat.service.SendMessageService;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class ComposeActivity extends Activity {
    private final static String KEY_RECEIVERS = "receivers";

    private UserDataManager mUserDataManager;
    private List<Contact> mContactsToChoose;
    private String mReceivers;
    private SendMsgReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_compose);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mUserDataManager = UserManager.getInstance().getCurrentUserDataManager();
        mReceivers = "";
        if (savedInstanceState != null) {
            mReceivers = savedInstanceState.getString(KEY_RECEIVERS);
        }

        EditText toEditText = (EditText) findViewById(R.id.compose_to);
        toEditText.addTextChangedListener(new TextWatcher(){
            @Override
            public void afterTextChanged(Editable s) {
                String str = s.toString();
                if (str.endsWith(" ")) {
                    String[] tokens = str.split(";");
                    String lastToken = tokens[tokens.length - 1].trim();
                    //Toast.makeText(ComposeActivity.this, lastToken, Toast.LENGTH_LONG).show();
                    onChooseContact(lastToken);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
        showSendProgress(false);
        //initBroadcoastReceiver();
        mReceiver = new SendMsgReceiver();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        /*
        IntentFilter filter = new IntentFilter();
        filter.addAction(Configure.BROADCAST_SEND_MSG_ACTION);
        registerReceiver(mReceiver, filter); */
        IntentFilter filter = new IntentFilter(Configure.BROADCAST_SEND_MSG_ACTION);
        LocalBroadcastManager.getInstance(MyApplication.getAppContext()).registerReceiver(mReceiver , filter);
        super.onResume();
    }

    @Override
    public void onPause() {
        //unregisterReceiver(mReceiver);
        LocalBroadcastManager.getInstance(MyApplication.getAppContext()).unregisterReceiver(mReceiver);
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(KEY_RECEIVERS, mReceivers);
        super.onSaveInstanceState(savedInstanceState);
    }

    public void onSubmit(View view) {
        StringBuilder to = new StringBuilder();
        String[] receiverArr = mReceivers.split(";");
        for (String receiver : receiverArr) {
            to.append(receiver.split(",")[0]);
            to.append(";");
        }

        EditText contentView = (EditText) findViewById(R.id.compose_content);
        String contentStr = contentView.getText().toString();

        Intent sendMsgIntent = new Intent(MyApplication.getAppContext(), SendMessageService.class);
        sendMsgIntent.putExtra(SendMessageService.DESTINATION, to.toString());
        sendMsgIntent.putExtra(SendMessageService.CONTENT, contentStr);
        startService(sendMsgIntent);
        showSendProgress(true);
        /*
        PostMessageTask postTask = new PostMessageTask(to.toString(), contentStr);
        postTask.execute();
        showSendProgress(true);
        */
    }

    private void showSendProgress(boolean showProgress) {
        ((EditText)findViewById(R.id.compose_to)).setFocusableInTouchMode(!showProgress);
        ((EditText)findViewById(R.id.compose_content)).setFocusableInTouchMode(!showProgress);
        if (showProgress) {
            findViewById(R.id.compose_progress).setVisibility(View.VISIBLE);
            findViewById(R.id.compose_send).setVisibility(View.GONE);
            findViewById(R.id.compose_dummy).requestFocus();
        } else {
            findViewById(R.id.compose_progress).setVisibility(View.GONE);
            findViewById(R.id.compose_send).setVisibility(View.VISIBLE);
        }
    }

    public void onChooseContact(String token) {
        Pattern pattern = Pattern.compile(Configure.RE_UNIT);
        Matcher matcher = pattern.matcher(token);
        if (matcher.matches()) {
            addUnitReceiver(token);
            return;
        }

        mContactsToChoose = mUserDataManager.getContactsByInitChars(token);
        int nContact = mContactsToChoose.size();
        if (nContact == 1) {
            addSingleReceiver(mContactsToChoose.get(0));
            return;
        }

        String contactNames[] = new String[nContact];
        for (int i = 0; i < nContact; i++) {
            Contact contact = mContactsToChoose.get(i);
            contactNames[i] = contact.getId() + " " + contact.getName();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.choose_contact));
        builder.setItems(contactNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Contact contact = mContactsToChoose.get(which);
                addSingleReceiver(contact);
            }
        });
        builder.create().show();
    }

    private void addSingleReceiver(Contact contact) {
        String str = contact.getId() + "," + contact.getName() + ";";
        mReceivers += str;
        EditText toView = (EditText) findViewById(R.id.compose_to);
        toView.setText(mReceivers);
        toView.setSelection(mReceivers.length());
    }

    private void addUnitReceiver(String unitExpr) {
        String str = unitExpr + "," + mUserDataManager.getUnitTitle(unitExpr) + ";";
        mReceivers += str;
        EditText toView = (EditText) findViewById(R.id.compose_to);
        toView.setText(mReceivers);
        toView.setSelection(mReceivers.length());
    }

    /*
    private class PostMessageTask extends AsyncTask<String, Void, String> {
        private String mDst;
        private String mContent;
        public PostMessageTask(String dst, String content) {
            super();
            mDst = dst;
            mContent = content;
        }

        @Override
        protected String doInBackground(String... args) {
            try {
                mUserDataManager.postNewMessage(mDst, mContent);
                Button button = (Button)findViewById(R.id.compose_send);
                button.setText(getString(R.string.send_message_OK));
                button.setClickable(false);
                showToastHint(getString(R.string.send_message_OK));
                
                //runOnUiThread(new Runnable() {
                //    @Override
                //    public void run() {
                //        onBackPressed();
                //    }
                //});
            } catch (NetworkException e) {
                showToastHint(getString(R.string.error_network));
            } catch (NetworkDataException e) {
                showToastHint(getString(R.string.error_network));
            } catch (AuthException e) {
                showToastHint(getString(R.string.error_auth_fail));
            } catch (HintException e) {
                showToastHint(e.getMessage());
            } finally {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showSendProgress(false);
                    }
                });
            }
            return "";
        }

        private void showToastHint(final String message) {
            runOnUiThread(new Runnable() {
               @Override
               public void run() {
                    Toast.makeText(ComposeActivity.this, message, Toast.LENGTH_LONG).show();
               }
            });
        }
    } */
    /*
    private void initBroadcoastReceiver() {
        IntentFilter filter = new IntentFilter(Configure.BROADCAST_SEND_MSG_ACTION);
        SendMsgReceiver receiver = new SendMsgReceiver();
        LocalBroadcastManager.getInstance(MyApplication.getAppContext()).registerReceiver(receiver , filter);
    } */

    private class SendMsgReceiver extends BroadcastReceiver {
        private SendMsgReceiver() {}

        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(SendMessageService.RESULT_STRING);
            Toast.makeText(MyApplication.getAppContext(), message, Toast.LENGTH_LONG).show();
            if (getString(R.string.send_message_OK).equals(message)) {
                onBackPressed();
            } else {
                showSendProgress(false);
            }
        }
    }
}
