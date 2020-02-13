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

package com.cw.youlite.main;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.cw.youlite.R;
import com.cw.youlite.config.About;
import com.cw.youlite.config.Config;
import com.cw.youlite.data.Contract;
import com.cw.youlite.data.DbHelper;
import com.cw.youlite.data.FetchService_category;
import com.cw.youlite.data.Provider;
import com.cw.youlite.db.DB_folder;
import com.cw.youlite.db.DB_page;
import com.cw.youlite.drawer.Drawer;
import com.cw.youlite.folder.Folder;
import com.cw.youlite.folder.FolderUi;
import com.cw.youlite.note_add.Add_note_option;
import com.cw.youlite.operation.audio.Audio_manager;
import com.cw.youlite.operation.audio.AudioPlayer_page;
import com.cw.youlite.operation.audio.BackgroundAudioService;
import com.cw.youlite.operation.delete.DeleteFolders;
import com.cw.youlite.operation.delete.DeletePages;
import com.cw.youlite.operation.import_export.Export_toSDCardAllJsonFragment;
import com.cw.youlite.operation.import_export.Export_toSDCardJsonFragment;
import com.cw.youlite.operation.import_export.GDriveRewriteAct;
import com.cw.youlite.operation.import_export.Import_filesListJson;
import com.cw.youlite.operation.import_export.Import_webJsonAct;
import com.cw.youlite.operation.mail.Mail_filesListJson;
import com.cw.youlite.operation.mail.Mail_fileViewJson;
import com.cw.youlite.page.Checked_notes_option;
import com.cw.youlite.page.PageUi;
import com.cw.youlite.page.Page_recycler;
import com.cw.youlite.tabs.AudioUi_page;
import com.cw.youlite.tabs.TabsHost;
import com.cw.youlite.util.DeleteFileAlarmReceiver;
import com.cw.youlite.db.DB_drawer;
import com.cw.youlite.util.Dialog_EULA;
import com.cw.youlite.util.audio.UtilAudio;
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
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
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
import androidx.core.app.NotificationManagerCompat;
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

    final int INIT_NUMBER = 1;

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
        /** 1 debug, download */
        Define.setAppBuildMode(Define.DEBUG_DEFAULT_BY_DOWNLOAD);

        /** 2 release, download */
//        Define.setAppBuildMode(Define.RELEASE_DEFAULT_BY_DOWNLOAD);

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
                renewDB();
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
                doCreate(savedInstanceState);
        }

    }

    // get default URL
    private String getDefaultUrl()
    {
        // data base is not created yet, call service for the first time
        String urlName = "catalog_url_".concat(String.valueOf(INIT_NUMBER));
        int id = getResources().getIdentifier(urlName,"string",getPackageName());
        return getString(id);
    }

    // Do major create operation
    void doCreate(Bundle savedInstanceState)
    {
        System.out.println("MainAct / _doCreate");

        mFolderTitles = new ArrayList<>();

        //Add note with the link which got from other App
        String intentLink = mMainUi.addNote_IntentLink(getIntent(), mAct);
        if (!Util.isEmptyString(intentLink)) {
            finish(); // YouLite not running at first, keep closing
            return;
        } else {
            // check DB
            final boolean ENABLE_DB_CHECK = false;//true;//false
            if (ENABLE_DB_CHECK) {
                // list all folder tables
                FolderUi.listAllFolderTables(mAct);

                // recover focus
                DB_folder.setFocusFolder_tableId(Pref.getPref_focusView_folder_tableId(this));
                DB_page.setFocusPage_tableId(Pref.getPref_focusView_page_tableId(this));
            }//if(ENABLE_DB_CHECK)

            mContext = getBaseContext();

            // add on back stack changed listener
            mFragmentManager = getSupportFragmentManager();
            mOnBackStackChangedListener = this;
            mFragmentManager.addOnBackStackChangedListener(mOnBackStackChangedListener);
        }

        isAddedOnNewIntent = false;

        // Register Bluetooth device receiver
        if (Build.VERSION.SDK_INT < 21) {
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
            this.registerReceiver(bluetooth_device_receiver, filter);
        } else // if(Build.VERSION.SDK_INT >= 21)
        {
            // Media session: to receive media button event of bluetooth device
            // new media browser instance and create BackgroundAudioService instance: support notification
            if (mMediaBrowserCompat == null) {
                mMediaBrowserCompat = new MediaBrowserCompat(mAct,
                        new ComponentName(mAct, BackgroundAudioService.class),
                        mMediaBrowserCompatConnectionCallback,
                        mAct.getIntent().getExtras());

                if (!mMediaBrowserCompat.isConnected())
                    mMediaBrowserCompat.connect();//cf: https://stackoverflow.com/questions/43169875/mediabrowser-subscribe-doesnt-work-after-i-get-back-to-activity-1-from-activity

                mCurrentState = STATE_PAUSED;
            }
        }

        // init audio parameters
        MainAct.mPlaying_folderPos = -1;
        Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_STOP);
        TabsHost.audioPlayTabPos = -1;
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
                if(TabsHost.audioUi_page != null)
                    TabsHost.audioUi_page.audioPanel_previous_btn.performClick();
                return true;

            case KeyEvent.KEYCODE_MEDIA_NEXT: //87
                if(TabsHost.audioUi_page != null)
                    TabsHost.audioUi_page.audioPanel_next_btn.performClick();

                return true;

            case KeyEvent.KEYCODE_MEDIA_PLAY: //126
                if(TabsHost.audioUi_page != null)
                    TabsHost.audioUi_page.audioPanel_play_button.performClick();
                else
                    playFirstAudio();
                return true;

            case KeyEvent.KEYCODE_MEDIA_PAUSE: //127
                if(TabsHost.audioUi_page != null)
                    TabsHost.audioUi_page.audioPanel_play_button.performClick();
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

    private boolean isStorageRequestedExportOne = false;
    private boolean isStorageRequestedExportAll = false;
    private boolean isStorageRequestedImport = false;

    // callback of granted permission
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        System.out.println("MainAct / _onRequestPermissionsResult / grantResults.length =" + grantResults.length);

        if ( (grantResults.length > 0) &&
             ( (grantResults[0] == PackageManager.PERMISSION_GRANTED) &&
               (grantResults[1] == PackageManager.PERMISSION_GRANTED)   ) )
        {
            switch (requestCode)
            {
                case Util.PERMISSIONS_REQUEST_STORAGE_WITH_DEFAULT_CONTENT_YES:
                    if(Define.DEFAULT_CONTENT == Define.BY_DOWNLOAD) {
                        // start Fetch category service
                        System.out.println("MainAct / _onRequestPermissionsResult / start Fetch category service =================================");
                        Toast.makeText(mAct, R.string.toast_update_database,Toast.LENGTH_LONG).show();
                        Intent serviceIntent = new Intent(MainAct.this, FetchService_category.class);
                        serviceIntent.putExtra("FetchUrl", getDefaultUrl());
                        startService(serviceIntent);
                    }
                    else {
                        Pref.setPref_will_create_default_content(this, true);
                        recreate();
                    }
                break;

                case Util.PERMISSIONS_REQUEST_STORAGE_WITH_DEFAULT_CONTENT_NO:
                    Pref.setPref_will_create_default_content(this, false);
                    recreate();
                break;

                case Util.PERMISSIONS_REQUEST_STORAGE_EXPORT_ONE:
                    isStorageRequestedExportOne = true;
                    break;

                case Util.PERMISSIONS_REQUEST_STORAGE_EXPORT_ALL:
                    isStorageRequestedExportAll = true;
                    break;

                case Util.PERMISSIONS_REQUEST_STORAGE_IMPORT:
                    isStorageRequestedImport = true;
                    break;
            }
        }
        else
        {
            Pref.setPref_will_create_default_content(this, false);
            recreate();
        }

        //normally, will go to _resume
    }


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

    boolean isAddedOnNewIntent;
    // if one YouLite Intent is already running, call it again in YouTube or Browser will run into this
    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        if(!isAddedOnNewIntent)
        {
            String intentTitle = mMainUi.addNote_IntentLink(intent, mAct);
            if (!Util.isEmptyString(intentTitle))
                TabsHost.reloadCurrentPage();

            if(Build.VERSION.SDK_INT >= O)//API26
                isAddedOnNewIntent = true; // fix 2 times _onNewIntent on API26
        }
    }

    // for Rotate screen
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
       super.onSaveInstanceState(outState);
        System.out.println("MainAct / onSaveInstanceState / getFocus_folderPos() = " + FolderUi.getFocus_folderPos());
        outState.putInt("NowFolderPosition", FolderUi.getFocus_folderPos());
        outState.putInt("Playing_pageId", mPlaying_pagePos);
        outState.putInt("Playing_folderPos", mPlaying_folderPos);
        outState.putInt("SeekBarProgress", AudioUi_page.mProgress);
        outState.putInt("AudioInfo_state", Audio_manager.getPlayerState());
        if(FolderUi.mHandler != null)
            FolderUi.mHandler.removeCallbacks(FolderUi.mTabsHostRun);
        FolderUi.mHandler = null;
    }

    // for After Rotate
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        System.out.println("MainAct / _onRestoreInstanceState ");
        if(savedInstanceState != null)
        {
            FolderUi.setFocus_folderPos(savedInstanceState.getInt("NowFolderPosition"));
            mPlaying_pagePos = savedInstanceState.getInt("Playing_pageId");
            mPlaying_folderPos = savedInstanceState.getInt("Playing_folderPos");
            Audio_manager.setPlayerState(savedInstanceState.getInt("AudioInfo_state"));
            AudioUi_page.mProgress = savedInstanceState.getInt("SeekBarProgress");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        bluetooth_device_receiver.abortBroadcast();//todo better place?
        System.out.println("MainAct / _onPause");
    }


    private FetchServiceResponseReceiver responseReceiver;
    LocalBroadcastManager localBroadcastMgr;

    @Override
    protected void onResume() {
        super.onResume();
    	System.out.println("MainAct / _onResume");

    	mAct = this;

        configLayoutView(); //createAssetsFile inside

        // Sync the toggle state after onRestoreInstanceState has occurred.
        if(bEULA_accepted)
            drawer.drawerToggle.syncState();

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
                    context.startActivity(new_intent);
                }
            }
        }
    }


    @Override
    protected void onResumeFragments() {
        System.out.println("MainAct / _onResumeFragments ");
        super.onResumeFragments();

        if(isStorageRequestedExportOne ||
           isStorageRequestedExportAll ||
           isStorageRequestedImport      )
        {
            //hide the menu
            mMenu.setGroupVisible(R.id.group_notes, false);
            mMenu.setGroupVisible(R.id.group_pages_and_more, false);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            if(isStorageRequestedExportOne) {
                DB_folder dB_folder = new DB_folder(this, Pref.getPref_focusView_folder_tableId(this));
                if (dB_folder.getPagesCount(true) > 0) {
                        Export_toSDCardJsonFragment exportFragment = new Export_toSDCardJsonFragment();
                        transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                        transaction.replace(R.id.content_frame, exportFragment, "export")
                                .addToBackStack(null)
                                .commit();
                } else
                    Toast.makeText(this, R.string.no_page_yet, Toast.LENGTH_SHORT).show();
                isStorageRequestedExportOne = false;
            }

            if(isStorageRequestedExportAll)
            {
                Export_toSDCardAllJsonFragment exportFragment = new Export_toSDCardAllJsonFragment();
                transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                transaction.replace(R.id.content_frame, exportFragment, "export").addToBackStack(null).commit();
                isStorageRequestedExportAll = false;
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
        if (dB_drawer.getFoldersCount(true) > 0) {
            System.out.println("MainAct / _openFolder / getFocus_folderPos() = " + FolderUi.getFocus_folderPos());

            int focus_folderPos = FolderUi.getFocus_folderPos();
            FolderUi.selectFolder(mAct, focus_folderPos);

            if (mAct.getSupportActionBar() != null)
                mAct.getSupportActionBar().setTitle(mFolderTitle);
        }
    }


    @Override
    protected void onDestroy()
    {
        System.out.println("MainAct / onDestroy");

        if(bluetooth_device_receiver != null)
        {
            try
            {
                unregisterReceiver(bluetooth_device_receiver);
            }
            catch (Exception e)
            {
            }
            bluetooth_device_receiver = null;
        }

        // stop audio player
        if(BackgroundAudioService.mMediaPlayer != null)
            Audio_manager.stopAudioPlayer();

        // disconnect MediaBrowserCompat
        if( (mMediaBrowserCompat != null) && mMediaBrowserCompat.isConnected())
            mMediaBrowserCompat.disconnect();

        //hide notification
        NotificationManagerCompat.from(MainAct.mAct).cancel(BackgroundAudioService.id);

        mMediaBrowserCompat = null;

        // unregister receiver
        localBroadcastMgr.unregisterReceiver(responseReceiver);
        responseReceiver = null;

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
    String nextLinkTitle;
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
        }

        // YouTube
        if(requestCode == Util.YOUTUBE_LINK_INTENT)
        {
            // preference of delay
            SharedPreferences pref_delay = getSharedPreferences("youtube_launch_delay", 0);
            count = Integer.valueOf(pref_delay.getString("KEY_YOUTUBE_LAUNCH_DELAY","10"));

            builder = new AlertDialog.Builder(this);

            do
            {
                TabsHost.getCurrentPage().mCurrPlayPosition++;
                if(Page_recycler.mCurrPlayPosition >= TabsHost.getCurrentPage().getNotesCountInPage(mAct))
                    TabsHost.getCurrentPage().mCurrPlayPosition = 0; //back to first index

                nextLinkTitle = mMainUi.getYouTubeLink(this,TabsHost.getCurrentPage().mCurrPlayPosition);
            }
            while (!Util.isYouTubeLink(nextLinkTitle));

            countStr = getResources().getString(R.string.message_continue_or_stop_YouTube_message);
            countStr = countStr.replaceFirst("[0-9]",String.valueOf(count));

            builder.setTitle(R.string.message_continue_or_stop_YouTube_title)
                    .setMessage(nextLinkTitle +"\n\n" + countStr)
                    .setNegativeButton(R.string.confirm_dialog_button_no, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog1, int which1)
                        {
                            alertDlg.dismiss();
                            mMainUi.cancelYouTubeHandler(handler,runCountDown);
                        }
                    })
                    .setPositiveButton(R.string.confirm_dialog_button_yes, new DialogInterface.OnClickListener()
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
        if(requestCode == Util.YOUTUBE_ADD_NEW_LINK_INTENT)
        {
            System.out.println("MainAct / _onActivityResult /YOUTUBE_ADD_NEW_LINK_INTENT");

            if(Build.VERSION.SDK_INT >= O)//API26
                isAddedOnNewIntent = false;
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
            messageView.setText(nextLinkTitle + "\n\n" +countStr);

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
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    /*************************
     * onCreate Options Menu
     *
     *************************/
    public static MenuItem mSubMenuItemAudio;
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
    public static int mPlaying_pageTableId;
    public static int mPlaying_pagePos;
    public static int mPlaying_folderPos;
    public static int mPlaying_folderTableId;

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
        DB_page dB_page = new DB_page(this,Pref.getPref_focusView_page_tableId(this));

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

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
            case MenuId.ADD_NEW_FOLDER:
                FolderUi.renewFirstAndLast_folderId();
                FolderUi.addNewFolder(this, FolderUi.mLastExist_folderTableId +1, mFolder.getAdapter());
                return true;

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

            case MenuId.DELETE_FOLDERS:
                mMenu.setGroupVisible(R.id.group_folders, false);

                if(dB_drawer.getFoldersCount(true)>0)
                {
                    drawer.closeDrawer();
                    mMenu.setGroupVisible(R.id.group_notes, false); //hide the menu
                    DeleteFolders delFoldersFragment = new DeleteFolders();
                    mFragmentTransaction = mFragmentManager.beginTransaction();
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
                if(dB_folder.getPagesCount(true)>0)
                {
                    mMenu.setGroupVisible(R.id.group_notes, false); //hide the menu
                    DeletePages delPgsFragment = new DeletePages();
                    mFragmentTransaction = mFragmentManager.beginTransaction();
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
		        if( (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) && //API23
				        (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) // check permission
						        != PackageManager.PERMISSION_GRANTED))
		        {
			        // No explanation needed, we can request the permission.
			        ActivityCompat.requestPermissions(this,
					        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
							        Manifest.permission.READ_EXTERNAL_STORAGE},
					        Util.PERMISSIONS_REQUEST_STORAGE_IMPORT);
		        }
		        else {
			        //hide the menu
			        mMenu.setGroupVisible(R.id.group_notes, false);
			        mMenu.setGroupVisible(R.id.group_pages_and_more, false);
			        // replace fragment
			        Import_filesListJson importFragmentJson = new Import_filesListJson();
			        transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
			        transaction.replace(R.id.content_frame, importFragmentJson, "import").addToBackStack(null).commit();
		        }
		        return true;

            case MenuId.EXPORT_TO_SD_CARD_JSON:
                if( (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) && //API23
                    (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) // check permission
                                != PackageManager.PERMISSION_GRANTED))
                {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE},
                            Util.PERMISSIONS_REQUEST_STORAGE_EXPORT_ONE);
                }
                else {
                    //hide the menu
                    mMenu.setGroupVisible(R.id.group_notes, false);
                    mMenu.setGroupVisible(R.id.group_pages_and_more, false);
                    if(dB_folder.getPagesCount(true)>0)
                    {
                        Export_toSDCardJsonFragment exportFragment = new Export_toSDCardJsonFragment();
                        transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                        transaction.replace(R.id.content_frame, exportFragment,"export").addToBackStack(null).commit();
                    }
                    else
                    {
                        Toast.makeText(this, R.string.no_page_yet, Toast.LENGTH_SHORT).show();
                    }
                }
                return true;

            case MenuId.EXPORT_TO_SD_CARD_ALL_JSON:
                if( (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) && //API23
                        (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) // check permission
                                != PackageManager.PERMISSION_GRANTED))
                {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE},
                            Util.PERMISSIONS_REQUEST_STORAGE_EXPORT_ALL);
                }
                else {
                    //hide the menu
                    mMenu.setGroupVisible(R.id.group_notes, false);
                    mMenu.setGroupVisible(R.id.group_pages_and_more, false);
                    Export_toSDCardAllJsonFragment exportFragment = new Export_toSDCardAllJsonFragment();
                    transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                    transaction.replace(R.id.content_frame, exportFragment, "export").addToBackStack(null).commit();
                }
                return true;

            case MenuId.EXPORT_TO_GDRIVE_ALL_JSON:
                //hide the menu
                mMenu.setGroupVisible(R.id.group_notes, false);
                mMenu.setGroupVisible(R.id.group_pages_and_more, false);
                Intent intent = new Intent(this, GDriveRewriteAct.class);
                startActivity(intent);
                return true;

            case MenuId.SEND_JSON:
                mMenu.setGroupVisible(R.id.group_notes, false); //hide the menu

                if(dB_folder.getPagesCount(true)>0)
                {
                    Mail_filesListJson mailFragment = new Mail_filesListJson();

                    transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                    transaction.replace(R.id.content_frame, mailFragment,"mail").addToBackStack(null).commit();
                }
                else
                {
                    Toast.makeText(this, R.string.no_page_yet, Toast.LENGTH_SHORT).show();
                }
                return true;

            case MenuId.IMPORT_RENEW:
                mMenu.setGroupVisible(R.id.group_notes, false); //hide the menu
                // delete database
                try {
                    System.out.println("MainAct/ _onOptionsItemSelected / will delete DB");
                    deleteDatabase(DbHelper.DATABASE_NAME);

                    ContentResolver resolver = getContentResolver();
                    ContentProviderClient client = resolver.acquireContentProviderClient(Contract.CONTENT_AUTHORITY);
                    Provider provider = (Provider) client.getLocalContentProvider();

                    provider.mContentResolver = resolver;
                    provider.mOpenHelper.close();

                    provider.mOpenHelper = new DbHelper(this);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        client.close();
                    else
                        client.release();

                } catch (Exception e) {
                    e.printStackTrace();
                }

                // start Fetch category service
                System.out.println("MainAct / _onOptionsItemSelected / start Fetch category service =================================");
                Toast.makeText(mAct,R.string.toast_update_database,Toast.LENGTH_LONG).show();
                Intent serviceIntent = new Intent(MainAct.this, FetchService_category.class);
                serviceIntent.putExtra("FetchUrl", getDefaultUrl());
                startService(serviceIntent);

                // reset focus view position
                Pref.setPref_focusView_folder_tableId(this, 1);
                Pref.setPref_focusView_page_tableId(this, 1);

                return true;

            case MenuId.CONFIG:
                mMenu.setGroupVisible(R.id.group_notes, false); //hide the menu
                mMenu.setGroupVisible(R.id.group_pages_and_more, false);
                setTitle(R.string.settings);

                mConfigFragment = new Config();
                mFragmentTransaction = mFragmentManager.beginTransaction();
                mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                mFragmentTransaction.replace(R.id.content_frame, mConfigFragment).addToBackStack("config").commit();
                return true;

            case MenuId.ABOUT:
                mMenu.setGroupVisible(R.id.group_notes, false); //hide the menu
                mMenu.setGroupVisible(R.id.group_pages_and_more, false);
                setTitle(R.string.about_title);

                mAboutFragment = new About();
                mFragmentTransaction = mFragmentManager.beginTransaction();
                mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                mFragmentTransaction.replace(R.id.content_frame, mAboutFragment).addToBackStack("about").commit();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    // renew data base
    void renewDB()
    {
        try {
            System.out.println("MainAct/ _renewDB");
            deleteDatabase(DbHelper.DATABASE_NAME);

            ContentResolver resolver = getContentResolver();
            ContentProviderClient client = resolver.acquireContentProviderClient(Contract.CONTENT_AUTHORITY);
            Provider provider = (Provider) client.getLocalContentProvider();

            provider.mContentResolver = resolver;
            provider.mOpenHelper.close();

            provider.mOpenHelper = new DbHelper(this);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                client.close();
            else
                client.release();

            Pref.setPref_DB_ready(this,false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // start Fetch category service
        System.out.println("MainAct / _onOptionsItemSelected / start Fetch category service =================================");
        Toast.makeText(mAct,R.string.toast_update_database,Toast.LENGTH_LONG).show();
        Intent serviceIntent = new Intent(MainAct.this, FetchService_category.class);
        serviceIntent.putExtra("FetchUrl", getDefaultUrl());
        startService(serviceIntent);

        // reset focus view position
        Pref.setPref_focusView_folder_tableId(this, 1);
        Pref.setPref_focusView_page_tableId(this, 1);
    }

    void playFirstAudio()
    {
        Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_PLAY);

        Audio_manager.setAudioPlayMode(Audio_manager.PAGE_PLAY_MODE);
        Audio_manager.mAudioPos = 0;

        // cancel playing
        if(BackgroundAudioService.mMediaPlayer != null)
        {
            if(BackgroundAudioService.mMediaPlayer.isPlaying())
                BackgroundAudioService.mMediaPlayer.pause();

            if((AudioPlayer_page.mAudioHandler != null) &&
                    (TabsHost.audioPlayer_page != null)        ){
                AudioPlayer_page.mAudioHandler.removeCallbacks(TabsHost.audioPlayer_page.page_runnable);
            }
            BackgroundAudioService.mMediaPlayer.release();
            BackgroundAudioService.mMediaPlayer = null;
        }

        // initial
        BackgroundAudioService.mMediaPlayer = null;//for first

        Page_recycler page = TabsHost.getCurrentPage();
        TabsHost.audioUi_page = new AudioUi_page(this,page.recyclerView);
        TabsHost.audioUi_page.initAudioBlock(this);

        TabsHost.audioPlayer_page = new AudioPlayer_page(this,TabsHost.audioUi_page);
        TabsHost.audioPlayer_page.prepareAudioInfo();
        TabsHost.audioPlayer_page.runAudioState();

        // update audio play position
        TabsHost.audioPlayTabPos = TabsHost.getFocus_tabPos();
        TabsHost.mTabsPagerAdapter.notifyDataSetChanged();

        UtilAudio.updateAudioPanel(TabsHost.audioUi_page.audioPanel_play_button,
                TabsHost.audioUi_page.audio_panel_title_textView);

        // update playing page position
        mPlaying_pagePos = TabsHost.getFocus_tabPos();

        // update playing page table Id
        mPlaying_pageTableId = TabsHost.getCurrentPageTableId();

        // update playing folder position
        mPlaying_folderPos = FolderUi.getFocus_folderPos();

        DB_drawer dB_drawer = new DB_drawer(this);
        MainAct.mPlaying_folderTableId = dB_drawer.getFolderTableId(MainAct.mPlaying_folderPos,true);
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
