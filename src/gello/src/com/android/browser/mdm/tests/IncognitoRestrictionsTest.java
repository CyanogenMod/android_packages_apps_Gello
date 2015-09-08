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
import com.android.browser.mdm.IncognitoRestriction;
import com.android.browser.mdm.ManagedProfileManager;

public class IncognitoRestrictionsTest extends ActivityInstrumentationTestCase2<BrowserActivity> {

    private final static String TAG = "IncognitoRestrictionsTest";

    private Instrumentation mInstrumentation;
    private BrowserActivity mActivity;
    private IncognitoRestriction mIncognitoRestriction;

    public IncognitoRestrictionsTest() {
        super(BrowserActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mActivity = getActivity();
        mIncognitoRestriction = IncognitoRestriction.getInstance();
    }

    public void test_IncognitoRestriction() throws Throwable {
        // Possible states
        //  enabled  | button enabled
        //  ---------------------------------
        //  not set  |   Yes
        //  true     |   No
        //  false    |   Yes

        clearIncognitoRestrictions();
        assertFalse(mIncognitoRestriction.isEnabled());
        assertEquals((float) 1.0, mIncognitoRestriction.getButtonAlpha());

        setIncognitoRestrictions(true);
        assertTrue(mIncognitoRestriction.isEnabled());
        assertEquals((float) 0.5, mIncognitoRestriction.getButtonAlpha());

        setIncognitoRestrictions(false);
        assertFalse(mIncognitoRestriction.isEnabled());
        assertEquals((float) 1.0, mIncognitoRestriction.getButtonAlpha());
    }

    /**
    * Activate Incognito restriction
     * @param clear    if true, sends an empty bundle
     * @param enabled  Required. true or false
    *
    */
    private void setIncognitoRestrictions(boolean clear, boolean enabled) {
        final Bundle restrictions = new Bundle();

        if (!clear) {
            restrictions.putBoolean(IncognitoRestriction.INCOGNITO_RESTRICTION_ENABLED, enabled);
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

    private void clearIncognitoRestrictions() {
        setIncognitoRestrictions(true, false);
    }

    private void setIncognitoRestrictions(boolean enabled) {
        setIncognitoRestrictions(false, enabled);
    }

}
