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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class BrowserYesNoPreference extends DialogPreference {
    private SharedPreferences mPrefs;
    private Context mContext;
    private String mNeutralBtnTxt;
    private String mPositiveBtnTxt;
    private String mNegativeBtnTxt;
    private boolean mNeutralBtnClicked = false;

    public static final int CANCEL_BTN = 0;
    public static final int OK_BTN = 1;
    public static final int OTHER_BTN = 2;

    // This is the constructor called by the inflater
    public BrowserYesNoPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mContext = context;
        final TypedArray a = mContext.obtainStyledAttributes(attrs,
                R.styleable.BrowserYesNoPreference, 0, 0);
        mNeutralBtnTxt = a.getString(R.styleable.BrowserYesNoPreference_neutralButtonText);
        mPositiveBtnTxt = a.getString(R.styleable.BrowserYesNoPreference_positiveButtonText);
        mNegativeBtnTxt = a.getString(R.styleable.BrowserYesNoPreference_negativeButtonText);
        setDialogIcon(R.drawable.ic_sp_level_warning);
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
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        if (mNeutralBtnTxt != null) {
            builder.setNeutralButton(mNeutralBtnTxt, this);
        }

        if (mPositiveBtnTxt != null) {
            builder.setPositiveButton(mPositiveBtnTxt, this);
        }

        if (mNegativeBtnTxt != null) {
            builder.setNegativeButton(mNegativeBtnTxt, this);
        }
    }

    @Override
    protected void onClick() {
        super.onClick();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        mNeutralBtnClicked = DialogInterface.BUTTON_NEUTRAL == which;
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
                        mContext.getString(R.string.history));
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
        Integer result = (positiveResult) ? 1 : 0;

        if (mNeutralBtnTxt != null && mNeutralBtnClicked) {
            result = 2;
        }

        if (callChangeListener(result)) {
            setEnabled(false);
            if (PreferenceKeys.PREF_CLEAR_SELECTED_DATA.equals(getKey())) {
                BrowserSettings settings = BrowserSettings.getInstance();
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
            }
            setEnabled(true);
        }
    }
}
