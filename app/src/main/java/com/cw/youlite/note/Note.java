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

package com.cw.youlite.note;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.cw.youlite.R;
import com.cw.youlite.db.DB_folder;
import com.cw.youlite.db.DB_page;
import com.cw.youlite.define.Define;
import com.cw.youlite.main.MainAct;
import com.cw.youlite.operation.mail.MailNotes;
import com.cw.youlite.page.PageAdapter_recycler;
import com.cw.youlite.tabs.TabsHost;
import com.cw.youlite.util.CustomWebView;
import com.cw.youlite.util.DeleteFileAlarmReceiver;
import com.cw.youlite.util.Util;
import com.cw.youlite.util.preferences.Pref;
import com.cw.youlite.util.uil.UilCommon;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class Note extends AppCompatActivity
{
    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    public ViewPager viewPager;
    public static boolean isPagerActive;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    public static PagerAdapter mPagerAdapter;

    // DB
    public DB_page mDb_page;
    public static Long mNoteId;
    int mEntryPosition;
    static int mStyle;
    
    static SharedPreferences mPref_show_note_attribute;

    Button optionButton;
    Button backButton;

    public AppCompatActivity act;

    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
	    System.out.println("Note / _onCreate");

	    // set current selection
	    mEntryPosition = getIntent().getExtras().getInt("POSITION");
	    NoteUi.setFocus_notePos(mEntryPosition);

	    act = this;

	    MainAct.mMediaBrowserCompat = null;
	} //onCreate end

	// callback of granted permission
//	@Override
//	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
//	{
//		System.out.println("grantResults.length =" + grantResults.length);
//		switch (requestCode)
//		{
//			case Util.PERMISSIONS_REQUEST_STORAGE:
//			{
//				View_note_option option = new View_note_option();
//				option.note_option(act,mNoteId);
//				// If request is cancelled, the result arrays are empty.
//				if ( (grantResults.length > 0) &&
//						( (grantResults[0] == PackageManager.PERMISSION_GRANTED) &&
//					      (grantResults[1] == PackageManager.PERMISSION_GRANTED)       )) {
//					option.doMailNote(act);
//				}
//				option.dlgAddNew.dismiss();
//			}//case
//		}//switch
//	}


	// Add to prevent resizing full screen picture,
	// when popup menu shows up at picture mode
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		System.out.println("Note / _onWindowFocusChanged");
	}

	// key event: 1 from bluetooth device 2 when notification bar dose not shown
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		int newPos;
		System.out.println("Note / _onKeyDown / keyCode = " + keyCode);
		switch (keyCode) {
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS: //88
				if(viewPager.getCurrentItem() == 0)
                    newPos = mPagerAdapter.getCount() - 1;//back to last one
				else
					newPos = NoteUi.getFocus_notePos()-1;

				NoteUi.setFocus_notePos(newPos);
				viewPager.setCurrentItem(newPos);
				return true;

			case KeyEvent.KEYCODE_MEDIA_NEXT: //87
				if(viewPager.getCurrentItem() == (mPagerAdapter.getCount() - 1))
					newPos = 0;
				else
					newPos = NoteUi.getFocus_notePos() + 1;

				NoteUi.setFocus_notePos(newPos);
				viewPager.setCurrentItem(newPos);

				return true;

			case KeyEvent.KEYCODE_MEDIA_PLAY: //126
				return true;

			case KeyEvent.KEYCODE_MEDIA_PAUSE: //127
				return true;

			case KeyEvent.KEYCODE_BACK:
                onBackPressed();
				return true;

			case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
				return true;

			case KeyEvent.KEYCODE_MEDIA_REWIND:
				return true;

			case KeyEvent.KEYCODE_MEDIA_STOP:
				return true;
		}
		return false;
	}



	void setLayoutView()
	{
        System.out.println("Note / _setLayoutView");


		// video view will be reset after _setContentView
		if(Util.isLandscapeOrientation(this))
			setContentView(R.layout.note_view_landscape);
		else
			setContentView(R.layout.note_view_portrait);

		Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(mToolbar);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);

		UilCommon.init();

		// DB
		DB_folder dbFolder = new DB_folder(act,Pref.getPref_focusView_folder_tableId(act));
		mStyle = dbFolder.getPageStyle(TabsHost.getFocus_tabPos(), true);

		mDb_page = new DB_page(act, TabsHost.getCurrentPageTableId());

		// Instantiate a ViewPager and a PagerAdapter.
		viewPager = (ViewPager) findViewById(R.id.tabs_pager);
		mPagerAdapter = new Note_adapter(viewPager,this);
		viewPager.setAdapter(mPagerAdapter);
		viewPager.setCurrentItem(NoteUi.getFocus_notePos());

		// tab style
//		if(TabsHost.mDbFolder != null)
//			TabsHost.mDbFolder.close();

		if(mDb_page != null) {
			mNoteId = mDb_page.getNoteId(NoteUi.getFocus_notePos(), true);
		}

		// Note: if viewPager.getCurrentItem() is not equal to mEntryPosition, _onPageSelected will
		//       be called again after rotation
		viewPager.setOnPageChangeListener(onPageChangeListener);//todo deprecated

		// send note button
		optionButton = (Button) findViewById(R.id.view_option);
		optionButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_more, 0, 0, 0);
		optionButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				View_note_option option = new View_note_option();
				option.note_option(act,mNoteId);
			}
		});

		// back button
		backButton = (Button) findViewById(R.id.view_back);
		backButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_back, 0, 0, 0);
		backButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view) {
				finish();
			}
		});
	}

	// on page change listener
	ViewPager.SimpleOnPageChangeListener onPageChangeListener = new ViewPager.SimpleOnPageChangeListener()
	{
		@Override
		public void onPageSelected(int nextPosition)
		{

			NoteUi.setFocus_notePos(viewPager.getCurrentItem());
			System.out.println("Note / _onPageSelected");
//			System.out.println("    NoteUi.getFocus_notePos() = " + NoteUi.getFocus_notePos());
//			System.out.println("    nextPosition = " + nextPosition);

			mIsViewModeChanged = false;

			mNoteId = mDb_page.getNoteId(nextPosition,true);
			System.out.println("Note / _onPageSelected / mNoteId = " + mNoteId);

            setOutline(act);
		}
	};

	public static int getStyle() {
		return mStyle;
	}

	public void setStyle(int style) {
		mStyle = style;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		System.out.println("Note / _onActivityResult ");
		if (requestCode == MailNotes.EMAIL) {
			Toast.makeText(act, R.string.mail_exit, Toast.LENGTH_SHORT).show();

			long triggerAtMillis = System.currentTimeMillis();
			if( Define.getCodeMode() == Define.DEBUG_MODE)
				triggerAtMillis += 1000 * 10;// test: 10 seconds
			else if(Define.getCodeMode() == Define.RELEASE_MODE)
				triggerAtMillis += 1000 * 60 * 5;// formal: 300 seconds

			new DeleteFileAlarmReceiver(act,triggerAtMillis,MailNotes.mAttachmentFileName);
		}

		// show current item
		if (requestCode == Util.YOUTUBE_LINK_INTENT)
			viewPager.setCurrentItem(viewPager.getCurrentItem());

		// check if there is one note at least in the pager
		if (viewPager.getAdapter().getCount() > 0)
			setOutline(act);
		else
			finish();
	}

    /** Set outline for selected view mode
    *
    *   Determined by view mode: all, picture, text
    *
    *   Controlled factor:
    *   - action bar: hide, show
    *   - full screen: full, not full
    */
	public static void setOutline(AppCompatActivity act)
	{
        // Set full screen or not, and action bar
		Util.setFullScreen_noImmersive(act);
        if(act.getSupportActionBar() != null)
		    act.getSupportActionBar().show();

        // renew pager
        showSelectedView();

		LinearLayout buttonGroup = (LinearLayout) act.findViewById(R.id.view_button_group);
        // button group
		buttonGroup.setVisibility(View.VISIBLE);

        // renew options menu
        act.invalidateOptionsMenu();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    System.out.println("Note / _onConfigurationChanged");

		// dismiss popup menu
		if(NoteUi.popup != null)
		{
			NoteUi.popup.dismiss();
			NoteUi.popup = null;
		}

		NoteUi.cancel_UI_callbacks();

        setLayoutView();

        // Set outline of view mode
        setOutline(act);
	}

	@Override
	protected void onStart() {
		super.onStart();
		System.out.println("Note / _onStart");
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		System.out.println("Note / _onResume");

		setLayoutView();

		isPagerActive = true;

		setOutline(act);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		System.out.println("Note / _onPause");

		isPagerActive = false;


		// to stop YouTube web view running
    	String tagStr = "current"+ viewPager.getCurrentItem()+"webView";
    	CustomWebView webView = (CustomWebView) viewPager.findViewWithTag(tagStr);
    	CustomWebView.pauseWebView(webView);
    	CustomWebView.blankWebView(webView);

		// to stop Link web view running
    	tagStr = "current"+ viewPager.getCurrentItem()+"linkWebView";
    	CustomWebView linkWebView = (CustomWebView) viewPager.findViewWithTag(tagStr);
    	CustomWebView.pauseWebView(linkWebView);
    	CustomWebView.blankWebView(linkWebView);

		NoteUi.cancel_UI_callbacks();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		System.out.println("Note / _onStop");
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		System.out.println("Note / _onDestroy");

	}

	// avoid exception: has leaked window android.widget.ZoomButtonsController
	@Override
	public void finish() {
		System.out.println("Note / _finish");
		if(mPagerHandler != null)
			mPagerHandler.removeCallbacks(mOnBackPressedRun);		
	    
		ViewGroup view = (ViewGroup) getWindow().getDecorView();
//	    view.setBackgroundColor(getResources().getColor(color.background_dark)); // avoid white flash
		view.setBackgroundColor(getResources().getColor(R.color.bar_color)); // avoid white flash
	    view.removeAllViews();

		super.finish();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		System.out.println("Note / _onSaveInstanceState");
	}

	Menu mMenu;
	// On Create Options Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
//		System.out.println("Note / _onCreateOptionsMenu");

		// inflate menu
		getMenuInflater().inflate(R.menu.pager_menu, menu);
		mMenu = menu;

		// menu item: checked status
		// get checked or not
		int isChecked = mDb_page.getNoteMarking(NoteUi.getFocus_notePos(),true);
		if( isChecked == 0)
			menu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_off_holo_dark);
		else
			menu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_on_holo_dark);

	    // menu item: previous
		MenuItem itemPrev = menu.findItem(R.id.ACTION_PREVIOUS);
		itemPrev.setEnabled(viewPager.getCurrentItem() > 0);
		itemPrev.getIcon().setAlpha(viewPager.getCurrentItem() > 0?255:30);

		// menu item: Next or Finish
		MenuItem itemNext = menu.findItem(R.id.ACTION_NEXT);
		itemNext.setTitle((viewPager.getCurrentItem() == mPagerAdapter.getCount() - 1)	?
									R.string.view_note_slide_action_finish :
									R.string.view_note_slide_action_next                  );

        // set Disable and Gray for Last item
		boolean isLastOne = (viewPager.getCurrentItem() == (mPagerAdapter.getCount() - 1));
        if(isLastOne)
        	itemNext.setEnabled(false);

        itemNext.getIcon().setAlpha(isLastOne?30:255);

        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	// called after _onCreateOptionsMenu
        return true;
    }  
    
    // for menu buttons
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
	            finish();
                return true;

			case R.id.VIEW_NOTE_CHECK:
				int markingNow = PageAdapter_recycler.toggleNoteMarking(this,NoteUi.getFocus_notePos());

				// update marking
				if(markingNow == 1)
					mMenu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_on_holo_dark);
				else
					mMenu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_off_holo_dark);

				return true;

            case R.id.ACTION_PREVIOUS:
                // Go to the previous step in the wizard. If there is no previous step,
                // setCurrentItem will do nothing.
            	NoteUi.setFocus_notePos(NoteUi.getFocus_notePos()-1);
            	viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
                return true;

            case R.id.ACTION_NEXT:
                // Advance to the next step in the wizard. If there is no next step, setCurrentItem
                // will do nothing.
				NoteUi.setFocus_notePos(NoteUi.getFocus_notePos()+1);
            	viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

//    //
//    // Open link by native YouTube
//    //
//    // Due to "AdWords or copyright" server limitation, for some URI,
//    // "video is not available" message could show up.
//    // At this case, one solution is to switch current mobile website to desktop website by browser setting.
//    // So, base on URI key words to decide "YouTube App" or "browser" launch.
//    public void openLink_YouTube(String linkUri)
//    {
//        // by YouTube App
//        if(linkUri.contains("youtu.be"))
//        {
//            // stop video if playing
//            stopAV();
//
//            String id = Util.getYoutubeId(linkUri);
//            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://" + id));
//            act.startActivity(intent);
//        }
//        // by Chrome browser
//        else if(linkUri.contains("youtube.com"))
//        {
//            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(linkUri));
//            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            i.setPackage("com.android.chrome");
//
//            try
//            {
//                act.startActivity(i);
//            }
//            catch (ActivityNotFoundException e)
//            {
//                // Chrome is probably not installed
//                // Try with the default browser
//                i.setPackage(null);
//                act.startActivity(i);
//            }
//        }
//    }

    // on back pressed
    @Override
    public void onBackPressed() {
		System.out.println("Note / _onBackPressed");
    	// web view can go back
    	String tagStr = "current"+ viewPager.getCurrentItem()+"linkWebView";
    	CustomWebView linkWebView = (CustomWebView) viewPager.findViewWithTag(tagStr);
        if (linkWebView.canGoBack()) {
        	linkWebView.goBack();
        } else {
    		System.out.println("Note / _onBackPressed / view all mode");
        	finish();
    	}
    }
    
    static Handler mPagerHandler;
	Runnable mOnBackPressedRun = new Runnable()
	{   @Override
		public void run()
		{
            String tagStr = "current"+ NoteUi.getFocus_notePos() +"pictureView";
            ViewGroup pictureGroup = (ViewGroup) viewPager.findViewWithTag(tagStr);
            System.out.println("Note / _showPictureViewUI / tagStr = " + tagStr);

            Button picView_back_button;
            if(pictureGroup != null)
            {
                picView_back_button = (Button) (pictureGroup.findViewById(R.id.image_view_back));
                picView_back_button.performClick();
            }

			if(Note_adapter.mIntentView != null)
				Note_adapter.mIntentView = null;
		}
	};
    
    // get current picture string
    public String getCurrentPictureString()
    {
		return mDb_page.getNotePictureUri(NoteUi.getFocus_notePos(),true);
    }

    // Mark current selected
    void markCurrentSelected(MenuItem subItem, String str)
    {
        if(mPref_show_note_attribute.getString("KEY_PAGER_VIEW_MODE", "ALL")
                .equalsIgnoreCase(str))
            subItem.setIcon(R.drawable.btn_radio_on_holo_dark);
        else
            subItem.setIcon(R.drawable.btn_radio_off_holo_dark);
    }

    // Show selected view
    static void showSelectedView()
    {
   		mIsViewModeChanged = false;

   		Note_adapter.mLastPosition = -1;

    	if(mPagerAdapter != null)
    		mPagerAdapter.notifyDataSetChanged(); // will call Note_adapter / _setPrimaryItem
    }
    
    public static boolean mIsViewModeChanged;
    
	static NoteUi picUI_touch;
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int maskedAction = event.getActionMasked();
        switch (maskedAction) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
    			 System.out.println("Note / _dispatchTouchEvent / MotionEvent.ACTION_UP / viewPager.getCurrentItem() =" + viewPager.getCurrentItem());
				 //1st touch to turn on UI
				 if(picUI_touch == null) {
				 	picUI_touch = new NoteUi(act, viewPager, viewPager.getCurrentItem());
				 	picUI_touch.tempShow_picViewUI(5000,getCurrentPictureString());
				 }
				 //2nd touch to turn off UI
				 else
					 setTransientPicViewUI();

				 //1st touch to turn off UI (primary)
				 if(Note_adapter.picUI_primary != null)
					 setTransientPicViewUI();
    	  	  	 break;

	        case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
	        case MotionEvent.ACTION_CANCEL: 
	        	 break;
        }

        return super.dispatchTouchEvent(event);
    }

	/**
	 * Set delay for transient picture view UI
	 *
	 */
    void setTransientPicViewUI()
    {
        NoteUi.cancel_UI_callbacks();
        picUI_touch = new NoteUi(act, viewPager, viewPager.getCurrentItem());

	    // for image
        picUI_touch.tempShow_picViewUI(111,getCurrentPictureString());
    }

}