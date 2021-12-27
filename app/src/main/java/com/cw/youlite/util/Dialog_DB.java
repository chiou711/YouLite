/*
 * Copyright (C) 2021 CW Chiu
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

package com.cw.youlite.util;
/**
 * This file provides simple End User License Agreement
 * It shows a simple dialog with the license text, and two buttons.
 * If user clicks on 'cancel' button, app closes and user will not be granted access to app.
 * If user clicks on 'accept' button, app access is allowed and this choice is saved in preferences
 * so next time this will not show, until next upgrade.
 */

import android.app.AlertDialog;
import android.content.DialogInterface;

import com.cw.youlite.R;
import com.cw.youlite.util.preferences.Pref;

import androidx.appcompat.app.AppCompatActivity;

public class Dialog_DB {

    private AppCompatActivity mAct;
    public DialogInterface.OnClickListener clickListener_Ok;
    public DialogInterface.OnClickListener clickListener_No;

    private String title;
    private String message;

    public Dialog_DB(AppCompatActivity act ){
        mAct = act;

        // Dialog title
        title = mAct.getString(R.string.app_name) +
                " v" +
                Pref.getPackageInfo(act).versionName;

        // DB check message
        message = mAct.getString(R.string.renew_title);
    }

    public void show()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(mAct)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.accept, clickListener_Ok)
                .setNegativeButton(android.R.string.cancel,clickListener_No);
        builder.create().show();
    }
}