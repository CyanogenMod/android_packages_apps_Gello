/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.browser;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;

import com.android.browser.UI.ComboViews;

public class ComboViewActivity extends Activity implements CombinedBookmarksCallbacks {

    private static final String LOGTAG = "ComboViewActivity";
    private static final String STATE_SELECTED_TAB = "tab";
    public static final String EXTRA_COMBO_ARGS = "combo_args";
    public static final String EXTRA_INITIAL_VIEW = "initial_view";

    public static final String EXTRA_OPEN_SNAPSHOT = "snapshot_id";
    public static final String EXTRA_OPEN_ALL = "open_all";
    public static final String EXTRA_CURRENT_URL = "url";
    private ViewPager mViewPager;
    private ComboTabsAdapter mTabsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!EngineInitializer.isInitialized()) {
            Log.e(LOGTAG, "Engine not Initialized");
            EngineInitializer.initializeSync((Context) getApplicationContext());
        }
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        Bundle extras = getIntent().getExtras();
        Bundle args = extras.getBundle(EXTRA_COMBO_ARGS);
        String svStr = extras.getString(EXTRA_INITIAL_VIEW, null);
        ComboViews startingView = svStr != null
                ? ComboViews.valueOf(svStr)
                : ComboViews.Bookmarks;
        mViewPager = new ViewPager(this);
        mViewPager.setId(R.id.tab_view);
        setContentView(mViewPager);

        final ActionBar bar = getActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        if (BrowserActivity.isTablet(this)) {
            bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
                    | ActionBar.DISPLAY_USE_LOGO);
            bar.setHomeButtonEnabled(true);
        } else {
            bar.setDisplayOptions(0);
        }

        mTabsAdapter = new ComboTabsAdapter(this, mViewPager);
        mTabsAdapter.addTab(bar.newTab().setText(R.string.bookmarks),
                BrowserBookmarksPage.class, args);
        mTabsAdapter.addTab(bar.newTab().setText(R.string.history),
                BrowserHistoryPage.class, args);
        mTabsAdapter.addTab(bar.newTab().setText(R.string.tab_snapshots),
                BrowserSnapshotPage.class, args);

        if (savedInstanceState != null) {
            bar.setSelectedNavigationItem(
                    savedInstanceState.getInt(STATE_SELECTED_TAB, 0));
        } else {
            switch (startingView) {
            case Bookmarks:
                mViewPager.setCurrentItem(0);
                break;
            case History:
                mViewPager.setCurrentItem(1);
                break;
            case Snapshots:
                mViewPager.setCurrentItem(2);
                break;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_TAB,
                getActionBar().getSelectedNavigationIndex());
    }

    @Override
    public void openUrl(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        setResult(RESULT_OK, i);
        finish();
    }

    @Override
    public void openInNewTab(String... urls) {
        Intent i = new Intent();
        i.putExtra(EXTRA_OPEN_ALL, urls);
        setResult(RESULT_OK, i);
        finish();
    }

    @Override
    public void close() {
        finish();
    }

    @Override
    public void openSnapshot(long id) {
        Intent i = new Intent();
        i.putExtra(EXTRA_OPEN_SNAPSHOT, id);
        setResult(RESULT_OK, i);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.combined, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.preferences_menu_id) {
            String url = getIntent().getStringExtra(EXTRA_CURRENT_URL);
            BrowserPreferencesPage.startPreferencesForResult(this, url, Controller.PREFERENCES_PAGE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static String makeFragmentName(int viewId, int index) {
        return "android:switcher:" + viewId + ":" + index;
    }

}
