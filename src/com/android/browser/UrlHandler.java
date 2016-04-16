/*
 * Copyright (C) 2010 The Android Open Source Project
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
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.android.browser.R;
import com.android.browser.platformsupport.Browser;

import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Matcher;

import org.codeaurora.swe.WebView;
import org.codeaurora.swe.util.SWEUrlUtils;

public class UrlHandler {

    private final static String TAG = "UrlHandler";

    // Use in overrideUrlLoading
    /* package */ final static String SCHEME_WTAI = "wtai://wp/";
    /* package */ final static String SCHEME_WTAI_MC = "wtai://wp/mc;";
    /* package */ final static String SCHEME_WTAI_SD = "wtai://wp/sd;";
    /* package */ final static String SCHEME_WTAI_AP = "wtai://wp/ap;";
    /* package */ final static String SCHEME_MAILTO = "mailto:";
    public static final String EXTRA_BROWSER_FALLBACK_URL = "browser_fallback_url";
    Controller mController;
    Activity mActivity;

    public UrlHandler(Controller controller) {
        mController = controller;
        mActivity = mController.getActivity();
    }

    boolean shouldOverrideUrlLoading(Tab tab, WebView view, String url) {
        if (view.isPrivateBrowsingEnabled()) {
            // Don't allow urls to leave the browser app when in
            // private browsing mode
            return false;
        }

        if (url.startsWith(WebView.SCHEME_TEL)) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(WebView.SCHEME_TEL +
                    Uri.encode(url.substring(WebView.SCHEME_TEL.length()))));
            mActivity.startActivity(intent);
            return true;
        }
        if (url.startsWith(SCHEME_WTAI)) {
            // wtai://wp/mc;number
            // number=string(phone-number)
            if (url.startsWith(SCHEME_WTAI_MC)) {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(WebView.SCHEME_TEL +
                        Uri.encode(url.substring(SCHEME_WTAI_MC.length()))));
                mActivity.startActivity(intent);
                // before leaving BrowserActivity, close the empty child tab.
                // If a new tab is created through JavaScript open to load this
                // url, we would like to close it as we will load this url in a
                // different Activity.
                mController.closeEmptyTab();
                return true;
            }
            // wtai://wp/sd;dtmf
            // dtmf=string(dialstring)
            if (url.startsWith(SCHEME_WTAI_SD)) {
                // TODO: only send when there is active voice connection
                return false;
            }
            // wtai://wp/ap;number;name
            // number=string(phone-number)
            // name=string
            if (url.startsWith(SCHEME_WTAI_AP)) {
                // TODO
                return false;
            }
        }

        // The "about:" schemes are internal to the browser; don't want these to
        // be dispatched to other apps.
        if (url.startsWith("about:")) {
            return false;
        }

        if (url.startsWith("ae://") && url.endsWith("add-fav")) {
            mController.startAddMyNavigation(url);
            return true;
        }

        // add for carrier feature - recognize additional website format
        // here add to support "mailto:" scheme
        if (url.startsWith(SCHEME_MAILTO) && handleMailtoTypeUrl(url)) {
            return true;
        }

        // add for carrier feature - wap2estore
        boolean wap2estore = BrowserConfig.getInstance(mController.getContext())
                .hasFeature(BrowserConfig.Feature.WAP2ESTORE);
        if (wap2estore && isEstoreTypeUrl(url) && handleEstoreTypeUrl(url)) {
            return true;
        }

        if (startActivityForUrl(tab, url)) {
            return true;
        }

        if (handleMenuClick(tab, url)) {
            return true;
        }

        return false;
    }

    private boolean handleMailtoTypeUrl(String url) {
        Intent intent;
        // perform generic parsing of the URI to turn it into an Intent.
        try {
            intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            mActivity.startActivity(intent);
        } catch (URISyntaxException ex) {
            Log.w(TAG, "Bad URI " + url + ": " + ex.getMessage());
            return false;
        } catch (ActivityNotFoundException ex) {
            Log.w(TAG, "No Activity Found for " + url);
        }

        return true;
    }

    private boolean isEstoreTypeUrl(String url) {
        if (url != null && url.startsWith("estore:")) {
            return true;
        }
        return false;
    }

    private boolean handleEstoreTypeUrl(String url) {
        if (url.getBytes().length > 256) {
            Toast.makeText(mActivity, R.string.estore_url_warning, Toast.LENGTH_LONG).show();
            return false;
        }

        Intent intent;
        // perform generic parsing of the URI to turn it into an Intent.
        try {
            intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            mActivity.startActivity(intent);
        } catch (URISyntaxException ex) {
            Log.w(TAG, "Bad URI " + url + ": " + ex.getMessage());
            return false;
        } catch (ActivityNotFoundException ex) {
            String downloadUrl = mActivity.getResources().getString(R.string.estore_homepage);
            mController.loadUrl(mController.getCurrentTab(), downloadUrl);
            Toast.makeText(mActivity, R.string.download_estore_app, Toast.LENGTH_LONG).show();
        }

        return true;
    }


    boolean startActivityForUrl(Tab tab, String url) {
      Intent intent;
      // perform generic parsing of the URI to turn it into an Intent.
      try {
          intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
      } catch (URISyntaxException ex) {
          Log.w(TAG, "Bad URI " + url + ": " + ex.getMessage());
          return false;
      }

      // handle fallback url for deep linking apps
      String browserFallbackUrl = intent.getStringExtra(EXTRA_BROWSER_FALLBACK_URL);
      if (browserFallbackUrl != null
              && SWEUrlUtils.isValidForIntentFallbackNavigation(browserFallbackUrl)) {
          mController.loadUrl(mController.getCurrentTab(), browserFallbackUrl);
          mController.closeEmptyTab();
          return true;
      }

      // check whether the intent can be resolved. If not, we will see
      // whether we can download it from the Market.
      if (mActivity.getPackageManager().resolveActivity(intent, 0) == null) {
          String packagename = intent.getPackage();
          if (packagename != null) {
              try {
                intent = new Intent(Intent.ACTION_VIEW, Uri
                        .parse("market://details?id=" + packagename));
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setPackage("com.android.vending");
                mActivity.startActivity(intent);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                // before leaving BrowserActivity, close the empty child tab.
                // If a new tab is created through JavaScript open to load this
                // url, we would like to close it as we will load this url in a
                // different Activity.
                mController.closeEmptyTab();
                return true;
              } catch (ActivityNotFoundException e) {
                Log.w(TAG, "Play store not found while searching for : " + packagename);
                CharSequence alert = mActivity.getResources().getString(
                    R.string.msg_no_google_play);
                Toast t = Toast.makeText(mActivity , alert, Toast.LENGTH_SHORT);
                t.show();
                return false;
              }
          } else {
              return false;
          }
      }

      // sanitize the Intent, ensuring web pages can not bypass browser
      // security (only access to BROWSABLE activities).
      intent.addCategory(Intent.CATEGORY_BROWSABLE);
      intent.setComponent(null);
      // Re-use the existing tab if the intent comes back to us
      if (tab != null) {
          if (tab.getAppId() == null) {
              tab.setAppId(mActivity.getPackageName() + "-" + tab.getId());
          }
          intent.putExtra(Browser.EXTRA_APPLICATION_ID, tab.getAppId());
      }

      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

      // Make sure webkit can handle it internally before checking for specialized
      // handlers. If webkit can't handle it internally, we need to call
      // startActivityIfNeeded
      Matcher m = UrlUtils.ACCEPTED_URI_SCHEMA.matcher(url);
      if (m.matches() && !isSpecializedHandlerAvailable(intent)) {
          return false;
      }
      try {
          intent.putExtra(BrowserActivity.EXTRA_DISABLE_URL_OVERRIDE, true);
          if (mActivity.startActivityIfNeeded(intent, -1)) {
              // before leaving BrowserActivity, close the empty child tab.
              // If a new tab is created through JavaScript open to load this
              // url, we would like to close it as we will load this url in a
              // different Activity.
              mController.closeEmptyTab();
              return true;
          }
      } catch (ActivityNotFoundException ex) {
          // ignore the error. If no application can handle the URL,
          // eg about:blank, assume the browser can handle it.
      }

      return false;
    }

    /**
     * Search for intent handlers that are specific to this URL
     * aka, specialized apps like google maps or youtube
     */
    private boolean isSpecializedHandlerAvailable(Intent intent) {
        PackageManager pm = mActivity.getPackageManager();
          List<ResolveInfo> handlers = pm.queryIntentActivities(intent,
                  PackageManager.GET_RESOLVED_FILTER);
          if (handlers == null || handlers.size() == 0) {
              return false;
          }
          for (ResolveInfo resolveInfo : handlers) {
              IntentFilter filter = resolveInfo.filter;
              if (filter == null) {
                  // No intent filter matches this intent?
                  // Error on the side of staying in the browser, ignore
                  continue;
              }
              if (filter.countDataAuthorities() == 0 && filter.countDataPaths() == 0) {
                  // Generic handler, skip
                  continue;
              }
              return true;
          }
          return false;
    }

    // In case a physical keyboard is attached, handle clicks with the menu key
    // depressed by opening in a new tab
    boolean handleMenuClick(Tab tab, String url) {
        if (mController.isMenuDown()) {
            mController.openTab(url,
                    (tab != null) && tab.isPrivateBrowsingEnabled(),
                    !BrowserSettings.getInstance().openInBackground(), true);
            mActivity.closeOptionsMenu();
            return true;
        }

        return false;
    }

}
