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

import org.leopub.mat.Configure;
import org.leopub.mat.MyApplication;
import org.leopub.mat.R;
import org.leopub.mat.UserDataManager;
import org.leopub.mat.UserManager;
import org.leopub.mat.model.User;
import org.leopub.mat.service.ConfirmMessageService;
import org.leopub.mat.service.UpdateMessageService;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;


public class MainActivity extends SmartActivity {
    private UserManager mUserManager;
    private UserDataManager mUserDataManager;

    private String mLastUpdateTime;
    private User mPausedUser;

    private Fragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUserManager = UserManager.getInstance();
        mLastUpdateTime = "?";
        initBroadcoastReceiver();

        mFragment = new InboxFragment();
        getActionBar().setTitle(R.string.action_inbox);

        Intent updateIntent = new Intent(this, UpdateMessageService.class);
        startService(updateIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Fragment fragment = null;
        Intent intent = null;
        switch(item.getItemId()) {
        case R.id.action_inbox:
            fragment = new InboxFragment();
            getActionBar().setTitle(R.string.action_inbox);
            break;
        case R.id.action_sent:
            fragment = new SentFragment();
            getActionBar().setTitle(R.string.action_sent);
            break;
        case R.id.action_compose:
            intent = new Intent(this, ComposeActivity.class);
            break;
        case R.id.action_settings:
            intent = new Intent(this, SettingsActivity.class);
            break;
        case R.id.action_change_password:
            intent = new Intent(this, ChangePasswordActivity.class);
            break;
        case R.id.action_personal_info:
            intent = new Intent(this, PersonalInfoActivity.class);
            break;
        }
        if (fragment != null) {
            /*
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                           .replace(android.R.id.content, fragment)
                           .commit();
            return true;
            */
            mFragment = fragment;
            updateView();
            return true;
        } else if (intent != null) {
            startActivity(intent);
        } else if (item.getItemId() == R.id.action_logout) {
            mUserManager.logoutCurrentUser();
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent); 
        } else if (item.getItemId() == R.id.action_sync) {
            intent = new Intent(this, UpdateMessageService.class);
            startService(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        mUserManager.setMainActivityRunning(false);
        mPausedUser = mUserManager.getCurrentUser();
        super.onPause();
    }

    @Override
    public void onResume() {
        mUserDataManager = mUserManager.getCurrentUserDataManager();
        super.onResume();

        checkUpdate();
        mUserManager.setMainActivityRunning(true);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    public void checkUpdate() {
        User currentUser = mUserManager.getCurrentUser();
        if (currentUser == null) return;

        if (mPausedUser == null || currentUser == null || currentUser != mPausedUser || !mUserDataManager.getBriefLastUpdateTime().equals(mLastUpdateTime)) {
            mLastUpdateTime = mUserDataManager.getBriefLastUpdateTime();

            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.cancel(0);

            //int nUnconfirmedInboxItem = mUserDataManager.getUnconfirmedInboxItems().size();
            updateView();
        }
    }

    protected void updateView() {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                       .replace(android.R.id.content, mFragment)
                       .commit();
    }

    private void initBroadcoastReceiver() {
        IntentFilter filter = new IntentFilter(Configure.BROADCAST_UPDATE_ACTION);
        filter.addAction(Configure.BROADCAST_CONFIRM_MSG_ACTION);
        filter.addAction(Configure.BROADCAST_SEND_MSG_ACTION);
        UpdateStateReceiver receiver = new UpdateStateReceiver();
        LocalBroadcastManager.getInstance(MyApplication.getAppContext()).registerReceiver(receiver , filter);
    }

    private class UpdateStateReceiver extends BroadcastReceiver {
        private UpdateStateReceiver() {}

        public void onReceive(Context context, Intent intent) {
            if (mUserManager.isMainActivityRunning()) {
                checkUpdate();
                String result = intent.getStringExtra(ConfirmMessageService.RESULT_STRING);
                if (result == null) {
                    result = getString(R.string.last_update_from) + mUserDataManager.getBriefLastUpdateTime();
                }
                Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
            }
        }
    }
}
