/*
 *  Copyright (c) 2014-2015 The Linux Foundation. All rights reserved.
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

import org.codeaurora.swe.BrowserCommandLine;
import org.codeaurora.swe.Engine;

import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.chromium.base.VisibleForTesting;

public class EngineInitializer {
    private final static String LOGTAG = "EngineInitializer";

    private BrowserActivity mActivity;

    private boolean mNotifyActivity = false;
    private boolean mActivityReady = false;
    private boolean mActivityDestroyed = false;
    private boolean mActivityStartPending = false;
    private boolean mOnResumePending  = false;

    private boolean mFirstDrawCompleted = false;
    private boolean mLibraryLoaded = false;
    private boolean mInitializationCompleted = false;

    private Handler mUiThreadHandler;

    class ActivityResult
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
    private ArrayList<ActivityResult> mPendingActivityResults = null;
    private ArrayList<Intent> mPendingIntents = null;

    private static EngineInitializer sEngineInitializer = null;
    public static  EngineInitializer getInstance() {
        if (sEngineInitializer == null) {
            sEngineInitializer = new EngineInitializer();
        }
        return sEngineInitializer;
    }

    private static long sDelayForTesting = 0;

    //Command line flag for strict mode
    private static final String STRICT_MODE = "enable-strict-mode";

    @VisibleForTesting
    public static void setDelayForTesting(long delay)
    {
        sDelayForTesting = delay;
    }

    private EngineInitializer() {
        mUiThreadHandler = new Handler(Looper.getMainLooper());
    }

    @VisibleForTesting
    public boolean isInitialized()
    {
        return mInitializationCompleted;
    }

    public boolean runningOnUiThread() {
        return mUiThreadHandler.getLooper() == Looper.myLooper();
    }

    public void postOnUiThread(Runnable task) {
        mUiThreadHandler.post(task);
    }

    private class InitializeTask extends AsyncTask<Void, Void, Boolean> {
        public InitializeTask() {
        }
        @Override
        protected Boolean doInBackground(Void... unused) {
            try
            {
                // For testing.
                if (sDelayForTesting > 0) {
                    Thread.sleep(sDelayForTesting);
                }

                Engine.loadNativeLibraries(mActivity.getApplicationContext());

                Engine.warmUpChildProcess(mActivity.getApplicationContext());

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
            mLibraryLoaded = true;
            if (mFirstDrawCompleted) {
                completeInitializationOnUiThread(mActivity.getApplicationContext());
            }
        }
    }
    private InitializeTask mInitializeTask = null;

    public void initializeSync(Context ctx) {
        assert runningOnUiThread() : "Tried to initialize the engine on the wrong thread.";

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
    }

    private void reset(BrowserActivity newActivity) {
        mActivity = newActivity;
        mActivityStartPending = false;
        mOnResumePending  = false;
        mNotifyActivity = true;
        mActivityReady = false;
        mPendingIntents = null;
        mPendingActivityResults = null;
        mFirstDrawCompleted = false;
        mActivityDestroyed = false;
    }

    public void onActivityCreate(BrowserActivity activity) {
        assert runningOnUiThread() : "Tried to initialize the engine on the wrong thread.";
        reset(activity);
        if (!mInitializationCompleted) {
            Engine.initializeCommandLine(mActivity.getApplicationContext());
            mInitializeTask = new InitializeTask();
            mInitializeTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void completeInitialization() {
        postOnUiThread(new Runnable() {
            @Override
            public void run() {
                completeInitializationOnUiThread(mActivity.getApplicationContext());
            }
        });
    }

    private void completeInitializationOnUiThread(Context ctx) {
        assert runningOnUiThread() : "Tried to initialize the engine on the wrong thread.";

        if (!mInitializationCompleted) {
            // TODO: Evaluate the benefit of async Engine.initialize()
            Engine.initialize(ctx);
            // Add the browser commandline options
            BrowserConfig.getInstance(ctx).initCommandLineSwitches();

            //Note: Only enable this for debugging.
            if (BrowserCommandLine.hasSwitch(STRICT_MODE)) {
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

            //Enable remote debugging by default
            Engine.setWebContentsDebuggingEnabled(true);
            mInitializationCompleted = true;
            mLibraryLoaded = true;
            BrowserSettings.getInstance().onEngineInitializationComplete();
        }
        if (mActivity != null && mNotifyActivity) {
            mNotifyActivity = false;
            postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mActivity.onEngineInitializationComplete();
                    mActivityReady = true;
                    processPendingEvents();
                }
            });
        }

    }

    private void completeInitializationAsynOnUiThread(final Context ctx) {
        assert runningOnUiThread() : "Tried to initialize the engine on the wrong thread.";

        if (!mInitializationCompleted) {
            // TODO: Evaluate the benefit of async Engine.initialize()
            Engine.StartupCallback callback =
                new Engine.StartupCallback() {
                    @Override
                    public void onSuccess(boolean alreadyStarted) {
                        if (Looper.myLooper() == Looper.getMainLooper()) {
                            Log.e(LOGTAG, "SWE engine initialization success");
                             // Add the browser commandline options
                            BrowserConfig.getInstance(ctx).initCommandLineSwitches();

                            //Note: Only enable this for debugging.
                            if (BrowserCommandLine.hasSwitch(STRICT_MODE)) {
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

                            //Enable remote debugging by default
                            Engine.setWebContentsDebuggingEnabled(true);
                            mInitializationCompleted = true;
                            mLibraryLoaded = true;
                            BrowserSettings.getInstance().onEngineInitializationComplete();

                            if (mActivity != null && mNotifyActivity) {
                                mNotifyActivity = false;
                                postOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mActivity.onEngineInitializationComplete();
                                        mActivityReady = true;
                                        processPendingEvents();
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onFailure() {
                        Log.e(LOGTAG, "SWE engine initialization failed");
                    }
            };
            Engine.initialize(ctx, callback);
        }
    }

    private void processPendingEvents() {
        assert runningOnUiThread() : "Tried to initialize the engine on the wrong thread.";

        if (mActivityStartPending) {
            mActivityStartPending = false;
            onActivityStart();
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
        if (mOnResumePending && !mActivityDestroyed) {
            onActivityResume();
        }
        mOnResumePending = false;
    }

    public void onPreDraw() {
        mFirstDrawCompleted = true;
        if (mLibraryLoaded) {
            completeInitialization();
        }
    }

    public void initializeResourceExtractor(Context ctx) {
        Engine.startExtractingResources(ctx);
    }

    public void onActivityPause() {
        mOnResumePending = false;
        if (mActivityReady) {
            mActivity.handleOnPause();
        }
    }

    public void onActivityStop() {
        mActivityStartPending = false;
        if (mActivityReady) {
            Engine.pauseTracing(mActivity.getApplicationContext());
            mActivity.handleOnStop();
        }
    }

    public void onActivityResume() {
        if (mActivityReady) {
            mActivity.handleOnResume();
            return;
        }
        mOnResumePending = true;
    }

    public void onActivityStart() {
        if (mActivityReady) {
            Engine.resumeTracing(mActivity.getApplicationContext());
            mActivity.handleOnStart();
            // TODO: We have no reliable mechanism to know when the app goes background.
            //ChildProcessLauncher.onBroughtToForeground();
            return;
        }
        mActivityStartPending = true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mActivityReady) {
            mActivity.handleOnActivityResult(requestCode, resultCode, data);
            return;
        }
        if (mPendingActivityResults == null) {
            mPendingActivityResults = new ArrayList<ActivityResult>(1);
        }
        mPendingActivityResults.add(new ActivityResult(requestCode, resultCode, data));
    }

    public void onNewIntent(Intent intent) {
        if (BrowserActivity.ACTION_RESTART.equals(intent.getAction())) {
            Engine.releaseSpareChildProcess();
        }
        if (mActivityReady) {
            mActivity.handleOnNewIntent(intent);
            return;
        }

        if (mPendingIntents == null) {
            mPendingIntents = new ArrayList<Intent>(1);
        }
        mPendingIntents.add(intent);
    }

    public void onActivityDestroy() {
        Engine.releaseSpareChildProcess();
        mActivityDestroyed = true;
    }


}
