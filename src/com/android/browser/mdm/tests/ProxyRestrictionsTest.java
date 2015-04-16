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
import org.chromium.net.ProxyChangeListener;

public class ProxyRestrictionsTest extends ActivityInstrumentationTestCase2<BrowserActivity>
        implements PreferenceKeys {

    private final static String TAG = "ProxyRestrictionsTest";

    private Instrumentation mInstrumentation;
    private BrowserActivity mActivity;
    private ProxyChangeListener mPcl;

    public ProxyRestrictionsTest() {
        super(BrowserActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mActivity = getActivity();
        mPcl = ProxyChangeListener.create(getInstrumentation().getTargetContext());
    }

    /*
     * Proxy Restrictions Tests
     */
    // Properties we can query ProxyChangeListener for:
    //  http.proxyHost     , https.proxyHost
    //  http.proxyPort     , https.proxyPort
    //  http.nonProxyHosts , https.nonProxyHosts
    //  ProxyServer          (ProxyChangeListener.PROXY_SERVER)
    //  ProxyPacUrl          (ProxyChangeListener.PROXY_PAC_URL)
    //  ProxyBypassList      (ProxyChangeListener.PROXY_BYPASS_LIST)
    public void logProxyConfig (ProxyChangeListener.ProxyConfig pc) {
        if (pc != null) {
            Log.v(TAG, "ProxyConfig Dump:");
            Log.v(TAG, pc.toString());
        } else {
            Log.v(TAG, "ProxyConfig Dump: pc was NULL");
        }
    }

    public void checkValue (String key, String expected) {
        if (expected == null) {
            assertNull(ProxyChangeListener.getProperty(key));
            // native gives us empty strings, not nulls
            assertEquals(mPcl.fetchProxyPropertyFromNative(key), "");
        }
        else {
            assertEquals(ProxyChangeListener.getProperty(key), expected);
            assertEquals(mPcl.fetchProxyPropertyFromNative(key), expected);
        }
    }

    // Ensure we start mode and proxyConfig as null
    public void testPreconditions() throws Throwable {
        Log.v(TAG, "== Init Conditions ==");

        // check configured proxy mode
        assertNull("Init: mode should be null", ProxyChangeListener.getmMdmProxyMode());

        // get the proxy config from ProxyChangeListener
        assertNull("Init: proxyConfig should be null", ProxyChangeListener.getMdmProxyConfig());

        checkValue("http.proxyHost", null);
        checkValue("http.proxyPort", null);
        checkValue("http.nonProxyHosts", null);
    }

    // If you choose to never use a proxy server and always connect directly,
    // all other options are ignored.
    public void testProxy_ModeDirect() throws Throwable {
        String mode = ProxyChangeListener.MODE_DIRECT;
        Log.v(TAG, "== Testing " + mode + " ==");

        // set the restrictions
        setProxyRestrictions(mode, null, null, null);

        // check configured proxy mode
        String configuredMode = ProxyChangeListener.getmMdmProxyMode();
        assertEquals(mode + ": configuration", mode, configuredMode);

        // get the proxy config from ProxyChangeListener
        ProxyChangeListener.ProxyConfig pc = ProxyChangeListener.getMdmProxyConfig();
        assertNull(mode +": proxyConfig should be null", pc);

        checkValue("http.proxyHost", null);
        checkValue("http.proxyPort", null);
        checkValue("http.nonProxyHosts", null);
    }

    // If you choose to use system proxy settings or auto detect the proxy server,
    // all other options are ignored.
    public void testProxy_ModeSystem() throws Throwable {
        String mode = ProxyChangeListener.MODE_SYSTEM;
        Log.v(TAG, "== Testing " + mode + " ==");

        // Clear any restrictions
        setProxyRestrictions(null, null, null, null);

        // set the restrictions
        setProxyRestrictions(mode, null, null, null);

        // check configured proxy mode
        String configuredMode = ProxyChangeListener.getmMdmProxyMode();
        assertNotNull(configuredMode);
        assertEquals(mode + ": configuration",mode,configuredMode);

        // get the proxy config from ProxyChangeListener
        ProxyChangeListener.ProxyConfig pc = ProxyChangeListener.getMdmProxyConfig();
        assertNull(mode +": proxyConfig should be null", pc);

        checkValue("http.proxyHost", null);
        checkValue("http.proxyPort", null);
        checkValue("http.nonProxyHosts", null);
    }

    // If you choose to use system proxy settings or auto detect the proxy server,
    // all other options are ignored.
    public void testProxy_ModeAutoDetect() throws Throwable {
        String mode = ProxyChangeListener.MODE_AUTO_DETECT;
        Log.v(TAG, "== Testing " + mode + " ==");

        // Clear any restrictions
        setProxyRestrictions(null, null, null, null);

        // set the restrictions
        setProxyRestrictions(mode, null, null, null);

        // check configured proxy mode
        String configuredMode = ProxyChangeListener.getmMdmProxyMode();
        assertNotNull(configuredMode);
        assertEquals(mode + ": configuration",mode,configuredMode);

        // get the proxy config from ProxyChangeListener
        ProxyChangeListener.ProxyConfig pc = ProxyChangeListener.getMdmProxyConfig();
        assertNull(mode +": proxyConfig should be null", pc);

        checkValue("http.proxyHost", null);
        checkValue("http.proxyPort", null);
        checkValue("http.nonProxyHosts", null);
    }

    // If you choose fixed server proxy mode, you can specify further options in
    // 'Address or URL of proxy server' and 'Comma-separated list of proxy bypass rules'.
    public void testProxy_ModeFixedServers() throws Throwable {
        String mode = ProxyChangeListener.MODE_FIXED_SERVERS;
        Log.v(TAG, "== Testing " + mode + " ==");

        String proxyHost   = "192.241.207.220";
        String proxyPort   = "9090";
        String proxyServer = "http://" + proxyHost + ":" + proxyPort;
        String configuredMode;

        // Clear any restrictions
        setProxyRestrictions(null, null, null, null);

        // Test that mode didn't get set if no proxy server is set
        setProxyRestrictions(mode, null, null, null);
        configuredMode = ProxyChangeListener.getmMdmProxyMode();
        assertNull(configuredMode);

        //
        // set the restrictions without Exclusion List
        //
        setProxyRestrictions(mode, proxyServer, null, null);

        // check configured proxy mode
        configuredMode = ProxyChangeListener.getmMdmProxyMode();
        assertNotNull(configuredMode);
        assertEquals(mode + ": configuration",mode,configuredMode);

        logProxyConfig(ProxyChangeListener.getMdmProxyConfig());

        // check proxy values
        checkValue("http.proxyHost", proxyHost);
        checkValue("http.proxyPort", proxyPort);
        checkValue("http.nonProxyHosts", null);

        //
        // set the restrictions with Exclusion list
        //
        setProxyRestrictions(mode, proxyServer, "*.google.com, *foo.com, 127.0.0.1:8080", null);

        // check configured proxy mode
        configuredMode = ProxyChangeListener.getmMdmProxyMode();
        assertNotNull(configuredMode);
        assertEquals(mode + ": configuration",mode,configuredMode);

        logProxyConfig(ProxyChangeListener.getMdmProxyConfig());

        // check properties
        checkValue("http.proxyHost", proxyHost);
        checkValue("http.proxyPort", proxyPort);

        String expected = "*.google.com|*foo.com|127.0.0.1:8080";
        checkValue("http.nonProxyHosts", expected);
    }

    // If you choose to use a .pac proxy script, you must specify the URL to the
    // script in 'URL to a proxy .pac file'.
    public void testProxy_ModePacScript() throws Throwable {
        String mode = ProxyChangeListener.MODE_PAC_SCRIPT;
        Log.v(TAG, "== Testing " + mode + " ==");

        // Clear any restrictions
        setProxyRestrictions(null, null, null, null);

        // set the restrictions without pac url
        setProxyRestrictions(mode, null, null, null);
        assertNull(ProxyChangeListener.getmMdmProxyMode()); // registered mode should be null

        // set the restrictions
        String pacUrl = "http://internal.site:8888/example.pac";
        setProxyRestrictions(mode, null, null, pacUrl);

        // check configured proxy mode
        String configuredMode = ProxyChangeListener.getmMdmProxyMode();
        assertNotNull(configuredMode);
        assertEquals(mode + ": configuration",mode,configuredMode);

        logProxyConfig(ProxyChangeListener.getMdmProxyConfig());

        checkValue(ProxyChangeListener.PROXY_PAC_URL, pacUrl);
    }

    public void testProxy_SwitchModesWithoutClear() throws Throwable {
        String mode;
        Log.v(TAG, "== Testing Proxy Switch ==");

        String proxyHost   = "192.241.207.220";
        String proxyPort   = "9090";
        String proxyServer = "http://" + proxyHost + ":" + proxyPort;
        String configuredMode;

        // Clear any restrictions
        setProxyRestrictions(null, null, null, null);

        //
        // set to Fixed Servers with exclusion list
        //
        mode = ProxyChangeListener.MODE_FIXED_SERVERS;
        Log.v(TAG, "-- Setting mode " + mode + " ==");

        setProxyRestrictions(mode, proxyServer, "*.google.com, *foo.com, 127.0.0.1:8080", null);

        // check configured proxy mode
        configuredMode = ProxyChangeListener.getmMdmProxyMode();
        assertNotNull(configuredMode);
        assertEquals(mode + ": configuration",mode,configuredMode);

        //logProxyConfig(ProxyChangeListener.getMdmProxyConfig());

        // check properties
        checkValue("http.proxyHost", proxyHost);
        checkValue("http.proxyPort", proxyPort);

        String expected = "*.google.com|*foo.com|127.0.0.1:8080";
        checkValue("http.nonProxyHosts", expected);

        //
        // Now set to direct mode
        //
        mode = ProxyChangeListener.MODE_DIRECT;
        Log.v(TAG, "-- Setting mode " + mode + " ==");

        // set the restrictions
        setProxyRestrictions(mode, null, null, null);

        // check configured proxy mode
        configuredMode = ProxyChangeListener.getmMdmProxyMode();
        assertEquals(mode + ": configuration", mode, configuredMode);

        // get the proxy config from ProxyChangeListener
        ProxyChangeListener.ProxyConfig pc = ProxyChangeListener.getMdmProxyConfig();
        assertNull(mode +": proxyConfig should be null", pc);

        checkValue("http.proxyHost", null);
        checkValue("http.proxyPort", null);
        checkValue("http.nonProxyHosts", null);
    }


    /**
     * Activate Proxy restriction
     * @param mode         Required. The Proxy mode we are to configure.
     * @param proxyServer  Required for MODE_FIXED_SERVERS, otherwise optional.
     * @param nonProxyList Optional for MODE_FIXED_SERVERS, otherwise optional.
     *                     This is a comma separated list of host patterns..
     * @param pacScriptUri Required for MODE_PAC_SCRIPT, otherwise optional.
     */
    private void setProxyRestrictions(String mode, String proxyServer,
                                      String nonProxyList, String pacScriptUri) {
        // Construct restriction bundle
        final Bundle restrictions = new Bundle();
        restrictions.putString(ProxyChangeListener.PROXY_MODE, mode);

        if (proxyServer  != null) {
            restrictions.putString(ProxyChangeListener.PROXY_SERVER, proxyServer);
        }
        if (nonProxyList != null) {
            restrictions.putString(ProxyChangeListener.PROXY_BYPASS_LIST, nonProxyList);
        }
        if (pacScriptUri != null) {
            restrictions.putString(ProxyChangeListener.PROXY_PAC_URL, pacScriptUri);
        }

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
}
