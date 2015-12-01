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
import android.util.SparseArray;
import android.webkit.ValueCallback;

import org.codeaurora.swe.WebHistoryItem;

public class EdgeSwipeModel {
    private static final int MS_TIME_BETWEEN_CAPTURES = 1000;
    private SparseArray<Bitmap> mBitmaps;
    private SparseArray<Integer> mColors;

    private long mLastCaptureTime;
    private int mLastCaptureIndex;
    private Bitmap mLastBitmap;

    private Tab mTab;
    private TitleBar mBar;

    private static final int mMinProgress = 85;

    private static final int mMaxBitmaps = 5;

    public EdgeSwipeModel(Tab tab, TitleBar bar) {
        mTab = tab;
        mBar = bar;
        mLastCaptureIndex = -1;
        mLastCaptureTime = 0;
        mBitmaps = new SparseArray<>();
        mColors = new SparseArray<>();
    }

    public void updateSnapshot(final int index) {
        if (mBitmaps.get(index) != null) {
            return;
        }

        final int captureIndex = mTab.getCaptureIndex(index);

        boolean bitmapExists = mTab.getWebView().hasSnapshot(captureIndex);

        if (!mTab.isFirstVisualPixelPainted()) {
            fetchSnapshot(index);
            return;
        }

        int progress = mBar.getProgressView().getProgressPercent();
        long currentTime = System.currentTimeMillis();

        if (bitmapExists) {
            if (progress < mMinProgress || (captureIndex == mLastCaptureIndex &&
                    (currentTime < (mLastCaptureTime + MS_TIME_BETWEEN_CAPTURES)))) {
                fetchSnapshot(index);
                return;
            }
        }

        mTab.getWebView().captureSnapshot(captureIndex,
                new ValueCallback<Bitmap>() {
                    @Override
                    public void onReceiveValue(Bitmap value) {
                        mBitmaps.put(index, value);
                        mLastCaptureTime = System.currentTimeMillis();
                        mLastCaptureIndex = captureIndex;
                        mLastBitmap = value;
                    }
                }
        );
    }

    public void fetchSnapshot(final int index) {
        if (mColors.get(index) == null && mTab.getWebView() != null) {
            WebHistoryItem item = mTab.getWebView().copyBackForwardList().getItemAtIndex(index);
            if (item != null) {
                String url = item.getUrl();
                int color = NavigationBarBase.getSiteIconColor(url);
                if (color != 0) {
                    mColors.put(index, color);
                }
            }
        }

        if (mBitmaps.get(index) != null) {
            return;
        }

        int captureIndex = mTab.getCaptureIndex(index);
        long currentTime = System.currentTimeMillis();
        if (captureIndex == mLastCaptureIndex &&
                (currentTime < (mLastCaptureTime + MS_TIME_BETWEEN_CAPTURES))) {
            mBitmaps.put(index, mLastBitmap);
            return;
        }

        mTab.getWebView().getSnapshot(captureIndex,
                new ValueCallback<Bitmap>() {
                    @Override
                    public void onReceiveValue(Bitmap bitmap) {
                        mBitmaps.put(index, bitmap);
                    }
                }
        );
    }

    public Bitmap readSnapshot(int index) {
        if (index < 0) {
            return null;
        }

        if (index > (mTab.getWebView().copyBackForwardList().getSize() - 1)) {
            return null;
        }

        return mBitmaps.get(index);
    }

    public int getColor(int index) {
        if (index < 0) {
            return Color.DKGRAY;
        }

        if (index > (mTab.getWebView().copyBackForwardList().getSize() - 1)) {
            return Color.DKGRAY;
        }

        Integer color = mColors.get(index);
        if (color != null) {
            return color.intValue();
        }

        return Color.DKGRAY;
    }

    public void deleteSnapshot(int index) {
        mBitmaps.delete(index);
    }

    public void cleanup() {
        mBitmaps.clear();
        mColors.clear();
    }
}
