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

package com.android.browser.provider;

import android.content.Context;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebResourceResponse;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import com.android.browser.BrowserSettings;
import com.android.browser.R;
import com.android.browser.homepages.RequestHandler;
import com.android.browser.mynavigation.MyNavigationRequestHandler;
import com.android.browser.mynavigation.MyNavigationUtil;
import com.android.browser.provider.BrowserProvider2;

public class MyNavigationProvider extends ContentProvider {

    private static final String LOGTAG = "MyNavigationProvider";
    private static final String TABLE_WEB_SITES = "websites";
    private static final int WEB_SITES_ALL = 0;
    private static final int WEB_SITES_ID = 1;
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        URI_MATCHER.addURI(MyNavigationUtil.AUTHORITY, "websites", WEB_SITES_ALL);
        URI_MATCHER.addURI(MyNavigationUtil.AUTHORITY, "websites/#", WEB_SITES_ID);
    }
    private static final Uri NOTIFICATION_URI = MyNavigationUtil.MY_NAVIGATION_URI;

    private SiteNavigationDatabaseHelper mOpenHelper;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Current not used, just return 0
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // Current not used, just return null
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // Current not used, just return null
        return null;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new SiteNavigationDatabaseHelper(this.getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(TABLE_WEB_SITES);
        switch (URI_MATCHER.match(uri)) {
            case WEB_SITES_ALL:
                break;
            case WEB_SITES_ID:
                qb.appendWhere(MyNavigationUtil.ID + "=" + uri.getPathSegments().get(0));
                break;
            default:
                Log.e(LOGTAG, "query Unknown URI: " + uri);
                return null;
        }

        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = null;
        } else {
            orderBy = sortOrder;
        }
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
        }
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = 0;

        switch (URI_MATCHER.match(uri)) {
            case WEB_SITES_ALL:
                count = db.update(TABLE_WEB_SITES, values, selection, selectionArgs);
                break;
            case WEB_SITES_ID:
                String newIdSelection = MyNavigationUtil.ID + "=" + uri.getLastPathSegment()
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : "");
                count = db.update(TABLE_WEB_SITES, values, newIdSelection, selectionArgs);
                break;
            default:
                Log.e(LOGTAG, "update Unknown URI: " + uri);
                return count;
        }

        if (count > 0) {
            ContentResolver cr = getContext().getContentResolver();
            cr.notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) {
        try {
            ParcelFileDescriptor[] pipes = ParcelFileDescriptor.createPipe();
            final ParcelFileDescriptor write = pipes[1];
            AssetFileDescriptor afd = new AssetFileDescriptor(write, 0, -1);
            new MyNavigationRequestHandler(getContext(), uri, afd.createOutputStream())
                    .start();
            return pipes[0];
        } catch (IOException e) {
            Log.e(LOGTAG, "Failed to handle request: " + uri, e);
            return null;
        }
    }

    public static WebResourceResponse shouldInterceptRequest(Context context,
            String url) {
        try {
            if (MyNavigationUtil.MY_NAVIGATION.equals(url)) {
                Uri uri = Uri.parse(url);
                if (MyNavigationUtil.AUTHORITY.equals(uri.getAuthority())) {
                    InputStream ins = context.getContentResolver()
                            .openInputStream(uri);
                    return new WebResourceResponse("text/html", "utf-8", ins);
                }
            }
            boolean listFiles = BrowserSettings.getInstance().isDebugEnabled();
            if (listFiles && interceptFile(url)) {
                PipedInputStream ins = new PipedInputStream();
                PipedOutputStream outs = new PipedOutputStream(ins);
                new RequestHandler(context, Uri.parse(url), outs).start();
                return new WebResourceResponse("text/html", "utf-8", ins);
            }
        } catch (Exception e) {}
        return null;
    }

    private static boolean interceptFile(String url) {
        if (!url.startsWith("file:///")) {
            return false;
        }
        String fpath = url.substring(7);
        File f = new File(fpath);
        if (!f.isDirectory()) {
            return false;
        }
        return true;
    }

    private class SiteNavigationDatabaseHelper extends SQLiteOpenHelper {
        private Context mContext;
        static final String DATABASE_NAME = "mynavigation.db";

        public SiteNavigationDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, 1); // "1" is the db version here
            // TODO Auto-generated constructor stub
            mContext = context;
        }

        public SiteNavigationDatabaseHelper(Context context, String name,
                CursorFactory factory, int version) {
            super(context, name, factory, version);
            // TODO Auto-generated constructor stub
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // TODO Auto-generated method stub
            createWebsitesTable(db);
            initWebsitesTable(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
            // TODO Auto-generated method stub
        }

        private void createWebsitesTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE websites (" +
                    MyNavigationUtil.ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    MyNavigationUtil.URL + " TEXT," +
                    MyNavigationUtil.TITLE + " TEXT," +
                    MyNavigationUtil.DATE_CREATED + " LONG," +
                    MyNavigationUtil.WEBSITE + " INTEGER," +
                    MyNavigationUtil.THUMBNAIL + " BLOB DEFAULT NULL," +
                    MyNavigationUtil.FAVICON + " BLOB DEFAULT NULL," +
                    MyNavigationUtil.DEFAULT_THUMB + " TEXT" +
                    ");");
        }

        // initial table , insert websites to table websites
        private void initWebsitesTable(SQLiteDatabase db) {
            int WebsiteNumber = MyNavigationUtil.WEBSITE_NUMBER;
            for (int i = 0; i < WebsiteNumber; i++) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                Bitmap bm = BitmapFactory.decodeResource(mContext.getResources(),
                        R.raw.my_navigation_add);
                bm.compress(Bitmap.CompressFormat.PNG, 100, os);
                ContentValues values = new ContentValues();
                values.put(MyNavigationUtil.URL, "ae://" + (i + 1) + "add-fav");
                values.put(MyNavigationUtil.TITLE, "");
                values.put(MyNavigationUtil.DATE_CREATED, 0 + "");
                values.put(MyNavigationUtil.WEBSITE, 1 + "");
                values.put(MyNavigationUtil.THUMBNAIL, os.toByteArray());
                db.insertOrThrow(TABLE_WEB_SITES, MyNavigationUtil.URL, values);
            }
        }
    }
}
