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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.cw.youlite.data.Contract.VideoEntry;
import com.cw.youlite.db.DB_drawer;
import com.cw.youlite.db.DB_folder;
import com.cw.youlite.db.DB_page;

/**
 * VideoDbHelper manages the creation and upgrade of the database used in this sample.
 */
public class DbHelper extends SQLiteOpenHelper {

    // Change this when you change the database schema.
    private static final int DATABASE_VERSION = 4;

    // The name of our database.
    public static final String DATABASE_NAME = "youlite.db";

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        System.out.println("DbHelper / constructor");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

	    // Create Drawer table
	    System.out.println("DbHelper / _onCreate (will create Drawer table)");
	    final String DB_CREATE_DRAWER_TABLE = "CREATE TABLE IF NOT EXISTS " + DB_drawer.DB_DRAWER_TABLE_NAME + "(" +
			    DB_drawer.KEY_FOLDER_ID + " INTEGER PRIMARY KEY," +
			    DB_drawer.KEY_FOLDER_TABLE_ID + " INTEGER," +
			    DB_drawer.KEY_FOLDER_TITLE + " TEXT," +
			    DB_drawer.KEY_FOLDER_CREATED + " INTEGER);";

	    db.execSQL(DB_CREATE_DRAWER_TABLE);

	    // Create folder table
	    System.out.println("DbHelper / _onCreate (will create Folder1 table)");
	    int id = 1;
	    String tableCreated = DB_folder.DB_FOLDER_TABLE_PREFIX.concat(String.valueOf(id));
	    final String DB_CREATE_FOLDER_TABLE = "CREATE TABLE IF NOT EXISTS " + tableCreated + "(" +
			    DB_folder.KEY_PAGE_ID + " INTEGER PRIMARY KEY," +
			    DB_folder.KEY_PAGE_TITLE + " TEXT," +
			    DB_folder.KEY_PAGE_TABLE_ID + " INTEGER," +
			    DB_folder.KEY_PAGE_STYLE + " INTEGER," +
			    DB_folder.KEY_PAGE_CREATED + " INTEGER);";
	    db.execSQL(DB_CREATE_FOLDER_TABLE);

        // Create page table
	    System.out.println("DbHelper / _onCreate (will create Page1_1 table)");
        final String DB_CREATE_PAGE_TABLE = "CREATE TABLE IF NOT EXISTS " + DB_page.DB_PAGE_TABLE_PREFIX.concat("1_1") + "(" +
	    DB_page.KEY_NOTE_ID + " INTEGER PRIMARY KEY," +
	    DB_page.KEY_NOTE_TITLE + " TEXT," +
	    DB_page.KEY_NOTE_PICTURE_URI + " TEXT," +
	    DB_page.KEY_NOTE_LINK_URI + " TEXT NOT NULL," + // TEXT UNIQUE NOT NULL will make the URL unique.
	    DB_page.KEY_NOTE_MARKING + " INTEGER," +
	    DB_page.KEY_NOTE_CREATED + " INTEGER);";

        db.execSQL(DB_CREATE_PAGE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Simply discard all old data and start over when upgrading.
        db.execSQL("DROP TABLE IF EXISTS " + VideoEntry.PAGE_TABLE_NAME);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Do the same thing as upgrading...
        onUpgrade(db, oldVersion, newVersion);
    }
}