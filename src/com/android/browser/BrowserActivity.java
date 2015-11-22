/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (c) 2015, The Linux Foundation. All rights reserved.
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

import android.app.ActionBar;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Process;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import org.chromium.base.VisibleForTesting;
import com.android.browser.R;
import com.android.browser.search.DefaultSearchEngine;
import com.android.browser.search.SearchEngine;
import com.android.browser.stub.NullController;

import java.util.Locale;

import org.codeaurora.net.NetworkServices;
import org.codeaurora.swe.CookieManager;
import org.codeaurora.swe.WebView;

public class BrowserActivity extends Activity {

    public static final String ACTION_SHOW_BOOKMARKS = "show_bookmarks";
    public static final String ACTION_SHOW_BROWSER = "show_browser";
    public static final String ACTION_RESTART = "--restart--";
    private static final String EXTRA_STATE = "state";
    public static final String EXTRA_DISABLE_URL_OVERRIDE = "disable_url_override";

    private final static String LOGTAG = "browser";

    private final static boolean LOGV_ENABLED = Browser.LOGV_ENABLED;

    private ActivityController mController = NullController.INSTANCE;

    private Handler mHandler = new Handler();
    private final Locale mCurrentLocale = Locale.getDefault();
    public static boolean killOnExitDialog = false;


    private UiController mUiController;
    private Handler mHandlerEx = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
           if (mUiController != null) {
               WebView current = mUiController.getCurrentWebView();
               if (current != null) {
                   current.postInvalidate();
               }
           }
        }
    };

    private Bundle mSavedInstanceState;
    private EngineInitializer.ActivityScheduler mActivityScheduler;
    public EngineInitializer.ActivityScheduler getScheduler() {
        return mActivityScheduler;
    }

    @Override
    public void onCreate(Bundle icicle) {
        if (LOGV_ENABLED) {
            Log.v(LOGTAG, this + " onStart, has state: "
                    + (icicle == null ? "false" : "true"));
        }
        super.onCreate(icicle);

        if (shouldIgnoreIntents()) {
            finish();
            return;
        }

        if (!isTablet(this)) {
            final ActionBar bar = getActionBar();
            bar.hide();
        }

        // If this was a web search request, pass it on to the default web
        // search provider and finish this activity.
        /*
        SearchEngine searchEngine = BrowserSettings.getInstance().getSearchEngine();
        boolean result = IntentHandler.handleWebSearchIntent(this, null, getIntent());
        if (result && (searchEngine instanceof DefaultSearchEngine)) {
            finish();
            return;
        }
        */

        mActivityScheduler = EngineInitializer.onActivityCreate(BrowserActivity.this);

        Thread.setDefaultUncaughtExceptionHandler(new CrashLogExceptionHandler(this));

        mSavedInstanceState = icicle;
        // Create the initial UI views
        mController = createController();

        // Workaround for the black screen flicker on SurfaceView creation
        ViewGroup topLayout = (ViewGroup) findViewById(R.id.main_content);
        topLayout.requestTransparentRegion(topLayout);

        EngineInitializer.onPostActivityCreate(BrowserActivity.this);
    }

    public static boolean isTablet(Context context) {
        return context.getResources().getBoolean(R.bool.isTablet);
    }

    private Controller createController() {
        Controller controller = new Controller(this);
        boolean xlarge = isTablet(this);
        UI ui = null;
        if (xlarge) {
            XLargeUi tablet = new XLargeUi(this, controller);
            ui = tablet;
            mUiController = tablet.getUiController();
        } else {
            PhoneUi phone = new PhoneUi(this, controller);
            ui = phone;
            mUiController = phone.getUiController();
        }
        controller.setUi(ui);
        return controller;
    }

    public void startController() {
        Intent intent = (mSavedInstanceState == null) ? getIntent() : null;
        mController.start(intent);
    }

    @VisibleForTesting
    //public to facilitate testing
    public Controller getController() {
        return (Controller) mController;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (getController().getCurrentWebView() != null) {
            if (getController().getCurrentWebView().onRequestPermissionsResult(
                    requestCode, permissions, grantResults)) {
                return;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (shouldIgnoreIntents()) return;
        EngineInitializer.onNewIntent(BrowserActivity.this, intent);
        // Note: Do not add any more application logic in this method.
        //       Move any additional app logic into handleOnNewIntent().
    }

    protected void handleOnNewIntent(Intent intent) {
        if (ACTION_RESTART.equals(intent.getAction())) {
            Bundle outState = new Bundle();
            mController.onSaveInstanceState(outState);
            finish();
            getApplicationContext().startActivity(
                    new Intent(getApplicationContext(), BrowserActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(EXTRA_STATE, outState));
            return;
        }
        mController.handleNewIntent(intent);
    }

    private KeyguardManager mKeyguardManager;
    private PowerManager mPowerManager;
    private boolean shouldIgnoreIntents() {
        // Only process intents if the screen is on and the device is unlocked
        // aka, if we will be user-visible
        if (mKeyguardManager == null) {
            mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        }
        if (mPowerManager == null) {
            mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        }
        boolean ignore = !mPowerManager.isScreenOn();
        ignore |= mKeyguardManager.inKeyguardRestrictedInputMode();
        if (LOGV_ENABLED) {
            Log.v(LOGTAG, "ignore intents: " + ignore);
        }
        return ignore;
    }

    @Override
    protected void onStart() {
        super.onStart();
        EngineInitializer.onActivityStart(BrowserActivity.this);
        if (!BrowserSettings.getInstance().isPowerSaveModeEnabled()) {
            //Notify about anticipated network activity
            NetworkServices.hintUpcomingUserActivity();
        }
    }

    @Override
    protected void onResume() {
        // Checking for Lollipop or above as only those builds will use the v21/styles(material)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                BrowserSettings.getInstance().isPowerSaveModeEnabled()) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }
        super.onResume();
        if (LOGV_ENABLED) {
            Log.v(LOGTAG, "BrowserActivity.onResume: this=" + this);
        }
        EngineInitializer.onActivityResume(BrowserActivity.this);
        // Note: Do not add any more application logic in this method.
        //       Move any additional app logic into handleOnResume().
    }

    protected void handleOnResume() {
        mController.onResume();
    }

    protected void handleOnStart() {
        mController.onStart();
    }

    @Override
    protected void onStop() {
        EngineInitializer.onActivityStop(BrowserActivity.this);
        super.onStop();
        // Note: Do not add any more application logic in this method.
        //       Move any additional app logic into handleOnStop().
    }

    protected void handleOnStop() {
        CookieManager.getInstance().flushCookieStore();
        mController.onStop();
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (Window.FEATURE_OPTIONS_PANEL == featureId) {
            mController.onMenuOpened(featureId, menu);
        }
        return true;
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        mController.onOptionsMenuClosed(menu);
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        super.onContextMenuClosed(menu);
        mController.onContextMenuClosed(menu);
    }

    /**
     *  onSaveInstanceState(Bundle map)
     *  onSaveInstanceState is called right before onStop(). The map contains
     *  the saved state.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (LOGV_ENABLED) {
            Log.v(LOGTAG, "BrowserActivity.onSaveInstanceState: this=" + this);
        }
        mController.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        EngineInitializer.onActivityPause(BrowserActivity.this);
        super.onPause();
        // Note: Do not add any more application logic in this method.
        //       Move any additional app logic into handleOnPause().
    }

    protected void handleOnPause() {
        mController.onPause();
    }

    @Override
    protected void onDestroy() {
        if (LOGV_ENABLED) {
            Log.v(LOGTAG, "BrowserActivity.onDestroy: this=" + this);
        }
        super.onDestroy();
        EngineInitializer.onActivityDestroy(BrowserActivity.this);
        mController.onDestroy();
        mController = NullController.INSTANCE;
        if (!Locale.getDefault().equals(mCurrentLocale) || killOnExitDialog) {
            Log.i(LOGTAG,"Force Killing Browser");
            Process.killProcess(Process.myPid());
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mController.onConfgurationChanged(newConfig);

        //For avoiding bug CR520353 temporarily, delay 300ms to refresh WebView.
        mHandlerEx.postDelayed(runnable, 300);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mController.onLowMemory();
    }

    @Override
    public void invalidateOptionsMenu() {
        super.invalidateOptionsMenu();
        mController.invalidateOptionsMenu();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            mUiController.getUi().hideComboView();
            return true;
        }
        if (!mController.onOptionsItemSelected(item)) {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        mController.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return mController.onContextItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mController.onKeyDown(keyCode, event) ||
            super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return mController.onKeyLongPress(keyCode, event) ||
            super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return mController.onKeyUp(keyCode, event) ||
            super.onKeyUp(keyCode, event);
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);
        mController.onActionModeStarted(mode);
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        super.onActionModeFinished(mode);
        mController.onActionModeFinished(mode);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode,
                                     Intent intent) {
        EngineInitializer.onActivityResult(BrowserActivity.this, requestCode, resultCode, intent);
    }

    protected void handleOnActivityResult (int requestCode, int resultCode, Intent intent) {
        mController.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public boolean onSearchRequested() {
        return mController.onSearchRequested();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return mController.dispatchKeyEvent(event)
                || super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        return mController.dispatchKeyShortcutEvent(event)
                || super.dispatchKeyShortcutEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return mController.dispatchTouchEvent(ev)
                || super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent ev) {
        return mController.dispatchTrackballEvent(ev)
                || super.dispatchTrackballEvent(ev);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        return mController.dispatchGenericMotionEvent(ev) ||
                super.dispatchGenericMotionEvent(ev);
    }

}
