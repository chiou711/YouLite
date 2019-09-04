/*
 * Copyright (C) 2018 CW Chiu
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

package com.cw.youlite.operation.import_export;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.cw.youlite.data.Contract;
import com.cw.youlite.data.DbHelper;
import com.cw.youlite.data.Provider;
import com.cw.youlite.db.DB_folder;
import com.cw.youlite.db.DB_page;
import com.cw.youlite.define.Define;
import com.cw.youlite.folder.FolderUi;
import com.cw.youlite.main.MainAct;
import com.cw.youlite.tabs.TabsHost;
import com.cw.youlite.util.preferences.Pref;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static com.cw.youlite.data.DbBuilder_video.TAG_LINKS;
import static com.cw.youlite.data.DbBuilder_video.TAG_LINK_PAGE;
import static com.cw.youlite.data.DbBuilder_video.TAG_TITLE;

public class ParseJsonToDB {

    DB_folder mDb_folder;
    DB_page mDb_page;

    private Context mContext;

    private FileInputStream fileInputStream = null;
    public static boolean isParsing;
    String fileBody = "";
    private boolean mEnableInsertDB = true;
    int folderTableId;

    ParseJsonToDB(FileInputStream fileInputStream, Context context)
    {
        mContext = context;
        this.fileInputStream = fileInputStream;

        folderTableId = Pref.getPref_focusView_folder_tableId(mContext);
        mDb_folder = new DB_folder(MainAct.mAct, folderTableId);

        mDb_page = new DB_page(MainAct.mAct,TabsHost.getCurrentPageTableId());

        isParsing = true;
    }

    public void parseJsonAndInsertDB(FileInputStream stream) throws JSONException,IOException
    {

        StringBuilder total = null;
        try
        {
            BufferedReader r = new BufferedReader(new InputStreamReader(stream));
            total = new StringBuilder();

            for (String line; (line = r.readLine()) != null; )
            {
                total.append(line).append('\n');
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

        final String jsonString = total.toString();
        System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / jsonString = " + jsonString);

        if(mEnableInsertDB) {
            // add folder
            JSONObject jsonObj = new JSONObject(jsonString);
            JSONArray contentArray = jsonObj.getJSONArray("content");
            List<ContentValues> catesToInsert = new ArrayList<>();
            System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / contentArray.length() = " + contentArray.length());

            FolderUi.renewFirstAndLast_folderId();
            int last = FolderUi.mLastExist_folderTableId;
            System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / last = " + last);

            for (int h = 0; h < contentArray.length(); h++) {

                System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / h = " + h);
                JSONObject contentObj = contentArray.getJSONObject(h);

                // category name
                String category_name = contentObj.getString("category");

                // save category names
                ContentValues categoryValues = new ContentValues();
                categoryValues.put("folder_title", category_name);
                categoryValues.put("folder_table_id", last+h+1);

                catesToInsert.add(categoryValues);
            }
            // add folder data

            List<ContentValues> contentValuesList1 = catesToInsert;

            ContentValues[] downloadedVideoContentValues1 =
                    contentValuesList1.toArray(new ContentValues[contentValuesList1.size()]);

            ContentResolver contentResolver1 = mContext.getApplicationContext().getContentResolver();
            System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / contentResolver = " + contentResolver1.toString());

            contentResolver1.bulkInsert(Contract.CategoryEntry.CONTENT_URI, downloadedVideoContentValues1);//The key is the column name for the field.

            jsonObj = new JSONObject(jsonString);
            List<List<List<ContentValues>>> contentList = new ArrayList<>();

            contentArray = jsonObj.getJSONArray("content");
            List<List<ContentValues>> pagesToInsert = null;
            List<ContentValues> pageToInsert = null;
            System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / contentArray.length() = " + contentArray.length());

            // categories (folder level)
            for (int h = 0; h < contentArray.length(); h++) {

                System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / h = " + h);

                // create folder table: example folder1
                DbHelper mOpenHelper = new DbHelper(mContext);
                SQLiteDatabase sqlDb;
                sqlDb = mOpenHelper.getWritableDatabase();

                int folder_id = last+h+1;
                String tableCreated = DB_folder.DB_FOLDER_TABLE_PREFIX.concat(String.valueOf(folder_id));
                final String DB_CREATE_FOLDER_TABLE = "CREATE TABLE IF NOT EXISTS " + tableCreated + "(" +
                        DB_folder.KEY_PAGE_ID + " INTEGER PRIMARY KEY," +
                        DB_folder.KEY_PAGE_TITLE + " TEXT," +
                        DB_folder.KEY_PAGE_TABLE_ID + " INTEGER," +
                        DB_folder.KEY_PAGE_STYLE + " INTEGER," +
                        DB_folder.KEY_PAGE_CREATED + " INTEGER);";
                sqlDb.execSQL(DB_CREATE_FOLDER_TABLE);


                JSONObject contentObj = contentArray.getJSONObject(h);

                JSONArray pageArray = contentObj.getJSONArray(TAG_LINK_PAGE);

                System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / pageArray.length() = " + pageArray.length());

                pagesToInsert = new ArrayList<>();

                // pages ( page level)
                for (int i = 0; i < pageArray.length(); i++) {

                    // page title
                    JSONObject page = pageArray.getJSONObject(i);
                    String pageTitle = page.getString(TAG_TITLE);

                    System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / pageTitle = " + pageTitle);

                    DB_folder db_folder = new DB_folder(mContext, folder_id);

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
                    System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / linksArray.length() = " + linksArray.length());

                    pageToInsert = new ArrayList<>();
                    // links ( note level)
                    for (int j = 0; j < linksArray.length(); j++) {
                        JSONObject link = linksArray.getJSONObject(j);

                        String linkTitle = link.optString("note_title");
                        System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / linkTitle = " + linkTitle);

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
                    String tableId = String.valueOf(folder_id).concat("_").concat(String.valueOf(i+1)); //Id starts from 1
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

            // add page data
            List<List<List<ContentValues>>> contentValuesList = contentList;//builder.fetch(serviceUrl);
            System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / contentValuesList.size() = " + contentValuesList.size());

            for(int i=0;i<contentValuesList.size();i++) {

                System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / i = " + i);

                for(int j=0;j<contentValuesList.get(i).size();j++) {
                    ContentValues[] downloadedVideoContentValues =
                            contentValuesList.get(i).get(j).toArray(new ContentValues[contentValuesList.get(i).get(j).size()]);

                    System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / j = " + j);

                    ContentResolver contentResolver = mContext.getApplicationContext().getContentResolver();

                    Provider.tableId = String.valueOf(last + i + 1).concat("_").concat(String.valueOf(j + 1));//todo temp
                    contentResolver.bulkInsert(Contract.VideoEntry.CONTENT_URI, downloadedVideoContentValues);
                }
            }
            isParsing = false;
        }
        else{
            fileBody = jsonString;
            isParsing = false;
        }
    }

    void handleJson()
    {
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    parseJsonAndInsertDB(fileInputStream);
                }
                catch (Exception e)
                { }
            }
        });
        thread.start();
    }

    void enableInsertDB(boolean en)
    {
        mEnableInsertDB = en;
    }
}