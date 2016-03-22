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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.DecelerateInterpolator;

import org.codeaurora.swe.WebView;

import android.widget.ImageView;

import com.android.browser.UrlInputView.StateListener;

import org.codeaurora.net.NetworkServices;

/**
 * Ui for regular phone screen sizes
 */
public class PhoneUi extends BaseUi {

    private static final String LOGTAG = "PhoneUi";

    private NavScreen mNavScreen;
    private AnimScreen mAnimScreen;
    private final NavigationBarPhone mNavigationBar;
    private boolean mNavScreenRequested = false;

    boolean mShowNav = false;
    private ComboView mComboView;

    private CountDownTimer mCaptureTimer;
    private static final int mCaptureMaxWaitMS = 1000;

    /**
     * @param browser
     * @param controller
     */
    public PhoneUi(Activity browser, UiController controller) {
        super(browser, controller);
        mNavigationBar = (NavigationBarPhone) mTitleBar.getNavigationBar();
    }

    @Override
    public void onDestroy() {
        hideTitleBar();
        // Free the allocated memory for GC to clear it from the heap.
        mAnimScreen = null;
    }

    @Override
    public void editUrl(boolean clearInput, boolean forceIME) {
        //Do nothing while at Nav show screen.
        if (mShowNav) return;
        super.editUrl(clearInput, forceIME);
    }

    @Override
    public void showComboView(ComboViews startingView, Bundle extras) {

        if (mComboView == null) {
            if (mNavScreen != null) {
                mNavScreen.setVisibility(View.GONE);
            }
            ViewStub stub = (ViewStub) mActivity.getWindow().
                    getDecorView().findViewById(R.id.combo_view_stub);
            mComboView = (ComboView) stub.inflate();
            mComboView.setVisibility(View.GONE);
            mComboView.setupViews(mActivity);
        }

        Bundle b = new Bundle();
        b.putString(ComboViewActivity.EXTRA_INITIAL_VIEW, startingView.name());
        b.putBundle(ComboViewActivity.EXTRA_COMBO_ARGS, extras);
        Tab t = getActiveTab();
        if (t != null) {
            b.putString(ComboViewActivity.EXTRA_CURRENT_URL, t.getUrl());
        }

        mComboView.showViews(mActivity, b);
    }

    @Override
    public void hideComboView() {
        if (mComboView != null)
            mComboView.hideViews();
    }

    @Override
    public boolean onBackKey() {
        if (showingNavScreen()) {
            mNavScreen.close(mUiController.getTabControl().getCurrentPosition());
            return true;
        }
        if (isComboViewShowing()) {
            hideComboView();
            return true;
        }
        return super.onBackKey();
    }

    private boolean showingNavScreen() {
        return mNavScreen != null && mNavScreen.getVisibility() == View.VISIBLE;
    }

    @Override
    public boolean dispatchKey(int code, KeyEvent event) {
        return false;
    }

    @Override
    public void setActiveTab(final Tab tab) {
        super.setActiveTab(tab);

        //if at Nav screen show, detach tab like what showNavScreen() do.
        if (mShowNav) {
            detachTab(mActiveTab);
        }

        BrowserWebView view = (BrowserWebView) tab.getWebView();
        // TabControl.setCurrentTab has been called before this,
        // so the tab is guaranteed to have a webview
        if (view == null) {
            Log.e(LOGTAG, "active tab with no webview detected");
            return;
        }
        // Request focus on the top window.
        view.setTitleBar(mTitleBar);

        // update nav bar state
        mNavigationBar.onStateChanged(StateListener.STATE_NORMAL);
    }

    // menu handling callbacks

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        updateMenuState(mActiveTab, menu);
        return true;
    }

    @Override
    public void updateMenuState(Tab tab, Menu menu) {
        MenuItem bm = menu.findItem(R.id.bookmarks_menu_id);
        if (bm != null) {
            bm.setVisible(!showingNavScreen());
        }
        MenuItem info = menu.findItem(R.id.page_info_menu_id);
        if (info != null) {
            info.setVisible(false);
        }

        if (showingNavScreen()) {
            setMenuItemVisibility(menu, R.id.find_menu_id, false);
            menu.setGroupVisible(R.id.LIVE_MENU, false);
            setMenuItemVisibility(menu, R.id.save_snapshot_menu_id, false);
            menu.setGroupVisible(R.id.SNAPSHOT_MENU, false);
            menu.setGroupVisible(R.id.NAV_MENU, false);
        }

        if (isComboViewShowing()) {
            menu.setGroupVisible(R.id.MAIN_MENU, false);
            menu.setGroupEnabled(R.id.MAIN_MENU, false);
            menu.setGroupEnabled(R.id.MAIN_SHORTCUT_MENU, false);
        }

        MenuItem closeOthers = menu.findItem(R.id.close_other_tabs_id);
        if (closeOthers != null) {
            boolean isLastTab = true;
            if (tab != null) {
                isLastTab = (mTabControl.getTabCount() <= 1);
            }
            closeOthers.setVisible(!isLastTab);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (showingNavScreen()
                && (item.getItemId() != R.id.snapshots_menu_id)) {
            hideNavScreen(mUiController.getTabControl().getCurrentPosition(), false);
        }
        return false;
    }

    @Override
    public void onContextMenuCreated(Menu menu) {
        hideTitleBar();
    }

    @Override
    public void onContextMenuClosed(Menu menu, boolean inLoad) {
        if (inLoad) {
            showTitleBar();
        }
    }

    // action mode callbacks

    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);
        if (!isEditingUrl()) {
            mTitleBar.setVisibility(View.GONE);
        }

        ActionBar actionBar = mActivity.getActionBar();
        if (actionBar != null) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            actionBar.hide();
        }
    }

    @Override
    public void onActionModeFinished(boolean inLoad) {
        super.onActionModeFinished(inLoad);
        mTitleBar.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean isWebShowing() {
        return super.isWebShowing() && !showingNavScreen() && !isComboViewShowing();
    }

    @Override
    public boolean isComboViewShowing() {
        return mComboView != null && mComboView.getVisibility() == View.VISIBLE;
    }

    @Override
    public void showWeb(boolean animate) {
        super.showWeb(animate);
        hideNavScreen(mUiController.getTabControl().getCurrentPosition(), animate);
    }

    //Unblock touch events
    private void unblockEvents() {
        mUiController.setBlockEvents(false);
    }
    //Block touch events
    private void blockEvents() {
        mUiController.setBlockEvents(true);
    }

    @Override
    public void cancelNavScreenRequest() {
        mNavScreenRequested = false;
    }

    private void thumbnailUpdated(Tab t) {
        mTabControl.setOnThumbnailUpdatedListener(null);

        // Discard the callback if the req is interrupted
        if (!mNavScreenRequested) {
            unblockEvents();
            return;
        }

        Bitmap bm = t.getScreenshot();
        if (bm == null) {
            t.initCaptureBitmap();
            bm = t.getScreenshot();
        }

        Bitmap sbm;
        WebView webView = getWebView();
        if (webView != null && webView.getWidth() != 0) {
            int view_width = webView.getWidth();
            int capture_width = mActivity.getResources().getDimensionPixelSize(
                    R.dimen.tab_thumbnail_width);

            float scale =  (float) view_width / capture_width;

            //Upscale the low-res bitmap to the needed size
            Matrix m = new Matrix();
            m.postScale(scale, scale);
            sbm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(),
                    bm.getHeight(), m, false);
        } else {
            sbm = bm;
        }

        onShowNavScreenContinue(sbm);
    }

    private void startCaptureTimer(final Tab tab) {
        mCaptureTimer = new CountDownTimer(mCaptureMaxWaitMS, mCaptureMaxWaitMS) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Do nothing
            }

            @Override
            public void onFinish() {
                Log.e(LOGTAG, "Screen capture timed out while showing navigation screen");
                thumbnailUpdated(tab);
            }
        }.start();
    }

    private void stopCaptureTimer() {
        if (mCaptureTimer != null) {
            mCaptureTimer.cancel();
            mCaptureTimer = null;
        }
    }

    void showNavScreen() {
        blockEvents();
        stopCaptureTimer();

        mNavScreenRequested = true;
        mTabControl.setOnThumbnailUpdatedListener(
                new TabControl.OnThumbnailUpdatedListener() {
                    @Override
                    public void onThumbnailUpdated(Tab t) {
                        stopCaptureTimer();
                        thumbnailUpdated(t);
                    }
                });
        if (!BrowserSettings.getInstance().isPowerSaveModeEnabled()) {
            //Notify about anticipated network activity
            NetworkServices.hintUpcomingUserActivity();
        }
        mActiveTab.capture();
        startCaptureTimer(mActiveTab);
    }

    void onShowNavScreenContinue(Bitmap viewportBitmap) {
        dismissIME();
        mShowNav = true;
        mNavScreenRequested = false;
        if (mNavScreen == null) {
            mNavScreen = new NavScreen(mActivity, mUiController, this);
            mCustomViewContainer.addView(mNavScreen, COVER_SCREEN_PARAMS);
        } else {
            mNavScreen.setVisibility(View.VISIBLE);
            mNavScreen.setAlpha(1f);
            mNavScreen.refreshAdapter();
        }
        if (mAnimScreen == null) {
            mAnimScreen = new AnimScreen(mActivity);
        } else {
            mAnimScreen.mMain.setAlpha(1f);
            mAnimScreen.setScaleFactor(1f);
        }
        mAnimScreen.set(getTitleBar(), viewportBitmap);
        if (mAnimScreen.mMain.getParent() == null) {
            mCustomViewContainer.addView(mAnimScreen.mMain, COVER_SCREEN_PARAMS);
        }
        mCustomViewContainer.setVisibility(View.VISIBLE);
        mCustomViewContainer.bringToFront();
        mAnimScreen.mMain.layout(0, 0, mContentView.getWidth(),
                mContentView.getHeight());
        int fromLeft = 0;
        int fromTop = getTitleBar().getHeight();
        int fromRight = mContentView.getWidth();
        int fromBottom = mContentView.getHeight();
        int width = mActivity.getResources().getDimensionPixelSize(R.dimen.nav_tab_width);
        int height = mActivity.getResources().getDimensionPixelSize(R.dimen.nav_tab_height);
        int ntth = mActivity.getResources().getDimensionPixelSize(R.dimen.nav_tab_titleheight);
        int toLeft = (mContentView.getWidth() - width) / 2;
        int toTop = ((fromBottom - (ntth + height)) / 2 + ntth);
        int toRight = toLeft + width;
        int toBottom = toTop + height;
        float toScaleFactor = width / (float) mContentView.getWidth();
        ObjectAnimator tx = ObjectAnimator.ofInt(mAnimScreen.mContent, "left", fromLeft, toLeft);
        ObjectAnimator ty = ObjectAnimator.ofInt(mAnimScreen.mContent, "top", fromTop, toTop);
        ObjectAnimator tr = ObjectAnimator.ofInt(mAnimScreen.mContent, "right", fromRight, toRight);
        ObjectAnimator tb = ObjectAnimator.ofInt(mAnimScreen.mContent, "bottom",
                fromBottom, toBottom);
        ObjectAnimator sx = ObjectAnimator.ofFloat(mAnimScreen, "scaleFactor", 1f, toScaleFactor);
        ObjectAnimator navTabsIn = mNavScreen.createToolbarInAnimator();
        mAnimScreen.mContent.layout(fromLeft, fromTop, fromRight, fromBottom);
        mAnimScreen.setScaleFactor(1f);

        AnimatorSet inanim = new AnimatorSet();
        inanim.playTogether(tx, ty, tr, tb, sx, navTabsIn);
        inanim.setInterpolator(new DecelerateInterpolator());
        inanim.setDuration(200);

        ObjectAnimator disappear = ObjectAnimator.ofFloat(mAnimScreen.mMain, "alpha", 1f, 0f);
        disappear.setInterpolator(new DecelerateInterpolator());
        disappear.setDuration(100);

        AnimatorSet set1 = new AnimatorSet();
        set1.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mContentView.setVisibility(View.GONE);
            }
            @Override
            public void onAnimationEnd(Animator anim) {
                mCustomViewContainer.removeView(mAnimScreen.mMain);
                finishAnimationIn();
                unblockEvents();
            }
        });
        set1.playSequentially(inanim, disappear);
        set1.start();
        unblockEvents();
    }

    private void finishAnimationIn() {
        if (showingNavScreen()) {
            // notify accessibility manager about the screen change
            mNavScreen.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        }
    }

    void hideNavScreen(int position, boolean animate) {

        mShowNav = false;
        if (!showingNavScreen()) return;
        final Tab tab = mUiController.getTabControl().getTab(position);
        if ((tab == null) || !animate) {
            if (tab != null) {
                setActiveTab(tab);
            } else if (mTabControl.getTabCount() > 0) {
                // use a fallback tab
                setActiveTab(mTabControl.getCurrentTab());
            }
            mContentView.setVisibility(View.VISIBLE);
            finishAnimateOut();
            return;
        }
        NavTabView tabview = (NavTabView) mNavScreen.getTabView(position);
        if (tabview == null) {
            if (mTabControl.getTabCount() > 0) {
                // use a fallback tab
                setActiveTab(mTabControl.getCurrentTab());
            }
            mContentView.setVisibility(View.VISIBLE);
            finishAnimateOut();
            return;
        }
        blockEvents();
        mUiController.setActiveTab(tab);
        mContentView.setVisibility(View.VISIBLE);
        if (mAnimScreen == null) {
            mAnimScreen = new AnimScreen(mActivity);
        }
        ImageView target = tabview.mImage;
        int width = target.getDrawable().getIntrinsicWidth();
        int height = target.getDrawable().getIntrinsicHeight();
        Bitmap bm = tab.getScreenshot();
        if (bm == null)
            bm = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        mAnimScreen.set(bm);
        if (mAnimScreen.mMain.getParent() == null) {
            mCustomViewContainer.addView(mAnimScreen.mMain, COVER_SCREEN_PARAMS);
        }
        mAnimScreen.mMain.layout(0, 0, mContentView.getWidth(),
                mContentView.getHeight());
        mNavScreen.getScroller().finishScroller();
        int toLeft = 0;
        int toTop = mTitleBar.calculateEmbeddedHeight();
        int toRight = mContentView.getWidth();
        int fromLeft = tabview.getLeft() + target.getLeft() - mNavScreen.getScroller().getScrollX();
        int fromTop = tabview.getTop() + target.getTop() - mNavScreen.getScroller().getScrollY();
        int fromRight = fromLeft + width;
        int fromBottom = fromTop + height;
        float scaleFactor = mContentView.getWidth() / (float) width;
        int toBottom = toTop + (int) (height * scaleFactor);
        mAnimScreen.mContent.setLeft(fromLeft);
        mAnimScreen.mContent.setTop(fromTop);
        mAnimScreen.mContent.setRight(fromRight);
        mAnimScreen.mContent.setBottom(fromBottom);
        mAnimScreen.setScaleFactor(1f);
        //ObjectAnimator fade2 = ObjectAnimator.ofFloat(mNavScreen, "alpha", 1f, 0f);
        //fade2.setDuration(100);
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator animAppear = ObjectAnimator.ofFloat(mAnimScreen.mMain, "alpha", 0f, 1f);
        animAppear.setDuration(100);
        ObjectAnimator l = ObjectAnimator.ofInt(mAnimScreen.mContent, "left", fromLeft, toLeft);
        ObjectAnimator t = ObjectAnimator.ofInt(mAnimScreen.mContent, "top", fromTop, toTop);
        ObjectAnimator r = ObjectAnimator.ofInt(mAnimScreen.mContent, "right", fromRight, toRight);
        ObjectAnimator b = ObjectAnimator.ofInt(mAnimScreen.mContent, "bottom",
                fromBottom, toBottom);
        ObjectAnimator scale = ObjectAnimator.ofFloat(mAnimScreen, "scaleFactor", 1f, scaleFactor);
        set.playTogether(animAppear, l, t, r, b, scale);
        set.setInterpolator(new DecelerateInterpolator());
        set.setDuration(200);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator anim) {
                checkTabReady();
            }
        });
        set.start();
    }


    private int mNumTries = 0;
    private void checkTabReady() {
        boolean isready = true;
        boolean zeroTries = mNumTries == 0;
        Tab tab = mUiController.getTabControl().getCurrentTab();
        BrowserWebView webview = null;
        if (tab == null)
            isready = false;
        else {
            webview = (BrowserWebView)tab.getWebView();
            if (webview == null) {
                isready = false;
            } else {
                isready = webview.isReady();
            }
        }
        // Post only when not ready and not crashed
        if (!isready && mNumTries++ < 150) {
            mCustomViewContainer.postDelayed(new Runnable() {
                public void run() {
                    checkTabReady();
                }
            }, 17); //WebView is not ready.  check again in for next frame.
            return;
        }
        mNumTries = 0;
        final boolean hasCrashed = (webview == null) ? false :false;
        // fast path: don't wait if we've been ready for a while
        if (zeroTries) {
            fadeOutCustomViewContainer();
            return;
        }
        mCustomViewContainer.postDelayed(new Runnable() {
            public void run() {
                fadeOutCustomViewContainer();
            }
        }, 32); //WebView is ready, but give it extra 2 frame's time to display and finish the swaps
    }

    private void fadeOutCustomViewContainer() {
        ObjectAnimator otheralpha = ObjectAnimator.ofFloat(mCustomViewContainer, "alpha", 1f, 0f);
        otheralpha.setDuration(100);
        otheralpha.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator anim) {
                mCustomViewContainer.removeView(mAnimScreen.mMain);
                finishAnimateOut();
                unblockEvents();
            }
        });
        otheralpha.setInterpolator(new DecelerateInterpolator());
        otheralpha.start();
    }

    private void finishAnimateOut() {
        if (mNavScreen != null) {
            mNavScreen.setVisibility(View.GONE);
        }
        mCustomViewContainer.setAlpha(1f);
        mCustomViewContainer.setVisibility(View.GONE);
        mAnimScreen.set(null);
    }

    @Override
    public boolean needsRestoreAllTabs() {
        return false;
    }

    public void toggleNavScreen() {
        if (!showingNavScreen()) {
            showNavScreen();
        } else {
            hideNavScreen(mUiController.getTabControl().getCurrentPosition(), false);
        }
    }

    static class AnimScreen {

        private View mMain;
        private ImageView mContent;
        private float mScale;

        public AnimScreen(Context ctx) {
            mMain = LayoutInflater.from(ctx).inflate(R.layout.anim_screen, null);
            mMain.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // just eat clicks when this view is visible
                }
            });
            mContent = (ImageView) mMain.findViewById(R.id.anim_screen_content);
            mContent.setScaleType(ImageView.ScaleType.MATRIX);
            mContent.setImageMatrix(new Matrix());
            mScale = 1.0f;
            setScaleFactor(getScaleFactor());
        }

        public void set(TitleBar tbar, Bitmap viewportBitmap) {
            if (tbar == null) {
                return;
            }
            mContent.setImageBitmap(viewportBitmap);
        }

        public void set(Bitmap image) {
            mContent.setImageBitmap(image);
        }

        private void setScaleFactor(float sf) {
            mScale = sf;
            Matrix m = new Matrix();
            m.postScale(sf, sf);
            mContent.setImageMatrix(m);
        }

        private float getScaleFactor() {
            return mScale;
        }

    }

}
