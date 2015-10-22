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
import android.app.Fragment;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import com.android.browser.preferences.AboutPreferencesFragment;
import com.android.browser.preferences.GeneralPreferencesFragment;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class BrowserPreferencesPage extends Activity {
    public static String sResultExtra;
    public static String LOGTAG = "BrowserPreferencesPage";
    private static ArrayList<String> sUpdatedUrls =
            new ArrayList<String>(); //List of URLS for whom settings were updated

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

    public static void startPreferenceFragmentExtraForResult(Activity callerActivity,
                                                             String fragmentName,
                                                             Bundle bundle,
                                                             int requestCode) {
        final Intent intent = new Intent(callerActivity, BrowserPreferencesPage.class);
        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, fragmentName);
        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, bundle);
        callerActivity.startActivityForResult(intent, requestCode);
    }


    @Override
    public void onCreate(Bundle icicle) {
        if (!EngineInitializer.isInitialized()) {
            Log.e(LOGTAG, "Engine not Initialized");
            EngineInitializer.initializeSync((Context) getApplicationContext());
        }
        super.onCreate(icicle);
        if (icicle != null) {
            return;
        }

        sResultExtra = "";
        sUpdatedUrls.clear();
        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getAction();
            // check if this page was invoked by 'App Data Usage' on the global data monitor
            if ("android.intent.action.MANAGE_NETWORK_USAGE".equals(action)) {
                // TODO: switch to the Network fragment here?
            }

            Bundle extras = intent.getExtras();
            if (extras == null){
                getFragmentManager().beginTransaction().replace(android.R.id.content,
                        new GeneralPreferencesFragment()).commit();
                return;
            }

            String fragment = (String) extras.getCharSequence(PreferenceActivity.EXTRA_SHOW_FRAGMENT);
            if (fragment != null) {
                try {
                    Class<?> cls = Class.forName(fragment);
                    Constructor<?> ctor = cls.getConstructor();
                    Object obj = ctor.newInstance();

                    if (obj instanceof Fragment) {
                        Fragment frag = (Fragment) obj;

                        Bundle bundle = extras.getBundle(PreferenceActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
                        if (bundle != null) {
                            frag.setArguments(bundle);
                        }

                        getFragmentManager().beginTransaction().replace(
                                android.R.id.content,
                                (Fragment) obj).commit();
                    }
                } catch (ClassNotFoundException e) {
                } catch (NoSuchMethodException e) {
                } catch (InvocationTargetException e) {
                } catch (InstantiationException e) {
                } catch (IllegalAccessException e) {
                }
                return;
            }
        }

        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new GeneralPreferencesFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (getFragmentManager().getBackStackEntryCount() > 0) {
                    getFragmentManager().popBackStack();
                } else {
                    finish();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        if (!TextUtils.isEmpty(sResultExtra)) {
            Intent intent = this.getIntent();
            intent.putExtra(Intent.EXTRA_TEXT, sResultExtra);
            intent.putStringArrayListExtra(Controller.EXTRA_UPDATED_URLS, sUpdatedUrls);
            this.setResult(RESULT_OK, intent);
        }
        super.finish();
    }

    public static void onUrlNeedsReload(String url) {
        String host = (Uri.parse(url)).getHost();
        if (!sUpdatedUrls.contains(host)) {
            sUpdatedUrls.add(host);
        }
    }
}
