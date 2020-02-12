/*
 * Copyright (C) 2019 CW Chiu
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
import com.cw.youlite.define.Define;
import com.cw.youlite.folder.FolderUi;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static com.cw.youlite.data.DbBuilder_video.TAG_LINKS;
import static com.cw.youlite.data.DbBuilder_video.TAG_LINK_PAGE;
import static com.cw.youlite.data.DbBuilder_video.TAG_TITLE;

public class ParseJsonToDB {

    private Context mContext;
    public static boolean isParsing;
    public String fileBody = "";
    String filePath;

    ParseJsonToDB(String filePath, Context context)
    {
        mContext = context;
        this.filePath = filePath;
        isParsing = true;
    }

    public ParseJsonToDB(Context context)
    {
        mContext = context;
        isParsing = true;
    }

    //
    // parse JSON file and insert content to DB tables
    //
    private void parseJsonFileAndInsertDB(String filePath) throws JSONException
    {
        final String jsonString = getJsonStringByFile(filePath);
        JSONObject jsonObj = new JSONObject(jsonString);
        parseJsonAndInsertDB(jsonObj);
    }

    //
    // parse JSON object and insert content to DB tables
    //
    public void parseJsonAndInsertDB(JSONObject jsonObj) throws JSONException
    {
        ContentResolver contentResolver = mContext.getApplicationContext().getContentResolver();

        // get last folder Id
        FolderUi.renewFirstAndLast_folderId();
        int lastFolderTableId = FolderUi.mLastExist_folderTableId;
//        System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / lastFolderTableId = " + lastFolderTableId);

        // start parsing
        JSONArray contentArray = jsonObj.getJSONArray("content");

//        System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / contentArray.length() = " + contentArray.length());

        List<ContentValues> drawerContent = new ArrayList<>();

        // Three levels content list:
        // level 1: category title and folder table Id (in drawer table)
        // level 2: page title, page table Id and page style (in folder table)
        // level 3: link title and link Uri (in page table)

        List<List<List<ContentValues>>> listL1 = new ArrayList<>();

        //----------------------------
        // level 1
        //----------------------------
        for (int h = 0; h < contentArray.length(); h++) {
            //System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / h = " + h);

            JSONObject cateObj = contentArray.getJSONObject(h);

            // get category content
            String category_name = cateObj.getString("category");
            ContentValues categoryValues = new ContentValues();
            categoryValues.put("folder_title", category_name);
            categoryValues.put("folder_table_id", lastFolderTableId+h+1);
            drawerContent.add(categoryValues);

            // create folder table
            DbHelper mOpenHelper = new DbHelper(mContext);
            SQLiteDatabase sqlDb;
            sqlDb = mOpenHelper.getWritableDatabase();
            int folder_table_id = lastFolderTableId+h+1;
            String folderTableCreated = DB_folder.DB_FOLDER_TABLE_PREFIX.concat(String.valueOf(folder_table_id));
            final String DB_CREATE_FOLDER_TABLE = "CREATE TABLE IF NOT EXISTS " + folderTableCreated + "(" +
                    DB_folder.KEY_PAGE_ID + " INTEGER PRIMARY KEY," +
                    DB_folder.KEY_PAGE_TITLE + " TEXT," +
                    DB_folder.KEY_PAGE_TABLE_ID + " INTEGER," +
                    DB_folder.KEY_PAGE_STYLE + " INTEGER," +
                    DB_folder.KEY_PAGE_CREATED + " INTEGER);";
            sqlDb.execSQL(DB_CREATE_FOLDER_TABLE);

            //----------------------------
            // level 2
            //----------------------------
            List<List<ContentValues>>  listL2 = new ArrayList<>();
            JSONArray pageArray = cateObj.getJSONArray(TAG_LINK_PAGE);
            //           System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / pageArray.length() = " + pageArray.length());
            for (int i = 0; i < pageArray.length(); i++) {

                // get page content
                JSONObject page = pageArray.getJSONObject(i);
                String pageTitle = page.getString(TAG_TITLE);
//                System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / pageTitle = " + pageTitle);

                // level 2 insert content: page to folder table
                DB_folder db_folder = new DB_folder(mContext, folder_table_id);
                db_folder.insertPage( folderTableCreated,
                        pageTitle,
                        i+1,
                        Define.STYLE_DEFAULT,true);//Define.STYLE_PREFER

                //----------------------------
                // level 3
                //----------------------------
                JSONArray linksArray = page.getJSONArray(TAG_LINKS);
                //System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / linksArray.length() = " + linksArray.length());

                // get links content
                List<ContentValues> listL3 = new ArrayList<>();
                for (int j = 0; j < linksArray.length(); j++) {
                    JSONObject link = linksArray.getJSONObject(j);

                    String linkTitle = link.optString("note_title");
                    System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / linkTitle = " + linkTitle);

                    String linkUrl = (String) link.opt("note_link_uri"); // Get the first link only.

                    String imageUri = (String) link.opt("note_image_uri"); // Get the first link only.

                    ContentValues videoValues = new ContentValues();
                    videoValues.put(Contract.VideoEntry.COLUMN_NOTE_TITLE, linkTitle);
                    videoValues.put(Contract.VideoEntry.COLUMN_NOTE_PICTURE_URI, imageUri);
                    videoValues.put(Contract.VideoEntry.COLUMN_NOTE_AUDIO_URI, "");
                    videoValues.put(Contract.VideoEntry.COLUMN_NOTE_DRAWING_URI, "");
                    videoValues.put(Contract.VideoEntry.COLUMN_NOTE_LINK_URI, linkUrl);
                    videoValues.put(Contract.VideoEntry.COLUMN_NOTE_BODY, "");
                    videoValues.put(Contract.VideoEntry.COLUMN_NOTE_MARKING, 1);
                    videoValues.put(Contract.VideoEntry.COLUMN_NOTE_CREATED, 1); //todo temp

                    listL3.add(videoValues);
                }

                // create page table
                sqlDb = mOpenHelper.getWritableDatabase();
                String tableId = String.valueOf(folder_table_id).concat("_").concat(String.valueOf(i+1)); //Id starts from 1
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
                sqlDb.execSQL(SQL_CREATE_PAGE_TABLE);

                listL2.add(listL3);
            }

            listL1.add(listL2);
        }

        // level 1 insert content: categories to drawer table
        ContentValues[] contentValues = drawerContent.toArray(new ContentValues[drawerContent.size()]);
        contentResolver.bulkInsert(Contract.CategoryEntry.CONTENT_URI, contentValues);//The key is the column name for the field.

        // level 3 insert content: links to page table
        //System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / contentList.size() = " + contentList.size());
        for(int i=0;i<listL1.size();i++) {
            //System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / i = " + i);
            for(int j=0;j<listL1.get(i).size();j++) {
//                System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / j = " + j);
                contentValues = listL1.get(i).get(j).toArray(new ContentValues[listL1.get(i).get(j).size()]);
                Provider.tableId = String.valueOf(lastFolderTableId + i + 1).concat("_").concat(String.valueOf(j + 1));//todo temp
                contentResolver.bulkInsert(Contract.VideoEntry.CONTENT_URI, contentValues);
            }
        }

        isParsing = false;
    }


    private String getJsonStringByFile(String filePath)
    {
        File file = new File(filePath);

        FileInputStream fileInputStream = null;
        try
        {
            fileInputStream = new FileInputStream(file);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }


        StringBuilder total = null;
        try
        {
            BufferedReader r = new BufferedReader(new InputStreamReader(fileInputStream));
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
        System.out.println("ParseJsonToDB / _getJsonString / jsonString = " + jsonString);

        return jsonString;
    }

    void handleParseJsonFileAndInsertDB()
    {
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                   parseJsonFileAndInsertDB(filePath);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    void handleViewJson()
    {
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    fileBody = getJsonStringByFile(filePath);
                    isParsing = false;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

}