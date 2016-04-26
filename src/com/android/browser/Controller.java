/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Copyright (c) 2015 The Linux Foundation, All rights reserved.
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
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.ScanResult;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Intents.Insert;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.widget.EditText;
import android.widget.Toast;

import org.codeaurora.swe.CookieManager;
import org.codeaurora.swe.CookieSyncManager;
import org.codeaurora.swe.Engine;
import org.codeaurora.swe.HttpAuthHandler;
import org.codeaurora.swe.WebSettings;
import org.codeaurora.swe.WebView;
import org.codeaurora.swe.WebBackForwardList;
import org.codeaurora.swe.WebHistoryItem;

import com.android.browser.IntentHandler.UrlData;
import com.android.browser.UI.ComboViews;
import com.android.browser.mdm.DownloadDirRestriction;
import com.android.browser.mdm.EditBookmarksRestriction;
import com.android.browser.mdm.IncognitoRestriction;
import com.android.browser.mdm.URLFilterRestriction;
import com.android.browser.mynavigation.AddMyNavigationPage;
import com.android.browser.mynavigation.MyNavigationUtil;
import com.android.browser.platformsupport.Browser;
import com.android.browser.platformsupport.BrowserContract;
import com.android.browser.preferences.AboutPreferencesFragment;
import com.android.browser.provider.BrowserProvider2.Thumbnails;
import com.android.browser.provider.SnapshotProvider.Snapshots;
import com.android.browser.reflect.ReflectHelper;
import com.android.browser.appmenu.AppMenuHandler;
import com.android.browser.appmenu.AppMenuPropertiesDelegate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controller for browser
 */
public class Controller
        implements WebViewController, UiController, ActivityController,
        AppMenuPropertiesDelegate {

    private static final String LOGTAG = "Controller";
    private static final String SEND_APP_ID_EXTRA =
        "android.speech.extras.SEND_APPLICATION_ID_EXTRA";
    public static final String INCOGNITO_URI = "chrome://incognito";
    public static final String EXTRA_REQUEST_CODE = "_fake_request_code_";
    public static final String EXTRA_RESULT_CODE = "_fake_result_code_";
    public static final String EXTRA_UPDATED_URLS = "updated_urls";

    // Remind switch to data connection if wifi is unavailable
    private static final int NETWORK_SWITCH_TYPE_OK = 1;

    // public message ids
    public final static int LOAD_URL = 1001;
    public final static int STOP_LOAD = 1002;

    // Message Ids
    private static final int FOCUS_NODE_HREF = 102;
    private static final int RELEASE_WAKELOCK = 107;
    private static final int UNKNOWN_TYPE_MSG = 109;

    private static final int OPEN_BOOKMARKS = 201;
    private static final int OPEN_MENU = 202;

    private static final int EMPTY_MENU = -1;

    // activity requestCode
    final static int COMBO_VIEW = 1;
    final static int PREFERENCES_PAGE = 3;
    final static int FILE_SELECTED = 4;
    final static int AUTOFILL_SETUP = 5;
    final static int VOICE_RESULT = 6;
    final static int MY_NAVIGATION = 7;

    private final static int WAKELOCK_TIMEOUT = 5 * 60 * 1000; // 5 minutes

    // As the ids are dynamically created, we can't guarantee that they will
    // be in sequence, so this static array maps ids to a window number.
    final static private int[] WINDOW_SHORTCUT_ID_ARRAY =
    { R.id.window_one_menu_id, R.id.window_two_menu_id,
      R.id.window_three_menu_id, R.id.window_four_menu_id,
      R.id.window_five_menu_id, R.id.window_six_menu_id,
      R.id.window_seven_menu_id, R.id.window_eight_menu_id };

    // "source" parameter for Google search through search key
    final static String GOOGLE_SEARCH_SOURCE_SEARCHKEY = "browser-key";
    // "source" parameter for Google search through simplily type
    final static String GOOGLE_SEARCH_SOURCE_TYPE = "browser-type";

    // "no-crash-recovery" parameter in intent to suppress crash recovery
    final static String NO_CRASH_RECOVERY = "no-crash-recovery";

    // A bitmap that is re-used in createScreenshot as scratch space
    private static Bitmap sThumbnailBitmap;

    private Activity mActivity;
    private UI mUi;
    private HomepageHandler mHomepageHandler;
    protected TabControl mTabControl;
    private BrowserSettings mSettings;
    private WebViewFactory mFactory;

    private WakeLock mWakeLock;

    private UrlHandler mUrlHandler;
    private UploadHandler mUploadHandler;
    private IntentHandler mIntentHandler;
    private NetworkStateHandler mNetworkHandler;

    private Message mAutoFillSetupMessage;

    private boolean mNetworkShouldNotify = true;

    // FIXME, temp address onPrepareMenu performance problem.
    // When we move everything out of view, we should rewrite this.
    private int mCurrentMenuState = 0;
    private int mMenuState = EMPTY_MENU;
    private int mOldMenuState = EMPTY_MENU;
    private Menu mCachedMenu;

    private boolean mMenuIsDown;

    private boolean mWasInPageLoad = false;
    private AppMenuHandler mAppMenuHandler;

    // For select and find, we keep track of the ActionMode so that
    // finish() can be called as desired.
    private ActionMode mActionMode;

    /**
     * Only meaningful when mOptionsMenuOpen is true.  This variable keeps track
     * of whether the configuration has changed.  The first onMenuOpened call
     * after a configuration change is simply a reopening of the same menu
     * (i.e. mIconView did not change).
     */
    private boolean mConfigChanged;

    /**
     * Keeps track of whether the options menu is open. This is important in
     * determining whether to show or hide the title bar overlay
     */
    private boolean mOptionsMenuOpen;

    /**
     * Whether or not the options menu is in its bigger, popup menu form. When
     * true, we want the title bar overlay to be gone. When false, we do not.
     * Only meaningful if mOptionsMenuOpen is true.
     */
    private boolean mExtendedMenuOpen;

    private boolean mActivityPaused = true;
    private boolean mActivityStopped = true;
    private boolean mLoadStopped;

    private Handler mHandler;
    // Checks to see when the bookmarks database has changed, and updates the
    // Tabs' notion of whether they represent bookmarked sites.
    private ContentObserver mBookmarksObserver;
    private CrashRecoveryHandler mCrashRecoveryHandler;

    private boolean mBlockEvents;

    private String mVoiceResult;
    private boolean mUpdateMyNavThumbnail;
    private String mUpdateMyNavThumbnailUrl;
    private float mLevel = 0.0f;
    private WebView.HitTestResult mResult;
    private PowerConnectionReceiver mLowPowerReceiver;
    private PowerConnectionReceiver mPowerChangeReceiver;

    private boolean mCurrentPageBookmarked;

    private Tab mLatestCreatedTab;

    private List<ValueCallback> mThumbnailCbList;

    private CountDownTimer mCaptureTimer;
    private static final int mCaptureMaxWaitMS = 200;

    public Controller(Activity browser) {
        mActivity = browser;
        mSettings = BrowserSettings.getInstance();
        mTabControl = new TabControl(this);
        mSettings.setController(this);
        mCrashRecoveryHandler = CrashRecoveryHandler.initialize(this);
        mCrashRecoveryHandler.preloadCrashState();
        mFactory = new BrowserWebViewFactory(browser);

        mUrlHandler = new UrlHandler(this);
        mIntentHandler = new IntentHandler(mActivity, this);

        startHandler();
        mBookmarksObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                int size = mTabControl.getTabCount();
                for (int i = 0; i < size; i++) {
                    mTabControl.getTab(i).updateBookmarkedStatus();
                }
            }

        };
        browser.getContentResolver().registerContentObserver(
                BrowserContract.Bookmarks.CONTENT_URI, true, mBookmarksObserver);

        mNetworkHandler = new NetworkStateHandler(mActivity, this);
        mHomepageHandler = new HomepageHandler(browser, this);
        mAppMenuHandler = new AppMenuHandler(browser, this, R.menu.browser);
        mThumbnailCbList = new ArrayList<ValueCallback>();
    }

    @Override
    public void start(final Intent intent) {
        mMenuState = R.id.MAIN_MENU;
        WebView.setShouldMonitorWebCoreThread();
        // mCrashRecoverHandler has any previously saved state.
        mCrashRecoveryHandler.startRecovery(intent);
    }

    void doStart(final Bundle icicle, final Intent intent) {
        // we dont want to ever recover incognito tabs
        final boolean restoreIncognitoTabs = false;

        // Find out if we will restore any state and remember the tab.
        final long currentTabId =
                mTabControl.canRestoreState(icicle, restoreIncognitoTabs);

        if (currentTabId == -1) {
            // Not able to restore so we go ahead and clear session cookies.  We
            // must do this before trying to login the user as we don't want to
            // clear any session cookies set during login.
            CookieManager.getInstance().removeSessionCookie();
            BackgroundHandler.execute(new PruneThumbnails(mActivity, null));
            if (intent == null) {
                // This won't happen under common scenarios. The icicle is
                // not null, but there aren't any tabs to restore.
                openTabToHomePage();
            } else {
                final Bundle extra = intent.getExtras();
                // Create an initial tab.
                // If the intent is ACTION_VIEW and data is not null, the Browser is
                // invoked to view the content by another application. In this case,
                // the tab will be close when exit.
                UrlData urlData = null;
                if (intent.getData() != null
                        && Intent.ACTION_VIEW.equals(intent.getAction())
                        && intent.getData().toString().startsWith("content://")) {
                    urlData = new UrlData(intent.getData().toString());
                } else {
                    urlData = IntentHandler.getUrlDataFromIntent(intent);
                }
                Tab t = null;
                if (urlData.isEmpty()) {
                    String landingPage = mActivity.getResources().getString(
                                                   R.string.def_landing_page);
                    if (!landingPage.isEmpty()) {
                        t = openTab(landingPage, false, true, true);
                    } else {
                        t = openTabToHomePage();
                    }
                } else {
                    t = openTab(urlData);
                    t.setDerivedFromIntent(true);
                }
                if (t != null) {
                    t.setAppId(intent.getStringExtra(Browser.EXTRA_APPLICATION_ID));
                }
                WebView webView = t.getWebView();
                if (extra != null) {
                    int scale = extra.getInt(Browser.INITIAL_ZOOM_LEVEL, 0);
                    if (scale > 0 && scale <= 1000) {
                        webView.setInitialScale(scale);
                    }
                }
            }
            mUi.updateTabs(mTabControl.getTabs());
        } else {
            mTabControl.restoreState(icicle, currentTabId, restoreIncognitoTabs,
                    mUi.needsRestoreAllTabs());
            List<Tab> tabs = mTabControl.getTabs();
            ArrayList<Long> restoredTabs = new ArrayList<Long>(tabs.size());

            for (Tab t : tabs) {
                //handle restored pages that may require a JS interface
                if (t.getWebView() != null) {
                    WebBackForwardList backForwardList = t.getWebView().copyBackForwardList();
                    if (backForwardList != null) {
                        for (int i = 0; i <  backForwardList.getSize(); i++) {
                            WebHistoryItem item = backForwardList.getItemAtIndex(i);
                            mHomepageHandler.registerJsInterface( t.getWebView(), item.getUrl());
                        }
                    }
                }
                restoredTabs.add(t.getId());
                if (t != mTabControl.getCurrentTab()) {
                    t.pause();
                }
            }
            BackgroundHandler.execute(new PruneThumbnails(mActivity, restoredTabs));
            if (tabs.size() == 0) {
                openTabToHomePage();
            }
            mUi.updateTabs(tabs);
            // TabControl.restoreState() will create a new tab even if
            // restoring the state fails.
            setActiveTab(mTabControl.getCurrentTab());
            // Intent is non-null when framework thinks the browser should be
            // launching with a new intent (icicle is null).
            if (intent != null) {
                mIntentHandler.onNewIntent(intent);
            }
        }
        // Read JavaScript flags if it exists.
        String jsFlags = getSettings().getJsEngineFlags();
        if (jsFlags.trim().length() != 0) {
            getCurrentWebView().setJsFlags(jsFlags);
        }
        if (intent != null
                && BrowserActivity.ACTION_SHOW_BOOKMARKS.equals(intent.getAction())) {
            bookmarksOrHistoryPicker(ComboViews.Bookmarks);
        }
        mLowPowerReceiver = new PowerConnectionReceiver();
        mPowerChangeReceiver = new PowerConnectionReceiver();

        //always track the android framework's power save mode
        IntentFilter filter = new IntentFilter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Power save mode only exists in Lollipop and above
            filter.addAction("android.os.action.POWER_SAVE_MODE_CHANGED");
        }
        filter.addAction(Intent.ACTION_BATTERY_OKAY);
        mActivity.registerReceiver(mPowerChangeReceiver, filter);
    }

    private static class PruneThumbnails implements Runnable {
        private Context mContext;
        private List<Long> mIds;

        PruneThumbnails(Context context, List<Long> preserveIds) {
            mContext = context.getApplicationContext();
            mIds = preserveIds;
        }

        @Override
        public void run() {
            ContentResolver cr = mContext.getContentResolver();
            if (mIds == null || mIds.size() == 0) {
                cr.delete(Thumbnails.CONTENT_URI, null, null);
            } else {
                int length = mIds.size();
                StringBuilder where = new StringBuilder();
                where.append(Thumbnails._ID);
                where.append(" not in (");
                for (int i = 0; i < length; i++) {
                    where.append(mIds.get(i));
                    if (i < (length - 1)) {
                        where.append(",");
                    }
                }
                where.append(")");
                cr.delete(Thumbnails.CONTENT_URI, where.toString(), null);
            }
        }

    }

    @Override
    public WebViewFactory getWebViewFactory() {
        return mFactory;
    }

    @Override
    public void onSetWebView(Tab tab, WebView view) {
        mUi.onSetWebView(tab, view);
        URLFilterRestriction.getInstance();
    }

    @Override
    public void createSubWindow(Tab tab) {
        endActionMode();
        WebView mainView = tab.getWebView();
        WebView subView = mFactory.createWebView((mainView == null)
                ? false
                : mainView.isPrivateBrowsingEnabled());
        mUi.createSubWindow(tab, subView);
    }

    @Override
    public Context getContext() {
        return mActivity;
    }

    @Override
    public Activity getActivity() {
        return mActivity;
    }

    void setUi(UI ui) {
        mUi = ui;
    }

    @Override
    public BrowserSettings getSettings() {
        return mSettings;
    }

    IntentHandler getIntentHandler() {
        return mIntentHandler;
    }

    @Override
    public UI getUi() {
        return mUi;
    }

    int getMaxTabs() {
        return mActivity.getResources().getInteger(R.integer.max_tabs);
    }

    @Override
    public TabControl getTabControl() {
        return mTabControl;
    }

    @Override
    public List<Tab> getTabs() {
        return mTabControl.getTabs();
    }

    private void startHandler() {
        mHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case OPEN_BOOKMARKS:
                        bookmarksOrHistoryPicker(ComboViews.Bookmarks);
                        break;
                    case UNKNOWN_TYPE_MSG:
                        HashMap unknownTypeMap = (HashMap) msg.obj;
                        WebView viewForUnknownType = (WebView) unknownTypeMap.get("webview");
                        /*
                        *  When the context menu is shown to the user
                        *  we need to assure that its happening on the current webview
                        *  and its the current webview only which had sent the UNKNOWN_TYPE_MSG
                        */
                        if (getCurrentWebView() != viewForUnknownType)
                            break;

                        String unknown_type_src = (String)msg.getData().get("src");
                        String unknown_type_url = (String)msg.getData().get("url");
                        WebView.HitTestResult result = new WebView.HitTestResult();

                        // Prevent unnecessary calls to context menu
                        // if url and image src are null
                        if (unknown_type_src == null && unknown_type_url == null)
                            break;

                        //setting the HitTestResult with new RESULT TYPE
                        if (!TextUtils.isEmpty(unknown_type_src)) {
                            result.setType(WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE);
                            result.setExtra(unknown_type_src);
                        } else {
                            result.setType(WebView.HitTestResult.SRC_ANCHOR_TYPE);
                            result.setExtra("about:blank");
                        }

                        mResult = result;
                        openContextMenu(viewForUnknownType);
                        mResult = null;

                        break;

                    case FOCUS_NODE_HREF:
                    {
                        String url = (String) msg.getData().get("url");
                        String title = (String) msg.getData().get("title");
                        String src = (String) msg.getData().get("src");
                        if (url == "") url = src; // use image if no anchor
                        if (TextUtils.isEmpty(url)) {
                            break;
                        }
                        HashMap focusNodeMap = (HashMap) msg.obj;
                        WebView view = (WebView) focusNodeMap.get("webview");
                        // Only apply the action if the top window did not change.
                        if (getCurrentTopWebView() != view) {
                            break;
                        }
                        switch (msg.arg1) {
                            case R.id.open_context_menu_id:
                                loadUrlFromContext(url);
                                break;
                            case R.id.view_image_context_menu_id:
                                loadUrlFromContext(src);
                                break;
                            case R.id.open_newtab_context_menu_id:
                                final Tab parent = mTabControl.getCurrentTab();
                                openTab(url, parent,
                                        !mSettings.openInBackground(), true);
                                break;
                            case R.id.copy_link_context_menu_id:
                                copy(url);
                                break;
                            case R.id.save_link_context_menu_id:
                            case R.id.download_context_menu_id:
                                DownloadHandler.onDownloadStartNoStream(
                                  mActivity, url, view.getSettings().getUserAgentString(),
                                  null, null, null, null, view.isPrivateBrowsingEnabled(), 0);
                                break;
                            case R.id.save_link_bookmark_context_menu_id:
                                if(title == null || title == "")
                                    title = url;

                                Intent bookmarkIntent = new Intent(mActivity, AddBookmarkPage.class);
                                //SWE TODO: No thumbnail support for the url obtained via
                                //browser context menu as its not loaded in webview.
                                if (bookmarkIntent != null) {
                                    bookmarkIntent.putExtra(BrowserContract.Bookmarks.URL, url);
                                    bookmarkIntent.putExtra(BrowserContract.Bookmarks.TITLE, title);
                                    mActivity.startActivity(bookmarkIntent);
                                }
                                break;
                        }
                        break;
                    }

                    case LOAD_URL:
                        loadUrlFromContext((String) msg.obj);
                        break;

                    case STOP_LOAD:
                        stopLoading();
                        break;

                    case RELEASE_WAKELOCK:
                        if (mWakeLock != null && mWakeLock.isHeld()) {
                            mWakeLock.release();
                            // if we reach here, Browser should be still in the
                            // background loading after WAKELOCK_TIMEOUT (5-min).
                            // To avoid burning the battery, stop loading.
                            mTabControl.stopAllLoading();
                        }
                        break;

                    case OPEN_MENU:
                        if (!mOptionsMenuOpen && mActivity != null ) {
                            mActivity.openOptionsMenu();
                        }
                        break;
                }
            }
        };

    }

    @Override
    public Tab getCurrentTab() {
        return mTabControl.getCurrentTab();
    }

    @Override
    public void shareCurrentPage() {
        shareCurrentPage(mTabControl.getCurrentTab());
    }

    private void shareCurrentPage(Tab tab) {
        if (tab == null || tab.getWebView() == null)
            return;

        final Tab mytab = tab;

        createScreenshotAsync(
            tab,
            new ValueCallback<Bitmap>() {
                @Override
                public void onReceiveValue(Bitmap bitmap) {
                    Bitmap bm = cropAndScaleBitmap(bitmap,
                            getDesiredThumbnailWidth(mActivity),
                            getDesiredThumbnailHeight(mActivity));
                    sharePage(mActivity, mytab.getTitle(), mytab.getUrl(),
                          mytab.getFavicon(), bm);
                }
            });
    }

    /**
     * Share a page, providing the title, url, favicon, and a screenshot.  Uses
     * an {@link Intent} to launch the Activity chooser.
     * @param c Context used to launch a new Activity.
     * @param title Title of the page.  Stored in the Intent with
     *          {@link Intent#EXTRA_SUBJECT}
     * @param url URL of the page.  Stored in the Intent with
     *          {@link Intent#EXTRA_TEXT}
     * @param favicon Bitmap of the favicon for the page.  Stored in the Intent
     *          with {@link Browser#EXTRA_SHARE_FAVICON}
     * @param screenshot Bitmap of a screenshot of the page.  Stored in the
     *          Intent with {@link Browser#EXTRA_SHARE_SCREENSHOT}
     */
    static final void sharePage(Context c, String title, String url,
            Bitmap favicon, Bitmap screenshot) {

        ShareDialog sDialog = new ShareDialog((Activity)c, title, url, favicon, screenshot);

        sDialog.sharePage();
    }

    private void copy(CharSequence text) {
        ClipboardManager cm = (ClipboardManager) mActivity
                .getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setText(text);
    }

    // lifecycle

    @Override
    public void onConfgurationChanged(Configuration config) {
        mConfigChanged = true;
        // update the menu in case of a locale change
        mActivity.invalidateOptionsMenu();
        mAppMenuHandler.hideAppMenu();
        if (mOptionsMenuOpen) {
            mActivity.closeOptionsMenu();
            mHandler.sendMessageDelayed(mHandler.obtainMessage(OPEN_MENU), 100);
        }
        mUi.onConfigurationChanged(config);
    }

    @Override
    public void handleNewIntent(Intent intent) {
        if (!mUi.isWebShowing()) {
            mUi.showWeb(false);
        }
        mIntentHandler.onNewIntent(intent);
    }

    @Override
    public void onPause() {
        if (mUi.isCustomViewShowing()) {
            hideCustomView();
        }
        if (mActivityPaused) {
            Log.e(LOGTAG, "BrowserActivity is already paused.");
            return;
        }
        mActivityPaused = true;

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save all the tabs
        Bundle saveState = createSaveState();

        // crash recovery manages all save & restore state
        mCrashRecoveryHandler.writeState(saveState);
        mSettings.setLastRunPaused(true);
    }

    /**
     * Save the current state to outState. Does not write the state to
     * disk.
     * @return Bundle containing the current state of all tabs.
     */
    /* package */ Bundle createSaveState() {
        Bundle saveState = new Bundle();
        mTabControl.saveState(saveState);
        // This method is called multiple times.Need to
        // guard against TabControl not having any tabs
        // during the destroy cycles which looses all the
        // existing saved information.
        if (saveState.isEmpty())
           return null;
        return saveState;
    }

    @Override
    public void onResume() {
        if (!mActivityPaused) {
            Log.e(LOGTAG, "BrowserActivity is already resumed.");
            return;
        }
        mActivityPaused = false;
        if (mVoiceResult != null) {
            mUi.onVoiceResult(mVoiceResult);
            mVoiceResult = null;
        }
    }

    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mHandler.removeMessages(RELEASE_WAKELOCK);
            mWakeLock.release();
        }
    }

    @Override
    public void onStop() {
        if (mActivityStopped) {
            Log.e(LOGTAG, "BrowserActivity is already stoped.");
            return;
        }
        mActivityStopped = true;
        Tab tab = mTabControl.getCurrentTab();
        if (tab != null) {
            tab.pause();
            if (!pauseWebViewTimers(tab)) {
                if (mWakeLock == null) {
                    PowerManager pm = (PowerManager) mActivity
                            .getSystemService(Context.POWER_SERVICE);
                    mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Browser");
                }
                mWakeLock.acquire();
                mHandler.sendMessageDelayed(mHandler
                        .obtainMessage(RELEASE_WAKELOCK), WAKELOCK_TIMEOUT);
            }
        }
        mUi.onPause();
        mNetworkHandler.onPause();
        NfcHandler.unregister(mActivity);
        if (mLowPowerReceiver != null)
            mActivity.unregisterReceiver(mLowPowerReceiver);
    }

    @Override
    public void onStart() {
        if (!mActivityStopped) {
            Log.e(LOGTAG, "BrowserActivity is already started.");
            return;
        }
        mActivityStopped = false;
        UpdateNotificationService.updateCheck(mActivity);
        // reset the search engine based on locale
        mSettings.updateSearchEngine(false);
        mSettings.setLastRunPaused(false);
        Tab current = mTabControl.getCurrentTab();
        if (current != null) {
            current.resume();
            resumeWebViewTimers(current);
        }
        releaseWakeLock();

        mUi.onResume();
        mNetworkHandler.onResume();
        NfcHandler.register(mActivity, this);
        if (current != null && current.getWebView().isShowingCrashView())
            current.getWebView().reload();
        mActivity.registerReceiver(mLowPowerReceiver, new IntentFilter(Intent.ACTION_BATTERY_LOW));

    }

    /**
     * resume all WebView timers using the WebView instance of the given tab
     * @param tab guaranteed non-null
     */
    private void resumeWebViewTimers(Tab tab) {
        boolean inLoad = tab.inPageLoad();
        if ((!mActivityStopped && !inLoad) || (mActivityStopped && inLoad)) {
            CookieSyncManager.getInstance().startSync();
            WebView w = tab.getWebView();
            WebViewTimersControl.getInstance().onBrowserActivityResume(w);
        }
    }

    /**
     * Pause all WebView timers using the WebView of the given tab
     * @param tab
     * @return true if the timers are paused or tab is null
     */
    private boolean pauseWebViewTimers(Tab tab) {
        if (tab == null) {
            return true;
        } else if (!tab.inPageLoad()) {
            CookieSyncManager.getInstance().stopSync();
            WebViewTimersControl.getInstance().onBrowserActivityPause(getCurrentWebView());
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        if (mUploadHandler != null && !mUploadHandler.handled()) {
            mUploadHandler.onResult(Activity.RESULT_CANCELED, null);
            mUploadHandler = null;
        }
        if (mTabControl == null) return;
        mUi.onDestroy();
        // Remove the current tab and sub window
        Tab t = mTabControl.getCurrentTab();
        if (t != null) {
            dismissSubWindow(t);
            removeTab(t);
        }
        mActivity.getContentResolver().unregisterContentObserver(mBookmarksObserver);
        // Destroy all the tabs
        mTabControl.destroy();
        // Unregister receiver
        if (mPowerChangeReceiver != null)
            mActivity.unregisterReceiver(mPowerChangeReceiver);
    }

    protected boolean isActivityPaused() {
        return mActivityPaused;
    }

    @Override
    public void onLowMemory() {
        mTabControl.freeMemory();
    }

    @Override
    public void stopLoading() {
        mLoadStopped = true;
        Tab tab = mTabControl.getCurrentTab();
        WebView w = getCurrentTopWebView();
        if (w != null) {
            w.stopLoading();
            mUi.onPageStopped(tab);
        }
    }

    boolean didUserStopLoading() {
        return mLoadStopped;
    }

    private void handleNetworkNotify(WebView view) {
        final String reminderType = getContext().getResources().getString(
                R.string.def_wifi_browser_interaction_remind_type);
        final String selectionConnnection = getContext().getResources().getString(
                R.string.def_action_wifi_selection_data_connections);
        final String wifiSelection = getContext().getResources().getString(
                R.string.def_intent_pick_network);

        if (reminderType.isEmpty() || selectionConnnection.isEmpty() ||
                wifiSelection.isEmpty())
            return;

        ConnectivityManager conMgr = (ConnectivityManager) this.getContext().getSystemService(
            Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conMgr.getActiveNetworkInfo();
        WifiManager wifiMgr = (WifiManager) this.getContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (networkInfo == null
            || (networkInfo != null && (networkInfo.getType() !=
                ConnectivityManager.TYPE_WIFI))) {
            int isReminder = Settings.System.getInt(mActivity.getContentResolver(),
                                                    reminderType, NETWORK_SWITCH_TYPE_OK);
            List<ScanResult> list = wifiMgr.getScanResults();
            // Have no AP's for Wifi's fall back to data
            if (list != null && list.size() == 0 && isReminder == NETWORK_SWITCH_TYPE_OK) {
                Intent intent = new Intent(selectionConnnection);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                this.getContext().startActivity(intent);
            } else {
                // Request to select Wifi AP
                try {
                    Intent intent = new Intent(wifiSelection);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    this.getContext().startActivity(intent);
                } catch (Exception e) {
                    String err_msg = this.getContext().getString(
                                R.string.activity_not_found, wifiSelection);
                    Toast.makeText(this.getContext(), err_msg, Toast.LENGTH_LONG).show();
                }
            }
            mNetworkShouldNotify = false;
        }
    }

    // WebViewController

    @Override
    public void onPageStarted(Tab tab, WebView view, Bitmap favicon) {

        // reset sync timer to avoid sync starts during loading a page
        CookieSyncManager.getInstance().resetSync();
        WifiManager wifiMgr = (WifiManager) this.getContext()
                .getSystemService(Context.WIFI_SERVICE);
        boolean networkNotifier = BrowserConfig.getInstance(getContext())
                .hasFeature(BrowserConfig.Feature.NETWORK_NOTIFIER);
        if (networkNotifier && mNetworkShouldNotify && wifiMgr.isWifiEnabled()){
            handleNetworkNotify(view);
        } else {
            if (!mNetworkHandler.isNetworkUp()) {
                view.setNetworkAvailable(false);
            }
        }

        // when BrowserActivity just starts, onPageStarted may be called before
        // onResume as it is triggered from onCreate. Call resumeWebViewTimers
        // to start the timer. As we won't switch tabs while an activity is in
        // pause state, we can ensure calling resume and pause in pair.
        if (mActivityStopped) {
            resumeWebViewTimers(tab);
        }
        mLoadStopped = false;
        endActionMode();

        mUi.onTabDataChanged(tab);

        String url = tab.getUrl();
        // update the bookmark database for favicon
        syncBookmarkFavicon(tab, null, url, favicon);

        Performance.tracePageStart(url);

        // Performance probe
        if (false) {
            Performance.onPageStarted();
        }

    }

    @Override
    public void onPageFinished(Tab tab) {
        mCrashRecoveryHandler.backupState();
        mUi.onTabDataChanged(tab);

        // Performance probe
        if (false) {
            Performance.onPageFinished(tab.getUrl());
         }

        tab.onPageFinished();
        syncBookmarkFavicon(tab, tab.getOriginalUrl(), tab.getUrl(), tab.getFavicon());

        Performance.tracePageFinished();
    }

    @Override
    public void onProgressChanged(Tab tab) {
        int newProgress = tab.getLoadProgress();

        if (newProgress == 100) {
            CookieSyncManager.getInstance().sync();
            // onProgressChanged() may continue to be called after the main
            // frame has finished loading, as any remaining sub frames continue
            // to load. We'll only get called once though with newProgress as
            // 100 when everything is loaded. (onPageFinished is called once
            // when the main frame completes loading regardless of the state of
            // any sub frames so calls to onProgressChanges may continue after
            // onPageFinished has executed)
            if (tab.inPageLoad()) {
                mWasInPageLoad = true;
                updateInLoadMenuItems(mCachedMenu, tab);
            } else if (mWasInPageLoad) {
                mWasInPageLoad = false;
                updateInLoadMenuItems(mCachedMenu, tab);
            }

            if (mActivityStopped && pauseWebViewTimers(tab)) {
                // pause the WebView timer and release the wake lock if it is
                // finished while BrowserActivity is in pause state.
                releaseWakeLock();
            }
        } else {
            if (!tab.inPageLoad()) {
                // onPageFinished may have already been called but a subframe is
                // still loading
                // updating the progress and
                // update the menu items.
                mWasInPageLoad = false;
                updateInLoadMenuItems(mCachedMenu, tab);
            } else {
                mWasInPageLoad = true;
            }
        }

        mUi.onProgressChanged(tab);
    }

    @Override
    public void onUpdatedSecurityState(Tab tab) {
        mUi.onTabDataChanged(tab);
    }

    @Override
    public void onReceivedTitle(Tab tab, final String title) {
        mUi.onTabDataChanged(tab);
        final String pageUrl = tab.getOriginalUrl();
        if (TextUtils.isEmpty(pageUrl) || pageUrl.length()
                >= SQLiteDatabase.SQLITE_MAX_LIKE_PATTERN_LENGTH) {
            return;
        }
        // Update the title in the history database if not in private browsing mode
        if (!tab.isPrivateBrowsingEnabled()) {
            DataController.getInstance(mActivity).updateHistoryTitle(pageUrl, title);
        }
    }

    @Override
    public void onFavicon(Tab tab, WebView view, Bitmap icon) {
        syncBookmarkFavicon(tab, view.getOriginalUrl(), view.getUrl(), icon);
        ((BaseUi)mUi).setFavicon(tab);
    }

    @Override
    public boolean shouldOverrideUrlLoading(Tab tab, WebView view, String url) {
        // if tab is snapshot tab we want to prevent navigation from occuring
        // since snapshot tab opens a new tab with the url
        return  goLive(url) || mUrlHandler.shouldOverrideUrlLoading(tab, view, url);
    }

    @Override
    public boolean shouldOverrideKeyEvent(KeyEvent event) {
        if (mMenuIsDown) {
            // only check shortcut key when MENU is held
            return mActivity.getWindow().isShortcutKey(event.getKeyCode(),
                    event);
        }
        int keyCode = event.getKeyCode();
        // We need to send almost every key to WebKit. However:
        // 1. We don't want to block the device on the renderer for
        // some keys like menu, home, call.
        // 2. There are no WebKit equivalents for some of these keys
        // (see app/keyboard_codes_win.h)
        // Note that these are not the same set as KeyEvent.isSystemKey:
        // for instance, AKEYCODE_MEDIA_* will be dispatched to webkit.
        if (keyCode == KeyEvent.KEYCODE_MENU ||
            keyCode == KeyEvent.KEYCODE_HOME ||
            keyCode == KeyEvent.KEYCODE_BACK ||
            keyCode == KeyEvent.KEYCODE_CALL ||
            keyCode == KeyEvent.KEYCODE_ENDCALL ||
            keyCode == KeyEvent.KEYCODE_POWER ||
            keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
            keyCode == KeyEvent.KEYCODE_CAMERA ||
            keyCode == KeyEvent.KEYCODE_FOCUS ||
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == KeyEvent.KEYCODE_VOLUME_MUTE ||
            keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return true;
        }

        // We also have to intercept some shortcuts before we send them to the ContentView.
        if (event.isCtrlPressed() && (
                keyCode == KeyEvent.KEYCODE_TAB ||
                keyCode == KeyEvent.KEYCODE_W ||
                keyCode == KeyEvent.KEYCODE_F4)) {
            return true;
        }

        return false;
    }

    private void handleMediaKeyEvent(KeyEvent event) {

        int keyCode = event.getKeyCode();
        // send media key events to audio manager
        if (Build.VERSION.SDK_INT >= 19) {

            switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case KeyEvent.KEYCODE_MEDIA_STOP:
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                case KeyEvent.KEYCODE_MEDIA_REWIND:
                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                case KeyEvent.KEYCODE_MUTE:
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                case KeyEvent.META_SHIFT_RIGHT_ON:
                case KeyEvent.KEYCODE_MEDIA_EJECT:
                case KeyEvent.KEYCODE_MEDIA_RECORD:
                case KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK:

                    AudioManager audioManager = (AudioManager) mActivity.getApplicationContext()
                            .getSystemService(Context.AUDIO_SERVICE);
                    audioManager.dispatchMediaKeyEvent(event);
            }
        }
    }

    @Override
    public boolean onUnhandledKeyEvent(KeyEvent event) {
        if (!isActivityPaused()) {
            handleMediaKeyEvent(event);
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                return mActivity.onKeyDown(event.getKeyCode(), event);
            } else {
                return mActivity.onKeyUp(event.getKeyCode(), event);
            }
        }
        return false;
    }

    @Override
    public void doUpdateVisitedHistory(Tab tab, boolean isReload) {
        // Don't save anything in private browsing mode or when disabling history
        // for regular tabs is enabled
        if (tab.isPrivateBrowsingEnabled() || BrowserConfig.getInstance(getContext())
                        .hasFeature(BrowserConfig.Feature.DISABLE_HISTORY))
            return;

        String url = tab.getOriginalUrl();

        if (TextUtils.isEmpty(url)
                || url.regionMatches(true, 0, "about:", 0, 6)) {
            return;
        }

        DataController.getInstance(mActivity).updateVisitedHistory(url);
        mCrashRecoveryHandler.backupState();
    }

    @Override
    public void getVisitedHistory(final ValueCallback<String[]> callback) {
        AsyncTask<Void, Void, String[]> task =
                new AsyncTask<Void, Void, String[]>() {
            @Override
            public String[] doInBackground(Void... unused) {
                return (String[]) Browser.getVisitedHistory(mActivity.getContentResolver());
            }
            @Override
            public void onPostExecute(String[] result) {
                callback.onReceiveValue(result);
            }
        };
        task.execute();
    }

    @Override
    public void onReceivedHttpAuthRequest(Tab tab, WebView view,
            final HttpAuthHandler handler, final String host,
            final String realm) {
        String username = null;
        String password = null;

        boolean reuseHttpAuthUsernamePassword
                = handler.useHttpAuthUsernamePassword();

        if (reuseHttpAuthUsernamePassword && view != null) {
            String[] credentials = view.getHttpAuthUsernamePassword(host, realm);
            if (credentials != null && credentials.length == 2) {
                username = credentials[0];
                password = credentials[1];
            }
        }

        if (username != null && password != null) {
            handler.proceed(username, password);
        } else {
            if (!tab.inForeground()) {
                handler.cancel();
            }
        }
    }

    @Override
    public void onDownloadStart(Tab tab, String url, String userAgent,
            String contentDisposition, String mimetype, String referer, String auth,
            long contentLength) {
        WebView w = tab.getWebView();
        if ( w == null) return;
        boolean ret = DownloadHandler.onDownloadStart(mActivity, url, userAgent,
                contentDisposition, mimetype, referer, auth,
                w.isPrivateBrowsingEnabled(), contentLength);
        if (ret == false && w.copyBackForwardList().getSize() == 0) {
            // This Tab was opened for the sole purpose of downloading a
            // file. Remove it.
            if (tab == mTabControl.getCurrentTab()) {
                // In this case, the Tab is still on top.
                if (tab.getDerivedFromIntent())
                    closeTab(tab);
                else
                    goBackOnePageOrQuit();
            } else {
                // In this case, it is not.
                closeTab(tab);
            }
        }
    }

    @Override
    public Bitmap getDefaultVideoPoster() {
        return mUi.getDefaultVideoPoster();
    }

    @Override
    public View getVideoLoadingProgressView() {
        return mUi.getVideoLoadingProgressView();
    }

    // helper method

    /*
     * Update the favorites icon if the private browsing isn't enabled and the
     * icon is valid.
     */
    private void syncBookmarkFavicon(Tab tab, final String originalUrl,
                                     final String url, Bitmap favicon) {
        if (favicon == null) {
            return;
        }
        if (!tab.isPrivateBrowsingEnabled()) {
            Bookmarks.updateFavicon(mActivity
                    .getContentResolver(), originalUrl, url, favicon);
        }
    }

    @Override
    public void bookmarkedStatusHasChanged(Tab tab) {
        // TODO: Switch to using onTabDataChanged after b/3262950 is fixed
        mUi.bookmarkedStatusHasChanged(tab);
    }

    // end WebViewController

    protected void pageUp() {
        getCurrentTopWebView().pageUp(false);
    }

    protected void pageDown() {
        getCurrentTopWebView().pageDown(false);
    }

    // callback from phone title bar
    @Override
    public void editUrl() {
        if (mOptionsMenuOpen) mActivity.closeOptionsMenu();
        mUi.editUrl(false, true);
    }

    @Override
    public void showCustomView(Tab tab, View view, int requestedOrientation,
            CustomViewCallback callback) {
        if (tab.inForeground()) {
            if (mUi.isCustomViewShowing()) {
                callback.onCustomViewHidden();
                return;
            }
            mUi.showCustomView(view, requestedOrientation, callback);
            // Save the menu state and set it to empty while the custom
            // view is showing.
            mOldMenuState = mMenuState;
            mMenuState = EMPTY_MENU;
            mActivity.invalidateOptionsMenu();
        }
    }

    @Override
    public void hideCustomView() {
        if (mUi.isCustomViewShowing()) {
            mUi.onHideCustomView();
            // Reset the old menu state.
            mMenuState = mOldMenuState;
            mOldMenuState = EMPTY_MENU;
            mActivity.invalidateOptionsMenu();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
            Intent intent) {
        if (getCurrentTopWebView() == null) return;
        switch (requestCode) {
            case PREFERENCES_PAGE:
                if (resultCode == Activity.RESULT_OK && intent != null) {
                    String action = intent.getStringExtra(Intent.EXTRA_TEXT);
                    if (PreferenceKeys.PREF_PRIVACY_CLEAR_HISTORY.equals(action)) {
                        mTabControl.removeParentChildRelationShips();
                    } else if (action.equals(PreferenceKeys.ACTION_RELOAD_PAGE)) {
                        ArrayList<String> origins =
                                intent.getStringArrayListExtra(EXTRA_UPDATED_URLS);
                        if (origins.isEmpty()) {
                            mTabControl.reloadLiveTabs();
                        }
                        else{
                            for (String origin : origins){
                                mTabControl.findAndReload(origin);
                            }
                        }
                    }
                }
                break;
            case FILE_SELECTED:
                // Chose a file from the file picker.
                if (null == mUploadHandler) break;
                mUploadHandler.onResult(resultCode, intent);
                break;
            case AUTOFILL_SETUP:
                // Determine whether a profile was actually set up or not
                // and if so, send the message back to the WebTextView to
                // fill the form with the new profile.
                if (getSettings().getAutoFillProfile() != null) {
                    mAutoFillSetupMessage.sendToTarget();
                    mAutoFillSetupMessage = null;
                }
                break;
            case COMBO_VIEW:
                if (intent == null || resultCode != Activity.RESULT_OK) {
                    break;
                }
                mUi.showWeb(false);
                if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                    Tab t = getCurrentTab();
                    Uri uri = intent.getData();
                    mUpdateMyNavThumbnail = true;
                    mUpdateMyNavThumbnailUrl = uri.toString();
                    loadUrl(t, uri.toString());
                } else if (intent.hasExtra(ComboViewActivity.EXTRA_OPEN_ALL)) {
                    String[] urls = intent.getStringArrayExtra(
                            ComboViewActivity.EXTRA_OPEN_ALL);
                    Tab parent = getCurrentTab();
                    for (String url : urls) {
                        if (url != null) {
                            parent = openTab(url, parent,
                                    !mSettings.openInBackground(), true);
                        }
                    }
                } else if (intent.hasExtra(ComboViewActivity.EXTRA_OPEN_SNAPSHOT)) {
                    long id = intent.getLongExtra(
                            ComboViewActivity.EXTRA_OPEN_SNAPSHOT, -1);
                    if (id >= 0) {
                        createNewSnapshotTab(id, true);
                    }
                }
                break;
            case VOICE_RESULT:
                if (resultCode == Activity.RESULT_OK && intent != null) {
                    ArrayList<String> results = intent.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS);
                    if (results.size() >= 1) {
                        mVoiceResult = results.get(0);
                    }
                }
                break;
             case MY_NAVIGATION:
                if (intent == null || resultCode != Activity.RESULT_OK) {
                    break;
                }

                if (intent.getBooleanExtra("need_refresh", false) &&
                        getCurrentTopWebView() != null) {
                    getCurrentTopWebView().reload();
                }
                break;
            default:
                break;
        }
        getCurrentTopWebView().requestFocus();
        getCurrentTopWebView().onActivityResult(requestCode, resultCode, intent);
    }

    /**
     * Open the Go page.
     * @param startWithHistory If true, open starting on the history tab.
     *                         Otherwise, start with the bookmarks tab.
     */
    @Override
    public void bookmarksOrHistoryPicker(ComboViews startView) {
        if (mTabControl.getCurrentWebView() == null) {
            return;
        }
        // clear action mode
        if (isInCustomActionMode()) {
            endActionMode();
        }
        Bundle extras = new Bundle();
        // Disable opening in a new window if we have maxed out the windows
        extras.putBoolean(BrowserBookmarksPage.EXTRA_DISABLE_WINDOW,
                !mTabControl.canCreateNewTab());
        mUi.showComboView(startView, extras);
    }

    // combo view callbacks

    // key handling
    protected void onBackKey() {
        if (!mUi.onBackKey()) {
            WebView subwindow = mTabControl.getCurrentSubWindow();
            if (subwindow != null) {
                if (subwindow.canGoBack()) {
                    subwindow.goBack();
                } else {
                    dismissSubWindow(mTabControl.getCurrentTab());
                }
            } else {
                goBackOnePageOrQuit();
            }
        }
    }

    protected boolean onMenuKey() {
        return mUi.onMenuKey();
    }

    // menu handling and state
    // TODO: maybe put into separate handler

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mMenuState == EMPTY_MENU) {
            return false;
        }
        MenuInflater inflater = mActivity.getMenuInflater();
        inflater.inflate(R.menu.browser, menu);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        if (v instanceof TitleBar) {
            return;
        }
        if (!(v instanceof WebView)) {
            return;
        }
        final WebView webview = (WebView) v;
        WebView.HitTestResult result;

        /*  Determine whether the ContextMenu got triggered because
         *  of user action of long click or because of  the UNKNOWN_TYPE_MSG
         *  received. The mResult acts as a flag to identify, how it got trigerred
         */
        if (mResult == null){
            result = webview.getHitTestResult();
        } else {
            result = mResult;
        }

        if (result == null) {
            return;
        }

        int type = result.getType();
        if (type == WebView.HitTestResult.UNKNOWN_TYPE) {

            HashMap<String, Object> unknownTypeMap = new HashMap<String, Object>();
            unknownTypeMap.put("webview", webview);
            final Message msg = mHandler.obtainMessage(
                                    UNKNOWN_TYPE_MSG, unknownTypeMap);
            /* As defined in android developers guide
            *  when UNKNOWN_TYPE is received as a result of HitTest
            *  you need to determing the type by invoking requestFocusNodeHref
            */
            webview.requestFocusNodeHref(msg);

            Log.w(LOGTAG,
                    "We should not show context menu when nothing is touched");
            return;
        }
        if (type == WebView.HitTestResult.EDIT_TEXT_TYPE) {
            // let TextView handles context menu
            return;
        }

        // Note, http://b/issue?id=1106666 is requesting that
        // an inflated menu can be used again. This is not available
        // yet, so inflate each time (yuk!)
        MenuInflater inflater = mActivity.getMenuInflater();
        inflater.inflate(R.menu.browsercontext, menu);

        // Show the correct menu group
        final String extra = result.getExtra();
        final String navigationUrl = MyNavigationUtil.getMyNavigationUrl(extra);
        if (extra == null) return;
        menu.setGroupVisible(R.id.PHONE_MENU,
                type == WebView.HitTestResult.PHONE_TYPE);
        menu.setGroupVisible(R.id.EMAIL_MENU,
                type == WebView.HitTestResult.EMAIL_TYPE);
        menu.setGroupVisible(R.id.GEO_MENU,
                type == WebView.HitTestResult.GEO_TYPE);
        String itemUrl = null;
        String url = webview.getOriginalUrl();
        if (url != null && url.equalsIgnoreCase(MyNavigationUtil.MY_NAVIGATION)) {
            itemUrl = Uri.decode(navigationUrl);
            if (itemUrl != null && !MyNavigationUtil.isDefaultMyNavigation(itemUrl)) {
                menu.setGroupVisible(R.id.MY_NAVIGATION_MENU,
                        type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE);
            } else {
                menu.setGroupVisible(R.id.MY_NAVIGATION_MENU, false);
            }
            menu.setGroupVisible(R.id.IMAGE_MENU, false);
            menu.setGroupVisible(R.id.ANCHOR_MENU, false);
        } else {
            menu.setGroupVisible(R.id.MY_NAVIGATION_MENU, false);

            menu.setGroupVisible(R.id.IMAGE_MENU,
                    type == WebView.HitTestResult.IMAGE_TYPE
                            || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE);
            menu.setGroupVisible(R.id.ANCHOR_MENU,
                    type == WebView.HitTestResult.SRC_ANCHOR_TYPE
                            || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE);

            if (DownloadDirRestriction.getInstance().downloadsAllowed()) {
                menu.findItem(R.id.save_link_context_menu_id).setEnabled(
                        UrlUtils.isDownloadableScheme(extra));
            }
            else {
                menu.findItem(R.id.save_link_context_menu_id).setEnabled(false);
            }
        }
        // Setup custom handling depending on the type
        switch (type) {
            case WebView.HitTestResult.PHONE_TYPE:
                menu.setHeaderTitle(Uri.decode(extra));
                menu.findItem(R.id.dial_context_menu_id).setIntent(
                        new Intent(Intent.ACTION_VIEW, Uri
                                .parse(WebView.SCHEME_TEL + Uri.encode(extra))));
                Intent addIntent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
                addIntent.putExtra(Insert.PHONE, Uri.decode(extra));
                addIntent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
                menu.findItem(R.id.add_contact_context_menu_id).setIntent(
                        addIntent);
                menu.findItem(R.id.copy_phone_context_menu_id)
                        .setOnMenuItemClickListener(
                        new Copy(extra));
                break;

            case WebView.HitTestResult.EMAIL_TYPE:
                menu.setHeaderTitle(extra);
                menu.findItem(R.id.email_context_menu_id).setIntent(
                        new Intent(Intent.ACTION_VIEW, Uri
                                .parse(WebView.SCHEME_MAILTO + extra)));
                menu.findItem(R.id.copy_mail_context_menu_id)
                        .setOnMenuItemClickListener(
                        new Copy(extra));
                break;

            case WebView.HitTestResult.GEO_TYPE:
                menu.setHeaderTitle(extra);
                menu.findItem(R.id.map_context_menu_id).setIntent(
                        new Intent(Intent.ACTION_VIEW, Uri
                                .parse(WebView.SCHEME_GEO
                                        + URLEncoder.encode(extra))));
                menu.findItem(R.id.copy_geo_context_menu_id)
                        .setOnMenuItemClickListener(
                        new Copy(extra));
                break;

            case WebView.HitTestResult.SRC_ANCHOR_TYPE:
            case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE:
                menu.setHeaderTitle(extra);
                // decide whether to show the open link in new tab option
                boolean showNewTab = mTabControl.canCreateNewTab();
                MenuItem newTabItem
                        = menu.findItem(R.id.open_newtab_context_menu_id);
                newTabItem.setTitle(getSettings().openInBackground()
                        ? R.string.contextmenu_openlink_newwindow_background
                        : R.string.contextmenu_openlink_newwindow);
                newTabItem.setVisible(showNewTab);
                if (showNewTab) {
                    if (WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE == type) {
                        newTabItem.setOnMenuItemClickListener(
                                new MenuItem.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        final HashMap<String, WebView> hrefMap =
                                                new HashMap<String, WebView>();
                                        hrefMap.put("webview", webview);
                                        final Message msg = mHandler.obtainMessage(
                                                FOCUS_NODE_HREF,
                                                R.id.open_newtab_context_menu_id,
                                                0, hrefMap);
                                        webview.requestFocusNodeHref(msg);
                                        return true;
                                    }
                                });
                    } else {
                        newTabItem.setOnMenuItemClickListener(
                                new MenuItem.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        final Tab parent = mTabControl.getCurrentTab();
                                        openTab(extra, parent,
                                                !mSettings.openInBackground(),
                                                true);
                                        return true;
                                    }
                                });
                    }
                }
                if (url != null && url.equalsIgnoreCase(MyNavigationUtil.MY_NAVIGATION)) {
                    menu.setHeaderTitle(navigationUrl);
                    menu.findItem(R.id.open_newtab_context_menu_id).setVisible(false);

                    if (itemUrl != null) {
                        if (!MyNavigationUtil.isDefaultMyNavigation(itemUrl)) {
                            menu.findItem(R.id.edit_my_navigation_context_menu_id)
                                    .setOnMenuItemClickListener(new OnMenuItemClickListener() {
                                        @Override
                                        public boolean onMenuItemClick(MenuItem item) {
                                            final Intent intent = new Intent(Controller.this
                                                    .getContext(),
                                                    AddMyNavigationPage.class);
                                            Bundle bundle = new Bundle();
                                            String url = Uri.decode(navigationUrl);
                                            bundle.putBoolean("isAdding", false);
                                            bundle.putString("url", url);
                                            bundle.putString("name", getNameFromUrl(url));
                                            intent.putExtra("websites", bundle);
                                            mActivity.startActivityForResult(intent, MY_NAVIGATION);
                                            return false;
                                        }
                                    });
                            menu.findItem(R.id.delete_my_navigation_context_menu_id)
                                    .setOnMenuItemClickListener(new OnMenuItemClickListener() {
                                        @Override
                                        public boolean onMenuItemClick(MenuItem item) {
                                            showMyNavigationDeleteDialog(Uri.decode(navigationUrl));
                                            return false;
                                        }
                                    });
                        }
                    } else {
                        Log.e(LOGTAG, "mynavigation onCreateContextMenu itemUrl is null!");
                    }
                }
                if (type == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                    break;
                }
                // otherwise fall through to handle image part
            case WebView.HitTestResult.IMAGE_TYPE:
                MenuItem shareItem = menu.findItem(R.id.share_link_context_menu_id);
                shareItem.setVisible(type == WebView.HitTestResult.IMAGE_TYPE);
                if (type == WebView.HitTestResult.IMAGE_TYPE) {
                    menu.setHeaderTitle(extra);
                    shareItem.setOnMenuItemClickListener(
                            new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    sharePage(mActivity, null, extra, null,
                                    null);
                                    return true;
                                }
                            }
                        );
                }
                menu.findItem(R.id.view_image_context_menu_id)
                        .setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        openTab(extra, mTabControl.getCurrentTab(), true, true);
                        return false;
                    }
                });
                menu.findItem(R.id.download_context_menu_id).setOnMenuItemClickListener(
                        new Download(mActivity, extra, webview.isPrivateBrowsingEnabled(),
                                webview.getSettings().getUserAgentString()));
                menu.findItem(R.id.set_wallpaper_context_menu_id).
                        setOnMenuItemClickListener(new WallpaperHandler(mActivity,
                                extra));
                break;

            default:
                Log.w(LOGTAG, "We should not get here.");
                break;
        }
        //update the ui
        mUi.onContextMenuCreated(menu);
    }

    public void startAddMyNavigation(String url) {
        final Intent intent = new Intent(Controller.this.getContext(), AddMyNavigationPage.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean("isAdding", true);
        bundle.putString("url", url);
        bundle.putString("name", getNameFromUrl(url));
        intent.putExtra("websites", bundle);
        mActivity.startActivityForResult(intent, MY_NAVIGATION);
    }

    private void showMyNavigationDeleteDialog(final String itemUrl) {
        new AlertDialog.Builder(this.getContext())
                .setTitle(R.string.my_navigation_delete_label)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.my_navigation_delete_msg)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        deleteMyNavigationItem(itemUrl);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteMyNavigationItem(final String itemUrl) {
        ContentResolver cr = this.getContext().getContentResolver();
        Cursor cursor = null;

        try {
            cursor = cr.query(MyNavigationUtil.MY_NAVIGATION_URI,
                    new String[] {
                        MyNavigationUtil.ID
                    }, "url = ?", new String[] {
                        itemUrl
                    }, null);
            if (null != cursor && cursor.moveToFirst()) {
                Uri uri = ContentUris.withAppendedId(MyNavigationUtil.MY_NAVIGATION_URI,
                        cursor.getLong(0));

                ContentValues values = new ContentValues();
                values.put(MyNavigationUtil.TITLE, "");
                values.put(MyNavigationUtil.URL, "ae://" + cursor.getLong(0) + "add-fav");
                values.put(MyNavigationUtil.WEBSITE, 0 + "");
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                Bitmap bm = BitmapFactory.decodeResource(this.getContext().getResources(),
                        R.raw.my_navigation_add);
                bm.compress(Bitmap.CompressFormat.PNG, 100, os);
                values.put(MyNavigationUtil.THUMBNAIL, os.toByteArray());
                Log.d(LOGTAG, "deleteMyNavigationItem uri is : " + uri);
                cr.update(uri, values, null, null);
            } else {
                Log.e(LOGTAG, "deleteMyNavigationItem the item does not exist!");
            }
        } catch (IllegalStateException e) {
            Log.e(LOGTAG, "deleteMyNavigationItem", e);
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }

        if (getCurrentTopWebView() != null) {
            getCurrentTopWebView().reload();
        }
    }

    private String getNameFromUrl(String itemUrl) {
        ContentResolver cr = this.getContext().getContentResolver();
        Cursor cursor = null;
        String name = null;

        try {
            cursor = cr.query(MyNavigationUtil.MY_NAVIGATION_URI,
                    new String[] {
                        MyNavigationUtil.TITLE
                    }, "url = ?", new String[] {
                        itemUrl
                    }, null);
            if (null != cursor && cursor.moveToFirst()) {
                name = cursor.getString(0);
            } else {
                Log.e(LOGTAG, "this item does not exist!");
            }
        } catch (IllegalStateException e) {
            Log.e(LOGTAG, "getNameFromUrl", e);
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
        return name;
    }

    private void updateMyNavigationThumbnail(final String itemUrl, final Bitmap bitmap) {
        final ContentResolver cr = mActivity.getContentResolver();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {
                boolean isMyNavigationUrl = MyNavigationUtil.isMyNavigationUrl(mActivity, itemUrl);
                if(!isMyNavigationUrl)
                    return null;

                ContentResolver cr = mActivity.getContentResolver();
                Cursor cursor = null;
                try {
                    cursor = cr.query(MyNavigationUtil.MY_NAVIGATION_URI,
                            new String[] {
                                MyNavigationUtil.ID
                            }, "url = ?", new String[] {
                                itemUrl
                            }, null);
                    if (null != cursor && cursor.moveToFirst()) {
                        final ByteArrayOutputStream os = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);

                        ContentValues values = new ContentValues();
                        values.put(MyNavigationUtil.THUMBNAIL, os.toByteArray());
                        Uri uri = ContentUris.withAppendedId(MyNavigationUtil.MY_NAVIGATION_URI,
                                cursor.getLong(0));
                        Log.d(LOGTAG, "updateMyNavigationThumbnail uri is " + uri);
                        cr.update(uri, values, null, null);
                        os.close();
                    }
                } catch (IllegalStateException e) {
                    Log.e(LOGTAG, "updateMyNavigationThumbnail", e);
                } catch (IOException e) {
                    Log.e(LOGTAG, "updateMyNavigationThumbnail", e);
                } finally {
                    if (null != cursor) {
                        cursor.close();
                    }
                }
                return null;
            }
        }.execute();
    }
    /**
     * As the menu can be open when loading state changes
     * we must manually update the state of the stop/reload menu
     * item
     */
    private void updateInLoadMenuItems(Menu menu, Tab tab) {
        if (menu == null) {
            return;
        }
        MenuItem dest = menu.findItem(R.id.stop_reload_menu_id);
        MenuItem src = ((tab != null) && tab.inPageLoad()) ?
                menu.findItem(R.id.stop_menu_id):
                menu.findItem(R.id.reload_menu_id);
        if (src != null) {
            dest.setIcon(src.getIcon());
            dest.setTitle(src.getTitle());
        }
        mActivity.invalidateOptionsMenu();
    }

    public void invalidateOptionsMenu() {
        mAppMenuHandler.invalidateAppMenu();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Software menu key (toolbar key)
        View overflowMenu = mActivity.findViewById(R.id.more_browser_settings);
        if (getCurrentTab() != null && getCurrentTab().isSnapshot()) {
            overflowMenu = mActivity.findViewById(R.id.more);
        }
        mAppMenuHandler.showAppMenu(overflowMenu, false, false);
        return true;
    }

    @Override
    public void prepareMenu(Menu menu) {
        updateInLoadMenuItems(menu, getCurrentTab());
        // hold on to the menu reference here; it is used by the page callbacks
        // to update the menu based on loading state
        mCachedMenu = menu;
        // Note: setVisible will decide whether an item is visible; while
        // setEnabled() will decide whether an item is enabled, which also means
        // whether the matching shortcut key will function.
        switch (mMenuState) {
            case EMPTY_MENU:
                if (mCurrentMenuState != mMenuState) {
                    menu.setGroupVisible(R.id.MAIN_MENU, false);
                    menu.setGroupEnabled(R.id.MAIN_MENU, false);
                    menu.setGroupEnabled(R.id.MAIN_SHORTCUT_MENU, false);
                }
                break;
            default:
                if (mCurrentMenuState != mMenuState) {
                    menu.setGroupVisible(R.id.MAIN_MENU, true);
                    menu.setGroupEnabled(R.id.MAIN_MENU, true);
                    menu.setGroupEnabled(R.id.MAIN_SHORTCUT_MENU, true);
                }
                updateMenuState(getCurrentTab(), menu);
                break;
        }
        mCurrentMenuState = mMenuState;
        mUi.onPrepareOptionsMenu(menu);

        IncognitoRestriction.getInstance()
                .registerControl(menu.findItem(R.id.incognito_menu_id).getIcon());
        EditBookmarksRestriction.getInstance()
                .registerControl(menu.findItem(R.id.bookmark_this_page_id).getIcon());
    }

    private void setMenuItemVisibility(Menu menu, int id,
                                       boolean visibility) {
        MenuItem item = menu.findItem(id);
        if (item != null) {
            item.setVisible(visibility);
        }
    }

    private int lookupBookmark( String url) {
        final ContentResolver cr = getActivity().getContentResolver();
        int count = 0;
        Cursor cursor = null;
        try {
            cursor = cr.query(BrowserContract.Bookmarks.CONTENT_URI,
                    BookmarksLoader.PROJECTION,
                    "url = ?",
                    new String[] {
                        url
                    },
                    null);

            if (cursor != null)
                count = cursor.getCount();

        } catch (IllegalStateException e) {
            Log.e(LOGTAG, "lookupBookmark ", e);
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
        return count;
    }

    private void resetMenuItems(Menu menu) {
        setMenuItemVisibility(menu, R.id.find_menu_id, true);

        WebView w = getCurrentTopWebView();
        MenuItem bookmark_icon = menu.findItem(R.id.bookmark_this_page_id);

        String title = w.getTitle();
        String url = w.getUrl();
        mCurrentPageBookmarked = (lookupBookmark(url) > 0);
        if (title != null && url != null && mCurrentPageBookmarked) {
            bookmark_icon.setChecked(true);
        } else {
            bookmark_icon.setChecked(false);
        }

        // update reader mode checkbox
        MenuItem readerSwitcher = menu.findItem(R.id.reader_mode_menu_id);
        readerSwitcher.setVisible(false);
        readerSwitcher.setChecked(false);
    }

    @Override
    public void updateMenuState(Tab tab, Menu menu) {
        boolean canGoForward = false;
        boolean isDesktopUa = false;
        boolean isLive = false;
        // Following flag is used to identify schemes for which the LIVE_MENU
        // items defined in res/menu/browser.xml should be enabled
        boolean isLiveScheme = false;
        boolean isPageFinished = false;
        boolean isSavable = false;

        boolean isDistillable = false;
        boolean isDistilled = false;
        resetMenuItems(menu);

        if (tab != null) {
            canGoForward = tab.canGoForward();
            isDesktopUa = mSettings.hasDesktopUseragent(tab.getWebView());
            isLive = !tab.isSnapshot();
            isLiveScheme = UrlUtils.isLiveScheme(tab.getWebView().getUrl());
            isPageFinished = (tab.getPageFinishedStatus() || !tab.inPageLoad());
            isSavable = tab.getWebView().isSavable();

            isDistillable = tab.isDistillable();
            isDistilled = tab.isDistilled();
        }

        final MenuItem forward = menu.findItem(R.id.forward_menu_id);
        forward.setEnabled(canGoForward);

        // decide whether to show the share link option
        PackageManager pm = mActivity.getPackageManager();
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        ResolveInfo ri = pm.resolveActivity(send,
                PackageManager.MATCH_DEFAULT_ONLY);
        menu.findItem(R.id.share_page_menu_id).setVisible(ri != null);

        boolean isNavDump = mSettings.enableNavDump();
        final MenuItem nav = menu.findItem(R.id.dump_nav_menu_id);
        nav.setVisible(isNavDump);
        nav.setEnabled(isNavDump);

        boolean showDebugSettings = mSettings.isDebugEnabled();
        final MenuItem uaSwitcher = menu.findItem(R.id.ua_desktop_menu_id);
        uaSwitcher.setChecked(isDesktopUa);
        setMenuItemVisibility(menu, R.id.find_menu_id, isLive);
        menu.setGroupVisible(R.id.NAV_MENU, isLive);
        setMenuItemVisibility(menu, R.id.find_menu_id, isLive && isLiveScheme);
        menu.setGroupVisible(R.id.SNAPSHOT_MENU, !isLive);
        setMenuItemVisibility(menu, R.id.add_to_homescreen,
                isLive && isPageFinished);
        setMenuItemVisibility(menu, R.id.save_snapshot_menu_id,
                isLive && ( isLiveScheme || isDistilled ) && isPageFinished && isSavable);
        // history and snapshots item are the members of COMBO menu group,
        // so if show history item, only make snapshots item invisible.
        menu.findItem(R.id.snapshots_menu_id).setVisible(false);


        // update reader mode checkbox
        final MenuItem readerSwitcher = menu.findItem(R.id.reader_mode_menu_id);
        // The reader mode checkbox is hidden only
        // when the current page is neither distillable nor distilled
        readerSwitcher.setVisible(isDistillable || isDistilled);
        readerSwitcher.setChecked(isDistilled);

        mUi.updateMenuState(tab, menu);
    }

    private Bitmap cropAndScaleBitmap(Bitmap bm, int width, int height) {
        if (width == 0 || height == 0 || bm == null)
            return bm;

        Bitmap cropped;

        if (bm.getHeight() > bm.getWidth()) {
            cropped = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(),
                    bm.getWidth() * height / width, null, true);
        } else {
            cropped = Bitmap.createBitmap(bm, 0, 0, bm.getHeight() * width / height,
                    bm.getHeight(), null, true);
        }

        Bitmap scaled = Bitmap.createScaledBitmap(cropped, width, height, true);
        cropped.recycle();
        return scaled;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (null == getCurrentTopWebView()) {
            return false;
        }
        if (mMenuIsDown) {
            // The shortcut action consumes the MENU. Even if it is still down,
            // it won't trigger the next shortcut action. In the case of the
            // shortcut action triggering a new activity, like Bookmarks, we
            // won't get onKeyUp for MENU. So it is important to reset it here.
            mMenuIsDown = false;
        }
        if (mUi.onOptionsItemSelected(item)) {
            // ui callback handled it
            return true;
        }
        switch (item.getItemId()) {
            // -- Main menu
            case R.id.new_tab_menu_id:
                openTabToHomePage();
                break;

            case R.id.incognito_menu_id:
                openIncognitoTab();
                break;

            case R.id.close_other_tabs_id:
                closeOtherTabs();
                break;

            case R.id.goto_menu_id:
                editUrl();
                break;

            case R.id.bookmarks_menu_id:
                bookmarksOrHistoryPicker(ComboViews.Bookmarks);
                break;

            case R.id.snapshots_menu_id:
                bookmarksOrHistoryPicker(ComboViews.Snapshots);
                break;

            case R.id.bookmark_this_page_id:
                bookmarkCurrentPage();
                break;

            case R.id.stop_reload_menu_id:
                if (isInLoad()) {
                    stopLoading();
                } else {
                    Tab currentTab = mTabControl.getCurrentTab();
                    getCurrentTopWebView().reload();
                }
                break;

            case R.id.forward_menu_id:
                getCurrentTab().goForward();
                break;

            case R.id.close_menu_id:
                // Close the subwindow if it exists.
                if (mTabControl.getCurrentSubWindow() != null) {
                    dismissSubWindow(mTabControl.getCurrentTab());
                    break;
                }
                closeCurrentTab();
                break;

            case R.id.exit_menu_id:
                Object[] params  = { new String("persist.debug.browsermonkeytest")};
                Class[] type = new Class[] {String.class};
                String ret = (String)ReflectHelper.invokeMethod(
                             "android.os.SystemProperties","get", type, params);
                if (ret != null && ret.equals("enable"))
                    break;
                if (BrowserConfig.getInstance(getContext())
                        .hasFeature(BrowserConfig.Feature.EXIT_DIALOG))
                    showExitDialog(mActivity);
                return true;
            case R.id.homepage_menu_id:
                Tab current = mTabControl.getCurrentTab();
                loadUrl(current, mSettings.getHomePage());
                break;

            case R.id.preferences_menu_id:
                openPreferences();
                break;

            case R.id.find_menu_id:
                findOnPage();
                break;

            case R.id.save_snapshot_menu_id:
                final Tab source = getTabControl().getCurrentTab();
                if (source == null) break;
                createScreenshotAsync(
                    source,
                    new ValueCallback<Bitmap>() {
                        @Override
                        public void onReceiveValue(Bitmap bitmap) {
                            Bitmap bm = cropAndScaleBitmap(bitmap,
                                    getDesiredThumbnailWidth(mActivity),
                                    getDesiredThumbnailHeight(mActivity));
                           new SaveSnapshotTask(source, bm).execute();
                        }
                    });
                break;

            case R.id.page_info_menu_id:
                showPageInfo();
                break;

            case R.id.snapshot_go_live:
                // passing null to distinguish between
                // "go live" button and navigating a web page
                //  on a snapshot tab
                return goLive(null);
            case R.id.share_page_menu_id:
                Tab currentTab = mTabControl.getCurrentTab();
                if (null == currentTab) {
                    return false;
                }
                shareCurrentPage(currentTab);
                break;

            case R.id.dump_nav_menu_id:
                getCurrentTopWebView().debugDump();
                break;

            case R.id.zoom_in_menu_id:
                getCurrentTopWebView().zoomIn();
                break;

            case R.id.zoom_out_menu_id:
                getCurrentTopWebView().zoomOut();
                break;

            case R.id.view_downloads_menu_id:
                viewDownloads();
                break;

            case R.id.ua_desktop_menu_id:
                toggleUserAgent();
                break;

            case R.id.reader_mode_menu_id:
                toggleReaderMode();
                break;

            case R.id.window_one_menu_id:
            case R.id.window_two_menu_id:
            case R.id.window_three_menu_id:
            case R.id.window_four_menu_id:
            case R.id.window_five_menu_id:
            case R.id.window_six_menu_id:
            case R.id.window_seven_menu_id:
            case R.id.window_eight_menu_id:
                {
                    int menuid = item.getItemId();
                    for (int id = 0; id < WINDOW_SHORTCUT_ID_ARRAY.length; id++) {
                        if (WINDOW_SHORTCUT_ID_ARRAY[id] == menuid) {
                            Tab desiredTab = mTabControl.getTab(id);
                            if (desiredTab != null &&
                                    desiredTab != mTabControl.getCurrentTab()) {
                                switchToTab(desiredTab);
                            }
                            break;
                        }
                    }
                }
                break;

            case R.id.about_menu_id:
                Bundle bundle = new Bundle();
                bundle.putCharSequence("UA", Engine.getDefaultUserAgent());
                bundle.putCharSequence("TabTitle", mTabControl.getCurrentTab().getTitle());
                bundle.putCharSequence("TabURL", mTabControl.getCurrentTab().getUrl());
                BrowserPreferencesPage.startPreferenceFragmentExtraForResult(mActivity,
                        AboutPreferencesFragment.class.getName(), bundle, 0);
                break;

            case R.id.add_to_homescreen:
                final WebView w = getCurrentTopWebView();
                final EditText input = new EditText(getContext());
                input.setText(w.getTitle());
                new AlertDialog.Builder(getContext())
                        .setTitle(getContext().getResources().getString(
                            R.string.add_to_homescreen))
                        .setMessage(R.string.my_navigation_name)
                        .setView(input)
                        .setPositiveButton(getContext().getResources().getString(
                            R.string.add_bookmark_short), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                mActivity.sendBroadcast(BookmarkUtils.createAddToHomeIntent(
                                        getContext(),
                                        w.getUrl(),
                                        input.getText().toString(),
                                        w.getViewportBitmap(),
                                        w.getFavicon()));

                                mActivity.startActivity(new Intent(Intent.ACTION_MAIN)
                                        .addCategory(Intent.CATEGORY_HOME));
                            }})
                        .setNegativeButton(getContext().getResources().getString(
                            R.string.cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing.
                            }
                        })
                        .show();
                break;

            default:
                return false;
        }
        return true;
    }

    private class SaveSnapshotTask extends AsyncTask<Void, Void, Long>
            implements OnCancelListener {

        private Tab mTab;
        private Dialog mProgressDialog;
        private ContentValues mValues;
        private Bitmap mBitmap;

        private SaveSnapshotTask(Tab tab, Bitmap bm) {
            mTab = tab;
            mBitmap = bm;
        }

        @Override
        protected void onPreExecute() {
            CharSequence message = mActivity.getText(R.string.saving_snapshot);
            mProgressDialog = ProgressDialog.show(mActivity, null, message,
                    true, true, this);
            mValues = mTab.createSnapshotValues(mBitmap);
        }

        @Override
        protected Long doInBackground(Void... params) {
            if (!mTab.saveViewState(mValues)) {
                return null;
            }
            if (isCancelled()) {
                String path = mValues.getAsString(Snapshots.VIEWSTATE_PATH);
                File file = mActivity.getFileStreamPath(path);
                if (!file.delete()) {
                    file.deleteOnExit();
                }
                return null;
            }
            final ContentResolver cr = mActivity.getContentResolver();
            Uri result = cr.insert(Snapshots.CONTENT_URI, mValues);
            if (result == null) {
                return null;
            }
            long id = ContentUris.parseId(result);
            return id;
        }

        @Override
        protected void onPostExecute(Long id) {
            if (isCancelled()) {
                return;
            }
            mProgressDialog.dismiss();
            if (id == null) {
                Toast.makeText(mActivity, R.string.snapshot_failed,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Bundle b = new Bundle();
            b.putLong(BrowserSnapshotPage.EXTRA_ANIMATE_ID, id);
            mUi.showComboView(ComboViews.Snapshots, b);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            cancel(true);
        }
    }

    @Override
    public void toggleUserAgent() {
        WebView web = getCurrentWebView();
        mSettings.toggleDesktopUseragent(web);
    }

    // This function calls the  method in the webview to enable/disable
    // the reader mode through the DOM distiller
    public void toggleReaderMode() {
        Tab t = mTabControl.getCurrentTab();
        if (t.isDistilled()) {
            closeTab(t);
        } else if (t.isDistillable()) {
            openTab(t.getDistilledUrl(), false, true, false, t);
        }
    }

    @Override
    public void findOnPage() {
        getCurrentTopWebView().showFindDialog(null, true);
    }

    @Override
    public void openPreferences() {
        BrowserPreferencesPage.startPreferencesForResult(mActivity, getCurrentTopWebView().getUrl(), PREFERENCES_PAGE);
    }

    @Override
    public void bookmarkCurrentPage() {
        if(EditBookmarksRestriction.getInstance().isEnabled()) {
            Toast.makeText(getContext(), R.string.mdm_managed_alert,
                    Toast.LENGTH_SHORT).show();
        }
        else {
            WebView w = getCurrentTopWebView();
            if (w == null)
                return;
            final Intent i = createBookmarkCurrentPageIntent(mCurrentPageBookmarked);
            mActivity.startActivity(i);
        }
    }

    public boolean goLive(String url) {
        if (!getCurrentTab().isSnapshot())
            return false;
        SnapshotTab t = (SnapshotTab) getCurrentTab();

        if (url == null) { // "go live" button was clicked
            url = t.getLiveUrl();
            closeTab(t);
        }
        Tab liveTab = createNewTab(false, true, false);
        loadUrl(liveTab, url);
        return true;
    }

    private void showExitDialog(final Activity activity) {
        BrowserActivity.killOnExitDialog = false;
        new AlertDialog.Builder(activity)
                .setTitle(R.string.exit_browser_title)
                /* disabled, was worrying people: .setIcon(android.R.drawable.ic_dialog_alert) */
                .setMessage(R.string.exit_browser_msg)
                .setNegativeButton(R.string.exit_minimize, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        activity.moveTaskToBack(true);
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(R.string.exit_quit, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mCrashRecoveryHandler.clearState(true);
                        BrowserActivity.killOnExitDialog = true;
                        activity.finish();
                        dialog.dismiss();
                    }
                })
                .show();
    }

    @Override
    public void showPageInfo() {
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // Let the History and Bookmark fragments handle menus they created.
        if (item.getGroupId() == R.id.CONTEXT_MENU) {
            return false;
        }

        int id = item.getItemId();
        boolean result = true;
        switch (id) {
            // -- Browser context menu
            case R.id.open_context_menu_id:
            case R.id.save_link_context_menu_id:
            case R.id.save_link_bookmark_context_menu_id:
            case R.id.copy_link_context_menu_id:
                final WebView webView = getCurrentTopWebView();
                if (null == webView) {
                    result = false;
                    break;
                }
                final HashMap<String, WebView> hrefMap =
                        new HashMap<String, WebView>();
                hrefMap.put("webview", webView);
                final Message msg = mHandler.obtainMessage(
                        FOCUS_NODE_HREF, id, 0, hrefMap);
                webView.requestFocusNodeHref(msg);
                break;

            default:
                // For other context menus
                result = onOptionsItemSelected(item);
        }
        return result;
    }

    /**
     * support programmatically opening the context menu
     */
    public void openContextMenu(View view) {
        mActivity.openContextMenu(view);
    }

    /**
     * programmatically open the options menu
     */
    public void openOptionsMenu() {
        mActivity.openOptionsMenu();
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (mOptionsMenuOpen) {
            if (mConfigChanged) {
                // We do not need to make any changes to the state of the
                // title bar, since the only thing that happened was a
                // change in orientation
                mConfigChanged = false;
            } else {
                if (!mExtendedMenuOpen) {
                    mExtendedMenuOpen = true;
                    mUi.onExtendedMenuOpened();
                } else {
                    // Switching the menu back to icon view, so show the
                    // title bar once again.
                    mExtendedMenuOpen = false;
                    mUi.onExtendedMenuClosed(isInLoad());
                }
            }
        } else {
            // The options menu is closed, so open it, and show the title
            mOptionsMenuOpen = true;
            mConfigChanged = false;
            mExtendedMenuOpen = false;
            mUi.onOptionsMenuOpened();
        }
        return true;
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        mOptionsMenuOpen = false;
        mUi.onOptionsMenuClosed(isInLoad());
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        mUi.onContextMenuClosed(menu, isInLoad());
    }

    // Helper method for getting the top window.
    @Override
    public WebView getCurrentTopWebView() {
        return mTabControl.getCurrentTopWebView();
    }

    @Override
    public WebView getCurrentWebView() {
        return mTabControl.getCurrentWebView();
    }

    /*
     * This method is called as a result of the user selecting the options
     * menu to see the download window. It shows the download window on top of
     * the current window.
     */
    void viewDownloads() {
        Intent intent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
        mActivity.startActivity(intent);
    }

    int getActionModeHeight() {
        TypedArray actionBarSizeTypedArray = mActivity.obtainStyledAttributes(
                    new int[] { android.R.attr.actionBarSize });
        int size = (int) actionBarSizeTypedArray.getDimension(0, 0f);
        actionBarSizeTypedArray.recycle();
        return size;
    }

    // action mode

    @Override
    public void onActionModeStarted(ActionMode mode) {
        mUi.onActionModeStarted(mode);
        mActionMode = mode;
    }

    /*
     * True if a custom ActionMode (i.e. find or select) is in use.
     */
    @Override
    public boolean isInCustomActionMode() {
        return mActionMode != null;
    }

    /*
     * End the current ActionMode.
     */
    @Override
    public void endActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    /*
     * Called by find and select when they are finished.  Replace title bars
     * as necessary.
     */
    @Override
    public void onActionModeFinished(ActionMode mode) {
        if (!isInCustomActionMode()) return;
        mUi.onActionModeFinished(isInLoad());
        mActionMode = null;
    }

    boolean isInLoad() {
        final Tab tab = getCurrentTab();
        return (tab != null) && tab.inPageLoad();
    }

    // bookmark handling

    /**
     * add the current page as a bookmark to the given folder id
     * @param folderId use -1 for the default folder
     * @param editExisting If true, check to see whether the site is already
     *          bookmarked, and if it is, edit that bookmark.  If false, and
     *          the site is already bookmarked, do not attempt to edit the
     *          existing bookmark.
     */
    @Override
    public Intent createBookmarkCurrentPageIntent(boolean editExisting) {
        WebView w = getCurrentTopWebView();
        if (w == null) {
            return null;
        }
        Intent i = new Intent(mActivity,
                AddBookmarkPage.class);
        i.putExtra(BrowserContract.Bookmarks.URL, w.getUrl());
        i.putExtra(BrowserContract.Bookmarks.TITLE, w.getTitle());
        String touchIconUrl = getCurrentTab().getTouchIconUrl();
        if (touchIconUrl != null) {
            i.putExtra(AddBookmarkPage.TOUCH_ICON_URL, touchIconUrl);
            WebSettings settings = w.getSettings();
            if (settings != null) {
                i.putExtra(AddBookmarkPage.USER_AGENT,
                        settings.getUserAgentString());
            }
        }
        //SWE: Thumbnail will need to be set asynchronously
        i.putExtra(BrowserContract.Bookmarks.FAVICON, w.getFavicon());
        if (editExisting) {
            i.putExtra(AddBookmarkPage.CHECK_FOR_DUPE, true);
        }
        // Put the dialog at the upper right of the screen, covering the
        // star on the title bar.
        i.putExtra("gravity", Gravity.RIGHT | Gravity.TOP);
        return i;
    }

    // file chooser
    @Override
    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
        mUploadHandler = new UploadHandler(this);
        mUploadHandler.openFileChooser(uploadMsg, acceptType, capture);
    }

    @Override
    public void showFileChooser(ValueCallback<String[]> uploadFilePaths, String acceptTypes,
                        boolean capture) {
        mUploadHandler = new UploadHandler(this);
        mUploadHandler.showFileChooser(uploadFilePaths, acceptTypes, capture);
    }

    // thumbnails

    /**
     * Return the desired width for thumbnail screenshots, which are stored in
     * the database, and used on the bookmarks screen.
     * @param context Context for finding out the density of the screen.
     * @return desired width for thumbnail screenshot.
     */
    static int getDesiredThumbnailWidth(Context context) {
        return context.getResources().getDimensionPixelOffset(
                R.dimen.bookmarkThumbnailWidth);
    }

    /**
     * Return the desired height for thumbnail screenshots, which are stored in
     * the database, and used on the bookmarks screen.
     * @param context Context for finding out the density of the screen.
     * @return desired height for thumbnail screenshot.
     */
    static int getDesiredThumbnailHeight(Context context) {
        return context.getResources().getDimensionPixelOffset(
                R.dimen.bookmarkThumbnailHeight);
    }

    void createScreenshotAsync(Tab tab, final ValueCallback<Bitmap> cb) {
        if (tab == null) {
            cb.onReceiveValue(null);
            return;
        }

        tab.capture();

        synchronized (mThumbnailCbList) {
            mThumbnailCbList.add(cb);
        }
 }

    private class Copy implements OnMenuItemClickListener {
        private CharSequence mText;

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            copy(mText);
            return true;
        }

        public Copy(CharSequence toCopy) {
            mText = toCopy;
        }
    }

    private static class Download implements OnMenuItemClickListener {
        private Activity mActivity;
        private String mText;
        private boolean mPrivateBrowsing;
        private String mUserAgent;
        private static final String FALLBACK_EXTENSION = "dat";
        private static final String IMAGE_BASE_FORMAT = "yyyy-MM-dd-HH-mm-ss-";

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            if (DataUri.isDataUri(mText)) {
                saveDataUri();
            } else {
                DownloadHandler.onDownloadStartNoStream(mActivity, mText, mUserAgent,
                        null, null, null, null, mPrivateBrowsing, 0);
            }
            return true;
        }

        public Download(Activity activity, String toDownload, boolean privateBrowsing,
                String userAgent) {
            mActivity = activity;
            mText = toDownload;
            mPrivateBrowsing = privateBrowsing;
            mUserAgent = userAgent;
        }

        /**
         * Treats mText as a data URI and writes its contents to a file
         * based on the current time.
         */
        private void saveDataUri() {
            FileOutputStream outputStream = null;
            try {
                DataUri uri = new DataUri(mText);
                File target = getTarget(uri);
                outputStream = new FileOutputStream(target);
                outputStream.write(uri.getData());
                final DownloadManager manager =
                        (DownloadManager) mActivity.getSystemService(Context.DOWNLOAD_SERVICE);
                 manager.addCompletedDownload(target.getName(),
                        mActivity.getTitle().toString(), false,
                        uri.getMimeType(), target.getAbsolutePath(),
                        uri.getData().length, true);
            } catch (IOException e) {
                Log.e(LOGTAG, "Could not save data URL");
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        // ignore close errors
                    }
                }
            }
        }

        /**
         * Creates a File based on the current time stamp and uses
         * the mime type of the DataUri to get the extension.
         */
        private File getTarget(DataUri uri) throws IOException {
            File dir = mActivity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            DateFormat format = new SimpleDateFormat(IMAGE_BASE_FORMAT, Locale.US);
            String nameBase = format.format(new Date());
            String mimeType = uri.getMimeType();
            MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
            String extension = mimeTypeMap.getExtensionFromMimeType(mimeType);
            if (extension == null) {
                Log.w(LOGTAG, "Unknown mime type in data URI" + mimeType);
                extension = FALLBACK_EXTENSION;
            }
            extension = "." + extension; // createTempFile needs the '.'
            File targetFile = File.createTempFile(nameBase, extension, dir);
            return targetFile;
        }
    }

    private static class SelectText implements OnMenuItemClickListener {
        private WebView mWebView;

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            if (mWebView != null) {
                return mWebView.selectText();
            }
            return false;
        }

        public SelectText(WebView webView) {
           mWebView = webView;
        }

    }

    /********************** TODO: UI stuff *****************************/

    // these methods have been copied, they still need to be cleaned up

    /****************** tabs ***************************************************/

    // basic tab interactions:

    // it is assumed that tabcontrol already knows about the tab
    protected void addTab(Tab tab) {
        mUi.addTab(tab);
    }

    protected void removeTab(Tab tab) {
        mUi.removeTab(tab);
        mTabControl.removeTab(tab);
        mCrashRecoveryHandler.backupState();
    }

    @Override
    public void setActiveTab(Tab tab) {
        // monkey protection against delayed start
        if (tab != null) {

            //Not going to the Nav Screen AnyMore. Unless NavScreen is already showing.
            mUi.cancelNavScreenRequest();
            mTabControl.setCurrentTab(tab);
            // the tab is guaranteed to have a webview after setCurrentTab
            mUi.setActiveTab(tab);


            tab.setTimeStamp();
            //Purge active tabs
            MemoryMonitor.purgeActiveTabs(mActivity.getApplicationContext(), this, mSettings);
        }
    }

    protected void closeEmptyTab() {
        Tab current = mTabControl.getCurrentTab();
        if (current != null
                && current.getWebView().copyBackForwardList().getSize() == 0) {
            closeCurrentTab();
        }
    }

    protected void reuseTab(Tab appTab, UrlData urlData) {
        //Cancel navscreen request
        mUi.cancelNavScreenRequest();
        // Dismiss the subwindow if applicable.
        dismissSubWindow(appTab);
        // Since we might kill the WebView, remove it from the
        // content view first.
        mUi.detachTab(appTab);
        // Recreate the main WebView after destroying the old one.
        mTabControl.recreateWebView(appTab);
        // TODO: analyze why the remove and add are necessary
        mUi.attachTab(appTab);
        if (mTabControl.getCurrentTab() != appTab) {
            switchToTab(appTab);
            loadUrlDataIn(appTab, urlData);
        } else {
            // If the tab was the current tab, we have to attach
            // it to the view system again.
            setActiveTab(appTab);
            loadUrlDataIn(appTab, urlData);
        }
    }

    // Remove the sub window if it exists. Also called by TabControl when the
    // user clicks the 'X' to dismiss a sub window.
    @Override
    public void dismissSubWindow(Tab tab) {
        removeSubWindow(tab);
        // dismiss the subwindow. This will destroy the WebView.
        tab.dismissSubWindow();
        WebView wv = getCurrentTopWebView();
        if (wv != null) {
            wv.requestFocus();
        }
    }

    @Override
    public void removeSubWindow(Tab t) {
        if (t.getSubWebView() != null) {
            mUi.removeSubWindow(t.getSubViewContainer());
        }
    }

    @Override
    public void attachSubWindow(Tab tab) {
        if (tab.getSubWebView() != null) {
            mUi.attachSubWindow(tab.getSubViewContainer());
            getCurrentTopWebView().requestFocus();
        }
    }

    private Tab showPreloadedTab(final UrlData urlData) {
        if (!urlData.isPreloaded()) {
            return null;
        }
        final PreloadedTabControl tabControl = urlData.getPreloadedTab();
        final String sbQuery = urlData.getSearchBoxQueryToSubmit();
        if (sbQuery != null) {
            if (!tabControl.searchBoxSubmit(sbQuery, urlData.mUrl, urlData.mHeaders)) {
                // Could not submit query. Fallback to regular tab creation
                tabControl.destroy();
                return null;
            }
        }
        // check tab count and make room for new tab
        if (!mTabControl.canCreateNewTab()) {
            Tab leastUsed = mTabControl.getLeastUsedTab(getCurrentTab());
            if (leastUsed != null) {
                closeTab(leastUsed);
            }
        }
        Tab t = tabControl.getTab();
        t.refreshIdAfterPreload();
        mTabControl.addPreloadedTab(t);
        addTab(t);
        setActiveTab(t);
        return t;
    }

    // open a non inconito tab with the given url data
    // and set as active tab
    public Tab openTab(UrlData urlData) {
        Tab tab = showPreloadedTab(urlData);
        if (tab == null) {
            tab = createNewTab(false, true, true);
            if ((tab != null) && !urlData.isEmpty()) {
                loadUrlDataIn(tab, urlData);
            }
        }
        return tab;
    }

    @Override
    public Tab openTabToHomePage() {
        return openTab(mSettings.getHomePage(), false, true, false);
    }

    @Override
    public Tab openIncognitoTab() {
        return openTab(INCOGNITO_URI, true, true, false);
    }

    @Override
    public Tab openTab(String url, boolean incognito, boolean setActive,
            boolean useCurrent) {
        return openTab(url, incognito, setActive, useCurrent, null);
    }

    @Override
    public Tab openTab(String url, Tab parent, boolean setActive,
            boolean useCurrent) {
        return openTab(url, (parent != null) && parent.isPrivateBrowsingEnabled(),
                setActive, useCurrent, parent);
    }

    public Tab openTab(String url, boolean incognito, boolean setActive,
            boolean useCurrent, Tab parent) {
        boolean change_tabs = false;
        if (setActive) {
            Tab currentTab = mTabControl.getCurrentTab();
            if (currentTab != null) {
                change_tabs = setActive;
                setActive = false;
                if (mUi instanceof PhoneUi)
                    currentTab.capture();
            }
        }

        Tab tab = createNewTab(incognito, setActive, useCurrent);
        if (tab != null) {
            if (parent instanceof SnapshotTab) {
                addTab(tab);
                if (setActive)
                    setActiveTab(tab);
            }else if (parent != null && parent != tab) {
                parent.addChildTab(tab);
            }
            if (url != null) {
                loadUrl(tab, url);
            }
        }

        if (change_tabs) {
            setActiveTab(tab);
            synchronized (mThumbnailCbList) {
                startCaptureTimer();
                mLatestCreatedTab = tab;
                mThumbnailCbList.add(new ValueCallback<Bitmap>() {
                    @Override
                    public void onReceiveValue(Bitmap bitmap) {
                        synchronized (mThumbnailCbList) {
                            if (mLatestCreatedTab != null) {
                                mLatestCreatedTab = null;
                            }
                            stopCaptureTimer();
                        }
                    }
                });
            }
        }

        return tab;
    }

    private void startCaptureTimer() {
        mCaptureTimer = new CountDownTimer(mCaptureMaxWaitMS, mCaptureMaxWaitMS) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Do nothing
            }

            @Override
            public void onFinish() {
                synchronized (mThumbnailCbList) {
                    Log.e(LOGTAG, "Screen capture timed out while opening new tab");
                    if (mLatestCreatedTab != null) {
                        setActiveTab(mLatestCreatedTab);
                        mLatestCreatedTab = null;
                    }
                }
            }
        }.start();
    }

    private void stopCaptureTimer() {
        if (mCaptureTimer != null) {
            mCaptureTimer.cancel();
            mCaptureTimer = null;
        }
    }


    // this method will attempt to create a new tab
    // incognito: private browsing tab
    // setActive: ste tab as current tab
    // useCurrent: if no new tab can be created, return current tab
    private Tab createNewTab(boolean incognito, boolean setActive,
            boolean useCurrent) {
        Tab tab = null;
        if (IncognitoRestriction.getInstance().isEnabled() && incognito) {
            Toast.makeText(getContext(), R.string.mdm_managed_alert, Toast.LENGTH_SHORT).show();
        } else {
            if (mTabControl.canCreateNewTab()) {
                tab = mTabControl.createNewTab(incognito, !setActive);
                addTab(tab);
                if (setActive) {
                    setActiveTab(tab);
                } else {
                    tab.pause();
                }
            } else {
                if (useCurrent) {
                    tab = mTabControl.getCurrentTab();
                    reuseTab(tab, null);
                } else {
                    mUi.showMaxTabsWarning();
                }
            }
        }
        return tab;
    }

    @Override
    public SnapshotTab createNewSnapshotTab(long snapshotId, boolean setActive) {
        SnapshotTab tab = null;
        if (mTabControl.canCreateNewTab()) {
            tab = mTabControl.createSnapshotTab(snapshotId, null);
            addTab(tab);
            if (setActive) {
                setActiveTab(tab);
            }
        } else {
            mUi.showMaxTabsWarning();
        }
        return tab;
    }

    /**
     * @param tab the tab to switch to
     * @return boolean True if we successfully switched to a different tab.  If
     *                 the indexth tab is null, or if that tab is the same as
     *                 the current one, return false.
     */
    @Override
    public boolean switchToTab(Tab tab) {
        Tab currentTab = mTabControl.getCurrentTab();
        if (tab == null || tab == currentTab) {
            return false;
        }
        setActiveTab(tab);
        return true;
    }

    @Override
    public void closeCurrentTab() {
        closeCurrentTab(false);
    }

    protected void closeCurrentTab(boolean andQuit) {
        if (mTabControl.getTabCount() == 1) {
            mCrashRecoveryHandler.clearState();
            mTabControl.removeTab(getCurrentTab());
            mActivity.finish();
            return;
        }
        final Tab current = mTabControl.getCurrentTab();
        final int pos = mTabControl.getCurrentPosition();
        Tab newTab = current.getParent();
        if (newTab == null) {
            newTab = mTabControl.getTab(pos + 1);
            if (newTab == null) {
                newTab = mTabControl.getTab(pos - 1);
            }
        }
        if (andQuit) {
            mTabControl.setCurrentTab(newTab);
            closeTab(current);
        } else if (switchToTab(newTab)) {
            // Close window
            closeTab(current);
        }
    }

    /**
     * Close the tab, remove its associated title bar, and adjust mTabControl's
     * current tab to a valid value.
     */
    @Override
    public void closeTab(Tab tab) {
        if (tab == mTabControl.getCurrentTab()) {
            closeCurrentTab();
        } else {
            removeTab(tab);
        }
    }

    /**
     * Close all tabs except the current one
     */
    @Override
    public void closeOtherTabs() {
        int inactiveTabs = mTabControl.getTabCount() - 1;
        for (int i = inactiveTabs; i >= 0; i--) {
            Tab tab = mTabControl.getTab(i);
            if (tab != mTabControl.getCurrentTab()) {
                removeTab(tab);
            }
        }
    }

    // Called when loading from context menu or LOAD_URL message
    protected void loadUrlFromContext(String url) {
        Tab tab = getCurrentTab();
        WebView view = tab != null ? tab.getWebView() : null;
        // In case the user enters nothing.
        if (url != null && url.length() != 0 && tab != null && view != null) {
            url = UrlUtils.smartUrlFilter(url);
            if (!((BrowserWebView) view).getWebViewClient().
                    shouldOverrideUrlLoading(view, url)) {
                loadUrl(tab, url);
            }
        }
    }

    /**
     * Load the URL into the given WebView and update the title bar
     * to reflect the new load.  Call this instead of WebView.loadUrl
     * directly.
     * @param view The WebView used to load url.
     * @param url The URL to load.
     */
    @Override
    public void loadUrl(Tab tab, String url) {
        loadUrl(tab, url, null);
    }

    protected void loadUrl(Tab tab, String url, Map<String, String> headers) {
        if (tab != null) {
            dismissSubWindow(tab);
            mHomepageHandler.registerJsInterface(tab.getWebView(), url);
            tab.loadUrl(url, headers);
            mUi.onProgressChanged(tab);
        }
    }

    /**
     * Load UrlData into a Tab and update the title bar to reflect the new
     * load.  Call this instead of UrlData.loadIn directly.
     * @param t The Tab used to load.
     * @param data The UrlData being loaded.
     */
    protected void loadUrlDataIn(Tab t, UrlData data) {
        if (data != null) {
            if (data.isPreloaded()) {
                // this isn't called for preloaded tabs
            } else {
                if (t != null && data.mDisableUrlOverride) {
                    t.disableUrlOverridingForLoad();
                }
                loadUrl(t, data.mUrl, data.mHeaders);
            }
        }
    }

    @Override
    public void onUserCanceledSsl(Tab tab) {
        // TODO: Figure out the "right" behavior
        //In case of tab can go back (aka tab has navigation entry) do nothing
        //else just load homepage in current tab.
        if (!tab.canGoBack()) {
            tab.loadUrl(mSettings.getHomePage(), null);
        }
    }

    void goBackOnePageOrQuit() {
        Tab current = mTabControl.getCurrentTab();
        if (current == null) {
            if (BrowserConfig.getInstance(getContext()).hasFeature(BrowserConfig.Feature.EXIT_DIALOG)) {
                showExitDialog(mActivity);
            } else {
                /*
                 * Instead of finishing the activity, simply push this to the back
                 * of the stack and let ActivityManager to choose the foreground
                 * activity. As BrowserActivity is singleTask, it will be always the
                 * root of the task. So we can use either true or false for
                 * moveTaskToBack().
                 */
                mActivity.moveTaskToBack(true);
            }
            return;
        }
        if (current.canGoBack()) {
            current.goBack();
        } else {
            // Check to see if we are closing a window that was created by
            // another window. If so, we switch back to that window.
            Tab parent = current.getParent();
            if (parent != null) {
                switchToTab(parent);
                // Now we close the other tab
                closeTab(current);
            } else if (BrowserConfig.getInstance(getContext())
                    .hasFeature(BrowserConfig.Feature.EXIT_DIALOG)) {
                showExitDialog(mActivity);
            } else {
                /*
                 * Instead of finishing the activity, simply push this to the back
                 * of the stack and let ActivityManager to choose the foreground
                 * activity. As BrowserActivity is singleTask, it will be always the
                 * root of the task. So we can use either true or false for
                 * moveTaskToBack().
                 */
                mActivity.moveTaskToBack(true);
            }
        }
    }

    /**
     * helper method for key handler
     * returns the current tab if it can't advance
     */
    private Tab getNextTab() {
        int pos = mTabControl.getCurrentPosition() + 1;
        if (pos >= mTabControl.getTabCount()) {
            pos = 0;
        }
        return mTabControl.getTab(pos);
    }

    /**
     * helper method for key handler
     * returns the current tab if it can't advance
     */
    private Tab getPrevTab() {
        int pos  = mTabControl.getCurrentPosition() - 1;
        if ( pos < 0) {
            pos = mTabControl.getTabCount() - 1;
        }
        return  mTabControl.getTab(pos);
    }

    boolean isMenuOrCtrlKey(int keyCode) {
        return (KeyEvent.KEYCODE_MENU == keyCode)
                || (KeyEvent.KEYCODE_CTRL_LEFT == keyCode)
                || (KeyEvent.KEYCODE_CTRL_RIGHT == keyCode);
    }

    /**
     * handle key events in browser
     *
     * @param keyCode
     * @param event
     * @return true if handled, false to pass to super
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && event.getRepeatCount() == 0) {
            // Hardware menu key
            if (!mUi.isComboViewShowing()) {
                mAppMenuHandler.showAppMenu(mActivity.findViewById(R.id.taburlbar),
                        true, false);
            }
            return true;
        }

        boolean noModifiers = event.hasNoModifiers();
        // Even if MENU is already held down, we need to call to super to open
        // the IME on long press.
        if (!noModifiers && isMenuOrCtrlKey(keyCode)) {
            mMenuIsDown = true;
            return false;
        }

        WebView webView = getCurrentTopWebView();
        Tab tab = getCurrentTab();
        if (webView == null || tab == null) return false;

        boolean ctrl = event.hasModifiers(KeyEvent.META_CTRL_ON);
        boolean shift = event.hasModifiers(KeyEvent.META_SHIFT_ON);

        switch(keyCode) {
            case KeyEvent.KEYCODE_TAB:
                if (event.isCtrlPressed()) {
                    if (event.isShiftPressed()) {
                        // prev tab
                        switchToTab(getPrevTab());
                    } else {
                        // next tab
                        switchToTab(getNextTab());
                    }
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_SPACE:
                // WebView/WebTextView handle the keys in the KeyDown. As
                // the Activity's shortcut keys are only handled when WebView
                // doesn't, have to do it in onKeyDown instead of onKeyUp.
                if (shift) {
                    pageUp();
                } else if (noModifiers) {
                    pageDown();
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (!noModifiers) break;
                event.startTracking();
                return true;
            case KeyEvent.KEYCODE_FORWARD:
                if (!noModifiers) break;
                tab.goForward();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (ctrl) {
                    tab.goBack();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (ctrl) {
                    tab.goForward();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_A:
                if (ctrl) {
                    webView.selectAll();
                    return true;
                }
                break;
//          case KeyEvent.KEYCODE_B:    // menu
            case KeyEvent.KEYCODE_C:
                if (ctrl ) {
                    webView.copySelection();
                    return true;
                }
                break;
//          case KeyEvent.KEYCODE_D:    // menu
//          case KeyEvent.KEYCODE_E:    // in Chrome: puts '?' in URL bar
//          case KeyEvent.KEYCODE_F:    // menu
//          case KeyEvent.KEYCODE_G:    // in Chrome: finds next match
//          case KeyEvent.KEYCODE_H:    // menu
//          case KeyEvent.KEYCODE_I:    // unused
//          case KeyEvent.KEYCODE_J:    // menu
//          case KeyEvent.KEYCODE_K:    // in Chrome: puts '?' in URL bar
//          case KeyEvent.KEYCODE_L:    // menu
//          case KeyEvent.KEYCODE_M:    // unused
//          case KeyEvent.KEYCODE_N:    // in Chrome: new window
//          case KeyEvent.KEYCODE_O:    // in Chrome: open file
//          case KeyEvent.KEYCODE_P:    // in Chrome: print page
//          case KeyEvent.KEYCODE_Q:    // unused
//          case KeyEvent.KEYCODE_R:
//          case KeyEvent.KEYCODE_S:    // in Chrome: saves page
            case KeyEvent.KEYCODE_T:
                // we can't use the ctrl/shift flags, they check for
                // exclusive use of a modifier
                if (event.isCtrlPressed()) {
                    if (event.isShiftPressed()) {
                        openIncognitoTab();
                    } else {
                        openTabToHomePage();
                    }
                    return true;
                }
                break;
//          case KeyEvent.KEYCODE_U:    // in Chrome: opens source of page
//          case KeyEvent.KEYCODE_V:    // text view intercepts to paste
//          case KeyEvent.KEYCODE_W:    // menu
//          case KeyEvent.KEYCODE_X:    // text view intercepts to cut
//          case KeyEvent.KEYCODE_Y:    // unused
//          case KeyEvent.KEYCODE_Z:    // unused
        }
        // it is a regular key and webview is not null
         return mUi.dispatchKey(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        switch(keyCode) {
        case KeyEvent.KEYCODE_BACK:
            if (mUi.isWebShowing()) {
                bookmarksOrHistoryPicker(ComboViews.History);
                return true;
            }
            break;
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isMenuOrCtrlKey(keyCode)) {
            mMenuIsDown = false;
            if (KeyEvent.KEYCODE_MENU == keyCode
                    && event.isTracking() && !event.isCanceled()) {
                return onMenuKey();
            }
        }
        if (!event.hasNoModifiers()) return false;
        switch(keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (event.isTracking() && !event.isCanceled()) {
                    onBackKey();
                    return true;
                }
                break;
        }
        return false;
    }

    public boolean isMenuDown() {
        return mMenuIsDown;
    }

    @Override
    public void setupAutoFill(Message message) {
        // Open the settings activity at the AutoFill profile fragment so that
        // the user can create a new profile. When they return, we will dispatch
        // the message so that we can autofill the form using their new profile.
        mAutoFillSetupMessage = message;
        BrowserPreferencesPage.startPreferenceFragmentForResult(mActivity,
                AutoFillSettingsFragment.class.getName(), AUTOFILL_SETUP);
    }

    @Override
    public boolean onSearchRequested() {
        mUi.editUrl(false, true);
        return true;
    }

    @Override
    public boolean shouldCaptureThumbnails() {
        return mUi.shouldCaptureThumbnails();
    }

    @Override
    public void onThumbnailCapture(Bitmap bm) {
        synchronized (mThumbnailCbList) {
            int num_entries = mThumbnailCbList.size();
            while (num_entries > 0){
                num_entries--;
                ValueCallback<Bitmap> cb = mThumbnailCbList.get(num_entries);
                cb.onReceiveValue(bm);
            }
            mThumbnailCbList.clear();
        }
    }

    @Override
    public boolean supportsVoice() {
        PackageManager pm = mActivity.getPackageManager();
        List activities = pm.queryIntentActivities(new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        return activities.size() != 0;
    }

    @Override
    public void startVoiceRecognizer() {
        Intent voice = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        voice.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        voice.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        mActivity.startActivityForResult(voice, VOICE_RESULT);
    }

    public void setWindowDimming(float level) {
        if (mLevel == level)
            return;
        mLevel = level;
        if (level != 0.0f) {
            WindowManager.LayoutParams lp = mActivity.getWindow().getAttributes();
            lp.dimAmount = level;
            mActivity.getWindow().setAttributes(lp);
            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        } else {
            mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
    }

    @Override
    public void setBlockEvents(boolean block) {
        mBlockEvents = block;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return mBlockEvents;
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        return mBlockEvents;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return mBlockEvents;
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent ev) {
        return mBlockEvents;
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        return mBlockEvents;
    }

    @Override
    public boolean shouldShowAppMenu() {
        return true;
    }

    @Override
    public int getMenuThemeResourceId() {
        return R.style.OverflowMenuTheme;
    }
}
