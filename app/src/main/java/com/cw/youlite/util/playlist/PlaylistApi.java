package com.cw.youlite.util.playlist;

/**
 *  get videos of a given play list
 *  cf.https://github.com/blundell/YouTubeUserFeed
 */

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import com.cw.youlite.R;
import com.cw.youlite.db.DB_drawer;
import com.cw.youlite.db.DB_folder;
import com.cw.youlite.util.preferences.Pref;

public class PlaylistApi {

	Activity act;
	int folders_count;
	int pages_count;
	int tableId;
	public PlaylistApi(Activity _act) {
		act = _act;

		DB_drawer db_drawer = new DB_drawer(act);
		folders_count = db_drawer.getFoldersCount(true);

		DB_folder db_folder = new DB_folder(act, Pref.getPref_focusView_folder_tableId(act));
		pages_count = db_folder.getPagesCount(true);
		tableId = Pref.getPref_focusView_page_tableId(act);
	}

	// Request YouTube play list items and save it to DB
	public void request_and_save_youTubePlaylist(String youtubeUrl,boolean isAdded_onNewIntent) {
		System.out.println("PlaylistApi / _request_and_save_youTubePlaylist / youtubeUrl = " + youtubeUrl);
		if ((folders_count == 0) || (pages_count == 0)) {
			Toast.makeText(act, R.string.toast_no_folder_or_no_page_yet, Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(act,R.string.renew_data,Toast.LENGTH_LONG).show();

			PlaylistJsonAsync playlistJsonAsync = new PlaylistJsonAsync(act,tableId,isAdded_onNewIntent);
			playlistJsonAsync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,youtubeUrl);
		}
	}

}