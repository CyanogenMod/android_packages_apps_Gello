/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

class BrowserYesNoPreference extends DialogPreference {
    private SharedPreferences mPrefs;
    private Context mContext;

    // This is the constructor called by the inflater
    public BrowserYesNoPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mContext = context;
    }

    @Override
    protected View onCreateView(ViewGroup group) {
        View child = super.onCreateView(group);
        View titleView = child.findViewById(android.R.id.title);
        if (titleView instanceof Button) {
            Button btn = (Button) titleView;
            final BrowserYesNoPreference pref = this;
            btn.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            pref.onClick();
                        }
                    }
            );
        }

        return child;
    }

    @Override
    protected void onClick() {
        super.onClick();
    }

    @Override
    protected View onCreateDialogView() {
        if (PreferenceKeys.PREF_CLEAR_SELECTED_DATA.equals(getKey())) {
            String dialogMessage = mContext.getString(R.string.pref_privacy_clear_selected_dlg);
            boolean itemSelected = false;

            if (mPrefs.getBoolean(PreferenceKeys.PREF_PRIVACY_CLEAR_CACHE, false)) {
                dialogMessage = dialogMessage.concat("\n\t" +
                        mContext.getString(R.string.pref_privacy_clear_cache));
                itemSelected = true;
            }
            if (mPrefs.getBoolean(PreferenceKeys.PREF_PRIVACY_CLEAR_COOKIES, false)) {
                dialogMessage = dialogMessage.concat("\n\t" +
                        mContext.getString(R.string.pref_privacy_clear_cookies));
                itemSelected = true;
            }
            if (mPrefs.getBoolean(PreferenceKeys.PREF_PRIVACY_CLEAR_HISTORY, false)) {
                dialogMessage = dialogMessage.concat("\n\t" +
                        mContext.getString(R.string.pref_privacy_clear_history));
                itemSelected = true;
            }
            if (mPrefs.getBoolean(PreferenceKeys.PREF_PRIVACY_CLEAR_FORM_DATA, false)) {
                dialogMessage = dialogMessage.concat("\n\t" +
                        mContext.getString(R.string.pref_privacy_clear_form_data));
                itemSelected = true;
            }
            if (mPrefs.getBoolean(PreferenceKeys.PREF_PRIVACY_CLEAR_PASSWORDS, false)) {
                dialogMessage = dialogMessage.concat("\n\t" +
                        mContext.getString(R.string.pref_privacy_clear_passwords));
                itemSelected = true;
            }
            if (mPrefs.getBoolean(PreferenceKeys.PREF_PRIVACY_CLEAR_GEOLOCATION_ACCESS,
                    false)) {
                dialogMessage = dialogMessage.concat("\n\t" +
                        mContext.getString(R.string.pref_privacy_clear_geolocation_access));
                itemSelected = true;
            }

            if (!itemSelected) {
                setDialogMessage(R.string.pref_select_items);
            } else {
                setDialogMessage(dialogMessage);
            }
        }

        return super.onCreateDialogView();
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (!positiveResult)
            return;

        if (callChangeListener(positiveResult)) {
            setEnabled(false);
            BrowserSettings settings = BrowserSettings.getInstance();
            if (PreferenceKeys.PREF_CLEAR_SELECTED_DATA.equals(getKey())) {
                if (mPrefs.getBoolean(PreferenceKeys.PREF_PRIVACY_CLEAR_CACHE, false)) {
                    settings.clearCache();
                    settings.clearDatabases();
                }
                if (mPrefs.getBoolean(PreferenceKeys.PREF_PRIVACY_CLEAR_COOKIES, false)) {
                    settings.clearCookies();
                }
                if (mPrefs.getBoolean(PreferenceKeys.PREF_PRIVACY_CLEAR_HISTORY, false)) {
                    settings.clearHistory();
                }
                if (mPrefs.getBoolean(PreferenceKeys.PREF_PRIVACY_CLEAR_FORM_DATA, false)) {
                    settings.clearFormData();
                }
                if (mPrefs.getBoolean(PreferenceKeys.PREF_PRIVACY_CLEAR_PASSWORDS, false)) {
                    settings.clearPasswords();
                }
                if (mPrefs.getBoolean(PreferenceKeys.PREF_PRIVACY_CLEAR_GEOLOCATION_ACCESS,
                        false)) {
                    settings.clearLocationAccess();
                }

                setEnabled(true);
            } else if (PreferenceKeys.PREF_RESET_DEFAULT_PREFERENCES.equals(
                    getKey())) {
                settings.resetDefaultPreferences();
                setEnabled(true);
            }
        }
    }
}
