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

package com.cw.youlite.operation.gdrive;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.cw.youlite.R;
import com.cw.youlite.util.Util;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.gson.Gson;

import org.json.JSONException;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

// modified on 2020/1/28
//
// Why is this migration needed?
// https://developers.google.com/drive/android/deprecation
// This API is deprecated.
// Clients must migrate to the Drive REST API or another suitable solution
// to avoid disruptions to your application.
//
// GitHub path 1:
// https://github.com/googleworkspace/android-samples/tree/master/drive/deprecation/app/src/main/java/com/google/android/gms/drive/sample
//
// GitHub path 2:
// https://github.com/ammarptn/GDrive-Rest-Android
//

/**
 * The main {@link Activity} for the Drive API migration sample app.
 */
public class ExportGDriveAct extends AppCompatActivity {
    private static final String TAG = "ExportGDriveAct";

    private static final int REQUEST_CODE_SIGN_IN = 1;
    private DriveServiceHelper mDriveServiceHelper;
    private EditText mFileTitleEditText;
    private EditText mDocContentEditText;
    String jsonContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.export_gdrive_json);

        // Store the EditText boxes to be updated when files are opened/created/modified.
        mFileTitleEditText = findViewById(R.id.file_title_edittext);
        mDocContentEditText = findViewById(R.id.doc_content_edittext);

        // Set the onClick listeners for the button bar.
        findViewById(R.id.cancel_btn).setOnClickListener(view -> exit());
        findViewById(R.id.export_btn).setOnClickListener(view -> exportConfirm());

        // Authenticate the user. For most apps, this should be done when the user performs an
        // action that requires Drive access rather than in onCreate.
        requestSignIn();

        // JSON file content
        Util util = new Util(this);
        jsonContent = null;
        try {
            jsonContent = util.getAllJson();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mDocContentEditText.setText(jsonContent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    handleSignInResult(resultData);
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
                               .setApplicationName("ExportGDriveAct")
                               .build();

                        // The DriveServiceHelper encapsulates all REST API and SAF functionality.
                        // Its instantiation is required before handling any onClick actions.
                        mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
                    })
                .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));
    }

    /**
     * Exit
     */
    private void exit() {
        Log.d(TAG, "Exit Export Json.");
        finish();
    }

    // open file with SAF, without using file Id
    Uri uri;

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }


    // Export Confirm
    void exportConfirm() {
        // dialog for confirming
        AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.confirm_gdrive_json, null);
        dlgBuilder.setTitle(R.string.confirm_dialog_title);
        dlgBuilder.setMessage(R.string.toast_export_gdrive_json);

        dlgBuilder.setView(dialogView);
        final AlertDialog dialog1 = dlgBuilder.create();
        dialog1.show();

        // cancel button
        Button btnCancel = (Button) dialogView.findViewById(R.id.cancel_gdrive_json);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog1.dismiss();
            }
        });

        // continue button
        Button btnContinue = (Button) dialogView.findViewById(R.id.confirm_gdrive_json);
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //hide the menu
                dialog1.dismiss();

                // save JSON to Google Drive
//                createJsonFile();

                Log.d(TAG, "click continue button");
                if (mDriveServiceHelper == null) {
                    return;
                }

                // JSON folder name on Google Drive
                String folderName = "YouLiteJson";

                // search folder name first
                mDriveServiceHelper.searchFolder(folderName)
                    .addOnSuccessListener(new OnSuccessListener<List<GoogleDriveFileHolder>>() {
                        @Override
                        public void onSuccess(List<GoogleDriveFileHolder> googleDriveFileHolders) {
                            Gson gson = new Gson();
                            Log.d(TAG, "search Json folder onSuccess: " + gson.toJson(googleDriveFileHolders));
                            // create JSON file in target folder
                            createJsonFile_targetFolder(folderName, googleDriveFileHolders);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "search Json folder onFailure: " + e.getMessage());
                            // create JSON file in new folder
                            createJsonFile_newFolder(folderName);
                        }
                    });
            } // onClick
        }); // setOnClickListener
    }// exportConfirm

    //
    // create JSON file in target folder
    //
    void createJsonFile_targetFolder(String folderName, List<GoogleDriveFileHolder> googleDriveFileHolders) {
        String destination_id = null;
        // check folder info
        Gson gson = new Gson();
        for(int i=0;i<googleDriveFileHolders.size();i++) {
            String jsonStr = gson.toJson(googleDriveFileHolders.get(i));
            FolderInfo folderInfoStr = gson.fromJson(jsonStr, FolderInfo.class);
            System.out.println(TAG + "createFileInJsonFolder  id=" +  folderInfoStr.id
                    + " name=" +folderInfoStr.name);

            if(folderInfoStr.name.equalsIgnoreCase(folderName)) {
                destination_id = folderInfoStr.id;
                break;
            }
        }

        if(destination_id == null) // includes [] case
            createJsonFile_newFolder(folderName);
        else {
            String fileContent = mDocContentEditText.getText().toString();
            String fileName = mFileTitleEditText.getText().toString().concat(".json");
            // existing folder case
            mDriveServiceHelper.createJsonFile(fileName, fileContent, destination_id)
                    .addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
                        @Override
                        public void onSuccess(GoogleDriveFileHolder googleDriveFileHolder) {
                            Gson gson = new Gson();
                            exportGDriveJsonSuccess();
                            Log.d(TAG, "create Json file with folder ID onSuccess: " + gson.toJson(googleDriveFileHolder));
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "create Json text file with folder ID onFailure: " + e.getMessage());
                        }
                    });
        }

    }

    //
    // create JSON file folder in new folder
    //
    void createJsonFile_newFolder(String folderName) {
        mDriveServiceHelper.createFolder(folderName, null)
                .addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
                    @Override
                    public void onSuccess(GoogleDriveFileHolder googleDriveFileHolder) {
                        Gson gson = new Gson();
                        Log.d(TAG, "createJsonFolderAndFile onSuccess: " + gson.toJson(googleDriveFileHolder));

                        // check folder info
                        String jsonStr = gson.toJson(googleDriveFileHolder);
                        FolderInfo folderInfoStr = gson.fromJson(jsonStr, FolderInfo.class);
                        String fileContent = mDocContentEditText.getText().toString();
                        String fileName = mFileTitleEditText.getText().toString();
                        mDriveServiceHelper.createJsonFile(fileName, fileContent,  folderInfoStr.id);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "createJsonFolderAndFile onFailure: " + e.getMessage());

                    }
                });
    }

    // toast for Export JSON successfully
    private void exportGDriveJsonSuccess() {
        Toast.makeText(this,R.string.exported_successfully,Toast.LENGTH_SHORT).show();
    }

    // save Json file
//    private void saveJsonFile() {
//        if (mDriveServiceHelper != null && mOpenFileId != null) {
//            Log.d(TAG, "Saving " + mOpenFileId);
//
//            String fileName = mFileTitleEditText.getText().toString();
//            String fileContent = mDocContentEditText.getText().toString();
//
//            mDriveServiceHelper.saveJsonFile(mOpenFileId, fileName, fileContent)
//                    .addOnFailureListener(exception ->
//                            Log.e(TAG, "Unable to save file via REST.", exception))
//                    .addOnSuccessListener(nameAndContent -> exportGDriveJsonSuccess());
//        }
//    }

    /**
     * Creates a new JSON file via the Drive REST API.
     */
//    private void createJsonFile() {
//        if (mDriveServiceHelper != null) {
//            Log.d(TAG, "Creating a JSON file.");
//
//            // file title
//            String file_title = mFileTitleEditText.getText().toString();
//
//            final String file_content = jsonContent;
//
//            mDriveServiceHelper.createJsonFile(file_title,file_content)
//                    .addOnSuccessListener(file_Id ->  exportGDriveJsonSuccess())
//                    .addOnFailureListener(exception ->
//                            Log.e(TAG, "Couldn't create JSON file.", exception));
//        }
//    }

    /**
     * Retrieves the JSON title and content of a file identified by {@code fileId} and populates the UI.
     */
//    private void readJsonFile_and_save(String file_Id, String jsonContent) {
//        if (mDriveServiceHelper != null) {
//            Log.d(TAG, "Reading file " + file_Id);
//
//            mDriveServiceHelper.readFile(file_Id)
//                    .addOnSuccessListener(nameAndContent -> {
//                            String name = nameAndContent.first;
//                            String content = nameAndContent.second;
//
//                            mFileTitleEditText.setText(name);
//                            mDocContentEditText.setText(jsonContent);
//
//                            setReadWriteMode(file_Id);
//
//                            //Save
//                            saveJsonFile();
//                        })
//                    .addOnFailureListener(exception ->
//                            Log.e(TAG, "Couldn't read file.", exception));
//        }
//    }

    /**
     * Updates the UI to read-only mode.
     */
//    private void setReadOnlyMode() {
//        mFileTitleEditText.setEnabled(false);
//        mDocContentEditText.setEnabled(false);
//        mOpenFileId = null;
//    }

    /**
     * Updates the UI to read/write mode on the document identified by {@code fileId}.
     */
//    private void setReadWriteMode(String fileId) {
//        mFileTitleEditText.setEnabled(true);
//        mDocContentEditText.setEnabled(true);
//        mOpenFileId = fileId;
//    }

}