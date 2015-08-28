/*
 * Copyright (c) 2015, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution.
 * * Neither the name of The Linux Foundation nor the names of its
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.android.browser.mdm;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.android.browser.platformsupport.BrowserContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static org.chromium.base.ApplicationStatus.getApplicationContext;

public class ManagedBookmarksRestriction extends Restriction {

    private final static String TAG = "+++MngdBookmarks_Rest";

    public static final String MANAGED_BOOKMARKS = "ManagedBookmarks";
    private static final String FOLDER_URL_KEY = "MDM";
    private static ManagedBookmarksRestriction sInstance;
    private String mJsonBookmarks;
    public BookmarksDb mDb;
    private boolean mCreatedMdmBookmarks;

    private ManagedBookmarksRestriction() {
        super(TAG);
    }

    public static ManagedBookmarksRestriction getInstance() {
        synchronized (ManagedBookmarksRestriction.class) {
            if (sInstance == null) {
                sInstance = new ManagedBookmarksRestriction();
            }
        }
        return sInstance;
    }

    @Override
    protected void doCustomInit() {
        mDb = new BookmarksDb();
        mCreatedMdmBookmarks = false;
    }

    public class BookmarksDb {
        private ContentResolver mCr = null;

        public class CRException extends Exception {
            public CRException(String s) {
                super(s);
            }
        }
        private ContentResolver cr() throws CRException {
            if (mCr == null) {
                mCr = getApplicationContext().getContentResolver();
            }
            if (mCr == null) {
                throw new CRException("Null ContentResolver");
            }
            return mCr;
        }

        public Cursor queryById(long id, String[] projections) {
            String where =  BrowserContract.Bookmarks._ID + " = " + id;
            Cursor c = null;
            try {
                c = cr().query(BrowserContract.Bookmarks.CONTENT_URI,
                        projections,  // projections... the columns we want. null means all
                        where,        // where clause (without the WHERE keyword)
                        null,         // selectionArgs
                        null);        // sortOrder
            } catch (android.database.sqlite.SQLiteException e) {
                Log.e(TAG, "queryById SQL Exception: [" + e.toString() + "]");
            } catch (CRException e) {
                Log.e(TAG, "queryById CR Exception: [" + e.toString() + "]");
            }
            return c;
        }

        public void deleteItemById(long id) {
            String where =  BrowserContract.Bookmarks._ID + " = " + id;
            try {
                cr().delete(BrowserContract.Bookmarks.CONTENT_URI, where, null);
            }
            catch (android.database.sqlite.SQLiteException e) {
                Log.e(TAG, "deleteItemById Exception: [" + e.toString() + "]");
            } catch (CRException e) {
                Log.e(TAG, "deleteItemById CR Exception: [" + e.toString() + "]");
            }
        }

        public long getMdmRootFolderId() {
            long result = -1;
            String projections[] = new String[] {BrowserContract.Bookmarks._ID};
            String where =  BrowserContract.Bookmarks.TITLE     + " = 'Managed' AND " +
                            BrowserContract.Bookmarks.IS_FOLDER + " = 1         AND " +
                            BrowserContract.Bookmarks.URL       + " like '" + FOLDER_URL_KEY + "%' AND " +
                            BrowserContract.Bookmarks.PARENT    + " = 1";
            try {
                Cursor c = cr().query(BrowserContract.Bookmarks.CONTENT_URI,
                        projections,  // projections... the columns we want. null means all
                        where,        // where clause (without the WHERE keyword)
                        null,         // selectionArgs
                        null);        // sortOrder
                if (c.getCount() != 0) {
                    c.moveToFirst();
                    result = c.getLong(0);
                }
                c.close();
            } catch (android.database.sqlite.SQLiteException e) {
                Log.e(TAG, "getMdmRootFolderId SQL Exception: [" + e.toString() + "]");
            } catch (CRException e) {
                Log.e(TAG, "getMdmRootFolderId CR Exception: [" + e.toString() + "]");
            }
            return result;
        }

        public Cursor getChildrenForMdmFolder(long id, String [] projections) {
            String where =  BrowserContract.Bookmarks.PARENT + " = " + id;
            Cursor c = null;
            try {
                c = cr().query(BrowserContract.Bookmarks.CONTENT_URI,
                        projections,  // projections... the columns we want. null means all
                        where,        // where clause (without the WHERE keyword)
                        null,         // selectionArgs
                        null);        // sortOrder
            } catch (android.database.sqlite.SQLiteException e) {
                Log.e(TAG, "getChildrenForMdmFolder SQL Exception: [" + e.toString() + "]");
            } catch (CRException e) {
                Log.e(TAG, "getChildrenForMdmFolder CR Exception: [" + e.toString() + "]");
            }
            return c;
        }

        private void addBookmark(String title, long parent, String url) {
            ContentValues values = new ContentValues();
            values.put(BrowserContract.Bookmarks.PARENT, parent);
            values.put(BrowserContract.Bookmarks.TITLE, title);
            values.put(BrowserContract.Bookmarks.URL, url);

            try {
                Uri uri = cr().insert(BrowserContract.Bookmarks.CONTENT_URI, values);
                if (uri == null) {
                    Log.e(TAG, "Bookmark '" + title + "' creation failed.");
                }
            } catch (android.database.sqlite.SQLiteException e) {
                Log.e(TAG, "addBookmark-SQL Exception during creation: [" + e.toString() + "]");
            } catch (CRException e) {
                Log.e(TAG, "addBookmark CR Exception: [" + e.toString() + "]");
            }
        }

        private boolean bookmarksAlreadyEnabled(int hash) {
            boolean ret = false;
            String incomingHash = String.valueOf(hash);
            long rootId = getMdmRootFolderId();
            if (rootId != -1) {
                Cursor c = queryById(rootId, new String[] {BrowserContract.Bookmarks.URL});
                if (c.getCount() == 1) {
                    c.moveToFirst();
                    String url = c.getString(0);
                    if (url.contains(incomingHash)) {
                        ret = true;
                    }
                }
                c.close();
            }
            return ret;
        }

        private void addFolder(String title, long parent, JSONArray children, int hash) {
            ContentValues values = new ContentValues();
            values.put(BrowserContract.Bookmarks.PARENT, parent);
            values.put(BrowserContract.Bookmarks.TITLE, title);
            values.put(BrowserContract.Bookmarks.IS_FOLDER, 1);

            // We are using the URL field (normally not used for folders) to
            // lock down that this folder is managed by Mdm.  This is certainly
            // non-standard, but the alternative would be to modify the database schema,
            // which would become a maintenance headache later when google updates the schema.
            if (hash != 0) {
                // We add the hash of the json string to the root folder. We use this
                // to check if we already have this bookmark set enabled.
                values.put(BrowserContract.Bookmarks.URL, FOLDER_URL_KEY + ":" + hash);
            }
            else {
                values.put(BrowserContract.Bookmarks.URL, FOLDER_URL_KEY);
            }

            try {
                Uri uri = cr().insert(BrowserContract.Bookmarks.CONTENT_URI, values);
                if (uri != null) {
                    long nodeId = ContentUris.parseId(uri);

                    if (children != null) {
                        for (int i = 0; i < children.length(); i++) {
                            JSONObject j = children.getJSONObject(i);

                            // if it has a URL, then it's a bookmark
                            if (j.has("url")) {
                                String t = j.getString("name");
                                String u = j.getString("url");
                                mDb.addBookmark(t, nodeId, u);
                            }
                            // if it has children, then it's a subfolder
                            else if (j.has("children")) {
                                String t = j.getString("name");
                                JSONArray ja = new JSONArray(j.getString("children"));
                                addFolder(t, nodeId, ja, 0);
                            } else {
                                Log.e(TAG, "Parse error processing children for [" + title + "]");
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "Folder creation failed.");
                }
            } catch (android.database.sqlite.SQLiteException e) {
                Log.e(TAG, "addFolder-SQL Exception during creation: [" + e.toString() + "]");
            } catch (JSONException e) {
                Log.e(TAG, "addFolder-JSON exception during creation: [" + e.toString() + "]");
            } catch (CRException e) {
                Log.e(TAG, "addFolder CR Exception: [" + e.toString() + "]");
            }
        }

        public boolean isMdmElement(long id) {
            boolean ret = false;

            String[] projections =
                    new String[]{
                            BrowserContract.Bookmarks.IS_FOLDER,
                            BrowserContract.Bookmarks.URL,
                            BrowserContract.Bookmarks.PARENT};

            Cursor c = mDb.queryById(id, projections);
            if(1 == c.getCount()) {
                c.moveToFirst();
                int isFolder = c.getInt(0);
                String url = c.getString(1);
                long parent = c.getLong(2);

                if (isFolder != 0) {
                    if (url != null && url.startsWith(FOLDER_URL_KEY)) {
                        ret = true;
                    }
                }
                else {
                    Cursor cp = mDb.queryById(parent, projections);
                    if(1 == cp.getCount()) {
                        cp.moveToFirst();
                        String u2 = cp.getString(1);
                        if (u2 != null && u2.startsWith(FOLDER_URL_KEY)) {
                            ret = true;
                        }
                    }
                    else {
                        Log.e(TAG,"isMdmElement: Invalid parent id ["+id+"]");
                    }
                }
            }
            else {
                Log.e(TAG,"isMdmElement: Invalid id ["+id+"]");
            }
            c.close();
            return ret;
        }

        private void deleteTree(long folderId) {
            if (folderId == -1) {
                Log.i(TAG, " deleteTree: no tree to delete.");
                return;
            }

            String[] projections =
                    new String[]{
                            BrowserContract.Bookmarks.IS_FOLDER,
                            BrowserContract.Bookmarks._ID};

            try {
                Cursor c = getChildrenForMdmFolder(folderId, projections);

                int n = c.getCount();
                if (n != 0) {
                    c.moveToFirst();
                    for (int i = 0; i < n; i++) {
                        int isFolder = c.getInt(0);
                        int itemId   = c.getInt(1);

                        if (isFolder == 1) {
                            deleteTree(itemId);
                        }
                        else {
                            mDb.deleteItemById(itemId);
                        }
                        c.moveToNext();
                    }
                }
                else {
                    Log.i(TAG,"DeleteTree: no children for id["+folderId+"]");
                }
                c.close();

                // now that the contents have been deleted, delete this folder
                mDb.deleteItemById(folderId);
            } catch (android.database.sqlite.SQLiteException e) {
                Log.e(TAG, "deleteTree SQL Exception: [" + e.toString() + "]");
            }
        }
    }

    /* For Debugging
    public void dumpCursor(Cursor c) {
        int n = c.getCount();
        int pos = c.getPosition();

        if (n != 0) {
            String cols[] = c.getColumnNames();
            Log.i(TAG, " ********* Dumping " + n + " records *************");
            c.moveToFirst();
            for (int i = 0; i < n; i++) {
                Log.i(TAG,"  == Record ["+i+"]");
                for (String col : cols) {
                    int ndx = c.getColumnIndex(col);
                    switch (c.getType(ndx)) {
                        case Cursor.FIELD_TYPE_NULL:
                            Log.i(TAG, "      " + col + " = <null>");
                            break;
                        case Cursor.FIELD_TYPE_BLOB:
                            Log.i(TAG, "      " + col + " = <blob>");
                            break;
                        case Cursor.FIELD_TYPE_FLOAT:
                            Log.i(TAG, "      " + col + " = " +
                                    c.getFloat(ndx));
                            break;
                        case Cursor.FIELD_TYPE_INTEGER:
                            Log.i(TAG, "      " + col + " = " +
                                    c.getInt(ndx));
                            break;
                        case Cursor.FIELD_TYPE_STRING:
                            Log.i(TAG, "      " + col + " = " +
                                    c.getString(ndx));
                            break;
                    }
                }
                c.moveToNext();
            }
            Log.i(TAG, " ********* END Dump *************");
        }
        c.moveToPosition(pos); // restore incoming position
    } */

    private void removeManagedBookmarks() {
        mDb.deleteTree(mDb.getMdmRootFolderId());
    }

    public boolean bookmarksWereCreated() {
        return mCreatedMdmBookmarks;
    }

    private void addManagedBookmarks() {
        String name = "Managed";
        int rootFolder = 1;
        mCreatedMdmBookmarks = false;

        int hash = mJsonBookmarks.hashCode();
        if (! mDb.bookmarksAlreadyEnabled(hash)) {
            Log.i(TAG, ">>>>>>> BOOKMARKS NOT ALREADY ENABLED <<<<<<<<<<<<");
            removeManagedBookmarks();
            JSONArray dict = null;
            try {
                dict = new JSONArray(mJsonBookmarks);
            } catch (JSONException e) {
                Log.w(TAG, "addManagedBookmarks: Incoming JSON didn't parse. Creating empty folder." + e.toString());
            }

            mDb.addFolder(name, rootFolder, dict, hash);

            mCreatedMdmBookmarks = true;
        }
        else {
            Log.w(TAG, ">>>>>>> BOOKMARKS ALREADY ENABLED <<<<<<<<<<<<");
            mCreatedMdmBookmarks = false;
        }
    }

    @Override
    public void enforce(Bundle restrictions) {
        mJsonBookmarks = restrictions.getString(MANAGED_BOOKMARKS);

        // Enable if we got something in the json bookmarks string
        enable(!(mJsonBookmarks == null || mJsonBookmarks.isEmpty()));

        Log.i(TAG, "Enforcing. enabled[" + isEnabled() + "]. val[" + mJsonBookmarks + "]");

        if(isEnabled()){
            addManagedBookmarks();
        }
        else {
            removeManagedBookmarks();
        }
    }

    public String getValue() {
        return mJsonBookmarks;
    }
}
