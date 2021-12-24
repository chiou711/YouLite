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

package com.cw.youlite.note;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cw.youlite.R;
import com.cw.youlite.db.DB_page;
import com.cw.youlite.note_edit.Note_edit;
import com.cw.youlite.operation.mail.MailNotes;
import com.cw.youlite.operation.youtube.SearchYouTube;
import com.cw.youlite.tabs.TabsHost;
import com.cw.youlite.util.Util;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cw on 2017/10/7.
 */
class View_note_option {
    private int option_id;
    private int option_drawable_id;
    private int option_string_id;

    View_note_option(){};

    private View_note_option(int id, int draw_id, int string_id)
    {
        this.option_id = id;
        this.option_drawable_id = draw_id;
        this.option_string_id = string_id;
    }

    /**
     *
     * 	Add new note
     *
     */
    static List<View_note_option> option_list;

    private final static int ID_OPTION_MAIL = 0;
    private final static int ID_OPTION_EDIT = 1;
    private final static int ID_OPTION_SEARCH_YOUTUBE = 2;
    private final static int ID_OPTION_BACK = 9;
    private static long noteId;
    private static GridIconAdapter mGridIconAdapter;

    void note_option(final AppCompatActivity act, long _noteId)
    {
        AbsListView gridView;
        noteId = _noteId;
        // get layout inflater
        View rootView = act.getLayoutInflater().inflate(R.layout.option_grid, null);

        option_list = new ArrayList<>();

        // mail
        option_list.add(new View_note_option(ID_OPTION_MAIL ,
                        android.R.drawable.ic_menu_send,
                        R.string.mail_notes_btn));

        // search youtube with keyword
        option_list.add(new View_note_option(ID_OPTION_SEARCH_YOUTUBE ,
                R.drawable.ic_youtube,
                R.string.search_youtube));

        // edit
        option_list.add(new View_note_option(ID_OPTION_EDIT ,
                android.R.drawable.ic_menu_edit,
                R.string.view_note_button_edit));

        // Back
        option_list.add(new View_note_option(ID_OPTION_BACK,
                R.drawable.ic_menu_back,
                R.string.btn_back));

        gridView = (GridView) rootView.findViewById(R.id.option_grid_view);

        // check if directory is created AND not empty
        if( (option_list != null  ) && (option_list.size() > 0))
        {
            mGridIconAdapter = new GridIconAdapter(act);
            gridView.setAdapter(mGridIconAdapter);
        }
        else
        {
            Toast.makeText(act,R.string.gallery_toast_no_file, Toast.LENGTH_SHORT).show();
            act.finish();
        }

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                System.out.println("View_note_option / _note_option / _OnItemClickListener / position = " + position +" id = " + id);
                startAddNoteActivity(act, option_list.get(position).option_id);
            }
        });

        // set view to dialog
        AlertDialog.Builder builder1 = new AlertDialog.Builder(act);
        builder1.setView(rootView);
        dlgAddNew = builder1.create();
        dlgAddNew.show();
    }
    AlertDialog dlgAddNew;

    private void startAddNoteActivity(AppCompatActivity act,int optionId)
    {
        System.out.println("View_note_option / _startAddNoteActivity / optionId = " + optionId);

        switch (optionId) {
            case ID_OPTION_MAIL:
                dlgAddNew.dismiss();
//                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)//API23
//                {
//                    // check permission
//                    if (ActivityCompat.checkSelfPermission(act, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                            != PackageManager.PERMISSION_GRANTED)
//                    {
//                        // No explanation needed, we can request the permission.
//                        ActivityCompat.requestPermissions(act,
//                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                                        Manifest.permission.READ_EXTERNAL_STORAGE},
//                                Util.PERMISSIONS_REQUEST_STORAGE);
//                    }
//                    else
//                        doMailNote(act);
//                }
//                else
                    doMailNote(act);
            break;

            case ID_OPTION_SEARCH_YOUTUBE:
                dlgAddNew.dismiss();

                DB_page dB_page = new DB_page(act, TabsHost.getCurrentPageTableId());
                String keyWord = dB_page.getNoteTitle_byId(noteId);
                Intent intent = new Intent(act,SearchYouTube.class);
                intent.putExtra("search_keywords", keyWord );
                act.startActivity(intent);
            break;

            case ID_OPTION_EDIT:
                dlgAddNew.dismiss();

                int position = NoteUi.getFocus_notePos();
                DB_page db_page = new DB_page(act, TabsHost.getCurrentPageTableId());
                Long rowId = db_page.getNoteId(position,true);

                Intent i = new Intent(act, Note_edit.class);
                i.putExtra("list_view_position", position);
                i.putExtra(DB_page.KEY_NOTE_ID, rowId);
                i.putExtra(DB_page.KEY_NOTE_TITLE, db_page.getNoteTitle_byId(rowId));
                i.putExtra(DB_page.KEY_NOTE_PICTURE_URI , db_page.getNotePictureUri_byId(rowId));
                i.putExtra(DB_page.KEY_NOTE_LINK_URI , db_page.getNoteLinkUri_byId(rowId));
                i.putExtra(DB_page.KEY_NOTE_CREATED, db_page.getNoteCreatedTime_byId(rowId));
                act.startActivity(i);
            break;

            case ID_OPTION_BACK:
                dlgAddNew.dismiss();
            break;

            // default
            default:
                break;
        }

    }

    void doMailNote(AppCompatActivity act)
    {
        // set Sent string Id
        String sentString = null;
        try {
            sentString = Util.getJson(TabsHost.getFocus_tabPos(),noteId).get(0).toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sentString = "{\"client\":\"TV YouTube\",\"content\":[{\"category\":\"new folder\",\"link_page\":[{\"title\":\"new page\",\"links\":[" +
                            sentString +
                             "]}]}]}";

        new MailNotes(act,sentString);
    }


    /**
     * Created by cw on 2017/10/7.
     */
    static class GridIconAdapter extends BaseAdapter {
        private AppCompatActivity act;
        GridIconAdapter(AppCompatActivity fragAct){act = fragAct;}

        @Override
        public int getCount() {
            return option_list.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            View view = convertView;
            if (view == null) {
                view = act.getLayoutInflater().inflate(R.layout.add_note_grid_item, parent, false);
                holder = new ViewHolder();
                assert view != null;
                holder.imageView = (ImageView) view.findViewById(R.id.grid_item_image);
                holder.text = (TextView) view.findViewById(R.id.grid_item_text);
                holder.text.setTextColor(Color.WHITE);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            Drawable drawable = act.getResources().getDrawable(option_list.get(position).option_drawable_id);
            holder.imageView.setImageDrawable(drawable);
            holder.text.setText(option_list.get(position).option_string_id);
            return view;
        }

        private class ViewHolder {
            ImageView imageView;
            TextView text;
        }
    }
}
