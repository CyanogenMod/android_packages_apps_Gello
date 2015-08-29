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

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityManager;

import org.codeaurora.swe.BrowserCommandLine;
import org.codeaurora.swe.WebView;

import android.widget.FrameLayout;

/**
 * Base class for a title bar used by the browser.
 */
public class TitleBar extends FrameLayout implements ViewTreeObserver.OnPreDrawListener {

    private static final int PROGRESS_MAX = 100;
    private static final float ANIM_TITLEBAR_DECELERATE = 2.5f;

    private UiController mUiController;
    private BaseUi mBaseUi;
    private FrameLayout mContentView;
    private PageProgressView mProgress;
    private AccessibilityManager mAccessibilityManager;

    private NavigationBarBase mNavBar;
    private SnapshotBar mSnapshotBar;

    //state
    private boolean mShowing;
    private boolean mInLoad;
    private boolean mIsFixedTitleBar;
    private float mCurrentTranslationY;
    private boolean mUpdateTranslationY = false;

    public TitleBar(Context context, UiController controller, BaseUi ui,
            FrameLayout contentView) {
        super(context, null);
        mUiController = controller;
        mBaseUi = ui;
        mContentView = contentView;
        mAccessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        initLayout(context);
        setFixedTitleBar();
    }

    private void initLayout(Context context) {
        LayoutInflater factory = LayoutInflater.from(context);
        factory.inflate(R.layout.title_bar, this);
        mProgress = (PageProgressView) findViewById(R.id.progress);
        mNavBar = (NavigationBarBase) findViewById(R.id.taburlbar);
        mNavBar.setTitleBar(this);
    }

    private void inflateSnapshotBar() {
        if (mSnapshotBar != null) {
            return;
        }

        ViewStub stub = (ViewStub) findViewById(R.id.snapshotbar_stub);
        mSnapshotBar = (SnapshotBar) stub.inflate();
        mSnapshotBar.setTitleBar(this);
    }

    @Override
    protected void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        setFixedTitleBar();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mBaseUi.setContentViewMarginTop(0);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        mCurrentTranslationY = this.getTranslationY();
        if (mCurrentTranslationY < 0) {
            mUpdateTranslationY = true;
            this.setTranslationY(0);

            final ViewTreeObserver observer = this.getViewTreeObserver();
            observer.addOnPreDrawListener(this);
        }
    }

    @Override
    public boolean onPreDraw() {
        if (mUpdateTranslationY) {
            this.setTranslationY(mCurrentTranslationY);
            mUpdateTranslationY = false;
        }
        final ViewTreeObserver observer = this.getViewTreeObserver();
        observer.removeOnPreDrawListener(this);
        return true;
    }

    private void setFixedTitleBar() {
        boolean isFixed = !getContext().getResources().getBoolean(R.bool.hide_title) ||
                BrowserCommandLine.hasSwitch(BrowserSwitches.DISABLE_TOP_CONTROLS);

        isFixed |= mAccessibilityManager.isEnabled() &&
            mAccessibilityManager.isTouchExplorationEnabled();
        // If getParent() returns null, we are initializing
        ViewGroup parent = (ViewGroup)getParent();
        if (mIsFixedTitleBar == isFixed && parent != null) return;
        mIsFixedTitleBar = isFixed;
        showTopControls(false);
        if (parent != null) {
            parent.removeView(this);
        }
        mContentView.addView(this, makeLayoutParams());
        mBaseUi.setContentViewMarginTop(0);
    }

    public BaseUi getUi() {
        return mBaseUi;
    }

    public UiController getUiController() {
        return mUiController;
    }

    void setShowProgressOnly(boolean progress) {
        if (progress && !wantsToBeVisible()) {
            mNavBar.setVisibility(View.GONE);
        } else {
            mNavBar.setVisibility(View.VISIBLE);
        }
    }

    boolean isShowing() {
        return mShowing;
    }

    private int getVisibleTitleHeight() {
        Tab tab = mBaseUi.getActiveTab();
        WebView webview = tab != null ? tab.getWebView() : null;
        return webview != null ? webview.getVisibleTitleHeight() : 0;
    }

    protected void hideTopControls(boolean animate) {
        if (mIsFixedTitleBar)
            return;
        Tab tab = mBaseUi.getActiveTab();
        WebView view = tab != null ? tab.getWebView() : null;
        if (view != null) {
            view.updateTopControls(true, false, animate);
        }
        mShowing = false;
    }

    protected void showTopControls(boolean animate) {
        Tab tab = mBaseUi.getActiveTab();
        WebView view = tab != null ? tab.getWebView() : null;
        if (view != null) {
            view.updateTopControls(false, true, animate);
        }
        mShowing = true;
    }

    protected void enableTopControls(boolean animate) {
        if (mIsFixedTitleBar || mNavBar.getTrustLevel() == SiteTileView.TRUST_AVOID
                || mNavBar.getTrustLevel() == SiteTileView.TRUST_UNTRUSTED)
            return;
        Tab tab = mBaseUi.getActiveTab();
        WebView view = tab != null ? tab.getWebView() : null;
        if (view != null)
            view.updateTopControls(true, true, animate);
    }


    /**
     * Update the progress, from 0 to 100.
     */
    public void setProgress(int newProgress) {
        if (newProgress >= PROGRESS_MAX) {
            mProgress.setProgress(PageProgressView.MAX_PROGRESS);
            mProgress.setVisibility(View.GONE);
            mInLoad = false;
            mNavBar.onProgressStopped();
            //onPageFinished
            enableTopControls(true);

        } else {
            if (!mInLoad) {
                mProgress.setVisibility(View.VISIBLE);
                mInLoad = true;
                mNavBar.onProgressStarted();
                mProgress.onProgressStarted();
                //onPageStarted
            }
            mProgress.setProgress(newProgress * PageProgressView.MAX_PROGRESS
                    / PROGRESS_MAX);
            showTopControls(false);
        }
    }

    public int getEmbeddedHeight() {
        if (mIsFixedTitleBar) return 0;
        return calculateEmbeddedHeight();
    }

     public boolean isFixed() {
        return mIsFixedTitleBar;
    }

    int calculateEmbeddedHeight() {
        int height = mNavBar.getHeight();
        return height;
    }

    public boolean wantsToBeVisible() {
        return (mSnapshotBar != null && mSnapshotBar.getVisibility() == View.VISIBLE
                    && mSnapshotBar.isAnimating());
    }

    public boolean isEditingUrl() {
        return mNavBar.isEditingUrl();
    }

    public WebView getCurrentWebView() {
        Tab t = mBaseUi.getActiveTab();
        if (t != null) {
            return t.getWebView();
        } else {
            return null;
        }
    }

    public PageProgressView getProgressView() {
        return mProgress;
    }

    public NavigationBarBase getNavigationBar() {
        return mNavBar;
    }

    public boolean isInLoad() {
        return mInLoad;
    }

    private ViewGroup.LayoutParams makeLayoutParams() {
        return new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
    }

    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        return mBaseUi.isCustomViewShowing() ? false :
            super.requestFocus(direction, previouslyFocusedRect);
    }

    @Override
    public View focusSearch(View focused, int dir) {
        WebView web = getCurrentWebView();
        if (FOCUS_DOWN == dir && hasFocus() && web != null
                && web.hasFocusable() && web.getParent() != null) {
            return web;
        }
        return super.focusSearch(focused, dir);
    }

    public void onTabDataChanged(Tab tab) {
        if (mSnapshotBar != null) {
            mSnapshotBar.onTabDataChanged(tab);
        }

        if (tab.isSnapshot() || tab.isDistilled()) {
            inflateSnapshotBar();
            mSnapshotBar.setVisibility(VISIBLE);
            mNavBar.setVisibility(GONE);
            if  (tab.isDistilled()) {
                mSnapshotBar.setTitle(tab.getWebView().getTitle());
                mSnapshotBar.setDate(tab.getNonDistilledUrl());
                mSnapshotBar.setSnapshoticonVisibility(View.GONE);
                mSnapshotBar.setFaviconVisibility(View.GONE);
                mSnapshotBar.setReadericonVisibility(View.VISIBLE);
            } else {
                mSnapshotBar.setSnapshoticonVisibility(View.VISIBLE);
                mSnapshotBar.setFaviconVisibility(View.GONE); // Snapshot Tabs don't have a Favicon
                mSnapshotBar.setReadericonVisibility(View.GONE);
            }
        } else {
            if (mSnapshotBar != null) {
                mSnapshotBar.setVisibility(GONE);
            }
            mNavBar.setVisibility(VISIBLE);
        }
    }

    public void onScrollChanged() {
        if (!mShowing && !mIsFixedTitleBar) {
            setTranslationY(getVisibleTitleHeight() - getEmbeddedHeight());
        }
    }

    public void onResume() {
        setFixedTitleBar();
    }

}
