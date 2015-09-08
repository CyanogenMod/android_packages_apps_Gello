// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.android.browser.appmenu;

import android.view.Menu;

/**
 * Interface for the App Handler to query the desired state of the App Menu.
 */
public interface AppMenuPropertiesDelegate {

    /**
     * @return Whether the App Menu should be shown.
     */
    boolean shouldShowAppMenu();

    /**
     * Allows the delegate to show and hide items before the App Menu is shown.
     * @param mMenu Menu that will be used as the source for the App Menu pop up.
     */
    void prepareMenu(Menu mMenu);

    /**
     * @return The theme resource to use for displaying the App Menu.
     */
    int getMenuThemeResourceId();
}
