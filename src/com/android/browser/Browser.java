/*
 * Copyright (C) 2006 The Android Open Source Project
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

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;
import android.util.Log;

import org.codeaurora.swe.Engine;

public class Browser extends Application {

    private final static String LOGTAG = "browser";

    // Set to true to enable verbose logging.
    final static boolean LOGV_ENABLED = false;

    // Set to true to enable extra debug logging.
    final static boolean LOGD_ENABLED = true;

    @Override
    public void onCreate() {
        super.onCreate();

        if (LOGV_ENABLED)
            Log.v(LOGTAG, "Browser.onCreate: this=" + this);

        // Chromium specific initialization.
        Engine.initializeApplicationParameters();

        final boolean isSandboxContext = checkPermission(Manifest.permission.INTERNET,
                Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED;

        if (isSandboxContext) {
            // SWE: Avoid initializing the engine for sandboxed processes.
        } else {
            // Initialize the SWE engine.
            Engine.initialize((Context) this);
            BrowserSettings.initialize((Context) this);
            Preloader.initialize((Context) this);
        }

    }
}

