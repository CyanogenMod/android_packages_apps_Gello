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
import android.util.Log;

import com.android.browser.BrowserActivity;
import com.android.browser.PreferenceKeys;
import com.android.browser.mdm.DoNotTrackRestriction;
import com.android.browser.mdm.ManagedProfileManager;

public class DNTRestrictionsTest extends ActivityInstrumentationTestCase2<BrowserActivity>
        implements PreferenceKeys {

    private final static String TAG = "DNTRestrictionsTest";

    private Instrumentation mInstrumentation;
    private BrowserActivity mActivity;
    private DoNotTrackRestriction mDNTRestriction;

    public DNTRestrictionsTest() {
        super(BrowserActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mActivity = getActivity();
        mDNTRestriction = DoNotTrackRestriction.getInstance();
    }

    // Possible states
    //   DNT_enabled  DNT_value  |  menu-item-enabled      check-box-value
    //   -----------------------------------------------------------------
    //     not set       x       |        Yes              curr-sys-value
    //       0           x       |        Yes              curr-sys-value
    //       1           0       |        No                   0
    //       1           1       |        No                   1
    public void test_DNT() throws Throwable {
        Log.i(TAG,"!!! ******** Starting DNT Tests *************");

        clearDNTRestrictions();
        assertFalse(mDNTRestriction.isEnabled());

        setDNTRestrictions(false, true);
        assertFalse(mDNTRestriction.isEnabled());

        setDNTRestrictions(true, false);
        assertTrue(mDNTRestriction.isEnabled());
        assertFalse(mDNTRestriction.getValue());

        setDNTRestrictions(true, true);
        assertTrue(mDNTRestriction.isEnabled());
        assertTrue(mDNTRestriction.getValue());
    }

    /**
     * Activate DoNotTrack restriction
     * @param clear  boolean. if true, clears the restriction by sending an empty bundle. In
     *               this case, the other args are ignored.
     *
     * @param enable boolean. Set the state of the restriction.
     *
     * @param value boolean. Set the state of Do Not Track if enabled is set to true.
     *              we still bundle it, but it should be ignored by the handler.
     */
    private void setDNTRestrictions(boolean clear, boolean enable, boolean value) {
        // Construct restriction bundle
        final Bundle restrictions = new Bundle();

        if(!clear) {
            restrictions.putBoolean(DoNotTrackRestriction.DO_NOT_TRACK_ENABLED,enable);
            restrictions.putBoolean(DoNotTrackRestriction.DO_NOT_TRACK_VALUE, value);
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

    private void setDNTRestrictions (boolean enable, boolean value) {
        setDNTRestrictions(false, enable, value);
    }

    private void clearDNTRestrictions() {
        setDNTRestrictions(true, false, false);
    }
}
