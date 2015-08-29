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

import com.android.browser.UI.ComboViews;

import android.content.Context;
import android.os.Handler;
import android.app.Activity;

import android.webkit.JavascriptInterface;

import org.codeaurora.swe.WebView;

public class HomepageHandler {

    private Activity mActivity;
    private Controller mController;
    private Handler mHandler = new Handler();

    HomepageHandler(Activity activity, Controller controller ){
        mActivity = activity;
        mController = controller;
    }

    // add for carrier homepage feature
    @JavascriptInterface
    public void loadBookmarks() {
        Tab t = mController.mTabControl.getCurrentTab();
        if (isDefaultLandingPage(t.mCurrentState.mUrl)) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mController.bookmarksOrHistoryPicker(ComboViews.Bookmarks);
                }
            });
        }
    }

    // add for carrier homepage feature
    @JavascriptInterface
    public void loadHistory() {
        Tab t = mController.mTabControl.getCurrentTab();
        if (isDefaultLandingPage(t.mCurrentState.mUrl)) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mController.bookmarksOrHistoryPicker(ComboViews.History);
                }
            });
        }
    }

    public void registerJsInterface(WebView webview, String url){
        if (isDefaultLandingPage(url)) {
            webview.getSettings().setJavaScriptEnabled(true);
            webview.addJavascriptInterface(this, "default_homepage");
        }
    }

    public boolean isDefaultLandingPage(String url) {
        return (url != null &&
            url.equals(mActivity.getResources().getString(R.string.def_landing_page)) &&
            url.startsWith("file:///"));
    }
}
