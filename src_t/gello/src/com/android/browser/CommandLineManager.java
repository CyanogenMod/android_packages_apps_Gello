/*
 *  Copyright (c) 2014 The Linux Foundation. All rights reserved.
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

import org.codeaurora.swe.utils.Logger;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;

public class CommandLineManager {

    private final static String LOGTAG = "CommandLineManager";
    public static final String COMMAND_LINE_FILE = "/data/local/tmp/swe-command-line";

    // Read from the InputStream object
    private static String parseCommandLine (InputStream is) {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                line = line.trim();

                int commentIndex = line.indexOf('#');
                if (commentIndex > -1) {
                    // trim out the comments
                    line = line.substring(0,commentIndex);
                }

                if (line.isEmpty())
                    continue;

                // Consider only the uncommented lines
                sb.append(line);
                sb.append(" ");
            }

            // Strip the browser name from the commandline options (First string) if options exists
            if (sb.indexOf("--") >= 0) {
                sb.delete(0, sb.indexOf(" "));
            }
        } catch (IOException e) {
            Logger.e(LOGTAG, "Exception:", e);
        } finally {
            try {
                // close the streams and reader
                is.close();
                if (br != null)
                    br.close();
            } catch (IOException e) {
                Logger.e(LOGTAG, "Exception:", e);
            }
        }
        return sb.toString();
    }

    // Set the browser options
    private static char[] append(InputStream inStreamDefaultCmdLine,
            InputStream inStreamUsrCmdLine) {
        // bail out since there no file to command line to deal with
        if (inStreamDefaultCmdLine == null && inStreamUsrCmdLine == null){
            return null;
        }

        String userArgs = "";
        String defaultArgs = "";
        if (inStreamDefaultCmdLine != null) {
            // Reading the default commandline file
            // Refers to  the internal filename residing in "raw" directory
            defaultArgs = parseCommandLine(inStreamDefaultCmdLine);
        }

        // Reading the user commandline file
        if (inStreamUsrCmdLine != null) {
            userArgs = parseCommandLine(inStreamUsrCmdLine);
        }

        // Assumption: The user commandline file can be empty or it exists
        //             with atleast one commandline option.
        // Insert the content of the default commandline file in the beginning of
        // the user commandline file content. So that, the user settings will get
        // the presidence
        userArgs = "Browser "+defaultArgs+" "+userArgs;


        Logger.v(LOGTAG, "(Command Line Arguments): " + userArgs);

        char[] buffer = userArgs.toString().toCharArray();
        return buffer;
    }

    public static char[] getCommandLineSwitches(Context context) {
        InputStream sweCmdLineStream =
                context.getResources().openRawResource(R.raw.swe_command_line);
        InputStream  usrCmdLineStream = null;
        File file = new File(COMMAND_LINE_FILE);
        if(file.exists()){
            try {
                usrCmdLineStream = new FileInputStream(COMMAND_LINE_FILE);
            } catch (FileNotFoundException e) {
            }
        }
        // Set the browser options here
        return append(sweCmdLineStream, usrCmdLineStream);
    }
}
