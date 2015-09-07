/*
 *  Copyright (c) 2014 The Linux Foundation. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 *      * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 *  ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 *  BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 *  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 *  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 *  OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 *  IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.android.browser;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.view.ViewTreeObserver;

import com.android.browser.mdm.DevToolsRestriction;

import org.codeaurora.swe.BrowserCommandLine;
import org.codeaurora.swe.Engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.chromium.base.VisibleForTesting;

public class EngineInitializer {

    private final static String LOGTAG = "EngineInitializer";

    private static boolean mInitializationStarted = false;
    private static boolean mSynchronousInitialization = false;
    private static boolean mInitializationCompleted = false;
    private static Handler mUiThreadHandler;

    static class ActivityResult
    {
        public Intent data;
        public int requestCode;
        public int resultCode;

        public ActivityResult(int requestCode, int resultCode, Intent data)
        {
            this.requestCode = requestCode;
            this.resultCode = resultCode;
            this.data = data;
        }
    }

    public static class ActivityScheduler implements ViewTreeObserver.OnPreDrawListener
    {
        private BrowserActivity mActivity = null;
        private ArrayList<ActivityResult> mPendingActivityResults = null;
        private ArrayList<Intent> mPendingIntents = null;

        private boolean mFirstDrawCompleted = false;
        private boolean mOnStartPending = false;
        private boolean mOnPausePending  = false;
        private boolean mEngineInitialized = false;
        private boolean mCanForwardEvents = false;

        public ActivityScheduler(BrowserActivity activity)
        {
            mActivity = activity;
            mFirstDrawCompleted = false;
            mOnStartPending = false;
            mOnPausePending  = false;
            mPendingIntents = null;
            mPendingActivityResults = null;
            mEngineInitialized = false;
            mCanForwardEvents = false;
        }

        @VisibleForTesting
        public boolean firstDrawCompleted() { return mFirstDrawCompleted; }
        @VisibleForTesting
        public boolean onStartPending() { return mOnStartPending; }
        @VisibleForTesting
        public boolean onPausePending() { return mOnPausePending; }
        @VisibleForTesting
        public boolean engineInitialized() { return mEngineInitialized; }
        @VisibleForTesting
        public boolean canForwardEvents() { return mCanForwardEvents; }

        public void processPendingEvents() {
            assert runningOnUiThread() : "Tried to initialize the engine on the wrong thread.";

            if (mOnStartPending) {
                mOnStartPending = false;
                mActivity.handleOnStart();
            }
            if (mOnPausePending) {
                mActivity.handleOnPause();
                mOnPausePending = false;
            }
            if (mPendingIntents != null) {
                for (int i = 0; i < mPendingIntents.size(); i++) {
                    mActivity.handleOnNewIntent(mPendingIntents.get(i));
                }
                mPendingIntents = null;
            }
            if (mPendingActivityResults != null) {
                for (int i = 0; i < mPendingActivityResults.size(); i++) {
                    ActivityResult result = mPendingActivityResults.get(i);
                    mActivity.handleOnActivityResult(result.requestCode, result.resultCode, result.data);
                }
                mPendingActivityResults = null;
            }
            mCanForwardEvents = true;
        }

        public void onActivityCreate(boolean engineInitialized) {
            mEngineInitialized = engineInitialized;
            if (!mEngineInitialized) {
                // Engine initialization is not completed, we should wait for the onPreDraw() notification.
                final ViewTreeObserver observer = mActivity.getWindow().getDecorView().getViewTreeObserver();
                observer.addOnPreDrawListener(this);
            } else {
                mFirstDrawCompleted = true;
                mCanForwardEvents = true;
            }
        }

        @Override
        public boolean onPreDraw() {
            final ViewTreeObserver observer = mActivity.getWindow().getDecorView().getViewTreeObserver();
            observer.removeOnPreDrawListener(this);

            if (mFirstDrawCompleted)
                return true;

            mFirstDrawCompleted = true;
            if (mEngineInitialized) {
                postOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.startController();
                        processPendingEvents();
                    }
                });
            }
            return true;
        }

        public void onEngineInitializationCompletion(boolean synchronous) {
            if (synchronous) {
                // Don't wait for pre-draw notification if it is synchronous
                onPreDraw();
            }
            mEngineInitialized = true;
            if (mFirstDrawCompleted) {
                mActivity.startController();
                processPendingEvents();
            }
        }

        public void onActivityPause() {
            if (mCanForwardEvents) {
                mActivity.handleOnPause();
                return;
            }
            mOnPausePending = true;
        }

        public void onActivityResume() {
            if (mCanForwardEvents) {
                mActivity.handleOnResume();
                return;
            }
            mOnPausePending = false;
        }

        public void onActivityStart() {
            if (mCanForwardEvents) {
                mActivity.handleOnStart();
                // TODO: We have no reliable mechanism to know when the app goes background.
                //ChildProcessLauncher.onBroughtToForeground();
                return;
            }
            mOnStartPending = true;
        }

        public void onActivityStop() {
            if (!mCanForwardEvents) {
                initializeSync(mActivity.getApplicationContext());
            }
            mActivity.handleOnStop();
        }

        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (mCanForwardEvents) {
                mActivity.handleOnActivityResult(requestCode, resultCode, data);
                return;
            }
            if (mPendingActivityResults == null) {
                mPendingActivityResults = new ArrayList<ActivityResult>(1);
            }
            mPendingActivityResults.add(new ActivityResult(requestCode, resultCode, data));
        }

        public void onNewIntent(Intent intent) {
            if (mCanForwardEvents) {
                mActivity.handleOnNewIntent(intent);
                return;
            }

            if (mPendingIntents == null) {
                mPendingIntents = new ArrayList<Intent>(1);
            }
            mPendingIntents.add(intent);
        }
    }

    private static HashMap<BrowserActivity, ActivityScheduler> mActivitySchedulerMap = null;
    private static long sDelayForTesting = 0;

    @VisibleForTesting
    public static void setDelayForTesting(long delay)
    {
        sDelayForTesting = delay;
    }

    @VisibleForTesting
    public static boolean isInitialized()
    {
        return mInitializationCompleted;
    }

    public static boolean runningOnUiThread() {
        return mUiThreadHandler.getLooper() == Looper.myLooper();
    }

    public static void postOnUiThread(Runnable task) {
        mUiThreadHandler.post(task);
    }

    private static class InitializeTask extends AsyncTask<Void, Void, Boolean> {
        private Context mApplicationContext;
        public InitializeTask(Context ctx) {
            mApplicationContext = ctx;
        }
        @Override
        protected Boolean doInBackground(Void... unused) {
            try
            {
                // For testing.
                if (sDelayForTesting > 0) {
                    Thread.sleep(sDelayForTesting);
                }

                Engine.loadNativeLibraries(mApplicationContext);
                if (!BrowserCommandLine.hasSwitch(BrowserSwitches.SINGLE_PROCESS)) {
                    Engine.warmUpChildProcess(mApplicationContext);
                }
                return true;
            }
            catch (Exception e)
            {
                Log.e(LOGTAG, "Unable to load native library.", e);
            }
            return false;
        }

        @Override
        protected void onPostExecute (Boolean result) {
            completeInitializationOnUiThread(mApplicationContext);
        }
    }
    private static InitializeTask mInitializeTask = null;

    public static void initializeSync(Context ctx) {
        assert runningOnUiThread() : "Tried to initialize the engine on the wrong thread.";
        mSynchronousInitialization = true;
        if (mInitializeTask != null) {
            try {
                // Wait for the InitializeTask to finish.
                mInitializeTask.get();
            } catch (CancellationException e1) {
                Log.e(LOGTAG, "Native library load cancelled", e1);
            } catch (ExecutionException e2) {
                Log.e(LOGTAG, "Native library load failed", e2);
            } catch (InterruptedException e3) {
                Log.e(LOGTAG, "Native library load interrupted", e3);
            }
        }
        completeInitializationOnUiThread(ctx);
        mSynchronousInitialization = false;
    }

    private static void initialize(Context ctx) {
        if (!mInitializationCompleted) {
            if (!mInitializationStarted) {
                mInitializationStarted = true;
                mUiThreadHandler = new Handler(Looper.getMainLooper());
                Engine.initializeCommandLine(ctx, CommandLineManager.getCommandLineSwitches(ctx));
                mInitializeTask = new InitializeTask(ctx);
                mInitializeTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                mActivitySchedulerMap = new HashMap<BrowserActivity, ActivityScheduler>();
            } else {
                // This is not the first activity, wait for the engine initialization to finish.
                initializeSync(ctx);
            }
        }
    }

    public static ActivityScheduler onActivityCreate(BrowserActivity activity) {
        assert runningOnUiThread() : "Tried to initialize the engine on the wrong thread.";

        Context ctx = activity.getApplicationContext();
        ActivityScheduler scheduler = new ActivityScheduler(activity);
        initialize(ctx);

        scheduler.onActivityCreate(mInitializationCompleted);
        if (!mInitializationCompleted) {
            mActivitySchedulerMap.put(activity, scheduler);
        }
        return scheduler;
    }

    public static void onPostActivityCreate(BrowserActivity activity) {
        EngineInitializer.initializeResourceExtractor(activity);
        if (EngineInitializer.isInitialized()) {
            activity.startController();
        }
    }

    private static void completeInitializationOnUiThread(Context ctx) {
        assert runningOnUiThread() : "Tried to initialize the engine on the wrong thread.";

        if (!mInitializationCompleted) {

            // TODO: Evaluate the benefit of async Engine.initialize()
            Engine.initialize(ctx, CommandLineManager.getCommandLineSwitches(ctx));
            // Add the browser commandline options
            BrowserConfig.getInstance(ctx).initCommandLineSwitches();

            //Note: Only enable this for debugging.
            if (BrowserCommandLine.hasSwitch(BrowserSwitches.STRICT_MODE)) {
                Log.v(LOGTAG, "StrictMode enabled");
                StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                        .detectDiskReads()
                        .detectDiskWrites()
                        .detectNetwork()
                        .penaltyLog()
                        .build());
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                        .detectLeakedSqlLiteObjects()
                        .detectLeakedClosableObjects()
                        .penaltyLog()
                        .penaltyDeath()
                        .build());
            }

            //Enable remote debugging by default as long as MDM restriction is not enabled
            Engine.setWebContentsDebuggingEnabled(!DevToolsRestriction.getInstance().isEnabled());
            mInitializationCompleted = true;
            mInitializationStarted = true;
            BrowserSettings.getInstance().onEngineInitializationComplete();
            Engine.resumeTracing(ctx);

            if (mActivitySchedulerMap != null) {
                for (Map.Entry<BrowserActivity, ActivityScheduler> entry : mActivitySchedulerMap.entrySet()) {
                    entry.getValue().onEngineInitializationCompletion(mSynchronousInitialization);
                }
                mActivitySchedulerMap.clear();
            }
        }
    }

    public static void initializeResourceExtractor(Context ctx) {
        Engine.startExtractingResources(ctx);
    }

    public static void onPreDraw(BrowserActivity activity) {
        activity.getScheduler().onPreDraw();
    }

    public static void onActivityPause(BrowserActivity activity) {
        activity.getScheduler().onActivityPause();
    }

    public static void onActivityStop(BrowserActivity activity) {
        activity.getScheduler().onActivityStop();
    }

    public static void onActivityResume(BrowserActivity activity) {
        activity.getScheduler().onActivityResume();
    }

    public static void onActivityStart(BrowserActivity activity) {
        activity.getScheduler().onActivityStart();
    }

    public static void onActivityResult(BrowserActivity activity, int requestCode, int resultCode, Intent data) {
        activity.getScheduler().onActivityResult(requestCode, resultCode, data);
    }

    public static void onNewIntent(BrowserActivity activity, Intent intent) {
        activity.getScheduler().onNewIntent(intent);
    }

    public static void onActivityDestroy(BrowserActivity activity) {
    }
}
