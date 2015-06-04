/*
 * Copyright (C) 2010 The Android Open Source Project
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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.webkit.WebChromeClient;

import org.codeaurora.swe.WebView;
import org.codeaurora.swe.util.ColorUtils;

import com.android.browser.R;

import java.util.List;

/**
 * Ui for xlarge screen sizes
 */
public class XLargeUi extends BaseUi {

    private static final String LOGTAG = "XLargeUi";

    private PaintDrawable mFaviconBackground;

    private ActionBar mActionBar;
    private TabBar mTabBar;

    private NavigationBarTablet mNavBar;
    private ComboView mComboView;

    private Handler mHandler;

    /**
     * @param browser
     * @param controller
     */
    public XLargeUi(Activity browser, UiController controller) {
        super(browser, controller);
        mHandler = new Handler();
        mNavBar = (NavigationBarTablet) mTitleBar.getNavigationBar();
        mTabBar = new TabBar(mActivity, mUiController, this);
        mActionBar = mActivity.getActionBar();
        setupActionBar();
    }

    private void setupActionBar() {
        mActionBar.setHomeButtonEnabled(false);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        mActionBar.setCustomView(mTabBar);
    }

    public void showComboView(ComboViews startWith, Bundle extras) {
        if (mComboView == null) {
            ViewStub stub = (ViewStub) mActivity.getWindow().getDecorView().findViewById(R.id.combo_view_stub);
            mComboView = (ComboView) stub.inflate();
            mComboView.setVisibility(View.GONE);
            mComboView.setupViews(mActivity);
        }
        mNavBar.setVisibility(View.GONE);
        if (mActionBar != null)
            mActionBar.hide();
        Bundle b = new Bundle();
        b.putString(ComboViewActivity.EXTRA_INITIAL_VIEW, startWith.name());
        b.putBundle(ComboViewActivity.EXTRA_COMBO_ARGS, extras);
        Tab t = getActiveTab();
        if (t != null) {
            b.putString(ComboViewActivity.EXTRA_CURRENT_URL, t.getUrl());
        }
        mComboView.showViews(mActivity, b);
    }

    @Override
    public void hideComboView() {
        if (showingComboView()) {
            mComboView.hideViews();
            mActionBar = mActivity.getActionBar();
            setupActionBar();
            if (mActionBar != null)
                mActionBar.show();
            mNavBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onBackKey() {
        if (showingComboView()) {
            hideComboView();
            return true;
        }
        return super.onBackKey();
    }

    private boolean showingComboView() {
        return mComboView != null && mComboView.getVisibility() == View.VISIBLE;
    }

    @Override
    public boolean isComboViewShowing() {
        return showingComboView();
    }

    private void checkHideActionBar() {
    }

    @Override
    public void onResume() {
        super.onResume();
        mNavBar.clearCompletions();
        checkHideActionBar();
    }

    @Override
    public void onDestroy() {
        hideTitleBar();
    }

    void stopWebViewScrolling() {
        BrowserWebView web = (BrowserWebView) mUiController.getCurrentWebView();
        if (web != null) {
            web.stopScroll();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem bm = menu.findItem(R.id.bookmarks_menu_id);
        if (bm != null) {
            bm.setVisible(false);
        }

        menu.setGroupVisible(R.id.NAV_MENU, false);

        return true;
    }


    // WebView callbacks

    @Override
    public void addTab(Tab tab) {
        mTabBar.onNewTab(tab);
    }

    protected void onAddTabCompleted(Tab tab) {
        checkHideActionBar();
    }

    @Override
    public void setActiveTab(final Tab tab) {
        mTitleBar.cancelTitleBarAnimation(true);
        mTitleBar.setSkipTitleBarAnimations(true);
        super.setActiveTab(tab);
        BrowserWebView view = (BrowserWebView) tab.getWebView();
        // TabControl.setCurrentTab has been called before this,
        // so the tab is guaranteed to have a webview
        if (view == null) {
            Log.e(LOGTAG, "active tab with no webview detected");
            return;
        }
        mTabBar.onSetActiveTab(tab);
        updateLockIconToLatest(tab);
        mTitleBar.setSkipTitleBarAnimations(false);
    }

    @Override
    public void updateTabs(List<Tab> tabs) {
        mTabBar.updateTabs(tabs);
        checkHideActionBar();
    }

    @Override
    public void removeTab(Tab tab) {
        mTitleBar.cancelTitleBarAnimation(true);
        mTitleBar.setSkipTitleBarAnimations(true);
        super.removeTab(tab);
        mTabBar.onRemoveTab(tab);
        mTitleBar.setSkipTitleBarAnimations(false);
    }

    @Override
    public void showCustomView(View view, int requestedOrientation,
                               WebChromeClient.CustomViewCallback callback) {
        super.showCustomView(view, requestedOrientation, callback);
        if (mActionBar != null)
            mActionBar.hide();
    }

    protected void onRemoveTabCompleted(Tab tab) {
        checkHideActionBar();
    }

    int getContentWidth() {
        if (mContentView != null) {
            return mContentView.getWidth();
        }
        return 0;
    }

    @Override
    public void editUrl(boolean clearInput, boolean forceIME) {
        super.editUrl(clearInput, forceIME);
    }

    // action mode callbacks

    @Override
    public void onActionModeStarted(ActionMode mode) {
        if (!mTitleBar.isEditingUrl()) {
            // hide the title bar when CAB is shown
            hideTitleBar();
        }
    }

    @Override
    public void onActionModeFinished(boolean inLoad) {
        checkHideActionBar();
        if (inLoad) {
            // the titlebar was removed when the CAB was shown
            // if the page is loading, show it again
            showTitleBar();
        }
    }

    @Override
    protected void updateNavigationState(Tab tab) {
        mNavBar.updateNavigationState(tab);
    }

    @Override
    public void setUrlTitle(Tab tab) {
        super.setUrlTitle(tab);
        mTabBar.onUrlAndTitle(tab, tab.getUrl(), tab.getTitle());
    }

    // Set the favicon in the title bar.
    @Override
    public void setFavicon(Tab tab) {
        super.setFavicon(tab);
        mTabBar.onFavicon(tab, tab.getFavicon());
        if (mActiveTab == tab) {
            int color = NavigationBarBase.getSiteIconColor(tab.getUrl());
            if (tab.hasFavicon()) {
                color = ColorUtils.getDominantColorForBitmap(tab.getFavicon());
            }
            mActionBar.setBackgroundDrawable(new ColorDrawable(color));
        }
    }

    @Override
    public void onHideCustomView() {
        super.onHideCustomView();
        checkHideActionBar();
        if (mActionBar != null)
            mActionBar.show();
    }

    @Override
    public boolean dispatchKey(int code, KeyEvent event) {
        if (mActiveTab != null) {
            WebView web = mActiveTab.getWebView();
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (code) {
                    case KeyEvent.KEYCODE_TAB:
                    case KeyEvent.KEYCODE_DPAD_UP:
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        if ((web != null) && web.hasFocus() && !mTitleBar.hasFocus()) {
                            editUrl(false, false);
                            return true;
                        }
                }
                boolean ctrl = event.hasModifiers(KeyEvent.META_CTRL_ON);
                if (!ctrl && isTypingKey(event) && !mTitleBar.isEditingUrl()) {
                    editUrl(true, false);
                    return mContentView.dispatchKeyEvent(event);
                }
            }
        }
        return false;
    }

    private boolean isTypingKey(KeyEvent evt) {
        return evt.getUnicodeChar() > 0;
    }

    TabBar getTabBar() {
        return mTabBar;
    }

    @Override
    public boolean shouldCaptureThumbnails() {
        return false;
    }

    private Drawable getFaviconBackground() {
        if (mFaviconBackground == null) {
            mFaviconBackground = new PaintDrawable();
            Resources res = mActivity.getResources();
            mFaviconBackground.getPaint().setColor(
                    res.getColor(R.color.tabFaviconBackground));
            mFaviconBackground.setCornerRadius(
                    res.getDimension(R.dimen.tab_favicon_corner_radius));
        }
        return mFaviconBackground;
    }

    @Override
    public Drawable getFaviconDrawable(Bitmap icon) {
        if (ENABLE_BORDER_AROUND_FAVICON) {
            Drawable[] array = new Drawable[2];
            array[0] = getFaviconBackground();
            if (icon == null) {
                array[1] = getGenericFavicon();
            } else {
                array[1] = new BitmapDrawable(mActivity.getResources(), icon);
            }
            LayerDrawable d = new LayerDrawable(array);
            d.setLayerInset(1, 2, 2, 2, 2);
            return d;
        }
        return icon == null ? getGenericFavicon() : new BitmapDrawable(mActivity.getResources(), icon);
    }

}
