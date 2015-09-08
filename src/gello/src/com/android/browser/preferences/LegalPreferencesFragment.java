/*
 *  Copyright (c) 2015 The Linux Foundation. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 *      * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 *  ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 *  BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 *  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 *  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 *  OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 *  IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.android.browser.preferences;

import android.app.ActionBar;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.android.browser.BrowserSwitches;
import com.android.browser.PreferenceKeys;
import com.android.browser.R;

import org.codeaurora.swe.BrowserCommandLine;

public class LegalPreferencesFragment extends PreferenceFragment
                            implements OnPreferenceClickListener {

    private static final String creditsUrl = "chrome://credits";
    PreferenceScreen mHeadPref = null;
    String mEulaUrl = "";
    String mPrivacyPolicyUrl = "";

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
            bar.setTitle(R.string.swe_legal);
        }
        addPreferencesFromResource(R.xml.legal_preferences);
        mHeadPref = (PreferenceScreen) findPreference(PreferenceKeys.PREF_LEGAL);


        setOnClickListener(PreferenceKeys.PREF_LEGAL_CREDITS, true);

        if(BrowserCommandLine.hasSwitch(BrowserSwitches.CMD_LINE_SWITCH_EULA_URL)) {
            mEulaUrl = BrowserCommandLine.getSwitchValue(BrowserSwitches.CMD_LINE_SWITCH_EULA_URL);
        } else {
            mEulaUrl = getResources().getString(R.string.swe_eula_url);
        }
        setOnClickListener(PreferenceKeys.PREF_LEGAL_EULA, !mEulaUrl.isEmpty());


        if(BrowserCommandLine.hasSwitch(BrowserSwitches.CMD_LINE_SWITCH_PRIVACY_POLICY_URL)) {
            mPrivacyPolicyUrl = BrowserCommandLine.getSwitchValue(
                    BrowserSwitches.CMD_LINE_SWITCH_PRIVACY_POLICY_URL);
        } else {
            mPrivacyPolicyUrl = getResources().getString(R.string.swe_privacy_policy_url);
        }
        setOnClickListener(PreferenceKeys.PREF_LEGAL_PRIVACY_POLICY, !mPrivacyPolicyUrl.isEmpty());
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        Bundle b = new Bundle();
        if(preference.getKey().equals(PreferenceKeys.PREF_LEGAL_CREDITS)) {
            Intent i = new Intent(getActivity(), LegalPreviewActivity.class);
            i.putExtra(LegalPreviewActivity.URL_INTENT_EXTRA, creditsUrl);
            startActivity(i);
            return true;
        } else if(preference.getKey().equals(PreferenceKeys.PREF_LEGAL_EULA)) {
            Intent i = new Intent(getActivity(), LegalPreviewActivity.class);
            i.putExtra(LegalPreviewActivity.URL_INTENT_EXTRA, mEulaUrl);
            startActivity(i);
            return true;
        } else if(preference.getKey().equals(PreferenceKeys.PREF_LEGAL_PRIVACY_POLICY)) {
            Intent i = new Intent(getActivity(), LegalPreviewActivity.class);
            i.putExtra(LegalPreviewActivity.URL_INTENT_EXTRA, mPrivacyPolicyUrl);
            startActivity(i);
            return true;
        }
        return false;
    }
}
