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

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;

public class IncognitoRestriction extends Restriction {

    private final static String TAG = "IncognitoRestriction";

    public static final String INCOGNITO_RESTRICTION_ENABLED = "IncognitoRestrictionEnabled"; // boolean

    private static IncognitoRestriction sInstance;

    private ArrayList<View> registeredViews;
    private ArrayList<Drawable> registeredDrawables;

    private IncognitoRestriction() {
        super(TAG);
        registeredViews = new ArrayList<>();
        registeredDrawables = new ArrayList<>();
    }

    public static IncognitoRestriction getInstance() {
        synchronized (IncognitoRestriction.class) {
            if (sInstance == null) {
                sInstance = new IncognitoRestriction();
            }
        }
        return sInstance;
    }

    @Override
    public void enforce(Bundle restrictions) {
        enable(restrictions.getBoolean(INCOGNITO_RESTRICTION_ENABLED, false));
        updateButton();
    }

    public void registerControl(View v) {
        if (!registeredViews.contains(v)) {
            registeredViews.add(v);
        }
        updateButton();
    }

    public void registerControl(Drawable d) {
        if (!registeredDrawables.contains(d)) {
            registeredDrawables.add(d);
        }
        updateButton();
    }

    private void updateButton() {
        if (null != registeredViews) {
            for (View v : registeredViews) {
                if (null != v) {
                    v.setAlpha((float) (isEnabled() ? 0.5 : 1.0));
                }
            }
        }
        if (null != registeredDrawables) {
            for (Drawable d : registeredDrawables) {
                if (null != d) {
                    d.setAlpha((isEnabled() ? 0x80 : 0xff));
                }
            }
        }
    }

    // For testing
    public float getButtonAlpha() {
        View v = registeredViews.get(0);
        return v != null ? v.getAlpha() : (float) 1.0;
    }
}
