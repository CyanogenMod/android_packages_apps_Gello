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

package com.android.browser.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.browser.FolderTileView;
import com.android.browser.R;
import com.android.browser.SiteTileView;

public class BookmarkContainer extends LinearLayout implements OnClickListener {

    private OnClickListener mClickListener;
    private boolean mIgnoreRequestLayout = false;

    private FrameLayout mTileContainer;
    private FolderTileView mFolderTile;
    private SiteTileView mSiteTile;
    private ImageView mOverlayBadge;

    public BookmarkContainer(Context context) {
        super(context);
        init();
    }

    public BookmarkContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BookmarkContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    void init() {
        setFocusable(true);

        if (mSiteTile == null) {
            mSiteTile = new SiteTileView(getContext(), (Bitmap)null);
        }

        if (mFolderTile == null) {
            mFolderTile = new FolderTileView(getContext(), null, null);
            mFolderTile.setClickable(true);
        }
        super.setOnClickListener(this);
    }

    public void reConfigureAsFolder(String title, String numItems) {
        // hide elements that may have been already created
        mSiteTile.setVisibility(View.GONE);
        if (mOverlayBadge != null)
            mOverlayBadge.setVisibility(View.GONE);

        // reconfigure the existing Folder
        mFolderTile.setVisibility(View.VISIBLE);
        mFolderTile.setText(title);
        mFolderTile.setLabel(numItems);
        addTileToContainer(mFolderTile);
    }

    public void reConfigureAsSite(Bitmap favicon) {
        // hide elements that may have been already created
        mFolderTile.setVisibility(View.GONE);
        if (mOverlayBadge != null)
            mOverlayBadge.setVisibility(View.GONE);

        // reconfigure the existing Site
        mSiteTile.setVisibility(View.VISIBLE);
        mSiteTile.replaceFavicon(favicon);
        addTileToContainer(mSiteTile);
    }

    public void setBottomLabelText(String bottomLabel) {
        ((TextView) findViewById(R.id.label)).setText(bottomLabel);
        ((TextView) findViewById(R.id.label)).setEllipsize(TextUtils.TruncateAt.END);
    }

    public void setOverlayBadge(int imgResId) {
        // remove the badge if already existing
        if (imgResId == 0) {
            if (mOverlayBadge != null) {
                mOverlayBadge.setVisibility(View.GONE);
                mOverlayBadge.setImageDrawable(null);
            }
            return;
        }

        // create the badge if needed and not present
        if (mOverlayBadge == null) {
            mOverlayBadge = new ImageView(getContext());
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.gravity = Gravity.BOTTOM | Gravity.END;
            FrameLayout frameLayout = (FrameLayout) findViewById(R.id.container);
            frameLayout.addView(mOverlayBadge, lp);
        }
        mOverlayBadge.setVisibility(View.VISIBLE);
        mOverlayBadge.bringToFront();
        mOverlayBadge.setImageResource(imgResId);
    }


    private void addTileToContainer(View view) {
        if (view.getParent() != null) {
            return;
        }

        // insert the view in the container, filling it
        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.container);
        frameLayout.addView(view, 0, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));
        // common customizations for folders or sites
        view.setLongClickable(true);
    }


    @Override
    public void setOnClickListener(OnClickListener l) {
        mClickListener = l;
        mSiteTile.setOnClickListener(l);
        mFolderTile.setOnClickListener(l);
    }

    @Override
    public void setTag(int key, final Object tag) {
        super.setTag(key, tag);
        mSiteTile.setTag(key, tag);
        mFolderTile.setTag(key, tag);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        updateTransitionDrawable(isPressed());
    }

    void updateTransitionDrawable(boolean pressed) {
        Drawable selector = getBackground();
        if (selector != null && selector instanceof StateListDrawable) {
            Drawable d = ((StateListDrawable)selector).getCurrent();
            if (d != null && d instanceof TransitionDrawable) {
                if (pressed && isLongClickable()) {
                    final int longPressTimeout = ViewConfiguration.getLongPressTimeout();
                    ((TransitionDrawable) d).startTransition(longPressTimeout);
                } else {
                    ((TransitionDrawable) d).resetTransition();
                }
            }
        }
    }

    @Override
    public void onClick(View view) {
        updateTransitionDrawable(false);
        if (mClickListener != null) {
            mClickListener.onClick(view);
        }
    }

    public void setIgnoreRequestLayout(boolean ignore) {
        mIgnoreRequestLayout = ignore;
    }

    @Override
    public void requestLayout() {
        if (!mIgnoreRequestLayout)
            super.requestLayout();
    }

}
