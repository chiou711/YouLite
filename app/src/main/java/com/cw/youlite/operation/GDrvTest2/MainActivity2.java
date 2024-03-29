package com.cw.youlite.operation.GDrvTest2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cw.youlite.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.services.drive.DriveScopes;
import com.google.gson.Gson;
//import com.google.android.gms.drive.Drive;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import static com.cw.youlite.operation.GDrvTest2.DriveServiceHelper.getGoogleDriveService;

// refer to  https://github.com/ammarptn/GDrive-Rest-Android
public class MainActivity2 extends AppCompatActivity {

    private static final int REQUEST_CODE_SIGN_IN = 100;
    private GoogleSignInClient mGoogleSignInClient;
    private DriveServiceHelper mDriveServiceHelper;
    private static final String TAG = "MainActivity2";
    private Button login;
    private LinearLayout gDriveAction;
    private Button searchFile;
    private Button searchFolder;
    private Button createTextFile;
    private Button createJsonFile;
    private Button createFolder;
    private Button uploadFile;
    private Button downloadFile;
    private Button deleteFileFolder;
    private TextView email;
    private Button viewFileFolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        initView();


        searchFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDriveServiceHelper == null) {
                    return;
                }
                mDriveServiceHelper.searchFile("textfilename.txt", "text/plain")
                        .addOnSuccessListener(new OnSuccessListener<List<GoogleDriveFileHolder>>() {
                            @Override
                            public void onSuccess(List<GoogleDriveFileHolder> googleDriveFileHolders) {

                                Gson gson = new Gson();
                                Log.d(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolders));
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG, "onFailure: " + e.getMessage());
                            }
                        });

            }
        });

        searchFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDriveServiceHelper == null) {
                    return;
                }

                mDriveServiceHelper.searchFolder("YouLiteJson")
                        .addOnSuccessListener(new OnSuccessListener<List<GoogleDriveFileHolder>>() {
                            @Override
                            public void onSuccess(List<GoogleDriveFileHolder> googleDriveFileHolders) {
                                Gson gson = new Gson();

                                // check folder info
                                for(int i=0;i<googleDriveFileHolders.size();i++) {
                                    String jsonStr = gson.toJson(googleDriveFileHolders.get(i));
                                    FolderInfo folderInfoStr = gson.fromJson(jsonStr, FolderInfo.class);
                                    System.out.println(TAG + " id=" +  folderInfoStr.id
                                                                            + " name=" +folderInfoStr.name);
                                }

                                Log.d(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolders));
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG, "onFailure: " + e.getMessage());
                            }
                        });
            }
        });

        createTextFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "click createTextFile");
                if (mDriveServiceHelper == null) {
                    return;
                }
                // you can provide  folder id in case you want to save this file inside some folder.
                // if folder id is null, it will save file to the root
                mDriveServiceHelper.createTextFile("textfilename.txt", "some text", null)
                        .addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
                            @Override
                            public void onSuccess(GoogleDriveFileHolder googleDriveFileHolder) {
                                Gson gson = new Gson();
                                Log.d(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolder));
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG, "onFailure: " + e.getMessage());
                            }
                        });
            }
        });

        createJsonFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "click createJsonFile");
                if (mDriveServiceHelper == null) {
                    return;
                }

                // JSON folder name
                String folderName = "YouLiteJson";

                mDriveServiceHelper.searchFolder(folderName)
                        .addOnSuccessListener(new OnSuccessListener<List<GoogleDriveFileHolder>>() {
                            @Override
                            public void onSuccess(List<GoogleDriveFileHolder> googleDriveFileHolders) {
                                Gson gson = new Gson();
                                Log.d(TAG, "search Json folder onSuccess: " + gson.toJson(googleDriveFileHolders));

                                createJsonFile_targetFolder(folderName, googleDriveFileHolders);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG, "search Json folder onFailure: " + e.getMessage());

                                createJsonFile_newFolder(folderName);
                            }
                        });

            }
        });

        createFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDriveServiceHelper == null) {
                    return;
                }
                // you can provide  folder id in case you want to save this file inside some folder.
                // if folder id is null, it will save file to the root
                mDriveServiceHelper.createFolder("YouLiteJson", null)
                        .addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
                            @Override
                            public void onSuccess(GoogleDriveFileHolder googleDriveFileHolder) {
                                Gson gson = new Gson();
                                Log.d(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolder));

                                // check folder info, name is null here
                                String jsonStr = gson.toJson(googleDriveFileHolder);
                                FolderInfo folderInfoStr = gson.fromJson(jsonStr, FolderInfo.class);
                                System.out.println(TAG + " id=" +  folderInfoStr.id
                                        + " name=" +folderInfoStr.name);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG, "onFailure: " + e.getMessage());

                            }
                        });
            }
        });

        uploadFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mDriveServiceHelper == null) {
                    return;
                }
                mDriveServiceHelper.uploadFile(new java.io.File(getApplicationContext().getFilesDir(), "dummy.txt"), "text/plain", null)
                        .addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
                            @Override
                            public void onSuccess(GoogleDriveFileHolder googleDriveFileHolder) {
                                Gson gson = new Gson();
                                Log.d(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolder));
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG, "onFailure: " + e.getMessage());
                            }
                        });
            }
        });

        downloadFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDriveServiceHelper == null) {
                    return;
                }
                mDriveServiceHelper.downloadFile(new java.io.File(getApplicationContext().getFilesDir(), "filename.txt"), "google_drive_file_id_here")
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                            }
                        });
            }
        });

        viewFileFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDriveServiceHelper == null) {
                    return;
                }

                mDriveServiceHelper.queryFiles()
                        .addOnSuccessListener(new OnSuccessListener<List<GoogleDriveFileHolder>>() {
                            @Override
                            public void onSuccess(List<GoogleDriveFileHolder> googleDriveFileHolders) {
                                Gson gson = new Gson();
                                Log.d(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolders));
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                            }
                        });
//                Intent openActivity = new Intent(getApplicationContext(), GDriveDebugViewActivity.class);
//                startActivity(openActivity);
            }
        });

    }

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
            // existing folder case
            mDriveServiceHelper.createTextFile("JsonFileName.json", "JSON file content", destination_id)
                .addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
                    @Override
                    public void onSuccess(GoogleDriveFileHolder googleDriveFileHolder) {
                        Gson gson = new Gson();
                        Log.d(TAG, "create text file with folder ID onSuccess: " + gson.toJson(googleDriveFileHolder));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "create text file with folder ID onFailure: " + e.getMessage());
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
                        mDriveServiceHelper.createTextFile(folderName, "JSON file content",  folderInfoStr.id);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "createJsonFolderAndFile onFailure: " + e.getMessage());

                    }
                });

    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());

        if (account == null) {

            signIn();

        } else {


            email.setText(account.getEmail());

            mDriveServiceHelper = new DriveServiceHelper(getGoogleDriveService(getApplicationContext(), account, "appName"));
        }
    }

    private void signIn() {

        mGoogleSignInClient = buildGoogleSignInClient();
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    private GoogleSignInClient buildGoogleSignInClient() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                        .requestScopes(Drive.SCOPE_FILE)
                        .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                        .requestEmail()
                        .build();
        return GoogleSignIn.getClient(getApplicationContext(), signInOptions);
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

    public void test() {
        System.out.println("test");
    }

    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
                    @Override
                    public void onSuccess(GoogleSignInAccount googleSignInAccount) {
                        Log.d(TAG, "Signed in as " + googleSignInAccount.getEmail());
                        email.setText(googleSignInAccount.getEmail());

                        mDriveServiceHelper = new DriveServiceHelper(getGoogleDriveService(getApplicationContext(), googleSignInAccount, "appName"));

                        Log.d(TAG, "handleSignInResult: " + mDriveServiceHelper);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Unable to sign in.", e);
                    }
                });
    }

    private void initView() {

        email = findViewById(R.id.email);
        gDriveAction = findViewById(R.id.g_drive_action);
        searchFile = findViewById(R.id.search_file);
        searchFolder = findViewById(R.id.search_folder);
        createTextFile = findViewById(R.id.create_text_file);
        createJsonFile = findViewById(R.id.create_json_file);
        createFolder = findViewById(R.id.create_folder);
        uploadFile = findViewById(R.id.upload_file);
        downloadFile = findViewById(R.id.download_file);
        deleteFileFolder = findViewById(R.id.delete_file_folder);
        viewFileFolder = findViewById(R.id.view_file_folder);
    }
}
