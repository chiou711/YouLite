/*
 * Copyright (c) 2016 The Android Open Source Project
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

package com.cw.youlite.data;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.cw.youlite.operation.import_export.ParseJsonToDB;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * FetchVideoService is responsible for fetching the videos from the Internet and inserting the
 * results into a local SQLite database.
 */
public class FetchService_category extends IntentService {
    private static final String TAG = "FetchService_category";
    public static String serviceUrl;

    /**
     * Creates an IntentService with a default name for the worker thread.
     */
    public FetchService_category() {
        super(TAG);
        System.out.println("FetchService_category / constructor / serviceUrl = " + serviceUrl);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        serviceUrl = workIntent.getStringExtra("FetchUrl");
        System.out.println("FetchService_category / _onHandleIntent / serviceUrl = " + serviceUrl);
	    DbBuilder_category builder = new DbBuilder_category(getApplicationContext());

        try {
            JSONObject jsonObj = builder.fetchJasonObject(serviceUrl);

	        ParseJsonToDB importObject = new ParseJsonToDB(this);
	        importObject.parseJsonAndInsertDB(jsonObj);//??? how to avoid DB exception
	        while (importObject.isParsing) ;

        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error occurred in downloading videos");
            e.printStackTrace();
        }

        // Puts the status into the Intent
        String status = "FetchCategoryServiceIsDone"; // any data that you want to send back to receivers
        Intent localIntent =  new Intent(Constants.BROADCAST_ACTION);
        localIntent.putExtra(Constants.EXTENDED_DATA_STATUS, status);

        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
        System.out.println("FetchService_category / _onHandleIntent / sendBroadcast");
    }

    public final class Constants {
        // Defines a custom Intent action
        public static final String BROADCAST_ACTION = "com.cw.youlite.BROADCAST";
        // Defines the key for the status "extra" in an Intent
        public static final String EXTENDED_DATA_STATUS = "com.cw.youlite.STATUS";
    }
}