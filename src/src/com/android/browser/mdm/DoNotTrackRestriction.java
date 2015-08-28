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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;

import com.android.browser.BrowserSettings;
import com.android.browser.PreferenceKeys;

public class DoNotTrackRestriction extends Restriction implements PreferenceKeys {

    private final static String TAG = "DoNotTrackRestriction";

    public static final String DO_NOT_TRACK_ENABLED = "DoNotTrackEnabled"; // boolean
    public static final String DO_NOT_TRACK_VALUE   = "DoNotTrackValue";   // boolean

    private static DoNotTrackRestriction sInstance;
    private boolean mDntValue;

    private MdmCheckBoxPreference mPref = null;

    private DoNotTrackRestriction() {
        super(TAG);
    }

    public static DoNotTrackRestriction getInstance() {
        synchronized (DoNotTrackRestriction.class) {
            if (sInstance == null) {
                sInstance = new DoNotTrackRestriction();
            }
        }
        return sInstance;
    }

    @Override
    public void enforce(Bundle restrictions) {
        SharedPreferences.Editor editor = BrowserSettings.getInstance().getPreferences().edit();
        // Possible states
        //   DNT_enabled  DNT_value  |  menu-item-enabled      check-box-value
        //   -----------------------------------------------------------------
        //     not set       x       |        Yes              curr-sys-value
        //       0           x       |        Yes              curr-sys-value
        //       1           0       |        No                   0
        //       1           1       |        No                   1

        boolean dntEnabled = restrictions.getBoolean(DO_NOT_TRACK_ENABLED,false);
        if (dntEnabled) {
            mDntValue = restrictions.getBoolean(DO_NOT_TRACK_VALUE, true); // default to true

            editor.putBoolean(PREF_DO_NOT_TRACK, mDntValue);
            editor.apply();

            // enable the restriction : controls enable of the menu item
            // Log.i(TAG, "DNT Restriction enabled. new val [" + mDntValue + "]");
            enable(true);
        }
        else {
            enable(false);
        }

        // Real time update of the Preference if it is registered
        updatePref();
    }

    private void updatePref() {
        if (null != mPref) {
            if (isEnabled()) {
                mPref.setChecked(getValue());
                mPref.disablePref();
            }
            else {
                mPref.enablePref();
            }
            mPref.setMdmRestrictionState(isEnabled());
        }
    }

    public boolean getValue() {
        return mDntValue;
    }

    public void registerPreference (Preference pref) {
        mPref = (MdmCheckBoxPreference) pref;
        updatePref();
    }
}
