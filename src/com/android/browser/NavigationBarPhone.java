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
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import org.codeaurora.swe.WebView;
import org.codeaurora.swe.util.Activator;
import org.codeaurora.swe.util.Observable;

import android.widget.ImageView;
import android.widget.TextView;
import com.android.browser.UrlInputView.StateListener;

public class NavigationBarPhone extends NavigationBarBase implements
        StateListener {

    private ImageView mStopButton;
    private ImageView mMagnify;
    private ImageView mClearButton;
    private ImageView mVoiceButton;
    private Drawable mStopDrawable;
    private Drawable mRefreshDrawable;
    private String mStopDescription;
    private String mRefreshDescription;
    private View mTabSwitcher;
    private TextView mTabText;
    private View mComboIcon;
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
        mStopButton = (ImageView) findViewById(R.id.stop);
        mStopButton.setOnClickListener(this);
        mClearButton = (ImageView) findViewById(R.id.clear);
        mClearButton.setOnClickListener(this);
        mVoiceButton = (ImageView) findViewById(R.id.voice);
        mVoiceButton.setOnClickListener(this);
        mMagnify = (ImageView) findViewById(R.id.magnify);
        mTabSwitcher = findViewById(R.id.tab_switcher);
        mTabSwitcher.setOnClickListener(this);
        mTabText = (TextView) findViewById(R.id.tab_switcher_text);
        mComboIcon = findViewById(R.id.iconcombo);
        mComboIcon.setOnClickListener(this);
        setFocusState(false);
        Resources res = getContext().getResources();
        mStopDrawable = res.getDrawable(R.drawable.ic_action_stop);
        mRefreshDrawable = res.getDrawable(R.drawable.ic_action_reload);
        mStopDescription = res.getString(R.string.accessibility_button_stop);
        mRefreshDescription = res.getString(R.string.accessibility_button_refresh);
        mUrlInput.setContainer(this);
        mUrlInput.setStateListener(this);
        mIncognitoIcon = findViewById(R.id.incognito_icon);

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
                            mTabText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTabSwitcherCompressedTextSize);
                        } else {
                            mTabText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTabSwitcherInitialTextSize);
                        }

                        mTabText.setText(Integer.toString((Integer) params[0]));
                    }
                },
                mUiController.getTabControl().getTabCountObservable());
    }

    @Override
    public void onProgressStarted() {
        super.onProgressStarted();
        if (mStopButton.getDrawable() != mStopDrawable) {
            mStopButton.setImageDrawable(mStopDrawable);
            mStopButton.setContentDescription(mStopDescription);
            if (mStopButton.getVisibility() != View.VISIBLE) {
                mComboIcon.setVisibility(View.GONE);
                mStopButton.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onProgressStopped() {
        super.onProgressStopped();
        mStopButton.setImageDrawable(mRefreshDrawable);
        mStopButton.setContentDescription(mRefreshDescription);
        if (!isEditingUrl()) {
            mComboIcon.setVisibility(View.VISIBLE);
        }
        onStateChanged(mUrlInput.getState());
    }

    /**
     * Update the text displayed in the title bar.
     * @param title String to display.  If null, the new tab string will be
     *      shown.
     */
    @Override
    void setDisplayTitle(String title) {
        mUrlInput.setTag(title);
        if (!isEditingUrl()) {
           // add for carrier requirement - show title from native instead of url
            Tab currentTab = mUiController.getTabControl().getCurrentTab();
            if (currentTab != null && currentTab.getTitle() != null) {
                mUrlInput.setText(currentTab.getTitle(), false);
            } else if (title == null) {
                mUrlInput.setText(R.string.new_tab);
            } else {
                mUrlInput.setText(UrlUtils.stripUrl(title), false);
            }
            mUrlInput.setSelection(0);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mStopButton) {
            if (mTitleBar.isInLoad()) {
                mUiController.stopLoading();
            } else {
                WebView web = mBaseUi.getWebView();
                if (web != null) {
                    stopEditingUrl();
                    Tab currentTab = mUiController.getTabControl().getCurrentTab();
                    if (currentTab.hasCrashed) {
                        currentTab.replaceCrashView(web, currentTab.getViewContainer());
                    }
                    web.reload();
                }
            }
        } else if (v == mTabSwitcher) {
            ((PhoneUi) mBaseUi).toggleNavScreen();
        } else if (mClearButton == v) {
            mUrlInput.setText("");
        } else if (mComboIcon == v) {
            mUiController.showPageInfo();
        } else if (mVoiceButton == v) {
            mUiController.startVoiceRecognizer();
        } else {
            super.onClick(v);
        }
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (view == mUrlInput && !hasFocus) {
            setDisplayTitle(mUrlInput.getText().toString());
        }
        super.onFocusChange(view, hasFocus);
    }

    @Override
    public void onStateChanged(int state) {
        super.onStateChanged(state);
        mVoiceButton.setVisibility(View.GONE);
        switch(state) {
        case StateListener.STATE_NORMAL:
            mComboIcon.setVisibility(View.VISIBLE);
            mStopButton.setVisibility(View.GONE);
            mClearButton.setVisibility(View.GONE);
            mMagnify.setVisibility(View.GONE);
            mTabSwitcher.setVisibility(View.VISIBLE);
            mTabText.setVisibility(View.VISIBLE);
            if (mUiController != null) {
                mUiController.setWindowDimming(0f);
            }
            break;
        case StateListener.STATE_HIGHLIGHTED:
            mComboIcon.setVisibility(View.GONE);
            mStopButton.setVisibility(View.VISIBLE);
            mClearButton.setVisibility(View.GONE);
            if ((mUiController != null) && mUiController.supportsVoice()) {
                mVoiceButton.setVisibility(View.VISIBLE);
            }
            mMagnify.setVisibility(View.GONE);
            mTabSwitcher.setVisibility(View.GONE);
            mTabText.setVisibility(View.GONE);

            if (!mUrlInput.getText().toString().equals(mUrlInput.getTag())) {
                // only change text if different
                mUrlInput.setText((String) mUrlInput.getTag(), false);
                mUrlInput.selectAll();
            }

            if (mUiController != null) {
                mUiController.setWindowDimming(0.75f);
            }
            break;
        case StateListener.STATE_EDITED:
            mComboIcon.setVisibility(View.GONE);
            mStopButton.setVisibility(View.GONE);
            mClearButton.setVisibility(View.VISIBLE);
            mMagnify.setVisibility(View.VISIBLE);
            mTabSwitcher.setVisibility(View.GONE);
            mTabText.setVisibility(View.GONE);
            break;
        }
    }

    @Override
    public void onTabDataChanged(Tab tab) {
        super.onTabDataChanged(tab);
        mIncognitoIcon.setVisibility(tab.isPrivateBrowsingEnabled()
                ? View.VISIBLE : View.GONE);
    }
}
