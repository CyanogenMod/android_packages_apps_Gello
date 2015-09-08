/*
 * Copyright (C) 2010 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.browser.preferences;

import android.app.ActionBar;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.Preference.OnPreferenceChangeListener;

import com.android.browser.PreferenceKeys;
import com.android.browser.R;

import org.codeaurora.swe.PermissionsServiceFactory;

public class DebugPreferencesFragment extends SWEPreferenceFragment
        implements OnPreferenceClickListener, OnPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the XML preferences file
        addPreferencesFromResource(R.xml.debug_preferences);

        SwitchPreference pref = (SwitchPreference) findPreference(PreferenceKeys.PREF_DISABLE_PERF);
        pref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object objValue) {
        if (getActivity() == null) {
            return false;
        }

        if (pref.getKey().equals(PreferenceKeys.PREF_DISABLE_PERF)) {
            PermissionsServiceFactory.setDefaultPermissions(
                PermissionsServiceFactory.PermissionType.WEBREFINER, (Boolean)objValue);
            return true;
        }

        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        ActionBar bar = getActivity().getActionBar();
        if (bar != null) {
            bar.setTitle(R.string.pref_development_title);
            bar.setDisplayHomeAsUpEnabled(false);
            bar.setHomeButtonEnabled(false);
        }
    }
}
