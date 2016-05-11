/*
 * Copyright (c) 2013,2014, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *         copyright notice, this list of conditions and the following
 *         disclaimer in the documentation and/or other materials provided
 *         with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *         contributors may be used to endorse or promote products derived
 *         from this software without specific prior written permission.
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

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import java.lang.Thread;

import com.android.browser.R;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.*;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.view.Window;
import android.widget.Toast;
import android.webkit.MimeTypeMap;
import android.text.TextUtils;
import android.content.pm.PackageManager;
import android.Manifest.permission;
import android.content.Context;

import com.android.browser.reflect.ReflectHelper;

public class DownloadSettings extends Activity {

    private EditText downloadFilenameET;
    private EditText downloadPathET;
    private TextView downloadEstimateSize;
    private TextView downloadEstimateTime;
    private ImageButton downloadStart;
    private ImageButton downloadCancel;
    private String url;
    private String userAgent;
    private String contentDisposition;
    private String mimetype;
    private String referer;
    private String authorization;
    private String filenameBase;
    private String filename;
    private String filenameExtension;
    private boolean privateBrowsing;
    private long contentLength;
    private String downloadPath;
    private String downloadPathForUser;
    private static final int downloadRate = (1024 * 100 * 60);// Download Rate
                                                              // 100KB/s
    private final static String LOGTAG = "DownloadSettings";
    private final static int DOWNLOAD_PATH = 0;
    private boolean isDownloadStarted = false;

    private static final String ENV_EMULATED_STORAGE_TARGET = "EMULATED_STORAGE_TARGET";
    private static final String APK_TYPE="apk";
    private static final String OCTET_STREAM = "application/octet-stream";

    private int nextRequestCode = 2000;

    protected void onCreate(Bundle savedInstanceState) {
        if (!EngineInitializer.isInitialized()) {
            Log.e(LOGTAG, "Engine not Initialized");
            EngineInitializer.initializeSync((Context) getApplicationContext());
        }
        super.onCreate(savedInstanceState);
        // initial the DownloadSettings view
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.download_settings);
        downloadFilenameET = (EditText) findViewById(R.id.download_filename_edit);
        downloadPathET = (EditText) findViewById(R.id.download_filepath_selected);
        downloadEstimateSize = (TextView) findViewById(R.id.download_estimate_size_content);
        downloadEstimateTime = (TextView) findViewById(R.id.download_estimate_time_content);
        downloadStart = (ImageButton) findViewById(R.id.download_start);
        downloadCancel = (ImageButton) findViewById(R.id.download_cancel);
        downloadPathET.setOnClickListener(downloadPathListener);
        downloadStart.setOnClickListener(downloadStartListener);
        downloadCancel.setOnClickListener(downloadCancelListener);

        // get the bundle from Intent
        Intent intent = getIntent();
        Bundle fileInfo = intent.getExtras();
        url = fileInfo.getString("url");
        userAgent = fileInfo.getString("userAgent");
        contentDisposition = fileInfo.getString("contentDisposition");
        mimetype = fileInfo.getString("mimetype");
        referer = fileInfo.getString("referer");
        authorization = fileInfo.getString("authorization");
        contentLength = fileInfo.getLong("contentLength");
        privateBrowsing = fileInfo.getBoolean("privateBrowsing");
        filename = fileInfo.getString("filename");

        // download filenamebase's length is depended on filenameLength's values
        // if filenamebase.length >= flienameLength, destroy the last string!

        filenameBase = DownloadHandler.getFilenameBase(filename);
        if (filenameBase.length() >= (BrowserUtils.FILENAME_MAX_LENGTH)) {
            filenameBase = filenameBase.substring(0, BrowserUtils.FILENAME_MAX_LENGTH);
        }

        // warring when user enter more over letters into the EditText
        BrowserUtils.maxLengthFilter(DownloadSettings.this, downloadFilenameET,
                BrowserUtils.FILENAME_MAX_LENGTH);

        downloadFilenameET.setText(filenameBase);

        String filenameExtension = DownloadHandler.getFilenameExtension(filename);

        // introspect for octet stream mimetype what type of file extension it has
        // and reassign mimetype
        if (mimetype == null  || mimetype.isEmpty()  || mimetype.equals(OCTET_STREAM)) {

            String updatedFileName = filenameBase + "." + filenameExtension;
            Object[] params = {updatedFileName};
            Class[] type = new Class[] {String.class};
            mimetype = (String) ReflectHelper.invokeMethod("android.media.MediaFile",
                                            "getMimeTypeForFile", type, params);
        }

        //Add special check for .apk files with octet-stream mimetype
        if (filenameExtension.equals(APK_TYPE) && mimetype != null && mimetype.equals(OCTET_STREAM)) {
            mimetype =  "application/vnd.android.package-archive";
        }

        // last way to fetch for mimetype if its still null
        if (mimetype  == null  || mimetype.isEmpty())
            mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(filenameExtension);

        downloadPath = chooseFolderFromMimeType(BrowserSettings.getInstance().getDownloadPath(),
                mimetype);
        downloadPathForUser = DownloadHandler.getDownloadPathForUser(DownloadSettings.this,
                downloadPath);

        autoupdateFileName(filenameBase, DownloadHandler.getFilenameExtension(filename), downloadPath);
        setDownloadPathForUserText(downloadPathForUser);
        setDownloadFileSizeText();
        setDownloadFileTimeText();
    }

    private void autoupdateFileName(String filenameBase, String  extension, String downloadPath) {
        String fullPath =  downloadPath + "/" + filenameBase + "." + extension;
        int count = 1;
        String newFilenameBase = "";

        while(new File(fullPath).exists()) {
            newFilenameBase = filenameBase+"-"+count++;
            fullPath =  downloadPath + "/" + newFilenameBase + "." + extension;
        }

        if(!TextUtils.isEmpty(newFilenameBase)) {
            filenameBase = newFilenameBase;
        }

        downloadFilenameET.setText(filenameBase);
    }

    private OnClickListener downloadPathListener = new OnClickListener() {

        @Override
        public void onClick(View v) {

            final String filemanagerIntent =
                getResources().getString(R.string.def_intent_file_manager);
            if (!TextUtils.isEmpty(filemanagerIntent)) {
                // start filemanager for getting download path
                try {
                    Intent downloadPathIntent = new Intent(filemanagerIntent);
                    DownloadSettings.this.startActivityForResult(downloadPathIntent, DOWNLOAD_PATH);
                } catch (Exception e) {
                    String err_msg = getString(R.string.activity_not_found,
                            filemanagerIntent);
                    Toast.makeText(DownloadSettings.this, err_msg, Toast.LENGTH_LONG).show();
                }
            } else {
                Log.e(LOGTAG, "File Manager intent not defined !!");
            }

        }
    };

    private OnClickListener downloadStartListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            onHandleDownload();
        }
    };

    private void onHandleDownload() {

        filenameBase = getFilenameBaseFromUserEnter();

        // check the filename user enter is null or not
        if (TextUtils.isEmpty(filenameBase) || TextUtils.isEmpty(downloadPath)) {
            DownloadHandler.showFilenameEmptyDialog(DownloadSettings.this);
            return;
        }

        // check for permission
        if (!hasPermission(permission.WRITE_EXTERNAL_STORAGE)) {
            requestPermissions(new String[] {permission.WRITE_EXTERNAL_STORAGE},
                ++nextRequestCode);
            return;
        }

        filenameExtension = DownloadHandler.getFilenameExtension(filename);
        filename = filenameBase + "." + filenameExtension;

        // check the storage status
        if (!DownloadHandler.isStorageStatusOK(DownloadSettings.this, filename, downloadPath)) {
            return;
        }

        // check the storage memory enough or not
        try {
            DownloadHandler.setAppointedFolder(downloadPath);
        } catch (Exception e) {
            DownloadHandler.showNoEnoughMemoryDialog(DownloadSettings.this);
            return;
        }
        boolean isNoEnoughMemory = DownloadHandler.manageNoEnoughMemory(contentLength,
                downloadPath);
        if (isNoEnoughMemory) {
            DownloadHandler.showNoEnoughMemoryDialog(DownloadSettings.this);
            return;
        }

        // check the download file is exist or not
        String fullFilename = downloadPath + "/" + filename;
        if (mimetype != null && new File(fullFilename).exists()) {
            DownloadHandler.fileExistQueryDialog(DownloadSettings.this);
            return;
        }

        download();

    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
        String[] permissions, int[] grantResults) {
        if (nextRequestCode == requestCode) {
            if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onHandleDownload();
            } else {
                finish();
            }
        }
    }

    private void download() {
        // staring downloading
        DownloadHandler.startingDownload(DownloadSettings.this,
                url, userAgent, contentDisposition,
                mimetype, referer, authorization,
                privateBrowsing, contentLength,
                Uri.encode(filename), downloadPath);
        isDownloadStarted = true;
    }

    private boolean hasPermission(String permission) {
        return (checkCallingOrSelfPermission(permission)
            == PackageManager.PERMISSION_GRANTED);
    }

    private OnClickListener downloadCancelListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };

    protected void onDestroy() {
        super.onDestroy();
    }

    protected void onPause() {
        super.onPause();
        if (isDownloadStarted) {
            finish();
        }
    }

    protected void onResume() {
        super.onResume();
        if (isDownloadStarted) {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (DOWNLOAD_PATH == requestCode) {
            if (resultCode == Activity.RESULT_OK && intent != null) {
                final String result_dir_sel =
                    getResources().getString(R.string.def_file_manager_result_dir);
                downloadPath = intent.getStringExtra(result_dir_sel);
                // Fallback logic to stock browser
                if (downloadPath == null) {
                    Uri uri = intent.getData();
                    if(uri != null)
                        downloadPath = uri.getPath();
                }
                if (downloadPath != null) {
                    String rawEmulatedStorageTarget = System.getenv(ENV_EMULATED_STORAGE_TARGET);
                    if (!TextUtils.isEmpty(rawEmulatedStorageTarget)) {
                        if (downloadPath.startsWith("/storage/sdcard0"))
                            downloadPath = downloadPath.replace("/storage/sdcard0",
                                    "/storage/emulated/0");
                        if (downloadPath.startsWith("/storage/emulated/legacy"))
                            downloadPath = downloadPath.replace("/storage/emulated/legacy",
                                    "/storage/emulated/0");
                    }
                    downloadPathForUser = DownloadHandler.getDownloadPathForUser(
                            DownloadSettings.this, downloadPath);
                    setDownloadPathForUserText(downloadPathForUser);
                }
            }
        }
    }

    // Add for carrier feature - download to related folders by mimetype.
    private static String chooseFolderFromMimeType(String path, String mimeType) {
        String destinationFolder = null;
        if (!path.contains(Environment.DIRECTORY_DOWNLOADS) || null == mimeType)
            return path;
        if (mimeType.startsWith("audio"))
            destinationFolder = Environment.DIRECTORY_MUSIC;
        else if (mimeType.startsWith("video"))
            destinationFolder = Environment.DIRECTORY_MOVIES;
        else if (mimeType.startsWith("image"))
            destinationFolder = Environment.DIRECTORY_PICTURES;
        if (null != destinationFolder)
            path = path.replace(Environment.DIRECTORY_DOWNLOADS, destinationFolder);
        return path;
    }

    /**
     * show download path for user
     *
     * @param downloadPath the download path user can see
     */
    private void setDownloadPathForUserText(String downloadPathForUser) {
        downloadPathET.setText(downloadPathForUser);
    }

    /**
     * get the filename from user select the download path
     *
     * @return String the filename from user selected
     */
    private String getFilenameBaseFromUserEnter() {
        return downloadFilenameET.getText().toString();
    }

    /**
     * set the download file size for user to be known
     */
    private void setDownloadFileSizeText() {
        String sizeText;
        if (contentLength <= 0) {
            sizeText = getString(R.string.unknow_length);
        } else {
            sizeText = getDownloadFileSize();
        }
        downloadEstimateSize.setText(sizeText);

    }

    /**
     * set the time which downloaded this file will be estimately use;
     */
    private void setDownloadFileTimeText() {
        String neededTimeText;
        if (contentLength <= 0) {
            neededTimeText = getString(R.string.unknow_length);
        } else {
            neededTimeText = getNeededTime() + getString(R.string.time_min);
        }
        downloadEstimateTime.setText(neededTimeText);
    }

    /**
     * count the download file's size and format the values
     *
     * @return String the format values
     */
    private String getDownloadFileSize() {
        String currentSizeText = "";
        if (contentLength > 0) {
            currentSizeText = Formatter.formatFileSize(DownloadSettings.this, contentLength);
        }
        return currentSizeText;
    }

    /**
     * get the time download this file will be use,and format this time values
     *
     * @return long the valses of time which download this file will be use
     */
    private long getNeededTime() {
        long timeNeeded = contentLength / downloadRate;
        if (timeNeeded < 1) {
            timeNeeded = 1;
        }
        Log.e(LOGTAG, "TimeNeeded:" + timeNeeded + "min");
        // return the time like 5 min, not 5 s;
        return timeNeeded;
    }
}
