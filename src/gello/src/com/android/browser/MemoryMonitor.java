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

package com.android.browser;

import android.app.ActivityManager;
import android.content.Context;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MemoryMonitor {

    /**
      * if number of tabs whose native tab is active, is greater
      * than MAX_ACTIVE_TABS destroy the nativetab of oldest used Tab
      */
    public static void purgeActiveTabs(Context context,
                                       Controller controller,
                                       BrowserSettings settings) {
        if(!settings.enableMemoryMonitor())
            return;

        int maxActiveTabs = getMaxActiveTabs(context);
        TabControl tabControl  = controller.getTabControl();

        ArrayList<Tab> activeTabList = new ArrayList<Tab>();

        for (int i = 0; i < tabControl.getTabCount(); i++) {
            Tab tab = tabControl.getTab(i);
            if(tab.isNativeActive())
                activeTabList.add(tab);
        }

        int numActiveTabsToRelease = activeTabList.size() - maxActiveTabs;

        if(numActiveTabsToRelease < 1)
            return;
        // sort tabs in order of LRU first
        Collections.sort(activeTabList, new Comparator<Tab>() {
            @Override
            public int compare(Tab tab1, Tab tab2) {
                return tab1.getTimestamp().compareTo(tab2.getTimestamp());
            }
        });

        for(int i = 0; i < numActiveTabsToRelease; i++) {
            if(tabControl.getCurrentTab()!=  activeTabList.get(i)) {
                activeTabList.get(i).destroyThroughMemoryMonitor();
            }
        }
    }

    /**
      * Returns the default max number of active tabs based on device's
      * memory class.
      */
    private static int getMaxActiveTabs(Context context) {
        return context.getResources()
                .getInteger(R.integer.feature_num_min_active_tabs);
    }
}
