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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.leopub.mat.exception.AuthException;
import org.leopub.mat.exception.HintException;
import org.leopub.mat.exception.NetworkDataException;
import org.leopub.mat.exception.NetworkException;
import org.leopub.mat.model.ConfirmItem;
import org.leopub.mat.model.InboxItem;
import org.leopub.mat.model.SentItem;
import org.leopub.mat.model.User;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

public class UserDataManager {
    private SQLiteDatabase mDatabase = null;
    private User mUser;
    private Context mContext;
    private Configure mConfigure;
    private Map<String, String> mMajorStringMap;
    private Map<Character, String> mUnitTitleStringMap;
    private List<InboxItem> mInboxItemsCache;
    private List<InboxItem> mUnconfirmedInboxItemsCache;
    private List<SentItem> mSentItemsCache;

    public UserDataManager(Context context, Configure configure, User user) {
        mContext = context;
        mConfigure = configure;
        mUser = user;
        mInboxItemsCache = null;
        mUnconfirmedInboxItemsCache = null;
        mSentItemsCache = null;
        initStringMap();

        mDatabase = SQLiteDatabase.openOrCreateDatabase(mConfigure.getSQLitePath(), null);
        // create table contact
        String query = "CREATE TABLE IF NOT EXISTS contact (`id` integer PRIMARY KEY, name varchar(255), name_char varchar(10), type char, unit varchar(10), title varchar(10), `f` integer, `b` integer, `t` integer, timestamp timestamp);";
        mDatabase.execSQL(query);
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_contact_timestamp ON contact (timestamp);");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_contact_unit ON contact (unit);");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_contact_name_char ON contact (name_char);");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_contact_f ON contact (f);");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_contact_b ON contact (b);");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_contact_t ON contact (t);");
       // create table inbox 
        mDatabase.execSQL("CREATE TABLE IF NOT EXISTS `inbox` (`msg_id` integer PRIMARY KEY, `src_id` integer, `src_title` varchar(40), param integer, `content` varchar(2048), `status` integer, `timestamp` timestamp);");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_inbox_timestamp ON inbox (`timestamp`);");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_inbox_status ON inbox(`status`);");
         // create table sent
        mDatabase.execSQL("CREATE TABLE IF NOT EXISTS sent (`msg_id` integer PRIMARY KEY, `dst_str` varchar(300), `dst_title` varchar(300), parsm integer, `content` varchar(2048), `status` integer, `timestamp` timestamp);");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_sent_timestamp ON sent (`timestamp`);");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_sent_status ON sent (`status`);");
        // create table confirm
        mDatabase.execSQL("CREATE TABLE IF NOT EXISTS `confirm` (confirm_id integer PRIMARY KEY, `msg_id` integer, dst_id integer, dst_title varchar(40), `status` integer, timestamp timestamp);");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_confirm_timestamp ON confirm(`timestamp`);");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_confirm_msg ON confirm(`msg_id`);");
        // create table update_record
        mDatabase.execSQL("CREATE TABLE IF NOT EXISTS `update_record`(`id` integer PRIMARY KEY, timestamp timestamp, length integer)");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_update_record_timestamp ON update_record(`timestamp`);");
    }

    public void initStringMap() {
        mMajorStringMap = new HashMap<String, String>();
        mUnitTitleStringMap = new HashMap<Character, String>();
        mMajorStringMap.put("cs", mContext.getString(R.string.short_name_of_cs));
        mUnitTitleStringMap.put('a', mContext.getString(R.string.title_for_a));
        mUnitTitleStringMap.put('b', mContext.getString(R.string.title_for_b));
        mUnitTitleStringMap.put('c', mContext.getString(R.string.title_for_c));
        mUnitTitleStringMap.put('h', mContext.getString(R.string.title_for_h));
        mUnitTitleStringMap.put('l', mContext.getString(R.string.title_for_l));
        mUnitTitleStringMap.put('t', mContext.getString(R.string.title_for_t));
        mUnitTitleStringMap.put('w', mContext.getString(R.string.title_for_w));
        mUnitTitleStringMap.put('x', mContext.getString(R.string.title_for_x));
        mUnitTitleStringMap.put('y', mContext.getString(R.string.title_for_y));
        mUnitTitleStringMap.put('z', mContext.getString(R.string.title_for_z));
    }

    public void updateFromServer(String serverResponse) throws NetworkException, NetworkDataException, AuthException {
        if (serverResponse == null) {
            String since = getLastUpdateTimeDigit();
            HttpUtil.getJSON(mUser, Configure.JSON_URL, since, mConfigure.getMsgJSONPath());
            serverResponse = readFile(mConfigure.getMsgJSONPath());
        }
        //String str = HttpUtil.getJSON(mUser, Configure.JSON_URL, since);
        try {
            JSONObject jsonObj = new JSONObject(serverResponse);

            updateContact(jsonObj.getJSONArray("contact"));

            updateInbox(jsonObj.getJSONArray("inbox"));
            mInboxItemsCache = null;
            mUnconfirmedInboxItemsCache = null;

            updateConfirm(jsonObj.getJSONArray("confirm"));

            updateSent(jsonObj.getJSONArray("sent"));
            mSentItemsCache = null;

            addUpdateRecord(jsonObj.getString("timestamp"), serverResponse.length());
        } catch (JSONException e) {
            throw new NetworkDataException("Invalid JSON File", e);
        }
    }
    
    public void updateContact(JSONArray arr) throws NetworkDataException {
        try {
            int n = arr.length();
            for (int i = 0; i < n; i++) {
                JSONObject obj = arr.getJSONObject(i);
//contact (`id` integer PRIMARY KEY, name varchar(255), type char, unit varchar(10), title varchar(10), timestamp timestamp)
                String query = String.format(
                        "INSERT OR REPLACE INTO contact VALUES('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s');",
                        obj.getString("id"),
                        obj.getString("name"),
                        obj.getString("name_char"),
                        obj.getString("type"),
                        obj.getString("unit"),
                        obj.getString("title"),
                        obj.getString("f"),
                        obj.getString("b"),
                        obj.getString("f"),
                        obj.getString("timestamp"));
                Logger.d("SQL", query);
                mDatabase.execSQL(query);
            }
        } catch (JSONException e) {
            throw new NetworkDataException("JSON/Contact:" + e.getMessage());
        }
    }

   public void updateInbox(JSONArray arr) throws NetworkDataException {
        try {
            int n = arr.length();
            for (int i = 0; i < n; i++) {
                JSONObject obj = arr.getJSONObject(i);
//inbox (`msg_id` integer PRIMARY KEY, `src_id` integer, `src_title` varchar(40), param integer, `content` varchar(2048), `status` integer, `timestamp` timestamp)
                String query = String.format("INSERT OR REPLACE INTO `inbox` VALUES('%s', '%s', '%s', '%s', %s, '%s', '%s');",
                        obj.getString("msg_id"),
                        obj.getString("src_id"),
                        getContactTitle(obj.getString("src_id")),
                        obj.getString("param"),
                        DatabaseUtils.sqlEscapeString(obj.getString("content")),
                        obj.getString("status"),
                        obj.getString("timestamp"));
                Logger.d("SQL", query);
                mDatabase.execSQL(query);
            }
        } catch (JSONException e) {
            throw new NetworkDataException("JSON/Inbox:" + e.getMessage());
        }
    }

    public void updateSent(JSONArray arr) throws NetworkDataException {
        try {
            int n = arr.length();
            for (int i = 0; i < n; i++) {
                JSONObject obj = arr.getJSONObject(i);
//sent (`msg_id` integer PRIMARY KEY, `dst_str` varchar(300), `dst_title` varchar(300), `content` varchar(2048), `status` integer, `timestamp` timestamp)
                String query = String.format("INSERT OR REPLACE INTO sent VALUES('%s', '%s', '%s', '%s', %s, '%s', '%s');",
                        obj.getString("msg_id"),
                        obj.getString("dst_str"),
                        getGroupsTitle(obj.getString("dst_str")),
                        obj.getString("param"),
                        DatabaseUtils.sqlEscapeString(obj.getString("content")),
                        obj.getString("status"),
                        obj.getString("timestamp"));
                Logger.d("SQL", query);
                mDatabase.execSQL(query);
            }
        } catch (JSONException e) {
            throw new NetworkDataException("JSON/Sent:" + e.getMessage());
        }
    }

    public void updateConfirm(JSONArray arr) throws NetworkDataException {
        try {
            int n = arr.length();
            for (int i = 0; i < n; i++) {
                JSONObject obj = arr.getJSONObject(i);
//confirm (confirm_id integer PRIMARY KEY, `msg_id` integer, dst_id integer, dst_title varchar(40), `status` integer, timestamp timestamp
                String query = String.format("INSERT OR REPLACE INTO `confirm` VALUES('%s', '%s', '%s', '%s', '%s', '%s');",
                        obj.getString("confirm_id"),
                        obj.getString("msg_id"),
                        obj.getString("dst_id"),
                        getContactTitle(obj.getString("dst_id")),
                        obj.getString("status"),
                        obj.getString("timestamp"));
                Logger.d("SQL", query);
                mDatabase.execSQL(query);
            }
        } catch (JSONException e) {
            throw new NetworkDataException("JSON/Confirm:" + e.getMessage());
        }
    }

    @SuppressLint("DefaultLocale")
    public void addUpdateRecord(String timestamp, int length) throws NetworkDataException {
        String query = String.format("INSERT INTO update_record VALUES(NULL, '%s', '%d');", timestamp, length);
        Logger.d("SQL", query);
        mDatabase.execSQL(query);
    }

    private String readFile(String path) {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(new File(path)));
            while (reader.ready()) {
                sb.append(reader.readLine());
                sb.append("\n");
            }
            reader.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    public String getLastUpdateTime() {
        String ret = null;
        String columns[] = { "timestamp" };
        Cursor cursor = mDatabase.query("update_record", columns, null, null, null, null, "timestamp DESC", "0, 1");
        if (cursor.moveToNext()) {
            ret = cursor.getString(0);
        }
        return ret;
    }

    public String getBriefLastUpdateTime() {
        String lastUpdateTime = getLastUpdateTime();
        String ret = "?";
        if (lastUpdateTime != null) {
            ret = getLastUpdateTime().substring(5);
        }
        return ret;
    }

    public List<Contact> getContactsByInitChars(String initChars) {
        List<Contact> res = new ArrayList<Contact>();
//contact (`id` integer PRIMARY KEY, name varchar(255), name_char varchar(10), type char, unit varchar(10), title varchar(10), timestamp timestamp)
        String sql = "SELECT id, name, type, unit, title FROM contact WHERE name_char=? ORDER BY id;";
        String params[] = { initChars };
        Cursor cursor = mDatabase.rawQuery(sql, params);
        while (cursor.moveToNext()) {
            Contact contact = new Contact();
            contact.setId(cursor.getString(0));
            contact.setName(cursor.getString(1));
            contact.setType(cursor.getString(2));
            contact.setUnit(cursor.getString(3));
            contact.setTitle(cursor.getString(4));
            res.add(contact);
        }
        cursor.close();
        return res;
    }

    public List<Contact> getUnderling() {
        List<Contact> res = new ArrayList<Contact>();
//contact (`id` integer PRIMARY KEY, name varchar(255), name_char varchar(10), type char, unit varchar(10), title varchar(10), timestamp timestamp)
        Contact me = getContact(mUser.getUsername());
        String sql = "SELECT id, name, type, unit, title FROM contact WHERE `" +  me.getTitle() + "`='" + me.getId() + "'ORDER BY id;";
        Cursor cursor = mDatabase.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            Contact contact = new Contact();
            contact.setId(cursor.getString(0));
            contact.setName(cursor.getString(1));
            contact.setType(cursor.getString(2));
            contact.setUnit(cursor.getString(3));
            contact.setTitle(cursor.getString(4));
            res.add(contact);
        }
        cursor.close();
        return res;
    }

    public Contact getContact(String id) {
        String sql = "SELECT id, name, type, unit, title FROM contact WHERE id=? ORDER BY id;";
        String params[] = { id };
        Cursor cursor = mDatabase.rawQuery(sql, params);
        Contact contact = null;
        if (cursor.moveToNext()) {
            contact = new Contact();
            contact.setId(cursor.getString(0));
            contact.setName(cursor.getString(1));
            contact.setType(cursor.getString(2));
            contact.setUnit(cursor.getString(3));
            contact.setTitle(cursor.getString(4));
        }
        cursor.close();
        return contact;
    }

    public List<InboxItem> getInboxItems() {
        if (mInboxItemsCache != null) return mInboxItemsCache;

        List<InboxItem> res = new ArrayList<InboxItem>();
//inbox (`msg_id` integer PRIMARY KEY, `src_id` integer, `src_title` varchar(40), `content` varchar(2048), `status` integer, `timestamp` timestamp)
        String sql = "SELECT msg_id, src_id, src_title, content, status, timestamp FROM inbox " +
                     "ORDER BY status ASC, inbox.timestamp DESC;";
        Cursor cursor = mDatabase.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            InboxItem item = new InboxItem();
            item.setMsgId(cursor.getInt(0));
            item.setSrcId(cursor.getInt(1));
            item.setSrcTitle(cursor.getString(2));
            item.setContent(cursor.getString(3));
            item.setStatus(ItemStatus.fromOrdial(cursor.getInt(4)));
            item.setTimestamp(cursor.getString(5));
            res.add(item);
        }
        cursor.close();
        mInboxItemsCache = res;
        return res;
    }

    public InboxItem getInboxItemByMsgId(int msgId) {
        if (mInboxItemsCache != null) {
            for (InboxItem item : mInboxItemsCache) {
                if (item.getMsgId() == msgId) {
                    return item;
                }
            }
        }
//inbox (`msg_id` integer PRIMARY KEY, `src_id` integer, `src_title` varchar(40), `content` varchar(2048), `status` integer, `timestamp` timestamp)
        String sql = "SELECT msg_id, src_id, src_title, content, status, timestamp FROM inbox WHERE msg_id=? " +
                     "ORDER BY status ASC, timestamp DESC;";
        String[] params = { String.valueOf(msgId) };
        InboxItem item = new InboxItem();
        Cursor cursor = mDatabase.rawQuery(sql, params);
        if (cursor.moveToNext()) {
            item.setMsgId(cursor.getInt(0));
            item.setSrcId(cursor.getInt(1));
            item.setSrcTitle(cursor.getString(2));
            item.setContent(cursor.getString(3));
            item.setStatus(ItemStatus.fromOrdial(cursor.getInt(4)));
            item.setTimestamp(cursor.getString(5));
        }
        cursor.close();
        return item;
    }

    public List<InboxItem> getUnconfirmedInboxItems() {
        if (mUnconfirmedInboxItemsCache != null) return mUnconfirmedInboxItemsCache;

        List<InboxItem> res = new ArrayList<InboxItem>();
//inbox (`msg_id` integer PRIMARY KEY, `src_id` integer, `src_title` varchar(40), `content` varchar(2048), `status` integer, `timestamp` timestamp)
        String sql = "SELECT msg_id, src_id, src_title, content, status, timestamp FROM inbox WHERE status = 0 " +
                     "ORDER BY timestamp DESC;";;
        Cursor cursor = mDatabase.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            InboxItem item = new InboxItem();
            item.setMsgId(cursor.getInt(0));
            item.setSrcId(cursor.getInt(1));
            item.setSrcTitle(cursor.getString(2));
            item.setContent(cursor.getString(3));
            item.setStatus(ItemStatus.fromOrdial(cursor.getInt(4)));
            item.setTimestamp(cursor.getString(5));
            res.add(item);
        }
        cursor.close();
        mUnconfirmedInboxItemsCache = res;
        return res;
    }

    public List<SentItem> getSentItems() {
        if (mSentItemsCache != null) return mSentItemsCache;

        List<SentItem> res = new ArrayList<SentItem>();
        String sql = "SELECT msg_id, dst_title, content, status, timestamp FROM sent ORDER BY timestamp DESC;";
        Cursor cursor = mDatabase.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            SentItem item = new SentItem();
            item.setMsgId(cursor.getInt(0));
            item.setDstTitle(cursor.getString(1));
            item.setContent(cursor.getString(2));
            item.setStatus(ItemStatus.fromOrdial(cursor.getInt(3)));
            item.setTimestamp(cursor.getString(4));
            item.setProgress(getSentItemProgress(item.getMsgId()));
            res.add(item);
            if (item.getStatus() == ItemStatus.Init) {
                
            }
        }
        cursor.close();
        mSentItemsCache = res;
        return res;
    }

    public SentItem getSentItemByMsgId(int msgId) {
        if (mSentItemsCache != null) {
            for (SentItem item : mSentItemsCache) {
                if (item.getMsgId() == msgId) {
                    return item;
                }
            }
        }

        String sql = "SELECT msg_id, dst_title, content, status, timestamp FROM sent WHERE msg_id=? ORDER BY timestamp DESC;";
        String[] params = { String.valueOf(msgId) };
        Cursor cursor = mDatabase.rawQuery(sql, params);
        SentItem item = null;
        if (cursor.moveToNext()) {
            item = new SentItem();
            item.setMsgId(cursor.getInt(0));
            item.setDstTitle(cursor.getString(1));
            item.setContent(cursor.getString(2));
            item.setStatus(ItemStatus.fromOrdial(cursor.getInt(3)));
            item.setTimestamp(cursor.getString(4));
            item.setProgress(getSentItemProgress(item.getMsgId()));
        }
        cursor.close();
        return item;
    }

    public List<ConfirmItem> getConfirmItems(int msgId) {
        List<ConfirmItem> res = new ArrayList<ConfirmItem>();
        String sql = "SELECT confirm_id, dst_id, dst_title, status, timestamp FROM confirm "
                   + "WHERE msg_id=? ORDER BY timestamp ASC;";
        String[] params = { String.valueOf(msgId) };
        Cursor cursor = mDatabase.rawQuery(sql, params);
        while (cursor.moveToNext()) {
            ConfirmItem item = new ConfirmItem();
            item.setId(cursor.getInt(0));
            item.setMsgId(msgId);
            item.setDstId(cursor.getInt(1));
            item.setDstTitle(cursor.getString(2));
            item.setStatus(ItemStatus.fromOrdial(cursor.getInt(3)));
            item.setTimestamp(cursor.getString(4));
            res.add(item);
        }
        cursor.close();
        return res;
    }

    public String getSentItemProgress(int msgId) {
        String sql = "SELECT status, COUNT(*) FROM confirm WHERE msg_id=? GROUP BY status;";
        String[] params = { String.valueOf(msgId) };
        Cursor cursor = mDatabase.rawQuery(sql, params);
        int all = 0;
        int confirmed = 0;
        while (cursor.moveToNext()) {
            if (cursor.getInt(0) > 0) {
                confirmed += cursor.getInt(1);
            }
            all += cursor.getInt(1);
        }
        cursor.close();
        return confirmed + "/" + all;
    }

    public String getContactTitle(String id) {
        String params[] = { id };
        Cursor cursor = mDatabase.rawQuery("SELECT name, type FROM contact WHERE id=?;", params);
        String res = null;
        if (cursor.moveToNext()) {
            String name = cursor.getString(0);
            String type = cursor.getString(1);
            if (type.equals("T")) {
                res = name + mContext.getString(R.string.type_for_U);
            } else if (type.equals("S")) {
                res = name + mContext.getString(R.string.type_for_S);
            }
        }
        cursor.close();
        return res;
    }

    private String getGroupsTitle(String groups) {
        StringBuilder sb = new StringBuilder();
        String groupArray[] = groups.split(";");

        for (String group : groupArray) {
            if (group.isEmpty()) {
                continue;
            }
            if (!group.contains(".")) {
                sb.append(getContactTitle(group));
            } else {
                sb.append(getUnitTitle(group));
            }
            sb.append(";");
        }
        return sb.toString();
    }

    public String getUnitTitle(String expr) {
        String exprArray[] = expr.split("\\.");
        String title = exprArray[0];
        String unit  = exprArray[1];
        String major = unit.substring(0, 2);
        StringBuilder sb = new StringBuilder();
        if (!major.equals("__")) {
            sb.append(mMajorStringMap.get(major));
        }
        String grade = unit.substring(3, 4);
        if (!grade.equals("_")) {
            sb.append("1" + grade + mContext.getString(R.string.unit_grade));
        }
        String className = unit.substring(5, 6);
        if (!className.equals("_")) {
            sb.append(className + mContext.getString(R.string.unit_class));
        }
        if (title.length() == 0) {
            sb.append(mContext.getString(R.string.all_students));
        } else {
            sb.append(mUnitTitleStringMap.get(title.charAt(0)));
        }
        int nTitle = title.length();
        for (int i = 1; i < nTitle; i++) {
            sb.append("��");
            sb.append(mUnitTitleStringMap.get(title.charAt(i)));
        }
        return sb.toString();
    }

    public String onlyKeepDigit(String s) {
        StringBuilder sb = new StringBuilder();
        char charArr[] = s.toCharArray();

        for (char c : charArr) {
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public String getUnderlingString() throws NetworkException, AuthException {
        /* String res = HttpUtil.getUrl(Configure.INFO_UNDERLING_URL, user);*/
        HttpUtil.getUrl(Configure.INFO_UNDERLING_URL, mUser, new File(mConfigure.getUnderlingJSONPath()));
        String response = readFile(mConfigure.getUnderlingJSONPath());
        return response;
    }

    public String getCategoryJSON(String id) throws NetworkException, AuthException {
        String response = HttpUtil.getUrl(Configure.INFO_CATEGORY_URL + "?id=" + id, mUser);
        return response;
    }

    public void sendMessage(String dst, String content) throws AuthException, HintException, NetworkException, NetworkDataException {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("since=");
            sb.append(getLastUpdateTimeDigit());
            sb.append("&dst=");
            sb.append(URLEncoder.encode(dst.toString(), "utf-8"));
            sb.append("&content=");
            sb.append(URLEncoder.encode(content, "utf-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        String response = HttpUtil.postURL(mUser, Configure.NEW_MESSAGE_URL, sb.toString());
        if (response.charAt(0) != '[' && response.charAt(0) != '{') {
            throw new HintException(mContext.getString(R.string.send_msg_fail));
        }
        updateFromServer(response);
    }

    @SuppressLint("DefaultLocale")
    public void confirmMessage(int srcId, int msgId) throws AuthException, HintException, NetworkException, NetworkDataException {
        String url = String.format(Configure.CONFIRM_URL, srcId, msgId, getLastUpdateTimeDigit());
        String response = HttpUtil.getUrl(url, mUser);
        if (response.charAt(0) != '[' && response.charAt(0) != '{') {
            throw new HintException(mContext.getString(R.string.confirm_msg_fail));
        }
        updateFromServer(response);
    }

    public void changePassword(String oldPassword, String newPassword) throws AuthException, HintException, NetworkException {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("old_password=");
            sb.append(URLEncoder.encode(oldPassword, "utf-8"));
            sb.append("&new_password=");
            sb.append(URLEncoder.encode(newPassword, "utf-8"));
            sb.append("&repeat_password=");
            sb.append(URLEncoder.encode(newPassword, "utf-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        String response = HttpUtil.postURL(mUser, Configure.CHANGE_PASSWORD_URL, sb.toString());
        if (!response.startsWith("Password Updated!")) {
            throw new HintException(response);
        }
    }

    private String getLastUpdateTimeDigit() {
        String lastUpdateTime = getLastUpdateTime();
        if (lastUpdateTime == null) {
            lastUpdateTime = "1970-01-01 00:00:01";
        }
        return onlyKeepDigit(lastUpdateTime);
    }
}
