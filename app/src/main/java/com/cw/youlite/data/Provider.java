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

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.cw.youlite.Utils;

import java.util.HashMap;

import androidx.annotation.NonNull;

/**
 * VideoProvider is a ContentProvider that provides videos for the rest of applications.
 */
public class Provider extends ContentProvider {
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    public DbHelper mOpenHelper;

    // These codes are returned from sUriMatcher#match when the respective Uri matches.
    private static final int VIDEO = 1;
    private static final int CATEGORY = 2;
    private static final int VIDEO_WITH_CATEGORY = 3;
    private static final int SEARCH_SUGGEST = 4;
    private static final int REFRESH_SHORTCUT = 5;

    private static SQLiteQueryBuilder sVideosContainingQueryBuilder;
    private static String[] sVideosContainingQueryColumns;
    private static final HashMap<String, String> sColumnMap = buildColumnMap();
    public ContentResolver mContentResolver;
    static public String table_id;
    Context context;

    @Override
    public boolean onCreate() {
        System.out.println("Provider / _onCreate");
        context = getContext();
        mContentResolver = context.getContentResolver();

        mOpenHelper = new DbHelper(context);

//        int focusCategoryNumber = Utils.getPref_focus_category_number(context);
//        table_id = String.valueOf(focusCategoryNumber);
//        System.out.println("Provider / _onCreate / table_id = " + table_id);
//
//
//        sVideosContainingQueryBuilder = new SQLiteQueryBuilder();
//        sVideosContainingQueryBuilder.setTables(Contract.VideoEntry.PAGE_TABLE_NAME.concat(table_id));
//        sVideosContainingQueryBuilder.setProjectionMap(sColumnMap);
//        sVideosContainingQueryColumns = new String[]{
//                Contract.VideoEntry._ID,
//                Contract.VideoEntry.COLUMN_NOTE_TITLE,
//                Contract.VideoEntry.COLUMN_NOTE_PICTURE_URI,
//                Contract.VideoEntry.COLUMN_NOTE_AUDIO_URI,
//                Contract.VideoEntry.COLUMN_NOTE_DRAWING_URI,
//                Contract.VideoEntry.COLUMN_NOTE_LINK_URI,
//                Contract.VideoEntry.COLUMN_NOTE_BODY,
//                Contract.VideoEntry.COLUMN_NOTE_MARKING,
//                Contract.VideoEntry.COLUMN_NOTE_CREATED,
//                SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID
//        };


        return true;
    }

    static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = Contract.CONTENT_AUTHORITY;

        // For each type of URI to add, create a corresponding code.
        matcher.addURI(authority, Contract.PATH_VIDEO, VIDEO);
        matcher.addURI(authority, Contract.PATH_VIDEO + "/*", VIDEO_WITH_CATEGORY);

        // For each type of URI to add, create a corresponding code.
        matcher.addURI(authority, Contract.PATH_CATEGORY, CATEGORY);
        matcher.addURI(authority, Contract.PATH_CATEGORY + "/*", VIDEO_WITH_CATEGORY);

        // Search related URIs.
        matcher.addURI(authority, "search/" + SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
        matcher.addURI(authority, "search/" + SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
        return matcher;
    }

    private Cursor getSuggestions(String query) {
        query = query.toLowerCase();
        return sVideosContainingQueryBuilder.query(
                mOpenHelper.getReadableDatabase(),
                sVideosContainingQueryColumns,
//                Contract.VideoEntry.COLUMN_NAME + " LIKE ? OR " +
//                        Contract.VideoEntry.COLUMN_DESC + " LIKE ?",
                Contract.VideoEntry.COLUMN_NOTE_TITLE + " LIKE ? OR " +
                        Contract.VideoEntry.COLUMN_NOTE_TITLE + " LIKE ?",
                new String[]{"%" + query + "%", "%" + query + "%"},
                null,
                null,
                null
        );
    }

    private static HashMap<String, String> buildColumnMap() {
        HashMap<String, String> map = new HashMap<>();
        map.put(Contract.VideoEntry._ID, Contract.VideoEntry._ID);
        map.put(Contract.VideoEntry.COLUMN_NOTE_TITLE, Contract.VideoEntry.COLUMN_NOTE_TITLE);
        map.put(Contract.VideoEntry.COLUMN_NOTE_LINK_URI,Contract.VideoEntry.COLUMN_NOTE_LINK_URI);
        map.put(Contract.VideoEntry.COLUMN_NOTE_PICTURE_URI,Contract.VideoEntry.COLUMN_NOTE_PICTURE_URI);
        //todo add more for search?
        map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, Contract.VideoEntry._ID + " AS " +
                SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
        map.put(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID,
                Contract.VideoEntry._ID + " AS " + SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);
        return map;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        System.out.println("----------------Provider / _query/ uri =  " + uri.toString());

//        table_id = String.valueOf(Utils.getPref_focus_category_number(context));
//        System.out.println("----------------Provider / _query/ table_id =  " + table_id);

        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
//            case SEARCH_SUGGEST: {
//                String rawQuery = "";
//                if (selectionArgs != null && selectionArgs.length > 0) {
//                    rawQuery = selectionArgs[0];
//                }
//                retCursor = getSuggestions(rawQuery);
//                break;
//            }
//            case VIDEO: {
//                retCursor = mOpenHelper.getReadableDatabase().query(
//                        Contract.VideoEntry.PAGE_TABLE_NAME.concat(table_id),//todo temp
//                        projection,
//                        selection,
//                        selectionArgs,
//                        null,
//                        null,
//                        sortOrder
//                );
//                break;
//            }
            case CATEGORY: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        Contract.CategoryEntry.DRAWER_TABLE_NAME,//todo temp
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }

        retCursor.setNotificationUri(mContentResolver, uri);
        return retCursor;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            // The application is querying the db for its own contents.
            case VIDEO_WITH_CATEGORY:
                return Contract.VideoEntry.CONTENT_TYPE;
            case VIDEO:
                return Contract.VideoEntry.CONTENT_TYPE;
            case CATEGORY:
                return Contract.CategoryEntry.CONTENT_TYPE;
            // The Android TV global search is querying our app for relevant content.
            case SEARCH_SUGGEST:
                return SearchManager.SUGGEST_MIME_TYPE;
            case REFRESH_SHORTCUT:
                return SearchManager.SHORTCUT_MIME_TYPE;

            // We aren't sure what is being asked of us.
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        System.out.println("Provider / _insert/ uri = " + uri.toString());
        final Uri returnUri;
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case VIDEO: {
                long _id = mOpenHelper.getWritableDatabase().insert(
                        Contract.VideoEntry.PAGE_TABLE_NAME, null, values);
                if (_id > 0) {
                    returnUri = Contract.VideoEntry.buildVideoUri(_id);
                } else {
                    throw new SQLException("Failed to insert row into " + uri);
                }
                break;
            }
            case CATEGORY: {
                long _id = mOpenHelper.getWritableDatabase().insert(
                        Contract.CategoryEntry.DRAWER_TABLE_NAME, null, values);
                if (_id > 0) {
                    returnUri = Contract.CategoryEntry.buildCategoryUri(_id);
                } else {
                    throw new SQLException("Failed to insert row into " + uri);
                }
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }

        mContentResolver.notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        final int rowsDeleted;

        if (selection == null) {
            throw new UnsupportedOperationException("Cannot delete without selection specified.");
        }

        switch (sUriMatcher.match(uri)) {
            case VIDEO: {
                rowsDeleted = mOpenHelper.getWritableDatabase().delete(
                        Contract.VideoEntry.PAGE_TABLE_NAME, selection, selectionArgs);
                break;
            }
            case CATEGORY: {
                rowsDeleted = mOpenHelper.getWritableDatabase().delete(
                        Contract.CategoryEntry.DRAWER_TABLE_NAME, selection, selectionArgs);
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }

        if (rowsDeleted != 0) {
            mContentResolver.notifyChange(uri, null);
        }

        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        final int rowsUpdated;

        switch (sUriMatcher.match(uri)) {
            case VIDEO: {
                rowsUpdated = mOpenHelper.getWritableDatabase().update(
                        Contract.VideoEntry.PAGE_TABLE_NAME, values, selection, selectionArgs);
                break;
            }
            case CATEGORY: {
                rowsUpdated = mOpenHelper.getWritableDatabase().update(
                        Contract.CategoryEntry.DRAWER_TABLE_NAME, values, selection, selectionArgs);
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }

        if (rowsUpdated != 0) {
            mContentResolver.notifyChange(uri, null);
        }

        return rowsUpdated;
    }

    public static String tableId;

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        System.out.println("Provider / _bulkInsert / uri = " + uri.toString());
        System.out.println("Provider / _bulkInsert / values.length = " + values.length);
        switch (sUriMatcher.match(uri)) {
            case VIDEO: {
                final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int returnCount = 0;

                db.beginTransaction();
                try {
                    for (ContentValues value : values) {
                        long _id = db.insertWithOnConflict(Contract.VideoEntry.PAGE_TABLE_NAME.concat(tableId),
                                null, value, SQLiteDatabase.CONFLICT_REPLACE);

                        System.out.println("Provider / _bulkInsert (case VIDEO) / _id = " + _id);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                mContentResolver.notifyChange(uri, null);
                return returnCount;
            }
            case CATEGORY: {
                final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int returnCount = 0;

                db.beginTransaction();
                try {
                    for (ContentValues value : values) {
                        long _id = db.insertWithOnConflict(Contract.CategoryEntry.DRAWER_TABLE_NAME,
                                null, value, SQLiteDatabase.CONFLICT_REPLACE);

                        System.out.println("Provider / _bulkInsert (case CATEGORY) / _id = " + _id);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                mContentResolver.notifyChange(uri, null);
                return returnCount;
            }
            default: {
                return super.bulkInsert(uri, values);
            }
        }
    }
}
