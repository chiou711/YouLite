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

package com.cw.youlite.define;

import android.content.Context;

import com.cw.youlite.R;

/**
 * Created by CW on 2016/6/16.
 * Modified by CW on 2019/9/11
 *
 * build apk file size:
 * 1) prefer w/ assets files: 15,483 KB
 *
 * 2) default w/ assets files: 15,483 KB
 *
 * 3) default w/o assets files: 1,173 KB
 *
 * 4) release: 706 KB
 */
public class Define {

    public final static int DEBUG_DEFAULT_BY_DOWNLOAD = 2;
    public final static int RELEASE_DEFAULT_BY_DOWNLOAD = 5;

    public static void setAppBuildMode( ) {
        /* 1 debug, download */
//        app_build_mode = DEBUG_DEFAULT_BY_DOWNLOAD;

        /* 2 release, download */
        app_build_mode = RELEASE_DEFAULT_BY_DOWNLOAD;

        switch (app_build_mode)
        {
            case DEBUG_DEFAULT_BY_DOWNLOAD:
                CODE_MODE = DEBUG_MODE;
                DEFAULT_CONTENT = BY_DOWNLOAD;
                break;

            case RELEASE_DEFAULT_BY_DOWNLOAD:
                CODE_MODE = RELEASE_MODE;
                DEFAULT_CONTENT = BY_DOWNLOAD;
                break;

            default:
                break;
        }
    }

    public static int app_build_mode = 0;

    /***************************************************************************
     * Set release/debug mode
     * - RELEASE_MODE
     * - DEBUG_MODE
     ***************************************************************************/
    public static int CODE_MODE;// = DEBUG_MODE; //DEBUG_MODE; //RELEASE_MODE;
    public static int DEBUG_MODE = 0;
    public static int RELEASE_MODE = 1;


    /****************************************************************************
     *
     * Flags for Default tables after App installation:
     * - default content: DEFAULT_CONTENT
     *      - by initial tables: INITIAL_FOLDERS_COUNT, INITIAL_PAGES_COUNT
     *      - by assets XML
     *      - by download XML
     * Note of flag setting: exclusive
     *
     * With default content
     * - true : un-mark preferred/assets/ line in build.gradle file
     * - false:    mark preferred/assets/ line in build.gradle file
     *
     * android {
     * ...
     *    sourceSets {
     *        main {
     *      // mark: W/O default content
     *      // un-mark: With default content
     *      // Apk file size will increase if assets directory is set at default location (src/main/assets)
     *           assets.srcDirs = ['preferred/assets/']
     *      }
     *    }
     * }
     *
     ************************************************************************************************************/

    /***
     *  With default content by XML file
     */
    public static int DEFAULT_CONTENT;
    // by downloaded JSON file
    public static int BY_DOWNLOAD = 2;


    /***************************************************************************
     *  Enable AdMob at page bottom
     *  Need to choose a case in AndroidManifest.xml
     *     <!-- AdMob: formal case    -->
     *     <!-- AdMob: debug case    -->
     *
     ***************************************************************************/
//    public static boolean ENABLE_ADMOB = false; //true; //false;


    // Apply system default for picture path
    public static boolean PICTURE_PATH_BY_SYSTEM_DEFAULT = true;


    // default style
    public static int STYLE_DEFAULT = 1;
    public static int STYLE_PREFER = 2;

    public static String getTabTitle(Context context, Integer Id)
    {
        String title;
            title = context.getResources().getString(R.string.default_page_name).concat(String.valueOf(Id));
        return title;
    }

}
