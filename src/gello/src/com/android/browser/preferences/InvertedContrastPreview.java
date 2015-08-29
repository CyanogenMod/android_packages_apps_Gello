/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.browser.preferences;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import org.codeaurora.swe.WebSettings;
import org.codeaurora.swe.WebView;

import com.android.browser.BrowserSettings;
import com.android.browser.BrowserWebView;
import com.android.browser.WebViewProperties;

public class InvertedContrastPreview extends WebViewPreview {

    static final String IMG_ROOT = "content://com.android.browser.home/res/raw/";
    static final String[] THUMBS = new String[] {
        "thumb_google",
        "thumb_amazon",
        "thumb_cnn",
        "thumb_espn",
        "", // break
        "thumb_bbc",
        "thumb_nytimes",
        "thumb_weatherchannel",
        "thumb_picasa",
    };

    String mHtml;

    public InvertedContrastPreview(
            Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public InvertedContrastPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InvertedContrastPreview(Context context) {
        super(context);
    }
    
    @Override
    protected void init(Context context) {
        super.init(context);
        StringBuilder builder = new StringBuilder("<html><body style=\"width: 1000px\">");
        for (String thumb : THUMBS) {
            if (TextUtils.isEmpty(thumb)) {
                builder.append("<br />");
                continue;
            }
            builder.append("<img src=\"");
            builder.append(IMG_ROOT);
            builder.append(thumb);
            builder.append("\" />&nbsp;");
        }
        builder.append("</body></html>");
        mHtml = builder.toString();
    }

    @Override
    protected void updatePreview(boolean forceReload) {
        if (mWebView == null) return;

    /* SWE: This class extends WebViewPreview, however, WebViewPreview has
    * been modified to use the system webview.  Commenting out code for now
    * which implements the preview as this class  requires refactoring
    * regardless & is currently not in use.

        WebSettings ws = mWebView.getSettings();
        BrowserSettings bs = BrowserSettings.getInstance();
        ws.setProperty(WebViewProperties.gfxInvertedScreen,
                bs.useInvertedRendering() ? "true" : "false");
        ws.setProperty(WebViewProperties.gfxInvertedScreenContrast,
                Float.toString(bs.getInvertedContrast()));
        if (forceReload) {
            mWebView.loadData(mHtml, "text/html", null);
        }

    */
    }

}
