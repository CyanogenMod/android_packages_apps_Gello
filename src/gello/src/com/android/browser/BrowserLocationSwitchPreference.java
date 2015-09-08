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
package com.android.browser;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;

import org.codeaurora.swe.PermissionsServiceFactory;

public class BrowserLocationSwitchPreference extends SwitchPreference {

    View mView;
    private boolean mSwitchEnabled = true; //internal state tracker
    private OnPreferenceClickListener onPreferenceClickListener;
    private OnPreferenceChangeListener oldPreferenceChangeListener;

    public BrowserLocationSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BrowserLocationSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public BrowserLocationSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BrowserLocationSwitchPreference(Context context) {
        super(context);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mView = view;
        if (oldPreferenceChangeListener == null){ //set it just the first time
            oldPreferenceChangeListener = getOnPreferenceChangeListener();
        }
        if (mView != null && mSwitchEnabled) {
            mView.setAlpha((float) 1.0);
        }
        else if (mView != null){
            mView.setAlpha((float) 0.5); //Gray out the option
        }
    }

    @Override
    public void onClick(){
        //This toggles teh switch
        if (PermissionsServiceFactory.isSystemLocationEnabled() && mSwitchEnabled) super.onClick();
        else {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            getContext().startActivity(intent);
        };
    }

    @Override
    public void setEnabled(boolean enable){
        // setEnabled will not call super.setEnabled(enable) because
        // we want to avoid the default behavior entirely.
        if (!mSwitchEnabled && enable) { //Transition from off to on
            if(mView != null)
            mView.setAlpha((float) 1.0);
            setOnPreferenceClickListener(onPreferenceClickListener);
            setOnPreferenceChangeListener(oldPreferenceChangeListener);
        }
        else if(!enable && mSwitchEnabled) { //Transition from on to off
            if (mView != null) {
                mView.setAlpha((float) 0.5); //Gray out the option
            }
            onPreferenceClickListener = getOnPreferenceClickListener();
            if (oldPreferenceChangeListener == null) //to protect against calling !enable onresume()
                oldPreferenceChangeListener = getOnPreferenceChangeListener();

            // Prevent clicks from registering.
            setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    return false; // Do Not update
                }
            });
        }
        mSwitchEnabled = enable;
    }
}
