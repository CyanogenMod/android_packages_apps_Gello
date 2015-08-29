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

import android.content.Context;
import android.os.Bundle;

import com.android.browser.Browser;
import com.android.browser.R;

public class DownloadDirRestriction extends Restriction {
    private final static String TAG = "DownloadDirRestriction";
    public static final String RESTRICTION_ENABLED = "DownloadRestrictionEnabled";
    public static final String DOWNLOADS_ALLOWED = "DownloadsAllowed";
    public static final String DOWNLOADS_DIR = "DownloadDirectory";

    private static DownloadDirRestriction sInstance;

    public static final boolean defaultDownloadsAllowed = true;
    public static String defaultDownloadDir;

    private String mCurrDownloadDir;
    private boolean mCurrDownloadsAllowed;

    private DownloadDirRestriction() {
        super(TAG);
    }

    @Override
    protected void doCustomInit() {
        mCurrDownloadsAllowed = defaultDownloadsAllowed;
        Context c = Browser.getContext();
        defaultDownloadDir = c.getString(R.string.download_default_path);
        mCurrDownloadDir = defaultDownloadDir;
    }

    public static DownloadDirRestriction getInstance() {
        synchronized (DownloadDirRestriction.class) {
            if (sInstance == null) {
                sInstance = new DownloadDirRestriction();
            }
        }
        return sInstance;
    }

    @Override
    public void enforce(Bundle restrictions) {
        enable(restrictions.getBoolean(RESTRICTION_ENABLED, false));

        if (isEnabled()) {
            mCurrDownloadsAllowed = restrictions.getBoolean(DOWNLOADS_ALLOWED, defaultDownloadsAllowed);
            mCurrDownloadDir = restrictions.getString(DOWNLOADS_DIR, defaultDownloadDir);
        }
        else {
            doCustomInit();
        }
    }

    public boolean downloadsAllowed() {
        return mCurrDownloadsAllowed;
    }

    public String getDownloadDirectory() {
        return mCurrDownloadDir;
    }
}
