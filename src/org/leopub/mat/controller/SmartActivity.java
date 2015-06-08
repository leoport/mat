package org.leopub.mat.controller;

import org.leopub.mat.UserManager;
import org.leopub.mat.model.User;

import android.app.Activity;
import android.content.Intent;

abstract public class SmartActivity extends Activity {
    @Override
    protected void onResume() {
        super.onResume();
        User currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser == null || !currentUser.isLogedIn()){
            if (isTaskRoot()) {
                Intent loginIntent = new Intent(this, LoginActivity.class);
                startActivity(loginIntent);
            } else {
                finish();
            }
        } else {
            updateView();
        }
    }

    abstract protected void updateView();
}