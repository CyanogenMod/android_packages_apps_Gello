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
import android.os.AsyncTask;
import android.support.v4.widget.ViewDragHelper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class EdgeSwipeSettings extends ViewDragHelper.Callback {
    private ViewDragHelper mDragHelper;
    private int mFromEdge = ViewDragHelper.EDGE_TOP;
    private int mLeft = 0;

    private ImageView mSlidingViewShadow;
    private LinearLayout mSettingsView;
    private DraggableFrameLayout mViewGroup;
    private View mLiveView;
    private ImageView mStationaryView;

    private int mSlidingViewIndex;
    private int mCurrIndex;

    private EdgeSwipeModel mModel;
    private Tab mActiveTab;
    private TitleBar mTitleBar;

    private boolean mbWaitForSettings = false;

    public EdgeSwipeSettings(final View container,
                             int stationaryViewId,
                             int settingViewId,
                             int slidingViewShadowId,
                             int liveViewId,
                             int viewGroupId,
                             final BaseUi ui) {
        DraggableFrameLayout viewGroup = (DraggableFrameLayout)
                container.findViewById(viewGroupId);

        mSlidingViewShadow = (ImageView) container.findViewById(slidingViewShadowId);
        mSettingsView = (LinearLayout) container.findViewById(settingViewId);
        mStationaryView = (ImageView) container.findViewById(stationaryViewId);
        mLiveView = container.findViewById(liveViewId);
        mViewGroup = (DraggableFrameLayout) container.findViewById(viewGroupId);

        final int childCount = mViewGroup.getChildCount();

        for (int i = childCount - 1; i >= 0; i--) {
            final View child = mViewGroup.getChildAt(i);
            if (mSettingsView == child) {
                mSlidingViewIndex = i;
                break;
            }
        }

        mActiveTab = ui.getActiveTab();
        mTitleBar = ui.getTitleBar();
        mModel = new EdgeSwipeModel(mActiveTab, mTitleBar);

        mDragHelper = ViewDragHelper.create(viewGroup, 0.5f, this);
        mDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT | ViewDragHelper.EDGE_RIGHT);
        mViewGroup.setDragHelper(mDragHelper);

        final Button closeBtn =
                (Button) container.findViewById(R.id.edge_sliding_settings_close_btn);
        closeBtn.setOnClickListener(
            new View.OnClickListener() {
                public void onClick(View v) {
                    goLive();
                }
            }
        );

        final RadioButton temporalNavButton =
                (RadioButton) container.findViewById(R.id.edge_sliding_settings_options_temporal);
        temporalNavButton.setOnClickListener(
            new View.OnClickListener() {
                public void onClick(View v) {
                    BrowserSettings.getInstance().setEdgeSwipeTemporal();
                    goLive();
                    applySettingsAndRefresh(ui, container);
                    Toast toast = Toast.makeText(ui.getActivity().getApplicationContext(),
                            R.string.pref_temporal_edge_swipe_enabled_toast, Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        );

        final RadioButton spatialNavButton =
                (RadioButton) container.findViewById(R.id.edge_sliding_settings_options_spatial);
        spatialNavButton.setOnClickListener(
            new View.OnClickListener() {
                public void onClick(View v) {
                    BrowserSettings.getInstance().setEdgeSwipeSpatial();
                    goLive();
                    applySettingsAndRefresh(ui, container);
                    Toast toast = Toast.makeText(ui.getActivity().getApplicationContext(),
                            R.string.pref_spatial_edge_swipe_enabled_toast, Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        );

        final RadioButton disabledNavButton =
                (RadioButton) container.findViewById(R.id.edge_sliding_settings_options_disabled);
        disabledNavButton.setOnClickListener(
            new View.OnClickListener() {
                public void onClick(View v) {
                    BrowserSettings.getInstance().setEdgeSwipeDisabled();
                    goLive();
                    applySettingsAndRefresh(ui, container);
                    Toast toast = Toast.makeText(ui.getActivity().getApplicationContext(),
                            R.string.pref_edge_swipe_disabled_toast, Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        );
    }

    private void applySettingsAndRefresh(final BaseUi ui, final View container) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {
                mDragHelper = null;
                ui.refreshEdgeSwipeController(container);
                return null;
            }
        }.execute();
    }

    private void goLive() {
        mbWaitForSettings = false;
        mFromEdge = ViewDragHelper.EDGE_TOP;
        mLiveView.setVisibility(View.VISIBLE);
        mStationaryView.setVisibility(View.GONE);
        mSlidingViewShadow.setVisibility(View.GONE);
        mSettingsView.setVisibility(View.GONE);
        mViewGroup.postInvalidate();
    }

    private void goDormant() {
        mLiveView.setVisibility(View.GONE);
        mStationaryView.setVisibility(View.VISIBLE);
        mViewGroup.invalidate();
    }

    public void onConfigurationChanged() {
        goLive();
    }

    public void cleanup() {
        synchronized (this) {
            goLive();
            mModel.cleanup();
        }
    }


    private void showCurrBitmap() {
        if (mStationaryView.getVisibility() == View.VISIBLE) {
            return;
        }

        Bitmap currBM = mModel.readSnapshot(mCurrIndex);
        if (currBM != null) {
            clampViewIfNeeded(mStationaryView);
            mStationaryView.setImageBitmap(currBM);
            goDormant();
            mModel.deleteSnapshot(mCurrIndex);
        }
    }

    private void clampViewIfNeeded(View view) {
        int offset = 0;
        if (mTitleBar.getY() >= 0) {
            offset = mTitleBar.getNavigationBar().getMeasuredHeight();
        }
        view.setPadding(0, offset - view.getTop(), 0, 0);
    }

    public void onViewDragStateChanged(int state) {
        if (ViewDragHelper.STATE_IDLE == state && !mbWaitForSettings) {
            goLive();
        }
    }

    public void onViewReleased(View releasedChild, float xvel, float yvel) {
        boolean bCrossedEventHorizon = Math.abs(mLeft) > mViewGroup.getWidth() / 2;

        switch (mFromEdge) {
            case ViewDragHelper.EDGE_LEFT:
                if (xvel > 0 || (xvel == 0 && mLeft > 0 && bCrossedEventHorizon)) {
                    showCurrBitmap();
                    mbWaitForSettings = true;
                    mDragHelper.settleCapturedViewAt(
                            releasedChild.getMeasuredWidth(),
                            releasedChild.getTop());
                    break;
                }
                mDragHelper.settleCapturedViewAt(0, releasedChild.getTop());
                break;
            case ViewDragHelper.EDGE_RIGHT:
                if (xvel < 0 || (xvel == 0 && mLeft < 0 && bCrossedEventHorizon)) {
                    showCurrBitmap();
                    mbWaitForSettings = true;
                    mDragHelper.settleCapturedViewAt(
                            -releasedChild.getMeasuredWidth(),
                            releasedChild.getTop());
                    break;
                }
                mDragHelper.settleCapturedViewAt(0, releasedChild.getTop());
                break;
            default:
                mDragHelper.settleCapturedViewAt(0, releasedChild.getTop());
                break;
        }
        mLeft = 0;
        mViewGroup.invalidate();
    }

    public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
        showCurrBitmap();
        switch (mFromEdge) {
            case ViewDragHelper.EDGE_LEFT:
                mSlidingViewShadow.setX(left);
                mViewGroup.invalidate();
                break;
            case ViewDragHelper.EDGE_RIGHT:
                mSlidingViewShadow.setX(mViewGroup.getMeasuredWidth() + left
                        - mSlidingViewShadow.getMeasuredWidth());
                mViewGroup.invalidate();
                break;
            default:
                break;
        }
    }

    public void onEdgeDragStarted(int edgeFlags, int pointerId) {
        if (mFromEdge != ViewDragHelper.EDGE_TOP) {
            return;
        }

        mCurrIndex = mActiveTab.getWebView().copyBackForwardList().getCurrentIndex();

        mModel.updateSnapshot(mCurrIndex);

        clampViewIfNeeded(mSettingsView);

        if (ViewDragHelper.EDGE_LEFT == (edgeFlags & ViewDragHelper.EDGE_LEFT)) {
            mFromEdge = ViewDragHelper.EDGE_LEFT;

            mSettingsView.setTranslationX(-mViewGroup.getMeasuredWidth());
            mSlidingViewShadow.setBackgroundResource(R.drawable.right_shade);
        } else if (ViewDragHelper.EDGE_RIGHT == (edgeFlags & ViewDragHelper.EDGE_RIGHT)) {
            mFromEdge = ViewDragHelper.EDGE_RIGHT;

            mSettingsView.setTranslationX(mViewGroup.getMeasuredWidth());
            mSlidingViewShadow.setBackgroundResource(R.drawable.left_shade);
        }

        mSettingsView.setVisibility(View.VISIBLE);
        mSlidingViewShadow.setVisibility(View.VISIBLE);

        showCurrBitmap();

        mViewGroup.invalidate();
    }

    public int getOrderedChildIndex(int index) {
        return mSlidingViewIndex;
    }

    public int getViewHorizontalDragRange(View child) {
        return child.getMeasuredWidth();
    }

    public boolean tryCaptureView(View child, int pointerId) {
        return (mFromEdge != ViewDragHelper.EDGE_TOP && child == mSettingsView);
    }

    public int clampViewPositionHorizontal(View child, int left, int dx) {
        mLeft = left;
        return left;
    }
}
