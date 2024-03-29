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

package com.dandytek.sms_blocker.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.dandytek.sms_blocker.R;
import com.dandytek.sms_blocker.fragments.ContactsFragment;
import com.dandytek.sms_blocker.fragments.FragmentArguments;
import com.dandytek.sms_blocker.fragments.InformationFragment;
import com.dandytek.sms_blocker.fragments.JournalFragment;
import com.dandytek.sms_blocker.fragments.SMSConversationFragment;
import com.dandytek.sms_blocker.fragments.SMSConversationsListFragment;
import com.dandytek.sms_blocker.fragments.SMSSendFragment;
import com.dandytek.sms_blocker.fragments.SettingsFragment;
import com.dandytek.sms_blocker.utils.ContactsAccessHelper;
import com.dandytek.sms_blocker.utils.DatabaseAccessHelper;
import com.dandytek.sms_blocker.utils.DatabaseAccessHelper.Contact;
import com.dandytek.sms_blocker.utils.Permissions;
import com.dandytek.sms_blocker.utils.Settings;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String ACTION_JOURNAL = "com.dandytek.sms_blocker.ACTION_JOURNAL";
    public static final String ACTION_SMS_CONVERSATIONS = "com.dandytek.sms_blocker.ACTION_SMS_CONVERSATIONS";
    public static final String ACTION_SETTINGS = "com.dandytek.sms_blocker.ACTION_SETTINGS";
    public static final String ACTION_SMS_SEND_TO = "android.intent.action.SENDTO";

    private static final String CURRENT_ITEM_ID = "CURRENT_ITEM_ID";
    private FragmentSwitcher fragmentSwitcher = new FragmentSwitcher();
    private NavigationView navigationView;
    private DrawerLayout drawer;
    private int selectedMenuItemId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Settings.applyCurrentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // permissions
        Permissions.checkAndRequest(this);

        // init settings defaults
        Settings.initDefaults(this);

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // show custom toolbar shadow on pre LOLLIPOP devices
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            View view = findViewById(R.id.toolbar_shadow);
            if (view != null) {
                view.setVisibility(View.VISIBLE);
            }
        }




        // drawer
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.Open_navigation_drawer, R.string.Close_navigation_drawer);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // navigation menu
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // if there was a screen rotation
        int itemId;
        if (savedInstanceState != null) {
            // get saved current navigation menu item
            itemId = savedInstanceState.getInt(CURRENT_ITEM_ID);
        } else {
            // choose the fragment by activity's action
            String action = getIntent().getAction();
            action = (action == null ? "" : action);


            // checking
            Log.d("checking action:",String.valueOf(action));

            switch (action) {
                case ACTION_SMS_SEND_TO:
                    // show SMS sending activity
                    showSendSMSActivity();
                    // switch to SMS chat fragment
                    itemId = R.id.nav_sms;
                    break;
                case ACTION_SMS_CONVERSATIONS:
                    // switch to SMS chat fragment
                    itemId = R.id.nav_sms;
                    break;
                case ACTION_SETTINGS:
                    // switch to settings fragment
                    itemId = R.id.nav_settings;
                    break;
                case ACTION_JOURNAL:
                    // switch to journal fragment
                    itemId = R.id.nav_journal;
                    break;
                default:
                    if (Settings.getBooleanValue(this, Settings.GO_TO_JOURNAL_AT_START)) {
                        // switch to journal fragment
                        itemId = R.id.nav_journal;
                    } else {
                        // switch to SMS chat fragment
                        itemId = R.id.nav_sms;
                    }
                    break;
            }


            // checking
            Log.d("checking itemId:",String.valueOf(itemId));

            // switch to chosen fragment
            fragmentSwitcher.switchFragment(itemId);
        }

        // select navigation menu item
        selectNavigationMenuItem(itemId);


        firebaseOfflineDataCollection();

       // startService(new Intent(this, FirebaseSpamTagService.class));



    }








    public void firebaseOfflineDataCollection(){

        Context context = getApplicationContext();
        final DatabaseAccessHelper fs_db = DatabaseAccessHelper.getInstance(context);

        if (fs_db != null)
        {

            // Access a Cloud Firestore instance from your Activity
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Log.d("testing","test firestore");

            final String return_data;


            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build();



// The default cache size threshold is 100 MB. Configure "setCacheSizeBytes"
// for a different threshold (minimum 1 MB) or set to "CACHE_SIZE_UNLIMITED"
// to disable clean-up.

            settings = new FirebaseFirestoreSettings.Builder()
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build();
            db.setFirestoreSettings(settings);


            final CollectionReference spamRef = db.collection("spam_sms");





            db.collection("spam_sms").whereEqualTo("type", "blacklist")
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot snapshots,
                                            @Nullable FirebaseFirestoreException e) {
                            if (e != null) {
                                Log.w("firestore listen error", "listen:error", e);
                                return;
                            }

                            assert snapshots != null;
                            for (DocumentChange dc : snapshots.getDocumentChanges()) {

                                // Query query = spamRef.whereArrayContains("type", "blacklist");
                                Map<String, Object> test_data = new HashMap<>();
                                test_data = dc.getDocument().getData();
                                Log.d("firestore key val",test_data.get("sender").toString());
                                String person = String.valueOf(test_data.get("sender"));
                                String sms_number = person;

                                Log.d("firestore type: ", String.valueOf(Contact.TYPE_FS_BLACK_LIST));
                                Log.d("firestore person",person);
                                Log.d("firestore sms_number",sms_number);

                                //assert fs_db != null;



                                if (person != null)
                                    fs_db.addContact(Contact.TYPE_FS_BLACK_LIST, person,sms_number);





                                // Log.d("firestore query: ", query.toString());

                                switch (dc.getType()) {
                                    case ADDED:
                                        Log.d("firestore data added", "New data: " + test_data);
                                        // Log.d("firestore added class", "New class: " + test_data.getClass().getName());
                                        break;
                                    case MODIFIED:
                                        Log.d("firestore data mod", "Modified data: " + test_data);
                                        break;
                                    case REMOVED:
                                        Log.d("firestore data removed", "Removed data: " + test_data);
                                        break;
                                }
                            }

                        }
                    });



        }





    }




    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_ITEM_ID, selectedMenuItemId);
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (!fragmentSwitcher.onBackPressed()) {
                if (!Settings.getBooleanValue(this, Settings.DONT_EXIT_ON_BACK_PRESSED)) {
                    super.onBackPressed();
                }
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        // exit item was clicked
        if (itemId == R.id.nav_exit) {
            finish();
            return true;
        }

        // switch to the new fragment
        fragmentSwitcher.switchFragment(itemId);
        drawer.closeDrawer(GravityCompat.START);

        // Normally we don't need to select navigation items manually. But in API 10
        // (and maybe some another) there is bug of menu item selection/deselection.
        // To resolve this problem we deselect the old selected item and select the
        // new one manually. And it's why we return false in the current method.
        // This way of deselection of the item was found as the most appropriate.
        // Because of some side effects of all others tried.
        selectNavigationMenuItem(itemId);

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // check for result code from the child activity (it could be a dialog-activity)
        if (requestCode == 0 && resultCode == RESULT_OK) {
            fragmentSwitcher.updateFragment();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        // process permissions results
        Permissions.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // check granted permissions and notify about not granted
        Permissions.notifyIfNotGranted(this);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            } else {
                drawer.openDrawer(GravityCompat.START);
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

//----------------------------------------------------------------------------

    private void selectNavigationMenuItem(int itemId) {
        navigationView.getMenu().clear();
        navigationView.inflateMenu(R.menu.activity_main_drawer);
        navigationView.getMenu().findItem(itemId).setChecked(true);
        // save selected item
        selectedMenuItemId = itemId;
        // checking
        Log.d("Menu item id:",String.valueOf(itemId));
    }

    // Switcher of activity's fragments
    private class FragmentSwitcher implements FragmentArguments {
        private final String CURRENT_FRAGMENT = "CURRENT_FRAGMENT";
        private ContactsFragment blackListFragment = new ContactsFragment();
        private ContactsFragment whiteListFragment = new ContactsFragment();
        private JournalFragment journalFragment = new JournalFragment();
        private SettingsFragment settingsFragment = new SettingsFragment();
        private InformationFragment informationFragment = new InformationFragment();
        private SMSConversationsListFragment smsFragment = new SMSConversationsListFragment();

        boolean onBackPressed() {
            return journalFragment.dismissSnackBar() ||
                    blackListFragment.dismissSnackBar() ||
                    whiteListFragment.dismissSnackBar();
        }

        // Switches fragment by navigation menu item
        void switchFragment(@IdRes int itemId) {
            Intent intent = getIntent();
            // passing intent's extra to the fragment
            Bundle extras = intent.getExtras();
            // checking
            Log.d("checking extras:",String.valueOf(extras));

            Bundle arguments = (extras != null ? new Bundle(extras) : new Bundle());

            // checking
            Log.d("checking arguments:",String.valueOf(arguments));



            switch (itemId) {
                case R.id.nav_journal:
                    arguments.putString(TITLE, getString(R.string.Journal));
                    switchFragment(journalFragment, arguments);
                    break;
                case R.id.nav_black_list:
                    arguments.putString(TITLE, getString(R.string.Black_list));
                    arguments.putInt(CONTACT_TYPE, Contact.TYPE_BLACK_LIST);
                    switchFragment(blackListFragment, arguments);
                    break;
                case R.id.nav_white_list:
                    arguments.putString(TITLE, getString(R.string.White_list));
                    arguments.putInt(CONTACT_TYPE, Contact.TYPE_WHITE_LIST);
                    switchFragment(whiteListFragment, arguments);
                    break;
                case R.id.nav_sms:
                    arguments.putString(TITLE, getString(R.string.Messaging));
                    switchFragment(smsFragment, arguments);
                    break;
                case R.id.nav_settings:
                    arguments.putString(TITLE, getString(R.string.Settings));
                    switchFragment(settingsFragment, arguments);
                    break;
                default:
                    arguments.putString(TITLE, getString(R.string.Information));
                    switchFragment(informationFragment, arguments);
                    break;
            }

            // remove used extras
            intent.removeExtra(LIST_POSITION);
        }

        // Switches to passed fragment
        private void switchFragment(Fragment fragment, Bundle arguments) {
            // replace the current showed fragment
            Fragment current = getSupportFragmentManager().findFragmentByTag(CURRENT_FRAGMENT);

            // checking
            Log.d("checking Fragment:",String.valueOf(current));

            if (current != fragment) {
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction().
                        replace(R.id.frame_layout, fragment, CURRENT_FRAGMENT).commit();
            }
        }

        // Updates the current fragment
        private void updateFragment() {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(CURRENT_FRAGMENT);
            if (fragment != null) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.detach(fragment).attach(fragment).commit();
            }
        }
    }

    // Shows the activity of SMS sending
    private void showSendSMSActivity() {
        Uri uri = getIntent().getData();
        if (uri == null) {
            return;
        }

        // get phone number where to send the SMS
        String ssp = uri.getSchemeSpecificPart();
        String number = ContactsAccessHelper.normalizePhoneNumber(ssp);
        if (number.isEmpty()) {
            return;
        }

        // find person by phone number in contacts
        String person = null;
        ContactsAccessHelper db = ContactsAccessHelper.getInstance(this);
        Contact contact = db.getContact(this, number);
        if (contact != null) {
            person = contact.name;
        }

        // get SMS thread id by phone number
        int threadId = db.getSMSThreadIdByNumber(this, number);
        if (threadId >= 0) {
            // get the count of unread sms of the thread
            int unreadCount = db.getSMSMessagesUnreadCountByThreadId(this, threadId);

            // open thread's SMS conversation activity
            Bundle arguments = new Bundle();
            arguments.putInt(FragmentArguments.THREAD_ID, threadId);
            arguments.putInt(FragmentArguments.UNREAD_COUNT, unreadCount);
            arguments.putString(FragmentArguments.CONTACT_NUMBER, number);
            String title = (person != null ? person : number);
            CustomFragmentActivity.show(this, title, SMSConversationFragment.class, arguments);
        }

        // open SMS sending activity
        Bundle arguments = new Bundle();
        arguments.putString(FragmentArguments.CONTACT_NAME, person);
        arguments.putString(FragmentArguments.CONTACT_NUMBER, number);
        String title = getString(R.string.New_message);
        CustomFragmentActivity.show(this, title, SMSSendFragment.class, arguments);
    }
}
