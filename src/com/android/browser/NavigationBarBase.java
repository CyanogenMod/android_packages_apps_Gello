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

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.browser.R;
import com.android.browser.UrlInputView.UrlInputListener;
import com.android.browser.reflect.ReflectHelper;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import org.codeaurora.swe.WebView;

public class NavigationBarBase extends LinearLayout implements
        OnClickListener, UrlInputListener, OnFocusChangeListener,
        TextWatcher {

    private final static String TAG = "NavigationBarBase";

    protected BaseUi mBaseUi;
    protected TitleBar mTitleBar;
    protected UiController mUiController;
    protected UrlInputView mUrlInput;

    private ImageView mFavicon;
    private ImageView mLockIcon;

    public NavigationBarBase(Context context) {
        super(context);
    }

    public NavigationBarBase(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NavigationBarBase(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mLockIcon = (ImageView) findViewById(R.id.lock);
        mFavicon = (ImageView) findViewById(R.id.favicon);
        mUrlInput = (UrlInputView) findViewById(R.id.url);
        mUrlInput.setUrlInputListener(this);
        mUrlInput.setOnFocusChangeListener(this);
        mUrlInput.setSelectAllOnFocus(true);
        mUrlInput.addTextChangedListener(this);
    }

    public void setTitleBar(TitleBar titleBar) {
        mTitleBar = titleBar;
        mBaseUi = mTitleBar.getUi();
        mUiController = mTitleBar.getUiController();
        mUrlInput.setController(mUiController);
    }

    public void setLock(Drawable d) {
        if (mLockIcon == null) return;
        if (d == null) {
            mLockIcon.setVisibility(View.GONE);
        } else {
            mLockIcon.setImageDrawable(d);
            mLockIcon.setVisibility(View.VISIBLE);
        }
    }

    public void setFavicon(Bitmap icon) {
        if (mFavicon == null) return;
        mFavicon.setImageDrawable(mBaseUi.getFaviconDrawable(icon));
    }

    @Override
    public void onClick(View v) {
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        // if losing focus and not in touch mode, leave as is
        if (hasFocus || view.isInTouchMode() || mUrlInput.needsUpdate()) {
            setFocusState(hasFocus);
        }
        if (hasFocus) {
            mBaseUi.showTitleBar();
        } else if (!mUrlInput.needsUpdate()) {
            mUrlInput.dismissDropDown();
            mUrlInput.hideIME();
            if (mUrlInput.getText().length() == 0) {
                Tab currentTab = mUiController.getTabControl().getCurrentTab();
                if (currentTab != null) {
                    setDisplayTitle(currentTab.getUrl());
                }
            }
            mBaseUi.suggestHideTitleBar();
        }
        mUrlInput.clearNeedsUpdate();
    }

    protected void setFocusState(boolean focus) {
    }

    public boolean isEditingUrl() {
        return mUrlInput.hasFocus();
    }

    void stopEditingUrl() {
        WebView currentTopWebView = mUiController.getCurrentTopWebView();
        if (currentTopWebView != null) {
            currentTopWebView.requestFocus();
        }
    }

    void setDisplayTitle(String title) {
        if (!isEditingUrl()) {
            if (!title.equals(mUrlInput.getText().toString())) {
                mUrlInput.setText(title, false);
            }
        }
    }

    void setIncognitoMode(boolean incognito) {
        mUrlInput.setIncognitoMode(incognito);
    }

    void clearCompletions() {
        mUrlInput.dismissDropDown();
    }

 // UrlInputListener implementation

    /**
     * callback from suggestion dropdown
     * user selected a suggestion
     */
    @Override
    public void onAction(String text, String extra, String source) {
        stopEditingUrl();
        if (UrlInputView.TYPED.equals(source)) {
            String url = null;
            Object[] params  = {new String("persist.env.browser.wap2estore"),
                                    Boolean.valueOf(false)};
            Class[] type = new Class[] {String.class, boolean.class};
            Boolean wap2estore = (Boolean) ReflectHelper.invokeMethod(
                      "android.os.SystemProperties", "getBoolean", type, params);
            if ((wap2estore && isEstoreTypeUrl(text)) || isRtspTypeUrl(text)
                || isMakeCallTypeUrl(text)) {
                url = text;
            } else {
                url = UrlUtils.smartUrlFilter(text, false);
            }

            Tab t = mBaseUi.getActiveTab();
            // Only shortcut javascript URIs for now, as there is special
            // logic in UrlHandler for other schemas
            if (url != null && t != null && url.startsWith("javascript:")) {
                mUiController.loadUrl(t, url);
                setDisplayTitle(text);
                return;
            }

            // add for carrier wap2estore feature
            if (url != null && t != null && wap2estore && isEstoreTypeUrl(url)) {
                handleEstoreTypeUrl(url);
                setDisplayTitle(text);
                return;
            }
            // add for rtsp scheme feature
            if (url != null && t != null && isRtspTypeUrl(url)) {
                if (handleRtspTypeUrl(url)) {
                    return;
                }
            }
            // add for "wtai://wp/mc;" scheme feature
            if (url != null && t != null && isMakeCallTypeUrl(url)) {
                if (handleMakeCallTypeUrl(url)) {
                    return;
                }
            }
        }
        Intent i = new Intent();
        String action = Intent.ACTION_SEARCH;
        i.setAction(action);
        i.putExtra(SearchManager.QUERY, text);
        if (extra != null) {
            i.putExtra(SearchManager.EXTRA_DATA_KEY, extra);
        }
        if (source != null) {
            Bundle appData = new Bundle();
            appData.putString("source", source);
            i.putExtra("source", appData);
        }
        mUiController.handleNewIntent(i);
        setDisplayTitle(text);
    }

    private boolean isMakeCallTypeUrl(String url) {
        String utf8Url = null;
        try {
            utf8Url = new String(url.getBytes("UTF-8"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "err " + e);
        }
        if (utf8Url != null && utf8Url.startsWith(UrlHandler.SCHEME_WTAI_MC)) {
            return true;
        }
        return false;
    }

    private boolean handleMakeCallTypeUrl(String url) {
        // wtai://wp/mc;number
        // number=string(phone-number)
        if (url.startsWith(UrlHandler.SCHEME_WTAI_MC)) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(WebView.SCHEME_TEL +
                    url.substring(UrlHandler.SCHEME_WTAI_MC.length())));
            getContext().startActivity(intent);
            // before leaving BrowserActivity, close the empty child tab.
            // If a new tab is created through JavaScript open to load this
            // url, we would like to close it as we will load this url in a
            // different Activity.
            Tab current = mUiController.getCurrentTab();
            if (current != null
                    && current.getWebView().copyBackForwardList().getSize() == 0) {
                mUiController.closeCurrentTab();
            }
            return true;
        }
        return false;
    }

    private boolean isEstoreTypeUrl(String url) {
        String utf8Url = null;
        try {
            utf8Url = new String(url.getBytes("UTF-8"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "err " + e);
        }
        if (utf8Url != null && utf8Url.startsWith("estore:")) {
            return true;
        }
        return false;
    }

    private void handleEstoreTypeUrl(String url) {
        String utf8Url = null, finalUrl = null;
        try {
            utf8Url = new String(url.getBytes("UTF-8"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "err " + e);
        }
        if (utf8Url != null) {
            finalUrl = utf8Url;
        } else {
            finalUrl = url;
        }
        if (finalUrl.replaceFirst("estore:", "").length() > 256) {
            Toast.makeText(getContext(), R.string.estore_url_warning, Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(finalUrl));
        try {
            getContext().startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            String downloadUrl = getContext().getResources().getString(R.string.estore_homepage);
            mUiController.loadUrl(mBaseUi.getActiveTab(), downloadUrl);
            Toast.makeText(getContext(), R.string.download_estore_app, Toast.LENGTH_LONG).show();
        }
    }

    private boolean isRtspTypeUrl(String url) {
        String utf8Url = null;
        try {
            utf8Url = new String(url.getBytes("UTF-8"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "err " + e);
        }
        if (utf8Url != null && utf8Url.startsWith("rtsp://")) {
            return true;
        }
        return false;
    }

    private boolean handleRtspTypeUrl(String url) {
        Intent intent;
        // perform generic parsing of the URI to turn it into an Intent.
        try {
            intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
        } catch (URISyntaxException ex) {
            Log.w("Browser", "Bad URI " + url + ": " + ex.getMessage());
            return false;
        }

        try {
            getContext().startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Log.w("Browser", "No resolveActivity " + url);
            return false;
        }
        return true;
    }

    @Override
    public void onDismiss() {
        final Tab currentTab = mBaseUi.getActiveTab();
        mBaseUi.hideTitleBar();
        post(new Runnable() {
            public void run() {
                clearFocus();
                if (currentTab != null) {
                    setDisplayTitle(currentTab.getUrl());
                }
            }
        });
    }

    /**
     * callback from the suggestion dropdown
     * copy text to input field and stay in edit mode
     */
    @Override
    public void onCopySuggestion(String text) {
        mUrlInput.setText(text, true);
        if (text != null) {
            mUrlInput.setSelection(text.length());
        }
    }

    public void setCurrentUrlIsBookmark(boolean isBookmark) {
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            // catch back key in order to do slightly more cleanup than usual
            stopEditingUrl();
        }
        return super.dispatchKeyEventPreIme(evt);
    }

    /**
     * called from the Ui when the user wants to edit
     * @param clearInput clear the input field
     */
    void startEditingUrl(boolean clearInput, boolean forceIME) {
        // editing takes preference of progress
        setVisibility(View.VISIBLE);
        if (mTitleBar.useQuickControls()) {
            mTitleBar.getProgressView().setVisibility(View.GONE);
        }
        if (!mUrlInput.hasFocus()) {
            mUrlInput.requestFocus();
        }
        if (clearInput) {
            mUrlInput.setText("");
        }
        if (forceIME) {
            mUrlInput.showIME();
        }
    }

    public void onProgressStarted() {
    }

    public void onProgressStopped() {
    }

    public boolean isMenuShowing() {
        return false;
    }

    public void onTabDataChanged(Tab tab) {
    }

    public void onVoiceResult(String s) {
        startEditingUrl(true, true);
        onCopySuggestion(s);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override
    public void afterTextChanged(Editable s) { }

}
