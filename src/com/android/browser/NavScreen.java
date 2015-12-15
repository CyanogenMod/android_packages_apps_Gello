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

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RelativeLayout;

import com.android.browser.NavTabScroller.OnRemoveListener;
import com.android.browser.mdm.IncognitoRestriction;

import java.util.HashMap;

public class NavScreen extends RelativeLayout
        implements OnClickListener, OnMenuItemClickListener {


    private final UiController mUiController;
    private final PhoneUi mUi;
    private final Activity mActivity;

    private View mToolbarLayout;
    private ImageButton mMore;
    private ImageButton mNewTab;
    private ImageButton mNewIncognitoTab;

    private NavTabScroller mScroller;
    private TabAdapter mAdapter;
    private int mOrientation;
    private HashMap<Tab, View> mTabViews;

    public NavScreen(Activity activity, UiController ctl, PhoneUi ui) {
        super(activity);
        mActivity = activity;
        mUiController = ctl;
        mUi = ui;
        mOrientation = activity.getResources().getConfiguration().orientation;
        init();
    }

    protected void showPopupMenu() {
        if (mUiController instanceof Controller) {
            PopupMenu popup = new PopupMenu(getContext(), mMore);
            Menu menu = popup.getMenu();

            Controller controller = (Controller) mUiController;
            controller.onPrepareOptionsMenu(menu);
        }
    }

    public NavTabScroller getScroller() {
        return mScroller;
    }

    public ObjectAnimator createToolbarInAnimator() {
        return ObjectAnimator.ofFloat(mToolbarLayout, "translationY",
                -getResources().getDimensionPixelSize(R.dimen.toolbar_height), 0f);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return mUiController.onOptionsItemSelected(item);
    }

    @Override
    protected void onConfigurationChanged(Configuration newconfig) {
        if (newconfig.orientation != mOrientation) {
            mOrientation = newconfig.orientation;

            // the only thing we need to change is the orientation. see nav_screen.xml
            //final int prevScroll = mScroller.getScrollValue();
            mScroller.setOrientation(mOrientation == Configuration.ORIENTATION_LANDSCAPE
                    ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
            mScroller.setScrollOnNextLayout();
        }
    }

    public void refreshAdapter() {
        mNewTab.setOnClickListener(this);
        mNewIncognitoTab.setOnClickListener(this);
        mScroller.handleDataChanged(
                mUiController.getTabControl().getTabPosition(mUi.getActiveTab()));
    }



    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.nav_screen, this);
        setContentDescription(getContext().getResources().getString(
                R.string.accessibility_transition_navscreen));
        mToolbarLayout = findViewById(R.id.nav_toolbar_animate);
        mNewIncognitoTab = (ImageButton) findViewById(R.id.newincognitotab);
        IncognitoRestriction.getInstance().registerControl(mNewIncognitoTab);
        mNewTab = (ImageButton) findViewById(R.id.newtab);
        mMore = (ImageButton) findViewById(R.id.more);
        mNewIncognitoTab.setOnClickListener(this);
        mNewTab.setOnClickListener(this);
        mMore.setOnClickListener(this);
        mScroller = (NavTabScroller) findViewById(R.id.scroller);
        TabControl tc = mUiController.getTabControl();
        mTabViews = new HashMap<Tab, View>(tc.getTabCount());
        mAdapter = new TabAdapter(getContext(), tc);
        mScroller.setOrientation(mOrientation == Configuration.ORIENTATION_LANDSCAPE
                ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        // update state for active tab
        mScroller.setAdapter(mAdapter,
                mUiController.getTabControl().getTabPosition(mUi.getActiveTab()));
        mScroller.setOnRemoveListener(new OnRemoveListener() {
            public void onRemovePosition(int pos) {
                Tab tab = mAdapter.getItem(pos);
                onCloseTab(tab);
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (mNewTab == v) {
            openNewTab();
            mNewIncognitoTab.setOnClickListener(null);
        } else if (mNewIncognitoTab == v) {
            openNewIncognitoTab();
            mNewTab.setOnClickListener(null);
        } else if (mMore == v) {
            showPopupMenu();
        }
    }

    private void onCloseTab(Tab tab) {
        if (tab != null) {
            if (tab == mUiController.getCurrentTab()) {
                mUiController.closeCurrentTab();
            } else {
                mUiController.closeTab(tab);
            }
            mTabViews.remove(tab);
        }
    }

    private void openNewIncognitoTab() {
        final Tab tab = mUiController.openIncognitoTab();
        if (tab != null) {
            mUiController.setBlockEvents(true);
            final int tix = mUi.mTabControl.getTabPosition(tab);
            switchToTab(tab);
            mUi.hideNavScreen(tix, true);
            mScroller.handleDataChanged(tix);
            mUiController.setBlockEvents(false);
        }
    }

    private void openNewTab() {
        // need to call openTab explicitely with setactive false
        final Tab tab = mUiController.openTab(BrowserSettings.getInstance().getHomePage(),
                false, false, false);
        if (tab != null) {
            mUiController.setBlockEvents(true);
            final int tix = mUi.mTabControl.getTabPosition(tab);
            switchToTab(tab);
            mUi.hideNavScreen(tix, true);
            mScroller.handleDataChanged(tix);
            mUiController.setBlockEvents(false);
        }
    }

    private void switchToTab(Tab tab) {
        if (tab != mUi.getActiveTab()) {
            mUiController.setActiveTab(tab);
        }
    }

    protected void close(int position) {
        close(position, true);
    }

    protected void close(int position, boolean animate) {
        mUi.hideNavScreen(position, animate);
    }

    protected NavTabView getTabView(int pos) {
        return mScroller.getTabView(pos);
    }

    class TabAdapter extends BaseAdapter {

        Context context;
        TabControl tabControl;

        public TabAdapter(Context ctx, TabControl tc) {
            context = ctx;
            tabControl = tc;
        }

        @Override
        public int getCount() {
            return tabControl.getTabCount();
        }

        @Override
        public Tab getItem(int position) {
            return tabControl.getTab(position);
        }

        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final NavTabView tabview = new NavTabView(mActivity);
            final Tab tab = getItem(position);
            tabview.setWebView(tab);
            mTabViews.put(tab, tabview.mImage);
            tabview.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (tabview.isTitle(v)) {
                        switchToTab(tab);
                        close(position, false);
                        mUi.editUrl(false, true);
                    } else if (tabview.isWebView(v)) {
                        close(position);
                    }
                }
            });
            return tabview;
        }

    }
}
