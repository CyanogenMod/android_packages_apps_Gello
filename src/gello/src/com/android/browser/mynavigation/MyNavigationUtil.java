/*
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *       * Redistributions in binary form must reproduce the above
 *         copyright notice, this list of conditions and the following
 *         disclaimer in the documentation and/or other materials provided
 *         with the distribution.
 *       * Neither the name of The Linux Foundation nor the names of its
 *         contributors may be used to endorse or promote products derived
 *         from this software without specific prior written permission.
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

package com.android.browser.mynavigation;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.android.browser.BrowserConfig;

public class MyNavigationUtil {

    public static final String ID = "_id";
    public static final String URL = "url";
    public static final String TITLE = "title";
    public static final String DATE_CREATED = "created";
    public static final String WEBSITE = "website";
    public static final String FAVICON = "favicon";
    public static final String THUMBNAIL = "thumbnail";
    public static final int WEBSITE_NUMBER = 12;

    public static final String AUTHORITY = BrowserConfig.AUTHORITY + ".mynavigation";
    public static final String MY_NAVIGATION = "content://" + AUTHORITY + "/" + "websites";
    public static final Uri MY_NAVIGATION_URI = Uri.parse(MY_NAVIGATION);
    public static final String DEFAULT_THUMB = "default_thumb";
    public static final String LOGTAG = "MyNavigationUtil";

    public static boolean isDefaultMyNavigation(String url) {
        if (url != null && url.startsWith("ae://") && url.endsWith("add-fav")) {
            Log.d(LOGTAG, "isDefaultMyNavigation will return true.");
            return true;
        }
        return false;
    }

    public static String getMyNavigationUrl(String srcUrl) {
        String srcPrefix = "data:image/png";
        String srcSuffix = ";base64,";
        if (srcUrl != null && srcUrl.startsWith(srcPrefix)) {
            int indexPrefix = srcPrefix.length();
            int indexSuffix = srcUrl.indexOf(srcSuffix);
            return srcUrl.substring(indexPrefix, indexSuffix);
        }
        return "";
    }

    public static boolean isMyNavigationUrl(Context context, String itemUrl) {
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = cr.query(MyNavigationUtil.MY_NAVIGATION_URI,
                    new String[] {
                        MyNavigationUtil.TITLE
                    }, "url = ?", new String[] {
                        itemUrl
                    }, null);
            if (null != cursor && cursor.moveToFirst()) {
                Log.d(LOGTAG, "isMyNavigationUrl will return true.");
                return true;
            }
        } catch (IllegalStateException e) {
            Log.e(LOGTAG, "isMyNavigationUrl", e);
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
        return false;
    }

    private static final String ACCEPTABLE_WEBSITE_SCHEMES[] = {
            "http:",
            "https:",
            "about:",
            "data:",
            "javascript:",
            "file:",
            "content:"
    };

    public static boolean urlHasAcceptableScheme(String url) {
        if (url == null) {
            return false;
        }

        for (int i = 0; i < ACCEPTABLE_WEBSITE_SCHEMES.length; i++) {
            if (url.startsWith(ACCEPTABLE_WEBSITE_SCHEMES[i])) {
                return true;
            }
        }
        return false;
    }
}
