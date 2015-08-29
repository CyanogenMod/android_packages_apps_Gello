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
import android.os.Bundle;
import android.test.ActivityInstrumentationTestCase2;

import com.android.browser.BrowserActivity;
import com.android.browser.mdm.DownloadDirRestriction;
import com.android.browser.mdm.ManagedProfileManager;

public class DownloadDirRestrictionsTest extends ActivityInstrumentationTestCase2<BrowserActivity> {

    // private final static String TAG = "IncognitoRestrictionsTest";

    private Instrumentation mInstrumentation;
    private BrowserActivity mActivity;
    private DownloadDirRestriction mDDirRestriction;

    public DownloadDirRestrictionsTest() {
        super(BrowserActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mActivity = getActivity();
        mDDirRestriction = DownloadDirRestriction.getInstance();
    }

    public void test_DownloadDirRestriction() throws Throwable {
        // Possible states
        // Enabled  Downloads  Download | allowed	dir
        //          Allowed     Dir     |
        // -----------------------------+-------------------
        // not set     n       default  |    y     default
        // not set     n       /fubar   |    y     default
        // not set     y       default  |    y     default
        // not set     y       /fubar   |    y     default
        //   No        n       default  |    y     default
        //   No        n       /fubar   |    y     default
        //   No        y       default  |    y     default
        //   No        y       /fubar   |    y     default
        //   Yes       n       default  |    n     default (Dir wouldn't be noticed since the dialog won't be seen)
        //   Yes       n       /fubar   |    n     /fubar  (Dir wouldn't be noticed since the dialog won't be seen)
        //   Yes       y       default  |    y     default
        //   Yes       y       /fubar   |    y     /fubar


        // Initial conditions: no restrictions at all
        clearAllRestrictions();
        assertFalse(mDDirRestriction.isEnabled());
        assertEquals(DownloadDirRestriction.defaultDownloadsAllowed, mDDirRestriction.downloadsAllowed());
        assertEquals(DownloadDirRestriction.defaultDownloadDir, mDDirRestriction.getDownloadDirectory());

        // Enable not set
        setDDirRestrictions("NotSet", false, DownloadDirRestriction.defaultDownloadDir);
        assertFalse(mDDirRestriction.isEnabled());
        assertEquals(DownloadDirRestriction.defaultDownloadsAllowed, mDDirRestriction.downloadsAllowed());
        assertEquals(DownloadDirRestriction.defaultDownloadDir, mDDirRestriction.getDownloadDirectory());

        setDDirRestrictions("NotSet", false, "/fubar");
        assertFalse(mDDirRestriction.isEnabled());
        assertEquals(DownloadDirRestriction.defaultDownloadsAllowed, mDDirRestriction.downloadsAllowed());
        assertEquals(DownloadDirRestriction.defaultDownloadDir, mDDirRestriction.getDownloadDirectory());

        setDDirRestrictions("NotSet", true, DownloadDirRestriction.defaultDownloadDir);
        assertFalse(mDDirRestriction.isEnabled());
        assertEquals(DownloadDirRestriction.defaultDownloadsAllowed, mDDirRestriction.downloadsAllowed());
        assertEquals(DownloadDirRestriction.defaultDownloadDir, mDDirRestriction.getDownloadDirectory());

        setDDirRestrictions("NotSet", true, "/fubar");
        assertFalse(mDDirRestriction.isEnabled());
        assertEquals(DownloadDirRestriction.defaultDownloadsAllowed, mDDirRestriction.downloadsAllowed());
        assertEquals(DownloadDirRestriction.defaultDownloadDir, mDDirRestriction.getDownloadDirectory());

        // Enable is false
        setDDirRestrictions("false", false, DownloadDirRestriction.defaultDownloadDir);
        assertFalse(mDDirRestriction.isEnabled());
        assertEquals(DownloadDirRestriction.defaultDownloadsAllowed, mDDirRestriction.downloadsAllowed());
        assertEquals(DownloadDirRestriction.defaultDownloadDir, mDDirRestriction.getDownloadDirectory());

        setDDirRestrictions("false", false, "/fubar");
        assertFalse(mDDirRestriction.isEnabled());
        assertEquals(DownloadDirRestriction.defaultDownloadsAllowed, mDDirRestriction.downloadsAllowed());
        assertEquals(DownloadDirRestriction.defaultDownloadDir, mDDirRestriction.getDownloadDirectory());

        setDDirRestrictions("false", true, DownloadDirRestriction.defaultDownloadDir);
        assertFalse(mDDirRestriction.isEnabled());
        assertEquals(DownloadDirRestriction.defaultDownloadsAllowed, mDDirRestriction.downloadsAllowed());
        assertEquals(DownloadDirRestriction.defaultDownloadDir, mDDirRestriction.getDownloadDirectory());

        setDDirRestrictions("false", true, "/fubar");
        assertFalse(mDDirRestriction.isEnabled());
        assertEquals(DownloadDirRestriction.defaultDownloadsAllowed, mDDirRestriction.downloadsAllowed());
        assertEquals(DownloadDirRestriction.defaultDownloadDir, mDDirRestriction.getDownloadDirectory());

        // Enable is True
        setDDirRestrictions("true", false, DownloadDirRestriction.defaultDownloadDir);
        assertTrue(mDDirRestriction.isEnabled());
        assertFalse(mDDirRestriction.downloadsAllowed());
        assertEquals(DownloadDirRestriction.defaultDownloadDir, mDDirRestriction.getDownloadDirectory());

        setDDirRestrictions("true", false, "/fubar");
        assertTrue(mDDirRestriction.isEnabled());
        assertFalse(mDDirRestriction.downloadsAllowed());
        assertEquals("/fubar", mDDirRestriction.getDownloadDirectory());

        setDDirRestrictions("true", true, DownloadDirRestriction.defaultDownloadDir);
        assertTrue(mDDirRestriction.isEnabled());
        assertTrue(mDDirRestriction.downloadsAllowed());
        assertEquals(DownloadDirRestriction.defaultDownloadDir, mDDirRestriction.getDownloadDirectory());

        setDDirRestrictions("true", true, "/fubar");
        assertTrue(mDDirRestriction.isEnabled());
        assertTrue(mDDirRestriction.downloadsAllowed());
        assertEquals("/fubar", mDDirRestriction.getDownloadDirectory());
    }

    /**
    * Activate Download Directory restriction
    * @param enabled  Required. "NotSet" | "true" | "false"
    * @param allow determines if downloads are allowed
    * @param dir override download dir
    *
    */
    private void setDDirRestrictions(String enabled, boolean allow, String dir) {
        final Bundle restrictions = new Bundle();

        if (!enabled.equals("NotSet")) {
            if (enabled.equals("true")) {
                restrictions.putBoolean(DownloadDirRestriction.RESTRICTION_ENABLED, true);
            } else if (enabled.equals("false")) {
                restrictions.putBoolean(DownloadDirRestriction.RESTRICTION_ENABLED, false);
            }
        }

        restrictions.putBoolean(DownloadDirRestriction.DOWNLOADS_ALLOWED, allow);
        restrictions.putString(DownloadDirRestriction.DOWNLOADS_DIR, dir);

        sendRestriction(restrictions);
    }

    private void clearAllRestrictions() {
        sendRestriction(new Bundle());
    }

    private void sendRestriction(final Bundle restrictions) {
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
}
