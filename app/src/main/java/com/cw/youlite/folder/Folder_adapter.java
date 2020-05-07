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

package com.cw.youlite.folder;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cw.youlite.R;
import com.cw.youlite.db.DB_drawer;
import com.cw.youlite.main.MainAct;
import com.mobeta.android.dslv.SimpleDragSortCursorAdapter;

import static com.cw.youlite.db.DB_drawer.KEY_FOLDER_TITLE;

/**
 * Created by cw on 2017/10/6.
 */

public class Folder_adapter extends SimpleDragSortCursorAdapter
{
    int cursor_count;
    Folder_adapter(Context context, int layout, Cursor c,
            String[] from, int[] to, int flags)
    {
        super(context, layout, c, from, to, flags);
        cursor_count = c.getCount();
    }

    @Override
    public int getCount() {
        return cursor_count;
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        ViewHolder viewHolder; // holds references to current item's GUI

        // if convertView is null, inflate GUI and create ViewHolder;
        // otherwise, get existing ViewHolder
        if (convertView == null)
        {
            convertView = MainAct.mAct.getLayoutInflater().inflate(R.layout.folder_row, parent, false);

            // set up ViewHolder for this ListView item
            viewHolder = new ViewHolder();
            viewHolder.folderTitle = (TextView) convertView.findViewById(R.id.folderText);
            viewHolder.dragIcon = (ImageView) convertView.findViewById(R.id.folder_drag);
            convertView.setTag(viewHolder); // store as View's tag
        }
        else // get the ViewHolder from the convertView's tag
            viewHolder = (ViewHolder) convertView.getTag();

        DB_drawer db_drawer = new DB_drawer(MainAct.mAct);
        db_drawer.open();

        Cursor cursor = db_drawer.mCursor_folder;
        cursor.moveToPosition(position);
        String folder_title="";
        try {
            folder_title =cursor.getString(cursor.getColumnIndex(KEY_FOLDER_TITLE));
        } catch (Exception e) {
            e.printStackTrace();
        }

        db_drawer.close();

        viewHolder.folderTitle.setText(folder_title);

        // dragger
        SharedPreferences pref = MainAct.mAct.getSharedPreferences("show_note_attribute", 0);;
        if(pref.getString("KEY_ENABLE_FOLDER_DRAGGABLE", "no").equalsIgnoreCase("yes"))
            viewHolder.dragIcon.setVisibility(View.VISIBLE);
        else
            viewHolder.dragIcon.setVisibility(View.GONE);

        return convertView;
    }


    private static class ViewHolder
    {
        TextView folderTitle; // refers to ListView item's ImageView
        ImageView dragIcon;
    }
}
