/*
 * Copyright (c) 2015, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.android.browser;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.CountDownTimer;
import android.support.v4.widget.ViewDragHelper;
import android.view.View;

import org.codeaurora.swe.WebHistoryItem;
import org.codeaurora.swe.WebView;
import org.codeaurora.swe.util.Activator;
import org.codeaurora.swe.util.Observable;

public class EdgeSwipeController extends ViewDragHelper.Callback {
    private ViewDragHelper mDragHelper;
    private int mState = ViewDragHelper.STATE_IDLE;
    private int mFromEdge = ViewDragHelper.EDGE_LEFT;
    private boolean mbNavigated = false;
    private int mOldX = 0;
    private int mOldDx = 0;
    private Observable mPageLoadTarget;
    private Observable mPageLoadObservable;

    private boolean mbCurrBMSynced = false;

    private Tab mActiveTab;
    private TitleBar mTitleBar;

    private static final float mMinAlpha = 0.5f;
    private static final int mMinProgress = 85;
    private static final int mProgressWaitMS = 1000;
    private static final int EDGE_SWIPE_INVALID_INDEX = -2;

    private CountDownTimer mLoadTimer, mCommitTimer;

    private int mCurrIndex = EDGE_SWIPE_INVALID_INDEX;
    private int mPrevIndex;
    private int mNextIndex;
    private int mMaxIndex;

    private EdgeSwipeModel mModel;
    private EdgeSwipeView mView;

    public EdgeSwipeController(View container,
                               int stationaryViewId,
                               int slidingViewId,
                               int slidingViewShadowId,
                               int opacityViewId,
                               int liveViewId,
                               int viewGroupId,
                               BaseUi ui) {
        DraggableFrameLayout viewGroup = (DraggableFrameLayout)
                container.findViewById(viewGroupId);

        mActiveTab = ui.getActiveTab();
        mTitleBar = ui.getTitleBar();

        mModel = new EdgeSwipeModel(mActiveTab, mTitleBar);
        mView = new EdgeSwipeView(
                container,
                stationaryViewId,
                slidingViewId,
                slidingViewShadowId,
                opacityViewId,
                liveViewId,
                viewGroupId,
                mTitleBar);

        mPageLoadTarget = mActiveTab.getTabHistoryUpdateObservable();
        mPageLoadObservable = Activator.activate(
                new Observable.Observer() {
                    @Override
                    public void onChange(Object... params) {
                        if (mDragHelper == null ||
                                mPageLoadTarget == null) {
                            return;
                        }

                        synchronized (this) {
                            int index = (int) params[0];
                            if (mState == ViewDragHelper.STATE_IDLE && index == mCurrIndex) {
                                monitorProgressAtHistoryUpdate(index);
                            }
                        }
                    }
                },
                mPageLoadTarget
        );

        mDragHelper = ViewDragHelper.create(viewGroup, 0.5f, this);
        mDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT | ViewDragHelper.EDGE_RIGHT);
        viewGroup.setDragHelper(mDragHelper);
    }

    private void swipeSessionCleanup() {
        mView.goLive();
        mModel.cleanup();
        mCurrIndex = EDGE_SWIPE_INVALID_INDEX;
        mState = ViewDragHelper.STATE_IDLE;
    }

    private boolean setState(int curState, int newState) {
        if (mState == curState) {
            mState = newState;
            return true;
        }
        return false;
    }

    public void cleanup() {
        if (mPageLoadObservable != null) {
            mPageLoadObservable.onOff(false);
            synchronized (this) {
                mDragHelper.cancel();
                swipeSessionCleanup();
            }
        }
    }

    public void onConfigurationChanged() {
        synchronized (this) {
            swipeSessionCleanup();
        }
    }

    private void showCurrBMInStationaryView() {
        if (!mbCurrBMSynced) {
            Bitmap currBM = mModel.readSnapshot(mCurrIndex);
            if (currBM != null) {
                mView.setStationaryViewBitmap(currBM, mModel.getColor(mCurrIndex));
                mbCurrBMSynced = true;
            }
        }
    }

    private void showCurrBMInSlidingView() {
        if (!mbCurrBMSynced) {
            Bitmap currBM = mModel.readSnapshot(mCurrIndex);
            mView.setSlidingViewBitmap(currBM, mModel.getColor(mCurrIndex));
            if (currBM != null) {
                mbCurrBMSynced = true;
            }
        }
    }

    private Bitmap getGrayscale(Bitmap bitmap)
    {
        if (bitmap == null)
            return null;

        int height = bitmap.getHeight();
        int width = bitmap.getWidth();

        Bitmap gray = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(gray);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();

        cm.setSaturation(0);

        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);

        paint.setColorFilter(f);

        c.drawBitmap(bitmap, 0, 0, paint);
        return gray;
    }

    private void monitorProgressAtLoad(final int pageIndex) {
        if (mLoadTimer != null) {
            mLoadTimer.cancel();
        }

        mLoadTimer = new CountDownTimer(mProgressWaitMS * 5, mProgressWaitMS) {
            boolean mGrayBM = false;

            public void onTick(long msRemain) {
                if (msRemain > mProgressWaitMS * 4) {
                    return;
                }
                synchronized (this) {
                    if (mTitleBar.getProgressView().getProgressPercent() >= mMinProgress) {
                        if (mState == ViewDragHelper.STATE_IDLE && pageIndex == mCurrIndex) {
                            swipeSessionCleanup();

                        }
                        cancel();
                    } else if(mState == ViewDragHelper.STATE_DRAGGING) {
                        if (mGrayBM) {
                            return;
                        }
                        switch (mFromEdge) {
                            case ViewDragHelper.EDGE_LEFT:
                                mView.setSlidingViewBitmap(
                                        getGrayscale(getSnapshotOrFavicon(pageIndex)),
                                        mModel.getColor(pageIndex));
                                mGrayBM = true;
                                break;
                            case ViewDragHelper.EDGE_RIGHT:
                                mView.setStationaryViewBitmap(
                                        getGrayscale(getSnapshotOrFavicon(pageIndex)),
                                        mModel.getColor(pageIndex));
                                mGrayBM = true;
                                break;
                        }
                    } else {
                        if (mGrayBM) {
                            return;
                        }
                        mView.setStationaryViewBitmap(
                                getGrayscale(getSnapshotOrFavicon(pageIndex)),
                                mModel.getColor(pageIndex));
                        mGrayBM = true;
                    }
                }
            }

            public void onFinish() {
                mGrayBM = false;
                synchronized (this) {
                    if (mTitleBar.getProgressView().getProgressPercent() >= mMinProgress) {
                        if (mState == ViewDragHelper.STATE_IDLE && pageIndex == mCurrIndex) {
                            swipeSessionCleanup();
                        }
                        cancel();
                    }
                }
            }
        }.start();
    }

    private int lastCommittedHistoryIndex() {
        WebView wv = mActiveTab.getWebView();
        if (wv == null || wv.getLastCommittedHistoryIndex() == -1)
            return 0; // WebView is null or No History has been committed for this tab
        else
            return wv.getLastCommittedHistoryIndex();
    }

    private void monitorProgressAtHistoryUpdate(final int pageIndex) {
        if (mCommitTimer != null) {
            mCommitTimer.cancel();
        }

        if (mTitleBar.getProgressView().getProgressPercent() >= mMinProgress
                && lastCommittedHistoryIndex() == pageIndex) {
            swipeSessionCleanup();
            return;
        }

        mCommitTimer = new CountDownTimer(mProgressWaitMS * 5, mProgressWaitMS) {
            public void onTick(long msRemain) {
                synchronized (this) {
                    if (mTitleBar.getProgressView().getProgressPercent() >= mMinProgress) {
                        if (mState == ViewDragHelper.STATE_IDLE && pageIndex == mCurrIndex) {
                            swipeSessionCleanup();

                        }
                        cancel();
                    }
                }
            }

            public void onFinish() {
                synchronized (this) {
                    if (mState == ViewDragHelper.STATE_IDLE && pageIndex == mCurrIndex) {
                        swipeSessionCleanup();
                    }
                }
            }
        }.start();
    }

    private boolean isPortrait(Bitmap bitmap) {
        return (bitmap.getHeight() < bitmap.getWidth());
    }

    private Bitmap getSnapshotOrFavicon(int index) {
        Bitmap bm = mModel.readSnapshot(index);
        if (bm == null || mView.isPortrait() != isPortrait(bm))  {
            WebHistoryItem item = mActiveTab.getWebView()
                    .copyBackForwardList().getItemAtIndex(index);
            if (item != null) {
                bm = item.getFavicon();
            }
        }
        return bm;
    }

    public void onViewDragStateChanged(int state) {
        synchronized (this) {
            if (mState != ViewDragHelper.STATE_SETTLING || state != ViewDragHelper.STATE_IDLE) {
                return;
            }

            mView.hideSlidingViews();

            if (mbNavigated) {
                mView.setStationaryViewBitmap(getSnapshotOrFavicon(mCurrIndex),
                        mModel.getColor(mCurrIndex));
            } else {
                swipeSessionCleanup();
            }

            mView.setStationaryViewAlpha(1.0f);
            mView.invalidate();

            setState(ViewDragHelper.STATE_SETTLING, ViewDragHelper.STATE_IDLE);
        }
    }

    public void onViewReleased(View releasedChild, float xvel, float yvel) {
        synchronized (this) {
            if (!setState(ViewDragHelper.STATE_DRAGGING, ViewDragHelper.STATE_SETTLING)) {
                mOldX = 0;
                mOldDx = 0;
                return;
            }

            mbNavigated = true;

            boolean bCrossedEventHorizon = Math.abs(mOldX) > mView.getWidth() / 2;

            if (mCurrIndex >= 0) {
                if ((xvel > 0 || (xvel == 0 && mOldX > 0 && bCrossedEventHorizon))
                        && mFromEdge == ViewDragHelper.EDGE_LEFT
                        && mActiveTab.getWebView().canGoToHistoryIndex(mCurrIndex - 1)) {
                    mCurrIndex -= 1;
                    mActiveTab.getWebView().stopLoading();
                    mActiveTab.getWebView().goToHistoryIndex(mCurrIndex);
                    monitorProgressAtLoad(mCurrIndex);
                    mDragHelper.settleCapturedViewAt(
                            releasedChild.getMeasuredWidth(),
                            releasedChild.getTop());
                } else if ((xvel < 0 || (xvel == 0 && mOldX < 0 && bCrossedEventHorizon))
                        && mFromEdge == ViewDragHelper.EDGE_RIGHT
                        && mActiveTab.getWebView().canGoToHistoryIndex(mCurrIndex + 1)) {
                    mCurrIndex += 1;
                    mActiveTab.getWebView().stopLoading();
                    mActiveTab.getWebView().goToHistoryIndex(mCurrIndex);
                    monitorProgressAtLoad(mCurrIndex);
                    mDragHelper.settleCapturedViewAt(
                            -releasedChild.getMeasuredWidth(),
                            releasedChild.getTop());
                    mView.goDormant();
                } else {
                    mbNavigated = false;
                    mDragHelper.settleCapturedViewAt(0, releasedChild.getTop());
                }
            }
            mOldX = 0;
            mOldDx = 0;

            mView.invalidate();
        }
    }

    public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
        float alpha = ((float) Math.abs(left)) / mView.getMeasuredWidth();

        synchronized (this) {
            switch (mFromEdge) {
                case ViewDragHelper.EDGE_LEFT:
                    if (mView.isLive()) {
                        return;
                    }
                    mView.setStationaryViewAlpha(mMinAlpha + alpha * (1 - mMinAlpha));

                    if (mState != ViewDragHelper.STATE_IDLE) {
                        mView.moveShadowView(left);
                    }

                    showCurrBMInSlidingView();

                    if (mPrevIndex >= 0) {
                        if (!mView.stationaryViewHasImage()) {
                            mView.setStationaryViewBitmap(getSnapshotOrFavicon(mPrevIndex),
                                    mModel.getColor(mPrevIndex));
                        }
                    }
                    break;
                case ViewDragHelper.EDGE_RIGHT:
                    mView.setStationaryViewAlpha(mMinAlpha + (1 - alpha) * (1 - mMinAlpha));
                    if (mState != ViewDragHelper.STATE_IDLE) {
                        mView.moveShadowView(mView.getMeasuredWidth() + left);

                        if (!mView.slidingViewHasImage() && mNextIndex < mMaxIndex) {
                            mView.setSlidingViewBitmap(getSnapshotOrFavicon(mNextIndex),
                                    mModel.getColor(mNextIndex));
                        }

                        showCurrBMInStationaryView();
                        if (mbCurrBMSynced) {
                            mView.goDormant();
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void onEdgeDragStarted(int edgeFlags, int pointerId) {
        synchronized (this) {
            if (mActiveTab.isPrivateBrowsingEnabled()) {
                mDragHelper.abort();
                return;
            }

            if (mDragHelper.getViewDragState() != ViewDragHelper.STATE_IDLE ||
                    !setState(ViewDragHelper.STATE_IDLE, ViewDragHelper.STATE_DRAGGING)) {
                mDragHelper.abort();
                return;
            }

            if ((edgeFlags & mFromEdge) != mFromEdge || mCurrIndex == EDGE_SWIPE_INVALID_INDEX) {
                onEdgeTouched(edgeFlags, pointerId);
            }

            mbCurrBMSynced = false;

            switch (mFromEdge) {
                case ViewDragHelper.EDGE_LEFT:
                    mView.showSlidingViews();
                    mView.goDormant();
                    mPrevIndex = mCurrIndex - 1;
                    mView.setStationaryViewBitmap(getSnapshotOrFavicon(mPrevIndex),
                            mModel.getColor(mPrevIndex));
                    showCurrBMInSlidingView();
                    break;
                case ViewDragHelper.EDGE_RIGHT:
                    mView.showSlidingViews();
                    mNextIndex = mCurrIndex + 1;
                    mView.setSlidingViewBitmap(getSnapshotOrFavicon(mNextIndex),
                            mModel.getColor(mNextIndex));
                    showCurrBMInStationaryView();
                    if (mbCurrBMSynced)
                        mView.goDormant();
                    break;
                default:
                    break;
            }
        }
    }

    public int getOrderedChildIndex(int index) {
        return mView.slidingViewIndex();
    }

    public void onEdgeTouched (int edgeFlags, int pointerId) {
        synchronized (this) {
            if (mActiveTab.getWebView() == null ||
                mActiveTab.isPrivateBrowsingEnabled() ||
                mActiveTab.isKeyboardShowing()) {
                mDragHelper.abort();
                return;
            }

            if (mState != ViewDragHelper.STATE_IDLE && mCurrIndex != EDGE_SWIPE_INVALID_INDEX) {
                mDragHelper.abort();
                return;
            }

            mView.init();

            if (mCurrIndex == EDGE_SWIPE_INVALID_INDEX) {
                mCurrIndex = lastCommittedHistoryIndex();
            }

            mMaxIndex = mActiveTab.getWebView().copyBackForwardList().getSize() - 1;
            mModel.updateSnapshot(mCurrIndex);

            if (ViewDragHelper.EDGE_LEFT == (edgeFlags & ViewDragHelper.EDGE_LEFT)) {
                mFromEdge = ViewDragHelper.EDGE_LEFT;
                mView.slidingViewTouched(mFromEdge);
                if (mCurrIndex > 0) {
                    mModel.fetchSnapshot(mCurrIndex - 1);
                }
            } else if (ViewDragHelper.EDGE_RIGHT == (edgeFlags & ViewDragHelper.EDGE_RIGHT)) {
                mFromEdge = ViewDragHelper.EDGE_RIGHT;
                mView.slidingViewTouched(mFromEdge);
                if (mCurrIndex < mMaxIndex) {
                    mModel.fetchSnapshot(mCurrIndex + 1);
                }
            }
        }
    }

    public int getViewHorizontalDragRange(View child) {
        return child.getMeasuredWidth();
    }

    public boolean tryCaptureView(View child, int pointerId) {
        return (mState == ViewDragHelper.STATE_DRAGGING && mView.allowCapture(child));
    }

    public int clampViewPositionHorizontal(View child, int left, int dx) {
        if (mOldX != 0 && Math.signum(dx) != Math.signum(mOldDx)) {
            mOldDx = dx;
            return mOldX;
        }

        switch (mFromEdge) {
            case ViewDragHelper.EDGE_LEFT:
                if (left < 0) {
                    mOldDx = dx;
                    return mOldX;
                }
                if (!mActiveTab.getWebView().canGoToHistoryIndex(mPrevIndex)) {
                    if (Math.abs(left) >= child.getMeasuredWidth() / 3) {
                        return child.getMeasuredWidth() / 3;
                    }
                }
                break;
            case ViewDragHelper.EDGE_RIGHT:
                if (left > 0) {
                    mOldDx = dx;
                    return mOldX;
                }
                if (!mActiveTab.getWebView().canGoToHistoryIndex(mNextIndex)) {
                    if (Math.abs(left) >= child.getMeasuredWidth() / 3) {
                        return -child.getMeasuredWidth() / 3;
                    }
                }
                break;
            default:
                break;
        }

        mOldX = left;
        mOldDx = dx;
        return left;
    }
}

