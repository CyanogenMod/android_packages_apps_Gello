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
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.browser.BrowserSettings;
import com.android.browser.R;
import org.codeaurora.swe.WebView;

public abstract class WebViewPreview extends Preference
        implements OnSharedPreferenceChangeListener {

    protected TextView mTextView;
    protected WebView mWebView;

    public WebViewPreview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public WebViewPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WebViewPreview(Context context) {
        super(context);
        init(context);
    }

    protected void init(Context context) {
        setLayoutResource(R.layout.webview_preview);
        BrowserSettings bs = BrowserSettings.getInstance();
        mWebView = bs.getTopWebView();
    }

    protected abstract void updatePreview(boolean forceReload);

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mTextView = (TextView) view.findViewById(R.id.text_size_preview);
        // Ignore all touch events & don't show scrollbars
        mTextView.setFocusable(false);
        mTextView.setFocusableInTouchMode(false);
        mTextView.setClickable(false);
        mTextView.setLongClickable(false);
        mTextView.setHorizontalScrollBarEnabled(false);
        mTextView.setVerticalScrollBarEnabled(false);
        updatePreview(true);
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPrepareForRemoval() {
        getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPrepareForRemoval();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        updatePreview(false);
    }

}
