/*
 * Copyright (c) 2015, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.android.browser.preferences;

import android.app.ActionBar;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;

import com.android.browser.R;

public abstract class SWEPreferenceFragment extends PreferenceFragment  {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        View view = super.onCreateView(inflater, container, bundle);

        ListView list = (ListView) view.findViewById(android.R.id.list);

        if (list == null) {
            return view;
        }

        ViewGroup.LayoutParams params = list.getLayoutParams();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        list.setLayoutParams(params);
        list.setPadding(0, list.getPaddingTop(), 0, list.getPaddingBottom());
        list.setDivider(null);
        list.setDividerHeight(0);

        list.setOnHierarchyChangeListener(
                new ViewGroup.OnHierarchyChangeListener() {
                    @Override
                    public void onChildViewAdded(View parent, View child) {
                        onChildViewAddedToHierarchy(parent, child);
                        findAndResizeSwitchPreferenceWidget(child);
                    }

                    @Override
                    public void onChildViewRemoved(View parent, View child) {
                        onChildViewRemovedFromHierarchy(parent, child);
                    }
                }
        );

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        /*ActionBar bar = getActivity().getActionBar();
        if (bar != null) {
            bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.accent)));
        }*/
    }

    private final void findAndResizeSwitchPreferenceWidget(View parent) {
        LinearLayout layout = (LinearLayout) parent.findViewById(android.R.id.widget_frame);
        if (layout != null) {
            for (int i = 0; i < layout.getChildCount(); i++) {
                View view = layout.getChildAt(i);
                if (view instanceof Switch) {
                    Switch switchView = (Switch) view;
                    switchView.setThumbTextPadding(0);
                    int width = switchView.getSwitchMinWidth();
                    switchView.setSwitchMinWidth(width/2);
                }
            }
        }
    }

    public void onChildViewAddedToHierarchy(View parent, View child) {

    }

    public void onChildViewRemovedFromHierarchy(View parent, View child) {

    }
}
