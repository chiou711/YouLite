package com.cw.youlite.db;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.cw.youlite.R;
import com.cw.youlite.data.Contract;
import com.cw.youlite.data.DbHelper;
import com.cw.youlite.data.FetchService_category;
import com.cw.youlite.data.Provider;
import com.cw.youlite.util.preferences.Pref;

import java.util.Objects;

import androidx.appcompat.app.AppCompatActivity;

public class RenewDB {
	// renew data base
	public RenewDB(AppCompatActivity act)
	{
		try {
			// delete database
			System.out.println("Renew / constructor");
			act.deleteDatabase(DbHelper.DATABASE_NAME);

			ContentResolver resolver = act.getContentResolver();
			ContentProviderClient client = resolver.acquireContentProviderClient(Contract.CONTENT_AUTHORITY);
			Provider provider = (Provider) Objects.requireNonNull(client).getLocalContentProvider();

			Objects.requireNonNull(provider).mContentResolver = resolver;
			provider.mOpenHelper.close();

			provider.mOpenHelper = new DbHelper(act);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
				client.close();
			else
				client.release();

			Pref.setDB_versionSyncReady(act,false);
			Pref.setPref_DB_ready(act,false);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// start Fetch category service
		System.out.println("Renew / constructor / will start Fetch category service");
//        Toast.makeText(act,R.string.toast_update_database,Toast.LENGTH_LONG).show();
		Toaster(act,act.getResources().getString(R.string.toast_update_database));
		Intent serviceIntent = new Intent(act, FetchService_category.class);
		serviceIntent.putExtra("FetchUrl", getDefaultUrl(act));
		act.startService(serviceIntent);

		// reset focus view position
		Pref.setPref_focusView_folder_tableId(act, 1);
		Pref.setPref_focusView_page_tableId(act, 1);
	}

	// get default URL
	int INIT_NUMBER = 1;
	private String getDefaultUrl(AppCompatActivity act)
	{
		// data base is not created yet, call service for the first time
		String urlName = "catalog_url_".concat(String.valueOf(INIT_NUMBER));
		int id = act.getResources().getIdentifier(urlName,"string",act.getPackageName());
		return act.getString(id);
	}


	private void Toaster(final Context ctx, final String text) {
		final Dialog dialog = new Dialog(ctx);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.getWindow().setFlags(
				android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
				android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
		dialog.getWindow().setBackgroundDrawable(
				new ColorDrawable(Color.TRANSPARENT));
		dialog.setCancelable(false);
		dialog.setContentView(R.layout.guide);
		TextView guide = (TextView) dialog.findViewById(R.id.g_text);
		guide.setText(text);

		Button buy = (Button) dialog.findViewById(R.id.gotit);
		buy.setVisibility(View.INVISIBLE);

		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
		lp.copyFrom(dialog.getWindow().getAttributes());
		lp.width = WindowManager.LayoutParams.MATCH_PARENT;
		lp.height = WindowManager.LayoutParams.MATCH_PARENT;
		dialog.show();
		dialog.getWindow().setAttributes(lp);
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				hideDialog(dialog);			}
		}, 2000);
	}

	private void hideDialog(Dialog dialog) {
		if(dialog != null) {
			if(dialog.isShowing()) {
				Context context = ((ContextWrapper)dialog.getContext()).getBaseContext();
				if(context instanceof Activity) {
					if(!((Activity)context).isFinishing() && !((Activity)context).isDestroyed())
						dialog.dismiss();
				} else {
					dialog.dismiss();
				}
			}
		}
	}

}