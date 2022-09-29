package com.cw.youlite.util;

/**
 *  get videos of a given play list
 *  cf.https://github.com/blundell/YouTubeUserFeed
 */

import com.cw.youlite.operation.youtube.YouTubeDeveloperKey;

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
import java.util.concurrent.Executors;

public class PlaylistApi {

	// Request YouTube play list items and save it to DB
	public static void request_and_save_youTubePlaylist(String youtubeUrl) {
		System.out.println("PlaylistApi / _request_and_save_youTubePlaylist / youtubeUrl = " + youtubeUrl);
		Executors.newSingleThreadExecutor().submit(new Runnable() {
			@Override
			public void run() {
				try {
					doRequest(youtubeUrl);
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("--- _request_and_save_youTubePlaylist / exception");
				}
			}
		});
	}

	// do request
	static void doRequest(String youtubeUrl){
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
			List<String> videos = new ArrayList<>();

			do {
				String reqStr= prefixUrlStr.concat("&playlistId=").concat(playlistIdStr)
						.concat("&key=").concat(apiKey)
						.concat("&pageToken=").concat(nextPageToken);

//				System.out.println("------------ reqStr = " + reqStr);

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
//				System.out.println("------------ jsonString = " + jsonString);

				// Create a JSON object that we can use from the String
				JSONObject json = new JSONObject(jsonString);

				// Create a list to store are videos in
				try {
					nextPageToken = json.getString("nextPageToken");
				}catch (JSONException e){
					nextPageToken = "";
				}
//				System.out.println("--- nextPageToken = " + nextPageToken);

				// Get JSON array
				JSONArray jsonArray = json.getJSONArray("items");

				// parsing JSON array
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject jsonObject = jsonArray.getJSONObject(i);

					// title
					String title = jsonObject.getJSONObject("snippet").getString("title");
//					System.out.println("--- title = " + title);

					// video ID
					String videoId = jsonObject.getJSONObject("contentDetails").getString("videoId");
//					System.out.println("--- videoId = " + videoId);

					// video URL
					String url = "https://youtu.be/".concat(videoId);
//					System.out.println("--- url = " + url);

					videos.add(url);
				}
			}while (!nextPageToken.isEmpty());

			//todo process videos
			System.out.println("----- videos.size() = " + videos.size());
			for(int i=0;i<videos.size();i++)
				System.out.println("----- videos.get("+ i+ ") = " + videos.get(i));

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