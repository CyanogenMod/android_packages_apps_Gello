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
import android.content.Intent;
import android.content.res.Resources;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
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

public class AdvancedPreferencesFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final int DOWNLOAD_PATH_RESULT_CODE = 1;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the XML preferences file
        addPreferencesFromResource(R.xml.advanced_preferences);

        PreferenceScreen websiteSettings = (PreferenceScreen) findPreference(
                PreferenceKeys.PREF_WEBSITE_SETTINGS);
        websiteSettings.setFragment(WebsiteSettingsFragment.class.getName());

        Preference e = findPreference(PreferenceKeys.PREF_DEFAULT_ZOOM);
        e.setOnPreferenceChangeListener(this);
        e.setSummary(getVisualDefaultZoomName(
                getPreferenceScreen().getSharedPreferences()
                .getString(PreferenceKeys.PREF_DEFAULT_ZOOM, null)) );

        e = findPreference(PreferenceKeys.PREF_DEFAULT_TEXT_ENCODING);
        e.setOnPreferenceChangeListener(this);

        e = findPreference(PreferenceKeys.PREF_RESET_DEFAULT_PREFERENCES);
        e.setOnPreferenceChangeListener(this);

        e = findPreference(PreferenceKeys.PREF_SEARCH_ENGINE);
        e.setOnPreferenceChangeListener(this);
        updateListPreferenceSummary((ListPreference) e);

        updateListPreferenceSummary((ListPreference) e);
        onInitdownloadSettingsPreference();
    }

    private void onInitdownloadSettingsPreference() {
        addPreferencesFromResource(R.xml.download_settings_preferences);
        PreferenceScreen downloadPathPreset =
                (PreferenceScreen) findPreference(PreferenceKeys.PREF_DOWNLOAD_PATH);
        downloadPathPreset.setOnPreferenceClickListener(onClickDownloadPathSettings());

        String downloadPath = downloadPathPreset.getSharedPreferences().
                getString(PreferenceKeys.PREF_DOWNLOAD_PATH,
                        BrowserSettings.getInstance().getDownloadPath());
        String downloadPathForUser = DownloadHandler.getDownloadPathForUser(this.getActivity(),
                downloadPath);
        downloadPathPreset.setSummary(downloadPathForUser);
    }

    private Preference.OnPreferenceClickListener onClickDownloadPathSettings() {
        return new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                try {
                    Intent i = new Intent("com.android.fileexplorer.action.DIR_SEL");
                    AdvancedPreferencesFragment.this.startActivityForResult(i,
                            DOWNLOAD_PATH_RESULT_CODE);
                } catch (Exception e) {
                    String err_msg = getResources().getString(R.string.activity_not_found,
                            "com.android.fileexplorer.action.DIR_SEL");
                    Toast.makeText(getActivity(), err_msg, Toast.LENGTH_LONG).show();
                }
                return true;
            }
        };
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

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
                            (PreferenceScreen) findPreference(PreferenceKeys.PREF_DOWNLOAD_PATH);
                    Editor editor = downloadPathPreset.getEditor();
                    editor.putString(PreferenceKeys.PREF_DOWNLOAD_PATH, downloadPath);
                    editor.apply();
                    String downloadPathForUser = DownloadHandler.getDownloadPathForUser(
                            this.getActivity(), downloadPath);
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
    @Override
    public void onResume() {
        super.onResume();
        final PreferenceScreen websiteSettings = (PreferenceScreen) findPreference(
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
        if (getActivity() == null) {
            // We aren't attached, so don't accept preferences changes from the
            // invisible UI.
            Log.w("PageContentPreferencesFragment", "onPreferenceChange called from detached fragment!");
            return false;
        }

        if (pref.getKey().equals(PreferenceKeys.PREF_DEFAULT_ZOOM)) {
            pref.setSummary(getVisualDefaultZoomName((String) objValue));
            return true;
        } else if (pref.getKey().equals(PreferenceKeys.PREF_DEFAULT_TEXT_ENCODING)) {
            pref.setSummary((String) objValue);
            return true;
        } else if (pref.getKey().equals(PreferenceKeys.PREF_RESET_DEFAULT_PREFERENCES)) {
            Boolean value = (Boolean) objValue;
            if (value.booleanValue() == true) {
                startActivity(new Intent(BrowserActivity.ACTION_RESTART, null,
                        getActivity(), BrowserActivity.class));
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

    private CharSequence getVisualDefaultZoomName(String enumName) {
        Resources res = getActivity().getResources();
        CharSequence[] visualNames = res.getTextArray(R.array.pref_default_zoom_choices);
        CharSequence[] enumNames = res.getTextArray(R.array.pref_default_zoom_values);

        // Sanity check
        if (visualNames.length != enumNames.length) {
            return "";
        }

        int length = enumNames.length;
        for (int i = 0; i < length; i++) {
            if (enumNames[i].equals(enumName)) {
                return visualNames[i];
            }
        }

        return "";
    }
}
