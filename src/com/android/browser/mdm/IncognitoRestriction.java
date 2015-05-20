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
import android.widget.ImageButton;

public class IncognitoRestriction extends Restriction {

    private final static String TAG = "IncognitoRestriction";

    public static final String INCOGNITO_RESTRICTION_ENABLED = "IncognitoRestrictionEnabled"; // boolean

    private static IncognitoRestriction sInstance;
    private ImageButton mIncogButton = null;

    private IncognitoRestriction() {
        super();
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

    public void registerButton (ImageButton ib) {
        mIncogButton = ib;
        updateButton();
    }

    private void updateButton() {
        if (null != mIncogButton) {
            mIncogButton.setAlpha((float) (isEnabled() ? 0.5 : 1.0));
        }
    }

    // For testing
    public float getButtonAlpha() {
        return mIncogButton != null ? mIncogButton.getAlpha() : (float) 1.0;
    }
}
