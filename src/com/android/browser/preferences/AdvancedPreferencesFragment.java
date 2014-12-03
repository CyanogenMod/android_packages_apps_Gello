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
import android.content.res.Resources;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.webkit.ValueCallback;
import android.widget.Toast;

import com.android.browser.BrowserActivity;
import com.android.browser.BrowserSettings;
import com.android.browser.DownloadHandler;
import com.android.browser.PreferenceKeys;
import com.android.browser.R;

import java.util.Map;
import java.util.Set;
import org.codeaurora.swe.GeolocationPermissions;
import org.codeaurora.swe.WebStorage;

public class AdvancedPreferencesFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final int DOWNLOAD_PATH_RESULT_CODE = 1;

    PreferenceFragment mFragment = null;

    AdvancedPreferencesFragment(PreferenceFragment fragment) {
        mFragment = fragment;

        PreferenceScreen websiteSettings = (PreferenceScreen) mFragment.findPreference(
                PreferenceKeys.PREF_WEBSITE_SETTINGS);
        websiteSettings.setFragment(WebsiteSettingsFragment.class.getName());
        websiteSettings.setOnPreferenceClickListener(this);

        Preference e = mFragment.findPreference(PreferenceKeys.PREF_RESET_DEFAULT_PREFERENCES);
        e.setOnPreferenceChangeListener(this);

        e = mFragment.findPreference(PreferenceKeys.PREF_SEARCH_ENGINE);
        e.setOnPreferenceChangeListener(this);
        updateListPreferenceSummary((ListPreference) e);

        e = mFragment.findPreference(PreferenceKeys.PREF_DEBUG_MENU);
        if (!BrowserSettings.getInstance().isDebugEnabled()) {
            PreferenceCategory category = (PreferenceCategory) mFragment.findPreference("advanced");
            category.removePreference(e);
        } else {
            e.setOnPreferenceClickListener(this);
        }

        e = mFragment.findPreference("accessibility_menu");
        e.setOnPreferenceClickListener(this);


        onInitdownloadSettingsPreference();
    }

    private void onInitdownloadSettingsPreference() {
        PreferenceScreen downloadPathPreset =
                (PreferenceScreen) mFragment.findPreference(PreferenceKeys.PREF_DOWNLOAD_PATH);
        downloadPathPreset.setOnPreferenceClickListener(onClickDownloadPathSettings());

        String downloadPath = downloadPathPreset.getSharedPreferences().
                getString(PreferenceKeys.PREF_DOWNLOAD_PATH,
                        BrowserSettings.getInstance().getDownloadPath());
        String downloadPathForUser = DownloadHandler.getDownloadPathForUser(mFragment.getActivity(),
                downloadPath);
        downloadPathPreset.setSummary(downloadPathForUser);
    }

    private Preference.OnPreferenceClickListener onClickDownloadPathSettings() {
        return new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                try {
                    Intent i = new Intent("com.android.fileexplorer.action.DIR_SEL");
                    mFragment.startActivityForResult(i,
                            DOWNLOAD_PATH_RESULT_CODE);
                } catch (Exception e) {
                    String err_msg = mFragment.getResources().getString(R.string.activity_not_found,
                            "com.android.fileexplorer.action.DIR_SEL");
                    Toast.makeText(mFragment.getActivity(), err_msg, Toast.LENGTH_LONG).show();
                }
                return true;
            }
        };
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DOWNLOAD_PATH_RESULT_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                String downloadPath = data.getStringExtra("result_dir_sel");
                // Fallback logic to stock browser
                if (downloadPath == null) {
                    Uri uri = data.getData();
                    if(uri != null)
                        downloadPath = uri.getPath();
                }
                if (downloadPath != null) {
                    PreferenceScreen downloadPathPreset =
                            (PreferenceScreen) mFragment.findPreference(
                                    PreferenceKeys.PREF_DOWNLOAD_PATH);
                    Editor editor = downloadPathPreset.getEditor();
                    editor.putString(PreferenceKeys.PREF_DOWNLOAD_PATH, downloadPath);
                    editor.apply();
                    String downloadPathForUser = DownloadHandler.getDownloadPathForUser(
                            mFragment.getActivity(), downloadPath);
                    downloadPathPreset.setSummary(downloadPathForUser);
                }

                return;
            }
        }
        return;
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
        final PreferenceScreen websiteSettings = (PreferenceScreen) mFragment.findPreference(
                PreferenceKeys.PREF_WEBSITE_SETTINGS);
        websiteSettings.setEnabled(false);
        WebStorage.getInstance().getOrigins(new ValueCallback<Map>() {
            @Override
            public void onReceiveValue(Map webStorageOrigins) {
                if ((webStorageOrigins != null) && !webStorageOrigins.isEmpty()) {
                    websiteSettings.setEnabled(true);
                }
            }
        });
        GeolocationPermissions.getInstance().getOrigins(new ValueCallback<Set<String> >() {
            @Override
            public void onReceiveValue(Set<String> geolocationOrigins) {
                if ((geolocationOrigins != null) && !geolocationOrigins.isEmpty()) {
                    websiteSettings.setEnabled(true);
                }
            }
        });
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object objValue) {
        if (mFragment.getActivity() == null) {
            // We aren't attached, so don't accept preferences changes from the
            // invisible UI.
            Log.w("PageContentPreferencesFragment", "onPreferenceChange called from detached fragment!");
            return false;
        }

        if (pref.getKey().equals(PreferenceKeys.PREF_RESET_DEFAULT_PREFERENCES)) {
            Boolean value = (Boolean) objValue;
            if (value.booleanValue() == true) {
                mFragment.startActivity(new Intent(BrowserActivity.ACTION_RESTART, null,
                        mFragment.getActivity(), BrowserActivity.class));
                return true;
            }
        } else if (pref.getKey().equals(PreferenceKeys.PREF_SEARCH_ENGINE)) {
            ListPreference lp = (ListPreference) pref;
            lp.setValue((String) objValue);
            updateListPreferenceSummary(lp);
            return false;
        }
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        FragmentManager fragmentManager = mFragment.getFragmentManager();

        if (preference.getKey().equals(PreferenceKeys.PREF_WEBSITE_SETTINGS)) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            Fragment newFragment = new WebsiteSettingsFragment();
            fragmentTransaction.replace(mFragment.getId(), newFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
            return true;
        } else if (preference.getKey().equals(PreferenceKeys.PREF_DEBUG_MENU)) {
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
        }

        return false;
    }
}
