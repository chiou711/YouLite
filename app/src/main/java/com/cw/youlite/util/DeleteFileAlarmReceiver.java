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

package com.cw.youlite.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.FilenameFilter;

public class  DeleteFileAlarmReceiver extends BroadcastReceiver
{
    private static final String EXTRA_FILENAME = "com.cwc.youlite.extras.filename";

	//Can not remove this constructor,although it seems to be not used
	public DeleteFileAlarmReceiver(){}

	// Delete files with Alarm Receiver
    public DeleteFileAlarmReceiver(Context context, long timeMilliSec, String[] filename)
    {
   	    for(int i=0; i<filename.length;i++) {
			Intent intent = new Intent(context, DeleteFileAlarmReceiver.class);
			intent.putExtra(EXTRA_FILENAME, filename[i]);

			AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	        // Add for Strongly consider using FLAG_IMMUTABLE
	        final int flags =  Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
			        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;
			PendingIntent pendIntent = PendingIntent.getBroadcast(context, 0, intent, flags);
			alarmMgr.set(AlarmManager.RTC_WAKEUP, timeMilliSec, pendIntent);
		}
    }

    @Override
    public void onReceive(final Context context, Intent intent)
    {
	   System.out.println("DeleteFileAlarmReceiver / _onReceive");
		// Note: if launch Send mail twice, file name founded is the first one, not the second one
	    // so, delete any file starts with YouLite_SEND and ends with txt
		
	    // SD card path + "/" + directory path
	    // old path: /storage/emulated/0/YouLite
//	    String folderString = Environment.getExternalStorageDirectory().toString() +
//			    "/" +
//			    /storage/emulated/0/Util.getStorageDirName(context);

	    // new path:
	    // /storage/sdcard0
	    // /Android/data/com.cw.youlite/files/Documents
	    String folderString = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).toString();
	    System.out.println("DeleteFileAlarmReceiver / _onReceive / folderString = " + folderString);

		File folder = new File( folderString);
		
		File[] files = folder.listFiles( new FilenameFilter() 
		{
		    @Override
		    public boolean accept( final File dir,
		                           final String name ) {
//		        return name.matches( "YouLite_SEND.*\\.txt" ); // starts with YouLite_SEND, ends with txt
//		        return name.matches( ".*\\.txt" ); // end with txt
//		        return name.matches("(YouLite_SEND.+(\\.(?i)(txt))$)" );
				boolean isMatch = false;
				if(name.matches("("+ Util.getStorageDirName(context)+"_SEND.+(\\.(?i)(json))$)" ) ||
				   name.matches("("+ Util.getStorageDirName(context)+"_SEND.+(\\.(?i)(txt))$)" )    )
				{
					isMatch = true;
				}
		        return isMatch;
		    }
		} );

		// delete file if found
		for ( final File fileFound : files ){
			System.out.println("DeleteFileAlarmReceiver / _onReceive / fileFound = " + fileFound.getName());
		    if ( !fileFound.delete() )
		        System.err.println( "Can't remove " + fileFound.getAbsolutePath() );
		}
    }
}
