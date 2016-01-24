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
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

import com.android.browser.R;
import com.android.browser.UI.ComboViews;

import org.codeaurora.swe.util.Activator;
import org.codeaurora.swe.util.Observable;

import java.text.DateFormat;
import java.util.Date;

public class SnapshotBar extends LinearLayout implements OnClickListener {

    private static final int MSG_SHOW_TITLE = 1;
    private static final long DURATION_SHOW_DATE = 1500;

    private ImageView mFavicon;
    private ImageView mSnapshoticon;
    private ImageView mReadericon;
    private TextView mDate;
    private TextView mTitle;
    private TitleBar mTitleBar;
    private View mTabSwitcher;
    private TextView mTabText;
    private View mOverflowMenu;
    private View mToggleContainer;
    private boolean mIsAnimating;
    private ViewPropertyAnimator mTitleAnimator, mDateAnimator;
    private float mAnimRadius = 20f;
    private float mTabSwitcherInitialTextSize = 0;
    private float mTabSwitcherCompressedTextSize = 0;

    public SnapshotBar(Context context) {
        super(context);
    }

    public SnapshotBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SnapshotBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setTitleBar(TitleBar titleBar) {
        mTitleBar = titleBar;
        setFavicon(null);
        Activator.activate(
                new Observable.Observer() {
                    @Override
                    public void onChange(Object... params) {
                        if (mTabText == null)
                            return;
                        if ((Integer) params[0] > 9) {
                            mTabText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTabSwitcherCompressedTextSize);
                        } else {
                            mTabText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTabSwitcherInitialTextSize);
                        }

                        mTabText.setText(Integer.toString((Integer) params[0]));
                    }
                },
                mTitleBar.getUiController().getTabControl().getTabCountObservable());
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_SHOW_TITLE) {
                mIsAnimating = false;
                showTitle();
            }
        }
    };

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mFavicon = (ImageView) findViewById(R.id.favicon);
        mSnapshoticon = (ImageView) findViewById(R.id.snapshot_icon);
        mReadericon = (ImageView) findViewById(R.id.reader_icon);
        mDate = (TextView) findViewById(R.id.date);
        mTitle = (TextView) findViewById(R.id.title);
        mTabSwitcher = findViewById(R.id.tab_switcher);
        mTabText = (TextView) findViewById(R.id.tab_switcher_text);
        mOverflowMenu = findViewById(R.id.more);
        mToggleContainer = findViewById(R.id.toggle_container);

        if (mTabSwitcher != null) {
            mTabSwitcher.setOnClickListener(this);
        }
        if (mOverflowMenu != null) {
            mOverflowMenu.setOnClickListener(this);
            mOverflowMenu.setVisibility(VISIBLE);
        }
        if (mToggleContainer != null) {
            mToggleContainer.setOnClickListener(this);
            resetAnimation();
        }

        if (mTabSwitcherInitialTextSize == 0 && mTabText != null) {
            mTabSwitcherInitialTextSize = mTabText.getTextSize();
            mTabSwitcherCompressedTextSize = (float) (mTabSwitcherInitialTextSize / 1.2);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mToggleContainer != null) {
            mAnimRadius = mToggleContainer.getHeight() / 2f;
        }
    }

    void resetAnimation() {
        if (mToggleContainer == null) {
            // No animation needed/used
            return;
        }
        if (mTitleAnimator != null) {
            mTitleAnimator.cancel();
            mTitleAnimator = null;
        }
        if (mDateAnimator != null) {
            mDateAnimator.cancel();
            mDateAnimator = null;
        }
        mIsAnimating = false;
        mHandler.removeMessages(MSG_SHOW_TITLE);
        mTitle.setAlpha(1f);
        mTitle.setTranslationY(0f);
        mTitle.setRotationX(0f);
        mDate.setAlpha(0f);
        mDate.setTranslationY(-mAnimRadius);
        mDate.setRotationX(90f);
    }

    private void showDate() {
        mTitleAnimator = mTitle.animate()
                .alpha(0f)
                .translationY(mAnimRadius)
                .rotationX(-90f);
        mDateAnimator = mDate.animate()
                .alpha(1f)
                .translationY(0f)
                .rotationX(0f);
    }

    private void showTitle() {
        mTitleAnimator = mTitle.animate()
                .alpha(1f)
                .translationY(0f)
                .rotationX(0f);
        mDateAnimator = mDate.animate()
                .alpha(0f)
                .translationY(-mAnimRadius)
                .rotationX(90f);
    }

    @Override
    public void onClick(View v) {
        if (mTabSwitcher == v) {
            ((PhoneUi) mTitleBar.getUi()).toggleNavScreen();
        } else if (mOverflowMenu == v) {
            NavigationBarBase navBar = mTitleBar.getNavigationBar();
            if (navBar instanceof NavigationBarPhone) {
                ((NavigationBarPhone)navBar).showMenu(mOverflowMenu);
            }
            else if (navBar instanceof  NavigationBarTablet) {
                ((NavigationBarTablet)navBar).showMenu(mOverflowMenu);
            }
        } else if (mToggleContainer == v && !mIsAnimating) {
            mIsAnimating = true;
            showDate();
            mTitleBar.showTopControls(false);
            Message m = mHandler.obtainMessage(MSG_SHOW_TITLE);
            mHandler.sendMessageDelayed(m, DURATION_SHOW_DATE);
        }
    }

    public void onTabDataChanged(Tab tab) {
        if (!tab.isSnapshot()) return;
        SnapshotTab snapshot = (SnapshotTab) tab;
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG);
        mDate.setText(dateFormat.format(new Date(snapshot.getDateCreated())));
        String title = snapshot.getTitle();
        if (TextUtils.isEmpty(title)) {
            title = UrlUtils.stripUrl(snapshot.getUrl());
        }
        mTitle.setText(title);
        setFavicon(tab.getFavicon());
        resetAnimation();
    }

    public void setFavicon(Bitmap icon) {
        if (mFavicon != null)
            mFavicon.setImageDrawable(mTitleBar.getUi().getFaviconDrawable(icon));
    }

    public boolean isAnimating() {
        return mIsAnimating;
    }

    public void setTitle(String title) {
        mTitle.setText(title);
    }

    public void setDate(String date) {
        mDate.setText(date);
    }

    public void setSnapshoticonVisibility(int visibility) {
        if (mSnapshoticon.getVisibility() != visibility) {
            mSnapshoticon.setVisibility(visibility);
        }
    }

    public void setReadericonVisibility(int visibility) {
        if (mReadericon != null && mReadericon.getVisibility() != visibility) {
            mReadericon.setVisibility(visibility);
        }
    }

    public void setFaviconVisibility(int visibility) {
        if (mFavicon != null && mFavicon.getVisibility() != visibility) {
            mFavicon.setVisibility(visibility);
        }
    }

}
