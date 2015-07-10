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
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.text.TextUtils;
import android.view.View;
import android.webkit.ValueCallback;
import android.widget.Button;
import android.widget.TextView;

import com.android.browser.NavigationBarBase;
import com.android.browser.PageDialogsHandler;
import com.android.browser.R;

import org.codeaurora.swe.PermissionsServiceFactory;
import org.codeaurora.swe.WebRefiner;
import org.codeaurora.swe.util.ColorUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SiteSpecificPreferencesFragment extends SWEPreferenceFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener,
        View.OnClickListener {
    public static final String EXTRA_SITE = "website";
    public static final String EXTRA_ORIGIN = "website_origin";
    public static final String EXTRA_FAVICON = "website_favicon";
    public static final String EXTRA_WEB_REFINER_INFO = "website_refiner_info";
    public static final String EXTRA_SECURITY_CERT = "website_security_cert";
    public static final String EXTRA_SECURITY_CERT_ERR = "website_security_cert_err";

    private PermissionsServiceFactory.PermissionsService.OriginInfo mOriginInfo;
    private PermissionsServiceFactory.PermissionsService mPermServ;
    private ActionBar mBar;
    private List<String> mLocationValues;

    private boolean mUsingDefaultSettings = true;
    private int mOriginalActionBarOptions;
    private int mIconColor = 0;

    private SslCertificate mSslCert;
    private SslError mSslError;

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
        }

        public void setResource(ViewType type, View parent, int resId) {
            String text = mTexts.get(type);
            mViews.remove(type);
            mViews.put(type, new SiteSecurityView(parent, resId, text));
        }
    }

    private SiteSecurityViewFactory mSecurityViews;

    private String mOriginText;

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

                    pref.setTitle(mOriginText);
                    try {
                        URL url = new URL(mOriginText);
                        pref.setSummary("(" + url.getHost() + ")");
                    } catch (MalformedURLException e) {
                    }
                    mOriginInfo = mPermServ.getOriginInfo(mOriginText);
                    setActionBarTitle(PermissionsServiceFactory.getPrettyUrl(mOriginText));
                    updatePreferenceInfo();
                }
            }
        );

        Bundle parcel = args.getParcelable(EXTRA_SECURITY_CERT);
        mSslCert = (parcel != null) ? SslCertificate.restoreState(parcel) : null;

        if (mSslCert != null) {
            Preference pref = findPreference("site_security_info");
            if (pref != null) {
                pref.setSelectable(true);
            }

            int certErrors = args.getInt(EXTRA_SECURITY_CERT_ERR, 0);

            if (certErrors == 0) {
                mSecurityViews.appendText(SiteSecurityViewFactory.ViewType.INFO,
                        "Valid SSL Certificate. ");
            } else {
                mSslError = new SslError(-1, mSslCert, mOriginText);

                if ((certErrors & (1 << SslError.SSL_DATE_INVALID)) != 0) {
                    mSslError.addError(SslError.SSL_DATE_INVALID);
                    mSecurityViews.appendText(SiteSecurityViewFactory.ViewType.ERROR,
                            "Invalid SSL Certificate. ");
                }

                if ((certErrors & (1 << SslError.SSL_EXPIRED)) != 0) {
                    mSslError.addError(SslError.SSL_EXPIRED);
                    mSecurityViews.appendText(SiteSecurityViewFactory.ViewType.ERROR,
                            "Invalid SSL Certificate. ");
                }

                if ((certErrors & (1 << SslError.SSL_IDMISMATCH)) != 0) {
                    mSslError.addError(SslError.SSL_IDMISMATCH);
                    mSecurityViews.appendText(SiteSecurityViewFactory.ViewType.ERROR,
                            "Invalid SSL Certificate. ");
                }

                if ((certErrors & (1 << SslError.SSL_INVALID)) != 0) {
                    mSslError.addError(SslError.SSL_INVALID);
                    mSecurityViews.appendText(SiteSecurityViewFactory.ViewType.ERROR,
                            "Invalid SSL Certificate. ");
                }

                if ((certErrors & (1 << SslError.SSL_NOTYETVALID)) != 0) {
                    mSslError.addError(SslError.SSL_NOTYETVALID);
                    mSecurityViews.appendText(SiteSecurityViewFactory.ViewType.WARNING,
                            "SSL Certificate warnings. ");
                }

                if ((certErrors & (1 << SslError.SSL_UNTRUSTED)) != 0) {
                    mSslError.addError(SslError.SSL_UNTRUSTED);
                    mSecurityViews.appendText(SiteSecurityViewFactory.ViewType.WARNING,
                            "SSL Certificate warnings. ");
                }
            }
        }

        int adBlocks = args.getInt(EXTRA_WEB_REFINER_INFO, 0);
        if (adBlocks > 0) {
            mSecurityViews.appendText(SiteSecurityViewFactory.ViewType.INFO,
                     getString(R.string.pref_web_refiner_blocked) + " " + adBlocks + " " +
                            getString(R.string.pref_web_refiner_advertisements));
        }
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
        String warningText = new String("");

        long permission = showPermission("select_geolocation",
                PermissionsServiceFactory.PermissionType.GEOLOCATION,
                R.string.pref_security_ask_before_using, R.string.pref_security_not_allowed);

        if (PermissionsServiceFactory.Permission.ALLOW == permission) {
            warningText = getString(R.string.pref_privacy_enable_geolocation);
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
            warningText = getString(R.string.pref_privacy_enable_geolocation);
            geolocation_pref.setValueIndex(1);
        } else if (permission == PermissionsServiceFactory.Permission.ALLOW) {
            geolocation_pref.setValueIndex(2);
        }

        permission = showPermission("microphone", PermissionsServiceFactory.PermissionType.VOICE,
                R.string.pref_security_ask_before_using, R.string.pref_security_not_allowed);

        if (PermissionsServiceFactory.Permission.ALLOW == permission) {
            if (!warningText.isEmpty()) {
                warningText += ", ";
            }
            warningText += getString(R.string.pref_security_allow_mic);
        }

        permission = showPermission("camera", PermissionsServiceFactory.PermissionType.VIDEO,
                R.string.pref_security_ask_before_using, R.string.pref_security_not_allowed);
        if (PermissionsServiceFactory.Permission.ALLOW == permission) {
            if (!warningText.isEmpty()) {
                warningText += ", ";
            }
            warningText += getString(R.string.pref_security_allow_camera);
        }

        if (!warningText.isEmpty()) {
            warningText += " ";
            warningText += getResources().getString(R.string.pref_security_access_is_allowed);
            mSecurityViews.appendText(SiteSecurityViewFactory.ViewType.WARNING, warningText);
        }

        permission = showPermission("distracting_contents",
                PermissionsServiceFactory.PermissionType.WEBREFINER,
                R.string.pref_security_allowed, R.string.pref_security_not_allowed);
        pref = findPreference("distracting_contents");
        if (permission == PermissionsServiceFactory.Permission.BLOCK) {
            ((TwoStatePreference) pref).setChecked(true);
        } else {
            ((TwoStatePreference) pref).setChecked(false);
        }

        showPermission("popup_windows", PermissionsServiceFactory.PermissionType.POPUP,
                R.string.pref_security_allowed, R.string.pref_security_not_allowed);

        showPermission("accept_cookies", PermissionsServiceFactory.PermissionType.COOKIE,
                R.string.pref_security_allowed, R.string.pref_security_not_allowed);

        if (!mUsingDefaultSettings && mBar != null) {
            mBar.getCustomView().setVisibility(View.VISIBLE);
        }

        if (mSecurityViews.mbEmpty) {
            PreferenceScreen screen = (PreferenceScreen)
                    findPreference("site_specific_prefs");

            pref = findPreference("site_security_info_title");
            if (pref != null && screen != null) {
                screen.removePreference(pref);
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();
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
                        mBar.setBackgroundDrawable(new ColorDrawable(color));
                        NavigationBarBase.setStatusAndNavigationBarColor(getActivity(),
                                NavigationBarBase.adjustColor(color, 1, 1, 0.7f));
                    }
                } else {
                    if (mIconColor != 0) {
                        mBar.setBackgroundDrawable(new ColorDrawable(mIconColor));
                        NavigationBarBase.setStatusAndNavigationBarColor(getActivity(),
                                NavigationBarBase.adjustColor(mIconColor, 1, 1, 0.7f));
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
            NavigationBarBase.setStatusAndNavigationBarColor(getActivity(),
                    NavigationBarBase.getDefaultStatusBarColor());
        }
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
                            R.string.website_settings_clear_all_dialog_ok_button,
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

                                        finish();
                                    }
                                }
                            })
                        .setNegativeButton(
                                R.string.website_settings_clear_all_dialog_cancel_button, null)
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
            PageDialogsHandler.createSslCertificateDialog(getActivity(),mSslCert, mSslError)
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
            updateTwoStatePreference(pref,
                    PermissionsServiceFactory.PermissionType.WEBREFINER, (boolean)objValue);
        } else if (pref.getKey().toString().equalsIgnoreCase("popup_windows")) {
            updateTwoStatePreference(pref,
                    PermissionsServiceFactory.PermissionType.POPUP, (boolean)objValue);
        } else if (pref.getKey().toString().equalsIgnoreCase("accept_cookies")) {
            updateTwoStatePreference(pref,
                    PermissionsServiceFactory.PermissionType.COOKIE, (boolean)objValue);
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        if (pref.getKey().toString().equalsIgnoreCase("clear_data")) {
            new AlertDialog.Builder(getActivity())
                .setMessage(R.string.website_settings_clear_all_dialog_message)
                .setPositiveButton(R.string.website_settings_clear_all_dialog_ok_button,
                        new AlertDialog.OnClickListener() {
                            public void onClick(DialogInterface dlg, int which) {
                                if (mOriginInfo != null) {
                                    mOriginInfo.clearAllStoredData();
                                    Preference e = findPreference("clear_data");
                                    e.setSummary("(Empty)");
                                }
                            }
                        })
                .setNegativeButton(R.string.website_settings_clear_all_dialog_cancel_button, null)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .show();
        }
        return true;
    }

}
