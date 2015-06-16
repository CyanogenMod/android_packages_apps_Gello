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

import org.codeaurora.swe.util.Activator;
import org.codeaurora.swe.util.Observable;

/**
 * Abstract implementation of a restriction set by a Mobile Device Management (MDM) agent on browser
 * instances running in a managed profile. A subclass must implement the abstract method enforce()
 * to set the restriction.
 */
public abstract class Restriction {

    private boolean mEnabled = false;

    public Restriction(String s) {
        // Register observer for restrictions
        Log.i("+++", "["+ s + "] is registering it's observer");
        doCustomInit();
        Activator.activate(new Observable.Observer() {
            @Override
            public void onChange(Object... params) {
                if (params[0] != null) {
                    enforce((Bundle) params[0]);
                }
            }
        }, ManagedProfileManager.getInstance());
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void enable(boolean enable) {
        mEnabled = enable;
    }

    abstract public void enforce(Bundle restrictions);

    protected void doCustomInit() {}
}

