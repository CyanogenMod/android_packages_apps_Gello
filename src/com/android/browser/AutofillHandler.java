
/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.browser;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;


import java.util.concurrent.CountDownLatch;

import org.codeaurora.swe.AutoFillProfile;


public class AutofillHandler {

    protected AutoFillProfile mAutoFillProfile = null;
    // Default to zero. In the case no profile is set up, the initial
    // value will come from the AutoFillSettingsFragment when the user
    // creates a profile. Otherwise, we'll read the ID of the last used
    // profile from the prefs db.
    protected String mAutoFillActiveProfileId = "";
    private static final int NO_AUTOFILL_PROFILE_SET = 0;
    private Context mContext;

    private static final String LOGTAG = "AutofillHandler";

    public AutofillHandler(Context context) {
        mContext = context.getApplicationContext();
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(mContext);
        mAutoFillActiveProfileId = p.getString(
                    PreferenceKeys.PREF_AUTOFILL_ACTIVE_PROFILE_ID,
                    mAutoFillActiveProfileId);
    }

    public synchronized void setAutoFillProfile(AutoFillProfile profile) {
        mAutoFillProfile = profile;
        if (profile == null)
            setActiveAutoFillProfileId("");
        else
            setActiveAutoFillProfileId(profile.getUniqueId());
    }

    public synchronized AutoFillProfile getAutoFillProfile() {
        return mAutoFillProfile;
    }

    public synchronized String getAutoFillProfileId() {
        return mAutoFillActiveProfileId;
    }

    private synchronized void setActiveAutoFillProfileId(String activeProfileId) {
        if (mAutoFillActiveProfileId.equals(activeProfileId)) {
            return;
        }
        mAutoFillActiveProfileId = activeProfileId;
        Editor ed = PreferenceManager.
            getDefaultSharedPreferences(mContext).edit();
        ed.putString(PreferenceKeys.PREF_AUTOFILL_ACTIVE_PROFILE_ID, activeProfileId);
        ed.apply();
    }
}
