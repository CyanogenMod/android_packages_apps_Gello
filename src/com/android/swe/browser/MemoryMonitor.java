/*
    * Copyright (c) 2013, The Linux Foundation. All rights reserved.
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

package com.android.swe.browser;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;
import java.sql.Timestamp;

public class MemoryMonitor {

    //This number is used with device memory class to calculate max number
    //of active tabs.
    private static int sMaxActiveTabs = 0;
    private static MemoryMonitor sMemoryMonitor;
    private TabControl mTabControl;
    private final static String LOGTAG = "MemoryMonitor";

    // Should be called only once

    public static MemoryMonitor getInstance(Context context,
                                            Controller controller) {
        if (sMemoryMonitor == null) {
            sMemoryMonitor = new MemoryMonitor(context,controller);
        }
        return sMemoryMonitor;
    }

    MemoryMonitor(Context context,Controller controller) {
        mTabControl = controller.getTabControl();
        sMaxActiveTabs = getMaxActiveTabs(context);
        Log.d(LOGTAG,"Max Active Tabs: "+ sMaxActiveTabs);
    }

    private int getActiveTabs() {
        int numNativeActiveTab = 0;
        int size = mTabControl.getTabCount();

        for (int i = 0; i < size; i++) {
           Tab tab =  mTabControl.getTab(i);
           if (((Tab)tab).isNativeActive()){
                numNativeActiveTab++;
           }
        }
        return numNativeActiveTab;
    }

    /**
      * if number of tabs whose native tab is active, is greater
      * than MAX_ACTIVE_TABS destroy the nativetab of oldest used Tab
      */

    public void destroyLeastRecentlyActiveTab() {
        int numActiveTabs = getActiveTabs();
        int numActiveTabsToRelease = numActiveTabs - sMaxActiveTabs;

        // The most common case will be that we need to delete one
        // NativeTab to make room for a new one.  So, find the most-stale.
        if (numActiveTabsToRelease == 1) {
            Tab mostStaleTab = null;
            for (Tab t : mTabControl.getTabs()) {
                if (t.isNativeActive() && !(t.inForeground())) {
                    if (mostStaleTab == null){
                        mostStaleTab = t;
                    }
                    else {
                        if (t.getTimestamp().compareTo(mostStaleTab.
                            getTimestamp()) < 0) {
                            mostStaleTab = t;
                        }
                    }
                }
            }
            if (mostStaleTab != null) {
                mostStaleTab.destroy();
           }
        } else if (numActiveTabsToRelease > 1) {
            // Since there is more than 1 "extra" tab, just release all
            // NativeTabs in the background. This would be true when
            // tracking was turned on after multiple tabs already exists
            for (Tab t : mTabControl.getTabs()) {
                if (t.isNativeActive() && !(t.inForeground())) {
                    t.destroy();
                }
            }
        }
    }

    /**
      * Returns the default max number of active tabs based on device's
      * memory class.
      */
    static int getMaxActiveTabs(Context context) {
        // We use device memory class to decide number of active tabs
        // (minimum memory class is 16).
        ActivityManager am =(ActivityManager)context.
            getSystemService(Context.ACTIVITY_SERVICE);
        if (am.getMemoryClass() < 33) {
            return 1;   // only 1 Tab can be active at a time
        }
        else {
            return 2;  // atleast 2 Tabs can be active at a time
        }
    }
}
