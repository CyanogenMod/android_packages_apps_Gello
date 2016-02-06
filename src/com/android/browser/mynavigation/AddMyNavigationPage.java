/*
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *       * Redistributions in binary form must reproduce the above
 *         copyright notice, this list of conditions and the following
 *         disclaimer in the documentation and/or other materials provided
 *         with the distribution.
 *       * Neither the name of The Linux Foundation nor the names of its
 *         contributors may be used to endorse or promote products derived
 *         from this software without specific prior written permission.
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

package com.android.browser.mynavigation;

import android.app.Activity;
import android.content.Context;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ParseException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.util.Log;

import com.android.browser.BrowserUtils;
import com.android.browser.R;
import com.android.browser.UrlUtils;
import com.android.browser.platformsupport.WebAddress;
import com.android.browser.EngineInitializer;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;

public class AddMyNavigationPage extends Activity {

    private static final String LOGTAG = "AddMyNavigationPage";
    private static final int SAVE_SITE_NAVIGATION = 100;

    private EditText mName;
    private EditText mAddress;
    private Button mButtonOK;
    private Button mButtonCancel;
    private Bundle mMap;
    private String mItemUrl;
    private boolean mIsAdding;
    private TextView mDialogText;
    private Handler mHandler;

    private View.OnClickListener mOKListener = new View.OnClickListener() {
        public void onClick(View v) {
            save();
        }
    };

    private View.OnClickListener mCancelListener = new View.OnClickListener() {
        public void onClick(View v) {
            finish();
        }
    };

    protected void onCreate(Bundle icicle) {
        if (!EngineInitializer.isInitialized()) {
            Log.e(LOGTAG, "Engine not Initialized");
            EngineInitializer.initializeSync((Context) getApplicationContext());
        }
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.my_navigation_add_page);
        String name = null;
        String url = null;
        mMap = getIntent().getExtras();
        if (mMap != null) {
            Bundle b = mMap.getBundle("websites");
            if (b != null) {
                mMap = b;
            }
            name = mMap.getString("name");
            url = mMap.getString("url");
            mIsAdding = mMap.getBoolean("isAdding");
        }

        // The original url
        mItemUrl = url;
        mName = (EditText) findViewById(R.id.title);
        mAddress = (EditText) findViewById(R.id.address);

        BrowserUtils.maxLengthFilter(AddMyNavigationPage.this, mName,
                BrowserUtils.FILENAME_MAX_LENGTH);
        BrowserUtils.maxLengthFilter(AddMyNavigationPage.this, mAddress,
                BrowserUtils.ADDRESS_MAX_LENGTH);

        if (url.startsWith("ae://") && url.endsWith("add-fav")) {
            mName.setText("");
            mAddress.setText("");
        } else {
            mName.setText(name);
            mAddress.setText(url);
        }
        mDialogText = (TextView) findViewById(R.id.dialog_title);
        if (mIsAdding) {
            mDialogText.setText(R.string.my_navigation_add_label);
        }

        mButtonOK = (Button) findViewById(R.id.OK);
        mButtonOK.setOnClickListener(mOKListener);
        mButtonCancel = (Button) findViewById(R.id.cancel);
        mButtonCancel.setOnClickListener(mCancelListener);

        if (!getWindow().getDecorView().isInTouchMode()) {
            mButtonOK.requestFocus();
        }
    }

    /**
     * Runnable to save a website
     */
    private class SaveMyNavigationRunnable implements Runnable {
        private Message mMessage;

        public SaveMyNavigationRunnable(Message msg) {
            mMessage = msg;
        }

        public void run() {
            Bundle bundle = mMessage.getData();
            String title = bundle.getString("title");
            String url = bundle.getString("url");
            String itemUrl = bundle.getString("itemUrl");
            Boolean toDefaultThumbnail = bundle.getBoolean("toDefaultThumbnail");
            ContentResolver cr = AddMyNavigationPage.this.getContentResolver();
            Cursor cursor = null;
            try {
                cursor = cr.query(MyNavigationUtil.MY_NAVIGATION_URI,
                        new String[] {
                            MyNavigationUtil.ID
                        }, "url = ?", new String[] {
                            itemUrl
                        }, null);
                if (cursor != null && cursor.moveToFirst()) {
                    ContentValues values = new ContentValues();
                    values.put(MyNavigationUtil.TITLE, title);
                    values.put(MyNavigationUtil.URL, url);
                    values.put(MyNavigationUtil.WEBSITE, 1 + "");
                    if (toDefaultThumbnail) {
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        Bitmap bm = BitmapFactory.decodeResource(
                                AddMyNavigationPage.this.getResources(),
                                R.raw.my_navigation_thumbnail_default);
                        bm.compress(Bitmap.CompressFormat.PNG, 100, os);
                        values.put(MyNavigationUtil.THUMBNAIL, os.toByteArray());
                    }
                    Uri uri = ContentUris.withAppendedId(MyNavigationUtil.MY_NAVIGATION_URI,
                            cursor.getLong(0));
                    cr.update(uri, values, null, null);
                    AddMyNavigationPage.this.setResult(Activity.RESULT_OK,
                        (new Intent()).putExtra("need_refresh", true));
                } else {
                    Log.e(LOGTAG, "this item does not exist!");
                }
            } catch (IllegalStateException e) {
                Log.e(LOGTAG, "SaveMyNavigationRunnable", e);
            } finally {
                if (null != cursor) {
                    cursor.close();
                    AddMyNavigationPage.this.finish();
                }
            }
        }
    }

    boolean save() {
        String name = mName.getText().toString().trim();
        String unfilteredUrl = UrlUtils.fixUrl(mAddress.getText().toString());
        boolean emptyTitle = name.length() == 0;
        boolean emptyUrl = unfilteredUrl.trim().length() == 0;
        Resources r = getResources();
        if (emptyTitle || emptyUrl) {
            if (emptyTitle) {
                mName.setError(r.getText(R.string.website_needs_title));
            }
            if (emptyUrl) {
                mAddress.setError(r.getText(R.string.website_needs_url));
            }
            return false;
        }
        String url = unfilteredUrl.trim();
        try {
            if (!url.toLowerCase().startsWith("javascript:")) {
                URI uriObj = new URI(url);
                String scheme = uriObj.getScheme();
                if (!MyNavigationUtil.urlHasAcceptableScheme(url)) {
                    if (scheme != null) {
                        mAddress.setError(r.getText(R.string.my_navigation_cannot_save_url));
                        return false;
                    }
                    WebAddress address;
                    try {
                        address = new WebAddress(unfilteredUrl);
                    } catch (ParseException e) {
                        throw new URISyntaxException("", "");
                    }
                    if (address.getHost().length() == 0) {
                        throw new URISyntaxException("", "");
                    }
                    url = address.toString();
                } else {
                    String mark = "://";
                    int iRet = -1;
                    if (null != url) {
                        iRet = url.indexOf(mark);
                    }
                    if (iRet > 0 && url.indexOf("/", iRet + mark.length()) < 0) {
                        url = url + "/";
                        Log.d(LOGTAG, "URL=" + url);
                    }
                }
            }
        } catch (URISyntaxException e) {
            mAddress.setError(r.getText(R.string.bookmark_url_not_valid));
            return false;
        }

        // When it is adding, avoid duplicate url that already existing in the
        // database
        if (!mItemUrl.equals(url)) {
            boolean exist = MyNavigationUtil.isMyNavigationUrl(this, url);
            if (exist) {
                mAddress.setError(r.getText(R.string.my_navigation_duplicate_url));
                return false;
            }
        }
        Bundle bundle = new Bundle();
        bundle.putString("title", name);
        bundle.putString("url", url);
        bundle.putString("itemUrl", mItemUrl);
        if (!mItemUrl.equals(url)) {
            bundle.putBoolean("toDefaultThumbnail", true);
        } else {
            bundle.putBoolean("toDefaultThumbnail", false);
        }
        Message msg = Message.obtain(mHandler, SAVE_SITE_NAVIGATION);
        msg.setData(bundle);
        Thread t = new Thread(new SaveMyNavigationRunnable(msg));
        t.start();
        return true;
    }
}
