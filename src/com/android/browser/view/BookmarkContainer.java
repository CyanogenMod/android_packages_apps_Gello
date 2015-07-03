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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

import com.android.browser.R;
import com.android.browser.SiteTileView;

public class BookmarkContainer extends LinearLayout implements OnClickListener {

    private OnClickListener mClickListener;
    private boolean mIgnoreRequestLayout = false;

    public BookmarkContainer(Context context) {
        super(context);
        init();
    }

    public BookmarkContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BookmarkContainer(
            Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    void init() {
        setFocusable(true);
        super.setOnClickListener(this);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        mClickListener = l;
        View thumb =  findViewById(R.id.thumb_image);
        if (thumb != null) {
            thumb.setOnClickListener(l);
        }
    }

    @Override
    public void setTag(int key, final Object tag) {
        super.setTag(key, tag);
        View thumb =  findViewById(R.id.thumb_image);
        if (thumb != null) {
            thumb.setTag(key, tag);
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        updateTransitionDrawable(isPressed());
    }

    void updateTransitionDrawable(boolean pressed) {
        final int longPressTimeout = ViewConfiguration.getLongPressTimeout();
        Drawable selector = getBackground();
        if (selector != null && selector instanceof StateListDrawable) {
            Drawable d = ((StateListDrawable)selector).getCurrent();
            if (d != null && d instanceof TransitionDrawable) {
                if (pressed && isLongClickable()) {
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
        if (!mIgnoreRequestLayout) {
            super.requestLayout();
        }
    }
}
