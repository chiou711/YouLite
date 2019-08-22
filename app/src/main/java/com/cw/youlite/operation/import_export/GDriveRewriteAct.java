/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cw.youlite.operation.import_export;

import android.os.Environment;
import android.util.Log;

import com.cw.youlite.R;
import com.cw.youlite.util.Util;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.tasks.Task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * An activity to illustrate how to edit contents of a Drive file.
 */
public class GDriveRewriteAct extends GDriveBaseAct {
    private static final String TAG = "GDriveRewriteAct";

    @Override
    protected void onDriveClientReady() {
//        pickTextFile()
        pickJsonFile()
                .addOnSuccessListener(this,
                        driveId -> rewriteContents(driveId.asDriveFile()))
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "No file selected", e);
                    showMessage(getString(R.string.file_not_selected));
                    finish();
                });
    }

    private void rewriteContents(DriveFile file) {
        // input file content
        String dirString  = Environment.getExternalStorageDirectory().toString() +
                                "/" + Util.getStorageDirName(this);
        System.out.println("GDriveRewriteAct / dirString = " + dirString);

        File srcFile = new File(dirString,"g_drive_src.json");
        StringBuilder total = null;
        try
        {
            FileInputStream fileInputStream = new FileInputStream(srcFile);
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
        System.out.println("GDriveRewriteAct / jsonString = " + jsonString);

        // [START drive_android_open_for_write]
        Task<DriveContents> openTask =
                getDriveResourceClient().openFile(file, DriveFile.MODE_WRITE_ONLY);
        // [END drive_android_open_for_write]

        // [START drive_android_rewrite_contents]
        openTask.continueWithTask(task -> {
            DriveContents driveContents = task.getResult();
            try (OutputStream out = driveContents.getOutputStream()) {
                out.write(jsonString.getBytes());
            }
            // [START drive_android_commit_content]
            Task<Void> commitTask =
                    getDriveResourceClient().commitContents(driveContents, null);
            // [END drive_android_commit_content]
            return commitTask;
        })
                .addOnSuccessListener(this,
                        aVoid -> {
                            showMessage(getString(R.string.content_updated));
                            finish();
                        })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Unable to update contents", e);
                    showMessage(getString(R.string.content_update_failed));
                    finish();
                });
        // [END drive_android_rewrite_contents]
    }
}
