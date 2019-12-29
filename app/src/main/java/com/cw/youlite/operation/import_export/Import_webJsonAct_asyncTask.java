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

import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.cw.youlite.R;
import com.cw.youlite.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by cw on 2019/8/7
 * Modified by cw on 2019/11/9
 */
// Show progress progressBar
class Import_webJsonAct_asyncTask extends AsyncTask<Void, Integer, Void> {

    private ProgressBar progressBar;
    private boolean enableSaveDB;
    private AppCompatActivity act;
    private File file;
    private View contentBlock;
    String filePath;

    Import_webJsonAct_asyncTask(AppCompatActivity _act, String _filePath)
    {
        act = _act;
        Util.lockOrientation(act);

        contentBlock = act.findViewById(R.id.contentBlock);
        contentBlock.setVisibility(View.GONE);

        progressBar = (ProgressBar) act.findViewById(R.id.import_progress);
        progressBar.setVisibility(View.VISIBLE);

//        file = new File(_filePath);
        filePath = _filePath;
    }

    void enableSaveDB(boolean enable)
    {
        enableSaveDB = enable;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        if (this.progressBar != null ){
            progressBar.setProgress(values[0]);
        }
    }

    @Override
    protected Void doInBackground(Void... params)
    {
        insertSelectedFileContentToDB(enableSaveDB);
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);

        if(enableSaveDB)
        {
            contentBlock.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);

//				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            Util.unlockOrientation(act);
            Toast.makeText(act, R.string.toast_import_finished,Toast.LENGTH_SHORT).show();
        }
    }

    ParseXmlToDB importObject;
    private void insertSelectedFileContentToDB(boolean enableInsertDB)
    {
        ParseJsonToDB importObject = new ParseJsonToDB(filePath, act);

        if(enableInsertDB)
            importObject.handleParseJsonFileAndInsertDB();
        else
            importObject.handleViewJson();

        while (ParseJsonToDB.isParsing) {
            try { Thread.sleep(100); } catch (InterruptedException e) {}
        }
    }
}

