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

package com.cw.youlite.operation.mail;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cw.youlite.R;
import com.cw.youlite.main.MainAct;
import com.cw.youlite.operation.import_export.Import_fileViewJson_asyncTask;
import com.cw.youlite.util.ColorSet;
import com.cw.youlite.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.Fragment;


public class Mail_fileViewJson extends Fragment
{
    TextView mTitleViewText;
    TextView mBodyViewText;
    String filePath;
    File mFile;
    View rootView;
	private Context mContext;

    @Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Bundle arguments = getArguments();
		filePath = arguments.getString("KEY_FILE_PATH");
		mContext = getActivity();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.import_sd_json_file_view,container, false);
		System.out.println("Import_fileViewJson / onCreate");

		mTitleViewText = (TextView) rootView.findViewById(R.id.view_title);
		mBodyViewText = (TextView) rootView.findViewById(R.id.view_body);

        Import_fileViewJson_asyncTask task = null;
		if(savedInstanceState == null) {
			task = new Import_fileViewJson_asyncTask(MainAct.mAct,rootView,filePath);
			task.enableSaveDB(false);// view
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		else
		{
			mFile = new File(filePath);
			mTitleViewText.setText(mFile.getName());
			mBodyViewText.setText(task.importObject.fileBody);
		}

		int style = 2;
		//set title color
		mTitleViewText.setTextColor(ColorSet.mText_ColorArray[style]);
		mTitleViewText.setBackgroundColor(ColorSet.mBG_ColorArray[style]);
		//set body color
		mBodyViewText.setTextColor(ColorSet.mText_ColorArray[style]);
		mBodyViewText.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

		// back button
		Button backButton = (Button) rootView.findViewById(R.id.view_back);
		backButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_back, 0, 0, 0);

		// confirm button
		Button confirmButton = (Button) rootView.findViewById(R.id.view_confirm);
		confirmButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_send, 0, 0, 0);
		confirmButton.setText(R.string.mail_notes_btn);

		// do cancel
		backButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
                backToListFragment();
			}
		});

		// confirm to import view to DB
		confirmButton.setOnClickListener(new View.OnClickListener()
		{

			public void onClick(View view)
			{
				inputEMailDialog(); // call next dialog
			}
		});


		return rootView;
	}


	// Send e-Mail
	// case A: input mail address from current activity
	// case B: input mail address from ViewNote activity
	String mDefaultEmailAddr;
	SharedPreferences mPref_email;
	EditText editEMailAddrText;
	String mEMailBodyString;
	AlertDialog mDialog;

	void inputEMailDialog()
	{
		AlertDialog.Builder builder1;

		mPref_email = getActivity().getSharedPreferences("email_addr", 0);
		editEMailAddrText = (EditText)getActivity().getLayoutInflater()
				.inflate(R.layout.edit_text_dlg, null);
		builder1 = new AlertDialog.Builder(getActivity());

		// get default email address
		mDefaultEmailAddr = mPref_email.getString("KEY_DEFAULT_EMAIL_ADDR","@");
		editEMailAddrText.setText(mDefaultEmailAddr);

		builder1.setTitle(R.string.mail_notes_dlg_title)
				.setMessage(R.string.mail_notes_dlg_message)
				.setView(editEMailAddrText)
				.setNegativeButton(R.string.edit_note_button_back,
						new DialogInterface.OnClickListener()
						{   @Override
						public void onClick(DialogInterface dialog, int which)
						{/*cancel*/
							dialog.dismiss();
						}

						})
				.setPositiveButton(R.string.mail_notes_btn, null); //call override

		mDialog = builder1.create();
		mDialog.show();

		// override positive button
		Button enterButton = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
		enterButton.setOnClickListener(new CustomListener(mDialog));


		// back
		mDialog.setOnKeyListener(new Dialog.OnKeyListener() {

			@Override
			public boolean onKey(DialogInterface arg0, int keyCode,
			                     KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					mDialog.dismiss();
					return true;
				}
				return false;
			}
		});
	}

	//for keeping dialog when eMail address is empty
	class CustomListener implements View.OnClickListener
	{
		private final Dialog dialog;
		public CustomListener(Dialog dialog){
			this.dialog = dialog;
		}

		@Override
		public void onClick(View v) {

			System.out.println("Mail_fileViewJson / CustomListener / _onClick / filePath = " + filePath);
			String[] attachmentFileName={""};
			String strEMailAddr = editEMailAddrText.getText().toString();
            if(strEMailAddr.length() > 0)
            {
                Bundle extras = getActivity().getIntent().getExtras();

	            attachmentFileName[0] = new File(filePath).getName();
	            System.out.println("Mail_fileViewJson / CustomListener / _onClick / attachmentFileName[0] = " + attachmentFileName[0]);


                // for page selection
                String[] picFileNameArr = null;

                mPref_email.edit().putString("KEY_DEFAULT_EMAIL_ADDR", strEMailAddr).apply();

                // call next dialog
                sendEMail(strEMailAddr,  // eMail address
                        attachmentFileName, // attachment file name
		                picFileNameArr ); // picture file name array. For page selection, this is null
                dialog.dismiss();
            }
            else
            {
                Toast.makeText(getActivity(),
                        R.string.toast_no_email_address,
                        Toast.LENGTH_SHORT).show();
            }
		}
	}

	Intent mEMailIntent;
	// Send e-Mail : send file by e-Mail
	public final static int EMAIL_JSON = 103;
	public static String[] mAttachmentFileName;
	void sendEMail(String strEMailAddr,  // eMail address
	               String[] attachmentFileName, // attachment name
	               String[] picFileNameArray) // attachment picture file name
	{
		mAttachmentFileName = attachmentFileName;
		// new ACTION_SEND intent
		mEMailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE); // for multiple attachments

		// set type
		mEMailIntent.setType("text/plain");//can select which APP will be used to send mail

		// open issue: cause warning for Key android.intent.extra.TEXT expected ArrayList
		String text_body = mContext.getResources().getString(R.string.eMail_body)// eMail text (body)
				+ " " + Util.getStorageDirName(mContext) + " (UTF-8)" + Util.NEW_LINE
				+ mEMailBodyString;

		// attachment: message
		List<String> filePaths = new ArrayList<String>();
		for(int i=0;i<attachmentFileName.length;i++) {
			String messagePath = "file:///" + Environment.getExternalStorageDirectory().getPath() +
					"/" + Util.getStorageDirName(mContext) + "/" +
					attachmentFileName[i];// message file name
			filePaths.add(messagePath);
		}

		// attachment: pictures
		if(picFileNameArray != null)
		{
			for(int i=0;i<picFileNameArray.length;i++)
			{
				filePaths.add(picFileNameArray[i]);
			}
		}

		ArrayList<Uri> uris = new ArrayList<Uri>();
		for (String file : filePaths)
		{
			Uri uri = Uri.parse(file);
			uris.add(uri);
			System.out.println("Mail_fileViewJson / _sendEMail / uri to string" + uri.toString());
		}

		// eMail extra
		mEMailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{strEMailAddr}) //address
				.putExtra(Intent.EXTRA_SUBJECT,Util.getStorageDirName(mContext)+" "+mContext.getResources().getString(R.string.eMail_subject )) //subject
				.putExtra(Intent.EXTRA_TEXT,text_body) //body (open issue)
				.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris); //multiple attachment

		Log.v(getClass().getSimpleName(),
				"attachment " + Uri.parse("file name is:"+ attachmentFileName));

		getActivity().startActivityForResult(Intent.createChooser(mEMailIntent,
				getResources().getText(R.string.mail_chooser_title)) ,
				EMAIL_JSON);
	}

	void backToListFragment()
    {
        getActivity().getSupportFragmentManager().popBackStack();
        View view1 = getActivity().findViewById(R.id.view_back_btn_bg);
        view1.setVisibility(View.VISIBLE);
        View view2 = getActivity().findViewById(R.id.file_list_title);
        view2.setVisibility(View.VISIBLE);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}