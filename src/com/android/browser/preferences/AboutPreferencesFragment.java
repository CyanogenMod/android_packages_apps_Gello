/*
    * Copyright (c) 2014, The Linux Foundation. All rights reserved.
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

package com.android.browser.preferences;

import android.app.ActionBar;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.Browser;

import com.android.browser.BrowserActivity;
import com.android.browser.BrowserPreferencesPage;
import com.android.browser.BrowserSwitches;
import com.android.browser.PreferenceKeys;
import com.android.browser.R;
import com.android.browser.UpdateNotificationService;

import org.codeaurora.swe.BrowserCommandLine;

public class AboutPreferencesFragment extends PreferenceFragment
                            implements OnPreferenceClickListener {

    final String ABOUT_TEXT_VERSION_KEY = "Version:";
    final String ABOUT_TEXT_BUILT_KEY = "Built:";
    final String ABOUT_TEXT_HASH_KEY = "Hash:";

    String mFeedbackRecipient = "";
    String mHelpURL = "";
    String mVersion = "";
    String mBuilt = "";
    String mHash = "";
    String mTabTitle = "";
    String mTabURL = "";

    String mAboutText = "";
    PreferenceScreen mHeadPref = null;

    private String findValueFromAboutText(String aboutKey) {
        int start = mAboutText.indexOf(aboutKey);
        int end = mAboutText.indexOf("\n", start);
        String value = "";

        if (start != -1 && end != -1) {
            start += aboutKey.length();
            value = mAboutText.substring(start, end);
        }
        return value;
    }

    private void setPreference(String prefKey, String value) {
        Preference pref = findPreference(prefKey);
        if (pref == null) {
            return;
        }

        if (value.isEmpty()) {
            if (mHeadPref != null)
                mHeadPref.removePreference(pref);
        } else {
            pref.setSummary(value);
        }
    }

    private void setOnClickListener(String prefKey, boolean set) {
        Preference pref = findPreference(prefKey);
        if (pref == null) {
            return;
        }

        if (set) {
            pref.setOnPreferenceClickListener(this);
        } else {
            if (mHeadPref != null)
                mHeadPref.removePreference(pref);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar bar = getActivity().getActionBar();
        if (bar != null) {
            bar.setTitle(R.string.about);
        }

        mAboutText = getString(R.string.about_text);

        addPreferencesFromResource(R.xml.about_preferences);
        mHeadPref = (PreferenceScreen) findPreference(PreferenceKeys.PREF_ABOUT);

        mVersion = findValueFromAboutText(ABOUT_TEXT_VERSION_KEY);
        setPreference(PreferenceKeys.PREF_VERSION, mVersion);

        mBuilt = findValueFromAboutText(ABOUT_TEXT_BUILT_KEY);
        setPreference(PreferenceKeys.PREF_BUILD_DATE, mBuilt);

        mHash = findValueFromAboutText(ABOUT_TEXT_HASH_KEY);
        setPreference(PreferenceKeys.PREF_BUILD_HASH, mHash);

        final Bundle arguments = getArguments();
        String user_agent = "";
        if (arguments != null) {
            user_agent = (String) arguments.getCharSequence("UA", "");
            mTabTitle = (String) arguments.getCharSequence("TabTitle", "");
            mTabURL = (String) arguments.getCharSequence("TabURL", "");
        }

        setPreference(PreferenceKeys.PREF_USER_AGENT, user_agent);

        if (BrowserCommandLine.hasSwitch(BrowserSwitches.CMD_LINE_SWITCH_HELPURL)) {
            mHelpURL = BrowserCommandLine.getSwitchValue(
                    BrowserSwitches.CMD_LINE_SWITCH_HELPURL);
        }

        setOnClickListener(PreferenceKeys.PREF_HELP, !mHelpURL.isEmpty());

        if (BrowserCommandLine.hasSwitch(BrowserSwitches.CMD_LINE_SWITCH_FEEDBACK)) {
            mFeedbackRecipient = BrowserCommandLine.getSwitchValue(
                    BrowserSwitches.CMD_LINE_SWITCH_FEEDBACK);
        }

        setOnClickListener(PreferenceKeys.PREF_FEEDBACK, !mFeedbackRecipient.isEmpty());

        setOnClickListener(PreferenceKeys.PREF_LEGAL, true);
        if (BrowserCommandLine.hasSwitch(BrowserSwitches.AUTO_UPDATE_SERVER_CMD)) {
            setPreference(PreferenceKeys.PREF_AUTO_UPDATE,
                    UpdateNotificationService.getLatestVersion(getActivity()));
            setOnClickListener(PreferenceKeys.PREF_AUTO_UPDATE,
                    UpdateNotificationService.getCurrentVersionCode(getActivity()) <
                            UpdateNotificationService.getLatestVersionCode(getActivity()));
        } else {
            Preference pref = findPreference(PreferenceKeys.PREF_AUTO_UPDATE);
            if (mHeadPref != null)
                mHeadPref.removePreference(pref);
        }

    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals(PreferenceKeys.PREF_HELP)) {
            Intent intent = new Intent(getActivity(), BrowserActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(mHelpURL));
            getActivity().startActivity(intent);
            return true;
        } else if(preference.getKey().equals(PreferenceKeys.PREF_LEGAL)) {
            Bundle bundle = new Bundle();
            BrowserPreferencesPage.startPreferenceFragmentExtraForResult(getActivity(),
                    LegalPreferencesFragment.class.getName(), bundle, 0);
            return true;
        } else if (preference.getKey().equals(PreferenceKeys.PREF_FEEDBACK)) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("message/rfc822");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{mFeedbackRecipient});
            intent.putExtra(Intent.EXTRA_SUBJECT,"Browser Feedback");

            String message = "";
            if (!mVersion.isEmpty()) {
                message += "Version: " + mVersion + "\n";
            }

            if (!mBuilt.isEmpty()) {
                message += "Build Date: " + mBuilt + "\n";
            }

            if (!mHash.isEmpty()) {
                message += "Build Hash: " + mHash + "\n";
            }

            if (!mTabTitle.isEmpty()) {
                message += "Tab Title: " + mTabTitle + "\n";
            }

            if (!mTabURL.isEmpty()) {
                message += "Tab URL: " + mTabURL + "\n";
            }

            message += "\nEnter your feedback here...";

            intent.putExtra(Intent.EXTRA_TEXT, message);
            startActivity(Intent.createChooser(intent, "Select email application"));
            return true;
        } else if (preference.getKey().equals(PreferenceKeys.PREF_AUTO_UPDATE)) {
            Intent intent = new Intent(getActivity(), BrowserActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, getActivity().getPackageName());
            intent.putExtra(Browser.EXTRA_CREATE_NEW_TAB, true);
            intent.setData(Uri.parse(
                    UpdateNotificationService.getLatestDownloadUrl(getActivity())));
            getActivity().startActivity(intent);
        }
        return false;
    }
}
