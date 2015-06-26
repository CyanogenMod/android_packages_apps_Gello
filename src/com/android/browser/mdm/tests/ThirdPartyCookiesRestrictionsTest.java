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
import com.android.browser.mdm.ManagedProfileManager;
import com.android.browser.mdm.ThirdPartyCookiesRestriction;

public class ThirdPartyCookiesRestrictionsTest extends ActivityInstrumentationTestCase2<BrowserActivity>
        implements PreferenceKeys {

    private final static String TAG = "TPCRestrictionsTest";

    private Instrumentation mInstrumentation;
    private BrowserActivity mActivity;
    private ThirdPartyCookiesRestriction mTBCRestriction;

    public ThirdPartyCookiesRestrictionsTest() {
        super(BrowserActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mActivity = getActivity();
        mTBCRestriction = ThirdPartyCookiesRestriction.getInstance();
    }

    public void test_DNT() throws Throwable {
        Log.i(TAG,"!!! ******** Starting TPC Tests *************");

        clearTPCRestrictions();
        assertFalse(mTBCRestriction.isEnabled());
        assertTrue(mTBCRestriction.getValue()); // default is 'allowed'

        setTPCRestrictions(false, true);
        assertFalse(mTBCRestriction.isEnabled());
        assertTrue(mTBCRestriction.getValue());

        setTPCRestrictions(true, false);
        assertTrue(mTBCRestriction.isEnabled());
        assertFalse(mTBCRestriction.getValue());

        setTPCRestrictions(true, true);
        assertTrue(mTBCRestriction.isEnabled());
        assertTrue(mTBCRestriction.getValue());
    }

    /**
     * Activate ThirdPartyCookies restriction
     * @param clear  boolean. if true, clears the restriction by sending an empty bundle. In
     *               this case, the other args are ignored.
     *
     * @param enable boolean. Set the state of the restriction.
     *
     * @param value boolean. Set the state of TPC. true == allowed. If enabled is set to true.
     *              we still bundle it, but it should be ignored by the handler.
     */
    private void setTPCRestrictions(boolean clear, boolean enable, boolean value) {
        // Construct restriction bundle
        final Bundle restrictions = new Bundle();

        if(!clear) {
            restrictions.putBoolean(ThirdPartyCookiesRestriction.TPC_ENABLED,enable);
            restrictions.putBoolean(ThirdPartyCookiesRestriction.TPC_ALLOWED, value);
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

    private void setTPCRestrictions(boolean enable, boolean value) {
        setTPCRestrictions(false, enable, value);
    }

    private void clearTPCRestrictions() {
        setTPCRestrictions(true, false, false);
    }
}
