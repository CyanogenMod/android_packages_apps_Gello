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

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.android.browser.BaseUi;
import com.android.browser.BrowserActivity;
import com.android.browser.BrowserSettings;
import com.android.browser.BrowserYesNoPreference;
import com.android.browser.DownloadHandler;
import com.android.browser.PreferenceKeys;
import com.android.browser.R;

public class AdvancedPreferencesFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    PreferenceFragment mFragment = null;

    AdvancedPreferencesFragment(PreferenceFragment fragment) {
        mFragment = fragment;

        Preference e = mFragment.findPreference(PreferenceKeys.PREF_RESET_DEFAULT_PREFERENCES);
        e.setOnPreferenceChangeListener(this);

        e = mFragment.findPreference(PreferenceKeys.PREF_SEARCH_ENGINE);
        e.setOnPreferenceChangeListener(this);
        updateListPreferenceSummary((ListPreference) e);

        e = mFragment.findPreference("privacy_security");
        e.setOnPreferenceClickListener(this);

        e = mFragment.findPreference(PreferenceKeys.PREF_DEBUG_MENU);
        if (!BrowserSettings.getInstance().isDebugEnabled()) {
            PreferenceCategory category = (PreferenceCategory) mFragment.findPreference("advanced");
            category.removePreference(e);
        } else {
            e.setOnPreferenceClickListener(this);
        }

        e = mFragment.findPreference("accessibility_menu");
        e.setOnPreferenceClickListener(this);

        // Below are preferences for carrier specific features
        PreferenceScreen contentSettingsPrefScreen =
                (PreferenceScreen) mFragment.findPreference("content_settings");
        contentSettingsPrefScreen.setOnPreferenceClickListener(this);

        ListPreference edgeSwipePref =
                (ListPreference) mFragment.findPreference("edge_swiping_action");
        edgeSwipePref.setOnPreferenceChangeListener(this);

        if (BaseUi.isUiLowPowerMode()) {
            edgeSwipePref.setEnabled(false);
        } else {
            String[] options = mFragment.getResources().getStringArray(
                    R.array.pref_edge_swiping_values);

            String value = BrowserSettings.getInstance().getEdgeSwipeAction();

            if (value.equals(mFragment.getString(R.string.value_unknown_edge_swipe))) {
                edgeSwipePref.setSummary(mFragment.getString(R.string.pref_edge_swipe_unknown));
            } else {
                for (int i = 0; i < options.length; i++) {
                    if (value.equals(options[i])) {
                        edgeSwipePref.setValueIndex(i);
                        break;
                    }
                }
            }
        }
    }

    void updateListPreferenceSummary(ListPreference e) {
        e.setSummary(e.getEntry());
    }

    /*
     * We need to set the PreferenceScreen state in onResume(), as the number of
     * origins with active features (WebStorage, Geolocation etc) could have
     * changed after calling the WebsiteSettingsActivity.
     */
    public void onResume() {
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object objValue) {
        if (mFragment.getActivity() == null) {
            // We aren't attached, so don't accept preferences changes from the
            // invisible UI.
            Log.w("PageContentPreferencesFragment", "onPreferenceChange called from detached fragment!");
            return false;
        }
        if(pref.getKey().equals("edge_swiping_action")){
            ListPreference lp = (ListPreference) pref;
            lp.setValue((String) objValue);
            updateListPreferenceSummary(lp);
            return true;
        }

        else if (pref.getKey().equals(PreferenceKeys.PREF_RESET_DEFAULT_PREFERENCES)) {
            Integer value = (Integer) objValue;
            if (value.intValue() != BrowserYesNoPreference.CANCEL_BTN) {
                BrowserSettings settings = BrowserSettings.getInstance();
                if (value.intValue() == BrowserYesNoPreference.OTHER_BTN) {
                    settings.clearCache();
                    settings.clearDatabases();
                    settings.clearCookies();
                    settings.clearHistory();
                    settings.clearFormData();
                    settings.clearPasswords();
                    settings.clearLocationAccess();
                }

                settings.resetDefaultPreferences();
                mFragment.startActivity(new Intent(BrowserActivity.ACTION_RESTART, null,
                        mFragment.getActivity(), BrowserActivity.class));
                return true;
            }

        } else if (pref.getKey().equals(PreferenceKeys.PREF_SEARCH_ENGINE)) {
            ListPreference lp = (ListPreference) pref;
            // update the user preference
            BrowserSettings.getInstance().setUserSearchEngine((String) objValue);
            lp.setValue((String) objValue);
            updateListPreferenceSummary(lp);
            return false;
        }
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        FragmentManager fragmentManager = mFragment.getFragmentManager();

        if (preference.getKey().equals(PreferenceKeys.PREF_DEBUG_MENU)) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            Fragment newFragment = new DebugPreferencesFragment();
            fragmentTransaction.replace(mFragment.getId(), newFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
            return true;
        } else if (preference.getKey().equals("accessibility_menu")) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            Fragment newFragment = new AccessibilityPreferencesFragment();
            fragmentTransaction.replace(mFragment.getId(), newFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
            return true;
        } else if (preference.getKey().equals("privacy_security")) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            Fragment newFragment = new PrivacySecurityPreferencesFragment();
            fragmentTransaction.replace(mFragment.getId(), newFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
            return true;
        } else if (preference.getKey().equals("content_settings")) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            Fragment newFragment = new ContentPreferencesFragment();
            fragmentTransaction.replace(mFragment.getId(), newFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
            return true;
        }
        return false;
    }
}
