/*
 * Copyright (C) 2017 Anton Kaliturin <dandytek@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.dandytek.sms_blocker.fragments;


import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;

import com.dandytek.sms_blocker.R;
import com.dandytek.sms_blocker.activities.CustomFragmentActivity;
import com.dandytek.sms_blocker.adapters.SMSConversationsListCursorAdapter;
import com.dandytek.sms_blocker.receivers.InternalEventBroadcast;
import com.dandytek.sms_blocker.utils.ButtonsBar;
import com.dandytek.sms_blocker.utils.ContactsAccessHelper;
import com.dandytek.sms_blocker.utils.ContactsAccessHelper.SMSConversation;
import com.dandytek.sms_blocker.utils.DatabaseAccessHelper;
import com.dandytek.sms_blocker.utils.DatabaseAccessHelper.Contact;
import com.dandytek.sms_blocker.utils.DefaultSMSAppHelper;
import com.dandytek.sms_blocker.utils.DialogBuilder;
import com.dandytek.sms_blocker.utils.Permissions;
import com.dandytek.sms_blocker.utils.ProgressDialogHolder;
import com.dandytek.sms_blocker.utils.Utils;

/**
 * Fragment for showing all SMS conversations
 */
public class SMSConversationsListFragment extends Fragment implements FragmentArguments {
    private InternalEventBroadcast internalEventBroadcast = null;
    private SMSConversationsListCursorAdapter cursorAdapter = null;
    private ListView listView = null;
    private JournalFragment journalFragment = null;
    private RecyclerView recyclerView = null;
    private int listPosition = 0;
    private String itemsFilter = "";
    private ButtonsBar snackBar = null;
    private SearchView searchView = null;
    private MenuItem itemSearch = null;

    public SMSConversationsListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // set activity title
        Bundle arguments = getArguments();
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (arguments != null && actionBar != null) {
            actionBar.setTitle(arguments.getString(TITLE));
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            listPosition = savedInstanceState.getInt(LIST_POSITION, 0);
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sms_conversations_list, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // notify user if permission isn't granted
        Permissions.notifyIfNotGranted(getContext(), Permissions.READ_SMS);
        Permissions.notifyIfNotGranted(getContext(), Permissions.READ_CONTACTS);

        // cursor adapter
        cursorAdapter = new SMSConversationsListCursorAdapter(getContext());
        // on row click listener (receives clicked row)
        cursorAdapter.setOnClickListener(new OnRowClickListener());
        // on row long click listener (receives clicked row)
        cursorAdapter.setOnLongClickListener(new OnRowLongClickListener());

        // add cursor listener to the list
        listView = (ListView) view.findViewById(R.id.rows_list);
        listView.setAdapter(cursorAdapter);

        // recylerview

       // recyclerView = (RecyclerView) view.findViewById(R.id.rows_list);
      //  recyclerView.setAdapter(cursorAdapter);

        // on list empty comment
        TextView textEmptyView = (TextView) view.findViewById(R.id.text_empty);
        listView.setEmptyView(textEmptyView);

        // init internal broadcast event receiver
        internalEventBroadcast = new InternalEventBroadcast() {
            // SMS was written
            @Override
            public void onSMSWasWritten(String phoneNumber) {
                ContactsAccessHelper db = ContactsAccessHelper.getInstance(getContext());
                int threadId = db.getSMSThreadIdByNumber(getContext(), phoneNumber);
                if (threadId >= 0 &&
                        // refresh cached list view items
                        cursorAdapter.invalidateCache(threadId)) {
                    Cursor cursor = cursorAdapter.getCursor();

                    Log.d("dialog not",String.valueOf(cursor));

                    cursorAdapter.changeCursor(cursor);
                    cursorAdapter.notifyDataSetChanged();
                } else {
                    // reload all list view items
                    loadListViewItems(false, false);
                }
            }

            // SMS was deleted
            @Override
            public void onSMSWasDeleted(String phoneNumber) {
                // reload all list view items
                Cursor cursor = cursorAdapter.getCursor();

                Log.d("dialog 2",String.valueOf(cursor));

                cursorAdapter.changeCursor(cursor);
                cursorAdapter.notifyDataSetChanged();
                loadListViewItems(listPosition,false, false);
            }

            // SMS thread was read
            @Override
            public void onSMSThreadWasRead(int threadId) {
                // refresh cached list view items
                cursorAdapter.invalidateCache(threadId);
                cursorAdapter.notifyDataSetChanged();
            }
        };
        internalEventBroadcast.register(getContext());

        // load SMS conversations to the list
        loadListViewItems(listPosition, true, true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(LIST_POSITION, listView.getFirstVisiblePosition());
    }

    @Override
    public void onDestroyView() {
        getLoaderManager().destroyLoader(0);
        internalEventBroadcast.unregister(getContext());
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);

        MenuItem menuItem = menu.findItem(R.id.write_message);
        Utils.setMenuIconTint(getContext(), menuItem, R.attr.colorAccent);
        menuItem.setVisible(true);
        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // open SMS sending activity
                CustomFragmentActivity.show(getContext(),
                        getString(R.string.New_message),
                        SMSSendFragment.class, null);
                return true;
            }
        });


        /*
        itemSearch = menu.findItem(R.id.action_search);
        Utils.setMenuIconTint(getContext(), itemSearch, R.attr.colorAccent);
        itemSearch.setVisible(true);

        // get the view from search menu item
        searchView = (SearchView) MenuItemCompat.getActionView(itemSearch);
        searchView.setQueryHint(getString(R.string.Search_action));
        // set on text change listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                reloadItems(newText, false);
                return true;
            }
        });

        // on search cancelling
        // SearchView.OnCloseListener is not calling so use other way...
        MenuItemCompat.setOnActionExpandListener(itemSearch,
                new MenuItemCompat.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        reloadItems("", false);
                        return true;
                    }
                });
        */




        super.onCreateOptionsMenu(menu, inflater);
    }


    /*

    // Reloads items
    public void reloadItems(@NonNull String itemsFilter, boolean force) {
        if (!force && this.itemsFilter.equals(itemsFilter)) {
            return;
        }
        Log.d("sms itemfilter: ",itemsFilter);
        this.itemsFilter = itemsFilter;
        dismissSnackBar();

        int listPosition = listView.getFirstVisiblePosition();
        Log.d("list position: ",String.valueOf(listPosition));
        Log.d("sms list position: ",String.valueOf(listPosition));
        loadListViewItems(itemsFilter, false, listPosition, false, true);
    }


    // Loads items to the list view
    private void loadListViewItems(String itemsFilter, boolean deleteItems, int listPosition, boolean markSeen, boolean showProgress) {
        if (!isAdded()) {
            return;
        }
        int loaderId = 0;
        Log.d("sms itemfilter: ", itemsFilter);
        ConversationsLoaderCallbacks callbacks =
                new ConversationsLoaderCallbacks(getContext(), listView,
                        listPosition, cursorAdapter, markSeen, showProgress);
        LoaderManager manager = getLoaderManager();
        if (manager.getLoader(loaderId) == null) {
            // init and run the items loader
            manager.initLoader(loaderId, null, callbacks);
        } else {
            // restart loader
            manager.restartLoader(loaderId, null, callbacks);
        }
    }


    // Closes snack bar
    public boolean dismissSnackBar() {
        clearCheckedItems();
        return snackBar != null && snackBar.dismiss();
    }

    // Clears all items selection
    private void clearCheckedItems() {
        if (cursorAdapter != null) {
            cursorAdapter.setAllItemsChecked(false);
        }
    }


    */

    @Override
    public void onPause() {
        super.onPause();
        listPosition = listView.getFirstVisiblePosition();
    }

//----------------------------------------------------------------------

    // On row click listener
    private class OnRowClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View row) {
            // get the clicked conversation
            final SMSConversation sms = cursorAdapter.getSMSConversation(row);
            if (sms != null) {
                String person = (sms.person != null ? sms.person : sms.number);
                // open activity with all the SMS of the conversation
                Bundle arguments = new Bundle();
                arguments.putString(CONTACT_NAME, person);
                arguments.putString(CONTACT_NUMBER, sms.number);
                arguments.putInt(THREAD_ID, sms.threadId);
                arguments.putInt(UNREAD_COUNT, sms.unread);
                CustomFragmentActivity.show(getContext(), person,
                        SMSConversationFragment.class, arguments);
            }
        }
    }

    // On row long click listener
    private class OnRowLongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View row) {
            final SMSConversation sms = cursorAdapter.getSMSConversation(row);
            if (sms == null) {
                return true;
            }

            final String person = (sms.person != null ? sms.person : sms.number);

            Log.d("person",person);
            Log.d("person's sms",sms.number);


            // create menu dialog
            DialogBuilder dialog = new DialogBuilder(getContext());

            dialog.setTitle(person);
            // add menu item of sms deletion
            dialog.addItem(R.string.Delete_thread, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (DefaultSMSAppHelper.isDefault(getContext())) {
                        // remove SMS thread
                        ContactsAccessHelper db = ContactsAccessHelper.getInstance(getContext());
                        if (db.deleteSMSMessagesByThreadId(getContext(), sms.threadId)) {

                            Cursor cursor = cursorAdapter.getCursor();

                            Log.d("dialog",String.valueOf(cursor));

                            cursorAdapter.changeCursor(cursor);
                            cursorAdapter.notifyDataSetChanged();

                            // reload list
                            int listPosition = listView.getFirstVisiblePosition();

                            loadListViewItems(listPosition,false, true);
                        }
                    } else {
                        Toast.makeText(getContext(), R.string.Need_default_SMS_app,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });

            final DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(getContext());
            if (db != null) {
                // 'move contact to black list'
                DatabaseAccessHelper.Contact contact = db.getContact(person, sms.number);
                Log.d("person contact: ",String.valueOf(contact));

                if (contact == null || contact.type != Contact.TYPE_BLACK_LIST || contact.type != Contact.TYPE_FS_BLACK_LIST) {
                    dialog.addItem(R.string.Move_to_black_list, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            db.addContact(Contact.TYPE_BLACK_LIST, person, sms.number);
                        }
                    });
                }

                // 'move contact to white list'
                if (contact == null || contact.type != Contact.TYPE_WHITE_LIST) {
                    dialog.addItem(R.string.Move_to_white_list, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            db.addContact(Contact.TYPE_WHITE_LIST, person, sms.number);
                        }
                    });
                }
            }

            dialog.show();

            return true;
        }
    }

//----------------------------------------------------------------------

    // Loads SMS conversations to the list view
    private void loadListViewItems(boolean markSeen, boolean showProgress) {
        int listPosition = listView.getFirstVisiblePosition();
        loadListViewItems(listPosition, markSeen, showProgress);
    }

    // Loads SMS conversations to the list view
    private void loadListViewItems(int listPosition, boolean markSeen, boolean showProgress) {
        if (!isAdded()) {
            return;
        }
        int loaderId = 0;
        ConversationsLoaderCallbacks callbacks =
                new ConversationsLoaderCallbacks(getContext(), listView,
                        listPosition, cursorAdapter, markSeen, showProgress);

        LoaderManager manager = getLoaderManager();
        Loader<?> loader = manager.getLoader(loaderId);
        if (loader == null) {
            // init and run the items loader
            Log.d("loader id null:",String.valueOf(loader));
            manager.initLoader(loaderId, null, callbacks);
        } else {
            // restart loader
            Log.d("loader not null:",String.valueOf(loader));
            Log.d("loader id not null:",String.valueOf(loaderId));
            manager.restartLoader(loaderId, null, callbacks);
        }
    }

    // SMS conversations loader
    private static class ConversationsLoader extends CursorLoader {

       // private String itemsFilter;

      /*  ConversationsLoader(Context context) {
            super(context);
        } */


        ConversationsLoader(Context context) {
            super(context);
           // this.itemsFilter = itemsFilter;
        }

        @Override
        public Cursor loadInBackground() {
            // get all SMS conversations

           /* if (!itemsFilter.equals("")) {
                ContactsAccessHelper db_1 = ContactsAccessHelper.getInstance(getContext());
                DatabaseAccessHelper db_2 = DatabaseAccessHelper.getInstance(getContext());
                if (db_2 != null){
                    return db_2.getContactNumbersByNumber(itemsFilter);
                }
            } */


            ContactsAccessHelper db = ContactsAccessHelper.getInstance(getContext());
            return db.getSMSConversations(getContext());

        }
    }






    // SMS conversations loader callbacks
    private static class ConversationsLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        private ProgressDialogHolder progress = new ProgressDialogHolder();
        private SMSConversationsListCursorAdapter cursorAdapter;
        private Context context;
        private ListView listView;
        private int listPosition;
        private boolean markSeen;
        private boolean showProgress;
       // private String itemsFilter;
       // private boolean deleteItems;


        ConversationsLoaderCallbacks(Context context,
                                     ListView listView,
                                     int listPosition,
                                     SMSConversationsListCursorAdapter cursorAdapter,
                                     boolean markSeen,
                                     boolean showProgress) {
            this.context = context;
            this.listView = listView;
            this.listPosition = listPosition;
            this.cursorAdapter = cursorAdapter;
            this.markSeen = markSeen;
            this.showProgress = showProgress;
           // this.itemsFilter = itemsFilter;
           // this.deleteItems = deleteItems;

           // Log.d("sms callbacks: ", itemsFilter);

          //  Log.d("list position:", String.valueOf(listPosition));

            listView.setSelection(listPosition);
            listView.setVisibility(View.VISIBLE);
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if (showProgress) {
                progress.show(context, R.string.Loading_);
            }
           // return new ConversationsLoader(context, itemsFilter);

            return new ConversationsLoader(context);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            Log.d("OnLoadFinised",String.valueOf(cursor));

            cursorAdapter.changeCursor(cursor);
            cursorAdapter.notifyDataSetChanged();


            // scroll list to the saved position
            listView.post(new Runnable() {
                @Override
                public void run() {
                    Cursor cursor = cursorAdapter.getCursor();
                    if (cursor != null && !cursor.isClosed() && cursor.getCount() > 0) {
                        cursorAdapter.notifyDataSetChanged();
                        listView.setSelection(listPosition);
                        listView.setVisibility(View.VISIBLE);
                    }
                }
            });

            if (markSeen) {
                // mark all SMS are seen
                new SMSSeenMarker(context).execute();
            }

            progress.dismiss();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            cursorAdapter.changeCursor(null);
            cursorAdapter.notifyDataSetChanged();
            progress.dismiss();
        }
    }

//----------------------------------------------------------------------

    // Async task - marks all SMS are seen
    private static class SMSSeenMarker extends AsyncTask<Void, Void, Void> {
        private Context context;

        SMSSeenMarker(Context context) {
            this.context = context;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(context);
            db.setSMSMessagesSeen(context);
            return null;
        }
    }
}
