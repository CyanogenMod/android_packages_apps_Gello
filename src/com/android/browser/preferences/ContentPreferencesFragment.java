/*
 * Copyright (c) 2015, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.android.browser.preferences;

import android.app.Activity;
import android.net.Uri;
import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.browser.BrowserConfig;
import com.android.browser.BrowserSettings;
import com.android.browser.DownloadHandler;
import com.android.browser.PreferenceKeys;
import com.android.browser.R;

public class ContentPreferencesFragment extends SWEPreferenceFragment {
    public static final int DOWNLOAD_PATH_RESULT_CODE = 1;
    private final static String LOGTAG = "ContentPreferences";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the XML preferences file
        addPreferencesFromResource(R.xml.content_preferences);

        PreferenceScreen screen = (PreferenceScreen) findPreference("content_settings");

        if (!BrowserConfig.getInstance(getActivity().getApplicationContext())
                .hasFeature(BrowserConfig.Feature.CUSTOM_DOWNLOAD_PATH)) {
            screen.removePreference(findPreference(PreferenceKeys.PREF_DOWNLOAD_PATH));
        } else {
            PreferenceScreen downloadPathPreset =
                    (PreferenceScreen) findPreference(PreferenceKeys.PREF_DOWNLOAD_PATH);
            downloadPathPreset.setOnPreferenceClickListener(onClickDownloadPathSettings());

            String downloadPath = downloadPathPreset.getSharedPreferences().
                    getString(PreferenceKeys.PREF_DOWNLOAD_PATH,
                            BrowserSettings.getInstance().getDownloadPath());
            String downloadPathForUser = DownloadHandler.getDownloadPathForUser(getActivity(),
                    downloadPath);
            downloadPathPreset.setSummary(downloadPathForUser);
        }
    }

    private Preference.OnPreferenceClickListener onClickDownloadPathSettings() {
        return new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                final String filemanagerIntent =
                        getResources().getString(R.string.def_intent_file_manager);
                if (!TextUtils.isEmpty(filemanagerIntent)) {
                    try {
                        Intent i = new Intent(filemanagerIntent);
                        startActivityForResult(i,
                                DOWNLOAD_PATH_RESULT_CODE);
                    } catch (Exception e) {
                        String err_msg = getResources().getString(
                                R.string.activity_not_found,
                                filemanagerIntent);
                        Toast.makeText(getActivity(), err_msg, Toast.LENGTH_LONG).show();
                    }
                    return true;
                } else {
                    Log.e(LOGTAG, "File Manager intent not defined !!");
                    return true;
                }
            }
        };
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == DOWNLOAD_PATH_RESULT_CODE &&
                (resultCode == Activity.RESULT_OK && intent != null)) {
            final String result_dir_sel =
                getActivity().getResources().getString(R.string.def_file_manager_result_dir);
            String downloadPath = intent.getStringExtra(result_dir_sel);
            // Fallback logic to stock browser
            if (downloadPath == null) {
                Uri uri = intent.getData();
                if(uri != null)
                    downloadPath = uri.getPath();
            }
            if (downloadPath != null) {
                PreferenceScreen downloadPathPreset =
                        (PreferenceScreen) findPreference(
                                PreferenceKeys.PREF_DOWNLOAD_PATH);
                Editor editor = downloadPathPreset.getEditor();
                editor.putString(PreferenceKeys.PREF_DOWNLOAD_PATH, downloadPath);
                editor.apply();
                String downloadPathForUser = DownloadHandler.getDownloadPathForUser(
                        getActivity(), downloadPath);
                downloadPathPreset.setSummary(downloadPathForUser);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ActionBar bar = getActivity().getActionBar();
        if (bar != null) {
            bar.setTitle(R.string.pref_content_title);
            bar.setDisplayHomeAsUpEnabled(false);
            bar.setHomeButtonEnabled(false);
        }
    }
}
