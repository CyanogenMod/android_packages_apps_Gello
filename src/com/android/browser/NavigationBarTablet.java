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

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.android.browser.UI.ComboViews;
import com.android.browser.UrlInputView.StateListener;

public class NavigationBarTablet extends NavigationBarBase implements StateListener {

    private View mUrlContainer;
    private ImageButton mBackButton;
    private ImageButton mForwardButton;
    private ImageView mStar;
    private View mNavButtons;
    private boolean mHideNavButtons;

    public NavigationBarTablet(Context context) {
        super(context);
        init(context);
    }

    public NavigationBarTablet(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public NavigationBarTablet(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mHideNavButtons = getResources().getBoolean(R.bool.hide_nav_buttons);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mNavButtons = findViewById(R.id.navbuttons);
        mBackButton = (ImageButton) findViewById(R.id.back);
        mForwardButton = (ImageButton) findViewById(R.id.forward);
        mStar = (ImageView) findViewById(R.id.star);
        mUrlContainer = findViewById(R.id.urlbar_focused);
        mBackButton.setOnClickListener(this);
        mForwardButton.setOnClickListener(this);
        mStar.setOnClickListener(this);
        mUrlInput.setContainer(mUrlContainer);
        mUrlInput.setStateListener(this);
    }

    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        Resources res = getContext().getResources();
        mHideNavButtons = res.getBoolean(R.bool.hide_nav_buttons);
        if (mUrlInput.hasFocus()) {
            if (mHideNavButtons && (mNavButtons.getVisibility() == View.VISIBLE)) {
                hideNavButtons();
            } else if (!mHideNavButtons && (mNavButtons.getVisibility() == View.GONE)) {
                showNavButtons();
            }
        }
    }

    @Override
    public void setTitleBar(TitleBar titleBar) {
        super.setTitleBar(titleBar);
        setFocusState(false);
    }

    void updateNavigationState(Tab tab) {
        if (tab != null) {
            mBackButton.setEnabled(tab.canGoBack());
            mForwardButton.setEnabled(tab.canGoForward());
        }
    }

    @Override
    public void onTabDataChanged(Tab tab) {
        super.onTabDataChanged(tab);
        showHideStar(tab);
    }

    @Override
    public void setCurrentUrlIsBookmark(boolean isBookmark) {
        mStar.setActivated(isBookmark);
    }

    @Override
    public void onClick(View v) {
        if ((mBackButton == v) && (mUiController.getCurrentTab() != null)) {
            mUiController.getCurrentTab().goBack();
        } else if ((mForwardButton == v)  && (mUiController.getCurrentTab() != null)) {
            mUiController.getCurrentTab().goForward();
        } else if (mStar == v) {
            Intent intent = mUiController.createBookmarkCurrentPageIntent(true);
            if (intent != null) {
                getContext().startActivity(intent);
            }
        } else {
            super.onClick(v);
        }
    }

    @Override
    protected void setFocusState(boolean focus) {
        super.setFocusState(focus);
        if (focus) {
            if (mHideNavButtons) {
                hideNavButtons();
            }
            mStar.setVisibility(View.GONE);
        } else {
            if (mHideNavButtons) {
                showNavButtons();
            }
            showHideStar(mUiController.getCurrentTab());
        }
    }

    private void hideNavButtons() {
        int aw = mNavButtons.getMeasuredWidth();
        mNavButtons.setVisibility(View.GONE);
        mNavButtons.setAlpha(0f);
        mNavButtons.setTranslationX(-aw);
    }

    private void showNavButtons() {
        mNavButtons.setVisibility(View.VISIBLE);
        mNavButtons.setAlpha(1f);
        mNavButtons.setTranslationX(0);
    }

    private void showHideStar(Tab tab) {
        // hide the bookmark star for data URLs
        if (tab != null && tab.inForeground()) {
            int starVisibility = View.VISIBLE;
            String url = tab.getUrl();
            if (DataUri.isDataUri(url)) {
                starVisibility = View.GONE;
            }
            mStar.setVisibility(starVisibility);
        }
    }
}
