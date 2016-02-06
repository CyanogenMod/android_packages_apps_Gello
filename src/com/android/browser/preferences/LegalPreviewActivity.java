/*
 *  Copyright (c) 2015 The Linux Foundation. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 *      * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 *  ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 *  BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 *  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 *  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 *  OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 *  IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.android.browser.preferences;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.util.Log;
import android.content.Context;
import com.android.browser.EngineInitializer;

import com.android.browser.R;

public class LegalPreviewActivity extends FragmentActivity {
    LegalPreviewFragment mLegalPreviewFragment;
    protected static final String URL_INTENT_EXTRA = "url";
    private final static String LOGTAG = "LegalPreviewActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (!EngineInitializer.isInitialized()) {
            Log.e(LOGTAG, "Engine not Initialized");
            EngineInitializer.initializeSync((Context) getApplicationContext());
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.credits_tab);
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setTitle(R.string.swe_open_source_licenses);
            bar.setDisplayHomeAsUpEnabled(true);
        }
        if (savedInstanceState == null) {
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            mLegalPreviewFragment = new LegalPreviewFragment();
            Bundle args = new Bundle();
            args.putString(URL_INTENT_EXTRA, getIntent().getExtras()
                            .getString(URL_INTENT_EXTRA));
            mLegalPreviewFragment.setArguments(args);
            fragmentTransaction.add(R.id.license_layout, mLegalPreviewFragment,
                "LegalPreviewFragmentTag");
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        } else  {
            mLegalPreviewFragment =
                (LegalPreviewFragment) getFragmentManager().findFragmentByTag(
                    "LegalPreviewFragmentTag");
        }
    }

    private boolean back() {
        if(!mLegalPreviewFragment.onBackPressed()) {
            onBackPressed();
            return true;
        } else {
            return false;
        }
    }
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (event.isTracking() && !event.isCanceled()) {
                    return back();
                }
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                return back();
        }
        return super.onOptionsItemSelected(item);
    }
}
