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

package org.leopub.mat;

import java.io.File;

public class Configure {
    public static final String BROADCAST_UPDATE_ACTION      = "org.leopub.mat.BROADCAST.UPDATE";
    public static final String BROADCAST_CONFIRM_MSG_ACTION = "org.leopub.mat.BROADCAST.CONFIRM_MSG";
    public static final String BROADCAST_SEND_MSG_ACTION    = "org.leopub.mat.BROADCAST.SEND_MSG";

    public static final String RE_CONTACTS      = "^((([abcfhltwxyz]*\\.(cs|__)[1_][234_][0_][1234_])|([1-9][0-9]*));)+$";
    public static final String RE_UNIT          = "^[abcfhltwxyz]*\\.(cs|__)[1_][234_][0_][1234_]$";

    public static final String LOGIN_URL           = "http://leopub.org/auth/login_check.php";
    public static final String CHANGE_PASSWORD_URL = "http://leopub.org/auth/change_password_done.php";
    public static final String JSON_URL            = "http://leopub.org/msg/client.php";
    public static final String CONFIRM_URL         = "http://leopub.org/msg/confirm_done.php?src=%d&msg=%d&since=%s";
    public static final String NEW_MESSAGE_URL     = "http://leopub.org/msg/new_msg_done.php";
    public static final String INFO_URL            = "http://leopub.org/info/";
    public static final String INFO_GET_URL        = "http://leopub.org/info/get.php";
    public static final String INFO_UNDERLING_URL  = "http://leopub.org/info/underling.php";
    public static final String INFO_CATEGORY_URL   = "http://leopub.org/info/category.php";
    public static final String INFO_UPDATE_URL     = "http://leopub.org/info/update.php";

    public static final String HOME_DIR_NAME         = "/MaT";
    public static final String LOGIN_SQLITE_FILENAME = "login.db";
    public static final String LOG_FILENAME          = "/log.txt";

    private static final String MSG_JSON_FILENAME        = "/msg_json.txt";
    private static final String UNDERLING_JSON_FILENAME  = "/underling_json.txt";
    private static final String USER_SQLITE_FILENAME  = "/mat.db";
    
    private String mUserHomePath;

    public Configure(String appHomePath, String username) {
        mUserHomePath = appHomePath + "/" + username;
        
        String[] filenames = {getUserHomePath()};
        for (String filename : filenames) {
            File file = new File(filename);
            if (!file.exists()) {
                file.mkdir();
            }
        }
    }

    public String getHomeDirName() {
        return HOME_DIR_NAME;
    }

    public String getUserHomePath() {
        return mUserHomePath;
    }

    public String getSQLitePath() {
        return mUserHomePath + USER_SQLITE_FILENAME;
    }

    public String getUnderlingJSONPath() {
        return mUserHomePath + UNDERLING_JSON_FILENAME;
    }

    public String getMsgJSONPath() {
        return mUserHomePath + MSG_JSON_FILENAME;
    }
}
