/**
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.cw.youlite.operation.gdrive;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.cw.youlite.R;
import com.cw.youlite.util.Util;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import org.json.JSONException;

import java.util.Collections;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * The main {@link Activity} for the Drive API migration sample app.
 *
 *  Modified: 2020/12/29
 *    - open Google drive file directly by file picker
 *    - overwrite it with all JSON content
 */
public class GDriveAct extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final int REQUEST_CODE_SIGN_IN = 1;
    private static final int REQUEST_CODE_OPEN_DOCUMENT = 2;

    private DriveServiceHelper mDriveServiceHelper;
//    private String mOpenFileId;

    private EditText mFileTitleEditText;
    private EditText mDocContentEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gdrive_main);

        // Store the EditText boxes to be updated when files are opened/created/modified.
        mFileTitleEditText = findViewById(R.id.file_title_edittext);
        mDocContentEditText = findViewById(R.id.doc_content_edittext);

        // open dialog for entering password
        // set view to dialog
        AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.check_password, null);
        dlgBuilder.setTitle("Check password");

        dlgBuilder.setView(dialogView);
        final AlertDialog dialog1 = dlgBuilder.create();
        dialog1.show();

        // cancel button
        Button btnCancel = (Button) dialogView.findViewById(R.id.check_password_cancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog1.dismiss();
            }
        });

        // continue button
        Button btnContinue = (Button) dialogView.findViewById(R.id.check_password_ok);
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //hide the menu
                dialog1.dismiss();

                // Set the onClick listeners for the button bar.
                findViewById(R.id.cancel_btn).setOnClickListener(view -> exit());
                findViewById(R.id.overwrite_btn).setOnClickListener(view -> overwriteFileFromFilePickerUri(getUri()));

                // Authenticate the user. For most apps, this should be done when the user performs an
                // action that requires Drive access rather than in onCreate.
                requestSignIn();
            }
        });

    }

    void exit() {
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        Log.d(TAG, "requestCode = " + requestCode);
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    handleSignInResult(resultData);
                }
                break;

            case REQUEST_CODE_OPEN_DOCUMENT:
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                      Uri uri = resultData.getData();
                      if (uri != null) {
                          openFileFromFilePicker(uri);
                      }
                      else
                          Log.d(TAG, "uri = null");
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, resultData);
    }

    /**
     * Starts a sign-in activity using {@link #REQUEST_CODE_SIGN_IN}.
     */
    private void requestSignIn() {
        Log.d(TAG, "Requesting sign-in");

        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                        .build();
        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);

        // The result of the sign-in Intent is handled in onActivityResult.
        startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    /**
     * Handles the {@code result} of a completed sign-in activity initiated from {@link
     * #requestSignIn()}.
     */
    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(googleAccount -> {
                        Log.d(TAG, "Signed in as " + googleAccount.getEmail());

                        // Use the authenticated account to sign in to the Drive service.
                        GoogleAccountCredential credential =
                            GoogleAccountCredential.usingOAuth2(
                                    this, Collections.singleton(DriveScopes.DRIVE_FILE));
                        credential.setSelectedAccount(googleAccount.getAccount());

                        Drive googleDriveService =
                            new Drive.Builder(
                                    new NetHttpTransport(),
                                    new GsonFactory(),
                                    credential)
                               .setApplicationName("Drive API Migration")
                               .build();

                        // The DriveServiceHelper encapsulates all REST API and SAF functionality.
                        // Its instantiation is required before handling any onClick actions.
                        mDriveServiceHelper = new DriveServiceHelper(googleDriveService);

                        // open file picker
                        openFilePicker();
                    })
                .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));
    }

    /**
     * Opens the Storage Access Framework file picker using {@link #REQUEST_CODE_OPEN_DOCUMENT}.
     */
    private void openFilePicker() {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Opening file picker.");

            Intent pickerIntent = mDriveServiceHelper.createFilePickerIntent();
            // The result of the SAF Intent is handled in onActivityResult.
            startActivityForResult(pickerIntent, REQUEST_CODE_OPEN_DOCUMENT);
        }
    }

    // open file with SAF, without using file Id
    Uri uri;

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    /**
     * Opens a file from its {@code uri} returned from the Storage Access Framework file picker
     * initiated by {@link #openFilePicker()}.
     */
    private void openFileFromFilePicker(Uri uri) {

        setUri(uri);

        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Opening " + uri.getPath());

            mDriveServiceHelper.openFileUsingStorageAccessFramework(getContentResolver(), uri)
                    .addOnSuccessListener(nameAndContent -> {
                            String name = nameAndContent.first;
                            String content = nameAndContent.second;

                            mFileTitleEditText.setText(name);
                            mDocContentEditText.setText(content);

                            // Files opened through SAF cannot be modified.
                            setReadOnlyMode();
                        })
                    .addOnFailureListener(exception ->
                            Log.e(TAG, "Unable to open file from picker.", exception));
        }
    }

    /**
     *  overwrite file form file picker Uri
     * */
    private void overwriteFileFromFilePickerUri(Uri uri) {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "overwriteFileFromFilePicker " + uri.getPath());

            // write content to picker file
            String allJsonStr = null;
            Util util = new Util(this);
            try {
                allJsonStr = util.getAllJson();

                // show overwritten content
                mDocContentEditText.setText(allJsonStr);

                mFileTitleEditText.setEnabled(true);
                mDocContentEditText.setEnabled(true);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            mDriveServiceHelper.writeToFileUsingStorageAccessFramework(getContentResolver(),uri,allJsonStr);
        }
    }



    /**
     * Creates a new file via the Drive REST API.
     */
//    private void createFile() {
//        if (mDriveServiceHelper != null) {
//            Log.d(TAG, "Creating a file.");
//
//            mDriveServiceHelper.createFile()
//                    .addOnSuccessListener(fileId -> readFile(fileId))
//                    .addOnFailureListener(exception ->
//                            Log.e(TAG, "Couldn't create file.", exception));
//        }
//    }

    /**
     * Retrieves the title and content of a file identified by {@code fileId} and populates the UI.
     */
//    private void readFile(String fileId) {
//        if (mDriveServiceHelper != null) {
//            Log.d(TAG, "Reading file " + fileId);
//
//            mDriveServiceHelper.readFile(fileId)
//                    .addOnSuccessListener(nameAndContent -> {
//                            String name = nameAndContent.first;
//                            String content = nameAndContent.second;
//
//                            mFileTitleEditText.setText(name);
//                            mDocContentEditText.setText(content);
//
//                            setReadWriteMode(fileId);
//                        })
//                    .addOnFailureListener(exception ->
//                            Log.e(TAG, "Couldn't read file.", exception));
//        }
//    }

    /**
     * Saves the currently opened file created via {@link #createFile()} if one exists.
     */
//    private void saveFile() {
//        if (mDriveServiceHelper != null && mOpenFileId != null) {
//            Log.d(TAG, "Saving " + mOpenFileId);
//
//            String fileName = mFileTitleEditText.getText().toString();
//            String fileContent = mDocContentEditText.getText().toString();
//
//            mDriveServiceHelper.saveFile(mOpenFileId, fileName, fileContent)
//                    .addOnFailureListener(exception ->
//                            Log.e(TAG, "Unable to save file via REST.", exception));
//        }
//    }

    /**
     * Queries the Drive REST API for files visible to this app and lists them in the content view.
     */
//    private void query() {
//        if (mDriveServiceHelper != null) {
//            Log.d(TAG, "Querying for files.");
//
//            mDriveServiceHelper.queryFiles()
//                .addOnSuccessListener(fileList -> {
//                        StringBuilder builder = new StringBuilder();
//                        for (File file : fileList.getFiles()) {
//                            builder.append(file.getName()).append("\n");
//                        }
//                        String fileNames = builder.toString();
//
//                        mFileTitleEditText.setText("File List");
//                        mDocContentEditText.setText(fileNames);
//
//                        setReadOnlyMode();
//                    })
//                .addOnFailureListener(exception -> Log.e(TAG, "Unable to query files.", exception));
//        }
//    }

    /**
     * Updates the UI to read-only mode.
     */
    private void setReadOnlyMode() {
        mFileTitleEditText.setEnabled(false);
        mDocContentEditText.setEnabled(false);
//        mOpenFileId = null;
    }

    /**
     * Updates the UI to read/write mode on the document identified by {@code fileId}.
     */
//    private void setReadWriteMode(String fileId) {
//        mFileTitleEditText.setEnabled(true);
//        mDocContentEditText.setEnabled(true);
//        mOpenFileId = fileId;
//    }
}
