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

import org.leopub.mat.Contact;
import org.leopub.mat.R;
import org.leopub.mat.UserDataManager;
import org.leopub.mat.UserManager;
import org.leopub.mat.service.UpdateMessageService;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public class UserFragment extends Fragment {
    private UserManager mUserManager;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserManager = UserManager.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstaceState) {
        View v = inflater.inflate(R.layout.fragment_user, parent, false);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        UserDataManager userDataManager = mUserManager.getCurrentUserDataManager();
        Contact me = userDataManager.getContact(mUserManager.getCurrentUser().getUsername());
        String title = me.getTitle();
        boolean isLeader = me.getId() <= 99999 || title.contains("b") || title.contains("t");

        // handle compose
        Button button = (Button) getView().findViewById(R.id.compose_message);
        if (isLeader) {
            button.setVisibility(View.VISIBLE);
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), ComposeActivity.class);
                    getActivity().startActivity(intent);
                }
            });
        } else {
            button.setVisibility(View.GONE);
        }

        // handle change password 
        button = (Button) getView().findViewById(R.id.change_password);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), ChangePasswordActivity.class);
                getActivity().startActivity(intent);
            }
        });

        // handle update_now
        button = (Button) getView().findViewById(R.id.update_now);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), UpdateMessageService.class);
                getActivity().startService(intent);
            }
        });

        // handle settings
        button = (Button) getView().findViewById(R.id.settings);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                getActivity().startActivity(intent);
            }
        });

        // handle personal_info
        button = (Button) getView().findViewById(R.id.personal_info);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), PersonalInfoActivity.class);
                startActivity(intent);
            }
        });

        // handle underling_info
        button = (Button) getView().findViewById(R.id.underling_info);
        if (isLeader) {
            button.setVisibility(View.VISIBLE);
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), UnderlingActivity.class);
                    startActivity(intent);
                }
            });
        } else {
            button.setVisibility(View.GONE);
        }

        // handle logout
        button = (Button) getView().findViewById(R.id.logout);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mUserManager.logoutCurrentUser();
                Intent loginIntent = new Intent(getActivity(), LoginActivity.class);
                startActivity(loginIntent);
            }
        });
    }
}
