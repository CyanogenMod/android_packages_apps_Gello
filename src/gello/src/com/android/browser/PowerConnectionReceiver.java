/* Copyright (c) 2015, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *      * Neither the name of The Linux Foundation nor the names of its
 *      contributors may be used to endorse or promote products derived
 *      from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.browser;

import com.android.browser.preferences.GeneralPreferencesFragment;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;

public class PowerConnectionReceiver extends BroadcastReceiver {
    static final String POWER_MODE_TOGGLE = "android.os.action.POWER_SAVE_MODE_CHANGED";
    static final String POWER_OKAY = Intent.ACTION_BATTERY_OKAY;

    @Override
    public void onReceive(Context context, Intent intent) {
        BrowserSettings settings = BrowserSettings.getInstance();
        String action = intent.getAction();
        if (POWER_MODE_TOGGLE.equals(action)) {
            // This feature is only on android L+
            PowerManager pm = (PowerManager) context.getSystemService(context.POWER_SERVICE);
            settings.setPowerSaveModeEnabled(pm.isPowerSaveMode());
        } else if (POWER_OKAY.equals(action)) {
            settings.setPowerSaveModeEnabled(false);
        } else {
            if (settings.isPowerSaveModeEnabled())
                return;
            Bundle bundle = new Bundle();
            bundle.putBoolean("LowPower", true);
            BrowserPreferencesPage.startPreferenceFragmentExtraForResult((Activity) context, GeneralPreferencesFragment.class.getName(), bundle, 0);
        }
    }
}
