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
import org.leopub.mat.Logger;
import org.leopub.mat.MyApplication;
import org.leopub.mat.R;
import org.leopub.mat.UserDataManager;
import org.leopub.mat.UserManager;
import org.leopub.mat.model.User;
import org.leopub.mat.service.ConfirmMessageService;
import org.leopub.mat.service.UpdateMessageService;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.widget.Toast;


public class MainActivity extends FragmentActivity implements ActionBar.TabListener {
    @SuppressWarnings("unchecked")
    static final private Class<? extends Fragment>[] FRAGMENTS = new Class[] {InboxFragment.class, SentFragment.class, UserFragment.class};
    static final private int[] FRAGMENT_ICONS = {R.drawable.ic_action_inbox, R.drawable.ic_action_sent, R.drawable.ic_action_user};

    private UserManager mUserManager;
    private UserDataManager mUserDataManager;
    private ViewPager mViewPager;
    private Tab[] mTabs;
    private Fragment[] mFragments;
    private String mLastUpdateTime;
    private User mLastUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pager);

        mUserManager = UserManager.getInstance();
        mLastUpdateTime = "?";
        initViewPager();
        initNaviTabs();
        initBroadcoastReceiver();

        Intent updateIntent = new Intent(this, UpdateMessageService.class);
        startService(updateIntent);
    }

    @Override
    public void onPause() {
        mUserManager.setMainActivityRunning(false);
        mLastUser = mUserManager.getCurrentUser();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        User currentUser = mUserManager.getCurrentUser();
        if (currentUser == null || !currentUser.isLogedIn() || mUserManager.getCurrentUserDataManager().getLastUpdateTime() == null) {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
        } else {
            mUserDataManager = mUserManager.getCurrentUserDataManager();

            checkUpdate();
            mUserManager.setMainActivityRunning(true);
            Logger.i("MAIN", this.toString());
        }
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
        //if (isTaskRoot()) {
            //moveTaskToBack(true);
        //} else {
            //super.onBackPressed();
        //}
        moveTaskToBack(true);
    }

    public void checkUpdate() {
        if (mUserManager.getCurrentUser() != mLastUser || !mUserDataManager.getBriefLastUpdateTime().equals(mLastUpdateTime)) {
            mFragments[0] = null;
            mFragments[1] = null;
            mViewPager.getAdapter().notifyDataSetChanged();
            mLastUpdateTime = mUserDataManager.getBriefLastUpdateTime();

            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.cancel(0);

            int nUnconfirmedInboxItem = mUserDataManager.getUnconfirmedInboxItems().size();
            if (nUnconfirmedInboxItem > 0) {
                mTabs[0].setText(String.valueOf(nUnconfirmedInboxItem));
            } else {
                mTabs[0].setText("");
            }
        }
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {}

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        for (int i = 0; i < mTabs.length; i++) {
            if (tab == mTabs[i]) {
                mViewPager.setCurrentItem(i);
                return;
            }
        }
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {}

    private void initViewPager() {
        FragmentManager fm = getSupportFragmentManager();
        mFragments = new Fragment[FRAGMENTS.length];
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(new FragmentPagerAdapter(fm) {
            @Override
            public int getCount() {
                return FRAGMENTS.length;
            }

            @Override
            public Fragment getItem(int pos) {
                try {
                    if (mFragments[pos] == null) {
                        mFragments[pos] = FRAGMENTS[pos].newInstance();
                    }
                    return mFragments[pos];
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public int getItemPosition(Object obj) {
                for (int i = 0; i < mFragments.length; i++) {
                    if (obj == mFragments[i]) {
                        return i;
                    }
                }
                return POSITION_NONE;
            }
        });
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int pos) {
                getActionBar().setSelectedNavigationItem(pos);
            }
        });
    }

    private void initNaviTabs() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);

        mTabs = new Tab[FRAGMENT_ICONS.length];
        for (int i = 0; i < mTabs.length; i++) {
            mTabs[i] = actionBar.newTab()
                        .setIcon(FRAGMENT_ICONS[i])
                        .setTabListener(this);
            actionBar.addTab(mTabs[i]);
        }
        /*
        int nUnconfirmedInboxItems = mUserDataManager.getUnconfirmedInboxItems().size();
        if (nUnconfirmedInboxItems > 0) {
            mTabs[0].setText(String.valueOf(nUnconfirmedInboxItems));
        } */
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
