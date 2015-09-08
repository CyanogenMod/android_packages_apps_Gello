/*
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.android.browser;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.SystemClock;
import android.net.http.AndroidHttpClient;
import android.util.Log;
import android.os.FileObserver;
import android.os.Handler;

import org.codeaurora.swe.BrowserCommandLine;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;


import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Integer;
import java.lang.StringBuilder;
import java.lang.System;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Calendar;
import java.util.zip.GZIPOutputStream;

public class CrashLogExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final String CRASH_LOG_FILE = "crash.log";
    private static final String CRASH_LOG_MAX_FILE_SIZE_CMD = "crash-log-max-file-size";
    private static final String CRASH_REPORT_DIR = "Crash Reports";

    private final static String LOGTAG = "CrashLog";

    private Context mAppContext = null;

    private UncaughtExceptionHandler mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();

    private String mLogServer = new String();

    private boolean mOverrideHandler = false;

    private int mMaxLogFileSize = 1024 * 1024;
    // To avoid increasing startup time an upload delay is used
    private static final int UPLOAD_DELAY = 3000;

    private static FileObserver crashObserver;

    private final Handler mCrashReportHandler = new Handler();

    public CrashLogExceptionHandler(Context ctx) {
        mAppContext = ctx;
        if (BrowserCommandLine.hasSwitch(BrowserSwitches.CRASH_LOG_SERVER_CMD)) {
            initNativeReporter(ctx);
            mLogServer = BrowserCommandLine.getSwitchValue(BrowserSwitches.CRASH_LOG_SERVER_CMD);
            if (mLogServer != null) {
                uploadPastCrashLog();
                mOverrideHandler = true;
            }
        }

        try {
            int size = Integer.parseInt(
                                BrowserCommandLine.getSwitchValue(CRASH_LOG_MAX_FILE_SIZE_CMD,
                                Integer.toString(mMaxLogFileSize)));
            mMaxLogFileSize = size;
        } catch (NumberFormatException nfe) {
            Log.e(LOGTAG,"Max log file size is not configured properly. Using default: "
                  + mMaxLogFileSize);
        }

    }

    private void initNativeReporter(Context ctx){
        final File crashReports = new File(ctx.getCacheDir(),CRASH_REPORT_DIR);
        // On fresh installs, make the directory before registering an observer
        if (!crashReports.isDirectory()) {
            crashReports.mkdir();
        }
        // Implement FileObserver for crashReports that don't bring the system down
        crashObserver = new FileObserver(crashReports.getAbsolutePath()) {
            @Override
            public void onEvent(int event, String path){
                if ((event == FileObserver.CREATE) || (event == FileObserver.MOVED_TO)){
                    Log.w(LOGTAG, "A crash report was generated");
                    checkNativeCrash(crashReports);
                }
            }
        };
        // Native Crash reporting if commandline is set
        mCrashReportHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkNativeCrash(crashReports);
            }
        }, UPLOAD_DELAY);
        // start watching the crash reports folder
        crashObserver.startWatching();
    }

    private void saveCrashLog(String crashLog) {
        // Check if log file exists and it's current size
        try {
            File file = new File(mAppContext.getFilesDir(), CRASH_LOG_FILE);
            if (file.exists()) {
                if (file.length() > mMaxLogFileSize) {
                    Log.e(LOGTAG,"CRASH Log file size(" + file.length()
                          + ") exceeded max log file size("
                          + mMaxLogFileSize + ")");
                    return;
                }
            }
        } catch (NullPointerException npe) {
            Log.e(LOGTAG,"Exception while checking file size: " + npe);
        }

        FileOutputStream crashLogFile = null;
        try {
            crashLogFile = mAppContext.openFileOutput(CRASH_LOG_FILE, Context.MODE_APPEND);
            crashLogFile.write(crashLog.getBytes());
        } catch(IOException ioe) {
            Log.e(LOGTAG,"Exception while writing file: " + ioe);
        } finally {
            if (crashLogFile != null) {
                try {
                    crashLogFile.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    private void uploadPastCrashLog() {
        FileInputStream crashLogFile = null;
        BufferedReader reader = null;
        try {
            crashLogFile = mAppContext.openFileInput(CRASH_LOG_FILE);

            reader = new BufferedReader(new InputStreamReader(crashLogFile));
            StringBuilder crashLog = new StringBuilder();
            String line = reader.readLine();
            if (line != null) {
                crashLog.append(line);
            }

            // Typically there's only one line (JSON string) in the crash
            // log file. This loop would not be executed.
            while ((line = reader.readLine()) != null) {
                crashLog.append("\n").append(line);
            }

            uploadCrashLog(crashLog.toString(), UPLOAD_DELAY);
        } catch(FileNotFoundException fnfe) {
            Log.v(LOGTAG,"No previous crash found");
        } catch(IOException ioe) {
            Log.e(LOGTAG,"Exception while reading crash file: " + ioe);
        } finally {
            if (crashLogFile != null) {
                try {
                    crashLogFile.close();
                } catch (IOException ignore) {
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    private void uploadCrashLog(String data, int after) {
        final String crashLog = data;
        final int waitFor = after;
        new Thread(new Runnable() {
                public void run(){
                    try {
                        SystemClock.sleep(waitFor);
                        AndroidHttpClient httpClient = AndroidHttpClient.newInstance("Android");;
                        HttpPost httpPost = new HttpPost(mLogServer);
                        HttpEntity se = new StringEntity(crashLog);
                        httpPost.setEntity(se);
                        HttpResponse response = httpClient.execute(httpPost);

                        File crashLogFile = new File(mAppContext.getFilesDir(),
                                                     CRASH_LOG_FILE);
                        if (crashLogFile != null) {
                            crashLogFile.delete();
                        } else {
                            Log.e(LOGTAG,"crash log file could not be opened for deletion");
                        }
                    } catch (ClientProtocolException pe) {
                        Log.e(LOGTAG,"Exception while sending http post: " + pe);
                    } catch (IOException ioe1) {
                        Log.e(LOGTAG,"Exception while sending http post: " + ioe1);
                    }
                }
            }).start();
    }

    public void uncaughtException(Thread t, Throwable e) {
        if (!mOverrideHandler) {
            mDefaultHandler.uncaughtException(t, e);
            return;
        }

        String crashLog = new String();

        try {
            Calendar calendar = Calendar.getInstance();
            JSONObject jsonBackTraceObj = new JSONObject();
            String date = calendar.getTime().toString();
            String aboutSWE = mAppContext.getResources().getString(R.string.about_text);
            String sweVer = findValueFromAboutText(aboutSWE, "Version: ");
            String sweHash = findValueFromAboutText(aboutSWE, "Hash: ");
            String sweBuildDate = findValueFromAboutText(aboutSWE, "Built: ");

            jsonBackTraceObj.put("date", date);
            jsonBackTraceObj.put("android-model", android.os.Build.MODEL);
            jsonBackTraceObj.put("android-device", android.os.Build.DEVICE);
            jsonBackTraceObj.put("android-ver", android.os.Build.VERSION.RELEASE);
            jsonBackTraceObj.put("browser-ver", sweVer);
            jsonBackTraceObj.put("browser-hash", sweHash);
            jsonBackTraceObj.put("browser-build-date", sweBuildDate);
            jsonBackTraceObj.put("thread", t.toString());
            jsonBackTraceObj.put("format", "crashmon-1");
            jsonBackTraceObj.put("monkey-test", ActivityManager.isUserAMonkey());

            JSONArray jsonStackArray = new JSONArray();

            Throwable throwable = e;
            String stackTag = "Exception thrown while running";
            while (throwable != null) {
                JSONObject jsonStackObj = new JSONObject();
                StackTraceElement[] arr = throwable.getStackTrace();
                JSONArray jsonStack = new JSONArray(arr);

                jsonStackObj.put("cause", throwable.getCause());
                jsonStackObj.put("message", throwable.getMessage());
                jsonStackObj.put(stackTag, jsonStack);

                jsonStackArray.put(jsonStackObj);

                stackTag = "stack";
                throwable = throwable.getCause();
            }
            jsonBackTraceObj.put("exceptions", jsonStackArray);

            JSONObject jsonMainObj = new JSONObject();
            jsonMainObj.put("backtraces", jsonBackTraceObj);

            Log.e(LOGTAG, "Exception: " + jsonMainObj.toString(4));
            crashLog = jsonMainObj.toString();

        } catch (JSONException je) {
            Log.e(LOGTAG, "Failed in JSON encoding: " + je);
        }

        saveCrashLog(crashLog);

        uploadCrashLog(crashLog, 0);

        mDefaultHandler.uncaughtException(t, e);
    }

    private String findValueFromAboutText(String aboutText, String aboutKey) {
        int start = aboutText.indexOf(aboutKey);
        int end = aboutText.indexOf("\n", start);
        String value = "";

        if (start != -1 && end != -1) {
            start += aboutKey.length();
            value = aboutText.substring(start, end);
        }
        return value;
    }

    private void checkNativeCrash(final File crashReportsDir) {
        // Search cache/Crash Reports/ for any crashes
        if (crashReportsDir.exists()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (File f : crashReportsDir.listFiles()) {
                        uploadNativeCrashReport(f);
                    }
                }
            }).start();
        }
    }

    private void uploadNativeCrashReport(final File report) {
        Log.w(LOGTAG, "Preparing Crash Report for upload " + report.getName());
        // get server url from commandline
        String server = BrowserCommandLine.getSwitchValue(BrowserSwitches.CRASH_LOG_SERVER_CMD);
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(server);

            // Compress the data
            ByteArrayOutputStream arrayStream = new ByteArrayOutputStream();
            OutputStream gzipData = new GZIPOutputStream(arrayStream);
            InputStream inputStream = new FileInputStream(report);
            long length = report.length();
            byte[] data = new byte[(int)length];

            // Read in the bytes
            int offset = 0;
            int numRead = 0;
            while (offset < data.length
                   && (numRead=inputStream.read(data, offset, data.length-offset)) >= 0) {
                offset += numRead;
            }
            gzipData.write(data);
            gzipData.close();

            AbstractHttpEntity entity = new ByteArrayEntity(arrayStream.toByteArray());

            // Send the report as a compressed Binary
            entity.setContentType("binary/octet-stream");
            entity.setContentEncoding("gzip");
            entity.setChunked(false);
            httpPost.setEntity(entity);

            HttpResponse response = httpClient.execute(httpPost);
            int status = response.getStatusLine().getStatusCode();
            if (status == 200)
                report.delete();
            else Log.w(LOGTAG, "Upload Failure. Will try again next time- " + status);

        } catch (Exception e) {
            Log.w(LOGTAG, "Crash Report failed to upload, will try again next time " + e);
        }
    }

}
