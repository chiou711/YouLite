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

package com.cw.youlite.operation.import_export;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import com.cw.youlite.R;
import com.cw.youlite.util.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class Import_webJsonAct extends AppCompatActivity
{
    String content = null;
    WebView webView;
    Button btn_import;
    // TODO Website path customization: input path, website rule for Import
    String homeUrl = "https://youlite-app.blogspot.com/2019/09/json.html";
    boolean isReady;

    // issue:
    //     java.lang.RuntimeException:
    //     Unable to start activity ComponentInfo{com.cw.litenote/com.cw.litenote.operation.import_export.Import_webJsonAct}:
    //     android.view.InflateException: Binary XML file line #12: Error inflating class android.webkit.WebView
    // fix:
    //     https://stackoverflow.com/questions/41025200/android-view-inflateexception-error-inflating-class-android-webkit-webview
    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //Add API30 permission check
        if(Build.VERSION.SDK_INT >= 30)
            checkStorageManagerPermission();
        else if(Build.VERSION.SDK_INT >= 23){
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED){
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                                                  new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                               Manifest.permission.READ_EXTERNAL_STORAGE},
                                                  Util.PERMISSIONS_REQUEST_STORAGE);
            } else
                doCreate();
        } else
            doCreate();
    }

    public void checkStorageManagerPermission() {
        if (!Environment.isExternalStorageManager()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            startActivityForResult(intent,Util.STORAGE_MANAGER_PERMISSION);

            // flow of this query:
            // Act / _onPause / _onStop
            // this query UI
            // onActivityResult
            // Act / _onStart / _onResume
        } else
            doCreate();
    }

    void doCreate()
    {
        setContentView(R.layout.import_web);

        // web view
        webView = (WebView)findViewById(R.id.webView);

        // cancel button
        Button btn_cancel = (Button) findViewById(R.id.import_web_cancel);
        btn_cancel.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                if (webView.canGoBack()) {
                    webView.goBack();
                    content = null;
                }
                else
                    finish();
            }
        });

        // import button
        btn_import = (Button) findViewById(R.id.import_web_import);
        btn_import.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                setResult(RESULT_OK);
                webView.loadUrl("javascript:window.YouLite.processContent(document.getElementsByTagName('body')[0].innerText);");
            }

        });

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setCacheMode( WebSettings.LOAD_NO_CACHE);

        // create instance
        final ImportInterface import_interface = new ImportInterface(webView);

        // load first web content
        // keyword YouLite will be used in javascript:window.YouLite.processContent(...
        webView.addJavascriptInterface(import_interface, "YouLite");

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                btn_import.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url)
            {
                System.out.println("Import_webJsonAct / _setWebViewClient / url = " + url);

                // todo: Need to modify the keyword below when json file destination changed
                String keywordStr = "drive.google.com/file";
                // enable Import if url path is directed to json file folder
                if(url.contains(keywordStr)){
                    btn_import.setVisibility(View.VISIBLE);
                    isReady = true;
                }

            }

        });

        // load content to web view
        webView.loadUrl(homeUrl);

        // init isReady flag
        isReady = false;
    }

    // callback of granted permission
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        System.out.println("grantResults.length =" + grantResults.length);
        switch (requestCode)
        {
            case Util.PERMISSIONS_REQUEST_STORAGE:
            {
                // If request is cancelled, the result arrays are empty.
                if ( (grantResults.length > 0) &&
                     ( (grantResults[0] == PackageManager.PERMISSION_GRANTED) &&
                       (grantResults[1] == PackageManager.PERMISSION_GRANTED)    ))
                    doCreate();
                else
                    finish();
            }//case
            break;

            case Util.STORAGE_MANAGER_PERMISSION:
                    if(Environment.isExternalStorageManager()){
                        doCreate();
                    }
            break;
        }//switch
    }

    @Override
    public void onBackPressed() {
        System.out.println("Import_webJsonAct / _onBackPressed");
        // web view can go back
        if (webView.canGoBack()) {
            webView.goBack();
            content = null;
        }
        else
            super.onBackPressed();

        ParseJsonToDB.isParsing = false;
    }

    /* An instance of this class will be registered as a JavaScript interface */
    class ImportInterface {

        ImportInterface(WebView _webView)
        {
            webView = _webView;
        }

        Runnable run;

        // process HTML content: save file, parsing, save DB
        @SuppressWarnings("unused")
        @android.webkit.JavascriptInterface
        public void processContent(final String _content)
        {
            run = new Runnable() {
                @Override
                public void run() {
                    content = _content;
                    int size = content.length();
                    System.out.println("Import_webJsonAct / content size = "+ size);
                    System.out.println("Import_webJsonAct / content = "+ content);

                    if(!isReady)
                        return;

                    // load web view content to update content string
                    webView.loadUrl("javascript:window.YouLite.processContent(document.getElementsByTagName('body')[0].innerText);");

                    // import
                    // save text in a file
                    String dirName = "Download";
                    String fileName = "temp.json";
                    String dirPath = Environment.getExternalStorageDirectory().toString() +
                            "/" +
                            dirName;
                    File file = new File(dirPath, fileName);

                    try
                    {
                        try {
                            file.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        FileOutputStream fOut = new FileOutputStream(file);
                        OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);

                        if(content != null) {
                            content = content.replaceAll("(?m)^[ \t]*\r?\n", "");
                        }
                        myOutWriter.append(content);
                        myOutWriter.close();

                        fOut.flush();
                        fOut.close();
                    }
                    catch (IOException e)
                    {
                        Log.e("Exception", "File write failed: " + e.toString());
                    }

                    // import file content to DB
                    Import_webJsonAct_asyncTask task = new Import_webJsonAct_asyncTask(Import_webJsonAct.this,file.getPath());
                    task.enableSaveDB(true);// import
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                    // import done, reset isReady flag
                    isReady = false;
                }
            };

            webView.post(run);
        }

        // note: this is used by home URL web page
        @android.webkit.JavascriptInterface
        public void showToast(String toastText) {
            Toast.makeText(Import_webJsonAct.this, toastText, Toast.LENGTH_LONG).show();
        }

    }
}