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

package com.cw.youlite.note_edit;

import java.util.Date;

import com.cw.youlite.db.DB_folder;
import com.cw.youlite.main.MainAct;
import com.cw.youlite.R;
import com.cw.youlite.db.DB_page;
import com.cw.youlite.tabs.TabsHost;
import com.cw.youlite.util.MyEditText;
import com.cw.youlite.util.image.TouchImageView;
import com.cw.youlite.util.image.UtilImage_bitmapLoader;
import com.cw.youlite.util.ColorSet;
import com.cw.youlite.util.preferences.Pref;
import com.cw.youlite.util.uil.UilCommon;
import com.cw.youlite.util.Util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.text.Html;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

public class Note_edit_ui {

	private ImageView picImageView;
	private String pictureUriInDB;
	String oriPictureUri;
	String currPictureUri;

	String oriLinkUri;

	private EditText linkEditText;
	private MyEditText titleEditText;
	private String oriTitle;

	private Long noteId;
	private Long oriCreatedTime;
	private Long oriMarking;

	boolean bRollBackData;
	boolean bRemovePictureUri = false;
	private boolean bEditPicture = false;

    private DB_page dB_page;
	private Activity act;
	private int style;
	private ProgressBar progressBar;
	private ProgressBar progressBarExpand;
	private TouchImageView enlargedImage;

	Note_edit_ui(Activity act, DB_page _db, Long noteId, String strTitle, String pictureUri, String linkUri, Long createdTime)
    {
    	this.act = act;
    	this.noteId = noteId;
    			
    	oriTitle = strTitle;
	    oriPictureUri = pictureUri;
	    oriLinkUri = linkUri;
	    
	    oriCreatedTime = createdTime;
	    currPictureUri = pictureUri;

	    dB_page = _db;//Page.mDb_page;
	    
	    oriMarking = dB_page.getNoteMarking_byId(noteId);
		
	    bRollBackData = false;
		bEditPicture = true;
		bShowEnlargedImage = false;
    }

	void UI_init()
    {

		UI_init_text();

    	linkEditText = (EditText) act.findViewById(R.id.edit_link);
        picImageView = (ImageView) act.findViewById(R.id.edit_picture);

        progressBar = (ProgressBar) act.findViewById(R.id.edit_progress_bar);
        progressBarExpand = (ProgressBar) act.findViewById(R.id.edit_progress_bar_expand);

		DB_folder dbFolder = new DB_folder(act, Pref.getPref_focusView_folder_tableId(act));
		style = dbFolder.getPageStyle(TabsHost.getFocus_tabPos(), true);

		enlargedImage = (TouchImageView)act.findViewById(R.id.expanded_image);

		//set link color
		if(linkEditText != null)
		{
			linkEditText.setTextColor(ColorSet.mText_ColorArray[style]);
			linkEditText.setBackgroundColor(ColorSet.mBG_ColorArray[style]);
		}

		picImageView.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

	    final InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);

		// set thumb nail listener
        picImageView.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view) {
            	if(bShowEnlargedImage)
            	{
            		closeEnlargedImage();
            		// show soft input
//            		if (act.getCurrentFocus() != null)
//            		    imm.showSoftInput(act.getCurrentFocus(), 0);
            	}
            	else
                {
            		// hide soft input
            		if (act.getCurrentFocus() != null)
            			imm.hideSoftInputFromWindow(act.getCurrentFocus().getWindowToken(), 0);

                	System.out.println("Note_edit_ui / pictureUriInDB = " + pictureUriInDB);
                	if( (!Util.isEmptyString(pictureUriInDB))  )
                	{
                		bRemovePictureUri = false;
                		System.out.println("picImageView.setOnClickListener / pictureUriInDB = " + pictureUriInDB);

                		// check if pictureUri has scheme
                		if(Util.isUriExisted(pictureUriInDB, act) )
                		{
	                		if(Uri.parse(pictureUriInDB).isAbsolute())
	                		{
//	                			int style =  Util.getCurrentPageStyle(TabsHost.getFocus_tabPos());
	                			new UtilImage_bitmapLoader(enlargedImage,
                                                           pictureUriInDB,
                                                           progressBarExpand,
//	                					                   (style % 2 == 1 ?
//                                                            UilCommon.optionsForRounded_light:
//                                                            UilCommon.optionsForRounded_dark),
                                                           UilCommon.optionsForFadeIn,
                                                           act);
	                			bShowEnlargedImage = true;
	                		}
	                		else
	                		{
	                			System.out.println("pictureUriInDB is not Uri format");
	                		}
                		}
                		else
                			Toast.makeText(act,R.string.file_not_found,Toast.LENGTH_SHORT).show();
                	}
                	else
            			Toast.makeText(act,R.string.file_is_not_created,Toast.LENGTH_SHORT).show();

				}
            }
        });

		// set thumb nail long click listener
        picImageView.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View view) {
            	if(bEditPicture) {
					if(!Util.isEmptyString(pictureUriInDB) )
						openSetPictureDialog();
				}
                return false;
            }
        });
    }

	private void UI_init_text()
	{
        int focusFolder_tableId = Pref.getPref_focusView_folder_tableId(act);
        DB_folder db = new DB_folder(MainAct.mAct, focusFolder_tableId);
		style = db.getPageStyle(TabsHost.getFocus_tabPos(), true);

		LinearLayout block = (LinearLayout) act.findViewById(R.id.edit_title_block);
		if(block != null)
			block.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

		titleEditText =  (MyEditText) act.findViewById(R.id.edit_title);
		linkEditText = (EditText) act.findViewById(R.id.edit_link);

		//set title color
		titleEditText.setTextColor(ColorSet.mText_ColorArray[style]);
		titleEditText.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

		//set link color
		linkEditText.setTextColor(ColorSet.mText_ColorArray[style]);
		linkEditText.setBackgroundColor(ColorSet.mBG_ColorArray[style]);
	}

    // set image close listener
	private void setCloseImageListeners(EditText editText)
    {
    	editText.setOnClickListener(new OnClickListener()
    	{   @Override
			public void onClick(View v) 
			{
				if(bShowEnlargedImage)
					closeEnlargedImage();
			}
		});
    	
    	editText.setOnFocusChangeListener(new OnFocusChangeListener() 
    	{   @Override
            public void onFocusChange(View v, boolean hasFocus) 
    		{
    				if(bShowEnlargedImage)
    					closeEnlargedImage();
            } 
    	});   
    }


	boolean bShowEnlargedImage;
	void closeEnlargedImage()
    {
    	System.out.println("closeExpandImage");
		enlargedImage.setVisibility(View.GONE);
		bShowEnlargedImage = false;
    }

	private void openSetPictureDialog()
    {
		AlertDialog.Builder builder = new AlertDialog.Builder(act);
		builder.setTitle(R.string.edit_note_set_picture_dlg_title)
			   .setMessage(currPictureUri)
			   .setNeutralButton(R.string.btn_Select, new DialogInterface.OnClickListener()
			   {
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						bRemovePictureUri = false; // reset
						// For selecting local gallery
//						Intent intent = new Intent(act, PictureGridAct.class);
//						intent.putExtra("gallery", false);
//						act.startActivityForResult(intent, Util.ACTIVITY_SELECT_PICTURE);
						
						// select global
						final String[] items = new String[]{act.getResources().getText(R.string.note_ready_image).toString(),
															act.getResources().getText(R.string.note_ready_video).toString()};
					    AlertDialog.Builder builder = new AlertDialog.Builder(act);
					   
					    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener()
					    {
							@Override
							public void onClick(DialogInterface dialog, int which) 
							{
								String mediaType = null;
								if(which ==0)
									mediaType = "image/*";
								else if(which ==1)
									mediaType = "video/*";
								
								System.out.println("Note_edit_ui / _openSetPictureDialog / mediaType = " + mediaType);
								act.startActivityForResult(Util.chooseMediaIntentByType(act, mediaType),
				   						Util.CHOOSER_SET_PICTURE);	
								//end
								dialog.dismiss();
							}
					    };
					    builder.setTitle(R.string.edit_note_set_picture_dlg_title)
							   .setSingleChoiceItems(items, -1, listener)
							   .setNegativeButton(R.string.btn_Cancel, null)
							   .show();
					}
				})					
			   .setNegativeButton(R.string.btn_Cancel, new DialogInterface.OnClickListener()
			   {
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{// cancel
					}
				});

				if(!Util.isEmptyString(pictureUriInDB))
				{
					builder.setPositiveButton(R.string.btn_None, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which) 
						{
							//just delete picture file name
							currPictureUri = "";
							oriPictureUri = "";
					    	removePictureStringFromCurrentEditNote(noteId);
					    	populateFields_all(noteId);
					    	bRemovePictureUri = true;
						}
					});
				}
		
		Dialog dialog = builder.create();
		dialog.show();
    }

	void deleteNote(Long rowId)
    {
    	System.out.println("Note_edit_ui / _deleteNote");
        // for Add new note (noteId is null first), but decide to cancel
        if(rowId != null)
        	dB_page.deleteNote(rowId,true);
    }
    
    // populate text fields
	void populateFields_text(Long rowId)
	{
		if (rowId != null) {
			// title
			String strTitleEdit = dB_page.getNoteTitle_byId(rowId);
			titleEditText.setText(strTitleEdit);
			titleEditText.setSelection(strTitleEdit.length());
			// request cursor
			titleEditText.requestFocus();

			// link
			String strLinkEdit = dB_page.getNoteLinkUri_byId(rowId);
			linkEditText.setText(strLinkEdit);
			linkEditText.setSelection(strLinkEdit.length());
		}
        else
        {
            // renew title
            String strBlank = "";
            titleEditText.setText(strBlank);
            titleEditText.setSelection(strBlank.length());
            titleEditText.requestFocus();

			// renew link
			linkEditText.setText(strBlank);
			linkEditText.setSelection(strBlank.length());
        }
	}

    // populate all fields
	void populateFields_all(Long rowId)
    {
    	if (rowId != null) 
    	{
			populateFields_text(rowId);

    		// for picture block
			pictureUriInDB = dB_page.getNotePictureUri_byId(rowId);
			System.out.println("Note_edit_ui /  _populateFields_all / pictureUriInDB = " + pictureUriInDB);
    		
			// load bitmap to image view
		    if(pictureUriInDB == null)
		    {
			    String thumbUri ="";
			    System.out.println("populateFields_all / oriLinkUri = " + oriLinkUri);
			    if(!Util.isEmptyString(oriLinkUri) && Util.isYouTubeLink(oriLinkUri)      )
			    {
				    thumbUri = "https://img.youtube.com/vi/"+Util.getYoutubeId(oriLinkUri)+"/0.jpg";
				    System.out.println("populateFields_all / thumbUri = " + thumbUri);

				    new UtilImage_bitmapLoader(picImageView,
						    thumbUri,
						    progressBar,
						    UilCommon.optionsForFadeIn,
						    act);
			    }
		    }
			else
			{
	    		picImageView.setImageResource(style %2 == 1 ?
		    			R.drawable.btn_radio_off_holo_light:
		    			R.drawable.btn_radio_off_holo_dark);
			}
			
			// set listeners for closing image view 
	    	if(!Util.isEmptyString(pictureUriInDB))
	    	{
	    		setCloseImageListeners(linkEditText);
	    		setCloseImageListeners(titleEditText);
	    	}
	    	
    		// link
			String strLinkEdit = dB_page.getNoteLink_byId(rowId);
            linkEditText.setText(strLinkEdit);
            linkEditText.setSelection(strLinkEdit.length());

            // title        	
			String strTitleEdit = dB_page.getNoteTitle_byId(rowId);
			final String curLinkStr = linkEditText.getText().toString();
			if( Util.isEmptyString(strTitleEdit) &&
				Util.isEmptyString(titleEditText.getText().toString()) )
			{
				if(Util.isYouTubeLink(curLinkStr) )
				{
					final String hint = Util.getYouTubeTitle(curLinkStr);

					titleEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            if (hasFocus) {
								titleEditText.setHint(Html.fromHtml("<small style=\"text-color: gray;\"><i>" +
																	  hint +
																	  "</i></small>") );
                            }
                        }
                    });

					titleEditText.setOnTouchListener(new View.OnTouchListener() {
						@Override
						public boolean onTouch(View v, MotionEvent event) {
						        ((EditText) v).setText(hint);
                                ((EditText) v).setSelection(hint.length());
                                v.performClick();
							return false;
						}
					});
				}
				else if(curLinkStr.startsWith("http"))
				{
					Util.setHttpTitle(curLinkStr, act, titleEditText);
				}
			}
        }
    	else
    	{
            // renew link
			String strLinkEdit = "";
			if(linkEditText != null)
			{
	            linkEditText.setText(strLinkEdit);
	            linkEditText.setSelection(strLinkEdit.length());
	            linkEditText.requestFocus();
			}
    	}
    }

	private boolean isLinkUriModified()
    {
    	return !oriLinkUri.equals(linkEditText.getText().toString());
    }

	private boolean isTitleModified()
    {
    	return !oriTitle.equals(titleEditText.getText().toString());
    }

	private boolean isPictureModified()
    {
	    if ( (oriPictureUri == null) && (pictureUriInDB == null) )
	        return  false;
		else
    	    return !oriPictureUri.equals(pictureUriInDB);
    }

	boolean isNoteModified()
    {
    	boolean bModified = false;
//		System.out.println("Note_edit_ui / _isNoteModified / isTitleModified() = " + isTitleModified());
//		System.out.println("Note_edit_ui / _isNoteModified / isPictureModified() = " + isPictureModified());
//		System.out.println("Note_edit_ui / _isNoteModified / bRemovePictureUri = " + bRemovePictureUri);
    	if( isTitleModified() ||
    		isPictureModified() ||
    		isLinkUriModified() ||
    		bRemovePictureUri )
    	{
    		bModified = true;
    	}
    	
    	return bModified;
    }

	Long saveStateInDB(Long rowId,boolean enSaveDb, String pictureUri)
	{
		String linkUri = "";
		if(linkEditText != null)
			linkUri = linkEditText.getText().toString();
    	String title = titleEditText.getText().toString();

        if(enSaveDb)
        {
	        if (rowId == null) // for Add new
	        {
	        	if( (!Util.isEmptyString(title)) ||
	        		(!Util.isEmptyString(pictureUri)) ||
	        		(!Util.isEmptyString(linkUri))            )
	        	{
	        		// insert
	        		System.out.println("Note_edit_ui / _saveStateInDB / insert");
	        		rowId = dB_page.insertNote(title, pictureUri,   linkUri,  0, (long) 0);// add new note, get return row Id
	        	}
        		currPictureUri = pictureUri; // update file name
	        }
	        else // for Edit
	        {
    	        Date now = new Date();
	        	if( !Util.isEmptyString(title) ||
	        		!Util.isEmptyString(pictureUri) ||
	        		!Util.isEmptyString(linkUri)       )
	        	{
	        		// update
	        		if(bRollBackData) //roll back
	        		{
			        	System.out.println("Note_edit_ui / _saveStateInDB / update: roll back");
			        	linkUri = oriLinkUri;
	        			title = oriTitle;
	        			Long time = oriCreatedTime;
	        			dB_page.updateNote(rowId, title, pictureUri,  linkUri,  oriMarking, time,true);
	        		}
	        		else // update new
	        		{
	        			System.out.println("Note_edit_ui / _saveStateInDB / update new");
						System.out.println("--- rowId = " + rowId);
						System.out.println("--- oriMarking = " + oriMarking);

                        long marking;
                        if(null == oriMarking)
                            marking = 0;
                        else
                            marking = oriMarking;

                        boolean isOK;
	        			isOK = dB_page.updateNote(rowId, title, pictureUri,  linkUri,
												marking, now.getTime(),true); // update note
	        		}
	        		currPictureUri = pictureUri;
	        	}
	        	else if( Util.isEmptyString(title) &&
 						 Util.isEmptyString(pictureUri) &&
			        	 Util.isEmptyString(linkUri)         )
	        	{
	        		// delete
	        		System.out.println("Note_edit_ui / _saveStateInDB / delete");
	        		deleteNote(rowId);
			        rowId = null;
	        	}
	        }
        }

		return rowId;
	}

	// for confirmation condition
	void removePictureStringFromOriginalNote(Long rowId) {
    	dB_page.updateNote(rowId,
				oriTitle,
    				   "",
				oriLinkUri,
				oriMarking,
				oriCreatedTime, true );
	}

	private void removePictureStringFromCurrentEditNote(Long rowId) {
        String linkUri = linkEditText.getText().toString();
        String title = titleEditText.getText().toString();

    	dB_page.updateNote(rowId,
    				   title,
    				   "",
    				   linkUri,
				oriMarking,
				oriCreatedTime, true );
	}

	void removeLinkUriFromCurrentEditNote(Long rowId) {
        String title = titleEditText.getText().toString();
        dB_page.updateNote(rowId,
    				   title,
				oriPictureUri,
    				   "",
				oriMarking,
				oriCreatedTime, true );
	}

	public int getCount()
	{
		int noteCount = dB_page.getNotesCount(true);
		return noteCount;
	}
	
}
