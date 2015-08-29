/*
 * Copyright (c) 2015, The Linux Foundation. All rights reserved.
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

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;


import org.codeaurora.swe.BrowserCommandLine;
import org.codeaurora.swe.Engine;
import org.codeaurora.swe.utils.Logger;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class UpdateNotificationService extends IntentService {
    private static final String LOGTAG = "UpdateNotificationService";
    private static final String ACTION_CHECK_UPDATES = BrowserConfig.AUTHORITY +
            ".action.check.update";
    public static final int DEFAULT_UPDATE_INTERVAL = 172800000; // two days
    public static final String UPDATE_SERVICE_PREF = "browser_update_service";
    public static final String UPDATE_JSON_VERSION_CODE = "versioncode";
    public static final String UPDATE_JSON_VERSION_STRING = "versionstring";
    public static final String UPDATE_JSON_MIN_INTERVAL = "interval";
    public static final String UPDATE_INTERVAL = "update_interval";
    public static final String UPDATE_VERSION_CODE = "version_code";
    public static final String UPDATE_VERSION = "update_version";
    public static final String UPDATE_URL = "update_url";
    public static final String UPDATE_TIMESTAMP = "update_timestamp";
    private static int NOTIFICATION_ID = 1000;
    private static boolean sIntentServiceInitialized = false;
    private static boolean sNotifyAlways = false;

    @Override
    public void onCreate() {
        super.onCreate();
        initEngine(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sIntentServiceInitialized)
            Engine.pauseTracing(this);
    }

    private static void initEngine(Context context) {
        if (!EngineInitializer.isInitialized()) {
            sIntentServiceInitialized = true;
            EngineInitializer.initializeSync((Context) context);
        }
    }

    public static void startActionUpdateNotificationService(Context context) {
        Intent intent = new Intent(context, UpdateNotificationService.class);
        intent.setAction(ACTION_CHECK_UPDATES);
        context.startService(intent);
    }

    public static String getFlavor(Context ctx) {
        String flavor = "";
        try {
            ApplicationInfo ai = ctx.getPackageManager().getApplicationInfo(
                    ctx.getPackageName(),PackageManager.GET_META_DATA);
            String compiler = (String) ai.metaData.get("Compiler");
            String arch = (String) ai.metaData.get("Architecture");
            flavor = "url-" + compiler + "-" + arch;
        } catch (Exception e) {
            Logger.e(LOGTAG, "getFlavor Exception : " + e.toString());
        }
        return flavor;
    }

    public static void updateCheck(Context context) {
        initEngine(context.getApplicationContext());
        if (!BrowserCommandLine.hasSwitch(BrowserSwitches.AUTO_UPDATE_SERVER_CMD)) {
            Logger.v(LOGTAG, "skip no command line: ");
            return;
        }
        long interval = getInterval(context);
        Long last_update_time = getLastUpdateTimestamp(context);
        if ((last_update_time +  interval) < System.currentTimeMillis()) {
            Logger.v(LOGTAG, "check for update now: ");
            startActionUpdateNotificationService(context);
        }
    }

    public static int getLatestVersionCode(Context ctx) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                UPDATE_SERVICE_PREF, Context.MODE_PRIVATE);
        return sharedPref.getInt(UPDATE_VERSION_CODE, 0);
    }

    public static String getLatestDownloadUrl(Context ctx) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                UPDATE_SERVICE_PREF, Context.MODE_PRIVATE);
        return sharedPref.getString(UPDATE_URL,"");
    }

    public static String getLatestVersion(Context ctx) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                UPDATE_SERVICE_PREF, Context.MODE_PRIVATE);
        return sharedPref.getString(UPDATE_VERSION, "");
    }

    private static long getLastUpdateTimestamp(Context ctx) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                UPDATE_SERVICE_PREF, Context.MODE_PRIVATE);
        return sharedPref.getLong(UPDATE_TIMESTAMP, 0);
    }

    private static int getInterval(Context ctx) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                UPDATE_SERVICE_PREF, Context.MODE_PRIVATE);
        return sharedPref.getInt(UPDATE_INTERVAL, DEFAULT_UPDATE_INTERVAL);
    }

    public UpdateNotificationService() {
        super("UpdateNotificationService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_CHECK_UPDATES.equals(action)) {
                handleUpdateCheck();
            }
        }
    }

    private void updateTimeStamp() {
        SharedPreferences sharedPref = getSharedPreferences(
                UPDATE_SERVICE_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(UPDATE_TIMESTAMP, System.currentTimeMillis());
        editor.commit();
    }

    private void persist(int versionCode, String url, String version, int interval) {
        SharedPreferences sharedPref = getSharedPreferences(
                UPDATE_SERVICE_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(UPDATE_VERSION_CODE, versionCode);
        editor.putInt(UPDATE_INTERVAL, interval);
        editor.putString(UPDATE_VERSION, version);
        editor.putString(UPDATE_URL, url);
        Logger.v(LOGTAG, "persist version code : " + versionCode);
        Logger.v(LOGTAG, "persist version : " + version);
        Logger.v(LOGTAG, "persist download url : " + url);
        editor.commit();
    }

    private void handleUpdateCheck() {
        String server_url = BrowserCommandLine.getSwitchValue(
                BrowserSwitches.AUTO_UPDATE_SERVER_CMD);
        int interval = DEFAULT_UPDATE_INTERVAL;
        InputStream stream = null;
        if (server_url != null && !server_url.isEmpty()) {
            try {
                URLConnection connection = new URL(server_url).openConnection();
                stream = connection.getInputStream();
                String result = readContents(stream);
                Logger.v(LOGTAG, "handleUpdateCheck result : " + result);
                JSONObject jsonResult = (JSONObject) new JSONTokener(result).nextValue();
                int versionCode = Integer.parseInt((String) jsonResult.get(UPDATE_JSON_VERSION_CODE));
                String url = (String) jsonResult.get(getFlavor(this));
                String version = (String) jsonResult.get(UPDATE_JSON_VERSION_STRING);
                if (jsonResult.has(UPDATE_JSON_MIN_INTERVAL))
                    interval = Integer.parseInt((String) jsonResult.get(UPDATE_JSON_MIN_INTERVAL));
                if (getCurrentVersionCode(this) < versionCode &&
                        (sNotifyAlways || getLatestVersionCode(this) != versionCode)) {
                    persist(versionCode, url, version, interval);
                    // notify only once per version change
                    showNotification(this, url, version);
                }
                stream.close();
            } catch (Exception e) {
                Logger.e(LOGTAG, "handleUpdateCheck Exception : " + e.toString());
            } finally {
                // always update the timestamp
                updateTimeStamp();
            }
        }
    }

    public static int getCurrentVersionCode(Context ctx) {
        PackageInfo pInfo = null;
        try {
            pInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Logger.e(LOGTAG, "getCurrentVersionCode Exception : " + e.toString());
        }
        return pInfo.versionCode;
    }

    private static void showNotification(Context ctx, String url, String version) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(ctx)
                        .setSmallIcon(R.drawable.img_notify_update_white)
                        .setContentTitle(ctx.getString(R.string.update))
                        .setContentText(ctx.getString(R.string.update_msg) + version);
        Intent resultIntent = new Intent(ctx, BrowserActivity.class);
        resultIntent.setAction(Intent.ACTION_VIEW);
        resultIntent.setData(Uri.parse(url));
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(ctx);
        stackBuilder.addParentStack(BrowserActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = builder.build();
        notification.flags = Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;
        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    private static void removeNotification(Context ctx) {
        NotificationManager mNotificationManager =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    private static String readContents(InputStream is) {
        String line = null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        try {
            line = reader.readLine();
            while (line != null) {
                line = line.replaceFirst("channel = ","");
                sb.append(line + "\n");
                line = reader.readLine();
            }
        } catch (Exception e) {
            Logger.e(LOGTAG, "convertStreamToString Exception : " + e.toString());
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                Logger.e(LOGTAG, "convertStreamToString Exception : " + e.toString());
            }
        }
        return sb.toString();
    }

}
