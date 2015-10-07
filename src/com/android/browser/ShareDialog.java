/*
    * Copyright (c) 2014, The Linux Foundation. All rights reserved.
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
package com.android.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import java.util.List;
import java.util.Collections;

import android.util.Log;

import org.chromium.base.ContentUriUtils;
import org.chromium.ui.UiUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ShareDialog extends AppItem {
    private Activity activity = null;
    public String title = null;
    public String url = null;
    public Bitmap favicon = null;
    public Bitmap screenshot = null;
    private List<ResolveInfo>apps = null;
    public final static String EXTRA_SHARE_SCREENSHOT = "share_screenshot";
    public final static String EXTRA_SHARE_FAVICON = "share_favicon";
    private static final String SCREENSHOT_DIRECTORY_NAME = "screenshot_share";
    private static final int MAX_SCREENSHOT_COUNT = 10;

    public ShareDialog (Activity activity, String title, String url, Bitmap favicon, Bitmap screenshot) {
        super(null);
        this.activity = activity;
        this.apps = getShareableApps();
        this.title = title;
        this.url = url;
        this.favicon = favicon;
        this.screenshot = screenshot;

        ContentUriUtils.setFileProviderUtil(new FileProviderHelper());
        trimScreenshots();
    }

    private void trimScreenshots() {
        try {
            File directory = getScreenshotDir();
            if (directory.list() != null && directory.list().length >= MAX_SCREENSHOT_COUNT) {
                clearSharedScreenshots();
            }
        } catch (IOException e) {
            e.printStackTrace();
            clearSharedScreenshots();
        }
    }

    private File getScreenshotDir() throws IOException {
        File baseDir = UiUtils.getDirectoryForImageCapture(activity);
        return new File(baseDir, SCREENSHOT_DIRECTORY_NAME);
    }

    private void deleteScreenshotFiles(File file) {
        if (!file.exists()) return;
        if (file.isDirectory()) {
            for (File f : file.listFiles()) deleteScreenshotFiles(f);
        }
    }

    /**
     * Clears all shared screenshot files.
     */
    private void clearSharedScreenshots() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    File dir = getScreenshotDir();
                    deleteScreenshotFiles(dir);
                } catch (IOException ie) {
                    // Ignore exception.
                }
            }
        });
    }

    private List<ResolveInfo> getShareableApps() {
        Intent shareIntent = new Intent("android.intent.action.SEND");
        shareIntent.setType("text/plain");
        PackageManager pm = activity.getPackageManager();
        List<ResolveInfo> launchables = pm.queryIntentActivities(shareIntent, 0);

        Collections.sort(launchables,
                     new ResolveInfo.DisplayNameComparator(pm));

        return launchables;
    }


    public List<ResolveInfo> getApps() {
        return apps;
    }

    public void loadView(final AppAdapter adapter) {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(activity);
        builderSingle.setIcon(R.mipmap.ic_launcher_browser);
        builderSingle.setTitle(activity.getString(R.string.choosertitle_sharevia));
        builderSingle.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int position) {
                dialog.dismiss();
                ResolveInfo launchable = adapter.getItem(position);
                ActivityInfo activityInfo = launchable.activityInfo;
                ComponentName name = new android.content.ComponentName(activityInfo.applicationInfo.packageName,
                                                     activityInfo.name);
                Intent i = new Intent(Intent.ACTION_SEND);
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                    // This flag clears the called app from the activity stack,
                    // so users arrive in the expected place next time this application is restarted
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                } else {
                    // flag used from Lollipop onwards
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                }

                i.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT |
                            Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, url);
                i.putExtra(Intent.EXTRA_SUBJECT, title);
                i.putExtra(EXTRA_SHARE_FAVICON, favicon);
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                i.putExtra(Intent.EXTRA_STREAM, getShareBitmapUri(screenshot));
                i.setComponent(name);
                activity.startActivity(i);
            }
        });

        builderSingle.show();
    }

    public Uri getShareBitmapUri(Bitmap screenshot) {
        Uri uri = null;
        if (screenshot != null) {
            FileOutputStream fOut = null;
            try {
                File path = getScreenshotDir();
                if (path.exists() || path.mkdir()) {
                    File saveFile = File.createTempFile(
                            String.valueOf(System.currentTimeMillis()), ".jpg", path);
                    fOut = new FileOutputStream(saveFile);
                    screenshot.compress(Bitmap.CompressFormat.JPEG, 90, fOut);
                    fOut.flush();
                    fOut.close();
                    uri = UiUtils.getUriForImageCaptureFile(activity, saveFile);
                }
            } catch (IOException ie) {
                if (fOut != null) {
                    try {
                        fOut.close();
                    } catch (IOException e) {
                        // Ignore exception.
                    }
                }
            }
        }
        return uri;
   }
}
