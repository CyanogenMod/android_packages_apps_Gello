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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.android.browser.Browser;
import com.android.browser.BrowserSettings;
import com.android.browser.PreferenceKeys;
import com.android.browser.R;
import com.android.browser.search.SearchEngineInfo;
import com.android.browser.search.SearchEngines;

public class SearchEngineRestriction extends Restriction implements PreferenceKeys {

    private final static String TAG = "SearchEngineRestriction";

    private static final String DEFAULT_SEARCH_PROVIDER_ENABLED = "DefaultSearchProviderEnabled"; // boolean
    private static final String SEARCH_PROVIDER_NAME = "SearchProviderName"; // String

    private static SearchEngineRestriction sInstance;

    private SearchEngineInfo mSearchEngineInfo;

    private SearchEngineRestriction() {
        super(TAG);
    }

    public static SearchEngineRestriction getInstance() {
        synchronized (SearchEngineRestriction.class) {
            if (sInstance == null) {
                sInstance = new SearchEngineRestriction();
            }
        }
        return sInstance;
    }

    @Override
    public void enforce(Bundle restrictions) {
        SharedPreferences.Editor editor = BrowserSettings.getInstance().getPreferences().edit();
        String searchEngineName = restrictions.getString(SEARCH_PROVIDER_NAME);
        SearchEngineInfo searchEngineInfo = null;
        Context context = Browser.getContext();

        if (searchEngineName != null)
            searchEngineInfo = SearchEngines.getSearchEngineInfo(context, searchEngineName);

        if (restrictions.getBoolean(DEFAULT_SEARCH_PROVIDER_ENABLED, false) &&
                searchEngineInfo != null) {
            mSearchEngineInfo = searchEngineInfo;
            // Set search engine
            editor.putString(PREF_SEARCH_ENGINE, searchEngineName);
            editor.apply();
            enable(true);
        } else if (isEnabled()) {
            mSearchEngineInfo = null;
            enable(false);
            // Restore default search engine
            editor.putString(PREF_SEARCH_ENGINE,
                    context.getString(R.string.default_search_engine_value));
            editor.apply();
        }
    }

    public SearchEngineInfo getSearchEngineInfo() {
        return mSearchEngineInfo;
    }
}
