/*
 * Copyright (c) 2015, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution.
 * * Neither the name of The Linux Foundation nor the names of its
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.android.browser.mdm;

import android.os.Bundle;
import android.util.Log;

import org.codeaurora.swe.Engine;

public class DevToolsRestriction extends Restriction {

    private final static String TAG = "DevToolsRestriction";

    public static final String DEV_TOOLS_RESTRICTION = "DevToolsEnabled";

    private static DevToolsRestriction sInstance;

    private DevToolsRestriction() {
        super(TAG);
    }

    public static DevToolsRestriction getInstance() {
        synchronized (DevToolsRestriction.class) {
            if (sInstance == null) {
                sInstance = new DevToolsRestriction();
            }
        }
        return sInstance;
    }

    @Override
    protected void doCustomInit() {
    }

    /*
     *   Note reversed logic:
     *       [x] 'Restrict' true  = DevToolsEnabled : false   => disable DevTools in swe
     *       [ ] 'Restrict' false = DevToolsEnabled : true    => enable DevTools in swe
     */
    @Override
    public void enforce(Bundle restrictions) {

        boolean bEnable = false;
        if (restrictions.containsKey(DEV_TOOLS_RESTRICTION)) {
            bEnable = ! restrictions.getBoolean(DEV_TOOLS_RESTRICTION);
        }
        Log.d(TAG, "Enforce [" + bEnable + "]");
        enable(bEnable);

        Engine.setWebContentsDebuggingEnabled(!isEnabled());
    }
}
