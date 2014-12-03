/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.browser;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.android.browser.preferences.GeneralPreferencesFragment;

public class BrowserPreferencesPage extends Activity {

    public static void startPreferencesForResult(Activity callerActivity, String url, int requestCode) {
        final Intent intent = new Intent(callerActivity, BrowserPreferencesPage.class);
        intent.putExtra(GeneralPreferencesFragment.EXTRA_CURRENT_PAGE, url);
        callerActivity.startActivityForResult(intent, requestCode);
    }

    public static void startPreferenceFragmentForResult(Activity callerActivity, String fragmentName, int requestCode) {
        final Intent intent = new Intent(callerActivity, BrowserPreferencesPage.class);
        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, fragmentName);
        callerActivity.startActivityForResult(intent, requestCode);
    }


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getAction();
            // check if this page was invoked by 'App Data Usage' on the global data monitor
            if ("android.intent.action.MANAGE_NETWORK_USAGE".equals(action)) {
                // TODO: switch to the Network fragment here?
            }
        }

        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new GeneralPreferencesFragment()).commit();
    }
}
