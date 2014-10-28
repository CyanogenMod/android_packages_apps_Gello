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

package com.android.browser.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.test.InstrumentationTestCase;
import android.view.KeyEvent;

import com.android.browser.BrowserActivity;
import com.android.browser.BrowserPreferencesPage;
import com.android.browser.EngineInitializer;

public class EngineInitializerTest extends InstrumentationTestCase {

    private Instrumentation mInstrumentation;
    private Context mContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mContext = getInstrumentation().getTargetContext();
    }


    public void test01() throws Throwable {
        Intent localIntent = new Intent();
        localIntent.setClassName("com.android.swe.browser", BrowserActivity.class.getName());
        localIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);

        final Activity activity = mInstrumentation.startActivitySync(localIntent);

        Intent restart = new Intent(BrowserActivity.ACTION_RESTART, null);
        restart.setClassName("com.android.swe.browser", BrowserActivity.class.getName());
        restart.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);

        mInstrumentation.callActivityOnNewIntent(activity, restart);

        Thread.sleep(2000);

        mInstrumentation.waitForIdleSync();

        assertEquals(EngineInitializer.getInstance().isInitialized(), true);

    }


    public void test02() throws Throwable {

        EngineInitializer.setDelayForTesting(1000);

        Intent localIntent = new Intent();
        localIntent.setClassName("com.android.swe.browser", BrowserActivity.class.getName());
        localIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);

        final Activity browserActivity = mInstrumentation.startActivitySync(localIntent);

        Intent pref = new Intent();
        pref.setClassName("com.android.swe.browser", BrowserPreferencesPage.class.getName());
        pref.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);

        final Activity preferencesActivity = mInstrumentation.startActivitySync(pref);

        Thread.sleep(2000);

        mInstrumentation.waitForIdleSync();

        assertEquals(EngineInitializer.getInstance().isInitialized(), true);
    }


    public void test03() throws Throwable {

        EngineInitializer.setDelayForTesting(2000);

        Intent pref = new Intent();
        pref.setClassName("com.android.swe.browser", BrowserPreferencesPage.class.getName());
        pref.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);

        final Activity preferencesActivity = mInstrumentation.startActivitySync(pref);

        assertEquals(EngineInitializer.getInstance().isInitialized(), true);

        Intent localIntent = new Intent();
        localIntent.setClassName("com.android.swe.browser", BrowserActivity.class.getName());
        localIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);

        final Activity browserActivity = mInstrumentation.startActivitySync(localIntent);

        Thread.sleep(3000);

        mInstrumentation.waitForIdleSync();

    }


    public void test04() throws Throwable {

        EngineInitializer.setDelayForTesting(2000);


        Intent localIntent = new Intent();
        localIntent.setClassName("com.android.swe.browser", BrowserActivity.class.getName());
        localIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);

        final Activity browserActivity = mInstrumentation.startActivitySync(localIntent);

        final Intent restart = new Intent(BrowserActivity.ACTION_RESTART, null);
        restart.setClassName("com.android.swe.browser", BrowserActivity.class.getName());
        restart.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);

        runTestOnUiThread(new Runnable () {
            @Override
            public void run() {
                mInstrumentation.callActivityOnNewIntent(browserActivity, restart);
            }
        });

        Intent pref = new Intent();
        pref.setClassName("com.android.swe.browser", BrowserPreferencesPage.class.getName());
        pref.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        final Activity preferencesActivity = mInstrumentation.startActivitySync(pref);

        Thread.sleep(3000);

        mInstrumentation.waitForIdleSync();

        assertEquals(EngineInitializer.getInstance().isInitialized(), true);
    }


    public void test05() throws Throwable {

        EngineInitializer.setDelayForTesting(2000);


        Intent localIntent = new Intent();
        localIntent.setClassName("com.android.swe.browser", BrowserActivity.class.getName());
        localIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);

        final Activity browserActivity = mInstrumentation.startActivitySync(localIntent);

        runTestOnUiThread(new Runnable () {
            @Override
            public void run() {
                Bundle outState = new Bundle();
                mInstrumentation.callActivityOnSaveInstanceState(browserActivity, outState);
                mInstrumentation.callActivityOnDestroy(browserActivity);
            }
        });

        Thread.sleep(3000);

        mInstrumentation.waitForIdleSync();

        assertEquals(EngineInitializer.getInstance().isInitialized(), true);
    }


    public void test06() throws Throwable {

        EngineInitializer.setDelayForTesting(2000);


        Intent localIntent = new Intent();
        localIntent.setClassName("com.android.swe.browser", BrowserActivity.class.getName());
        localIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);

        final Activity browserActivity = mInstrumentation.startActivitySync(localIntent);

        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);

        Thread.sleep(3000);

        mInstrumentation.waitForIdleSync();

        assertEquals(EngineInitializer.getInstance().isInitialized(), true);
    }

}
