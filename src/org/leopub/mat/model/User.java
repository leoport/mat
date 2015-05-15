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

package org.leopub.mat.model;

public class User {
    private String mUsername;
    private String mSessionId;
    private String mCookieId;

    public User(String username) {
        mUsername = username;
        mSessionId = null;
        mCookieId = null;
    }

    public String getUsername() {
        return mUsername;
    }

    public String getSessionId() {
        return mSessionId;
    }

    public void setSessionId(String sessionId) {
        mSessionId = sessionId;
    }

    public String getCookieId() {
        return mCookieId;
    }

    public void setCookieId(String cookieId) {
        mCookieId = cookieId;
    }

    public boolean isLogedIn() {
        return mCookieId != null;
    }
}
