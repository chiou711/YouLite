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
 */
package com.cw.youlite.operation.GDrvTest1;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

import com.cw.youlite.R;
import com.cw.youlite.util.Util;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import org.json.JSONException;

import java.util.Collections;

import androidx.appcompat.app.AppCompatActivity;

// Why is this migration needed?
// https://developers.google.com/drive/android/deprecation
// This API is deprecated.
// Clients must migrate to the Drive REST API or another suitable solution
// to avoid disruptions to your application.
//
// GitHub path:
// https://github.com/googleworkspace/android-samples/tree/master/drive/deprecation/app/src/main/java/com/google/android/gms/drive/sample
//

/**
 * The main {@link Activity} for the Drive API migration sample app.
 */
public class MainActivity1 extends AppCompatActivity {
    private static final String TAG = "GDrv";

    private static final int REQUEST_CODE_SIGN_IN = 1;
    private static final int REQUEST_CODE_OPEN_DOCUMENT = 2;

    private DriveServiceHelper mDriveServiceHelper;
    private String mOpenFileId;

    private EditText mFileTitleEditText;
    private EditText mDocContentEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main1);

        // Store the EditText boxes to be updated when files are opened/created/modified.
        mFileTitleEditText = findViewById(R.id.file_title_edittext);
        mDocContentEditText = findViewById(R.id.doc_content_edittext);

        // Set the onClick listeners for the button bar.
        findViewById(R.id.open_btn).setOnClickListener(view -> openFilePicker());

//        findViewById(R.id.create_btn).setOnClickListener(view -> createFile());
        findViewById(R.id.create_btn).setOnClickListener(view -> createJsonFile());

//        findViewById(R.id.save_btn).setOnClickListener(view -> saveFile());
        findViewById(R.id.save_btn).setOnClickListener(view -> saveJsonFile());

        findViewById(R.id.query_btn).setOnClickListener(view -> query());

        // Authenticate the user. For most apps, this should be done when the user performs an
        // action that requires Drive access rather than in onCreate.
        requestSignIn();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
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
                          setUri(uri);
                          openFileFromFilePicker(uri);
                      }
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
                                    AndroidHttp.newCompatibleTransport(),
                                    new GsonFactory(),
                                    credential)
                               .setApplicationName("Drive API Migration")
                               .build();

                        // The DriveServiceHelper encapsulates all REST API and SAF functionality.
                        // Its instantiation is required before handling any onClick actions.
                        mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
                    })
                .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));
    }

    /**
     * Opens the Storage Access Framework file picker using {@link #REQUEST_CODE_OPEN_DOCUMENT}.
     */
    private void openFilePicker() {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Opening file picker.");

//            Intent pickerIntent = mDriveServiceHelper.createFilePickerIntent();
            Intent pickerIntent = mDriveServiceHelper.createJsonFilePickerIntent();

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
     *  overwrite file form file picker Uri
     * */
    //todo Add
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
     * Opens a file from its {@code uri} returned from the Storage Access Framework file picker
     * initiated by {@link #openFilePicker()}.
     */
    private void openFileFromFilePicker(Uri uri) {
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
     * Creates a new file via the Drive REST API.
     */
    private void createFile() {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Creating a file.");

            mDriveServiceHelper.createFile()
                    .addOnSuccessListener(fileId -> readFile(fileId))
                    .addOnFailureListener(exception ->
                            Log.e(TAG, "Couldn't create file.", exception));
        }
    }

    /**
     * Creates a new JSON file via the Drive REST API.
     */
    private void createJsonFile() {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Creating a JSON file.");

            // file title
            String file_title = mFileTitleEditText.getText().toString();

            // JSON file content
            Util util = new Util(this);
            String jsonContent = null;
            try {
                jsonContent = util.getAllJson();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // todo temp
//            final String file_content = "jsonContent";
            final String file_content = jsonContent;

            mDriveServiceHelper.createJsonFile(file_title)
                    .addOnSuccessListener(file_Id -> readJsonFile_and_save(file_Id,file_content))
                    .addOnFailureListener(exception ->
                            Log.e(TAG, "Couldn't create JSON file.", exception));
        }
    }

    /**
     * Retrieves the title and content of a file identified by {@code fileId} and populates the UI.
     */
    private void readFile(String fileId) {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Reading file " + fileId);

            mDriveServiceHelper.readFile(fileId)
                    .addOnSuccessListener(nameAndContent -> {
                            String name = nameAndContent.first;
                            String content = nameAndContent.second;

                            mFileTitleEditText.setText(name);
                            mDocContentEditText.setText(content);

                            setReadWriteMode(fileId);
                        })
                    .addOnFailureListener(exception ->
                            Log.e(TAG, "Couldn't read file.", exception));
        }
    }

    /**
     * Retrieves the JSON title and content of a file identified by {@code fileId} and populates the UI.
     */
    private void readJsonFile_and_save(String file_Id, String jsonContent) {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Reading file " + file_Id);

            mDriveServiceHelper.readFile(file_Id)
                    .addOnSuccessListener(nameAndContent -> {
                            String name = nameAndContent.first;
                            String content = nameAndContent.second;

                            mFileTitleEditText.setText(name);
//                            mDocContentEditText.setText(content);
                            mDocContentEditText.setText(jsonContent);

                            setReadWriteMode(file_Id);

                            //todo One button Save?
                            saveJsonFile();
                        })
                    .addOnFailureListener(exception ->
                            Log.e(TAG, "Couldn't read file.", exception));
        }
    }

    /**
     * Saves the currently opened file created via {@link #createJsonFile()} if one exists.
     */
    private void saveFile() {
        if (mDriveServiceHelper != null && mOpenFileId != null) {
            Log.d(TAG, "Saving " + mOpenFileId);

            String fileName = mFileTitleEditText.getText().toString();
            String fileContent = mDocContentEditText.getText().toString();

            mDriveServiceHelper.saveFile(mOpenFileId, fileName, fileContent)
                    .addOnFailureListener(exception ->
                            Log.e(TAG, "Unable to save file via REST.", exception));
        }
    }

    private void saveJsonFile() {
        if (mDriveServiceHelper != null && mOpenFileId != null) {
            Log.d(TAG, "Saving " + mOpenFileId);

            String fileName = mFileTitleEditText.getText().toString();
            String fileContent = mDocContentEditText.getText().toString();

            mDriveServiceHelper.saveJsonFile(mOpenFileId, fileName, fileContent)
                    .addOnFailureListener(exception ->
                            Log.e(TAG, "Unable to save file via REST.", exception));
        } else if(mDriveServiceHelper != null && mOpenFileId == null) {
            Log.d(TAG, "Saving mOpenFileId = null");
            // todo Overwrite
        } else {
            Log.d(TAG, "mDriveServiceHelper = null ,Saving mOpenFileId = null");
        }
    }


    /**
     * Queries the Drive REST API for files visible to this app and lists them in the content view.
     */
    private void query() {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Querying for files.");

            mDriveServiceHelper.queryFiles()
                .addOnSuccessListener(fileList -> {
                        StringBuilder builder = new StringBuilder();
                        for (File file : fileList.getFiles()) {
                            builder.append(file.getName()).append("\n");
                        }
                        String fileNames = builder.toString();

                        mFileTitleEditText.setText("File List");
                        mDocContentEditText.setText(fileNames);

                        setReadOnlyMode();
                    })
                .addOnFailureListener(exception -> Log.e(TAG, "Unable to query files.", exception));
        }
    }

    /**
     * Updates the UI to read-only mode.
     */
    private void setReadOnlyMode() {
        mFileTitleEditText.setEnabled(false);
        mDocContentEditText.setEnabled(false);
        mOpenFileId = null;
    }

    /**
     * Updates the UI to read/write mode on the document identified by {@code fileId}.
     */
    private void setReadWriteMode(String fileId) {
        mFileTitleEditText.setEnabled(true);
        mDocContentEditText.setEnabled(true);
        mOpenFileId = fileId;
    }
}
