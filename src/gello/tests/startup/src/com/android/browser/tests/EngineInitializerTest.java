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

package com.android.browser.tests;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.android.browser.BrowserActivity;
import com.android.browser.BrowserPreferencesPage;
import com.android.browser.EngineInitializer;

public class EngineInitializerTest extends InstrumentationTestCase {

    private static final String gTestTargetPackageName = "org.codeaurora.swe.browser.beta";
    private static final int gEngineInitDelay = 2000;
    private Instrumentation mInstrumentation;
    private Context mContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mContext = getInstrumentation().getTargetContext();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @LargeTest
    public void test01() throws Throwable {

        Intent localIntent = new Intent();
        localIntent.setClassName(gTestTargetPackageName, BrowserActivity.class.getName());
        localIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);


        final Intent restart = new Intent(BrowserActivity.ACTION_RESTART, null);
        restart.setClassName(gTestTargetPackageName, BrowserActivity.class.getName());
        restart.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        EngineInitializer.setDelayForTesting(gEngineInitDelay);
        final BrowserActivity activity = (BrowserActivity) mInstrumentation.startActivitySync(localIntent);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertEquals(EngineInitializer.isInitialized(), false);
                assertEquals(activity.getScheduler().canForwardEvents(), false);
                assertEquals(activity.getScheduler().onStartPending(), true);
                assertEquals(activity.getScheduler().engineInitialized(), false);
                assertEquals(activity.getScheduler().onPausePending(), false);

                mInstrumentation.callActivityOnPause(activity);
                assertEquals(activity.getScheduler().onPausePending(), true);

                mInstrumentation.callActivityOnStop(activity);
                assertEquals(EngineInitializer.isInitialized(), true);
                assertEquals(activity.getScheduler().engineInitialized(), true);
                assertEquals(activity.getScheduler().onStartPending(), false);
                assertEquals(activity.getScheduler().onPausePending(), false);
                assertEquals(activity.getScheduler().canForwardEvents(), true);

                mInstrumentation.callActivityOnNewIntent(activity, restart);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    @LargeTest
    public void test02() throws Throwable {

        EngineInitializer.setDelayForTesting(gEngineInitDelay);

        Intent localIntent = new Intent();
        localIntent.setClassName(gTestTargetPackageName, BrowserActivity.class.getName());
        localIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        final BrowserActivity activity = (BrowserActivity)mInstrumentation.startActivitySync(localIntent);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertEquals(EngineInitializer.isInitialized(), false);
                assertEquals(activity.getScheduler().onStartPending(), true);
                assertEquals(activity.getScheduler().onPausePending(), false);
                assertEquals(activity.getScheduler().engineInitialized(), false);
                assertEquals(activity.getScheduler().canForwardEvents(), false);

                mInstrumentation.callActivityOnPause(activity);
                assertEquals(activity.getScheduler().onPausePending(), true);

                mInstrumentation.callActivityOnResume(activity);
                assertEquals(activity.getScheduler().onPausePending(), false);

                mInstrumentation.callActivityOnPause(activity);
                assertEquals(activity.getScheduler().onStartPending(), true);
                assertEquals(activity.getScheduler().onPausePending(), true);
                assertEquals(activity.getScheduler().canForwardEvents(), false);
            }
        });

        Intent pref = new Intent();
        pref.setClassName(gTestTargetPackageName, BrowserPreferencesPage.class.getName());
        pref.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        final Activity preferencesActivity = mInstrumentation.startActivitySync(pref);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertEquals(EngineInitializer.isInitialized(), true);
                assertEquals(activity.getScheduler().onStartPending(), false);
                assertEquals(activity.getScheduler().onPausePending(), false);
                assertEquals(activity.getScheduler().engineInitialized(), true);
                assertEquals(activity.getScheduler().canForwardEvents(), true);
            }
        });

        mInstrumentation.waitForIdleSync();
    }

    @LargeTest
    public void test03() throws Throwable {

        EngineInitializer.setDelayForTesting(gEngineInitDelay);

        Intent pref = new Intent();
        pref.setClassName(gTestTargetPackageName, BrowserPreferencesPage.class.getName());
        pref.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        assertEquals(EngineInitializer.isInitialized(), false);
        final Activity preferencesActivity = mInstrumentation.startActivitySync(pref);
        assertEquals(EngineInitializer.isInitialized(), true);

        Intent localIntent = new Intent();
        localIntent.setClassName(gTestTargetPackageName, BrowserActivity.class.getName());
        localIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        final BrowserActivity activity = (BrowserActivity)mInstrumentation.startActivitySync(localIntent);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertEquals(EngineInitializer.isInitialized(), true);
                assertEquals(activity.getScheduler().onStartPending(), false);
                assertEquals(activity.getScheduler().onPausePending(), false);
                assertEquals(activity.getScheduler().engineInitialized(), true);
                assertEquals(activity.getScheduler().canForwardEvents(), true);
            }
        });

        mInstrumentation.waitForIdleSync();
    }

    @LargeTest
    public void test04() throws Throwable {

        EngineInitializer.setDelayForTesting(gEngineInitDelay);

        Intent localIntent = new Intent();
        localIntent.setClassName(gTestTargetPackageName, BrowserActivity.class.getName());
        localIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        assertEquals(EngineInitializer.isInitialized(), false);
        final BrowserActivity activity = (BrowserActivity)mInstrumentation.startActivitySync(localIntent);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertEquals(EngineInitializer.isInitialized(), false);
                assertEquals(activity.getScheduler().onStartPending(), true);
                assertEquals(activity.getScheduler().onPausePending(), false);
                assertEquals(activity.getScheduler().engineInitialized(), false);
                assertEquals(activity.getScheduler().canForwardEvents(), false);
            }
        });

        final Intent restart = new Intent(BrowserActivity.ACTION_RESTART, null);
        restart.setClassName(gTestTargetPackageName, BrowserActivity.class.getName());
        restart.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mInstrumentation.callActivityOnNewIntent(activity, restart);
            }
        });

        Intent pref = new Intent();
        pref.setClassName(gTestTargetPackageName, BrowserPreferencesPage.class.getName());
        pref.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        final Activity preferencesActivity = mInstrumentation.startActivitySync(pref);
        assertEquals(EngineInitializer.isInitialized(), true);

        mInstrumentation.waitForIdleSync();
    }


    @LargeTest
    public void test05() throws Throwable {

        EngineInitializer.setDelayForTesting(gEngineInitDelay);

        Intent localIntent = new Intent();
        localIntent.setClassName(gTestTargetPackageName, BrowserActivity.class.getName());
        localIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        final BrowserActivity activity = (BrowserActivity)mInstrumentation.startActivitySync(localIntent);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                Bundle outState = new Bundle();
                mInstrumentation.callActivityOnPause(activity);
                assertEquals(activity.getScheduler().onStartPending(), true);
                assertEquals(activity.getScheduler().onPausePending(), true);
                mInstrumentation.callActivityOnSaveInstanceState(activity, outState);
                assertEquals(activity.getScheduler().engineInitialized(), false);
                assertEquals(activity.getScheduler().canForwardEvents(), false);
                mInstrumentation.callActivityOnStop(activity);
                assertEquals(EngineInitializer.isInitialized(), true);
                assertEquals(activity.getScheduler().engineInitialized(), true);
                mInstrumentation.callActivityOnRestart(activity);
            }
        });

        mInstrumentation.waitForIdleSync();
    }

    @LargeTest
    public void test06() throws Throwable {

        EngineInitializer.setDelayForTesting(gEngineInitDelay);

        Intent localIntent = new Intent();
        localIntent.setClassName(gTestTargetPackageName, BrowserActivity.class.getName());
        localIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        final BrowserActivity activity = (BrowserActivity)mInstrumentation.startActivitySync(localIntent);
        assertEquals(EngineInitializer.isInitialized(), false);

        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        long now = SystemClock.uptimeMillis();
        mInstrumentation.sendTrackballEventSync(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 100, 100, 0));
        mInstrumentation.sendTrackballEventSync(MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, 100, 100, 0));
        now = SystemClock.uptimeMillis();
        mInstrumentation.sendPointerSync(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 100, 100, 0));
        mInstrumentation.sendPointerSync(MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, 100, 100, 0));

        mInstrumentation.callActivityOnUserLeaving(activity);

        mInstrumentation.waitForIdleSync();

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                Bundle outState = new Bundle();
                mInstrumentation.callActivityOnPause(activity);
                assertEquals(activity.getScheduler().onStartPending(), true);
                assertEquals(activity.getScheduler().onPausePending(), true);
                mInstrumentation.callActivityOnSaveInstanceState(activity, outState);
                assertEquals(activity.getScheduler().engineInitialized(), false);
                assertEquals(activity.getScheduler().canForwardEvents(), false);
                mInstrumentation.callActivityOnStop(activity);
                assertEquals(EngineInitializer.isInitialized(), true);
                mInstrumentation.callActivityOnRestart(activity);
            }
        });

        mInstrumentation.waitForIdleSync();
    }
}
