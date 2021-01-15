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
import com.cw.youlite.folder.FolderUi;
import com.cw.youlite.main.MainAct;
import com.cw.youlite.operation.import_export.ParseJsonToDB;
import com.cw.youlite.util.preferences.Pref;
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
import org.json.JSONObject;

import java.util.Collections;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * The main {@link Activity} for the Drive API migration sample app.
 *
 *  Modified: 2021/1/15
 *    - open Google drive file directly by file picker
 *    - overwrite it with all JSON content
 */
public class ImportGDriveAct extends AppCompatActivity {
    private static final String TAG = "ImportGDriveAct";

    private static final int REQUEST_CODE_SIGN_IN = 1;
    private static final int REQUEST_CODE_OPEN_DOCUMENT = 2;

    private DriveServiceHelper mDriveServiceHelper;

    private EditText mFileTitleEditText;
    private EditText mDocContentEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.import_gdrive_json);

        // Store the EditText boxes to be updated when files are opened/created/modified.
        mFileTitleEditText = findViewById(R.id.file_title_edittext);
        mDocContentEditText = findViewById(R.id.doc_content_edittext);

        // Set the onClick listeners for the button bar.
        findViewById(R.id.cancel_btn).setOnClickListener(view -> exit());
        findViewById(R.id.import_btn).setOnClickListener(view -> importConfirm());

        // request sign in
        requestSignIn();
    }

    void exit() {
        finish();
    }

    // Import Confirm
    void importConfirm(){
        // dialog for confirming
        AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.confirm_gdrive_json, null);
        dlgBuilder.setTitle(R.string.confirm_dialog_title);
        dlgBuilder.setMessage(R.string.toast_import_gdrive_json);

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

                // import JSON
                importJson();
            }
        });
    }

    // import Google Drive JSON file content
    void importJson() {
        //import Json and Add to DB
        ParseJsonToDB importObject = new ParseJsonToDB(this);
        JSONObject jsonObj;

        try {
            jsonObj = new JSONObject(jsonContent);
            importObject.parseJsonAndInsertDB(jsonObj);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        while (importObject.isParsing) ;

        // after Import
        finish();

        Intent new_intent = new Intent(this, MainAct.class);
        new_intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK);
        new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);

        // reset focus view to default
        Pref.setPref_focusView_folder_tableId(this, 1);
        FolderUi.setFocus_folderPos(0);
        Pref.setPref_focusView_page_tableId(this, 1);

        startActivity(new_intent);
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
                               .setApplicationName("ImportGDriveAct")
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

    String jsonContent;
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

                            jsonContent = content;

                            mFileTitleEditText.setText(name);
                            mDocContentEditText.setText(content);

                            // Files opened through SAF cannot be modified.
                            //setReadOnlyMode();

                        })
                    .addOnFailureListener(exception ->
                            Log.e(TAG, "Unable to open file from picker.", exception));
        }
    }


    /**
     * Updates the UI to read-only mode.
     */
    private void setReadOnlyMode() {
        mFileTitleEditText.setEnabled(false);
        mDocContentEditText.setEnabled(false);
    }

}
