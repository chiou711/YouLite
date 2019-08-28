/*
 * Copyright (c) 2016 The Android Open Source Project
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
package com.cw.youlite.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.cw.youlite.db.DB_folder;
import com.cw.youlite.define.Define;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import androidx.annotation.NonNull;

/**
 * The VideoDbBuilder is used to grab a JSON file from a server and parse the data
 * to be placed into a local database
 */
public class DbBuilder_video {
    public static final String TAG_LINK_PAGE = "link_page";
    public static final String TAG_LINKS = "links";
    public static final String TAG_TITLE = "title";

    private static final String TAG = "DbBuilder_video";

    private Context mContext;

    /**
     * Default constructor that can be used for tests
     */

    public DbBuilder_video(Context mContext) {
        this.mContext = mContext;
    }

    /**
     * Fetches JSON data representing videos from a server and populates that in a database
     * @param url The location of the video list
     */
    public @NonNull
    List<List<List<ContentValues>>> fetch(String url)
            throws IOException, JSONException {
        JSONObject videoData = fetchJSON(url);
        System.out.println("DbBuilder_video / _fetch / videoData length = " + videoData.length()) ;
        return buildMedia(videoData);
    }

    /**
     * Takes the contents of a JSON object and populates the database
     * @param jsonObj The JSON object of videos
     * @throws JSONException if the JSON object is invalid
     */
    public List<List<List<ContentValues>>> buildMedia(JSONObject jsonObj) throws JSONException {

        System.out.println("DbBuilder_video / _buildMedia / jsonObj.toString = " + jsonObj.toString());

        JSONArray contentArray = jsonObj.getJSONArray("content");
        List<List<List<ContentValues>>> contentList = new ArrayList<>();
        List<List<ContentValues>> pagesToInsert = null;
        List<ContentValues> pageToInsert = null;
        System.out.println("DbBuilder_video / _buildMedia / contentArray.length() = " + contentArray.length());

        // categories (folder level)
        for (int h = 0; h < contentArray.length(); h++) {

            System.out.println("DbBuilder_video / _buildMedia / h = " + h);

            // create folder table: example folder1
            DbHelper mOpenHelper = new DbHelper(mContext);
            SQLiteDatabase sqlDb;
            sqlDb = mOpenHelper.getWritableDatabase();

            int id = h+1;
            String tableCreated = DB_folder.DB_FOLDER_TABLE_PREFIX.concat(String.valueOf(id));
            final String DB_CREATE_FOLDER_TABLE = "CREATE TABLE IF NOT EXISTS " + tableCreated + "(" +
                    DB_folder.KEY_PAGE_ID + " INTEGER PRIMARY KEY," +
                    DB_folder.KEY_PAGE_TITLE + " TEXT," +
                    DB_folder.KEY_PAGE_TABLE_ID + " INTEGER," +
                    DB_folder.KEY_PAGE_STYLE + " INTEGER," +
                    DB_folder.KEY_PAGE_CREATED + " INTEGER);";
            sqlDb.execSQL(DB_CREATE_FOLDER_TABLE);


            JSONObject contentObj = contentArray.getJSONObject(h);

            JSONArray pageArray = contentObj.getJSONArray(TAG_LINK_PAGE);

            System.out.println("DbBuilder_video / _buildMedia / pageArray.length() = " + pageArray.length());

            pagesToInsert = new ArrayList<>();

            // pages ( page level)
            for (int i = 0; i < pageArray.length(); i++) {

                // page title
                JSONObject page = pageArray.getJSONObject(i);
                String pageTitle = page.getString(TAG_TITLE);

                System.out.println("DbBuilder_video / _buildMedia / pageTitle = " + pageTitle);

                DB_folder db_folder = new DB_folder(mContext, id);

                //db_folder.insertPageTable(db_folder, i, j, true);

                // insert page data to folder table
                db_folder.insertPage(
                        tableCreated,//DB_folder.DB_FOLDER_TABLE_PREFIX.concat(String.valueOf(id)),
                        pageTitle,
                        i+1,
                        Define.STYLE_DEFAULT,true);//Define.STYLE_PREFER

                // page links
                JSONArray linksArray;
                linksArray = page.getJSONArray(TAG_LINKS);
                System.out.println("DbBuilder_video / _buildMedia / linksArray.length() = " + linksArray.length());

                pageToInsert = new ArrayList<>();
                // links ( note level)
                for (int j = 0; j < linksArray.length(); j++) {
                    JSONObject link = linksArray.getJSONObject(j);

                    String linkTitle = link.optString("note_title");
                    System.out.println("DbBuilder_video / _buildMedia / linkTitle = " + linkTitle);

                    String linkUrl = (String) link.opt("note_link_uri"); // Get the first link only.

                    ContentValues videoValues = new ContentValues();
                    videoValues.put(Contract.VideoEntry.COLUMN_NOTE_TITLE, linkTitle);
                    videoValues.put(Contract.VideoEntry.COLUMN_NOTE_PICTURE_URI, "");
                    videoValues.put(Contract.VideoEntry.COLUMN_NOTE_AUDIO_URI, "");
                    videoValues.put(Contract.VideoEntry.COLUMN_NOTE_DRAWING_URI, "");
                    videoValues.put(Contract.VideoEntry.COLUMN_NOTE_LINK_URI, linkUrl);
                    videoValues.put(Contract.VideoEntry.COLUMN_NOTE_BODY, "");
                    videoValues.put(Contract.VideoEntry.COLUMN_NOTE_MARKING, 1);
                    videoValues.put(Contract.VideoEntry.COLUMN_NOTE_CREATED, 1); //todo temp

                    pageToInsert.add(videoValues);
                }

                //Will call DbHelper.onCreate()first time when WritableDatabase is not created yet
                sqlDb = mOpenHelper.getWritableDatabase();
                String tableId = String.valueOf(h+1).concat("_").concat(String.valueOf(i+1)); //Id starts from 1
                // Create a table to hold videos.
                final String SQL_CREATE_PAGE_TABLE = "CREATE TABLE IF NOT EXISTS " + Contract.VideoEntry.PAGE_TABLE_NAME.concat(tableId) + " (" +
                        Contract.VideoEntry._ID + " INTEGER PRIMARY KEY," +
                        Contract.VideoEntry.COLUMN_NOTE_TITLE + " TEXT," +
                        Contract.VideoEntry.COLUMN_NOTE_PICTURE_URI + " TEXT," +
                        Contract.VideoEntry.COLUMN_NOTE_AUDIO_URI + " TEXT," +
                        Contract.VideoEntry.COLUMN_NOTE_DRAWING_URI + " TEXT," +
                        Contract.VideoEntry.COLUMN_NOTE_LINK_URI + " TEXT NOT NULL," + // TEXT UNIQUE NOT NULL will make the URL unique.
                        Contract.VideoEntry.COLUMN_NOTE_BODY + " TEXT," +
                        Contract.VideoEntry.COLUMN_NOTE_MARKING + " INTEGER," +
                        Contract.VideoEntry.COLUMN_NOTE_CREATED + " INTEGER);";

                // Do the creating of the databases.
                sqlDb.execSQL(SQL_CREATE_PAGE_TABLE);

                pagesToInsert.add(pageToInsert);

            }

            contentList.add(pagesToInsert);
        }
        return contentList;
    }

    /**
     * Fetch JSON object from a given URL.
     *
     * @return the JSONObject representation of the response
     * @throws JSONException
     * @throws IOException
     */
    private JSONObject fetchJSON(String urlString) throws JSONException, IOException {
        System.out.println("DbBuilder_video / fetchJSON / urlString = " + urlString);

        BufferedReader reader = null;
        java.net.URL url = new java.net.URL(urlString);
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        try {
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(),
                    "utf-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String json = sb.toString();
            return new JSONObject(json);
        } finally {
            urlConnection.disconnect();
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "JSON feed closed", e);
                }
            }
        }
    }
}