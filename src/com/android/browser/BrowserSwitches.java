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

package com.android.browser;

// Contains all of the command line switches that are specific to the SWE Browser

public class BrowserSwitches {
    //Command line flag for strict mode
    public final static String STRICT_MODE = "enable-strict-mode";

    // Command line flag for single-process mode.
    // Must match the value of kSingleProcess in content_switches.cc
    public static final String SINGLE_PROCESS = "single-process";

    //SWE TODO : Add description for each switch.

    public static final String OVERRIDE_USER_AGENT = "user-agent";

    public static final String OVERRIDE_MEDIA_DOWNLOAD = "media-download";

    public static final String HTTP_HEADERS = "http-headers";

    public static final String ENABLE_SWE = "enabled-swe";

    public static final String DISABLE_TOP_CONTROLS = "disable-top-controls";

    public static final String TOP_CONTROLS_HIDE_THRESHOLD = "top-controls-hide-threshold";

    public static final String TOP_CONTROLS_SHOW_THRESHOLD = "top-controls-show-threshold";

    public static final String CRASH_LOG_SERVER_CMD = "crash-log-server";

    public static final String CMD_LINE_SWITCH_FEEDBACK = "mail-feedback-to";

    public static final String CMD_LINE_SWITCH_HELPURL = "help-url";

    public static final String CMD_LINE_SWITCH_EULA_URL = "legal-eula-url";

    public static final String CMD_LINE_SWITCH_PRIVACY_POLICY_URL = "legal-privacy-policy-url";

    public static final String AUTO_UPDATE_SERVER_CMD = "auto-update-server";

}
