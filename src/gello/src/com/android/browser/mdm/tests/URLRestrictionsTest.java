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

package com.android.browser.mdm.tests;

import android.app.Instrumentation;
import android.os.Bundle;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.android.browser.BrowserActivity;
import com.android.browser.PreferenceKeys;
import com.android.browser.mdm.ManagedProfileManager;
import com.android.browser.mdm.URLFilterRestriction;

import org.codeaurora.swe.MdmManager;

public class URLRestrictionsTest extends ActivityInstrumentationTestCase2<BrowserActivity>
        implements PreferenceKeys {

    private final static String TAG = "URLRestrictionsTest";

    private Instrumentation mInstrumentation;
    private BrowserActivity mActivity;
    private URLFilterRestriction mUrlRestriction;

    public URLRestrictionsTest() {
        super(BrowserActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mActivity = getActivity();
        mUrlRestriction = URLFilterRestriction.getInstance();
    }

    private boolean isBlocked (final String url) {
        // Query native for blocked status for this url
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MdmManager.isMdmUrlBlocked(url);
            }
        });
        mInstrumentation.waitForIdleSync();

        // Wait for native to post the result
        while(!MdmManager.isMdmUrlBlockedResultReady()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Retrieve the result
        return MdmManager.getMdmUrlBlockedResult();
    }

    private boolean isBlocked (final String url, boolean expected) {
        boolean lastBlockedResult = isBlocked(url);

        if (lastBlockedResult != expected) {
            Log.e(TAG, "[" + url + "] should" + (expected ? " " : " NOT ") + "have been blocked");
        }
        else {
            //Log.i(TAG, "[" + url + "] was " + (expected ? " " : " not ") + "blocked as expected");
        }
        return expected;
    }

    public void testBl_FileBased() throws Throwable {
        Log.i(TAG,"!!! ******** Starting File Based Tests *************");

        clearURLRestrictions();
        assertFalse(isBlocked("file:///data/local/tmp/bl-test/1.html"));
        setUrlBlacklist("file:///data/local/tmp/bl-test/1.html");
        assertTrue(isBlocked("file:///data/local/tmp/bl-test/1.html"));
        clearURLRestrictions();
        assertFalse(isBlocked("file:///data/local/tmp/bl-test/1.html"));

        setUrlBlacklist("file:///data/local/tmp/bl-test");
        assertTrue(isBlocked("file:///data/local/tmp/bl-test/2.html"));
        clearURLRestrictions();
        assertFalse(isBlocked("file:///data/local/tmp/bl-test/2.html"));

        setUrlBlacklist("file:///data/local/tmp");
        assertTrue(isBlocked("file:///data/local/tmp/bl-test/3.html"));
        clearURLRestrictions();
        assertFalse(isBlocked("file:///data/local/tmp/bl-test/3.html"));

        setUrlBlacklist("file://*");
        assertTrue(isBlocked("file:///data/local/tmp/bl-test/4.html"));
        assertFalse(isBlocked("http://www.google.com"));
        clearURLRestrictions();
        assertFalse(isBlocked("file:///data/local/tmp/bl-test/4.html"));

        setUrlBlacklist("*");
        assertTrue(isBlocked("file:///data/local/tmp/bl-test/5.html"));

        clearURLRestrictions();
        assertFalse(isBlocked("file:///data/local/tmp/bl-test/5.html"));
    }

    public void testBl_ServerBased() throws Throwable {
        Log.i(TAG,"!!! ******** Starting Server Based *************");
        // only restricts http traffic on port 8080 that includes given path prefix on this server
        setUrlBlacklist("http://server:8080/path");
        assertTrue(isBlocked( "http://server:8080/path"));
        assertFalse(isBlocked("https://server:8080/path"));
        assertFalse(isBlocked("http://server:8080"));
        assertFalse(isBlocked("http://server:9090/path"));
        assertFalse(isBlocked("http://server/path"));
        assertFalse(isBlocked("http://server:80/path"));
        assertFalse(isBlocked("http://server"));

        // only restricts http traffic on port 8080 (any path) on given server
        setUrlBlacklist("http://server:8080");
        assertTrue(isBlocked("http://server:8080/path"));
        assertTrue(isBlocked("http://server:8080"));
        assertFalse(isBlocked("https://server:8080/path"));
        assertFalse(isBlocked("http://server:9090/path"));
        assertFalse(isBlocked("http://server/path"));
        assertFalse(isBlocked("http://server:80/path"));
        assertFalse(isBlocked("http://server"));

        // restricts all http traffic at given server on all ports
        setUrlBlacklist("http://server");
        assertTrue(isBlocked("http://server:8080/path"));
        assertTrue(isBlocked("http://server:8080"));
        assertFalse(isBlocked("https://server:8080/path"));
        assertFalse(isBlocked("ftp://server"));
        assertTrue(isBlocked("http://server:9090/path"));
        assertTrue(isBlocked("http://server/path"));
        assertTrue(isBlocked("http://server:80/path"));
        assertTrue(isBlocked("http://server"));
    }

    private void exampleDotComChecks() {
        assertFalse(isBlocked("http://www.google.com"));
        assertFalse(isBlocked("http://www.yahoo.com"));

        assertTrue(isBlocked("http://example.com"));
        assertTrue(isBlocked("http://example.com:80"));
        assertTrue(isBlocked("http://example.com:8080"));
        assertTrue(isBlocked("http://example.com:8080/path"));
        assertTrue(isBlocked("http://foo.example.com"));
        assertTrue(isBlocked("http://foo.example.com:80"));
        assertTrue(isBlocked("http://foo.example.com:8080"));
        assertTrue(isBlocked("http://foo.example.com:8080/path"));

        assertTrue(isBlocked("https://example.com"));
        assertTrue(isBlocked("https://example.com:80"));
        assertTrue(isBlocked("https://example.com:8080"));
        assertTrue(isBlocked("https://example.com:8080/path" ));
        assertTrue(isBlocked("https://foo.example.com"));
        assertTrue(isBlocked("https://foo.example.com:80"));
        assertTrue(isBlocked("https://foo.example.com:8080"));
        assertTrue(isBlocked("https://foo.example.com:8080/path"));

        assertTrue(isBlocked("ftp://example.com"));
        assertTrue(isBlocked("ftp://example.com:80"));
        assertTrue(isBlocked("ftp://example.com:8080"));
        assertTrue(isBlocked("ftp://example.com:8080/path" ));
        assertTrue(isBlocked("ftp://foo.example.com"));
        assertTrue(isBlocked("ftp://foo.example.com:80"));
        assertTrue(isBlocked("ftp://foo.example.com:8080"));
        assertTrue(isBlocked("ftp://foo.example.com:8080/path"));
    }

    public void testBl_URL_DomainOnly() throws Throwable {
        Log.i(TAG,"!!! ******** Starting domain only *************");
        // restricts all (i.e. http, https, ftp, ...) traffic on any port at given domain and any subdomains
        setUrlBlacklist("example.com");
        exampleDotComChecks();
    }

    public void testBl_URL_IPOnly() throws Throwable {
        Log.i(TAG,"!!! ******** Starting IP only *************");
        setUrlBlacklist("192.168.0.123");
        assertFalse(isBlocked("http://www.google.com"));
        assertFalse(isBlocked("http://www.yahoo.com"));

        assertTrue(isBlocked("http://192.168.0.123"));
        assertTrue(isBlocked("http://192.168.0.123:80"));
        assertTrue(isBlocked("http://192.168.0.123:8080"));
        assertTrue(isBlocked("http://192.168.0.123:8080/path"));

        assertTrue(isBlocked("https://192.168.0.123"));
        assertTrue(isBlocked("https://192.168.0.123:80"));
        assertTrue(isBlocked("https://192.168.0.123:8080"));
        assertTrue(isBlocked("https://192.168.0.123:8080/path"));

        assertTrue(isBlocked("ftp://192.168.0.123"));
        assertTrue(isBlocked("ftp://192.168.0.123:80"));
        assertTrue(isBlocked("ftp://192.168.0.123:8080"));
        assertTrue(isBlocked("ftp://192.168.0.123:8080/path"));
    }

    private void sslServerDotComChecks() {
        assertFalse(isBlocked("http://www.google.com"));
        assertFalse(isBlocked("http://www.yahoo.com"));

        assertTrue(isBlocked("https://ssl.server.com"));
        assertTrue(isBlocked("https://ssl.server.com:80"));
        assertTrue(isBlocked("https://ssl.server.com:8080"));
        assertTrue(isBlocked("https://ssl.server.com:8080/path"));
        assertTrue(isBlocked("https://foo.ssl.server.com"));
        assertTrue(isBlocked("https://foo.ssl.server.com:80"));
        assertTrue(isBlocked("https://foo.ssl.server.com:8080"));
        assertTrue(isBlocked("https://foo.ssl.server.com:8080/path"));

        assertFalse(isBlocked("http://ssl.server.com"));
        assertFalse(isBlocked("http://ssl.server.com:80"));
        assertFalse(isBlocked("http://ssl.server.com:8080"));
        assertFalse(isBlocked("http://ssl.server.com:8080/path"));
        assertFalse(isBlocked("http://foo.ssl.server.com"));
        assertFalse(isBlocked("http://foo.ssl.server.com:80"));
        assertFalse(isBlocked("http://foo.ssl.server.com:8080"));
        assertFalse(isBlocked("http://foo.ssl.server.com:8080/path"));

        assertFalse(isBlocked("ftp://ssl.server.com"));
        assertFalse(isBlocked("ftp://ssl.server.com:80"));
        assertFalse(isBlocked("ftp://ssl.server.com:8080"));
        assertFalse(isBlocked("ftp://ssl.server.com:8080/path"));
        assertFalse(isBlocked("ftp://foo.ssl.server.com"));
        assertFalse(isBlocked("ftp://foo.ssl.server.com:80"));
        assertFalse(isBlocked("ftp://foo.ssl.server.com:8080"));
        assertFalse(isBlocked("ftp://foo.ssl.server.com:8080/path"));
    }

    public void testBl_URL_Https() throws Throwable {
        Log.i(TAG,"!!! ******** Starting https tests *************");
        // restricts https traffic on any port at given domain and any subdomains
        setUrlBlacklist("https://ssl.server.com");
        sslServerDotComChecks();
    }

    private void hostingDotComChecks()  {
        assertFalse(isBlocked("http://www.google.com"));
        assertFalse(isBlocked("http://www.yahoo.com"));

        assertFalse(isBlocked("http://hosting.com"));
        assertFalse(isBlocked("http://hosting.com:80"));
        assertFalse(isBlocked("http://hosting.com:8080"));
        assertTrue(isBlocked("http://hosting.com:8080/bad_path"));
        assertFalse(isBlocked("http://foo.hosting.com"));
        assertFalse(isBlocked("http://foo.hosting.com:80"));
        assertFalse(isBlocked("http://foo.hosting.com:8080"));
        assertTrue(isBlocked("http://foo.hosting.com:8080/bad_path"));

        assertFalse(isBlocked("https://hosting.com"));
        assertFalse(isBlocked("https://hosting.com:80"));
        assertFalse(isBlocked("https://hosting.com:8080"));
        assertTrue(isBlocked("https://hosting.com:8080/bad_path"));
        assertFalse(isBlocked("https://foo.hosting.com"));
        assertFalse(isBlocked("https://foo.hosting.com:80"));
        assertFalse(isBlocked("https://foo.hosting.com:8080"));
        assertTrue(isBlocked("https://foo.hosting.com:8080/bad_path"));

        assertFalse(isBlocked("ftp://hosting.com" ));
        assertFalse(isBlocked("ftp://hosting.com:80"));
        assertFalse(isBlocked("ftp://hosting.com:8080"));
        assertTrue(isBlocked("ftp://hosting.com:8080/bad_path"));
        assertFalse(isBlocked("ftp://foo.hosting.com"));
        assertFalse(isBlocked("ftp://foo.hosting.com:80"));
        assertFalse(isBlocked("ftp://foo.hosting.com:8080"));
        assertTrue(isBlocked("ftp://foo.hosting.com:8080/bad_path"));
    }

    public void testBl_URL_DomainPath() throws Throwable {
        Log.i(TAG,"!!! ******** Starting Domain Path tests *************");
        //restricts all traffic on any port at given domain (and any subdomains) that includes given path prefix
        setUrlBlacklist("hosting.com/bad_path");
        hostingDotComChecks();
    }

    public void testBl_URL_SubDomains() throws Throwable {
        Log.i(TAG,"!!! ******** Starting SubDomain tests *************");
        // restricts all traffic on any port from domain 'exact.hostname.com' but allows traffic from subdomains
        // like "foobar.exact.hostname.com"
        setUrlBlacklist(".exact.hostname.com");
        assertFalse(isBlocked("http://www.google.com"));
        assertFalse(isBlocked("http://www.yahoo.com"));

        assertFalse(isBlocked("http://www.google.com"));
        assertFalse(isBlocked("http://www.yahoo.com"));

        assertTrue(isBlocked("http://exact.hostname.com"));
        assertTrue(isBlocked("http://exact.hostname.com:8080"));
        assertTrue(isBlocked("http://exact.hostname.com:8080/path"));

        assertTrue(isBlocked("https://exact.hostname.com"));
        assertTrue(isBlocked("https://exact.hostname.com:8080"));
        assertTrue(isBlocked("https://exact.hostname.com:8080/path"));

        assertTrue(isBlocked("ftp://exact.hostname.com"));
        assertTrue(isBlocked("ftp://exact.hostname.com:8080"));
        assertTrue(isBlocked("ftp://exact.hostname.com:8080/path"));

        assertFalse(isBlocked("http://foo.exact.hostname.com"));
        assertFalse(isBlocked("http://foo.exact.hostname.com:8080"));
        assertFalse(isBlocked("http://foo.exact.hostname.com:8080/path"));

        assertFalse(isBlocked("https://foo.exact.hostname.com"));
        assertFalse(isBlocked("https://foo.exact.hostname.com:8080"));
        assertFalse(isBlocked("https://foo.exact.hostname.com:8080/path"));

        assertFalse(isBlocked("ftp://foo.exact.hostname.com"));
        assertFalse(isBlocked("ftp://foo.exact.hostname.com:8080"));
        assertFalse(isBlocked("ftp://foo.exact.hostname.com:8080/path"));
    }

    public void testBl_URL_Universal() throws Throwable {
        Log.i(TAG,"!!! ******** Starting Universal Blacklist tests *************");
        //restricts everything. No URLS will get through.
        setUrlBlacklist("*");
        assertTrue(isBlocked("http://www.google.com"));
        assertTrue(isBlocked("http://www.yahoo.com"));

        assertTrue(isBlocked("http://hosting.com"));
        assertTrue(isBlocked("ftp://hosting.com:8080/bad_path"));
        assertTrue(isBlocked("ftp://hosting.com:8080"));
        assertTrue(isBlocked("ftp://hosting.com:80"));
        assertTrue(isBlocked("https://ssl.server.com"));
        assertTrue(isBlocked("https://ssl.server.com:80"));
        assertTrue(isBlocked("https://ssl.server.com:8080"));
        assertTrue(isBlocked("https://ssl.server.com:8080/path"));
        assertTrue(isBlocked("https://foo.ssl.server.com"));
        assertTrue(isBlocked("https://foo.ssl.server.com:80"));
        assertTrue(isBlocked("https://foo.ssl.server.com:8080"));
        assertTrue(isBlocked("https://foo.ssl.server.com:8080/path"));
        assertTrue(isBlocked("http://192.168.0.123"));
        assertTrue(isBlocked("http://192.168.0.123:80"));
        assertTrue(isBlocked("http://192.168.0.123:8080"));
        assertTrue(isBlocked("http://192.168.0.123:8080/path"));
        assertTrue(isBlocked("http://server:8080/path"));
        assertTrue(isBlocked("https://server:8080/path"));
        assertTrue(isBlocked("http://server:8080"));
        assertTrue(isBlocked("http://server:9090/path"));
        assertTrue(isBlocked("http://server/path"));
        assertTrue(isBlocked("http://server:80/path"));
        assertTrue(isBlocked("http://server"));
    }

    public void testBl_Multiple() throws Throwable {
        Log.i(TAG,"!!! ******** Starting Multiple Blacklist tests *************");

        // Multiple black list specs, comma separated
        setUrlBlacklist("hosting.com/bad_path,https://ssl.server.com,example.com");
        hostingDotComChecks();
        sslServerDotComChecks();
        exampleDotComChecks();

        // test extra whitespace
        setUrlBlacklist("    hosting.com/bad_path  , https://ssl.server.com\t,     example.com  ");
        hostingDotComChecks();
        sslServerDotComChecks();
        exampleDotComChecks();
    }

    // Basic Whitelist Test
    public void testWl_Basic() throws Throwable {
        Log.i(TAG,"!!! ******** Starting Whitelist tests *************");
        // basic whitelist.  First we set universal black list, then exempt specific
        // urls.
        setURLRestrictions("*","google.com, https://yahoo.com," +
                               " http://foo.com:80, http://bar.com:80/path, .fubar.com");

        // Check that sites unrelated to whitelist are blocked
        assertTrue(isBlocked("http://facebook.com"));
        assertTrue(isBlocked("http://twitter.com"));


        // google.com:
        //   types: all
        //   ports: all
        //   paths: all
        //   subdomains: all
        assertFalse(isBlocked("http://google.com"));
        assertFalse(isBlocked("https://google.com"));
        assertFalse(isBlocked("ftp://google.com"));

        assertFalse(isBlocked("http://google.com:80"));
        assertFalse(isBlocked("http://google.com:8080"));

        assertFalse(isBlocked("http://google.com/path1"));
        assertFalse(isBlocked("http://google.com/path2"));

        assertFalse(isBlocked("http://fr.google.com"));
        assertFalse(isBlocked("http://us.google.com"));

        // yahoo.com:
        //   types: https only
        //   ports: all
        //   paths: all
        //   subdomains: all
        assertTrue(isBlocked("http://yahoo.com"));
        assertFalse(isBlocked("https://yahoo.com"));
        assertTrue(isBlocked("ftp://yahoo.com"));

        assertFalse(isBlocked("https://yahoo.com:80"));
        assertFalse(isBlocked("https://yahoo.com:8080"));

        assertFalse(isBlocked("https://yahoo.com/path1"));
        assertFalse(isBlocked("https://yahoo.com/path2"));

        assertFalse(isBlocked("https://fr.yahoo.com"));
        assertFalse(isBlocked("https://us.yahoo.com"));

        // foo.com
        //   types: http only
        //   ports: 80 only
        //   paths: all
        //   subdomains: all
        assertFalse(isBlocked("http://foo.com:80"));
        assertTrue(isBlocked("https://foo.com:80"));
        assertTrue(isBlocked("ftp://foo.com:80"));

        assertFalse(isBlocked("http://foo.com:80"));
        assertTrue(isBlocked("http://foo.com:8080"));

        assertFalse(isBlocked("http://foo.com:80/path1"));
        assertFalse(isBlocked("http://foo.com:80/path2"));

        assertFalse(isBlocked("http://fr.foo.com:80"));
        assertFalse(isBlocked("http://us.foo.com:80"));

        // bar.com
        //   types: http only
        //   ports: 80 only
        //   paths: 'path' and all subs
        //   subdomains: all
        assertFalse(isBlocked("http://bar.com:80/path"));
        assertTrue(isBlocked("https://bar.com:80/path"));
        assertTrue(isBlocked("ftp://bar.com:80/path"));

        assertFalse(isBlocked("http://bar.com:80/path"));
        assertTrue(isBlocked("http://bar.com:8080/path"));

        assertFalse(isBlocked("http://bar.com:80/path"));
        assertFalse(isBlocked("http://bar.com:80/path/sub1"));
        assertFalse(isBlocked("http://bar.com:80/path/sbu2/sub3"));

        assertTrue(isBlocked("http://bar.com:80/anotherpath"));

        assertFalse(isBlocked("http://fr.bar.com:80/path"));
        assertFalse(isBlocked("http://us.bar.com:80/path"));

        // fubar.com
        //   types: all
        //   ports: all
        //   paths: all
        //   subdomains: none
        assertFalse(isBlocked("http://fubar.com"));
        assertFalse(isBlocked("https://fubar.com"));
        assertFalse(isBlocked("ftp://fubar.com"));

        assertFalse(isBlocked("http://fubar.com:80"));
        assertFalse(isBlocked("http://fubar.com:8080"));

        assertFalse(isBlocked("http://fubar.com/path1"));
        assertFalse(isBlocked("http://fubar.com/path2"));

        assertTrue(isBlocked("http://fr.fubar.com"));
        assertTrue(isBlocked("http://us.fubar.com"));
    }

        /**
         * Activate URL restriction
         * @param blackList  Required. comma separated list of URL restrictions. If null,
         *                   no restrictions are set.
         * @param whiteList  Optional exceptions to blacklist.
         *
         * Note: we don't enforce blackList/whiteList requirements here, this will be done
         *                   in the URLRestrictions.enforce() method.
         */
    private void setURLRestrictions(String blackList, String whiteList) {
        // Construct restriction bundle
        final Bundle restrictions = new Bundle();

        if (blackList != null)
            restrictions.putString(URLFilterRestriction.URL_BLACK_LIST, blackList);

        if (whiteList != null)
            restrictions.putString(URLFilterRestriction.URL_WHITE_LIST, whiteList);

        // Deliver restriction on UI thread
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ManagedProfileManager.getInstance().setMdmRestrictions(restrictions);
            }
        });

        // Wait to ensure restriction is set
        mInstrumentation.waitForIdleSync();
    }

    private void clearURLRestrictions() {
        setURLRestrictions(null, null);
    }

    private void setUrlBlacklist(String bl) {
        setURLRestrictions(bl, null);
    }
}
