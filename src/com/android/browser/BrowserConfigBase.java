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

    private static final String OVERRIDE_USER_AGENT = "user-agent";
    private Context mContext;

    public BrowserConfigBase(Context context) {
        mContext = context;
    }

    public void overrideUserAgent() {
        BrowserCommandLine bcl = BrowserCommandLine.getInstance();
        // Check if the UA is already present using command line file
        if (bcl.hasSwitch(OVERRIDE_USER_AGENT)) {
            return;
        }

        String ua = mContext.getResources().getString(R.string.def_useragent);

        if (TextUtils.isEmpty(ua))
            return;

        ua = constructUserAgent(ua);

        if (!TextUtils.isEmpty(ua)){
            bcl.appendSwitchWithValue(OVERRIDE_USER_AGENT, ua);
        }
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
}

