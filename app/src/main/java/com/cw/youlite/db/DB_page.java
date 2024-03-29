/*
 * Copyright (C) 2020 CW Chiu
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

package com.cw.youlite.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import java.util.Date;


/**
 *  Data Base Class for Page
 *
 */
public class DB_page
{

    private Context mContext = null;
    private static DatabaseHelper mDbHelper ;
    private SQLiteDatabase mSqlDb;

	// Table name format: Page1_2
	public static String DB_PAGE_TABLE_PREFIX = "Page";
    public static String DB_PAGE_TABLE_NAME; // Note: name = prefix + id

	// Note rows
    public static final String KEY_NOTE_ID = "_id"; //do not rename _id for using CursorAdapter (BaseColumns._ID)
    public static final String KEY_NOTE_TITLE = "note_title";
    public static final String KEY_NOTE_MARKING = "note_marking";
    public static final String KEY_NOTE_PICTURE_URI = "note_picture_uri";
    public static final String KEY_NOTE_LINK_URI = "note_link_uri";
    public static final String KEY_NOTE_CREATED = "note_created";

	// DB
    public DB_page mDb_page;

	// Cursor
	public Cursor mCursor_note;

	// Table Id
    private static int mTableId_page;

    /** Constructor */
	public DB_page(Context context, int pageTableId)
	{
		mContext = context;
		//System.out.println("DB_page / constructor / pageTableId = " + pageTableId);
		setFocusPage_tableId(pageTableId);
	}

    /**
     * DB functions
     * 
     */
	public DB_page open() throws SQLException
	{
		mDbHelper = new DatabaseHelper(mContext);

		// Will call DatabaseHelper.onCreate()first time when WritableDatabase is not created yet
		mSqlDb = mDbHelper.getWritableDatabase();

		//try to get note cursor
		try
		{
			//System.out.println("DB_page / _open / open page table Try / getFocusPage_tableId() = " + getFocusPage_tableId());
			mCursor_note = this.getNoteCursor_byPageTableId(getFocusPage_tableId());
		}
		catch(Exception e)
		{
			System.out.println("DB_page / _open / open page table NG! / table name = " + DB_PAGE_TABLE_NAME);
		}//catch

		return DB_page.this;
	}

	public void close()
	{
		if((mCursor_note != null)&& (!mCursor_note.isClosed()))
			mCursor_note.close();

		mDbHelper.close();
	}

    /**
     *  Page table columns for note row
     * 
     */
    private String[] strNoteColumns = new String[] {
          KEY_NOTE_ID,
          KEY_NOTE_TITLE,
          KEY_NOTE_PICTURE_URI,
          KEY_NOTE_LINK_URI,
          KEY_NOTE_MARKING,
          KEY_NOTE_CREATED
      };

    // select all notes
    private Cursor getNoteCursor_byPageTableId(int pageTableId) {

        // table number initialization: name = prefix + id
        DB_PAGE_TABLE_NAME = DB_PAGE_TABLE_PREFIX.concat(
                                                    String.valueOf(DB_folder.getFocusFolder_tableId())+
                                                    "_"+
                                                    String.valueOf(pageTableId) );

        return mSqlDb.query(DB_PAGE_TABLE_NAME,
             strNoteColumns,
             null, 
             null, 
             null, 
             null, 
             null  
             );    
    }   
    
    //set page table id
    public static void setFocusPage_tableId(int id)
    {
    	mTableId_page = id;
    }
    
    //get page table id
    public static int getFocusPage_tableId()
    {
    	return mTableId_page;
    }
    
    // Insert note
    // createTime: 0 will update time
    public long insertNote(String title,String pictureUri, String linkUri,  int marking, Long createTime)
    {
    	this.open();

        Date now = new Date();  
        ContentValues args = new ContentValues(); 
        args.put(KEY_NOTE_TITLE, title);   
        args.put(KEY_NOTE_PICTURE_URI, pictureUri);
        args.put(KEY_NOTE_LINK_URI, linkUri);
        if(createTime == 0)
        	args.put(KEY_NOTE_CREATED, now.getTime());
        else
        	args.put(KEY_NOTE_CREATED, createTime);
        	
        args.put(KEY_NOTE_MARKING,marking);
        long rowId = mSqlDb.insert(DB_PAGE_TABLE_NAME, null, args);

		System.out.println("DB_page / _insertNote / DB_PAGE_TABLE_NAME = " + DB_PAGE_TABLE_NAME +
				" & rowId = " + rowId);
        this.close();

        return rowId;  
    }

	// without open/close
	public long insertNote_no_openClose(String title, String pictureUri, String linkUri, int marking, Long createTime)
	{
		Date now = new Date();
		ContentValues args = new ContentValues();
		args.put(KEY_NOTE_TITLE, title);
		args.put(KEY_NOTE_PICTURE_URI, pictureUri);
		args.put(KEY_NOTE_LINK_URI, linkUri);
		if(createTime == 0)
			args.put(KEY_NOTE_CREATED, now.getTime());
		else
			args.put(KEY_NOTE_CREATED, createTime);

		args.put(KEY_NOTE_MARKING,marking);
		long rowId = mSqlDb.insert(DB_PAGE_TABLE_NAME, null, args);

		System.out.println("DB_page / _insertNote / DB_PAGE_TABLE_NAME = " + DB_PAGE_TABLE_NAME +
				" & rowId = " + rowId);
		return rowId;
	}


	public boolean deleteNote(long rowId,boolean enDbOpenClose)
    {
    	if(enDbOpenClose)
    		this.open();

    	int rowsEffected = mSqlDb.delete(DB_PAGE_TABLE_NAME, KEY_NOTE_ID + "=" + rowId, null);

        if(enDbOpenClose)
        	this.close();

        return (rowsEffected > 0);
    }    
    
    //query note
    public Cursor queryNote(long rowId) throws SQLException 
    {  
        Cursor mCursor = mSqlDb.query(true,
									DB_PAGE_TABLE_NAME,
					                new String[] {KEY_NOTE_ID,
				  								  KEY_NOTE_TITLE,
				  								  KEY_NOTE_PICTURE_URI,
				  								  KEY_NOTE_LINK_URI,
        										  KEY_NOTE_MARKING,
        										  KEY_NOTE_CREATED},
					                KEY_NOTE_ID + "=" + rowId,
					                null, null, null, null, null);

        if (mCursor != null) { 
            mCursor.moveToFirst();
        }

        return mCursor;
    }

    // update note
    // 		createTime:  0 for Don't update time
    @SuppressLint("Range")
    public boolean updateNote(long rowId, String title, String pictureUri,
                              String linkUri, long marking, long createTime, boolean enDbOpenClose)
    {
//	    System.out.println("DB_page / _updateNote / rowId = " + rowId);
//	    System.out.println("DB_page / _updateNote / title = " + title);
    	if(enDbOpenClose)
    		this.open();

        ContentValues args = new ContentValues();
        args.put(KEY_NOTE_TITLE, title);
        args.put(KEY_NOTE_PICTURE_URI, pictureUri);
        args.put(KEY_NOTE_LINK_URI, linkUri);
        args.put(KEY_NOTE_MARKING, marking);
        
        Cursor cursor = queryNote(rowId);
        if(createTime == 0) {
			if( (cursor!=null) && (cursor.getColumnIndex(KEY_NOTE_CREATED)>=0)) {
				args.put(KEY_NOTE_CREATED,cursor.getLong(cursor.getColumnIndex(KEY_NOTE_CREATED)));
			}
        }
        else
        	args.put(KEY_NOTE_CREATED, createTime);

        int cUpdateItems = mSqlDb.update(DB_PAGE_TABLE_NAME, args, KEY_NOTE_ID + "=" + rowId, null);

		if(enDbOpenClose)
        	this.close();

		return cUpdateItems > 0;
    }    
    
    
	public int getNotesCount(boolean enDbOpenClose)
	{
		if(enDbOpenClose)
			this.open();

		int count = 0;
		if(mCursor_note != null)
			count = mCursor_note.getCount();

		if(enDbOpenClose)
			this.close();

		return count;
	}	
	
	public int getCheckedNotesCount()
	{
		this.open();

		int countCheck =0;
		int notesCount = getNotesCount(false);
		for(int i=0;i< notesCount ;i++)
		{
			if(getNoteMarking(i,false) == 1)
				countCheck++;
		}

		this.close();

		return countCheck;
	}
	
	
	// get note by Id
	public String getNoteLink_byId(Long mRowId)
	{
		this.open();

		String link = queryNote(mRowId).getString(queryNote(mRowId)
									   .getColumnIndexOrThrow(DB_page.KEY_NOTE_LINK_URI));
		this.close();

		return link;
	}	
	
	public String getNoteTitle_byId(Long mRowId)
	{
		this.open();

		String title = queryNote(mRowId).getString(queryNote(mRowId)
											.getColumnIndexOrThrow(DB_page.KEY_NOTE_TITLE));

		this.close();

		return title;
	}
	
	public String getNotePictureUri_byId(Long mRowId)
	{
		this.open();

        String pictureUri = queryNote(mRowId).getString(queryNote(mRowId)
														.getColumnIndexOrThrow(DB_page.KEY_NOTE_PICTURE_URI));

		this.close();

		return pictureUri;
	}
	
	public String getNotePictureUri_byId(Long mRowId, boolean enOpen, boolean enClose)
	{
		if(enOpen)
			this.open();

        String pictureUri = queryNote(mRowId).getString(queryNote(mRowId)
														.getColumnIndexOrThrow(DB_page.KEY_NOTE_PICTURE_URI));
		if(enClose)
			this.close();

		return pictureUri;
	}	
	
	public String getNoteLinkUri_byId(Long mRowId)
	{
		this.open();
		String linkUri = queryNote(mRowId).getString(queryNote(mRowId)
														.getColumnIndexOrThrow(DB_page.KEY_NOTE_LINK_URI));
		this.close();

		return linkUri;
	}		
	
	public Long getNoteMarking_byId(Long mRowId)
	{
		this.open();
		Long marking = queryNote(mRowId).getLong(queryNote(mRowId)
											.getColumnIndexOrThrow(DB_page.KEY_NOTE_MARKING));
		this.close();

		return marking;

	}

	public Long getNoteCreatedTime_byId(Long mRowId)
	{
		this.open();

		Long time = queryNote(mRowId).getLong(queryNote(mRowId)
											.getColumnIndexOrThrow(DB_page.KEY_NOTE_CREATED));

		this.close();

		return time;
	}

	// get note by position
	@SuppressLint("Range")
	public Long getNoteId(int position,boolean enDbOpenClose)
	{
		if(enDbOpenClose)
			this.open();

		mCursor_note.moveToPosition(position);
		Long id = null;
		if( (mCursor_note!=null) && (mCursor_note.getColumnIndex(KEY_NOTE_ID)>=0)) {
			id = mCursor_note.getLong(mCursor_note.getColumnIndex(KEY_NOTE_ID));
		}

		if(enDbOpenClose)
	    	this.close();

		return id;
	}	
	
	@SuppressLint("Range")
	public String getNoteTitle(int position, boolean enDbOpenClose)
	{
		String title = null;

		if(enDbOpenClose)
			this.open();

		if(mCursor_note.moveToPosition(position)) {
			if ((mCursor_note != null) && (mCursor_note.getColumnIndex(KEY_NOTE_TITLE) >= 0))
				title = mCursor_note.getString(mCursor_note.getColumnIndex(KEY_NOTE_TITLE));
		}
		if(enDbOpenClose)
        	this.close();

		return title;
	}

	@SuppressLint("Range")
	public String getNotePictureUri(int position,boolean enDbOpenClose)
	{
		if(enDbOpenClose)
			this.open();

		mCursor_note.moveToPosition(position);
		
		String pictureUri = null;
		if( (mCursor_note!=null) && (mCursor_note.getColumnIndex(KEY_NOTE_PICTURE_URI)>=0)) {
			 pictureUri = mCursor_note.getString(mCursor_note.getColumnIndex(KEY_NOTE_PICTURE_URI));
		}

		if(enDbOpenClose)
        	this.close();

		return pictureUri;
	}
	
	@SuppressLint("Range")
	public String getNoteLinkUri(int position, boolean enDbOpenClose)
	{
		if(enDbOpenClose) 
			this.open();

		mCursor_note.moveToPosition(position);

		String linkUri = null;
		if( (mCursor_note!=null) && (mCursor_note.getColumnIndex(KEY_NOTE_LINK_URI)>=0)) {
			linkUri = mCursor_note.getString(mCursor_note.getColumnIndex(KEY_NOTE_LINK_URI));
		}

		if(enDbOpenClose)
        	this.close();

		return linkUri;
	}	
	
	@SuppressLint("Range")
	public int getNoteMarking(int position, boolean enDbOpenClose)
	{
		if(enDbOpenClose)
			this.open();

		mCursor_note.moveToPosition(position);
		int marking = 0;
		if( (mCursor_note!=null) && (mCursor_note.getColumnIndex(KEY_NOTE_MARKING)>=0)) {
			marking = mCursor_note.getInt(mCursor_note.getColumnIndex(KEY_NOTE_MARKING));
		}
		if(enDbOpenClose)
			this.close();

		return marking;
	}
	
	@SuppressLint("Range")
	public Long getNoteCreatedTime(int position, boolean enDbOpenClose)
	{
		if(enDbOpenClose)
			this.open();

		mCursor_note.moveToPosition(position);
		Long time = null;
		if( (mCursor_note!=null) && (mCursor_note.getColumnIndex(KEY_NOTE_CREATED)>=0)) {
			time = mCursor_note.getLong(mCursor_note.getColumnIndex(KEY_NOTE_CREATED));
		}
		if(enDbOpenClose)
			this.close();

		return time;
	}
}