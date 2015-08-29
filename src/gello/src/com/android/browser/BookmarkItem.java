/*
 * Copyright (C) 2008 The Android Open Source Project
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
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 *  Custom layout for an item representing a bookmark in the browser.
 */
class BookmarkItem extends ScrollView {

    final static int MAX_TEXTVIEW_LEN = 80;

    protected TextView     mTextView;
    protected TextView     mUrlText;
    protected SiteTileView mTileView;
    protected String       mUrl;
    protected String       mTitle;
    protected boolean mEnableScrolling = false;

    protected Bitmap  mBitmap;
    /**
     *  Instantiate a bookmark item, including a default favicon.
     *
     *  @param context  The application context for the item.
     */
    BookmarkItem(Context context) {
        super(context);

        setClickable(false);
        setEnableScrolling(false);
        LayoutInflater factory = LayoutInflater.from(context);
        factory.inflate(R.layout.history_item, this);
        mTextView = (TextView) findViewById(R.id.title);
        mUrlText = (TextView) findViewById(R.id.url);
        mTileView = (SiteTileView) findViewById(R.id.favicon);
        View star = findViewById(R.id.star);
        star.setVisibility(View.GONE);
    }

    /**
     * Return the name assigned to this bookmark item.
     */
    /* package */ String getName() {
        return mTitle;
    }

    /* package */ String getUrl() {
        return mUrl;
    }

    /**
     *  Set the favicon for this item.
     *
     *  @param b    The new bitmap for this item.
     *              If it is null, will use the default.
     */
    /* package */ void setFavicon(Bitmap b) {
        if (b != null) {
            mTileView.replaceFavicon(b);
            mBitmap = b;
        }
    }

    void setFaviconBackground(Drawable d) {
        mTileView.setBackgroundDrawable(d);
    }

    /**
     *  Set the new name for the bookmark item.
     *
     *  @param name The new name for the bookmark item.
     */
    /* package */ void setName(String name) {
        if (name == null) {
            return;
        }

        mTitle = name;

        if (name.length() > MAX_TEXTVIEW_LEN) {
            name = name.substring(0, MAX_TEXTVIEW_LEN);
        }

        mTextView.setText(name);
    }

    /**
     *  Set the new url for the bookmark item.
     *  @param url  The new url for the bookmark item.
     */
    /* package */ void setUrl(String url) {
        if (url == null) {
            return;
        }

        mUrl = url;

        url = UrlUtils.stripUrl(url);

        /*
        * Since there are more than 80 characters
        * in the URL this is formatting the url
        * to a vertical Scroll View.
        */
        if (url.length() > MAX_TEXTVIEW_LEN) {

            // url cannot exceed max length
            if (url.length() > UrlInputView.URL_MAX_LENGTH) {
                url = url.substring(0, UrlInputView.URL_MAX_LENGTH);
            }

            mUrlText.setHorizontallyScrolling(false);
            mUrlText.setSingleLine(false);
            mUrlText.setVerticalScrollBarEnabled(true);
            /*
            * Only the first 3 lines of the URL will be visible
            * Rest of it will be scrollable.
            */
            mUrlText.setMaxLines(3);
        }

        mUrlText.setText(url);
    }

    void setEnableScrolling(boolean enable) {
        mEnableScrolling = enable;
        setFocusable(mEnableScrolling);
        setFocusableInTouchMode(mEnableScrolling);
        requestDisallowInterceptTouchEvent(!mEnableScrolling);
        requestLayout();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mEnableScrolling) {
            return super.onTouchEvent(ev);
        }
        return false;
    }

    @Override
    protected void measureChild(View child, int parentWidthMeasureSpec,
            int parentHeightMeasureSpec) {
        if (mEnableScrolling) {
            super.measureChild(child, parentWidthMeasureSpec, parentHeightMeasureSpec);
            return;
        }

        final ViewGroup.LayoutParams lp = child.getLayoutParams();

        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                getPaddingLeft() + getPaddingRight(), lp.width);
        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                getPaddingTop() + getPaddingBottom(), lp.height);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void measureChildWithMargins(View child,
            int parentWidthMeasureSpec, int widthUsed,
            int parentHeightMeasureSpec, int heightUsed) {
        if (mEnableScrolling) {
            super.measureChildWithMargins(child, parentWidthMeasureSpec,
                    widthUsed, parentHeightMeasureSpec, heightUsed);
            return;
        }

        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin
                        + widthUsed, lp.width);
        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin
                        + heightUsed, lp.height);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }
}
