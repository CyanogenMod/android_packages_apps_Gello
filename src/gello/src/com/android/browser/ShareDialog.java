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
import android.os.Build;

import java.util.List;
import java.util.Collections;

import android.util.Log;


public class ShareDialog extends AppItem {
    private Activity activity = null;
    public String title = null;
    public String url = null;
    public Bitmap favicon = null;
    public Bitmap screenshot = null;
    private List<ResolveInfo>apps = null;
    public final static String EXTRA_SHARE_SCREENSHOT = "share_screenshot";
    public final static String EXTRA_SHARE_FAVICON = "share_favicon";


    public ShareDialog (Activity activity, String title, String url, Bitmap favicon, Bitmap screenshot) {
        super(null);
        this.activity = activity;
        this.apps = getShareableApps();
        this.title = title;
        this.url = url;
        this.favicon = favicon;
        this.screenshot = screenshot;
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
                i.putExtra(EXTRA_SHARE_SCREENSHOT, screenshot);
                i.setComponent(name);
                activity.startActivity(i);
            }
        });

        builderSingle.show();
    }
}
