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

import org.leopub.mat.Configure;
import org.leopub.mat.HttpUtil;
import org.leopub.mat.MyApplication;
import org.leopub.mat.R;
import org.leopub.mat.UserManager;
import org.leopub.mat.exception.AuthException;
import org.leopub.mat.exception.NetworkException;
import org.leopub.mat.model.User;
import org.leopub.mat.service.ConfirmMessageService;
import org.leopub.mat.service.UpdateMessageService;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {
    private UserManager mUserManager;
    private UpdateMessageReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_login);
        mUserManager = UserManager.getInstance();
        mBroadcastReceiver = new UpdateMessageReceiver();

        IntentFilter filter = new IntentFilter(Configure.BROADCAST_UPDATE_ACTION);
        LocalBroadcastManager.getInstance(MyApplication.getAppContext()).registerReceiver(mBroadcastReceiver, filter);

    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    public void onResume() {
        fillAccount();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(MyApplication.getAppContext()).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }
    /*
    @Override
    public void onResume() {
        super.onResume();
        mUserManager = UserManager.getInstance();

        User currentUser = mUserManager.getCurrentUser();
        if (currentUser != null && currentUser.isLogedIn()) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
        } else {
            fillAccount();
        }
    }  */

    public void fillAccount() {
        User currentUser = mUserManager.getCurrentUser();

        if (currentUser != null) {
            EditText usernameView = (EditText) findViewById(R.id.username);
            usernameView.setText(currentUser.getUsername());

            EditText passwordView = (EditText) findViewById(R.id.password);
            passwordView.setText("");
            //if (currentUser.isLogedIn()) {
                //passwordView.setText("********");
            //} else {
                //passwordView.setText("");
            //}
        }
    }

    public void onLogin(View view) {
        EditText usernameView = (EditText) findViewById(R.id.username);
        String username = usernameView.getText().toString();

        EditText passwordView = (EditText) findViewById(R.id.password);
        String password = passwordView.getText().toString();

        new NetworkTask().execute(username, password);
    }

    public void onLogout(View view) {
        EditText passwordView = (EditText) findViewById(R.id.password);
        passwordView.setText("");

        mUserManager.logoutCurrentUser();
    }

    public void onSwithUser(View view) {
        List<User> users = mUserManager.getUsers();
        int nUser = users.size();
        String usernames[] = new String[nUser];
        for (int i = 0; i < nUser; i++) {
            usernames[i] = users.get(i).getUsername();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.switch_user));
        builder.setItems(usernames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mUserManager.setCurrentUser(mUserManager.getUsers().get(which));
                fillAccount();
            }
        });
        builder.create().show();
    }

    private void showLoginProgress(boolean showProgress) {
        ((EditText)findViewById(R.id.username)).setFocusableInTouchMode(!showProgress);
        ((EditText)findViewById(R.id.password)).setFocusableInTouchMode(!showProgress);
        if (showProgress) {
            findViewById(R.id.login_progress).setVisibility(View.VISIBLE);
            findViewById(R.id.login_buttons).setVisibility(View.GONE);
            findViewById(R.id.login_dummy).requestFocus();
        } else {
            findViewById(R.id.login_progress).setVisibility(View.GONE);
            findViewById(R.id.login_buttons).setVisibility(View.VISIBLE);
        }
    }

    private class NetworkTask extends AsyncTask<String, Void, String> {
        @Override
        public void onPreExecute() {
            showLoginProgress(true);
        }

        @Override
        protected String doInBackground(String... args) {
            String username = args[0];
            String password = args[1];
            String result = null;
            try {
                User user = HttpUtil.auth(Configure.LOGIN_URL, username, password);
                mUserManager.setCurrentUser(user);
                Intent intent = new Intent(LoginActivity.this, UpdateMessageService.class);
                startService(intent);
            } catch (NetworkException e) {
                result = getString(R.string.error_network);
            } catch (AuthException e) {
                result = getString(R.string.login_fail);
            }
            return result;
        }

        @Override
        public void onPostExecute(String result) {
            if (result == null) {
                Toast.makeText(LoginActivity.this, getString(R.string.initializing), Toast.LENGTH_LONG).show();
            } else {
                showLoginProgress(false);
                Toast.makeText(LoginActivity.this, result, Toast.LENGTH_LONG).show();
            }
        }
    }

    private class UpdateMessageReceiver extends BroadcastReceiver {
        private UpdateMessageReceiver() {}

        public void onReceive(Context context, Intent intent) {
            showLoginProgress(false);
            String result = intent.getStringExtra(ConfirmMessageService.RESULT_STRING);
            if (result == null) {
                finish();
            } else {
                Toast.makeText(MyApplication.getAppContext(), result, Toast.LENGTH_LONG).show();
            }
        }
    }
}
