/*
 * Copyright (C) 2022 CW Chiu
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

package com.cw.youlite.util.playlist;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;

import com.cw.youlite.db.DB_page;
import com.cw.youlite.main.MainAct;
import com.cw.youlite.operation.youtube.YouTubeDeveloperKey;
import com.cw.youlite.util.Util;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

class PlaylistJsonAsync extends AsyncTask <String,Void,String> //Generic: Params, Progress, Result
{
	@SuppressLint("StaticFieldLeak")
	Activity act;
	int tableId;
	boolean isAdded_onNewIntent;

	// Video class
	static class Video{
		String url;
		String title;

		public Video(String url, String title) {
			this.url = url;
			this.title = title;
		}
	}

	List<Video> videos;

	// constructor
	public PlaylistJsonAsync(Activity _act, int _tableId,boolean _isAdded_onNewIntent) {
		this.act = _act;
		this.tableId = _tableId;
		this.isAdded_onNewIntent = _isAdded_onNewIntent;
	}

	@Override
	protected String doInBackground(String... youtubeUrl) {
		doRequest(youtubeUrl[0]);
		return null;
	}

    @Override
    protected void onPostExecute(String result) {
    	System.out.println("PlaylistJsonAsync / _onPostExecute / result (title)= " + result);

	    // insert links to DB
		DB_page dB_page = new DB_page(act, tableId);
	    dB_page.open();
		for(int i=0;i<videos.size();i++) {
			dB_page.insertNote_no_openClose(videos.get(i).title, "", videos.get(i).url, 0, (long) 0);// add new note, get return row Id
		}
	    dB_page.close();

	    // make sure DB update is OK
	    if(isAdded_onNewIntent) {
		    act.finish();
		    Intent intent = new Intent(act, MainAct.class);
		    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		    act.startActivity(intent);
	    } else
		    act.finish();

	    if(!this.isCancelled()) {
		    this.cancel(true);
	    }
    }


	// do request
	void doRequest(String youtubeUrl) {
		System.out.println("PlaylistJsonAsync / _doRequest");
		// get playlist ID
		String playlistIdStr = Util.getYoutubePlaylistId(youtubeUrl);

		try {
			String apiKey = YouTubeDeveloperKey.DEVELOPER_KEY;
			String prefixUrlStr = "https://youtube.googleapis.com/youtube/v3/playlistItems?" +
					"part=contentDetails" +
					"&part=snippet";
//					"&part=id" +
//					"&part=status";
			String nextPageToken = "";
			videos = new ArrayList<>();
			List<String> tokens = new ArrayList<>();
			boolean tokenIsRepeated = false;

			do {
				int repeatedTimes = 0;
				for (int i = 0; i < tokens.size(); i++) {
					if (!nextPageToken.isEmpty() && tokens.get(i).equalsIgnoreCase(nextPageToken)) {
						repeatedTimes++;

						if (repeatedTimes > 1)
							tokenIsRepeated = true;
					}
				}

				String reqStr = prefixUrlStr.concat("&playlistId=").concat(playlistIdStr)
						.concat("&key=").concat(apiKey)
						.concat("&pageToken=").concat(nextPageToken);

				System.out.println("------------ reqStr = " + reqStr);

				URL urlConn = new URL(reqStr);
				URLConnection urlConnection = urlConn.openConnection();
				InputStream response = urlConnection.getInputStream();

				// Convert this response into a readable string
				BufferedReader r = new BufferedReader(new InputStreamReader(response));
				StringBuilder total = new StringBuilder();
				for (String line; (line = r.readLine()) != null; ) {
					total.append(line).append('\n');
				}
				String jsonString = total.toString();
				System.out.println("------------ jsonString = " + jsonString);

				//todo Import JSON
				// Create a JSON object that we can use from the String
				JSONObject json = new JSONObject(jsonString);

				// Create a list to store are videos in
				try {
					nextPageToken = json.getString("nextPageToken");
					tokens.add(nextPageToken);
				} catch (JSONException e) {
					nextPageToken = "";
				}
				System.out.println("--- nextPageToken = " + nextPageToken);

				// Get JSON array
				JSONArray jsonArray = json.getJSONArray("items");

				// parsing JSON array
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject jsonObject = jsonArray.getJSONObject(i);

					// title
					String title = jsonObject.getJSONObject("snippet").getString("title");
					System.out.println("--- title = " + title);

					// video ID
					String videoId = jsonObject.getJSONObject("contentDetails").getString("videoId");
					System.out.println("--- videoId = " + videoId);

					// video URL
					String url = "https://youtu.be/".concat(videoId);
					System.out.println("--- url = " + url);

					// add video instance
					Video video = new Video(url, title);
					videos.add(video);
				}
			}
			while (!nextPageToken.isEmpty() && !tokenIsRepeated);//todo next page token makes endless loop

		} catch (ClientProtocolException e) {
			e.printStackTrace();
			System.out.println("PlaylistApi / _request_and_save_youTubePlaylist / ClientProtocolException ");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("PlaylistApi / _request_and_save_youTubePlaylist / IOException ");
		} catch (JSONException e) {
			e.printStackTrace();
			System.out.println("PlaylistApi / _request_and_save_youTubePlaylist / JSONException ");
		}
	}

}