/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import org.codeaurora.swe.HttpAuthHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient.CustomViewCallback;
import org.codeaurora.swe.WebView;

import java.util.List;

/**
 * WebView aspect of the controller
 */
public interface WebViewController {

    Context getContext();

    Activity getActivity();

    TabControl getTabControl();

    WebViewFactory getWebViewFactory();

    void onSetWebView(Tab tab, WebView view);

    void createSubWindow(Tab tab);

    void onPageStarted(Tab tab, WebView view, Bitmap favicon);

    void onPageFinished(Tab tab);

    void onProgressChanged(Tab tab);

    void onReceivedTitle(Tab tab, final String title);

    void onFavicon(Tab tab, WebView view, Bitmap icon);

    boolean shouldOverrideUrlLoading(Tab tab, WebView view, String url);

    boolean shouldOverrideKeyEvent(KeyEvent event);

    boolean onUnhandledKeyEvent(KeyEvent event);

    void doUpdateVisitedHistory(Tab tab, boolean isReload);

    void getVisitedHistory(final ValueCallback<String[]> callback);

    void onReceivedHttpAuthRequest(Tab tab, WebView view, final HttpAuthHandler handler,
            final String host, final String realm);

    void onDownloadStart(Tab tab, String url, String useragent, String contentDisposition,
            String mimeType, String referer, long contentLength);

    void showCustomView(Tab tab, View view, int requestedOrientation,
            CustomViewCallback callback);

    void hideCustomView();

    Bitmap getDefaultVideoPoster();

    View getVideoLoadingProgressView();

    void onUserCanceledSsl(Tab tab);

    void onUpdatedSecurityState(Tab tab);

    void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture);

    void showFileChooser(ValueCallback<String[]> uploadFilePaths, String acceptTypes,
            boolean capture);

    void endActionMode();

    void attachSubWindow(Tab tab);

    void dismissSubWindow(Tab tab);

    Tab openTab(String url, boolean incognito, boolean setActive,
            boolean useCurrent);

    Tab openTab(String url, Tab parent, boolean setActive,
            boolean useCurrent);

    boolean switchToTab(Tab tab);

    void closeTab(Tab tab);

    void setupAutoFill(Message message);

    void bookmarkedStatusHasChanged(Tab tab);

    boolean shouldCaptureThumbnails();

    void onThumbnailCapture(Bitmap bm);
}
