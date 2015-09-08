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
import android.graphics.Color;
import android.support.v4.widget.ViewDragHelper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class EdgeSwipeView {
    private ImageView mStationaryView;
    private ImageView mSlidingView;
    private ImageView mSlidingViewShadow;
    private View mOpacityView;
    private FrameLayout mLiveView;
    private DraggableFrameLayout mViewGroup;

    private int mSlidingViewIndex;

    private boolean mbShowingLive = true;

    private boolean mbStationaryViewBMSet = false;
    private boolean mbSlidingViewBMSet = false;

    private TitleBar mTitleBar;

    public EdgeSwipeView(
            View container,
            int stationaryViewId,
            int slidingViewId,
            int slidingViewShadowId,
            int opacityViewId,
            int liveViewId,
            int viewGroupId,
            TitleBar titleBar) {
        mStationaryView = (ImageView) container.findViewById(stationaryViewId);
        mSlidingView = (ImageView) container.findViewById(slidingViewId);
        mSlidingViewShadow = (ImageView) container.findViewById(slidingViewShadowId);
        mOpacityView = container.findViewById(opacityViewId);
        mLiveView = (FrameLayout) container.findViewById(liveViewId);
        mViewGroup = (DraggableFrameLayout) container.findViewById(viewGroupId);
        mSlidingViewShadow.setBackgroundResource(R.drawable.left_shade);

        mSlidingView.setVisibility(View.GONE);
        mSlidingViewShadow.setVisibility(View.GONE);
        mOpacityView.setVisibility(View.GONE);

        final int childCount = mViewGroup.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            final View child = mViewGroup.getChildAt(i);
            if (mSlidingView == child) {
                mSlidingViewIndex = i;
                break;
            }
        }

        mTitleBar = titleBar;
    }

    public void goLive() {
        if (mbShowingLive)
            return;

        mLiveView.setVisibility(View.VISIBLE);
        mStationaryView.setVisibility(View.GONE);
        mSlidingView.setVisibility(View.GONE);
        mSlidingViewShadow.setVisibility(View.GONE);
        mOpacityView.setVisibility(View.GONE);
        mbShowingLive = true;
    }

    public void goDormant() {
        if (!mbShowingLive)
            return;

        mSlidingView.setVisibility(View.VISIBLE);
        mStationaryView.setVisibility(View.VISIBLE);
        mOpacityView.setVisibility(View.VISIBLE);
        mLiveView.setVisibility(View.GONE);
        mbShowingLive = false;
    }

    public boolean isLive() {
        return mbShowingLive;
    }

    private Bitmap getColorBitmap(int color)
    {
        int height = mViewGroup.getMeasuredHeight();
        int width = mViewGroup.getMeasuredWidth();
        height -= (mTitleBar.getY()>= 0) ? mTitleBar.getNavigationBar().getMeasuredHeight() : 0;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        bitmap.eraseColor(color);
        return bitmap;
    }

    private void clampViewIfNeeded(View view) {
        int offset = 0;
        if (mTitleBar.getY() >= 0) {
            offset = mTitleBar.getNavigationBar().getMeasuredHeight();
        }
        view.setPadding(0, offset - view.getTop(), 0, 0);
    }

    public boolean isPortrait() {
        return (mViewGroup.getHeight() < mViewGroup.getWidth());
    }

    private void setBitmap(ImageView view, Bitmap bitmap, int color) {
        clampViewIfNeeded(view);
        if (bitmap == null) {
            bitmap = getColorBitmap(color);
        }

        int offset = 0;
        if (mTitleBar.getY() >= 0) {
            offset = mTitleBar.getNavigationBar().getMeasuredHeight();
        }

        if ((view.getMeasuredHeight() > view.getMeasuredWidth()) !=
                (bitmap.getHeight() > bitmap.getWidth())) {
            view.setImageBitmap(bitmap);
            return;
        }

        int bitmap_height = bitmap.getHeight();

        if (view.getMeasuredHeight() != 0) {
            bitmap_height = (view.getMeasuredHeight() - offset) * bitmap.getWidth() /
                    view.getMeasuredWidth();
        }

        if ((bitmap.getHeight() - bitmap_height)  > 5) {
            Bitmap cropped = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap_height);
            view.setImageBitmap(cropped);
        } else {
            view.setImageBitmap(bitmap);
        }
    }

    public void setStationaryViewBitmap(Bitmap bitmap, int color) {
        mbStationaryViewBMSet = null != bitmap;
        setBitmap(mStationaryView, bitmap, color);
    }

    public void setStationaryViewAlpha(float alpha) {
        mStationaryView.setAlpha(alpha);
    }

    public void setSlidingViewBitmap(Bitmap bitmap, int color) {
        mbSlidingViewBMSet = null != bitmap;
        setBitmap(mSlidingView, bitmap, color);
    }

    public boolean slidingViewHasImage() {
        return mbSlidingViewBMSet;
    }

    public boolean stationaryViewHasImage() {
        return mbStationaryViewBMSet;
    }

    public void slidingViewTouched(int edge) {
        if (edge == ViewDragHelper.EDGE_RIGHT) {
            mSlidingView.setTranslationX(mViewGroup.getMeasuredWidth());
        } else {
            mSlidingView.setTranslationX(0);
        }
    }

    public void hideSlidingViews() {
        mSlidingViewShadow.setVisibility(View.GONE);
        mSlidingView.setVisibility(View.GONE);
    }

    public void showSlidingViews() {
        mSlidingViewShadow.setVisibility(View.VISIBLE);
        mSlidingView.setVisibility(View.VISIBLE);
    }

    public int slidingViewIndex() {
        return mSlidingViewIndex;
    }

    public void moveShadowView(float x) {
        x -= mSlidingViewShadow.getMeasuredWidth();
        mSlidingViewShadow.setX(x);
        mSlidingViewShadow.setVisibility(View.VISIBLE);
        mOpacityView.setVisibility(View.VISIBLE);
    }

    public boolean allowCapture(View view) {
        return (view == mSlidingView);
    }

    public int getMeasuredWidth() {
        return mViewGroup.getMeasuredWidth();
    }

    public int getWidth() {
        return mViewGroup.getWidth();
    }

    public void init() {
        clampViewIfNeeded(mSlidingView);
        clampViewIfNeeded(mStationaryView);
    }

    public void invalidate() {
        mViewGroup.invalidate();
    }
}
