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
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.android.browser.AutoFillSettingsFragment;
import com.android.browser.BrowserPreferencesPage;
import com.android.browser.BrowserSettings;
import com.android.browser.PreferenceKeys;
import com.android.browser.R;
import com.android.browser.UrlUtils;
import com.android.browser.homepages.HomeProvider;
import com.android.browser.mdm.AutoFillRestriction;
import com.android.browser.mdm.SearchEngineRestriction;

import org.codeaurora.swe.PermissionsServiceFactory;

public class GeneralPreferencesFragment extends SWEPreferenceFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    static final String TAG = "GeneralPreferencesFragment";

    public static final String EXTRA_CURRENT_PAGE = "currentPage";

    static final String BLANK_URL = "about:blank";
    static final String CURRENT = "current";
    static final String BLANK = "blank";
    static final String DEFAULT = "default";
    static final String MOST_VISITED = "most_visited";
    static final String OTHER = "other";

    static final String PREF_HOMEPAGE_PICKER = "homepage_picker";
    static final String PREF_POWERSAVE = "powersave_enabled";

    String[] mChoices, mValues;
    String mCurrentPage;

    AdvancedPreferencesFragment mAdvFrag = null;
    PrivacySecurityPreferencesFragment mPrivFrag = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Resources res = getActivity().getResources();
        mChoices = res.getStringArray(R.array.pref_homepage_choices);
        mValues = res.getStringArray(R.array.pref_homepage_values);
        mCurrentPage = getActivity().getIntent().getStringExtra(EXTRA_CURRENT_PAGE);

        // Load the XML preferences file
        addPreferencesFromResource(R.xml.general_preferences);

        ListPreference pref = (ListPreference) findPreference(PREF_HOMEPAGE_PICKER);
        pref.setSummary(getHomepageSummary());
        pref.setPersistent(false);
        pref.setValue(getHomepageValue());
        pref.setOnPreferenceChangeListener(this);

        PreferenceScreen autofill = (PreferenceScreen) findPreference(
                PreferenceKeys.PREF_AUTOFILL_PROFILE);
        autofill.setOnPreferenceClickListener(this);

        SwitchPreference powersave = (SwitchPreference) findPreference(PREF_POWERSAVE);
        powersave.setOnPreferenceChangeListener(this);

        SwitchPreference nightmode = (SwitchPreference) findPreference(
                PreferenceKeys.PREF_NIGHTMODE_ENABLED);
        nightmode.setOnPreferenceChangeListener(this);

        final Bundle arguments = getArguments();
        if (arguments != null && arguments.getBoolean("LowPower")) {
            LowPowerDialogFragment fragment = LowPowerDialogFragment.newInstance();
            fragment.show(getActivity().getFragmentManager(), "setPowersave dialog");
        }

        //Disable set search engine preference if SEARCH_ENGINE restriction is enabled
        if (SearchEngineRestriction.getInstance().isEnabled()) {
            findPreference(PreferenceKeys.PREF_SEARCH_ENGINE).setEnabled(false);
        }

        // Register Preference objects with their MDM restriction handlers
        AutoFillRestriction.getInstance().
                registerPreference(findPreference(PreferenceKeys.PREF_AUTOFILL_ENABLED));

        mAdvFrag = new AdvancedPreferencesFragment(this);
        // reset the search engine based on locale
        pref = (ListPreference) findPreference(PreferenceKeys.PREF_SEARCH_ENGINE);
        String search_engine = BrowserSettings.getInstance().getUserSearchEngine();
        pref.setValue((String) search_engine);
        pref.setSummary(pref.getEntry());
        //mPrivFrag = new PrivacySecurityPreferencesFragment(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Un-register Preference objects from their MDM restriction handlers
        AutoFillRestriction.getInstance().registerPreference(null);
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object objValue) {
        if (getActivity() == null) {
            // We aren't attached, so don't accept preferences changes from the
            // invisible UI.
            Log.w("PageContentPreferencesFragment", "onPreferenceChange called from detached fragment!");
            return false;
        }

        if (pref.getKey().equals(PREF_HOMEPAGE_PICKER)) {
            BrowserSettings settings = BrowserSettings.getInstance();
            if (CURRENT.equals(objValue)) {
                settings.setHomePage(mCurrentPage);
            }
            if (BLANK.equals(objValue)) {
                settings.setHomePage(BLANK_URL);
            }
            if (DEFAULT.equals(objValue)) {
                settings.setHomePage(BrowserSettings.getFactoryResetHomeUrl(
                        getActivity()));
            }
            if (MOST_VISITED.equals(objValue)) {
                settings.setHomePage(HomeProvider.MOST_VISITED);
            }
            if (OTHER.equals(objValue)) {
                promptForHomepage();
                return false;
            }
            pref.setSummary(getHomepageSummary());
            ((ListPreference)pref).setValue(getHomepageValue());
            return false;
        }

        if (pref.getKey().equals(PREF_POWERSAVE)) {
                BrowserSettings settings = BrowserSettings.getInstance();
                settings.setPowerSaveModeEnabled((Boolean)objValue);
                PermissionsServiceFactory.setDefaultPermissions(
                        PermissionsServiceFactory.PermissionType.WEBREFINER, !(Boolean) objValue);
                BrowserPreferencesPage.sResultExtra = PreferenceKeys.ACTION_RELOAD_PAGE;
                restartGello(getActivity(), (Boolean) objValue);
        }

        if (pref.getKey().equals(PreferenceKeys.PREF_NIGHTMODE_ENABLED)) {
            BrowserPreferencesPage.sResultExtra = PreferenceKeys.ACTION_RELOAD_PAGE;
        }
        return true;
    }

    void promptForHomepage() {
        MyAlertDialogFragment fragment = MyAlertDialogFragment.newInstance();
        fragment.setTargetFragment(this, -1);
        fragment.show(getActivity().getFragmentManager(), "setHomepage dialog");
    }

    String getHomepageValue() {
        BrowserSettings settings = BrowserSettings.getInstance();
        String homepage = settings.getHomePage();
        if (TextUtils.isEmpty(homepage) || BLANK_URL.endsWith(homepage)) {
            return BLANK;
        }
        if (HomeProvider.MOST_VISITED.equals(homepage)) {
            return MOST_VISITED;
        }
        String defaultHomepage = BrowserSettings.getFactoryResetHomeUrl(
                getActivity());
        if (TextUtils.equals(defaultHomepage, homepage)) {
            return DEFAULT;
        }
        if (TextUtils.equals(mCurrentPage, homepage)) {
            return CURRENT;
        }
        return OTHER;
    }

    String getHomepageSummary() {
        BrowserSettings settings = BrowserSettings.getInstance();
        if (settings.useMostVisitedHomepage()) {
            return getHomepageLabel(MOST_VISITED);
        }
        String homepage = settings.getHomePage();
        if (TextUtils.isEmpty(homepage) || BLANK_URL.equals(homepage)) {
            return getHomepageLabel(BLANK);
        }
        return homepage;
    }

    String getHomepageLabel(String value) {
        for (int i = 0; i < mValues.length; i++) {
            if (value.equals(mValues[i])) {
                return mChoices[i];
            }
        }
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();

        mAdvFrag.onResume();
        refreshUi();
    }

    void refreshUi() {
        ActionBar bar = getActivity().getActionBar();
        if (bar != null) {
            bar.setTitle(R.string.menu_preferences);
            bar.setDisplayHomeAsUpEnabled(true);
        }

        PreferenceScreen autoFillSettings =
                (PreferenceScreen)findPreference(PreferenceKeys.PREF_AUTOFILL_PROFILE);
        autoFillSettings.setDependency(PreferenceKeys.PREF_AUTOFILL_ENABLED);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals(PreferenceKeys.PREF_AUTOFILL_PROFILE)) {
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            Fragment newFragment = new AutoFillSettingsFragment();
            fragmentTransaction.replace(getId(), newFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
            return true;
        }
        return false;
    }

    private void restartGello(final Context context, boolean toggle) {
            String toastInfo;
            toastInfo = toggle ?
                context.getResources().getString(R.string.powersave_dialog_on) :
                context.getResources().getString(R.string.powersave_dialog_off);
            Toast.makeText(context, toastInfo, Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d("Gello", "Power save mode changed, restarting...");
                Intent restartIntent = context.getPackageManager()
                        .getLaunchIntentForPackage(context.getPackageName());
                PendingIntent intent = PendingIntent.getActivity(
                        context, 0, restartIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                manager.set(AlarmManager.RTC, System.currentTimeMillis() +  1, intent);
                System.exit(2);
            }
        }, 1500);
    }

    /*
      Add this class to manage AlertDialog lifecycle.
    */
    public static class MyAlertDialogFragment extends DialogFragment {
        private  final String HOME_PAGE = "homepage";
        private EditText editText = null;
        public static MyAlertDialogFragment newInstance() {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final BrowserSettings settings = BrowserSettings.getInstance();
            editText = new EditText(getActivity());
            String homePage = savedInstanceState != null ?
                    savedInstanceState.getString(HOME_PAGE): settings.getHomePage();
            editText.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_URI);
            editText.setText(homePage);
            editText.setSelectAllOnFocus(true);
            editText.setSingleLine(true);
            editText.setImeActionLabel(null, EditorInfo.IME_ACTION_DONE);
            final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setView(editText)
                    .setPositiveButton(android.R.string.ok, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String homepage = editText.getText().toString().trim();
                            homepage = UrlUtils.smartUrlFilter(homepage);
                            settings.setHomePage(homepage);
                            Fragment frag = getTargetFragment();
                            if (frag == null || !(frag instanceof GeneralPreferencesFragment)) {
                                Log.e("MyAlertDialogFragment", "get target fragment error!");
                                return;
                            }
                            GeneralPreferencesFragment target = (GeneralPreferencesFragment)frag;
                            ListPreference pref = (ListPreference) target.
                                    findPreference(PREF_HOMEPAGE_PICKER);
                            pref.setValue(target.getHomepageValue());
                            pref.setSummary(target.getHomepageSummary());
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .setTitle(R.string.pref_set_homepage_to)
                    .create();

            editText.setOnEditorActionListener(new OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                        return true;
                    }
                    return false;
                }
            });

            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

            return dialog;
        }

        @Override
        public void onSaveInstanceState(Bundle outState){
            super.onSaveInstanceState(outState);
            outState.putString(HOME_PAGE, editText.getText().toString().trim());
        }
    }
    public static class LowPowerDialogFragment extends DialogFragment {
        public static LowPowerDialogFragment newInstance() {
            LowPowerDialogFragment frag = new LowPowerDialogFragment();
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final BrowserSettings settings = BrowserSettings.getInstance();
            final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setPositiveButton(android.R.string.ok, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            settings.setPowerSaveModeEnabled(true);
                            getActivity().finish();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            getActivity().finish();
                        }
                    })
                    .setTitle(R.string.pref_powersave_enabled_summary)
                    .create();

            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

            return dialog;
        }
    }
}
