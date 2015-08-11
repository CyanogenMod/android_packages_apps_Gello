/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.browser;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import org.codeaurora.swe.WebChromeClient;
import org.codeaurora.swe.WebView;
import org.codeaurora.swe.WebViewClient;

import java.util.Map;

/**
 * Manage WebView scroll events
 */
public class BrowserWebView extends WebView implements WebView.TitleBarDelegate {
    private static final boolean ENABLE_ROOTVIEW_BACKREMOVAL_OPTIMIZATION = true;

    public interface OnScrollChangedListener {
        void onScrollChanged(int l, int t, int oldl, int oldt);
    }

    private boolean mBackgroundRemoved = false;
    private TitleBar mTitleBar;
    private OnScrollChangedListener mOnScrollChangedListener;
    private WebChromeClient mWebChromeClient;
    private WebViewClient mWebViewClient;

    /**
     * @param context
     * @param attrs
     * @param defStyle
     * @param javascriptInterfaces
     */
    public BrowserWebView(Context context, AttributeSet attrs, int defStyle,
            Map<String, Object> javascriptInterfaces, boolean privateBrowsing) {
        super(context, attrs, defStyle, privateBrowsing);
        this.setJavascriptInterfaces(javascriptInterfaces);
    }

    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public BrowserWebView(
            Context context, AttributeSet attrs, int defStyle, boolean privateBrowsing, boolean backgroundTab) {
        super(context, attrs, defStyle, privateBrowsing, backgroundTab);
    }

    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public BrowserWebView(
            Context context, AttributeSet attrs, int defStyle, boolean privateBrowsing) {
        super(context, attrs, defStyle, privateBrowsing);
    }

    /**
     * @param context
     * @param attrs
     */
    public BrowserWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @param context
     */
    public BrowserWebView(Context context) {
        super(context);
    }

    @Override
    public void setWebChromeClient(WebChromeClient client) {
        mWebChromeClient = client;
        super.setWebChromeClient(client);
    }

    public WebChromeClient getWebChromeClient() {
      return mWebChromeClient;
    }

    @Override
    public void setWebViewClient(WebViewClient client) {
        mWebViewClient = client;
        super.setWebViewClient(client);
    }

    public WebViewClient getWebViewClient() {
      return mWebViewClient;
    }

    public void setTitleBar(TitleBar title) {
        mTitleBar = title;
        enableTopControls(true);
    }

    public void enableTopControls(boolean shinkViewport) {
        Resources res = getContext().getResources();
        int titlebarHeight = (int) (res.getDimension(R.dimen.toolbar_height)
                                / res.getDisplayMetrics().density);
        setTopControlsHeight(titlebarHeight, shinkViewport);
    }

    // From TitleBarDelegate
    @Override
    public int getTitleHeight() {
        return (mTitleBar != null) ? mTitleBar.getEmbeddedHeight() : 0;
    }

    // From TitleBarDelegate
    @Override
    public void onSetEmbeddedTitleBar(final View title) {
        // TODO: Remove this method; it is never invoked.
    }

    public boolean hasTitleBar() {
        return (mTitleBar != null);
    }

    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);

        // if enabled, removes the background from the main view (assumes coverage with opaqueness)
        if (ENABLE_ROOTVIEW_BACKREMOVAL_OPTIMIZATION) {
            if (!mBackgroundRemoved && getRootView().getBackground() != null) {
                mBackgroundRemoved = true;
                post(new Runnable() {
                    public void run() {
                        getRootView().setBackgroundDrawable(null);
                    }
                });
            }
        }
    }

    public void drawContent(Canvas c) {
        //super.drawContent(c);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // block touch event if title bar is selected
        if (mTitleBar.isEditingUrl()) {
            requestFocus();
            return true;
        }
        else
            return super.onTouchEvent(event);
    }

    @Override
    public void onScrollChanged(int l, int t, int oldl, int oldt) {
        // NOTE: this function seems to not be called when the WebView is scrolled (it may be fine)
        super.onScrollChanged(l, t, oldl, oldt);
        if (mTitleBar != null) {
            mTitleBar.onScrollChanged();
        }
        if (mOnScrollChangedListener != null) {
            mOnScrollChangedListener.onScrollChanged(l, t, oldl, oldt);
        }
    }

    public void setOnScrollChangedListener(OnScrollChangedListener listener) {
        mOnScrollChangedListener = listener;
    }

    @Override
    public boolean showContextMenuForChild(View originalView) {
        return false;
    }

    @Override
    public void destroy() {
        BrowserSettings.getInstance().stopManagingSettings(getSettings());
        super.destroy();
    }

    @Override
    public Bitmap getFavicon() {
        Tab currentTab = mTitleBar.getUiController().getCurrentTab();
        if (currentTab != null){
            return currentTab.getFavicon();
        }
        else return BitmapFactory.decodeResource(
                this.getResources(), R.drawable.ic_deco_favicon_normal);
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        Tab currentTab = mTitleBar.getUiController().getCurrentTab();
        if (currentTab != null && currentTab.isKeyboardShowing()){
            // Try to detect the "back" key that dismisses the keyboard
            if(event.getAction() == KeyEvent.ACTION_DOWN &&
                    event.getKeyCode() == KeyEvent.KEYCODE_BACK)
                mWebViewClient.onKeyboardStateChange(false);
        }
        return super.dispatchKeyEventPreIme(event);
    }

}
