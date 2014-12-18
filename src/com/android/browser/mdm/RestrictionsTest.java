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

import android.app.Instrumentation;
import android.content.Context;
import android.os.Bundle;
import android.test.ActivityInstrumentationTestCase2;

import com.android.browser.BrowserActivity;
import com.android.browser.BrowserSettings;
import com.android.browser.PreferenceKeys;
import com.android.browser.R;

public class RestrictionsTest extends ActivityInstrumentationTestCase2<BrowserActivity>
        implements PreferenceKeys {

    private final static String TAG = "RestrictionsTest";
    private final static String VALID_SEARCH_ENGINE_NAME_1 = "netsprint";
    private final static String VALID_SEARCH_ENGINE_NAME_2 = "naver";
    //Search engine name that does not match an entry in res/values/all_search_engines.xml
    private final static String INVALID_SEARCH_ENGINE_NAME = "foo";

    private Instrumentation mInstrumentation;
    private Context mContext;
    private BrowserActivity mActivity;

    public RestrictionsTest() {
        super(BrowserActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mContext = getInstrumentation().getTargetContext();
        mActivity = getActivity();
    }

    public void testSetDefaultSearchProvider() throws Throwable {
        SearchEngineRestriction searchEngineRestriction = SearchEngineRestriction.getInstance();

        // Ensure we start with the default search engine and no restriction
        String defaultSearchEngineName = mActivity.getApplicationContext()
                .getString(R.string.default_search_engine_value);
        assertFalse("Search engine restriction", searchEngineRestriction.isEnabled());
        assertEquals("Search provider", defaultSearchEngineName,
                BrowserSettings.getInstance().getSearchEngineName());

        // Restriction is not set when DefaultSearchProviderEnabled is not present
        setDefaultSearchProvider(null, VALID_SEARCH_ENGINE_NAME_1);
        assertFalse("Search engine restriction", searchEngineRestriction.isEnabled());
        assertEquals("Search provider", defaultSearchEngineName,
                BrowserSettings.getInstance().getSearchEngineName());

        // Restriction is not set when DefaultSearchProviderEnabled is FALSE
        setDefaultSearchProvider(false, VALID_SEARCH_ENGINE_NAME_1);
        assertFalse("Search engine restriction", searchEngineRestriction.isEnabled());
        assertEquals("Search provider", defaultSearchEngineName,
                BrowserSettings.getInstance().getSearchEngineName());

        // Restriction is not set when DefaultSearchProviderName is not present
        setDefaultSearchProvider(true, null);
        assertFalse("Search engine restriction", searchEngineRestriction.isEnabled());
        assertEquals("Search provider", defaultSearchEngineName,
                BrowserSettings.getInstance().getSearchEngineName());

        // Restriction is not set when DefaultSearchProviderName is INVALID
        setDefaultSearchProvider(true, INVALID_SEARCH_ENGINE_NAME);
        assertFalse("Search engine restriction", searchEngineRestriction.isEnabled());
        assertEquals("Search provider", defaultSearchEngineName,
                BrowserSettings.getInstance().getSearchEngineName());

        // Restriction is set when DefaultSearchProviderEnabled is TRUE and
        // DefaultSearchProviderName is VALID
        setDefaultSearchProvider(true, VALID_SEARCH_ENGINE_NAME_1);
        assertTrue("Search engine restriction", searchEngineRestriction.isEnabled());
        assertEquals("Search provider", VALID_SEARCH_ENGINE_NAME_1,
                BrowserSettings.getInstance().getSearchEngineName());

        setDefaultSearchProvider(true, VALID_SEARCH_ENGINE_NAME_2);
        assertTrue("Search engine restriction", searchEngineRestriction.isEnabled());
        assertEquals("Search provider", VALID_SEARCH_ENGINE_NAME_2,
                BrowserSettings.getInstance().getSearchEngineName());

        // Restriction is lifted when neither DefaultSearchProviderEnabled nor
        // DefaultSearchProviderName are present
        setDefaultSearchProvider(null, null);
        assertFalse("Search engine restriction", searchEngineRestriction.isEnabled());
        assertEquals("Search provider", defaultSearchEngineName,
                BrowserSettings.getInstance().getSearchEngineName());

        // Restriction is lifted when DefaultSearchProviderEnabled is FALSE

        // first set a valid search engine restriction
        setDefaultSearchProvider(true, VALID_SEARCH_ENGINE_NAME_1);
        assertTrue("Search engine restriction", searchEngineRestriction.isEnabled());
        assertEquals("Search provider", VALID_SEARCH_ENGINE_NAME_1,
                BrowserSettings.getInstance().getSearchEngineName());
        // then lift the restriction
        setDefaultSearchProvider(false, VALID_SEARCH_ENGINE_NAME_2);
        assertFalse("Search engine restriction", searchEngineRestriction.isEnabled());
        assertEquals("Search provider", defaultSearchEngineName,
                BrowserSettings.getInstance().getSearchEngineName());

        // Restriction is lifted when DefaultSearchProviderEnabled is TRUE and
        // DefaultSearchProviderName is INVALID

        // first set a valid search engine restriction
        setDefaultSearchProvider(true, VALID_SEARCH_ENGINE_NAME_1);
        assertTrue("Search engine restriction", searchEngineRestriction.isEnabled());
        assertEquals("Search provider", VALID_SEARCH_ENGINE_NAME_1,
                BrowserSettings.getInstance().getSearchEngineName());
        // then lift the restriction
        setDefaultSearchProvider(true, INVALID_SEARCH_ENGINE_NAME);
        assertFalse("Search engine restriction", searchEngineRestriction.isEnabled());
        assertEquals("Search provider", defaultSearchEngineName,
                BrowserSettings.getInstance().getSearchEngineName());
    }

    /**
     * Activate search engine restriction
     * @param defaultSearchProviderEnabled must be true to activate restriction
     * @param defaultSearchProviderName must be an entry in res/values/all_search_engines.xml,
     *                                  otherwise restriction is not set
     */
    private void setDefaultSearchProvider(Boolean defaultSearchProviderEnabled,
                                          String defaultSearchProviderName) {
        // Construct restriction bundle
        final Bundle restrictions = new Bundle();
        if (defaultSearchProviderEnabled != null)
            restrictions.putBoolean("DefaultSearchProviderEnabled", defaultSearchProviderEnabled);
        if (defaultSearchProviderName != null)
            restrictions.putString("SearchProviderName", defaultSearchProviderName);

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
