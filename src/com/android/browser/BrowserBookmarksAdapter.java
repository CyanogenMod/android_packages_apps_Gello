/*
 * Copyright (C) 2006 The Android Open Source Project
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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.browser.mdm.EditBookmarksRestriction;
import com.android.browser.mdm.ManagedBookmarksRestriction;
import com.android.browser.platformsupport.BrowserContract.Bookmarks;
import com.android.browser.util.ThreadedCursorAdapter;
import com.android.browser.view.BookmarkContainer;
import com.android.browser.view.BookmarkThumbImageView;

public class BrowserBookmarksAdapter extends
        ThreadedCursorAdapter<BrowserBookmarksAdapterItem> {

    private static final String TAG = "BrowserBookmarksAdapter";
    LayoutInflater mInflater;
    Context mContext;

    /**
     *  Create a new BrowserBookmarksAdapter.
     */
    public BrowserBookmarksAdapter(Context context) {
        // Make sure to tell the CursorAdapter to avoid the observer and auto-requery
        // since the Loader will do that for us.
        super(context, null);
        mInflater = LayoutInflater.from(context);
        mContext = context;
    }

    @Override
    protected long getItemId(Cursor c) {
        return c.getLong(BookmarksLoader.COLUMN_INDEX_ID);
    }

    @Override
    public View newView(Context context, ViewGroup parent) {
        return mInflater.inflate(R.layout.bookmark_thumbnail, parent, false);
    }

    @Override
    public void bindView(View view, BrowserBookmarksAdapterItem object) {
        BookmarkContainer container = (BookmarkContainer) view;
        container.setIgnoreRequestLayout(true);
        bindGridView(view, mContext, object);
        container.setIgnoreRequestLayout(false);
    }

    CharSequence getTitle(Cursor cursor) {
        int type = cursor.getInt(BookmarksLoader.COLUMN_INDEX_TYPE);
        switch (type) {
        case Bookmarks.BOOKMARK_TYPE_OTHER_FOLDER:
            return mContext.getText(R.string.other_bookmarks);
        }
        return cursor.getString(BookmarksLoader.COLUMN_INDEX_TITLE);
    }

    void bindGridView(View view, Context context, BrowserBookmarksAdapterItem item) {
        // We need to set this to handle rotation and other configuration change
        // events. If the padding didn't change, this is a no op.
        int padding = context.getResources()
                .getDimensionPixelSize(R.dimen.combo_horizontalSpacing);
        view.setPadding(padding, view.getPaddingTop(),
                padding, view.getPaddingBottom());
        SiteTileView thumb =  (SiteTileView) view.findViewById(R.id.thumb_image);
        TextView tv = (TextView) view.findViewById(R.id.label);
        tv.setText(item.title);

        Bitmap b;

        thumb.setFloating(false);

        if (item.is_folder) {
            b = BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.ic_deco_folder_normal);
            thumb.setFloating(true);
        }
        else if (item.thumbnail == null || !item.has_thumbnail) {
            b = BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.browser_thumbnail);
        }
        else {
            b = item.thumbnail.getBitmap();
        }

        // If the item is managed by mdm or edit bookmark restriction enabled
        if (item.title != null &&
                (item.is_mdm_managed || EditBookmarksRestriction.getInstance().isEnabled())) {
            int containerWidth = view.getResources().getDimensionPixelSize(R.dimen.bookmarkThumbnailWidth);
            int containerHeight = view.getResources().getDimensionPixelSize(R.dimen.bookmarkThumbnailHeight);
            Bitmap bm;

            if (item.is_mdm_managed) {
                bm = BrowserBookmarksPage.overlayBookmarkBitmap(mContext, b,
                        R.drawable.img_deco_mdm_badge_bright,
                        containerWidth, containerHeight, 0.6f, 185, 20);
            }
            else {
                bm = BrowserBookmarksPage.overlayBookmarkBitmap(mContext, b,
                        R.drawable.ic_deco_secure,
                        containerWidth, containerHeight, 1.7f, 110, 0);
            }

            thumb.replaceFavicon(bm);
        }
        else {
            thumb.replaceFavicon(b);
        }
        thumb.setLongClickable(true);
    }

    @Override
    public BrowserBookmarksAdapterItem getRowObject(Cursor c,
            BrowserBookmarksAdapterItem item) {
        if (item == null) {
            item = new BrowserBookmarksAdapterItem();
        }
        Bitmap thumbnail = item.thumbnail != null ? item.thumbnail.getBitmap() : null;

        thumbnail = BrowserBookmarksPage.getBitmap(c,
                BookmarksLoader.COLUMN_INDEX_TOUCH_ICON, thumbnail);
        if (thumbnail == null) {
            thumbnail = BrowserBookmarksPage.getBitmap(c,
                    BookmarksLoader.COLUMN_INDEX_THUMBNAIL, thumbnail);
        }
        item.has_thumbnail = thumbnail != null;
        if (thumbnail != null
                && (item.thumbnail == null || item.thumbnail.getBitmap() != thumbnail)) {
            item.thumbnail = new BitmapDrawable(mContext.getResources(), thumbnail);
        }
        item.is_folder = c.getInt(BookmarksLoader.COLUMN_INDEX_IS_FOLDER) != 0;
        item.title = getTitle(c);
        item.url = c.getString(BookmarksLoader.COLUMN_INDEX_URL);
        item.is_mdm_managed = ManagedBookmarksRestriction.getInstance().mDb.isMdmElement(getItemId(c));
        return item;
    }

    @Override
    public BrowserBookmarksAdapterItem getLoadingObject() {
        BrowserBookmarksAdapterItem item = new BrowserBookmarksAdapterItem();
        return item;
    }
}
