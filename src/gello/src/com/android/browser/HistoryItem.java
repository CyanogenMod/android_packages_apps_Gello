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
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ExpandableListView;

import com.android.browser.platformsupport.Browser;
/**
 *  Layout representing a history item in the classic history viewer.
 */
/* package */ class HistoryItem extends BookmarkItem
        implements OnCheckedChangeListener, View.OnClickListener {

    private CompoundButton  mStar;      // Star for bookmarking
    /**
     *  Create a new HistoryItem.
     *  @param context  Context for this HistoryItem.
     */
    /* package */ HistoryItem(Context context) {
        this(context, true);
    }

    /* package */ HistoryItem(Context context, boolean showStar) {
        super(context);

        mStar = (CompoundButton) findViewById(R.id.star);
        mStar.setOnCheckedChangeListener(this);
        if (showStar) {
            mStar.setVisibility(View.VISIBLE);
        } else {
            mStar.setVisibility(View.GONE);
        }

        mTileView.setOnClickListener(this);
    }
    
    /* package */ void copyTo(HistoryItem item) {
        item.mTextView.setText(mTextView.getText());
        item.mUrlText.setText(mUrlText.getText());
        item.setIsBookmark(mStar.isChecked());
        item.mTileView.replaceFavicon(mBitmap);
    }

    /**
     * Whether or not this item represents a bookmarked site
     */
    /* package */ boolean isBookmark() {
        return mStar.isChecked();
    }

    /**
     *  Set whether or not this represents a bookmark, and make sure the star
     *  behaves appropriately.
     */
    /* package */ void setIsBookmark(boolean isBookmark) {
        mStar.setOnCheckedChangeListener(null);
        mStar.setChecked(isBookmark);
        mStar.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView,
            boolean isChecked) {
        if (isChecked) {
            // Uncheck ourseves. When the bookmark is actually added,
            // we will be notified
            setIsBookmark(false);
            Browser.saveBookmark(getContext(), getName(), mUrl);
        } else {
            Bookmarks.removeFromBookmarks(getContext(),
                    getContext().getContentResolver(), mUrl, getName());
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mTileView) {
            ExpandableListView list = (ExpandableListView) getTag(R.id.combo_view_container);
            int group = (int) getTag(R.id.group_position);
            int pos = (int) getTag(R.id.child_position);
            if (list != null) {
                long packedPos = list.getPackedPositionForChild(group, pos);
                int flatPos = list.getFlatListPosition(packedPos);
                list.performItemClick(
                        list.getAdapter().getView(flatPos, null, null),
                        flatPos, list.getAdapter().getItemId(flatPos));
            }
            performClick();
        }
    }
}
