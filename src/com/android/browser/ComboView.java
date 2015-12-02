/*
 * Copyright (c) 2015, The Linux Foundation. All rights reserved.
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
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.android.browser.UI.ComboViews;

import java.util.Iterator;
import java.util.Set;

public class ComboView extends LinearLayout
        implements CombinedBookmarksCallbacks, View.OnLayoutChangeListener {

    private Activity mActivity;
    private ViewPager mViewPager;
    private AnimationSet mInAnimation;
    private AnimationSet mOutAnimation;
    private int mActionBarContainerId;

    private ComboTabsAdapter mTabsAdapter;
    private Bundle mExtraArgs;

    public ComboView(Context context) {
        super(context);
    }

    public ComboView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ComboView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setupViews(Activity activity) {
        mActivity = activity;

        this.setId(R.id.combo_view_container);

        mViewPager = (ViewPager)this.findViewById(R.id.combo_view_pager);
        mViewPager.setId(R.id.tab_view); // ???

        mInAnimation = (AnimationSet) AnimationUtils.loadAnimation(mActivity, R.anim.combo_view_enter);
        mOutAnimation = (AnimationSet) AnimationUtils.loadAnimation(mActivity, R.anim.combo_view_exit);

        final ActionBar bar = activity.getActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayOptions(0);

        mActionBarContainerId = getResources().getIdentifier("action_bar_container", "id", "android");
        ViewGroup actionBarContainer = (ViewGroup) mActivity.getWindow().getDecorView().findViewById(mActionBarContainerId);
        if (actionBarContainer != null) {
            actionBarContainer.addOnLayoutChangeListener(this);
        }
    }

    private View getScrollingTabContainerView() {
        ViewGroup actionBarContainer = (ViewGroup) mActivity.getWindow().getDecorView().findViewById(mActionBarContainerId);
        int count = actionBarContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = actionBarContainer.getChildAt(i);
            if (child instanceof HorizontalScrollView) {
                return child;
            }
        }
        return null;
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (mActivity != null) {
            if (!isShowing()) {
                View container = getScrollingTabContainerView();
                if (container != null) {
                    container.setVisibility(View.INVISIBLE);
                }
            }
        }

    }

    private boolean compareArgs(Bundle b1, Bundle b2) {

        if(b1.size() != b2.size()) {
            return false;
        }

        Set<String> keys = b1.keySet();
        Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            String key = it.next();
            final Object v1 = b1.get(key);
            final Object v2 = b2.get(key);
            if (!b2.containsKey(key)) {
                return false;
            } else if (v1 == null) {
                if (v2 != null) {
                    return false;
                }
            } else if (!v1.equals(v2)) {
                return false;
            }
        }
        return true;
    }

    public void showViews(Activity activity, Bundle extras /*Bundle savedInstanceState*/) {
        Bundle args = extras.getBundle(ComboViewActivity.EXTRA_COMBO_ARGS);
        String svStr = extras.getString(ComboViewActivity.EXTRA_INITIAL_VIEW, null);
        ComboViews startingView = svStr != null ? ComboViews.valueOf(svStr) : ComboViews.Bookmarks;


        // Compare the items in args with old args and recreate the fragments if they don't match.
        if (mExtraArgs != null && !compareArgs(mExtraArgs, args)) {
            mTabsAdapter.removeAllTabs();
            mTabsAdapter = null;
        }

        final ActionBar bar = activity.getActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        if (BrowserActivity.isTablet(activity)) {
            bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
                    | ActionBar.DISPLAY_USE_LOGO);
            bar.setDisplayHomeAsUpEnabled(true);
        } else {
            bar.setDisplayOptions(0);
        }
        if (mTabsAdapter == null) {
            mExtraArgs = args;
            mTabsAdapter = new ComboTabsAdapter(activity, mViewPager);
            mTabsAdapter.addTab(bar.newTab().setText(R.string.bookmarks),
                    BrowserBookmarksPage.class, args);
            mTabsAdapter.addTab(bar.newTab().setText(R.string.history),
                    BrowserHistoryPage.class, args);
            mTabsAdapter.addTab(bar.newTab().setText(R.string.tab_snapshots),
                    BrowserSnapshotPage.class, args);
        }

        /*if (savedInstanceState != null) {
            bar.setSelectedNavigationItem(
                    savedInstanceState.getInt(STATE_SELECTED_TAB, 0));
        } else*/ {
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

        if (!bar.isShowing()) {
            View v = getScrollingTabContainerView();
            if (v != null) {
                v.setVisibility(View.VISIBLE);
            }
            bar.show();
        }

        if (!this.isShowing()) {
            this.setVisibility(View.VISIBLE);
            this.requestFocus();
            if (!(BrowserActivity.isTablet(activity))) {
                this.startAnimation(mInAnimation);
            }
        }
    }

    public boolean isShowing() {
        return (this.getVisibility() == View.VISIBLE);
    }

    public void hideViews() {
        if (isShowing()) {
            if (!(BrowserActivity.isTablet(mActivity)))
                this.startAnimation(mOutAnimation);
            this.setVisibility(View.INVISIBLE);
            mActionBarContainerId = getResources().getIdentifier("action_bar_container", "id", "android");
            ViewGroup actionBarContainer = (ViewGroup) mActivity.getWindow().getDecorView().findViewById(mActionBarContainerId);
            if (actionBarContainer != null) {
                actionBarContainer.removeOnLayoutChangeListener(this);
            }
            ActionBar actionBar = mActivity.getActionBar();
            actionBar.hide();
        }
    }

    //TODO: Save the selected tab on BrowserActivity's onSaveInstanceState
    /*public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_TAB,
                getActionBar().getSelectedNavigationIndex());
    }*/

    @Override
    public void openUrl(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.setClassName(getContext().getPackageName(), BrowserActivity.class.getName());
        i.putExtra(Controller.EXTRA_REQUEST_CODE, Controller.COMBO_VIEW);
        i.putExtra(Controller.EXTRA_RESULT_CODE, Activity.RESULT_OK);
        this.getContext().startActivity(i);
        hideViews();
    }

    @Override
    public void openInNewTab(String... urls) {
        Intent i = new Intent();
        i.putExtra(ComboViewActivity.EXTRA_OPEN_ALL, urls);
        i.putExtra(Controller.EXTRA_REQUEST_CODE, Controller.COMBO_VIEW);
        i.putExtra(Controller.EXTRA_RESULT_CODE, Activity.RESULT_OK);
        i.setClassName(getContext().getPackageName(), BrowserActivity.class.getName());
        this.getContext().startActivity(i);
        hideViews();
    }

    @Override
    public void close() {
        hideViews();
    }

    @Override
    public void openSnapshot(long id) {
        Intent i = new Intent();
        i.putExtra(ComboViewActivity.EXTRA_OPEN_SNAPSHOT, id);
        i.putExtra(Controller.EXTRA_REQUEST_CODE, Controller.COMBO_VIEW);
        i.putExtra(Controller.EXTRA_RESULT_CODE, Activity.RESULT_OK);
        i.setClassName(getContext().getPackageName(), BrowserActivity.class.getName());
        this.getContext().startActivity(i);
        hideViews();
    }
}
