/*
 * Copyright (c) 2015, The Linux Foundation. All rights reserved.
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

package com.android.browser.mdm;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.browser.Browser;
import com.android.browser.PreferenceKeys;

public class ProxyRestriction extends Restriction implements PreferenceKeys {

    private final static String TAG = "ProxyRestriction";

    // These strings are duplicated in chromium.net.ProxyChangeListener.java
    // (duplication due to layer policy)
    // If you need to change/add/delete, make sure they stay in sync
    public static final String MDM_PROXY_CHANGE_ACTION = "swe.mdm.intent.action.PROXY_CHANGE";
    public static final String PROXY_MODE = "ProxyMode";
    public static final String MODE_DIRECT = "direct";
    public static final String MODE_SYSTEM = "system";
    public static final String MODE_AUTO_DETECT = "auto_detect";
    public static final String MODE_FIXED_SERVERS = "fixed_servers";
    public static final String MODE_PAC_SCRIPT = "pac_script";

    public static final String PROXY_SERVER = "ProxyServer";
    public static final String PROXY_PORT = "ProxyPort";
    public static final String PROXY_PAC_URL = "ProxyPacUrl";
    public static final String PROXY_BYPASS_LIST = "ProxyBypassList";

    private static ProxyRestriction sInstance;

    private ProxyRestriction() {
        super(TAG);
    }

    public static ProxyRestriction getInstance() {
        synchronized (ProxyRestriction.class) {
            if (sInstance == null) {
                sInstance = new ProxyRestriction();
            }
        }
        return sInstance;
    }

    @Override
    public void enforce(Bundle restrictions) {
        String proxyMode = restrictions.getString(PROXY_MODE);

        // Leaving ProxyMode not set lifts any managed profile proxy restrictions, allowing users to
        // choose the proxy settings on their own.
        if (proxyMode == null) {
            Log.v(TAG, "enforce: proxyMode is null, disabling.");
            saveProxyConfig(false, null, null, -1, null, null);
        }

        // If policy is to not use the proxy and always connect directly, then all other options
        // are ignored.
        else if (proxyMode.equals(MODE_DIRECT)) {
            Log.v(TAG, "enforce: proxyMode is MODE_DIRECT, enabling and passing to ProxyChangeListener.");
            saveProxyConfig(true, proxyMode, null, -1, null, null);
        }

        // If you choose to use system proxy settings or auto detect the proxy server,
        // all other options are ignored.
        else if (proxyMode.equals(MODE_SYSTEM) ||
                 proxyMode.equals(MODE_AUTO_DETECT)) {
            saveProxyConfig(true, proxyMode, null, -1, null, null);
        }

        // If you choose fixed server proxy mode, you can specify further options in 'Address or URL
        // of proxy server' and 'Comma-separated list of proxy bypass rules'.
        else if (proxyMode.equals(MODE_FIXED_SERVERS)) {
            String host;
            int port;
            try {
                Uri proxyServerUri = Uri.parse(restrictions.getString(PROXY_SERVER));
                host = proxyServerUri.getHost();
                // Bail out if host is not present
                if (host == null) {
                    Log.e(TAG, "enforce: host == null while processing MODE_FIXED_SERVERS");
                    saveProxyConfig(false, null, null, -1, null, null);
                    return;
                }
                port = proxyServerUri.getPort();
            } catch (Exception e) {
                // Bail out if ProxyServer string is missing
                Log.e(TAG,"enforce: Exception caught while processing MODE_FIXED_SERVERS");
                saveProxyConfig(false, null, null, -1, null, null);
                return;
            }
            String proxyBypassList = restrictions.getString(PROXY_BYPASS_LIST);
            Log.v(TAG,"enforce: saving MODE_FIXED_SERVERS proxy config: ");
            Log.v(TAG,"   - host       : " + host);
            Log.v(TAG,"   - port       : " + port);
            Log.v(TAG,"   - bypassList : " + (proxyBypassList != null ? proxyBypassList : "NULL"));

            saveProxyConfig(true, proxyMode, host, port, null, proxyBypassList);
        }

        // This policy only takes effect if you have selected manual proxy settings at 'Choose how
        // to specify proxy server settings'. You should leave this policy not set if you have
        // selected any other mode for setting proxy policies.
        else if (proxyMode.equals(MODE_PAC_SCRIPT)) {
            String proxyPacUrl = restrictions.getString(PROXY_PAC_URL);
            // Bail out if ProxyPacUrl string is missing
            if (proxyPacUrl == null) {
                Log.v(TAG, "enforce: MODE_PAC_SCRIPT. proxyPacUrl is null. disabling");
                saveProxyConfig(false, null, null, -1, null, null);

            } else {
                Log.v(TAG, "enforce: MODE_PAC_SCRIPT. proxyPacUrl ["+ proxyPacUrl +
                           "]. sending and enabling");
                saveProxyConfig(true, proxyMode, null, -1, proxyPacUrl, null);
            }
        }
    }

    private void saveProxyConfig(boolean isEnabled, String proxyMode, String host,
                                 int port, String proxyPacUrl, String proxyBypassList) {

        enable(isEnabled);

        Intent proxySignal = new Intent(MDM_PROXY_CHANGE_ACTION);

        proxySignal.putExtra(PROXY_MODE, isEnabled ? proxyMode : null);

        if (isEnabled) {
            proxySignal.putExtra(PROXY_SERVER, host);
            proxySignal.putExtra(PROXY_PORT, port);
            proxySignal.putExtra(PROXY_PAC_URL, proxyPacUrl);
            proxySignal.putExtra(PROXY_BYPASS_LIST, proxyBypassList);
        }

        // Send the broadcast. We use LocalBroadcastManager because it is more secure
        // and it provides a handy synchronous call.
        LocalBroadcastManager.getInstance(Browser.getContext()).sendBroadcastSync(proxySignal);
    }
}
