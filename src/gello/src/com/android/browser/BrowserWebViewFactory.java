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
package com.android.browser;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.view.View;

import com.android.browser.reflect.ReflectHelper;

import org.codeaurora.swe.WebView;

/**
 * Web view factory class for creating {@link BrowserWebView}'s.
 */
public class BrowserWebViewFactory implements WebViewFactory {

    private final Context mContext;

    public BrowserWebViewFactory(Context context) {
        mContext = context;
    }

    protected WebView instantiateWebView(AttributeSet attrs, int defStyle,
            boolean privateBrowsing, boolean backgroundTab) {
        return new BrowserWebView(mContext, attrs, defStyle, privateBrowsing, backgroundTab);
    }

    @Override
    public WebView createSubWebView(boolean privateBrowsing) {
        return createWebView(privateBrowsing);
    }

    @Override
    public WebView createWebView(boolean privateBrowsing) {
        return createWebView(privateBrowsing, false);
    }

    @Override
    public WebView createWebView(boolean privateBrowsing, boolean backgroundTab) {
        WebView w = instantiateWebView(null, android.R.attr.webViewStyle, privateBrowsing, backgroundTab);
        initWebViewSettings(w);
        return w;
    }

    protected void initWebViewSettings(WebView w) {
        w.setScrollbarFadingEnabled(true);
        w.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        w.setMapTrackballToArrowKeys(false); // use trackball directly
        // Enable the built-in zoom
        w.getSettings().setBuiltInZoomControls(true);
        final PackageManager pm = mContext.getPackageManager();
        boolean supportsMultiTouch =
                pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH)
                || pm.hasSystemFeature(PackageManager.FEATURE_FAKETOUCH_MULTITOUCH_DISTINCT);
        w.getSettings().setDisplayZoomControls(!supportsMultiTouch);

        // Add this WebView to the settings observer list and update the
        // settings
        final BrowserSettings s = BrowserSettings.getInstance();
        s.startManagingSettings(w.getSettings());

    }

}
