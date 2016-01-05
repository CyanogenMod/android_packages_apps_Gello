/*
 * Copyright (C) 2009 The Android Open Source Project
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
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.URLUtil;
import android.webkit.WebResourceResponse;
import android.webkit.WebStorage;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.webkit.ValueCallback;
import android.widget.CheckBox;
import android.widget.Toast;

import com.android.browser.TabControl.OnThumbnailUpdatedListener;
import com.android.browser.homepages.HomeProvider;
import com.android.browser.mynavigation.MyNavigationUtil;
import com.android.browser.provider.MyNavigationProvider;
import com.android.browser.provider.SnapshotProvider.Snapshots;

import org.codeaurora.swe.BrowserCommandLine;
import org.codeaurora.swe.BrowserDownloadListener;
import org.codeaurora.swe.HttpAuthHandler;
import org.codeaurora.swe.WebBackForwardList;
import org.codeaurora.swe.WebChromeClient;
import org.codeaurora.swe.WebView;
import org.codeaurora.swe.WebView.PictureListener;
import org.codeaurora.swe.WebView.CreateWindowParams;
import org.codeaurora.swe.WebViewClient;
import org.codeaurora.swe.util.Observable;
import org.codeaurora.swe.DomDistillerUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import java.util.List;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Class for maintaining Tabs with a main WebView and a subwindow.
 */
class Tab implements PictureListener {

    // Log Tag
    private static final String LOGTAG = "Tab";
    private static final boolean LOGD_ENABLED = com.android.browser.Browser.LOGD_ENABLED;
    // Special case the logtag for messages for the Console to make it easier to
    // filter them and match the logtag used for these messages in older versions
    // of the browser.
    private static final String CONSOLE_LOGTAG = "browser";

    private static final int MSG_CAPTURE = 42;
    private static final int CAPTURE_DELAY = 1000;
    private static final int INITIAL_PROGRESS = 5;

    private static Bitmap sDefaultFavicon;
    private boolean mIsKeyboardUp = false;

    private static Paint sAlphaPaint = new Paint();
    static {
        sAlphaPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        sAlphaPaint.setColor(Color.TRANSPARENT);
    }

    public enum SecurityState {
        // The page's main resource does not use SSL. Note that we use this
        // state irrespective of the SSL authentication state of sub-resources.
        SECURITY_STATE_NOT_SECURE,
        // The page's main resource uses SSL and the certificate is good. The
        // same is true of all sub-resources.
        SECURITY_STATE_SECURE,
        // The page's main resource uses SSL and the certificate is good, but
        // some sub-resources either do not use SSL or have problems with their
        // certificates.
        SECURITY_STATE_MIXED,
        // The page's main resource uses SSL but there is a problem with its
        // certificate.
        SECURITY_STATE_BAD_CERTIFICATE,
    }

    Context mContext;
    protected WebViewController mWebViewController;

    // The tab ID
    private long mId = -1;

    // Main WebView wrapper
    private View mContainer;
    // Main WebView
    private WebView mMainView;
    // Subwindow container
    private View mSubViewContainer;
    // Subwindow WebView
    private WebView mSubView;
    // Saved bundle for when we are running low on memory. It contains the
    // information needed to restore the WebView if the user goes back to the
    // tab.
    private Bundle mSavedState;
    // Parent Tab. This is the Tab that created this Tab, or null if the Tab was
    // created by the UI
    private Tab mParent;
    // Tab that constructed by this Tab. This is used when this Tab is
    // destroyed, it clears all mParentTab values in the children.
    private Vector<Tab> mChildren;
    // If true, the tab is in the foreground of the current activity.
    private boolean mInForeground;
    // If true, the tab is in page loading state (after onPageStarted,
    // before onPageFinsihed)
    private boolean mInPageLoad;
    private boolean mPageFinished;
    private boolean mDisableOverrideUrlLoading;
    private boolean mFirstVisualPixelPainted = false;
    // The last reported progress of the current page
    private int mPageLoadProgress;
    // The time the load started, used to find load page time
    private long mLoadStartTime;
    // Application identifier used to find tabs that another application wants
    // to reuse.
    private String mAppId;
    // flag to indicate if tab should be closed on back
    private boolean mCloseOnBack;
    // flag to indicate if the tab was opened from an intent
    private boolean mDerivedFromIntent = false;
    // The listener that gets invoked when a download is started from the
    // mMainView
    private final BrowserDownloadListener mDownloadListener;
    private DataController mDataController;

    // AsyncTask for downloading touch icons
    DownloadTouchIcon mTouchIconLoader;

    private BrowserSettings mSettings;
    private int mCaptureWidth;
    private int mCaptureHeight;
    private Bitmap mCapture;
    private Bitmap mViewportCapture;
    private Handler mHandler;
    private boolean mUpdateThumbnail;
    private Timestamp timestamp;
    private boolean mFullScreen = false;
    private boolean mReceivedError;

    // determine if webview is destroyed to MemoryMonitor
    private boolean mWebViewDestroyedByMemoryMonitor;

    // Tab started initally in background
    private boolean mBackgroundTab;

    private String mTouchIconUrl;

    private Observable mFirstPixelObservable;
    private Observable mTabHistoryUpdateObservable;

    Observable getFirstPixelObservable() {
        return mFirstPixelObservable;
    }

    Observable getTabHistoryUpdateObservable() {
        return mTabHistoryUpdateObservable;
    }

    // dertermines if the tab contains a disllable page
    private boolean mIsDistillable = false;

    private static synchronized Bitmap getDefaultFavicon(Context context) {
        if (sDefaultFavicon == null) {
            sDefaultFavicon = BitmapFactory.decodeResource(
                    context.getResources(), R.drawable.ic_deco_favicon_normal);
        }
        return sDefaultFavicon;
    }

    // All the state needed for a page
    protected static class PageState {
        String mUrl;
        String mOriginalUrl;
        String mTitle;
        // This is non-null only when mSecurityState is SECURITY_STATE_BAD_CERTIFICATE.
        SecurityState mSecurityState;
        // This is non-null only when onReceivedIcon is called or SnapshotTab restores it.
        Bitmap mFavicon;
        boolean mIsBookmarkedSite;
        boolean mIncognito;

        PageState(Context c, boolean incognito) {
            mIncognito = incognito;
            mOriginalUrl = mUrl = "";
            if (mIncognito) {
                mTitle = c.getString(R.string.new_incognito_tab);
            } else {
                mTitle = c.getString(R.string.new_tab);
            }
            mSecurityState = SecurityState.SECURITY_STATE_NOT_SECURE;
        }

        PageState(Context c, boolean incognito, String url) {
            mIncognito = incognito;
            if (mIncognito)
                mOriginalUrl = mUrl = "";
            else
                mOriginalUrl = mUrl = url;
            mSecurityState = SecurityState.SECURITY_STATE_NOT_SECURE;
        }

    }

    // The current/loading page's state
    protected PageState mCurrentState;

    // Used for saving and restoring each Tab
    static final String ID = "ID";
    static final String CURRURL = "currentUrl";
    static final String CURRTITLE = "currentTitle";
    static final String PARENTTAB = "parentTab";
    static final String APPID = "appid";
    static final String INCOGNITO = "privateBrowsingEnabled";
    static final String USERAGENT = "useragent";
    static final String CLOSEFLAG = "closeOnBack";

    public void setNetworkAvailable(boolean networkUp) {
        if (networkUp && mReceivedError && (mMainView != null)) {
            mMainView.reload();
        }
    }

    public boolean isFirstVisualPixelPainted() {
        return mFirstVisualPixelPainted;
    }

    public int getCaptureIndex(int navIndex) {
        int orientation = mWebViewController.getActivity().
                getResources().getConfiguration().orientation;

        int orientationBit = (orientation == Configuration.ORIENTATION_LANDSCAPE) ? 0 : 1;

        int index = orientationBit << 31 | (((int)mId & 0x7f) << 24) | (navIndex & 0xffffff);
        return index;
    }

    public int getTabIdxFromCaptureIdx(int index) {
        return (index & 0x7f000000) >> 24;
    }

    public int getOrientationFromCaptureIdx(int index) {
        return ((index & 0x80000000) == 0) ? Configuration.ORIENTATION_LANDSCAPE :
                Configuration.ORIENTATION_PORTRAIT;

    }

    public int getNavIdxFromCaptureIdx(int index) {
        return (index & 0xffffff);
    }

    public static SecurityState getWebViewSecurityState(WebView view) {
        switch (view.getSecurityLevel()) {
            case WebView.SecurityLevel.EV_SECURE:
            case WebView.SecurityLevel.SECURE:
                return SecurityState.SECURITY_STATE_SECURE;
            case WebView.SecurityLevel.SECURITY_ERROR:
                return SecurityState.SECURITY_STATE_BAD_CERTIFICATE;
            case WebView.SecurityLevel.SECURITY_POLICY_WARNING:
            case WebView.SecurityLevel.SECURITY_WARNING:
                return SecurityState.SECURITY_STATE_MIXED;
        }
        return SecurityState.SECURITY_STATE_NOT_SECURE;
    }

    // -------------------------------------------------------------------------
    // WebViewClient implementation for the main WebView
    // -------------------------------------------------------------------------

    private final WebViewClient mWebViewClient = new WebViewClient() {
        private Message mDontResend;
        private Message mResend;

        private boolean providersDiffer(String url, String otherUrl) {
            Uri uri1 = Uri.parse(url);
            Uri uri2 = Uri.parse(otherUrl);
            return !uri1.getEncodedAuthority().equals(uri2.getEncodedAuthority());
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            setIsDistillable(false);
            mBackgroundTab = false;
            mInPageLoad = true;
            mPageFinished = false;
            mFirstVisualPixelPainted = false;
            mFirstPixelObservable.set(false);
            mReceivedError = false;
            mUpdateThumbnail = true;
            mPageLoadProgress = INITIAL_PROGRESS;
            mCurrentState = new PageState(mContext,
                    view.isPrivateBrowsingEnabled(), url);
            mLoadStartTime = SystemClock.uptimeMillis();
            // Need re-enable FullScreenMode on Page navigation if needed
            if (BrowserSettings.getInstance().useFullscreen()){
                Controller controller = (Controller) mWebViewController;
                BaseUi ui = (BaseUi) controller.getUi();
                ui.forceDisableFullscreenMode(false);
            }
            // If we start a touch icon load and then load a new page, we don't
            // want to cancel the current touch icon loader. But, we do want to
            // create a new one when the touch icon url is known.
            if (mTouchIconLoader != null) {
                mTouchIconLoader.mTab = null;
                mTouchIconLoader = null;
            }

            // finally update the UI in the activity if it is in the foreground
            mWebViewController.onPageStarted(Tab.this, view, favicon);

            updateBookmarkedStatus();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            mDisableOverrideUrlLoading = false;
            if (!isPrivateBrowsingEnabled()) {
                LogTag.logPageFinishedLoading(
                        url, SystemClock.uptimeMillis() - mLoadStartTime);
            }
            syncCurrentState(view, url);
            mWebViewController.onPageFinished(Tab.this);
            setSecurityState(getWebViewSecurityState(view));
        }

        @Override
        public void onFirstVisualPixel(WebView view) {
            mFirstVisualPixelPainted = true;
            mFirstPixelObservable.set(true);
        }

        // return true if want to hijack the url to let another app to handle it
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (!mDisableOverrideUrlLoading && mInForeground) {
                return mWebViewController.shouldOverrideUrlLoading(Tab.this,
                        view, url);
            } else {
                return false;
            }
        }

        @Override
        public boolean shouldDownloadFavicon(WebView view, String url) {
            return true;
        }

        /**
         * Updates the security state. This method is called when we discover
         * another resource to be loaded for this page (for example,
         * javascript). While we update the security state, we do not update
         * the lock icon until we are done loading, as it is slightly more
         * secure this way.
         */
        @Override
        public void onLoadResource(WebView view, String url) {
            if (url != null && url.length() > 0) {
                // It is only if the page claims to be secure that we may have
                // to update the security state:
                if (mCurrentState.mSecurityState == SecurityState.SECURITY_STATE_SECURE) {
                    // If NOT a 'safe' url, change the state to mixed content!
                    if (!(URLUtil.isHttpsUrl(url) || URLUtil.isDataUrl(url)
                            || URLUtil.isAboutUrl(url))) {
                        mCurrentState.mSecurityState = SecurityState.SECURITY_STATE_MIXED;
                    }
                }
            }
        }

        /**
         * Show a dialog informing the user of the network error reported by
         * WebCore if it is in the foreground.
         */
        @Override
        public void onReceivedError(WebView view, int errorCode,
                String description, String failingUrl) {
            // Used for the syncCurrentState to use
            // the failing url instead of using webview url
            mReceivedError = true;
        }

        /**
         * Check with the user if it is ok to resend POST data as the page they
         * are trying to navigate to is the result of a POST.
         */
        @Override
        public void onFormResubmission(WebView view, final Message dontResend,
                                       final Message resend) {
            if (!mInForeground) {
                dontResend.sendToTarget();
                return;
            }
            if (mDontResend != null) {
                Log.w(LOGTAG, "onFormResubmission should not be called again "
                        + "while dialog is still up");
                dontResend.sendToTarget();
                return;
            }
            mDontResend = dontResend;
            mResend = resend;
            new AlertDialog.Builder(mContext).setTitle(
                    R.string.browserFrameFormResubmitLabel).setMessage(
                    R.string.browserFrameFormResubmitMessage)
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    if (mResend != null) {
                                        mResend.sendToTarget();
                                        mResend = null;
                                        mDontResend = null;
                                    }
                                }
                            }).setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    if (mDontResend != null) {
                                        mDontResend.sendToTarget();
                                        mResend = null;
                                        mDontResend = null;
                                    }
                                }
                            }).setOnCancelListener(new OnCancelListener() {
                        public void onCancel(DialogInterface dialog) {
                            if (mDontResend != null) {
                                mDontResend.sendToTarget();
                                mResend = null;
                                mDontResend = null;
                            }
                        }
                    }).show();
        }

        /**
         * Insert the url into the visited history database.
         * @param url The url to be inserted.
         * @param isReload True if this url is being reloaded.
         * FIXME: Not sure what to do when reloading the page.
         */
        @Override
        public void doUpdateVisitedHistory(WebView view, String url,
                boolean isReload) {
            mWebViewController.doUpdateVisitedHistory(Tab.this, isReload);
        }

        /**
         * Handles an HTTP authentication request.
         *
         * @param handler The authentication handler
         * @param host The host
         * @param realm The realm
         */
        @Override
        public void onReceivedHttpAuthRequest(WebView view,
                final HttpAuthHandler handler, final String host,
                final String realm) {
            mWebViewController.onReceivedHttpAuthRequest(Tab.this, view, handler, host, realm);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view,
                String url) {
            //intercept if opening a new incognito tab - show the incognito welcome page

            // show only incognito content and webview has private
            // and cannot go back(only supported if explicit from UI )
            if (view.isPrivateBrowsingEnabled() &&
                !view.canGoBack() &&
                url.startsWith(Controller.INCOGNITO_URI)) {
                Resources resourceHandle = mContext.getResources();
                InputStream inStream = resourceHandle.openRawResource(
                        com.android.browser.R.raw.incognito_mode_start_page);
                return new WebResourceResponse("text/html", "utf8", inStream);
            }
            WebResourceResponse res;
            if (MyNavigationUtil.MY_NAVIGATION.equals(url)) {
                res = MyNavigationProvider.shouldInterceptRequest(mContext, url);
            } else {
                res = HomeProvider.shouldInterceptRequest(mContext, url);
            }
            return res;
        }

        @Override
        public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
            if (!mInForeground) {
                return false;
            }
            return mWebViewController.shouldOverrideKeyEvent(event);
        }

        @Override
        public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
            if (!mInForeground) {
                return;
            }
            if (!mWebViewController.onUnhandledKeyEvent(event)) {
                super.onUnhandledKeyEvent(view, event);
            }
        }

        @Override
        public void beforeNavigation(WebView view, String url) {
            mTouchIconUrl = null;
            TitleBar titleBar = null;
            Controller controller = (Controller)mWebViewController;
            UI ui = controller.getUi();

            // Clear the page state
            mCurrentState = new PageState(mContext,
                    view.isPrivateBrowsingEnabled(), url);

            if (ui instanceof BaseUi) {
                titleBar = ((BaseUi)ui).getTitleBar();
                if (titleBar != null) {
                    NavigationBarBase navBar = titleBar.getNavigationBar();
                    navBar.showCurrentFavicon(Tab.this); // Show the default Favicon while loading a new page
                }
            }

            if (BaseUi.isUiLowPowerMode()) {
                return;
            }

            if (isPrivateBrowsingEnabled()) {
                return;
            }

            if (!mFirstVisualPixelPainted) {
                return;
            }

            final int idx = view.copyBackForwardList().getCurrentIndex();
            boolean bitmapExists = view.hasSnapshot(idx);

            int progress = 100;
            if (titleBar != null) {
                progress = titleBar.getProgressView().getProgressPercent();
            }

            if (bitmapExists && progress < 85) {
                return;
            }

            int index = getCaptureIndex(view.getLastCommittedHistoryIndex());
            view.captureSnapshot(index, null);
        }

        @Override
        public void onHistoryItemCommit(WebView view, int index) {
            if (BaseUi.isUiLowPowerMode()) {
                return;
            }

            // prevent snapshot tab from commiting any history
            if (isSnapshot()) {
                return;
            }

            mTabHistoryUpdateObservable.set(index);
            final int maxIdx = view.copyBackForwardList().getSize();
            final WebView wv = view;
            final int currIdx = index;
            final int currentTabIdx = (int) Tab.this.getId();
            view.getSnapshotIds(new ValueCallback <List<Integer>>() {
                @Override
                public void onReceiveValue(List<Integer> ids) {
                    for (Integer id : ids) {
                        int tabIdx = getTabIdxFromCaptureIdx(id);
                        int navIdx = getNavIdxFromCaptureIdx(id);
                        if (tabIdx == currentTabIdx && (navIdx >= maxIdx || navIdx == currIdx)) {
                            wv.deleteSnapshot(id);
                        }
                    }
                }
            });
        }

        @Override
        public void onKeyboardStateChange(boolean popup) {
            boolean keyboardWasShowing = isKeyboardShowing();
            mIsKeyboardUp = popup;
            Controller controller = (Controller)mWebViewController;
            BaseUi ui = (BaseUi) controller.getUi();
            // lock the title bar
            if (popup)
                ui.getTitleBar().showTopControls(true);
            if (keyboardWasShowing && popup)
                ui.getTitleBar().enableTopControls(true);
            if (BrowserSettings.getInstance().useFullscreen()) {
                ui.forceDisableFullscreenMode(popup);
            }
        }

        @Override
        public void onAttachInterstitialPage(WebView mWebView) {
            Controller controller = (Controller)mWebViewController;
            BaseUi ui = (BaseUi) controller.getUi();
            ui.getTitleBar().showTopControls(false);
        }

        @Override
        public void onDetachInterstitialPage(WebView mWebView) {
            Controller controller = (Controller)mWebViewController;
            BaseUi ui = (BaseUi) controller.getUi();
            ui.getTitleBar().enableTopControls(true);
        }
    };

    private void syncCurrentState(WebView view, String url) {
        // Sync state (in case of stop/timeout)



        if (mReceivedError) {
            mCurrentState.mUrl =  url;
            mCurrentState.mOriginalUrl = url;
        } else if (view.isPrivateBrowsingEnabled() &&
                   !TextUtils.isEmpty(url) &&
                   url.contains(Controller.INCOGNITO_URI)) {
            mCurrentState.mUrl = mCurrentState.mOriginalUrl = Controller.INCOGNITO_URI;
        }

        else {
            mCurrentState.mUrl = view.getUrl();
            mCurrentState.mOriginalUrl = view.getOriginalUrl();
        }

        if (mCurrentState.mUrl == null) {
            mCurrentState.mUrl = "";
        }
        mCurrentState.mTitle = view.getTitle();


        if (!URLUtil.isHttpsUrl(mCurrentState.mUrl)) {
            // In case we stop when loading an HTTPS page from an HTTP page
            // but before a provisional load occurred
            mCurrentState.mSecurityState = SecurityState.SECURITY_STATE_NOT_SECURE;
        }
        mCurrentState.mIncognito = view.isPrivateBrowsingEnabled();
    }

    public String getTouchIconUrl() {
        return mTouchIconUrl;
    }

    public boolean isKeyboardShowing() {
        Controller controller = (Controller)mWebViewController;
        return (mIsKeyboardUp || controller.getUi().isEditingUrl());
    }

    public boolean isTabFullScreen() {
        return mFullScreen;
    }

    protected void setTabFullscreen(boolean fullScreen) {
        Controller controller = (Controller)mWebViewController;
        controller.getUi().showFullscreen(fullScreen);
        mFullScreen = fullScreen;
    }

    public boolean exitFullscreen() {
        if (mFullScreen) {
            Controller controller = (Controller)mWebViewController;
            controller.getUi().showFullscreen(false);
            if (getWebView() != null)
                getWebView().exitFullscreen();
            mFullScreen = false;
            return true;
        }
        return false;
    }




    // -------------------------------------------------------------------------
    // WebChromeClient implementation for the main WebView
    // -------------------------------------------------------------------------

    private final WebChromeClient mWebChromeClient = new WebChromeClient() {
        // Helper method to create a new tab or sub window.
        private void createWindow(final boolean dialog, final Message msg) {
            this.createWindow(dialog, msg, null, false);
        }

        private void createWindow(final boolean dialog, final Message msg, final String url,
                                  final boolean opener_suppressed) {
            WebView.WebViewTransport transport =
                    (WebView.WebViewTransport) msg.obj;
            if (dialog) {
                createSubWindow();
                mWebViewController.attachSubWindow(Tab.this);
                transport.setWebView(mSubView);
            } else {
                final Tab newTab = mWebViewController.openTab(url,
                        Tab.this, true, true);
                // This is special case for rendering links on a webpage in
                // a new tab. If opener is suppressed, the WebContents created
                // by the content layer are not fully initialized. This check
                // will prevent content layer from overriding WebContents
                // created by new tab with the uninitialized instance.
                if (!opener_suppressed) {
                    transport.setWebView(newTab.getWebView());
                }
            }
            msg.sendToTarget();
        }

        @Override
        public void toggleFullscreenModeForTab(boolean enterFullscreen) {
            if (mWebViewController instanceof Controller) {
                setTabFullscreen(enterFullscreen);
            }
        }

        @Override
        public void onOffsetsForFullscreenChanged(float topControlsOffsetYPix,
                                                  float contentOffsetYPix,
                                                  float overdrawBottomHeightPix) {
            if (mWebViewController instanceof Controller) {
                Controller controller = (Controller)mWebViewController;
                controller.getUi().translateTitleBar(topControlsOffsetYPix);
                // Resize the viewport if top controls is not visible
                if (mMainView != null &&
                        (topControlsOffsetYPix == 0.0f || contentOffsetYPix == 0.0f))
                    ((BrowserWebView)mMainView).enableTopControls(
                        (topControlsOffsetYPix == 0.0f) ? true : false);
            }
        }

        @Override
        public boolean isTabFullScreen() {
          return mFullScreen;
        }

        @Override
        public boolean onCreateWindow(WebView view, final boolean dialog,
                final boolean userGesture, final Message resultMsg) {
            // only allow new window or sub window for the foreground case
            if (!mInForeground) {
                return false;
            }
            // Short-circuit if we can't create any more tabs or sub windows.
            if (dialog && mSubView != null) {
                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.too_many_subwindows_dialog_title)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setMessage(R.string.too_many_subwindows_dialog_message)
                        .setPositiveButton(R.string.ok, null)
                        .show();
                return false;
            } else if (!mWebViewController.getTabControl().canCreateNewTab()) {
                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.too_many_windows_dialog_title)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setMessage(R.string.too_many_windows_dialog_message)
                        .setPositiveButton(R.string.ok, null)
                        .show();
                return false;
            }

            // Short-circuit if this was a user gesture.
            if (userGesture || !mSettings.blockPopupWindows()) {
                WebView.WebViewTransport transport =
                        (WebView.WebViewTransport) resultMsg.obj;
                CreateWindowParams windowParams = transport.getCreateWindowParams();
                if (windowParams.mOpenerSuppressed) {
                    createWindow(dialog, resultMsg, windowParams.mURL, true);
                    // This is special case for rendering links on a webpage in
                    // a new tab. If opener is suppressed, the WebContents created
                    // by the content layer are not fully initialized. Returning false
                    // will prevent content layer from overriding WebContents
                    // created by new tab with the uninitialized instance.
                    return false;
                }

                createWindow(dialog, resultMsg);
                return true;
            }

            createWindow(dialog, resultMsg);
            return true;
        }

        @Override
        public void onRequestFocus(WebView view) {
            if (!mInForeground) {
                mWebViewController.switchToTab(Tab.this);
            }
        }

        @Override
        public void onCloseWindow(WebView window) {
            if (mParent != null) {
                // JavaScript can only close popup window.
                if (mInForeground) {
                    mWebViewController.switchToTab(mParent);
                }
                mWebViewController.closeTab(Tab.this);
            }
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            mPageLoadProgress = newProgress;
            if (newProgress == 100) {
                mInPageLoad = false;
            }
            mWebViewController.onProgressChanged(Tab.this);
            if (mUpdateThumbnail && newProgress == 100) {
                mUpdateThumbnail = false;
            }
        }

        @Override
        public void onReceivedTitle(WebView view, final String title) {
            mCurrentState.mTitle = title;
            mWebViewController.onReceivedTitle(Tab.this, title);
        }

        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            mCurrentState.mFavicon = icon;
            mWebViewController.onFavicon(Tab.this, view, icon);
        }

        @Override
        public void onReceivedTouchIconUrl(WebView view, String url,
                boolean precomposed) {
            final ContentResolver cr = mContext.getContentResolver();
            // Let precomposed icons take precedence over non-composed
            // icons.
            if (precomposed && mTouchIconLoader != null) {
                mTouchIconLoader.cancel(false);
                mTouchIconLoader = null;
            }
            // Have only one async task at a time.
            if (mTouchIconLoader == null) {
                mTouchIconLoader = new DownloadTouchIcon(Tab.this,
                        mContext, cr, view);
                mTouchIconLoader.execute(url);
            }
            mTouchIconUrl = url;
        }

        @Override
        public void onShowCustomView(View view,
                CustomViewCallback callback) {
            Activity activity = mWebViewController.getActivity();
            if (activity != null) {
                onShowCustomView(view, activity.getRequestedOrientation(), callback);
            }
        }

        @Override
        public void onShowCustomView(View view, int requestedOrientation,
                CustomViewCallback callback) {
            if (mInForeground) mWebViewController.showCustomView(Tab.this, view,
                    requestedOrientation, callback);
        }

        @Override
        public void onHideCustomView() {
            if (mInForeground) mWebViewController.hideCustomView();
        }

        /**
         * The origin has exceeded its database quota.
         * @param url the URL that exceeded the quota
         * @param databaseIdentifier the identifier of the database on which the
         *            transaction that caused the quota overflow was run
         * @param currentQuota the current quota for the origin.
         * @param estimatedSize the estimated size of the database.
         * @param totalUsedQuota is the sum of all origins' quota.
         * @param quotaUpdater The callback to run when a decision to allow or
         *            deny quota has been made. Don't forget to call this!
         */
        @Override
        public void onExceededDatabaseQuota(String url,
            String databaseIdentifier, long currentQuota, long estimatedSize,
            long totalUsedQuota, WebStorage.QuotaUpdater quotaUpdater) {
            mSettings.getWebStorageSizeManager()
                    .onExceededDatabaseQuota(url, databaseIdentifier,
                            currentQuota, estimatedSize, totalUsedQuota,
                            quotaUpdater);
        }

        /**
         * The Application Cache has exceeded its max size.
         * @param spaceNeeded is the amount of disk space that would be needed
         *            in order for the last appcache operation to succeed.
         * @param totalUsedQuota is the sum of all origins' quota.
         * @param quotaUpdater A callback to inform the WebCore thread that a
         *            new app cache size is available. This callback must always
         *            be executed at some point to ensure that the sleeping
         *            WebCore thread is woken up.
         */
        @Override
        public void onReachedMaxAppCacheSize(long spaceNeeded,
                long totalUsedQuota, WebStorage.QuotaUpdater quotaUpdater) {
            mSettings.getWebStorageSizeManager()
                    .onReachedMaxAppCacheSize(spaceNeeded, totalUsedQuota,
                            quotaUpdater);
        }

        /* Adds a JavaScript error message to the system log and if the JS
         * console is enabled in the about:debug options, to that console
         * also.
         * @param consoleMessage the message object.
         */
        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            // Don't log console messages in private browsing mode
            if (isPrivateBrowsingEnabled()) return true;

            String message = "Console: " + consoleMessage.message() + " "
                    + consoleMessage.sourceId() +  ":"
                    + consoleMessage.lineNumber();

            switch (consoleMessage.messageLevel()) {
                case TIP:
                    Log.v(CONSOLE_LOGTAG, message);
                    break;
                case LOG:
                    Log.i(CONSOLE_LOGTAG, message);
                    break;
                case WARNING:
                    Log.w(CONSOLE_LOGTAG, message);
                    break;
                case ERROR:
                    Log.e(CONSOLE_LOGTAG, message);
                    break;
                case DEBUG:
                    Log.d(CONSOLE_LOGTAG, message);
                    break;
            }

            return true;
        }

        /**
         * Ask the browser for an icon to represent a <video> element.
         * This icon will be used if the Web page did not specify a poster attribute.
         * @return Bitmap The icon or null if no such icon is available.
         */
        @Override
        public Bitmap getDefaultVideoPoster() {
            if (mInForeground) {
                return mWebViewController.getDefaultVideoPoster();
            }
            return null;
        }

        /**
         * Ask the host application for a custom progress view to show while
         * a <video> is loading.
         * @return View The progress view.
         */
        @Override
        public View getVideoLoadingProgressView() {
            if (mInForeground) {
                return mWebViewController.getVideoLoadingProgressView();
            }
            return null;
        }

        @Override
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            if (mInForeground) {
                mWebViewController.openFileChooser(uploadMsg, acceptType, capture);
            } else {
                uploadMsg.onReceiveValue(null);
            }
        }

        @Override
        public void showFileChooser(ValueCallback<String[]> uploadFilePaths, String acceptTypes,
                boolean capture) {
            if (mInForeground) {
                mWebViewController.showFileChooser(uploadFilePaths, acceptTypes, capture);
            } else {
                uploadFilePaths.onReceiveValue(null);
            }
        }

        /**
         * Deliver a list of already-visited URLs
         */
        @Override
        public void getVisitedHistory(final ValueCallback<String[]> callback) {
            mWebViewController.getVisitedHistory(callback);
        }

        @Override
        public void setupAutoFill(Message message) {
            // Prompt the user to set up their profile.
            final Message msg = message;
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            final View layout = inflater.inflate(R.layout.setup_autofill_dialog, null);

            builder.setView(layout)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        CheckBox disableAutoFill = (CheckBox) layout.findViewById(
                                R.id.setup_autofill_dialog_disable_autofill);

                        if (disableAutoFill.isChecked()) {
                            // Disable autofill and show a toast with how to turn it on again.
                            mSettings.setAutofillEnabled(false);
                            Toast.makeText(mContext,
                                    R.string.autofill_setup_dialog_negative_toast,
                                    Toast.LENGTH_LONG).show();
                        } else {
                            // Take user to the AutoFill profile editor. When they return,
                            // we will send the message that we pass here which will trigger
                            // the form to get filled out with their new profile.
                            mWebViewController.setupAutoFill(msg);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        }
    };

    // -------------------------------------------------------------------------
    // WebViewClient implementation for the sub window
    // -------------------------------------------------------------------------

    // Subclass of WebViewClient used in subwindows to notify the main
    // WebViewClient of certain WebView activities.
    private static class SubWindowClient extends WebViewClient {
        // The main WebViewClient.
        private final WebViewClient mClient;
        private final WebViewController mController;

        SubWindowClient(WebViewClient client, WebViewController controller) {
            mClient = client;
            mController = controller;
        }
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            // Unlike the others, do not call mClient's version, which would
            // change the progress bar.  However, we do want to remove the
            // find or select dialog.
            mController.endActionMode();
        }
        @Override
        public void doUpdateVisitedHistory(WebView view, String url,
                boolean isReload) {
            mClient.doUpdateVisitedHistory(view, url, isReload);
        }
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return mClient.shouldOverrideUrlLoading(view, url);
        }
        @Override
        public void onReceivedHttpAuthRequest(WebView view,
                HttpAuthHandler handler, String host, String realm) {
            mClient.onReceivedHttpAuthRequest(view, handler, host, realm);
        }
        @Override
        public void onFormResubmission(WebView view, Message dontResend,
                Message resend) {
            mClient.onFormResubmission(view, dontResend, resend);
        }
        @Override
        public void onReceivedError(WebView view, int errorCode,
                String description, String failingUrl) {
            mClient.onReceivedError(view, errorCode, description, failingUrl);
        }
        @Override
        public boolean shouldOverrideKeyEvent(WebView view,
                android.view.KeyEvent event) {
            return mClient.shouldOverrideKeyEvent(view, event);
        }
        @Override
        public void onUnhandledKeyEvent(WebView view,
                android.view.KeyEvent event) {
            mClient.onUnhandledKeyEvent(view, event);
        }
    }

    // -------------------------------------------------------------------------
    // WebChromeClient implementation for the sub window
    // -------------------------------------------------------------------------

    private class SubWindowChromeClient extends WebChromeClient {
        // The main WebChromeClient.
        private final WebChromeClient mClient;

        SubWindowChromeClient(WebChromeClient client) {
            mClient = client;
        }
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            mClient.onProgressChanged(view, newProgress);
        }
        @Override
        public boolean onCreateWindow(WebView view, boolean dialog,
                boolean userGesture, android.os.Message resultMsg) {
            return mClient.onCreateWindow(view, dialog, userGesture, resultMsg);
        }
        @Override
        public void onCloseWindow(WebView window) {
            if (window != mSubView) {
                Log.e(LOGTAG, "Can't close the window");
            }
            mWebViewController.dismissSubWindow(Tab.this);
        }
    }

    // -------------------------------------------------------------------------

    // Construct a new tab
    Tab(WebViewController wvcontroller, WebView w) {
        this(wvcontroller, w, null);
    }

    Tab(WebViewController wvcontroller, Bundle state) {
        this(wvcontroller, null, state);
    }

    Tab(WebViewController wvcontroller, WebView w, Bundle state) {
        this(wvcontroller, null, state, false);
    }

    Tab(WebViewController wvcontroller, WebView w, Bundle state, boolean backgroundTab) {
        mWebViewController = wvcontroller;
        mContext = mWebViewController.getContext();
        mSettings = BrowserSettings.getInstance();
        mDataController = DataController.getInstance(mContext);
        mCurrentState = new PageState(mContext, w != null
                ? w.isPrivateBrowsingEnabled() : false);
        setTimeStamp();
        mInPageLoad = false;
        mInForeground = false;
        mWebViewDestroyedByMemoryMonitor = false;
        mBackgroundTab = backgroundTab;

        mDownloadListener = new BrowserDownloadListener() {
            public void onDownloadStart(String url, String userAgent,
                    String contentDisposition, String mimetype, String referer, String auth,
                    long contentLength) {
                mWebViewController.onDownloadStart(Tab.this, url, userAgent, contentDisposition,
                        mimetype, referer, auth, contentLength);
            }
        };

        mCaptureWidth = mContext.getResources().getDimensionPixelSize(R.dimen.tab_thumbnail_width);
        mCaptureHeight =mContext.getResources().getDimensionPixelSize(R.dimen.tab_thumbnail_height);

        initCaptureBitmap();

        restoreState(state);
        if (getId() == -1) {
            mId = TabControl.getNextId();
        }
        setWebView(w);

        UI ui = ((Controller)mWebViewController).getUi();
        if (ui instanceof BaseUi) {
            TitleBar titleBar = ((BaseUi)ui).getTitleBar();
            if (titleBar != null) {
                NavigationBarBase navBar = titleBar.getNavigationBar();
                navBar.showCurrentFavicon(this); // Show the default Favicon while loading a new page
            }
        }

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message m) {
                switch (m.what) {
                case MSG_CAPTURE:
                    capture();
                    break;
                }
            }
        };

        mFirstPixelObservable = new Observable();
        mFirstPixelObservable.set(false);
        mTabHistoryUpdateObservable = new Observable();
    }

    public void initCaptureBitmap() {
        mCapture = Bitmap.createBitmap(mCaptureWidth, mCaptureHeight, Bitmap.Config.RGB_565);
        mCapture.eraseColor(Color.WHITE);
    }

    /**
     * This is used to get a new ID when the tab has been preloaded, before it is displayed and
     * added to TabControl. Preloaded tabs can be created before restoreInstanceState, leading
     * to overlapping IDs between the preloaded and restored tabs.
     */
    public void refreshIdAfterPreload() {
        mId = TabControl.getNextId();
    }

    public void setController(WebViewController ctl) {
        mWebViewController = ctl;

        if (mWebViewController.shouldCaptureThumbnails()) {
            synchronized (Tab.this) {
                if (mCapture == null) {
                    initCaptureBitmap();
                    if (mInForeground && !mHandler.hasMessages(MSG_CAPTURE)) {
                        mHandler.sendEmptyMessageDelayed(MSG_CAPTURE, CAPTURE_DELAY);
                    }
                }
            }
        } else {
            synchronized (Tab.this) {
                mCapture = null;
                deleteThumbnail();
            }
        }
    }

    public long getId() {
        return mId;
    }

    void setWebView(WebView w) {
        setWebView(w, true);
    }

    public boolean isNativeActive(){
        if (mMainView == null)
            return false;
        return true;
    }

    public void setTimeStamp(){
        Date d = new Date();
        timestamp = (new Timestamp(d.getTime()));
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }
    /**
     * Sets the WebView for this tab, correctly removing the old WebView from
     * the container view.
     */
    void setWebView(WebView w, boolean restore) {
        if (mMainView == w) {
            return;
        }

        mWebViewController.onSetWebView(this, w);

        if (mMainView != null) {
            mMainView.setPictureListener(null);
            if (w != null) {
                syncCurrentState(w, null);
            } else if(!mWebViewDestroyedByMemoryMonitor) {
                mCurrentState = new PageState(mContext,
                    mMainView.isPrivateBrowsingEnabled());
            }
        }
        // set the new one
        mMainView = w;

        // attach the WebViewClient, WebChromeClient and DownloadListener
        if (mMainView != null) {
            mMainView.setWebViewClient(mWebViewClient);
            mMainView.setWebChromeClient(mWebChromeClient);
            // Attach DownloadManager so that downloads can start in an active
            // or a non-active window. This can happen when going to a site that
            // does a redirect after a period of time. The user could have
            // switched to another tab while waiting for the download to start.
            mMainView.setDownloadListener(mDownloadListener);
            TabControl tc = mWebViewController.getTabControl();
            if (tc != null /*&& tc.getOnThumbnailUpdatedListener() != null*/) {
                mMainView.setPictureListener(this);
            }
            if (restore && (mSavedState != null)) {
                restoreUserAgent();
                WebBackForwardList restoredState
                        = mMainView.restoreState(mSavedState);
                if (restoredState == null || restoredState.getSize() == 0) {
                    Log.w(LOGTAG, "Failed to restore WebView state!");
                    loadUrl(mCurrentState.mOriginalUrl, null);
                }
                mWebViewDestroyedByMemoryMonitor = false;
                mSavedState = null;
            } else if(restore && mBackgroundTab && mWebViewDestroyedByMemoryMonitor) {
                loadUrl(mCurrentState.mOriginalUrl, null);
                mWebViewDestroyedByMemoryMonitor = false;
            }
        }
    }

    public void destroyThroughMemoryMonitor() {
        mWebViewDestroyedByMemoryMonitor = true;
        destroy();
    }

    /**
     * Destroy the tab's main WebView and subWindow if any
     */
    void destroy() {

        if (mPostponeDestroy) {
            mShouldDestroy = true;
            return;
        }
        mShouldDestroy = false;
        if (mMainView != null) {
            dismissSubWindow();
            // save the WebView to call destroy() after detach it from the tab
            final WebView webView = mMainView;
            setWebView(null);
            if (!mWebViewDestroyedByMemoryMonitor && !BaseUi.isUiLowPowerMode()) {
                final int destroyedTabIdx = (int) mId;
                // Tabs can be reused with new instance of WebView so delete the snapshots
                webView.getSnapshotIds(new ValueCallback<List<Integer>>() {
                    @Override
                    public void onReceiveValue(List<Integer> ids) {
                        for (Integer id : ids) {
                            if (getTabIdxFromCaptureIdx(id) == destroyedTabIdx) {
                                webView.deleteSnapshot(id);
                            }
                        }
                        webView.destroy();
                    }
                });
            } else {
                webView.destroy();
            }
        }
    }

    private boolean mPostponeDestroy = false;
    private boolean mShouldDestroy = false;

    public void postponeDestroy() {
        mPostponeDestroy = true;
    }

    public void performPostponedDestroy() {
        mPostponeDestroy = false;
        if (mShouldDestroy) {
            destroy();
        }
    }

    /**
     * Remove the tab from the parent
     */
    void removeFromTree() {
        // detach the children
        if (mChildren != null) {
            for(Tab t : mChildren) {
                t.setParent(null);
            }
        }
        // remove itself from the parent list
        if (mParent != null) {
            mParent.mChildren.remove(this);
        }

        mCapture = null;
        deleteThumbnail();
    }

    /**
     * Create a new subwindow unless a subwindow already exists.
     * @return True if a new subwindow was created. False if one already exists.
     */
    boolean createSubWindow() {
        if (mSubView == null) {
            mWebViewController.createSubWindow(this);
            mSubView.setWebViewClient(new SubWindowClient(mWebViewClient,
                    mWebViewController));
            mSubView.setWebChromeClient(new SubWindowChromeClient(
                    mWebChromeClient));
            // Set a different DownloadListener for the mSubView, since it will
            // just need to dismiss the mSubView, rather than close the Tab
            mSubView.setDownloadListener(new BrowserDownloadListener() {
                public void onDownloadStart(String url, String userAgent,
                        String contentDisposition, String mimetype, String referer, String auth,
                        long contentLength) {
                    mWebViewController.onDownloadStart(Tab.this, url, userAgent,
                            contentDisposition, mimetype, referer, auth, contentLength);
                    if (mSubView.copyBackForwardList().getSize() == 0) {
                        // This subwindow was opened for the sole purpose of
                        // downloading a file. Remove it.
                        mWebViewController.dismissSubWindow(Tab.this);
                    }
                }
            });
            mSubView.setOnCreateContextMenuListener(mWebViewController.getActivity());
            return true;
        }
        return false;
    }

    /**
     * Dismiss the subWindow for the tab.
     */
    void dismissSubWindow() {
        if (mSubView != null) {
            mWebViewController.endActionMode();
            mSubView.destroy();
            mSubView = null;
            mSubViewContainer = null;
        }
    }


    /**
     * Set the parent tab of this tab.
     */
    void setParent(Tab parent) {
        if (parent == this) {
            throw new IllegalStateException("Cannot set parent to self!");
        }
        mParent = parent;
        // This tab may have been freed due to low memory. If that is the case,
        // the parent tab id is already saved. If we are changing that id
        // (most likely due to removing the parent tab) we must update the
        // parent tab id in the saved Bundle.
        if (mSavedState != null) {
            if (parent == null) {
                mSavedState.remove(PARENTTAB);
            } else {
                mSavedState.putLong(PARENTTAB, parent.getId());
            }
        }

        // Sync the WebView useragent with the parent
        if (parent != null && mSettings.hasDesktopUseragent(parent.getWebView())
                != mSettings.hasDesktopUseragent(getWebView())) {
            mSettings.toggleDesktopUseragent(getWebView());
        }

        if (parent != null && parent.getId() == getId()) {
            throw new IllegalStateException("Parent has same ID as child!");
        }
    }

    /**
     * If this Tab was created through another Tab, then this method returns
     * that Tab.
     * @return the Tab parent or null
     */
    public Tab getParent() {
        return mParent;
    }

    /**
     * When a Tab is created through the content of another Tab, then we
     * associate the Tabs.
     * @param child the Tab that was created from this Tab
     */
    void addChildTab(Tab child) {
        if (mChildren == null) {
            mChildren = new Vector<Tab>();
        }
        mChildren.add(child);
        child.setParent(this);
    }

    Vector<Tab> getChildren() {
        return mChildren;
    }

    void resume() {
        if (mMainView != null) {
            setupHwAcceleration(mMainView);
            mMainView.onResume();
            if (mSubView != null) {
                mSubView.onResume();
            }
        }
    }

    private void setupHwAcceleration(View web) {
        if (web == null) return;
        BrowserSettings settings = BrowserSettings.getInstance();
        if (settings.isHardwareAccelerated()) {
            web.setLayerType(View.LAYER_TYPE_NONE, null);
        } else {
            web.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    void pause() {
        if (mMainView != null) {
            mMainView.onPause();
            if (mSubView != null) {
                mSubView.onPause();
            }
        }
    }

    void putInForeground() {
        if (mInForeground) {
            return;
        }
        mInForeground = true;
        resume();
        Activity activity = mWebViewController.getActivity();
        mMainView.setOnCreateContextMenuListener(activity);
        if (mSubView != null) {
            mSubView.setOnCreateContextMenuListener(activity);
        }

        mWebViewController.bookmarkedStatusHasChanged(this);
    }

    void putInBackground() {
        if (!mInForeground) {
            return;
        }
        mInForeground = false;
        pause();
        mMainView.setOnCreateContextMenuListener(null);
        if (mSubView != null) {
            mSubView.setOnCreateContextMenuListener(null);
        }
    }

    boolean inForeground() {
        return mInForeground;
    }

    /**
     * Return the top window of this tab; either the subwindow if it is not
     * null or the main window.
     * @return The top window of this tab.
     */
    WebView getTopWindow() {
        if (mSubView != null) {
            return mSubView;
        }
        return mMainView;
    }

    /**
     * Return the main window of this tab. Note: if a tab is freed in the
     * background, this can return null. It is only guaranteed to be
     * non-null for the current tab.
     * @return The main WebView of this tab.
     */
    WebView getWebView() {
        return mMainView;
    }

    void setViewContainer(View container) {
        mContainer = container;
    }

    View getViewContainer() {
        return mContainer;
    }

    /**
     * Return whether private browsing is enabled for the main window of
     * this tab.
     * @return True if private browsing is enabled.
     */
    boolean isPrivateBrowsingEnabled() {
        return mCurrentState.mIncognito;
    }

    /**
     * Return the subwindow of this tab or null if there is no subwindow.
     * @return The subwindow of this tab or null.
     */
    WebView getSubWebView() {
        return mSubView;
    }

    void setSubWebView(WebView subView) {
        mSubView = subView;
    }

    View getSubViewContainer() {
        return mSubViewContainer;
    }

    void setSubViewContainer(View subViewContainer) {
        mSubViewContainer = subViewContainer;
    }


    /**
     * @return The application id string
     */
    String getAppId() {
        return mAppId;
    }

    /**
     * Set the application id string
     * @param id
     */
    void setAppId(String id) {
        mAppId = id;
    }

    boolean closeOnBack() {
        return mCloseOnBack;
    }

    void setCloseOnBack(boolean close) {
        mCloseOnBack = close;
    }

    boolean getDerivedFromIntent() {
        return mDerivedFromIntent;
    }

    void setDerivedFromIntent(boolean derived) {
        mDerivedFromIntent = derived;
    }

    String getUrl() {
        return UrlUtils.filteredUrl(mCurrentState.mUrl);
    }


    protected void onPageFinished() {
        mPageFinished = true;
        isDistillable();
    }

    public boolean getPageFinishedStatus() {
        return mPageFinished;
    }

    String getOriginalUrl() {
        if (mCurrentState.mOriginalUrl == null) {
            return getUrl();
        }
        return UrlUtils.filteredUrl(mCurrentState.mOriginalUrl);
    }

    /**
     * Get the title of this tab.
     */
    String getTitle() {
        return mCurrentState.mTitle;
    }

    /**
     * Get the favicon of this tab.
     */
    Bitmap getFavicon() {
        if (mCurrentState.mFavicon != null) {
            return mCurrentState.mFavicon;
        }
        return getDefaultFavicon(mContext);
    }

    public boolean hasFavicon() {
        return mCurrentState.mFavicon != null;
    }

    public boolean isBookmarkedSite() {
        return mCurrentState.mIsBookmarkedSite;
    }

    /**
     * Sets the security state, clears the SSL certificate error and informs
     * the controller.
     */
    private void setSecurityState(SecurityState securityState) {
        mCurrentState.mSecurityState = securityState;
        mWebViewController.onUpdatedSecurityState(this);
    }

    /**
     * @return The tab's security state.
     */
    SecurityState getSecurityState() {
        return mCurrentState.mSecurityState;
    }

    int getLoadProgress() {
        if (mInPageLoad) {
            return mPageLoadProgress;
        }
        return 100;
    }

    /**
     * @return TRUE if onPageStarted is called while onPageFinished is not
     *         called yet.
     */
    boolean inPageLoad() {
        return mInPageLoad;
    }

    /**
     * @return The Bundle with the tab's state if it can be saved, otherwise null
     */
    public Bundle saveState() {
        // If the WebView is null it means we ran low on memory and we already
        // stored the saved state in mSavedState.
        if (mMainView == null) {
            return mSavedState;
        }

        if (TextUtils.isEmpty(mCurrentState.mUrl)) {
            return null;
        }

        mSavedState = new Bundle();
        WebBackForwardList savedList = mMainView.saveState(mSavedState);
        if (savedList == null || savedList.getSize() == 0) {
            Log.w(LOGTAG, "Failed to save back/forward list for "
                    + mCurrentState.mUrl);
        }

        mSavedState.putLong(ID, mId);
        mSavedState.putString(CURRURL, mCurrentState.mUrl);
        mSavedState.putString(CURRTITLE, mCurrentState.mTitle);
        mSavedState.putBoolean(INCOGNITO, mMainView.isPrivateBrowsingEnabled());
        if (mAppId != null) {
            mSavedState.putString(APPID, mAppId);
        }
        mSavedState.putBoolean(CLOSEFLAG, mCloseOnBack);
        // Remember the parent tab so the relationship can be restored.
        if (mParent != null) {
            mSavedState.putLong(PARENTTAB, mParent.mId);
        }
        mSavedState.putBoolean(USERAGENT,
                mSettings.hasDesktopUseragent(getWebView()));
        return mSavedState;
    }

    /*
     * Restore the state of the tab.
     */
    private void restoreState(Bundle b) {
        mSavedState = b;
        if (mSavedState == null) {
            return;
        }
        // Restore the internal state even if the WebView fails to restore.
        // This will maintain the app id, original url and close-on-exit values.
        mId = b.getLong(ID);
        mAppId = b.getString(APPID);
        mCloseOnBack = b.getBoolean(CLOSEFLAG);
        restoreUserAgent();
        String url = b.getString(CURRURL);
        String title = b.getString(CURRTITLE);
        boolean incognito = b.getBoolean(INCOGNITO);
        mCurrentState = new PageState(mContext, incognito, url);
        mCurrentState.mTitle = title;
        synchronized (Tab.this) {
            if (mCapture != null) {
                DataController.getInstance(mContext).loadThumbnail(this);
            }
        }
    }

    private void restoreUserAgent() {
        if (mMainView == null || mSavedState == null) {
            return;
        }
        if (mSavedState.getBoolean(USERAGENT)
                != mSettings.hasDesktopUseragent(mMainView)) {
            mSettings.toggleDesktopUseragent(mMainView);
        }
    }

    public void updateBookmarkedStatus() {
        mDataController.queryBookmarkStatus(getUrl(), mIsBookmarkCallback);
    }

    private DataController.OnQueryUrlIsBookmark mIsBookmarkCallback
            = new DataController.OnQueryUrlIsBookmark() {
        @Override
        public void onQueryUrlIsBookmark(String url, boolean isBookmark) {
            if (TextUtils.isEmpty(mCurrentState.mUrl) || mCurrentState.mUrl.equals(url)) {
                mCurrentState.mIsBookmarkedSite = isBookmark;
                mWebViewController.bookmarkedStatusHasChanged(Tab.this);
            }
        }
    };

    public Bitmap getScreenshot() {
        synchronized (Tab.this) {
            return mCapture;
        }
    }

    public boolean isSnapshot() {
        return false;
    }

    private static class SaveCallback implements ValueCallback<String> {
        boolean onReceiveValueCalled = false;
        private String mPath;

        @Override
        public void onReceiveValue(String path) {
            this.onReceiveValueCalled = true;
            this.mPath = path;
            synchronized (this) {
                notifyAll();
            }
        }

        public String getPath() {
          return mPath;
        }
    }

    /**
     * Must be called on the UI thread
     */
    public ContentValues createSnapshotValues(Bitmap bm) {
        WebView web = getWebView();
        if (web == null) return null;
        ContentValues values = new ContentValues();
        values.put(Snapshots.TITLE, mCurrentState.mTitle);
        values.put(Snapshots.URL, mCurrentState.mUrl);
        values.put(Snapshots.BACKGROUND, web.getPageBackgroundColor());
        values.put(Snapshots.DATE_CREATED, System.currentTimeMillis());
        values.put(Snapshots.FAVICON, compressBitmap(getFavicon()));
        values.put(Snapshots.THUMBNAIL, compressBitmap(bm));
        return values;
    }

    /**
     * Probably want to call this on a background thread
     */
    public boolean saveViewState(ContentValues values) {
        WebView web = getWebView();
        if (web == null) return false;
        String filename = UUID.randomUUID().toString();
        SaveCallback callback = new SaveCallback();
        try {
            synchronized (callback) {
               web.saveViewState(filename, callback);
               callback.wait();
            }
        } catch (Exception e) {
            Log.w(LOGTAG, "Failed to save view state", e);
            String path = callback.getPath();
            if (path != null) {
                File file = mContext.getFileStreamPath(path);
                if (file.exists() && !file.delete()) {
                    file.deleteOnExit();
                }
            }
            return false;
        }

        String path = callback.getPath();
        // could be that saving of file failed
        if (path == null) {
            return false;
        }

        File savedFile = new File(path);
        if (!savedFile.exists()) {
           return false;
        }
        values.put(Snapshots.VIEWSTATE_PATH, path.substring(path.lastIndexOf('/') + 1));
        values.put(Snapshots.VIEWSTATE_SIZE, savedFile.length());
        return true;
    }

    public byte[] compressBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public void loadUrl(String url, Map<String, String> headers) {
        if (mMainView != null) {
            mPageLoadProgress = INITIAL_PROGRESS;
            mCurrentState = new PageState(
                                mContext, mMainView.isPrivateBrowsingEnabled(), url);
            mMainView.loadUrl(url, headers);
        }
    }

    public void disableUrlOverridingForLoad() {
        mDisableOverrideUrlLoading = true;
    }

    private void thumbnailUpdated() {
        mHandler.removeMessages(MSG_CAPTURE);

        TabControl tc = mWebViewController.getTabControl();
        if (tc != null) {
            OnThumbnailUpdatedListener updateListener = tc.getOnThumbnailUpdatedListener();
            if (updateListener != null) {
                updateListener.onThumbnailUpdated(this);
            }
        }

        if (mViewportCapture != null) {
            mWebViewController.onThumbnailCapture(mViewportCapture);
            mViewportCapture.recycle();
            mViewportCapture = null;
        } else {
            mWebViewController.onThumbnailCapture(mCapture);
        }
    }

    protected void capture() {
        if (mMainView == null || mCapture == null || !mMainView.isReady() ||
                mMainView.getContentWidth() <= 0 || mMainView.getContentHeight() <= 0 ||
                !mFirstVisualPixelPainted || mMainView.isShowingCrashView()) {
            mViewportCapture = null;
            initCaptureBitmap();
            thumbnailUpdated();
            return;
        }
        int orientation = mWebViewController.getActivity().
                getResources().getConfiguration().orientation;
        int width = (orientation == Configuration.ORIENTATION_PORTRAIT) ? mMainView.getWidth() :
                    mMainView.getHeight();
        mMainView.getContentBitmapAsync((float) mCaptureWidth / width, new Rect(),
            new ValueCallback<Bitmap>() {
                @Override
                public void onReceiveValue(Bitmap bitmap) {
                    mViewportCapture = bitmap;

                    if (mCapture == null) {
                        initCaptureBitmap();
                    }

                    if (bitmap == null) {
                        thumbnailUpdated();
                        return;
                    }

                    Canvas c = new Canvas(mCapture);
                    mCapture.eraseColor(Color.WHITE);
                    c.drawBitmap(bitmap, 0, 0, null);

                    // manually anti-alias the edges for the tilt
                    c.drawRect(0, 0, 1, mCapture.getHeight(), sAlphaPaint);
                    c.drawRect(mCapture.getWidth() - 1, 0, mCapture.getWidth(),
                            mCapture.getHeight(), sAlphaPaint);
                    c.drawRect(0, 0, mCapture.getWidth(), 1, sAlphaPaint);
                    c.drawRect(0, mCapture.getHeight() - 1, mCapture.getWidth(),
                            mCapture.getHeight(), sAlphaPaint);
                    c.setBitmap(null);

                    persistThumbnail();
                    thumbnailUpdated();
                }
            }
        );
    }

    @Override
    public void onNewPicture(WebView view, Picture picture) {
    }

    public boolean canGoBack() {
        return mMainView != null ? mMainView.canGoBack() : false;
    }

    public boolean canGoForward() {
        return mMainView != null ? mMainView.canGoForward() : false;
    }

    public void goBack() {
        if (mMainView != null) {
            mMainView.goBack();
        }
    }

    public void goForward() {
        if (mMainView != null) {
            mMainView.goForward();
        }
    }

    protected void persistThumbnail() {
        DataController.getInstance(mContext).saveThumbnail(this);
    }

    protected void deleteThumbnail() {
        DataController.getInstance(mContext).deleteThumbnail(this);
    }

    void updateCaptureFromBlob(byte[] blob) {
        synchronized (Tab.this) {
            if (mCapture == null) {
                return;
            }
            ByteBuffer buffer = ByteBuffer.wrap(blob);
            try {
                mCapture.copyPixelsFromBuffer(buffer);
            } catch (RuntimeException rex) {
                Log.e(LOGTAG, "Load capture has mismatched sizes; buffer: "
                        + buffer.capacity() + " blob: " + blob.length
                        + "capture: " + mCapture.getByteCount());
                throw rex;
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(100);
        builder.append(mId);
        builder.append(") has parent: ");
        if (getParent() != null) {
            builder.append("true[");
            builder.append(getParent().getId());
            builder.append("]");
        } else {
            builder.append("false");
        }
        builder.append(", incog: ");
        builder.append(isPrivateBrowsingEnabled());
        if (!isPrivateBrowsingEnabled()) {
            builder.append(", title: ");
            builder.append(getTitle());
            builder.append(", url: ");
            builder.append(getUrl());
        }
        return builder.toString();
    }

    // dertermines if the tab contains a dislled page
    public boolean isDistilled() {
        if (!BrowserCommandLine.hasSwitch("reader-mode")) {
            return false;
        }
        try {
            return DomDistillerUtils.isUrlDistilled(getUrl());
        } catch (Exception e) {
            return false;
        }
    }

    //determines if the tab contains a distillable page
    public boolean isDistillable() {
        if (!BrowserCommandLine.hasSwitch("reader-mode")) {
            mIsDistillable = false;
            return mIsDistillable;
        }
        final ValueCallback<String> onIsDistillable =  new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String str) {
                mIsDistillable = Boolean.parseBoolean(str);
            }
        };

        if (isDistilled()) {
            mIsDistillable = true;
            return mIsDistillable;
        }

        try {
            DomDistillerUtils.isWebViewDistillable(getWebView(), onIsDistillable);
        } catch (Exception e) {
            mIsDistillable = false;
        }

        return mIsDistillable;
    }

    // Function that sets the mIsDistillable variable
    public void setIsDistillable(boolean value) {
        if (!BrowserCommandLine.hasSwitch("reader-mode")) {
            mIsDistillable = false;
        }
        mIsDistillable = value;
    }

    // Function that returns the distilled url of the current url
    public String getDistilledUrl() {
        if (getUrl() != null) {
            return DomDistillerUtils.getDistilledUrl(getUrl());
        }
        return new String();
    }

    // function that returns the non-distilled version of the current url
    public String getNonDistilledUrl() {
        if (getUrl() != null) {
            return DomDistillerUtils.getOriginalUrlFromDistilledUrl(getUrl());
        }
        return new String();
    }
}
