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
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;

import java.util.List;
import java.util.Collections;

import android.util.Log;

import org.chromium.base.ContentUriUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ShareDialog {
    private Activity activity = null;
    public String title = null;
    public String url = null;
    public Bitmap favicon = null;
    public Bitmap screenshot = null;
    public final static String EXTRA_SHARE_SCREENSHOT = "share_screenshot";
    public final static String EXTRA_SHARE_FAVICON = "share_favicon";
    private static final String SCREENSHOT_DIRECTORY_NAME = "screenshot_share";
    private static final int MAX_SCREENSHOT_COUNT = 10;
    public static final String EXTERNAL_IMAGE_FILE_PATH = "browser-images";
    public static final String IMAGE_FILE_PATH = "images";

    public ShareDialog (Activity activity, String title, String url, Bitmap favicon, Bitmap screenshot) {
        this.activity = activity;
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
        File baseDir = getDirectoryForImageCapture(activity);
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

    public Uri getShareBitmapUri(Bitmap screenshot) {
        Uri uri = null;
        if (screenshot != null) {
            FileOutputStream fOut = null;
            try {
                File path = getScreenshotDir();
                if (path.exists() || path.mkdir()) {
                    File saveFile = File.createTempFile(
                            String.valueOf(System.currentTimeMillis()), ".png", path);
                    fOut = new FileOutputStream(saveFile);
                    screenshot.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                    fOut.flush();
                    fOut.close();
                    uri = getUriForImageCaptureFile(activity, saveFile);
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

    public static Uri getUriForImageCaptureFile(Context context, File file) {
        return ContentUriUtils.getContentUriFromFile(context, file);
    }

    public static File getDirectoryForImageCapture(Context context) throws IOException {
        File path = new File(context.getFilesDir(), IMAGE_FILE_PATH);
        if (!path.exists() && !path.mkdir()) {
            throw new IOException("Folder cannot be created.");
        }
        return path;
    }

    public void sharePage() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT |
                Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_TEXT, url);
        intent.putExtra(Intent.EXTRA_SUBJECT, title);
        intent.putExtra(EXTRA_SHARE_FAVICON, favicon);
        intent.putExtra(Intent.EXTRA_STREAM, getShareBitmapUri(screenshot));

        activity.startActivity(Intent.createChooser(intent,
                activity.getString(R.string.choosertitle_sharevia)));
    }

}
