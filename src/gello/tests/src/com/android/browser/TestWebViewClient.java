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

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Message;
import android.view.KeyEvent;
import android.webkit.WebViewClientClassicExt;

import org.codeaurora.swe.HttpAuthHandler;
import org.codeaurora.swe.SslErrorHandler;
import org.codeaurora.swe.WebView;
import org.codeaurora.swe.WebViewClient;
/**
 *
 *
 * WebViewClient for browser tests.
 * Wraps around existing client so that specific methods can be overridden if needed.
 *
 */
abstract class TestWebViewClient extends WebViewClientClassicExt {

  private WebViewClient mWrappedClient;

  protected TestWebViewClient(WebViewClient wrappedClient) {
    mWrappedClient = wrappedClient;
  }

  /** {@inheritDoc} */
  @Override
  public boolean shouldOverrideUrlLoading(WebView view, String url) {
      return mWrappedClient.shouldOverrideUrlLoading(view, url);
  }

  /** {@inheritDoc} */
  @Override
  public void onPageStarted(WebView view, String url, Bitmap favicon) {
    mWrappedClient.onPageStarted(view, url, favicon);
  }

  /** {@inheritDoc} */
  @Override
  public void onPageFinished(WebView view, String url) {
    mWrappedClient.onPageFinished(view, url);
  }

  /** {@inheritDoc} */
  @Override
  public void onLoadResource(WebView view, String url) {
    mWrappedClient.onLoadResource(view, url);
  }

  /** {@inheritDoc} */
  @Deprecated
  @Override
  public void onTooManyRedirects(WebView view, Message cancelMsg,
          Message continueMsg) {
      mWrappedClient.onTooManyRedirects(view, cancelMsg, continueMsg);
  }

  /** {@inheritDoc} */
  @Override
  public void onReceivedError(WebView view, int errorCode,
          String description, String failingUrl) {
    mWrappedClient.onReceivedError(view, errorCode, description, failingUrl);
  }

  /** {@inheritDoc} */
  @Override
  public void onFormResubmission(WebView view, Message dontResend,
          Message resend) {
    mWrappedClient.onFormResubmission(view, dontResend, resend);
  }

  /** {@inheritDoc} */
  @Override
  public void doUpdateVisitedHistory(WebView view, String url,
          boolean isReload) {
    mWrappedClient.doUpdateVisitedHistory(view, url, isReload);
  }

  /** {@inheritDoc} */
  @Override
  public void onReceivedSslError(WebView view, SslErrorHandler handler,
          SslError error) {
      mWrappedClient.onReceivedSslError(view, handler, error);
  }

  /** {@inheritDoc} */
  @Override
  public void onReceivedHttpAuthRequest(WebView view,
          HttpAuthHandler handler, String host, String realm) {
      mWrappedClient.onReceivedHttpAuthRequest(view, handler, host, realm);
  }

  /** {@inheritDoc} */
  @Override
  public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
      return mWrappedClient.shouldOverrideKeyEvent(view, event);
  }

  /** {@inheritDoc} */
  @Override
  public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
    mWrappedClient.onUnhandledKeyEvent(view, event);
  }

  /** {@inheritDoc} */
  @Override
  public void onScaleChanged(WebView view, float oldScale, float newScale) {
    mWrappedClient.onScaleChanged(view, oldScale, newScale);
  }
}
