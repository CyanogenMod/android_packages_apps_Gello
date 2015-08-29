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

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
//import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.browser.R;

public class MdmCheckBoxPreference extends SwitchPreference {

    View mView = null;
    OnPreferenceClickListener mOrigClickListener = null;
    boolean mMdmRestrictionState;
    private boolean mPrefEnabled = true;

    public MdmCheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MdmCheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public MdmCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MdmCheckBoxPreference(Context context) {
        super(context);
    }

    public void setMdmRestrictionState(boolean val) {
        // Log.i("+++", "setMdmRestrictionState(" + val + ")");
        mMdmRestrictionState = val;
    }

    public void disablePref() {
        // Log.i("+++", "disablePref(): mView[" +
        //        (mView != null ? "OK" : "Null") +
        //        "]  mPrefEnabled[" + mPrefEnabled + "]");

        if (null != mView && mPrefEnabled == true) {
            // Set the onClick listener that will present the toast message to the user
            mOrigClickListener = getOnPreferenceClickListener();

            // Log.i("+++", "Setting toast");
            setOnPreferenceClickListener( new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Toast.makeText(getContext(), R.string.mdm_managed_alert, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });

            // Prevent clicks from registering. We can't use setEnable() because
            // we need the click to trigger the toast message.
            setOnPreferenceChangeListener( new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    return false; // Do Not update
                }
            });

            // Dim the view. setEnable usually does this for us, but we can't
            // use it here.
            mView.setAlpha((float) 0.5);
            mPrefEnabled = false;

        }
    }

    public void enablePref () {
        // Log.i("+++", "enablePref(): mView[" +
        //        (mView != null ? "OK" : "Null") +
        //        "]  mPrefEnabled[" + mPrefEnabled + "]");

        if (null != mView && mPrefEnabled == false) {
            setOnPreferenceClickListener(mOrigClickListener);
            setOnPreferenceChangeListener(null);
            mView.setAlpha((float) 1.0);
            mPrefEnabled = true;
        }
    }

    @Override
	protected void onBindView(View view) {
        // Log.i("+++", "onBindView() : rs[" + mMdmRestrictionState + "]");
        super.onBindView(view);

        mView = view;
        if (mMdmRestrictionState) {
            disablePref();
        }
    }
}
