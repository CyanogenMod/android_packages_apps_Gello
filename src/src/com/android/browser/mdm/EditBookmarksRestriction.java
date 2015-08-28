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

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ExpandableListView;

import java.util.ArrayList;

public class EditBookmarksRestriction extends Restriction {

    private final static String TAG = "EditBookmarkRestriction";

    public static final String EDIT_BOOKMARKS_RESTRICTION = "EditBookmarksEnabled";

    private static EditBookmarksRestriction sInstance;

    private ExpandableListView targetListView;
    private ArrayList<Button> registeredButtons;
    private ArrayList<Drawable> registeredDrawables;
    private ColorStateList initialButtonColors;
    private int disabledColor;

    private EditBookmarksRestriction() {
        super(TAG);
    }

    public static EditBookmarksRestriction getInstance() {
        synchronized (EditBookmarksRestriction.class) {
            if (sInstance == null) {
                sInstance = new EditBookmarksRestriction();
            }
        }
        return sInstance;
    }

    @Override
    protected void doCustomInit() {
        targetListView = null;
        registeredButtons = new ArrayList<>();
        registeredDrawables = new ArrayList<>();
        initialButtonColors = null;
        disabledColor = Color.parseColor("grey");
    }

    public void registerView(ExpandableListView f) {
        targetListView = f;
    }

    public void registerControl(Button v) {
        if (initialButtonColors == null) {
            // we assume both buttons will have the same color info
            initialButtonColors = v.getTextColors();
        }
        if (!registeredButtons.contains(v)) {
            registeredButtons.add(v);
        }
        updateUiElems();
    }

    public void registerControl(Drawable d) {
        if (!registeredDrawables.contains(d)) {
            registeredDrawables.add(d);
        }
        updateUiElems();
    }

    private void updateUiElems() {
        if (null != registeredButtons) {
            for (Button v : registeredButtons) {
                if (null != v) {
                    if (isEnabled()) {
                        v.setTextColor(disabledColor);
                    }
                    else {
                        v.setTextColor(initialButtonColors);
                    }
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
        if (targetListView != null) {
            targetListView.invalidateViews();
        }
    }

    /*
     *   Note reversed logic:
     *       [x] 'Restrict' true  = EditBookmarksEnabled : false   => disable editing in swe
     *       [ ] 'Restrict' false = EditBookmarksEnabled : true    => enable editing in swe
     */
    @Override
    public void enforce(Bundle restrictions) {
        boolean bEnable = false;
        if (restrictions.containsKey(EDIT_BOOKMARKS_RESTRICTION)) {
            bEnable = ! restrictions.getBoolean(EDIT_BOOKMARKS_RESTRICTION);
        }
        Log.i(TAG, "Enforce [" + bEnable + "]");
        enable(bEnable);

        updateUiElems();
    }
}
