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
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import org.codeaurora.swe.util.Activator;
import org.codeaurora.swe.util.Observable;

import android.widget.TextView;
import com.android.browser.UrlInputView.StateListener;

public class NavigationBarPhone extends NavigationBarBase implements StateListener {

    private View mTabSwitcher;
    private TextView mTabText;
    private View mIncognitoIcon;
    private float mTabSwitcherInitialTextSize = 0;
    private float mTabSwitcherCompressedTextSize = 0;

    public NavigationBarPhone(Context context) {
        super(context);
    }

    public NavigationBarPhone(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NavigationBarPhone(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTabSwitcher = findViewById(R.id.tab_switcher);
        mTabSwitcher.setOnClickListener(this);
        mTabText = (TextView) findViewById(R.id.tab_switcher_text);
        setFocusState(false);
        mUrlInput.setContainer(this);
        mUrlInput.setStateListener(this);
        mIncognitoIcon = findViewById(R.id.incognito_icon);

        mTabSwitcher.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    ((PhoneUi) mBaseUi).toggleNavScreen();
                }
                return true;
            }
        });

        if (mTabSwitcherInitialTextSize == 0) {
            mTabSwitcherInitialTextSize = mTabText.getTextSize();
            mTabSwitcherCompressedTextSize = (float) (mTabSwitcherInitialTextSize / 1.2);
        }
    }

    @Override
    public void setTitleBar(TitleBar titleBar) {
        super.setTitleBar(titleBar);
        Activator.activate(
                new Observable.Observer() {
                    @Override
                    public void onChange(Object... params) {
                        if ((Integer)params[0] > 9) {
                            mTabText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                                    mTabSwitcherCompressedTextSize);
                        } else {
                            mTabText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                                    mTabSwitcherInitialTextSize);
                        }

                        mTabText.setText(Integer.toString((Integer) params[0]));
                    }
                },
                mUiController.getTabControl().getTabCountObservable());
    }

    @Override
    public void onProgressStopped() {
        super.onProgressStopped();
        onStateChanged(mUrlInput.getState());
    }

    /**
     * Update the text displayed in the title bar.
     * @param title String to display.  If null, the new tab string will be
     * @param url
     */
    @Override
    void setDisplayTitle(String title, String url) {
        mUrlInput.setTag(title);
        if (!isEditingUrl()) {
            // add for carrier requirement - show title from native instead of url
            if ((BrowserConfig.getInstance(getContext())
                    .hasFeature(BrowserConfig.Feature.TITLE_IN_URL_BAR) ||
                    mTrustLevel == SiteTileView.TRUST_TRUSTED) && title != null) {
                mUrlInput.setText(title, false);
            } else if (url == null) {
                mUrlInput.setText(R.string.new_tab);
            } else {
                mUrlInput.setText(UrlUtils.stripUrl(url), false);
            }
            mUrlInput.setSelection(0);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mTabSwitcher) {
            ((PhoneUi) mBaseUi).toggleNavScreen();
        } else {
            super.onClick(v);
        }
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (view == mUrlInput && !hasFocus) {
            Tab currentTab = mUiController.getTabControl().getCurrentTab();
            setDisplayTitle(currentTab.getTitle(), currentTab.getUrl());
        }
        super.onFocusChange(view, hasFocus);
    }

    @Override
    public void onStateChanged(int state) {
        switch(state) {
        case StateListener.STATE_NORMAL:
            mStopButton.setVisibility(View.GONE);
            mTabSwitcher.setVisibility(View.VISIBLE);
            mTabText.setVisibility(View.VISIBLE);
            break;
        case StateListener.STATE_HIGHLIGHTED:
            mTabSwitcher.setVisibility(View.GONE);
            mTabText.setVisibility(View.GONE);
            break;
        case StateListener.STATE_EDITED:
            mStopButton.setVisibility(View.GONE);
            mTabSwitcher.setVisibility(View.GONE);
            mTabText.setVisibility(View.GONE);
            break;
        }
        super.onStateChanged(state);
    }

    @Override
    public void onTabDataChanged(Tab tab) {
        super.onTabDataChanged(tab);
        boolean isPrivate = tab.isPrivateBrowsingEnabled();
        mIncognitoIcon.setVisibility(isPrivate ? View.VISIBLE : View.GONE);
        // change the background to a darker tone to reflect the 'incognito' state
        setBackgroundColor(getResources().getColor(isPrivate ?
                R.color.NavigationBarBackgroundIncognito : R.color.NavigationBarBackground));

    }
}
