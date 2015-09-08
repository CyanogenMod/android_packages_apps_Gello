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
import android.content.Context;
import android.os.Bundle;
import android.test.ActivityInstrumentationTestCase2;

import com.android.browser.BrowserActivity;
import com.android.browser.BrowserSettings;
import com.android.browser.PreferenceKeys;
import com.android.browser.R;
import com.android.browser.mdm.ManagedProfileManager;
import com.android.browser.mdm.SearchEngineRestriction;

public class SearchRestrictionsTest extends ActivityInstrumentationTestCase2<BrowserActivity>
        implements PreferenceKeys {

    private final static String TAG = "RestrictionsTest";
    private final static String VALID_SEARCH_ENGINE_NAME_1 = "netsprint";
    private final static String VALID_SEARCH_ENGINE_NAME_2 = "naver";
    //Search engine name that does not match an entry in res/values/all_search_engines.xml
    private final static String INVALID_SEARCH_ENGINE_NAME = "foo";

    private Instrumentation mInstrumentation;
    private Context mContext;
    private BrowserActivity mActivity;
    private SearchEngineRestriction mSearchEngineRestriction;
    String mDefaultSearchEngineName;

    public SearchRestrictionsTest() {
        super(BrowserActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mContext = getInstrumentation().getTargetContext();
        mActivity = getActivity();
        mSearchEngineRestriction = SearchEngineRestriction.getInstance();
        mDefaultSearchEngineName = mActivity.getApplicationContext()
                .getString(R.string.default_search_engine_value);
    }

    /*
     * Search Engine Restrictions Tests
     */

    // Ensure we start with the default search engine and no restriction
    public void testSR_initConditions() throws Throwable {
        assertFalse("Search engine restriction", mSearchEngineRestriction.isEnabled());
        assertEquals("Search provider", mDefaultSearchEngineName,
                BrowserSettings.getInstance().getSearchEngineName());
    }

    // Restriction is not set when Enabled is null or false
    public void testSR_NotSetWhenNotEnabled() throws Throwable {
        setDefaultSearchProvider(null, VALID_SEARCH_ENGINE_NAME_1);
        assertFalse("Search engine restriction", mSearchEngineRestriction.isEnabled());
        assertEquals("Search provider", mDefaultSearchEngineName,
                BrowserSettings.getInstance().getSearchEngineName());

        setDefaultSearchProvider(false, VALID_SEARCH_ENGINE_NAME_1);
        assertFalse("Search engine restriction", mSearchEngineRestriction.isEnabled());
        assertEquals("Search provider", mDefaultSearchEngineName,
                BrowserSettings.getInstance().getSearchEngineName());
    }

    // Restriction is not set when DefaultSearchProviderName is null or invalid
    public void testSR_NotSetWhenNameNullOrInvalid() throws Throwable {
        setDefaultSearchProvider(true, null);
        assertFalse("Search engine restriction", mSearchEngineRestriction.isEnabled());
        assertEquals("Search provider", mDefaultSearchEngineName,
                BrowserSettings.getInstance().getSearchEngineName());

        setDefaultSearchProvider(true, INVALID_SEARCH_ENGINE_NAME);
        assertFalse("Search engine restriction", mSearchEngineRestriction.isEnabled());
        assertEquals("Search provider", mDefaultSearchEngineName,
                BrowserSettings.getInstance().getSearchEngineName());
    }

    // Restriction is set when Enabled is TRUE and Name is VALID
    public void testSR_SetWhenEnabledAndNameValid() throws Throwable {
        setDefaultSearchProvider(true, VALID_SEARCH_ENGINE_NAME_1);
        assertTrue("Search engine restriction", mSearchEngineRestriction.isEnabled());
        assertEquals("Search provider", VALID_SEARCH_ENGINE_NAME_1,
                BrowserSettings.getInstance().getSearchEngineName());

        setDefaultSearchProvider(true, VALID_SEARCH_ENGINE_NAME_2);
        assertTrue("Search engine restriction", mSearchEngineRestriction.isEnabled());
        assertEquals("Search provider", VALID_SEARCH_ENGINE_NAME_2,
                BrowserSettings.getInstance().getSearchEngineName());

        // Restriction is lifted when neither Enabled nor Name are present
        setDefaultSearchProvider(null, null);
        assertFalse("Search engine restriction", mSearchEngineRestriction.isEnabled());
        assertEquals("Search provider", mDefaultSearchEngineName,
                BrowserSettings.getInstance().getSearchEngineName());
    }

    // Restriction is lifted when DefaultSearchProviderEnabled is FALSE or null
    public void testSR_LiftedWhenDisabledOrNull() throws Throwable {
        // set a valid search engine restriction
        setDefaultSearchProvider(true, VALID_SEARCH_ENGINE_NAME_1);
        assertTrue("Search engine restriction", mSearchEngineRestriction.isEnabled());
        assertEquals("Search provider", VALID_SEARCH_ENGINE_NAME_1,
                BrowserSettings.getInstance().getSearchEngineName());
        // then lift the restriction (false)
        setDefaultSearchProvider(false, VALID_SEARCH_ENGINE_NAME_2);
        assertFalse("Search engine restriction", mSearchEngineRestriction.isEnabled());
        assertEquals("Search provider", mDefaultSearchEngineName,
                BrowserSettings.getInstance().getSearchEngineName());

        // set a valid search engine restriction
        setDefaultSearchProvider(true, VALID_SEARCH_ENGINE_NAME_1);
        assertTrue("Search engine restriction", mSearchEngineRestriction.isEnabled());
        assertEquals("Search provider", VALID_SEARCH_ENGINE_NAME_1,
                BrowserSettings.getInstance().getSearchEngineName());
        // then lift the restriction (null)
        setDefaultSearchProvider(null, VALID_SEARCH_ENGINE_NAME_2);
        assertFalse("Search engine restriction", mSearchEngineRestriction.isEnabled());
        assertEquals("Search provider", mDefaultSearchEngineName,
                BrowserSettings.getInstance().getSearchEngineName());
    }

    // Restriction is lifted when Enabled is TRUE and Name is null or INVALID
    public void testSR_LiftedWhenNameIsNullOrInvalid() throws Throwable {
        // set a valid search engine restriction
        setDefaultSearchProvider(true, VALID_SEARCH_ENGINE_NAME_1);
        assertTrue("Search engine restriction", mSearchEngineRestriction.isEnabled());
        assertEquals("Search provider", VALID_SEARCH_ENGINE_NAME_1,
                BrowserSettings.getInstance().getSearchEngineName());
        // then lift the restriction with invalid name
        setDefaultSearchProvider(true, INVALID_SEARCH_ENGINE_NAME);
        assertFalse("Search engine restriction", mSearchEngineRestriction.isEnabled());
        assertEquals("Search provider", mDefaultSearchEngineName,
                BrowserSettings.getInstance().getSearchEngineName());

        // set a valid search engine restriction
        setDefaultSearchProvider(true, VALID_SEARCH_ENGINE_NAME_1);
        assertTrue("Search engine restriction", mSearchEngineRestriction.isEnabled());
        assertEquals("Search provider", VALID_SEARCH_ENGINE_NAME_1,
                BrowserSettings.getInstance().getSearchEngineName());
        // then lift the restriction with a null
        setDefaultSearchProvider(true, null);
        assertFalse("Search engine restriction", mSearchEngineRestriction.isEnabled());
        assertEquals("Search provider", mDefaultSearchEngineName,
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
