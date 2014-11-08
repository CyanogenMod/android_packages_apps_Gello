// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.android.browser.appmenu;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * A helper class for a menu button to decide when to show the app menu and forward touch
 * events.
 *
 * Simply construct this class and pass the class instance to a menu button as TouchListener.
 * Then this class will handle everything regarding showing app menu for you.
 */
public class AppMenuButtonHelper implements OnTouchListener {
    private final View mMenuButton;
    private final AppMenuHandler mMenuHandler;
    private Runnable mOnAppMenuShownListener;

    /**
     * @param menuButton  Menu button instance that will trigger the app menu.
     * @param menuHandler MenuHandler implementation that can show and get the app menu.
     */
    public AppMenuButtonHelper(View menuButton, AppMenuHandler menuHandler) {
        mMenuButton = menuButton;
        mMenuHandler = menuHandler;
    }

    /**
     * @param onAppMenuShownListener This is called when the app menu is shown by this class.
     */
    public void setOnAppMenuShownListener(Runnable onAppMenuShownListener) {
        mOnAppMenuShownListener = onAppMenuShownListener;
    }

    /**
     * Shows the app menu if it is not already shown.
     * @param startDragging Whether dragging is started.
     * @return Whether or not if the app menu is successfully shown.
     */
    private boolean showAppMenu(boolean startDragging) {
        if (!mMenuHandler.isAppMenuShowing() &&
                mMenuHandler.showAppMenu(mMenuButton, false, startDragging)) {

            if (mOnAppMenuShownListener != null) {
                mOnAppMenuShownListener.run();
            }
            return true;
        }
        return false;
    }

    /**
     * @return Whether app menu is active. That is, AppMenu is showing or menu button is consuming
     *         touch events to prepare AppMenu showing.
     */
    public boolean isAppMenuActive() {
        return mMenuButton.isPressed() || mMenuHandler.isAppMenuShowing();
    }

    /**
     * Handle the key press event on a menu button.
     * @return Whether the app menu was shown as a result of this action.
     */
    public boolean onEnterKeyPress() {
        return showAppMenu(false);
    }

    @Override

    public boolean onTouch(View view, MotionEvent event) {
        boolean isTouchEventConsumed = false;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                isTouchEventConsumed |= true;
                mMenuButton.setPressed(true);
                showAppMenu(true);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isTouchEventConsumed |= true;
                mMenuButton.setPressed(false);
                break;
            default:
        }

        return isTouchEventConsumed;
    }
}