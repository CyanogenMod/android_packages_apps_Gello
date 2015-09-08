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

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.http.SslCertificate;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.ValueCallback;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.browser.BrowserLocationListPreference;
import com.android.browser.BrowserPreferencesPage;
import com.android.browser.BrowserSettings;
import com.android.browser.NavigationBarBase;
import com.android.browser.PreferenceKeys;
import com.android.browser.R;
import com.android.browser.reflect.ReflectHelper;

import org.codeaurora.swe.PermissionsServiceFactory;
import org.codeaurora.swe.WebRefiner;
import org.codeaurora.swe.util.ColorUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Formatter;
import java.util.List;
import java.util.Map;

public class SiteSpecificPreferencesFragment extends SWEPreferenceFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener,
        View.OnClickListener {
    public static final String EXTRA_SITE = "website";
    public static final String EXTRA_ORIGIN = "website_origin";
    public static final String EXTRA_FAVICON = "website_favicon";
    public static final String EXTRA_SITE_TITLE = "website_title";
    public static final String EXTRA_WEB_REFINER_ADS_INFO = "website_refiner_ads_info";
    public static final String EXTRA_WEB_REFINER_TRACKER_INFO = "website_refiner_tracker_info";
    public static final String EXTRA_WEB_REFINER_MALWARE_INFO = "website_refiner_malware_info";
    public static final String EXTRA_SECURITY_CERT = "website_security_cert";
    public static final String EXTRA_SECURITY_CERT_BAD = "website_security_cert_bad";
    public static final String EXTRA_SECURITY_CERT_MIXED = "website_security_cert_mixed";

    private PermissionsServiceFactory.PermissionsService.OriginInfo mOriginInfo;
    private PermissionsServiceFactory.PermissionsService mPermServ;
    private ActionBar mBar;
    private List<String> mLocationValues;

    private Preference mSecurityInfoPrefs;

    private boolean mUsingDefaultSettings = true;
    private int mOriginalActionBarOptions;
    private int mIconColor = 0;

    private SslCertificate mSslCert;
    private int mSslState;

    private static class SiteSecurityViewFactory {
        private class SiteSecurityView {
            private TextView mTextView;
            private View mContainer;
            private String mDisplayText;

            public SiteSecurityView(View parent, int resId, String text) {
                mContainer = parent.findViewById(resId);
                mTextView = (TextView) mContainer.findViewById(R.id.security_view_text);
                mTextView.setText(text);
                mDisplayText = text;
                updateVisibility();
            }

            private void updateVisibility() {
                if (TextUtils.isEmpty(mDisplayText)) {
                    mContainer.setVisibility(View.GONE);
                } else {
                    mContainer.setVisibility(View.VISIBLE);
                }
            }

            public void setText(String text) {
                mDisplayText = text;
                mTextView.setText(mDisplayText);
                updateVisibility();
            }

            public void clearText() {
                mDisplayText = null;
                updateVisibility();
            }
        }

        public enum ViewType{
            ERROR,
            WARNING,
            INFO
        };

        private Map<ViewType, SiteSecurityView> mViews =
                new EnumMap<ViewType, SiteSecurityView>(ViewType.class);
        private Map<ViewType, String> mTexts = new EnumMap<ViewType, String>(ViewType.class);

        private boolean mbEmpty = true;

        public void setText(ViewType type, String text) {
            mTexts.put(type, text);

            SiteSecurityView view = mViews.get(type);
            if (view != null) {
                view.setText(text);
            }

            mbEmpty = false;
        }

        public void appendText(ViewType type, String text) {
            String new_text = mTexts.get(type);
            if (new_text != null)
                new_text += text;
            else
                new_text = text;

            mTexts.put(type, new_text);

            SiteSecurityView view = mViews.get(type);
            if (view != null) {
                view.setText(new_text);
            }

            mbEmpty = false;
        }

        public void clearText(ViewType type) {
            mTexts.remove(type);

            SiteSecurityView view = mViews.get(type);
            if (view != null) {
                view.clearText();
            }

            boolean empty = true;
            for (Map.Entry<ViewType, String> entry: mTexts.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    empty = false;
                }
            }
            mbEmpty = empty;
        }

        public void setResource(ViewType type, View parent, int resId) {
            String text = mTexts.get(type);
            mViews.remove(type);
            mViews.put(type, new SiteSecurityView(parent, resId, text));
        }
    }

    private SiteSecurityViewFactory mSecurityViews;

    private String mOriginText;
    private String mSiteTitle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.site_specific_preferences);

        mBar = getActivity().getActionBar();

        mLocationValues = Arrays.asList(
                getResources().getStringArray(R.array.geolocation_settings_choices));

        mSecurityViews = new SiteSecurityViewFactory();

        Bundle args = getArguments();
        if (args != null) {
            mOriginText = args.getString(EXTRA_ORIGIN, null);
            mSiteTitle = args.getString(EXTRA_SITE_TITLE, null);

            if (mOriginText == null) {
                mOriginText = args.getString(EXTRA_SITE);
            }
        }

        mIconColor = NavigationBarBase.getSiteIconColor(mOriginText);

        PermissionsServiceFactory.getPermissionsService(
            new ValueCallback<PermissionsServiceFactory.PermissionsService>() {
                @Override
                public void onReceiveValue(PermissionsServiceFactory.PermissionsService value) {
                    mPermServ = value;
                    Preference pref = findPreference("site_name");

                    pref.setTitle((mSiteTitle != null) ?
                            mSiteTitle :
                            mOriginText);

                    try {
                        URL url = new URL(mOriginText);
                        pref.setSummary((mSiteTitle != null) ?
                                mOriginText :
                                "(" + url.getHost() + ")");
                    } catch (MalformedURLException e) {
                    }
                    mOriginInfo = mPermServ.getOriginInfo(mOriginText);
                    setActionBarTitle(PermissionsServiceFactory.getPrettyUrl(mOriginText));
                    updatePreferenceInfo();
                }
            }
        );

        if (!BrowserSettings.getInstance().getPreferences()
                .getBoolean(PreferenceKeys.PREF_WEB_REFINER, false)) {
            PreferenceCategory category = (PreferenceCategory) findPreference("site_pref_list");
            if (category != null) {
                Preference pref = findPreference("distracting_contents");
                category.removePreference(pref);
            }
        }

        int ads = args.getInt(EXTRA_WEB_REFINER_ADS_INFO, 0);
        String[] strings = new String[3];
        int index = 0;

        if (ads > 0) {
            strings[index++] = getResources().getQuantityString(
                    R.plurals.pref_web_refiner_advertisements, ads, ads);
        }

        int trackers = args.getInt(EXTRA_WEB_REFINER_TRACKER_INFO, 0);
        if (trackers > 0) {
            strings[index++] = getResources().getQuantityString(
                    R.plurals.pref_web_refiner_trackers, trackers, trackers);

        }

        int malware = args.getInt(EXTRA_WEB_REFINER_MALWARE_INFO, 0);
        if (malware > 0) {
            strings[index++] = getResources().getQuantityString(
                    R.plurals.pref_web_refiner_malware, malware, malware);
        }

        if (index > 0) {
            String[] formats = getResources().getStringArray(R.array.pref_web_refiner_message);
            Formatter formatter = new Formatter();
            formatter.format(formats[index - 1], strings[0], strings[1], strings[2]);
            mSecurityViews.appendText(SiteSecurityViewFactory.ViewType.INFO, formatter.toString());
        }

        Bundle parcel = args.getParcelable(EXTRA_SECURITY_CERT);
        mSslCert = (parcel != null) ? SslCertificate.restoreState(parcel) : null;

        if (mSslCert != null) {
            Preference pref = findPreference("site_security_info");
            if (pref != null) {
                pref.setSelectable(true);
            }

            boolean certBad = args.getBoolean(EXTRA_SECURITY_CERT_BAD, false);
            boolean certMix = args.getBoolean(EXTRA_SECURITY_CERT_MIXED, false);
            if (!certBad && !certMix) {
                final String string = getString(R.string.pref_valid_cert);
                mSecurityViews.appendText(SiteSecurityViewFactory.ViewType.INFO,
                        string);
                mSslState = 0;
            } else if (certMix) {
                mSecurityViews.appendText(SiteSecurityViewFactory.ViewType.WARNING,
                        getString(R.string.pref_warning_cert));
                mSslState = 1;
            } else {
                mSecurityViews.appendText(SiteSecurityViewFactory.ViewType.ERROR,
                        getString(R.string.pref_invalid_cert));
                mSslState = 2;
            }
        }

        updateSecurityViewVisibility();
    }

    private AlertDialog.Builder createSslCertificateDialog(Context ctx,
                                                           SslCertificate certificate) {
        Object[] params = {ctx};
        Class[] type = new Class[] {Context.class};
        View certificateView = (View) ReflectHelper.invokeMethod(certificate,
                "inflateCertificateView", type, params);
        Resources res = Resources.getSystem();
        // load 'android.R.placeholder' via introspection, since it's not a public resource ID
        int placeholder_id = res.getIdentifier("placeholder", "id", "android");
        final LinearLayout placeholder =
                (LinearLayout)certificateView.findViewById(placeholder_id);

        LayoutInflater factory = LayoutInflater.from(ctx);
        int iconId = R.drawable.ic_cert_trusted;
        TextView textView;

        switch (mSslState) {
            case 0:
                iconId = R.drawable.ic_cert_trusted;
                LinearLayout table = (LinearLayout)factory.inflate(R.layout.ssl_success, placeholder);
                textView = (TextView)table.findViewById(R.id.success);
                textView.setText(R.string.ssl_certificate_is_valid);
                break;
            case 1:
                iconId = R.drawable.ic_cert_untrusted;
                textView = (TextView) factory.inflate(R.layout.ssl_warning, placeholder, false);
                textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_sp_level_warning,
                        0, 0, 0);
                textView.setText(R.string.ssl_unknown);
                placeholder.addView(textView);
                break;
            case 2:
                iconId = R.drawable.ic_cert_avoid;
                textView = (TextView) factory.inflate(R.layout.ssl_warning, placeholder, false);
                textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_sp_level_severe,
                        0, 0, 0);
                textView.setText(R.string.ssl_invalid);
                placeholder.addView(textView);
                break;
        }

        return new AlertDialog.Builder(ctx)
                .setTitle(R.string.ssl_certificate)
                .setIcon(iconId)
                .setView(certificateView);
    }

    private void setActionBarTitle(String url) {
        if (mBar != null) {
            mBar.setTitle("  " + url);
        }
    }

    private String getStorage() {
        if (mOriginInfo == null) {
            return new String("");
        }

        long value = mOriginInfo.getStoredData();
        if (value == 0) {
            return "Empty";
        }

        if (value < (1 << 10)) {
            return value + "B";
        } else if (value < (1 << 20)) {
            return (value >> 10) + "KB";
        } else if (value < (1 << 30)) {
            return (value >> 20) + "MB";
        }

        return (value >> 30) + "GB";
    }

    private long showPermission(CharSequence key, PermissionsServiceFactory.PermissionType type,
                                int defaultOnSummary, int defaultOffSummary) {
        Preference pref = findPreference(key);
        long permission = (mOriginInfo != null) ? mOriginInfo.getPermission(type) :
                PermissionsServiceFactory.Permission.NOTSET;

        pref.setOnPreferenceChangeListener(this);

        if (permission == PermissionsServiceFactory.Permission.ALLOW) {
            if (pref instanceof TwoStatePreference) {
                ((TwoStatePreference) pref).setChecked(true);
                ((TwoStatePreference) pref).setSummaryOn(R.string.pref_security_allowed);
            } else {
                pref.setSummary(R.string.pref_security_allowed);
            }
            mUsingDefaultSettings = false;
        } else if (permission == PermissionsServiceFactory.Permission.BLOCK) {
            if (pref instanceof TwoStatePreference) {
                ((TwoStatePreference) pref).setChecked(false);
            } else {
                pref.setSummary(R.string.pref_security_not_allowed);
            }
            mUsingDefaultSettings = false;
        } else if (permission == PermissionsServiceFactory.Permission.ASK) {
            if (pref instanceof TwoStatePreference) {
                ((TwoStatePreference) pref).setChecked(true);
                ((TwoStatePreference) pref).setSummaryOn(R.string.pref_security_ask_before_using);
            } else {
                pref.setSummary(R.string.pref_security_ask_before_using);
            }
            mUsingDefaultSettings = false;
        } else if (permission == PermissionsServiceFactory.Permission.NOTSET) {
            boolean defaultPerm = PermissionsServiceFactory.getDefaultPermissions(type);
            if (pref instanceof TwoStatePreference) {
                if (!defaultPerm) {
                    ((TwoStatePreference) pref).setChecked(false);
                    ((TwoStatePreference) pref).setSummaryOff(defaultOffSummary);
                    return PermissionsServiceFactory.Permission.BLOCK;
                } else {
                    ((TwoStatePreference) pref).setChecked(true);
                    ((TwoStatePreference) pref).setSummaryOn(defaultOnSummary);
                    return PermissionsServiceFactory.Permission.ASK;
                }
            } else {
                if (!defaultPerm) {
                    pref.setSummary(defaultOffSummary);
                    return PermissionsServiceFactory.Permission.BLOCK;
                } else {
                    pref.setSummary(defaultOnSummary);
                    return PermissionsServiceFactory.Permission.ASK;
                }
            }
        }
        return permission;
    }

    private void updateStorageInfo(Preference pref) {
        if (mOriginInfo != null) {
            pref.setTitle(R.string.webstorage_clear_data_title);
            pref.setSummary("(" + getStorage() + ")");
        }
    }

    private void updatePreferenceInfo() {
        Preference pref = findPreference("clear_data");
        updateStorageInfo(pref);
        pref.setOnPreferenceClickListener(this);
        String warningText = (mSslState == 1) ? getString(R.string.pref_warning_cert) + " " :
                new String("");
        boolean setting_warnings = false;

        long permission = showPermission("select_geolocation",
                PermissionsServiceFactory.PermissionType.GEOLOCATION,
                R.string.pref_security_ask_before_using, R.string.pref_security_not_allowed);

        if (PermissionsServiceFactory.Permission.ALLOW == permission) {
            warningText += getString(R.string.pref_privacy_enable_geolocation);
            setting_warnings = true;
        }

        ListPreference geolocation_pref = (ListPreference) findPreference("select_geolocation");
        geolocation_pref.setValueIndex(0);
        if (permission == PermissionsServiceFactory.Permission.CUSTOM) {
            pref = findPreference("select_geolocation");
            long custom = mOriginInfo.getPermissionCustomValue(
                    PermissionsServiceFactory.PermissionType.GEOLOCATION);
            float customHrs = ((float) custom) / (60 * 60);
            String customSummary = "Allowed for " + String.format("%.02f", customHrs) + " hours";
            if (pref instanceof TwoStatePreference) {
                ((TwoStatePreference) pref).setChecked(true);
                ((TwoStatePreference) pref).setSummaryOn(customSummary);
            } else {
                pref.setSummary(customSummary);
            }
            mUsingDefaultSettings = false;
            warningText += getString(R.string.pref_privacy_enable_geolocation);
            setting_warnings = true;
            geolocation_pref.setValueIndex(1);
        } else if (permission == PermissionsServiceFactory.Permission.ALLOW) {
            geolocation_pref.setValueIndex(2);
        }

        permission = showPermission("microphone", PermissionsServiceFactory.PermissionType.VOICE,
                R.string.pref_security_ask_before_using, R.string.pref_security_not_allowed);

        if (PermissionsServiceFactory.Permission.ALLOW == permission) {
            if (!warningText.isEmpty() && setting_warnings) {
                warningText += ", ";
            }
            warningText += getString(R.string.pref_security_allow_mic);
            setting_warnings = true;
        }

        permission = showPermission("camera", PermissionsServiceFactory.PermissionType.VIDEO,
                R.string.pref_security_ask_before_using, R.string.pref_security_not_allowed);
        if (PermissionsServiceFactory.Permission.ALLOW == permission) {
            if (!warningText.isEmpty() && setting_warnings) {
                warningText += ", ";
            }
            warningText += getString(R.string.pref_security_allow_camera);
            setting_warnings = true;
        }

        if (!warningText.isEmpty()) {
            if (setting_warnings) {
                warningText += " ";
                warningText += getResources().getString(R.string.pref_security_access_is_allowed);
            }
            mSecurityViews.setText(SiteSecurityViewFactory.ViewType.WARNING, warningText);
        } else {
            mSecurityViews.clearText(SiteSecurityViewFactory.ViewType.WARNING);
        }

        pref = findPreference("distracting_contents");
        if (pref != null) {
            permission = showPermission("distracting_contents",
                    PermissionsServiceFactory.PermissionType.WEBREFINER,
                    R.string.pref_security_allowed, R.string.pref_security_not_allowed);
            if (permission == PermissionsServiceFactory.Permission.BLOCK) {
                ((TwoStatePreference) pref).setChecked(true);
            } else {
                ((TwoStatePreference) pref).setChecked(false);
            }
        }

        showPermission("popup_windows", PermissionsServiceFactory.PermissionType.POPUP,
                R.string.pref_security_allowed, R.string.pref_security_not_allowed);

        showPermission("accept_cookies", PermissionsServiceFactory.PermissionType.COOKIE,
                R.string.pref_security_allowed, R.string.pref_security_not_allowed);

        if (!mUsingDefaultSettings && mBar != null) {
            mBar.getCustomView().setVisibility(View.VISIBLE);
        }

        updateSecurityViewVisibility();
    }

    private void updateSecurityViewVisibility() {
        if (mSecurityViews.mbEmpty) {
            PreferenceScreen screen = (PreferenceScreen)
                    findPreference("site_specific_prefs");

            if (mSecurityInfoPrefs == null) {
                mSecurityInfoPrefs = findPreference("site_security_info_title");
            }

            if (mSecurityInfoPrefs != null && screen != null) {
                screen.removePreference(mSecurityInfoPrefs);
            }
        } else {
            PreferenceScreen screen = (PreferenceScreen)
                    findPreference("site_specific_prefs");

            Preference pref = findPreference("site_security_info_title");
            if (pref == null && mSecurityInfoPrefs != null) {
                screen.addPreference(mSecurityInfoPrefs);
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        BrowserLocationListPreference pref =
                (BrowserLocationListPreference) findPreference("select_geolocation");
        if ( pref != null) pref.setEnabled(PermissionsServiceFactory.isSystemLocationEnabled());
        if (mBar != null) {
            mOriginalActionBarOptions = mBar.getDisplayOptions();
            mBar.setDisplayHomeAsUpEnabled(false);
            mBar.setHomeButtonEnabled(false);

            assignResetButton();

            Bundle args = getArguments();
            if (args != null) {
                byte[] data = args.getByteArray(EXTRA_FAVICON);
                if (data != null) {
                    Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
                    if (bm != null) {
                        Bitmap bitmap = Bitmap.createScaledBitmap(bm, 150, 150, true);
                        int color = ColorUtils.getDominantColorForBitmap(bitmap);

                        appendActionBarDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                                ActionBar.DISPLAY_SHOW_TITLE);
                        mBar.setHomeButtonEnabled(true);
                        mBar.setIcon(new BitmapDrawable(getResources(), bitmap));
                    }
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mBar != null) {
            mBar.setDisplayOptions(mOriginalActionBarOptions);
        }

        // flush all the settings in pause to assure that writes happen
        //  as soon the user leaves the activity
        PermissionsServiceFactory.flushPendingSettings();

    }

    private void appendActionBarDisplayOptions(int extraOptions) {
        int options = mBar.getDisplayOptions();
        options |= extraOptions;
        mBar.setDisplayOptions(options);
    }

    private void assignResetButton() {
        appendActionBarDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        mBar.setCustomView(R.layout.swe_preference_custom_actionbar);
        //mBar.getCustomView().setVisibility(View.GONE);
        Button btn = (Button) mBar.getCustomView().findViewById(R.id.reset);
        if (btn == null) {
            return;
        }

        btn.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.pref_extras_reset_default_dlg)
                        .setPositiveButton(
                            R.string.ok,
                            new AlertDialog.OnClickListener() {
                                public void onClick(DialogInterface dlg, int which) {
                                    if (mOriginInfo != null) {
                                        mOriginInfo.resetSitePermission();
                                        Preference e = findPreference("clear_data");
                                        e.setSummary("(Empty)");
                                        updatePreferenceInfo();

                                        WebRefiner refiner = WebRefiner.getInstance();
                                        if (refiner != null) {
                                            String[] origins = new String[1];
                                            origins[0] = mOriginInfo.getOrigin();
                                            refiner.useDefaultPermissionForOrigins(origins);
                                        }

                                        BrowserPreferencesPage.sResultExtra =
                                                PreferenceKeys.ACTION_RELOAD_PAGE;
                                        BrowserPreferencesPage.onUrlNeedsReload(mOriginText);
                                        finish();
                                    }
                                }
                            })
                        .setNegativeButton(
                                R.string.cancel, null)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .show();
                }
            }
        );
    }

    private void finish() {
        Activity activity = getActivity();
        if (activity != null) {
            getActivity().getFragmentManager().popBackStack();
        }
    }

    @Override
    public void onChildViewAddedToHierarchy(View parent, View child) {
        if (child.getId() == R.id.site_security_info) {
            mSecurityViews.setResource(SiteSecurityViewFactory.ViewType.ERROR,
                    child, R.id.site_security_error);
            mSecurityViews.setResource(SiteSecurityViewFactory.ViewType.WARNING,
                    child, R.id.site_security_warning);
            mSecurityViews.setResource(SiteSecurityViewFactory.ViewType.INFO,
                    child, R.id.site_security_verbose);

            if (mSslCert != null) {
                child.setOnClickListener(this);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.site_security_info) {
            createSslCertificateDialog(getActivity(), mSslCert)
                    .setPositiveButton(R.string.ok, null)
                    .show();
        }
    }

    private void updateTwoStatePreference(Preference pref,
                                          PermissionsServiceFactory.PermissionType type,
                                          boolean state) {
        if (state) {
            mOriginInfo.setPermission(type, PermissionsServiceFactory.Permission.ALLOW);
            ((TwoStatePreference)pref).setSummaryOn(R.string.pref_security_allowed);
        } else {
            mOriginInfo.setPermission(type, PermissionsServiceFactory.Permission.BLOCK);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object objValue) {
        if (mOriginInfo == null) {
            if (mOriginText != null) {
                mOriginInfo = mPermServ.addOriginInfo(mOriginText);
                if (mOriginInfo == null) {
                    mOriginInfo = mPermServ.getOriginInfo(mOriginText);
                    if (mOriginInfo == null) {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }
        if (pref.getKey().toString().equalsIgnoreCase("select_geolocation")) {
            int index = mLocationValues.indexOf(objValue.toString());
            switch (index) {
                case 0:
                    mOriginInfo.setPermission(PermissionsServiceFactory.PermissionType.GEOLOCATION,
                            PermissionsServiceFactory.Permission.BLOCK);
                    pref.setSummary(R.string.pref_security_not_allowed);
                    break;
                case 1:
                    mOriginInfo.setPermission(PermissionsServiceFactory.PermissionType.GEOLOCATION,
                            PermissionsServiceFactory.Permission.CUSTOM);
                    pref.setSummary(R.string.geolocation_permissions_prompt_share_for_limited_time);
                    break;
                case 2:
                    mOriginInfo.setPermission(PermissionsServiceFactory.PermissionType.GEOLOCATION,
                            PermissionsServiceFactory.Permission.ALLOW);
                    pref.setSummary(R.string.pref_security_allowed);
                    break;
                default:
                    break;
            }
        } else if (pref.getKey().toString().equalsIgnoreCase("microphone")) {
            updateTwoStatePreference(pref,
                    PermissionsServiceFactory.PermissionType.VOICE, (boolean)objValue);
        } else if (pref.getKey().toString().equalsIgnoreCase("camera")) {
            updateTwoStatePreference(pref,
                    PermissionsServiceFactory.PermissionType.VIDEO, (boolean)objValue);
        } else if (pref.getKey().toString().equalsIgnoreCase("distracting_contents")) {
            WebRefiner refiner = WebRefiner.getInstance();
            if (refiner != null) {
                boolean disable = (boolean) objValue;
                String[] origins = new String[1];
                origins[0] = mOriginInfo.getOrigin();
                refiner.setPermissionForOrigins(origins, !disable);
            }
            // Distracting contents and web refiner complimentary of each other
            updateTwoStatePreference(pref,
                    PermissionsServiceFactory.PermissionType.WEBREFINER, !(boolean)objValue);
        } else if (pref.getKey().toString().equalsIgnoreCase("popup_windows")) {
            updateTwoStatePreference(pref,
                    PermissionsServiceFactory.PermissionType.POPUP, (boolean)objValue);
        } else if (pref.getKey().toString().equalsIgnoreCase("accept_cookies")) {
            updateTwoStatePreference(pref,
                    PermissionsServiceFactory.PermissionType.COOKIE, (boolean)objValue);
        }
        BrowserPreferencesPage.sResultExtra = PreferenceKeys.ACTION_RELOAD_PAGE;
        BrowserPreferencesPage.onUrlNeedsReload(mOriginText);
        updatePreferenceInfo();
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        if (pref.getKey().toString().equalsIgnoreCase("clear_data")) {
            new AlertDialog.Builder(getActivity())
                .setMessage(R.string.website_settings_clear_all_dialog_message)
                .setPositiveButton(R.string.ok,
                        new AlertDialog.OnClickListener() {
                            public void onClick(DialogInterface dlg, int which) {
                                if (mOriginInfo != null) {
                                    mOriginInfo.clearAllStoredData();
                                    Preference e = findPreference("clear_data");
                                    e.setSummary("(Empty)");
                                    BrowserPreferencesPage.sResultExtra =
                                            PreferenceKeys.ACTION_RELOAD_PAGE;
                                    BrowserPreferencesPage.onUrlNeedsReload(mOriginText);
                                }
                            }
                        })
                .setNegativeButton(R.string.cancel, null)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .show();
        }
        return true;
    }

}
