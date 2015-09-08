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
import android.util.AttributeSet;
import com.android.browser.BrowserSettings;
import com.android.browser.R;

import org.codeaurora.swe.WebSettings;

public class FontSizePreview extends WebViewPreview {

    //default size for normal sized preview text
    static final int DEFAULT_FONT_PREVIEW_SIZE = 13;

    public FontSizePreview(
            Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public FontSizePreview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FontSizePreview(Context context) {
        super(context);
    }

    @Override
    protected void updatePreview(boolean forceReload) {
        if (mWebView == null || mTextView == null)
            return;

        WebSettings ws = mWebView.getSettings();
        BrowserSettings bs = BrowserSettings.getInstance();
        ws.setMinimumFontSize(bs.getMinimumFontSize());
        ws.setTextZoom(bs.getTextZoom());
        mTextView.setText(R.string.pref_sample_font_size);
        mTextView.setTextSize(DEFAULT_FONT_PREVIEW_SIZE * bs.getTextZoom() / 100);
    }
}
