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

package com.android.browser.mdm.tests;

import android.app.Instrumentation;
import android.database.Cursor;
import android.os.Bundle;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.android.browser.BrowserActivity;
import com.android.browser.PreferenceKeys;
import com.android.browser.mdm.ManagedBookmarksRestriction;
import com.android.browser.mdm.ManagedProfileManager;
import com.android.browser.platformsupport.BrowserContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ManagedBookmarksRestrictionsTest extends ActivityInstrumentationTestCase2<BrowserActivity>
        implements PreferenceKeys {

    private final static String TAG = "BkmrksRestTest";

    private Instrumentation mInstrumentation;
    private BrowserActivity mActivity;
    private ManagedBookmarksRestriction managedBookmarksRestriction;
    private ManagedBookmarksRestriction.BookmarksDb mDb;

    private ArrayList<bmTuple> mBookmarks;
    private String mSubDirName;

    public ManagedBookmarksRestrictionsTest() {
        super(BrowserActivity.class);
    }

    private class bmTuple {
        String name;
        String url;
        public bmTuple(String n, String u) {
            name = n;
            url = u;
        }
        public String getName(){ return name;}
        public String getUrl() { return url;}
    }

    void initializeTestData() {
        mBookmarks = new ArrayList<>();
        // Level 0
        mBookmarks.add(new bmTuple("Chromium for Snapdragon", "www.codeaurora.org/forums/chromium-snapdragon"));
        mBookmarks.add(new bmTuple("Chromium Browser for Snapdragon", "www.codeaurora.org/xwiki/bin/Chromium+for+Snapdragon"));
        mSubDirName = "Repos And Patches";

        // Level 1
        mBookmarks.add(new bmTuple("Code Aurora git repositories", "www.codeaurora.org/cgit/quic/chrome4sdp"));
        mBookmarks.add(new bmTuple("Patches", "www.codeaurora.org/patches/quic/chrome4snapdragon"));
    }

    private String getBookmarksDict(int indent) {
        JSONArray dict = new JSONArray();

        JSONObject bm_0_0 = new JSONObject();
        JSONObject bm_0_1 = new JSONObject();

        JSONObject level_1_Folder = new JSONObject();
        JSONArray level_1_Children = new JSONArray();
        JSONObject bm_1_0 = new JSONObject();
        JSONObject bm_1_1 = new JSONObject();
        try {
            bm_0_0.put("name", mBookmarks.get(0).getName());
            bm_0_0.put("url",  mBookmarks.get(0).getUrl());

            bm_0_1.put("name", mBookmarks.get(1).getName());
            bm_0_1.put("url",  mBookmarks.get(1).getUrl());

            bm_1_0.put("name", mBookmarks.get(2).getName());
            bm_1_0.put("url",  mBookmarks.get(2).getUrl());

            bm_1_1.put("name", mBookmarks.get(3).getName());
            bm_1_1.put("url",  mBookmarks.get(3).getUrl());

            level_1_Children.put(bm_1_0);
            level_1_Children.put(bm_1_1);

            level_1_Folder.put("name", mSubDirName);
            level_1_Folder.put("children", level_1_Children);

            dict.put(bm_0_0);
            dict.put(bm_0_1);
            dict.put(level_1_Folder);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String ret = null;
        try {
            ret = (indent != 0 ? dict.toString(indent): dict.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return ret;
    }
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mActivity = getActivity();
        managedBookmarksRestriction = ManagedBookmarksRestriction.getInstance();
        mDb = managedBookmarksRestriction.mDb;
        initializeTestData();
    }

    public void test_MB() throws Throwable {
        Log.i(TAG,"!!! ******** Starting Managed Bookmark Tests *************");

        clearMBRestrictions();
        assertFalse(managedBookmarksRestriction.isEnabled());
        assertNull(managedBookmarksRestriction.getValue());

        setMBRestrictions(true);
        assertTrue(managedBookmarksRestriction.isEnabled());
        assertEquals(getBookmarksDict(0), managedBookmarksRestriction.getValue());

        // Now check the DB for our bookmark records
        assertTrue(managedBookmarksRestriction.bookmarksWereCreated());
        long rootId = mDb.getMdmRootFolderId();
        assertFalse(rootId == -1);
        assertTrue(mDb.isMdmElement(rootId));

        String[] projections = new String[] {
                BrowserContract.Bookmarks.URL,
                BrowserContract.Bookmarks.IS_FOLDER,
                BrowserContract.Bookmarks.TITLE,
                BrowserContract.Bookmarks._ID,
        };

        long chromeLinksId = -1;  // we need the folder id for the 2nd level checks

        //
        // Check Level 0
        //
        Cursor c = mDb.getChildrenForMdmFolder(rootId, projections);
        int n = c.getCount();
        assertTrue(n == 3);

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            String url   = c.getString(0);
            int isFolder = c.getInt(1);
            String title = c.getString(2);
            long id = c.getLong(3);

            assertTrue(mDb.isMdmElement(id));

            boolean found = false;
            for (bmTuple t : mBookmarks){
                if (t.getName().equals(title)) {
                    found = true;
                    assertEquals(t.getUrl(), url);
                    assertEquals(0, isFolder);
                }
            }

            if (title.equals(mSubDirName)) {
                assertEquals("MDM", url);
                assertEquals(1, isFolder);
                chromeLinksId = id;
                found = true;
            }
            if (!found) {
                assertFalse("Unexpected entry ["+title+"] found", true);
            }
        }
        //
        // Check Level 1
        //
        c = mDb.getChildrenForMdmFolder(chromeLinksId, projections);
        n = c.getCount();
        assertTrue(n == 2);

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            String url   = c.getString(0);
            int isFolder = c.getInt(1);
            String title = c.getString(2);
            long id = c.getLong(3);

            assertTrue(mDb.isMdmElement(id));

            boolean found = false;
            for (bmTuple t : mBookmarks){
                if (t.getName().equals(title)) {
                    found = true;
                    assertEquals(t.getUrl(), url);
                    assertEquals(0, isFolder);
                }
            }
            if (!found) {
                assertFalse("Unexpected entry ["+title+"] found", true);
            }
        }

        //
        // attempt to add bookmarks again.  Should fail silently.
        // catt bookmarksWereCreated() to  see if they were actually created or not
        //
        setMBRestrictions(true);
        assertFalse(managedBookmarksRestriction.bookmarksWereCreated());

        //
        // Now, clear the restriction and check that there is no root mdm folder in the DB
        //
        clearMBRestrictions();
        assertFalse(managedBookmarksRestriction.isEnabled());
        assertNull(managedBookmarksRestriction.getValue());
        rootId = mDb.getMdmRootFolderId();
        assertTrue(rootId == -1);
    }

    /**
     * Activate ManagedBookmarks restriction
     * @param enable boolean. Set the state of the restriction.
     */
    private void setMBRestrictions(boolean enable) {
        // Construct restriction bundle
        final Bundle restrictions = new Bundle();

        if(enable) {
            restrictions.putString(ManagedBookmarksRestriction.MANAGED_BOOKMARKS, getBookmarksDict(0));
        }

        // Deliver restriction on UI thread
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ManagedProfileManager.getInstance().setMdmRestrictions(restrictions);
            }
        });

        // Wait to ensure restriction is set
        mInstrumentation.waitForIdleSync();
    }

    private void clearMBRestrictions() {
        setMBRestrictions(false);
    }
}
