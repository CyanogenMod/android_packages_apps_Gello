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

package com.android.browser;

import com.android.browser.R;

import java.util.Locale;

import android.os.Build;
import android.content.Context;
import android.text.TextUtils;

import org.codeaurora.swe.BrowserCommandLine;

abstract class BrowserConfigBase {


    private Context mContext;

    public BrowserConfigBase(Context context) {
        mContext = context;
    }

    public void overrideUserAgent() {
        // Check if the UA is already present using command line file
        if (BrowserCommandLine.hasSwitch(BrowserSwitches.OVERRIDE_USER_AGENT)) {
            return;
        }

        String ua = mContext.getResources().getString(R.string.def_useragent);

        if (TextUtils.isEmpty(ua))
            return;

        ua = constructUserAgent(ua);

        if (!TextUtils.isEmpty(ua)){
            BrowserCommandLine.appendSwitchWithValue(BrowserSwitches.OVERRIDE_USER_AGENT, ua);
        }
    }

    public void overrideMediaDownload() {
        boolean defaultAllowMediaDownloadsValue = mContext.getResources().getBoolean(
            R.bool.def_allow_media_downloads);
        if (defaultAllowMediaDownloadsValue)
            BrowserCommandLine.appendSwitchWithValue(BrowserSwitches.OVERRIDE_MEDIA_DOWNLOAD, "1");
    }

    public void setExtraHTTPRequestHeaders() {
        String headers = mContext.getResources().getString(
            R.string.def_extra_http_headers);
        if (!TextUtils.isEmpty(headers))
            BrowserCommandLine.appendSwitchWithValue(BrowserSwitches.HTTP_HEADERS, headers);
    }

    public void initCommandLineSwitches() {
        //SWE-hide-title-bar - enable following flags
        BrowserCommandLine.appendSwitchWithValue(BrowserSwitches.TOP_CONTROLS_SHOW_THRESHOLD, "0.5");
        BrowserCommandLine.appendSwitchWithValue(BrowserSwitches.TOP_CONTROLS_HIDE_THRESHOLD, "0.5");

        // Allow to override UserAgent
        overrideUserAgent();
        overrideMediaDownload();
        setExtraHTTPRequestHeaders();
    }

    private String constructUserAgent(String userAgent) {
        try {
            userAgent = userAgent.replaceAll("<%build_model>", Build.MODEL);
            userAgent = userAgent.replaceAll("<%build_version>", Build.VERSION.RELEASE);
            userAgent = userAgent.replaceAll("<%build_id>", Build.ID);
            userAgent = userAgent.replaceAll("<%language>", Locale.getDefault().getLanguage());
            userAgent = userAgent.replaceAll("<%country>", Locale.getDefault().getCountry());
            return userAgent;
        } catch (Exception ex) {
            return null;
        }
    }

    public static enum Feature {
        WAP2ESTORE, /* Launch custom app when URL scheme is 'estore:' */
        DRM_UPLOADS, /* Prevent uploading files with DRM filename extensions */
        NETWORK_NOTIFIER, /* Prompt user to select WiFi access point or otherwise enable WLAN */
        EXIT_DIALOG, /* Add 'Exit' menu item and show 'Minimize or quit' dialog */
        TITLE_IN_URL_BAR, /* Display page title instead of url in URL bar */
        CUSTOM_DOWNLOAD_PATH, /* Allow users to provide custom download path */
        DISABLE_HISTORY /* Allow disabling saving history for non-incognito tabs */
    }

    public boolean hasFeature(Feature feature) {
        switch (feature) {
            case WAP2ESTORE:
                return mContext.getResources().getBoolean(R.bool.feature_wap2estore);
            case DRM_UPLOADS:
                return mContext.getResources().getBoolean(R.bool.feature_drm_uploads);
            case NETWORK_NOTIFIER:
                return mContext.getResources().getBoolean(R.bool.feature_network_notifier);
            case EXIT_DIALOG:
                return mContext.getResources().getBoolean(R.bool.feature_exit_dialog);
            case TITLE_IN_URL_BAR:
                return mContext.getResources().getBoolean(R.bool.feature_title_in_URL_bar);
            case CUSTOM_DOWNLOAD_PATH:
                return mContext.getResources().getBoolean(R.bool.feature_custom_download_path);
            case DISABLE_HISTORY:
                return mContext.getResources().getBoolean(R.bool.feature_disable_history);
            default:
                return false;
        }
    }
}

