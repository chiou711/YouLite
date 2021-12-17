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

package com.cw.youlite.main;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.cw.youlite.R;
import com.cw.youlite.config.About;
import com.cw.youlite.config.Config;
import com.cw.youlite.data.FetchService_category;
import com.cw.youlite.db.DB_folder;
import com.cw.youlite.db.DB_page;
import com.cw.youlite.db.RenewDB;
import com.cw.youlite.drawer.Drawer;
import com.cw.youlite.folder.Folder;
import com.cw.youlite.folder.FolderUi;
import com.cw.youlite.note_add.Add_note_option;
import com.cw.youlite.operation.delete.DeleteFolders;
import com.cw.youlite.operation.delete.DeletePages;
import com.cw.youlite.operation.gdrive.ExportGDriveAct;
import com.cw.youlite.operation.gdrive.ImportGDriveAct;
import com.cw.youlite.operation.import_export.Export_toSDCardAllJsonFragment;
import com.cw.youlite.operation.import_export.Export_toSDCardJsonFragment;
import com.cw.youlite.operation.import_export.Import_filesListJson;
import com.cw.youlite.operation.import_export.Import_webJsonAct;
import com.cw.youlite.operation.mail.Mail_filesListJson;
import com.cw.youlite.operation.mail.Mail_fileViewJson;
import com.cw.youlite.page.Checked_notes_option;
import com.cw.youlite.page.PageUi;
import com.cw.youlite.page.Page_recycler;
import com.cw.youlite.tabs.TabsHost;
import com.cw.youlite.util.DeleteFileAlarmReceiver;
import com.cw.youlite.db.DB_drawer;
import com.cw.youlite.util.Dialog_EULA;
import com.cw.youlite.operation.slideshow.SlideshowInfo;
import com.cw.youlite.util.image.UtilImage;
import com.cw.youlite.define.Define;
import com.cw.youlite.operation.mail.MailNotes;
import com.cw.youlite.util.OnBackPressedListener;
import com.cw.youlite.util.Util;
import com.cw.youlite.util.preferences.Pref;
import com.mobeta.android.dslv.DragSortListView;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.os.StrictMode;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener;
import androidx.fragment.app.FragmentTransaction;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.O;

public class MainAct extends AppCompatActivity implements OnBackStackChangedListener
{
    public static CharSequence mFolderTitle;
    public static CharSequence mAppTitle;
    public Context mContext;
    public Config mConfigFragment;
    public About mAboutFragment;
    public static Menu mMenu;
    public static List<String> mFolderTitles;
    public static AppCompatActivity mAct;//TODO static issue
    public FragmentManager mFragmentManager;
    public static FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener;
    public static int mLastOkTabId = 1;
    public static SharedPreferences mPref_show_note_attribute;
    OnBackPressedListener onBackPressedListener;
    public Drawer drawer;
    public static Folder mFolder;
    public static MainUi mMainUi;
    public static Toolbar mToolbar;

    public static MediaBrowserCompat mMediaBrowserCompat;
    public static MediaControllerCompat mMediaControllerCompat;
    public static int mCurrentState;
    public final static int STATE_PAUSED = 0;
    public final static int STATE_PLAYING = 1;
    public boolean bEULA_accepted;
    public static boolean isEdited_YouTube_link;
    public static boolean isEdited_Web_link;
    public static int edit_position;

	// Main Act onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        /**
         * Set APP build mode
         * Note:
         *  1. for AdMob: it works after Google Play store release
         *  2. for assets mode: need to enable build.gradle assets.srcDirs = ['preferred/assets/']
         */
        Define.setAppBuildMode();

        // Release mode: no debug message
        if (Define.CODE_MODE == Define.RELEASE_MODE) {
            OutputStream nullDev = new OutputStream() {
                public void close() {}
                public void flush() {}
                public void write(byte[] b) {}
                public void write(byte[] b, int off, int len) {}
                public void write(int b) {}
            };
            System.setOut(new PrintStream(nullDev));
        }

        // filter unimportant log
        System.setOut(new FilteringPrintStream(System.out));

        System.out.println("================start application ==================");
        System.out.println("MainAct / _onCreate");

        mAct = this;
        mAppTitle = getTitle();
        mMainUi = new MainUi();

        // File provider implementation is needed after Android version 24
        // if not, will encounter android.os.FileUriExposedException
        // cf. https://stackoverflow.com/questions/38200282/android-os-fileuriexposedexception-file-storage-emulated-0-test-txt-exposed

        // add the following to disable this requirement
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                // method 1
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);

                // method 2
//                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
//                StrictMode.setVmPolicy(builder.build());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Show Api version
        if (Define.CODE_MODE == Define.DEBUG_MODE)
            Toast.makeText(this, mAppTitle + " " + "API_" + Build.VERSION.SDK_INT, Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, mAppTitle, Toast.LENGTH_SHORT).show();

        UtilImage.getDefaultScaleInPercent(MainAct.this);

        // EULA
        Dialog_EULA dialog_EULA = new Dialog_EULA(this);
        bEULA_accepted = dialog_EULA.isEulaAlreadyAccepted();

        // Show dialog of EULA
        if (!bEULA_accepted)
        {
            // Ok button listener
            dialog_EULA.clickListener_Ok = (DialogInterface dialog, int i) -> {
                dialog_EULA.applyPreference();
                new RenewDB(this);
            };

            // No button listener
            dialog_EULA.clickListener_No = (DialogInterface dialog, int which) -> {
                // Close the activity as they have declined
                // the EULA
                dialog.dismiss();
                mAct.finish();
            };

            dialog_EULA.show();
        }
        else {
            if(Pref.getPref_DB_ready(this))
                doCreate();
        }

        edit_position = 0;
    }

    // filter unimportant log
    public class FilteringPrintStream extends PrintStream {

        public FilteringPrintStream(OutputStream out)  {
            super(out);
        }

        @Override
        public void println(String x) {
            if (x==null || !x.contains("isSBSettingEnabled")) {
                super.println(x);
            }
        }
    }

    // Do major create operation
    void doCreate()
    {
        System.out.println("MainAct / _doCreate");

        mFolderTitles = new ArrayList<>();

        // Add link from other App
        isAdded_onNewIntent = false; // for judging YouLite is running or not when doing YouTube Share

        MainAct.isEdited_Web_link = false;
        MainAct.isEdited_YouTube_link = false;

        String intentExtras;
        intentExtras = mMainUi.addNote_IntentLink(getIntent(), mAct, isAdded_onNewIntent);

        if(intentExtras == null) {
            // check DB
            final boolean ENABLE_DB_CHECK = false;
            if (ENABLE_DB_CHECK) {
                // list all folder tables
                FolderUi.listAllFolderTables(mAct);

                // recover focus
                DB_folder.setFocusFolder_tableId(Pref.getPref_focusView_folder_tableId(this));
                DB_page.setFocusPage_tableId(Pref.getPref_focusView_page_tableId(this));
            }

            mContext = getBaseContext();

            // add on back stack changed listener
            mFragmentManager = getSupportFragmentManager();
            mOnBackStackChangedListener = this;
            mFragmentManager.addOnBackStackChangedListener(mOnBackStackChangedListener);

            if (bEULA_accepted)
                configLayoutView(); //createAssetsFile inside
        }
    }


    Intent intentReceive;
    //The BroadcastReceiver that listens for bluetooth broadcasts
    private BroadcastReceiver bluetooth_device_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("MainAct / _BroadcastReceiver / onReceive");
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //Device is now connected
                Toast.makeText(getApplicationContext(), "ACTION_ACL_CONNECTED: device is " + device, Toast.LENGTH_LONG).show();
            }

            intentReceive = intent;
            KeyEvent keyEvent = (KeyEvent) intentReceive.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if(keyEvent != null)
                onKeyDown( keyEvent.getKeyCode(),keyEvent);
        }
    };


    // key event: 1 from bluetooth device 2 when notification bar dose not shown
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        System.out.println("MainAct / _onKeyDown / keyCode = " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS: //88
                return true;

            case KeyEvent.KEYCODE_MEDIA_NEXT: //87
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

    private boolean isStorageRequestedImport = false;
    private boolean isStorageRequested = false;

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
    }

    /**
     * initialize action bar
     */
    void initActionBar()
    {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
            mToolbar.setNavigationIcon(R.drawable.ic_drawer);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Drawer.drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }
    }

    // set action bar for fragment
    void initActionBar_home()
    {
        drawer.drawerToggle.setDrawerIndicatorEnabled(false);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
            getSupportActionBar().setDisplayShowHomeEnabled(false);//false: no launcher icon
        }

        mToolbar.setNavigationIcon(R.drawable.ic_menu_back);
        mToolbar.getChildAt(1).setContentDescription(getResources().getString(R.string.btn_back));
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("MainAct / _initActionBar_home / click to popBackStack");

                // check if DB is empty
                DB_drawer db_drawer = new DB_drawer(mAct);
                int focusFolder_tableId = Pref.getPref_focusView_folder_tableId(mAct);
                DB_folder db_folder = new DB_folder(mAct,focusFolder_tableId);
                if((db_drawer.getFoldersCount(true) == 0) ||
                   (db_folder.getPagesCount(true) == 0)      )
                {
                    finish();
                    Intent intent  = new Intent(mAct,MainAct.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
                else
                    getSupportFragmentManager().popBackStack();
            }
        });

    }


    /*********************************************************************************
     *
     *                                      Life cycle
     *
     *********************************************************************************/

    public static boolean isAdded_onNewIntent;
    // if one YouLite Intent is already running, call it again in YouTube or Browser will run into this
    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        System.out.println("MainAct / _onNewIntent ");

        if(isEdited_Web_link || isEdited_YouTube_link) {
            System.out.println("MainAct / _onNewIntent / call Edited link");
            mMainUi.editNote_IntentLink(intent, mAct, isAdded_onNewIntent,edit_position);
        } else {
            if(Build.VERSION.SDK_INT >= O)//API26
                isAdded_onNewIntent = true; // fix 2 times _onNewIntent on API26

            // Add link from new intent
            mMainUi.addNote_IntentLink(intent, mAct, isAdded_onNewIntent);//todo How to judge Add or Edit?
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        System.out.println("MainAct / _onPause");
    }

    private FetchServiceResponseReceiver responseReceiver;
    LocalBroadcastManager localBroadcastMgr;

    @Override
    protected void onResume() {
        super.onResume();
    	System.out.println("MainAct / _onResume");

    	mAct = this;

        // Sync the toggle state after onRestoreInstanceState has occurred.
        if(bEULA_accepted) {
        	if(drawer != null)
                drawer.drawerToggle.syncState();

            //   get folder table id by preference
            DB_drawer dbDrawer = new DB_drawer(mAct);
            int foldersCnt = dbDrawer.getFoldersCount(true);
            int focus_folder_tableId =  Pref.getPref_focusView_folder_tableId(mAct);

            // select focus folder view by preference
            for (int pos=0;pos< foldersCnt;pos++)
            {
                if(focus_folder_tableId == dbDrawer.getFolderTableId(pos,true))
                    FolderUi.setFocus_folderPos(pos);
            }
        }

        // receiver for fetch category service
        IntentFilter statusIntentFilter = new IntentFilter(FetchService_category.Constants.BROADCAST_ACTION);
        responseReceiver = new FetchServiceResponseReceiver();

        // Registers the FetchServiceResponseReceiver and its intent filters
        localBroadcastMgr = LocalBroadcastManager.getInstance(this);
        localBroadcastMgr.registerReceiver(responseReceiver, statusIntentFilter );
    }

    // Broadcast receiver for receiving status updates from the IntentService
    class FetchServiceResponseReceiver extends BroadcastReceiver {
        // Called when the BroadcastReceiver gets an Intent it's registered to receive
        public void onReceive(Context context, Intent intent) {
            /*
             * You get notified here when your IntentService is done
             * obtaining data form the server!
             */
            String statusStr = intent.getExtras().getString(FetchService_category.Constants.EXTENDED_DATA_STATUS);
            System.out.println("MainFragment / _FetchServiceResponseReceiver / _onReceive / statusStr = " + statusStr);

            if((statusStr != null) && statusStr.equalsIgnoreCase("FetchCategoryServiceIsDone"))
            {
                if (context != null) {
                    // unregister receiver
                    localBroadcastMgr.unregisterReceiver(responseReceiver);
                    responseReceiver = null;

                    Pref.setPref_DB_ready(MainAct.this,true);

                    System.out.println("MainFragment / _FetchServiceResponseReceiver / will start new main activity");
                    Intent new_intent = new Intent(context, MainAct.class);
                    new_intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK);
                    new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);

                    // reset focus view to default
                    Pref.setPref_focusView_folder_tableId(MainAct.this, 1);
                    FolderUi.setFocus_folderPos(0);
                    Pref.setPref_focusView_page_tableId(MainAct.this, 1);

                    context.startActivity(new_intent);
                }
            }
        }
    }


    @Override
    protected void onResumeFragments() {
        System.out.println("MainAct / _onResumeFragments ");
        super.onResumeFragments();

        if(isStorageRequested ||
           isStorageRequestedImport      )
        {
            //hide the menu
            mMenu.setGroupVisible(R.id.group_notes, false);
            mMenu.setGroupVisible(R.id.group_pages_and_more, false);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            if(isStorageRequested)
            {
                DB_folder dB_folder = new DB_folder(this, Pref.getPref_focusView_folder_tableId(this));
                if(dB_folder.getPagesCount(true)>0)
                {
                    Mail_filesListJson mailFragment = new Mail_filesListJson();

                    mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                    mFragmentTransaction.replace(R.id.content_frame, mailFragment,"mail").addToBackStack(null).commit();
                }
                else
                {
                    Toast.makeText(this, R.string.no_page_yet, Toast.LENGTH_SHORT).show();
                }

                isStorageRequested = false;
            }

            if(isStorageRequestedImport)
            {
                Import_filesListJson importFragmentJson = new Import_filesListJson();
                transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                transaction.replace(R.id.content_frame, importFragmentJson, "import").addToBackStack(null).commit();
                isStorageRequestedImport = false;
            }

            if (FolderUi.mHandler != null)
                FolderUi.mHandler.removeCallbacks(FolderUi.mTabsHostRun);
        }
		// fix: home button failed after power off/on in Config fragment
        else {

            if (bEULA_accepted) {
                if(mFragmentManager != null)
                    mFragmentManager.popBackStack();

                if (!mAct.isDestroyed()) {
                    System.out.println("MainAct / _onResumeFragments / mAct is not Destroyed()");
                    openFolder();
                } else
                    System.out.println("MainAct / _onResumeFragments / mAct is Destroyed()");
            }
        }
    }

    // open folder
    public static void openFolder()
    {
        System.out.println("MainAct / _openFolder");
        DB_drawer dB_drawer = new DB_drawer(mAct);
        int folders_count = dB_drawer.getFoldersCount(true);

        if (folders_count > 0) {
            int pref_focus_table_id = Pref.getPref_focusView_folder_tableId(MainAct.mAct);
            for(int folder_pos=0; folder_pos<folders_count; folder_pos++)
            {
                if(dB_drawer.getFolderTableId(folder_pos,true) == pref_focus_table_id) {
                    // select folder
                    FolderUi.selectFolder(mAct, folder_pos);

                    // set focus folder position
                    FolderUi.setFocus_folderPos(folder_pos);
                }
            }
            // set focus table Id
            DB_folder.setFocusFolder_tableId(pref_focus_table_id);

            if (mAct.getSupportActionBar() != null)
                mAct.getSupportActionBar().setTitle(mFolderTitle);
        }
    }


    @Override
    protected void onDestroy()
    {
        System.out.println("MainAct / _onDestroy");
        // unregister receiver
        if(localBroadcastMgr != null) {
            localBroadcastMgr.unregisterReceiver(responseReceiver);
            responseReceiver = null;
        }

        super.onDestroy();
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        System.out.println("MainAct / _onConfigurationChanged");

        configLayoutView();

        // Pass any configuration change to the drawer toggles
        drawer.drawerToggle.onConfigurationChanged(newConfig);

		drawer.drawerToggle.syncState();

        FolderUi.startTabsHostRun();
    }


    /**
     *  on Back button pressed
     */
    @Override
    public void onBackPressed()
    {
        System.out.println("MainAct / _onBackPressed");
        doBackKeyEvent();
    }

    void doBackKeyEvent()
    {
        if (onBackPressedListener != null)
        {
            DB_drawer dbDrawer = new DB_drawer(this);
            int foldersCnt = dbDrawer.getFoldersCount(true);

            if(foldersCnt == 0)
            {
                finish();
                Intent intent  = new Intent(this,MainAct.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
            else {
                onBackPressedListener.doBack();
            }
        }
        else
        {
            if((drawer != null) && drawer.isDrawerOpen())
                drawer.closeDrawer();
            else
                super.onBackPressed();
        }

    }


    @Override
    public void onBackStackChanged() {
        int backStackEntryCount = mFragmentManager.getBackStackEntryCount();
        System.out.println("MainAct / _onBackStackChanged / backStackEntryCount = " + backStackEntryCount);

        if(backStackEntryCount == 1) // fragment
        {
            System.out.println("MainAct / _onBackStackChanged / fragment");
            initActionBar_home();
        }
        else if(backStackEntryCount == 0) // init
        {
            System.out.println("MainAct / _onBackStackChanged / init");
            onBackPressedListener = null;

            if(mFolder.adapter!=null)
                mFolder.adapter.notifyDataSetChanged();

            configLayoutView();

            drawer.drawerToggle.syncState(); // make sure toggle icon state is correct
        }
    }

    public void setOnBackPressedListener(OnBackPressedListener onBackPressedListener) {
        this.onBackPressedListener = onBackPressedListener;
    }

    /**
     * on Activity Result
     */
    AlertDialog.Builder builder;
    AlertDialog alertDlg;
    Handler handler;
    int count;
    String countStr;
    String nextLink;
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode,resultCode,data);
        System.out.println("MainAct / _onActivityResult ");
        String stringFileName[] = null;

        // mail
        if((requestCode== MailNotes.EMAIL) ||
           (requestCode== Mail_fileViewJson.EMAIL_JSON)) {
            if (requestCode == MailNotes.EMAIL)
                stringFileName = MailNotes.mAttachmentFileName;
            else if (requestCode == Mail_fileViewJson.EMAIL_JSON)
                stringFileName = Mail_fileViewJson.mAttachmentFileName;

            Toast.makeText(mAct, R.string.mail_exit, Toast.LENGTH_SHORT).show();
	        System.out.println("MainAct / _onActivityResult / stringFileName = " + stringFileName);
            // note: result code is always 0 (cancel), so it is not used
            new DeleteFileAlarmReceiver(mAct,
                    System.currentTimeMillis() + 1000 * 60 * 5, // formal: 300 seconds
//					System.currentTimeMillis() + 1000 * 10, // test: 10 seconds
                    stringFileName);

            recreate();
        }

        // YouTube
        if(requestCode == Util.YOUTUBE_LINK_INTENT)
        {
            // preference of delay
            SharedPreferences pref_delay = getSharedPreferences("youtube_launch_delay", 0);
            count = Integer.valueOf(pref_delay.getString("KEY_YOUTUBE_LAUNCH_DELAY","5"));

            builder = new AlertDialog.Builder(this);

            do
            {
                TabsHost.getCurrentPage().mCurrPlayPosition++;
                if(Page_recycler.mCurrPlayPosition >= TabsHost.getCurrentPage().getNotesCountInPage(mAct))
                    TabsHost.getCurrentPage().mCurrPlayPosition = 0; //back to first index

                nextLink = mMainUi.getYouTubeLink(this,TabsHost.getCurrentPage().mCurrPlayPosition);
            }
            while (!Util.isYouTubeLink(nextLink));

            countStr = getResources().getString(R.string.message_continue_or_stop_YouTube_message);
            countStr = countStr.replaceFirst("[0-9]",String.valueOf(count));

            builder.setTitle(R.string.message_continue_or_stop_YouTube_title)
                    .setMessage(countStr)
                    .setNegativeButton(R.string.btn_Stop, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog1, int which1)
                        {
                            alertDlg.dismiss();
                            mMainUi.cancelYouTubeHandler(handler,runCountDown);
                        }
                    })
                    .setPositiveButton(R.string.btn_Continue, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog1, int which1) {
                            alertDlg.dismiss();
                            mMainUi.cancelYouTubeHandler(handler,runCountDown);
                            mMainUi.launchNextYouTubeIntent(mAct,handler,runCountDown);
                        }
                    });

            alertDlg = builder.create();

            // set listener for selection
            alertDlg.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dlgInterface) {
                    handler = new Handler();
                    handler.postDelayed(runCountDown,1000);
                }
            });
            alertDlg.show();
        }

        // make sure main activity is still executing
        if( (requestCode == Util.ADD_NEW_YOUTUBE_LINK_INTENT) ||
            (requestCode == Util.ADD_NEW_LINK_INTENT)    )
        {
            if(Build.VERSION.SDK_INT >= O)//API26
                isAdded_onNewIntent = false;
        }
    }

    /**
     * runnable for counting down
     */
    Runnable runCountDown = new Runnable() {
        public void run() {
            // show count down
            TextView messageView = (TextView) alertDlg.findViewById(android.R.id.message);
            count--;
            countStr = getResources().getString(R.string.message_continue_or_stop_YouTube_message);
            countStr = countStr.replaceFirst("[0-9]",String.valueOf(count));
            messageView.setText( countStr);

            if(count>0)
                handler.postDelayed(runCountDown,1000);
            else
            {
                // launch next intent
                alertDlg.dismiss();
                mMainUi.cancelYouTubeHandler(handler,runCountDown);
                System.out.println("MainAct / _runCountDown /alertDlg  dismiss ");
                mMainUi.launchNextYouTubeIntent(mAct,handler,runCountDown);
            }
        }
    };



    /***********************************************************************************
     *
     *                                          Menu
     *
     ***********************************************************************************/

    /****************************************************
     *  On Prepare Option menu :
     *  Called whenever we call invalidateOptionsMenu()
     ****************************************************/
    @Override
    public boolean onPrepareOptionsMenu(android.view.Menu menu) {
        System.out.println("MainAct / _onPrepareOptionsMenu");

        if((drawer == null) || (drawer.drawerLayout == null) || (!bEULA_accepted))
            return false;

        DB_drawer db_drawer = new DB_drawer(this);
        int foldersCnt = db_drawer.getFoldersCount(true);

        /**
         * Folder group
         */
        // If the navigation drawer is open, hide action items related to the content view
        if(drawer.isDrawerOpen())
        {
            // for landscape: the layout file contains folder menu
            if(Util.isLandscapeOrientation(mAct)) {
                mMenu.setGroupVisible(R.id.group_folders, true);
                // set icon for folder draggable: landscape
                if(MainAct.mPref_show_note_attribute != null)
                {
                    if (MainAct.mPref_show_note_attribute.getString("KEY_ENABLE_FOLDER_DRAGGABLE", "no")
                            .equalsIgnoreCase("yes"))
                        mMenu.findItem(R.id.ENABLE_FOLDER_DRAG_AND_DROP).setIcon(R.drawable.btn_check_on_holo_light);
                    else
                        mMenu.findItem(R.id.ENABLE_FOLDER_DRAG_AND_DROP).setIcon(R.drawable.btn_check_off_holo_light);
                }
            }

            mMenu.setGroupVisible(R.id.group_pages_and_more, false);
            mMenu.setGroupVisible(R.id.group_notes, false);
        }
        else if(!drawer.isDrawerOpen())
        {
            if(Util.isLandscapeOrientation(mAct))
                mMenu.setGroupVisible(R.id.group_folders, false);

            /**
             * Page group and more
             */
//            mMenu.setGroupVisible(R.id.group_pages_and_more, foldersCnt >0);//todo temp need overall check

            // EXPORT TO SD CARD ALL JSON
            mMenu.findItem(R.id.EXPORT_TO_SD_CARD_ALL_JSON).setVisible(foldersCnt >0);

            // EXPORT TO Google Drive ALL JSON
            mMenu.findItem(R.id.EXPORT_TO_GDrive_ALL_JSON).setVisible(foldersCnt >0);

            if(foldersCnt>0)
            {
                getSupportActionBar().setTitle(mFolderTitle);

                // pages count
                int pgsCnt = FolderUi.getFolder_pagesCount(this,FolderUi.getFocus_folderPos());

                // notes count
                int notesCnt = 0;
                int pageTableId = Pref.getPref_focusView_page_tableId(this);

                if(pageTableId > 0) {
                    DB_page dB_page = new DB_page(this, pageTableId);
                    if (dB_page != null) {
                        try {
                            notesCnt = dB_page.getNotesCount(true);
                        } catch (Exception e) {
                            System.out.println("MainAct / _onPrepareOptionsMenu / dB_page.getNotesCount() error");
                            notesCnt = 0;
                        }
                    }
                }

                // change page color
                mMenu.findItem(R.id.CHANGE_PAGE_COLOR).setVisible(pgsCnt >0);

                // pages order
                mMenu.findItem(R.id.SHIFT_PAGE).setVisible(pgsCnt >1);

                // delete pages
                mMenu.findItem(R.id.DELETE_PAGES).setVisible(pgsCnt >0);

                // note operation
                mMenu.findItem(R.id.note_operation).setVisible( (pgsCnt >0) && (notesCnt>0) );


	            // EXPORT TO SD CARD JSON
	            mMenu.findItem(R.id.EXPORT_TO_SD_CARD_JSON).setVisible(pgsCnt >0);


                /**
                 *  Note group
                 */
                // group of notes
                mMenu.setGroupVisible(R.id.group_notes, pgsCnt > 0);

                // play
                //mMenu.findItem(R.id.PLAY).setVisible( (pgsCnt >0) && (notesCnt>0) );

                // HANDLE CHECKED NOTES
                mMenu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible( (pgsCnt >0) && (notesCnt>0) );
            }
            else if(foldersCnt==0)
            {
                /**
                 *  Note group
                 */
                mMenu.setGroupVisible(R.id.group_notes, false);

                // disable page operation sub menu entry
                mMenu.findItem(R.id.page_operation).setVisible(false);

                // disable note operation sub menu entry
                mMenu.findItem(R.id.note_operation).setVisible(false);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    /*************************
     * onCreate Options Menu
     *
     *************************/
    MenuItem playOrStopMusicButton;
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu)
    {
//		System.out.println("MainAct / _onCreateOptionsMenu");
        mMenu = menu;

        // inflate menu
        getMenuInflater().inflate(R.menu.main_menu, menu);

        // enable drag note
        mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);
        if(mPref_show_note_attribute.getString("KEY_ENABLE_DRAGGABLE", "yes").equalsIgnoreCase("yes"))
            menu.findItem(R.id.ENABLE_NOTE_DRAG_AND_DROP)
                    .setIcon(R.drawable.btn_check_on_holo_light)
                    .setTitle(R.string.drag_note) ;
        else
            menu.findItem(R.id.ENABLE_NOTE_DRAG_AND_DROP)
                    .setIcon(R.drawable.btn_check_off_holo_light)
                    .setTitle(R.string.drag_note) ;

        // enable show body
        mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);
        if(mPref_show_note_attribute.getString("KEY_SHOW_BODY", "yes").equalsIgnoreCase("yes"))
            menu.findItem(R.id.SHOW_BODY)
                    .setIcon(R.drawable.btn_check_on_holo_light)
                    .setTitle(R.string.preview_note_body) ;
        else
            menu.findItem(R.id.SHOW_BODY)
                .setIcon(R.drawable.btn_check_off_holo_light)
                .setTitle(R.string.preview_note_body) ;

        return super.onCreateOptionsMenu(menu);
    }

    /******************************
     * on options item selected
     *
     ******************************/
    public static SlideshowInfo slideshowInfo;
    public static FragmentTransaction mFragmentTransaction;

    static int mMenuUiState;

    public static void setMenuUiState(int mMenuState) {
        mMenuUiState = mMenuState;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        //System.out.println("MainAct / _onOptionsItemSelected");
        setMenuUiState(item.getItemId());
        DB_drawer dB_drawer = new DB_drawer(this);
        DB_folder dB_folder = new DB_folder(this, Pref.getPref_focusView_folder_tableId(this));

        mFragmentManager = getSupportFragmentManager();
        mFragmentTransaction = getSupportFragmentManager().beginTransaction();

        // Go back: check if Configure fragment now
        if( (item.getItemId() == android.R.id.home ))
        {

            System.out.println("MainAct / _onOptionsItemSelected / Home key of Config is pressed / mFragmentManager.getBackStackEntryCount() =" +
            mFragmentManager.getBackStackEntryCount());

            if(mFragmentManager.getBackStackEntryCount() > 0 )
            {
                int foldersCnt = dB_drawer.getFoldersCount(true);
                System.out.println("MainAct / _onOptionsItemSelected / Home key of Config is pressed / foldersCnt = " + foldersCnt);

                if(foldersCnt == 0)
                {
                    finish();
                    Intent intent  = new Intent(this,MainAct.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
                else
                {
                    mFragmentManager.popBackStack();

                    initActionBar();

                    mFolderTitle = dB_drawer.getFolderTitle(FolderUi.getFocus_folderPos(),true);
                    setTitle(mFolderTitle);
                    drawer.closeDrawer();
                }
                return true;
            }
        }


        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (drawer.drawerToggle.onOptionsItemSelected(item))
        {
            System.out.println("MainAct / _onOptionsItemSelected / drawerToggle.onOptionsItemSelected(item) == true ");
            return true;
        }

        switch (item.getItemId())
        {
            // for landscape layout
            case MenuId.ADD_NEW_FOLDER:
                FolderUi.renewFirstAndLast_folderId();
                FolderUi.addNewFolder(this, FolderUi.mLastExist_folderTableId +1, mFolder.getAdapter());
                return true;

            // for landscape layout
            case MenuId.ENABLE_FOLDER_DRAG_AND_DROP:
                if(MainAct.mPref_show_note_attribute.getString("KEY_ENABLE_FOLDER_DRAGGABLE", "no")
                        .equalsIgnoreCase("yes"))
                {
                    mPref_show_note_attribute.edit().putString("KEY_ENABLE_FOLDER_DRAGGABLE","no")
                            .apply();
                    DragSortListView listView = (DragSortListView) this.findViewById(R.id.drawer_listview);
                    listView.setDragEnabled(false);
                    Toast.makeText(this,getResources().getString(R.string.drag_folder)+
                                    ": " +
                                    getResources().getString(R.string.set_disable),
                            Toast.LENGTH_SHORT).show();
                }
                else
                {
                    mPref_show_note_attribute.edit().putString("KEY_ENABLE_FOLDER_DRAGGABLE","yes")
                            .apply();
                    DragSortListView listView = (DragSortListView) this.findViewById(R.id.drawer_listview);
                    listView.setDragEnabled(true);
                    Toast.makeText(this,getResources().getString(R.string.drag_folder) +
                                    ": " +
                                    getResources().getString(R.string.set_enable),
                            Toast.LENGTH_SHORT).show();
                }
                mFolder.getAdapter().notifyDataSetChanged();
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                return true;

            // for landscape layout
            case MenuId.DELETE_FOLDERS:
                mMenu.setGroupVisible(R.id.group_folders, false);

                if(dB_drawer.getFoldersCount(true)>0)
                {
                    drawer.closeDrawer();
                    mMenu.setGroupVisible(R.id.group_notes, false); //hide the menu
                    DeleteFolders delFoldersFragment = new DeleteFolders();
                    mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                    mFragmentTransaction.replace(R.id.content_frame, delFoldersFragment).addToBackStack("delete_folders").commit();
                }
                else
                {
                    Toast.makeText(this, R.string.config_export_none_toast, Toast.LENGTH_SHORT).show();
                }
                return true;

            case MenuId.ADD_NEW_NOTE:
                if(Build.VERSION.SDK_INT >= M)//api23
                {
                    // check permission
                    int permissionWriteExtStorage = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    if(permissionWriteExtStorage == PackageManager.PERMISSION_GRANTED)
                        Add_note_option.createSelection(this,true);
                    else
                        Add_note_option.createSelection(this,false);
                }
                else
                    Add_note_option.createSelection(this,true);
                return true;

            case MenuId.CHECKED_OPERATION:
                Checked_notes_option op = new Checked_notes_option(this);
                op.open_option_grid(this);
                return true;

            case MenuId.ADD_NEW_PAGE:

                // get current Max page table Id
                int currentMaxPageTableId = 0;
                int pgCnt = FolderUi.getFolder_pagesCount(this,FolderUi.getFocus_folderPos());
                DB_folder db_folder = new DB_folder(this,DB_folder.getFocusFolder_tableId());

                for(int i=0;i< pgCnt;i++)
                {
                    int id = db_folder.getPageTableId(i,true);
                    if(id >currentMaxPageTableId)
                        currentMaxPageTableId = id;
                }

                PageUi.addNewPage(this, currentMaxPageTableId + 1);
                return true;

            case MenuId.CHANGE_PAGE_COLOR:
                PageUi.changePageColor(this);
                return true;

            case MenuId.SHIFT_PAGE:
                PageUi.shiftPage(this);
            return true;

            case MenuId.DELETE_PAGES:
                //hide the menu
                mMenu.setGroupVisible(R.id.group_notes, false);
                mMenu.setGroupVisible(R.id.group_pages_and_more, false);

                if(dB_folder.getPagesCount(true)>0)
                {

                    DeletePages delPgsFragment = new DeletePages();
                    mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                    mFragmentTransaction.replace(R.id.content_frame, delPgsFragment).addToBackStack("delete_pages").commit();
                }
                else
                {
                    Toast.makeText(this, R.string.no_page_yet, Toast.LENGTH_SHORT).show();
                }
            return true;

            case MenuId.ENABLE_NOTE_DRAG_AND_DROP:
                mPref_show_note_attribute = mContext.getSharedPreferences("show_note_attribute", 0);
                if(mPref_show_note_attribute.getString("KEY_ENABLE_DRAGGABLE", "yes").equalsIgnoreCase("yes")) {
                    mPref_show_note_attribute.edit().putString("KEY_ENABLE_DRAGGABLE", "no").apply();
                    Toast.makeText(this,getResources().getString(R.string.drag_note)+
                                        ": " +
                                        getResources().getString(R.string.set_disable),
                                   Toast.LENGTH_SHORT).show();
                }
                else {
                    mPref_show_note_attribute.edit().putString("KEY_ENABLE_DRAGGABLE", "yes").apply();
                    Toast.makeText(this,getResources().getString(R.string.drag_note) +
                                        ": " +
                                        getResources().getString(R.string.set_enable),
                                   Toast.LENGTH_SHORT).show();
                }
                invalidateOptionsMenu();
                TabsHost.reloadCurrentPage();
                return true;

            case MenuId.SHOW_BODY:
                mPref_show_note_attribute = mContext.getSharedPreferences("show_note_attribute", 0);
                if(mPref_show_note_attribute.getString("KEY_SHOW_BODY", "yes").equalsIgnoreCase("yes")) {
                    mPref_show_note_attribute.edit().putString("KEY_SHOW_BODY", "no").apply();
                    Toast.makeText(this,getResources().getString(R.string.preview_note_body) +
                                        ": " +
                                        getResources().getString(R.string.set_disable),
                                    Toast.LENGTH_SHORT).show();
                }
                else {
                    mPref_show_note_attribute.edit().putString("KEY_SHOW_BODY", "yes").apply();
                    Toast.makeText(this,getResources().getString(R.string.preview_note_body) +
                                        ": " +
                                        getResources().getString(R.string.set_enable),
                                   Toast.LENGTH_SHORT).show();
                }
                invalidateOptionsMenu();
                TabsHost.reloadCurrentPage();
                return true;

            // sub menu for backup

            case MenuId.IMPORT_JSON_FROM_WEB:
                Intent importJson_web = new Intent(this, Import_webJsonAct.class);
                startActivityForResult(importJson_web,8000);
                return true;

	        case MenuId.IMPORT_FROM_SD_CARD_JSON:
                //hide the menu
                mMenu.setGroupVisible(R.id.group_notes, false);
                mMenu.setGroupVisible(R.id.group_pages_and_more, false);
                // replace fragment
                Import_filesListJson importFragmentJson = new Import_filesListJson();
                mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                mFragmentTransaction.replace(R.id.content_frame, importFragmentJson, "import").addToBackStack(null).commit();
		        return true;

            case MenuId.IMPORT_FROM_GDRIVE_JSON:
                mMenu.setGroupVisible(R.id.group_notes, false);
                mMenu.setGroupVisible(R.id.group_pages_and_more, false);
                Intent intent = new Intent(this, ImportGDriveAct.class);
                startActivity(intent);
                return true;

            case MenuId.EXPORT_TO_SD_CARD_JSON:
                //hide the menu
                mMenu.setGroupVisible(R.id.group_notes, false);
                mMenu.setGroupVisible(R.id.group_pages_and_more, false);
                if(dB_folder.getPagesCount(true)>0)
                {
                    Export_toSDCardJsonFragment exportFragment = new Export_toSDCardJsonFragment();
                    mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                    mFragmentTransaction.replace(R.id.content_frame, exportFragment,"export").addToBackStack(null).commit();
                }
                else
                {
                    Toast.makeText(this, R.string.no_page_yet, Toast.LENGTH_SHORT).show();
                }
                return true;

            case MenuId.EXPORT_TO_SD_CARD_ALL_JSON:
                //hide the menu
                mMenu.setGroupVisible(R.id.group_notes, false);
                mMenu.setGroupVisible(R.id.group_pages_and_more, false);
                Export_toSDCardAllJsonFragment exportFragment = new Export_toSDCardAllJsonFragment();
                mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                mFragmentTransaction.replace(R.id.content_frame, exportFragment, "export").addToBackStack(null).commit();
                return true;

            case MenuId.EXPORT_TO_GDRIVE_ALL_JSON:
                //hide the menu
                mMenu.setGroupVisible(R.id.group_notes, false);
                mMenu.setGroupVisible(R.id.group_pages_and_more, false);
                Intent intnt = new Intent(this, ExportGDriveAct.class);
                startActivity(intnt);
                return true;

            case MenuId.SEND_JSON:
                //hide the menu
                mMenu.setGroupVisible(R.id.group_notes, false);
                mMenu.setGroupVisible(R.id.group_pages_and_more, false);

                if(dB_folder.getPagesCount(true)>0)
                {
                    Mail_filesListJson mailFragment = new Mail_filesListJson();

                    mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                    mFragmentTransaction.replace(R.id.content_frame, mailFragment,"mail").addToBackStack(null).commit();
                }
                else
                {
                    Toast.makeText(this, R.string.no_page_yet, Toast.LENGTH_SHORT).show();
                }
                return true;

            case MenuId.IMPORT_RENEW:
                mMenu.setGroupVisible(R.id.group_notes, false); //hide the menu
                new RenewDB(this);
                return true;

            case MenuId.CONFIG:
                mMenu.setGroupVisible(R.id.group_notes, false); //hide the menu
                mMenu.setGroupVisible(R.id.group_pages_and_more, false);
                setTitle(R.string.settings);

                mConfigFragment = new Config();
                mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                mFragmentTransaction.replace(R.id.content_frame, mConfigFragment).addToBackStack("config").commit();
                return true;

            case MenuId.ABOUT:
                mMenu.setGroupVisible(R.id.group_notes, false); //hide the menu
                mMenu.setGroupVisible(R.id.group_pages_and_more, false);
                setTitle(R.string.about_title);

                mAboutFragment = new About();
                mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                mFragmentTransaction.replace(R.id.content_frame, mAboutFragment).addToBackStack("about").commit();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // configure layout view
    void configLayoutView()
    {
        System.out.println("MainAct / _configLayoutView");

        setContentView(R.layout.drawer);
        initActionBar();

        // new drawer
        drawer = new Drawer(this);
        drawer.initDrawer();

        // new folder
        mFolder = new Folder(this);
        openFolder();
    }


    // callback: media browser connection
    public static MediaBrowserCompat.ConnectionCallback mMediaBrowserCompatConnectionCallback = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            super.onConnected();

            System.out.println("MainAct / MediaBrowserCompat.Callback / _onConnected");
            try {
                mMediaControllerCompat = new MediaControllerCompat(mAct, mMediaBrowserCompat.getSessionToken());
                mMediaControllerCompat.registerCallback(mMediaControllerCompatCallback);
                MediaControllerCompat.setMediaController(mAct,mMediaControllerCompat);
            } catch( RemoteException e ) {
                System.out.println("MainAct / MediaBrowserCompat.Callback / RemoteException");
            }
        }
    };

    // callback: media controller
    public static MediaControllerCompat.Callback mMediaControllerCompatCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
//            System.out.println("MainAct / _MediaControllerCompat.Callback / _onPlaybackStateChanged / state = " + state);
            if( state == null ) {
                return;
            }

            switch( state.getState() ) {
                case STATE_PLAYING: {
                    mCurrentState = STATE_PLAYING;
                    break;
                }
                case STATE_PAUSED: {
                    mCurrentState = STATE_PAUSED;
                    break;
                }
            }
        }
    };

}
