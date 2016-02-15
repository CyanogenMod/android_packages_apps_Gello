/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Log;
import org.codeaurora.swe.CookieManager;
import org.w3c.dom.Text;

import android.webkit.URLUtil;
import android.widget.Toast;

import com.android.browser.R;
import com.android.browser.mdm.DownloadDirRestriction;
import com.android.browser.platformsupport.WebAddress;
import com.android.browser.reflect.ReflectHelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.File;
/**
 * Handle download requests
 */
public class DownloadHandler {

    private static final boolean LOGD_ENABLED =
            com.android.browser.Browser.LOGD_ENABLED;

    private static final String LOGTAG = "DLHandler";
    private static String mInternalStorage;
    private static String mExternalStorage;
    private final static String INVALID_PATH = "/storage";

    public static void startingDownload(Activity activity,
            String url, String userAgent, String contentDisposition,
            String mimetype, String referer, String auth,
            boolean privateBrowsing, long contentLength,
            String filename, String downloadPath) {
        // java.net.URI is a lot stricter than KURL so we have to encode some
        // extra characters. Fix for b 2538060 and b 1634719
        WebAddress webAddress;
        try {
            webAddress = new WebAddress(url);
            webAddress.setPath(encodePath(webAddress.getPath()));
        } catch (Exception e) {
            // This only happens for very bad urls, we want to chatch the
            // exception here
            Log.e(LOGTAG, "Exception trying to parse url:" + url);
            return;
        }

        String addressString = webAddress.toString();
        Uri uri = Uri.parse(addressString);
        final DownloadManager.Request request;
        try {
            request = new DownloadManager.Request(uri);
        } catch (IllegalArgumentException e) {
            Toast.makeText(activity, R.string.cannot_download, Toast.LENGTH_SHORT).show();
            return;
        }
        request.setMimeType(mimetype);
        // set downloaded file destination to /sdcard/Download.
        // or, should it be set to one of several Environment.DIRECTORY* dirs
        // depending on mimetype?
        try {
            setDestinationDir(downloadPath, filename, request);
        } catch (Exception e) {
            showNoEnoughMemoryDialog(activity);
            return;
        }
        // let this downloaded file be scanned by MediaScanner - so that it can
        // show up in Gallery app, for example.
        request.allowScanningByMediaScanner();
        request.setDescription(webAddress.getHost());
        // XXX: Have to use the old url since the cookies were stored using the
        // old percent-encoded url.

        String cookies = CookieManager.getInstance().getCookie(url, privateBrowsing);
        request.addRequestHeader("cookie", cookies);
        request.addRequestHeader("User-Agent", userAgent);
        request.addRequestHeader("Referer", referer);
        request.setVisibleInDownloadsUi(!privateBrowsing);
        if (!TextUtils.isEmpty(auth)) {
            request.addRequestHeader("Authorization", auth);
        }
        request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        final DownloadManager manager = (DownloadManager) activity
                .getSystemService(Context.DOWNLOAD_SERVICE);
        new Thread("Browser download") {
            public void run() {
                try {
                    manager.enqueue(request);
                } catch (Exception e) {
                     Log.w("DLHandler", "Could not enqueue the download", e);
                }
            }
        }.start();
        showStartDownloadToast(activity, privateBrowsing);
    }

    private static boolean isAudioFileType(int fileType){
        Object[] params  = {Integer.valueOf(fileType)};
        Class[] type = new Class[] {int.class};
        Boolean result = (Boolean) ReflectHelper.invokeMethod("android.media.MediaFile",
                               "isAudioFileType", type, params);
        return result;
    }

    private static boolean isVideoFileType(int fileType){
        Object[] params  = {Integer.valueOf(fileType)};
        Class[] type = new Class[] {int.class};
        Boolean result = (Boolean) ReflectHelper.invokeMethod("android.media.MediaFile",
                             "isVideoFileType", type, params);
        return result;
    }

    /**
     * Notify the host application a download should be done, or that
     * the data should be streamed if a streaming viewer is available.
     * @param activity Activity requesting the download.
     * @param url The full url to the content that should be downloaded
     * @param userAgent User agent of the downloading application.
     * @param contentDisposition Content-disposition http header, if present.
     * @param mimetype The mimetype of the content reported by the server
     * @param referer The referer associated with the downloaded url
     * @param privateBrowsing If the request is coming from a private browsing tab.
     */
    public static boolean onDownloadStart(final Activity activity, final String url,
            final String userAgent, final String contentDisposition, final String mimetype,
            final String referer, final String auth, final boolean privateBrowsing,
            final long contentLength) {
        // if we're dealing wih A/V content that's not explicitly marked
        //     for download, check if it's streamable.
        if (contentDisposition == null
                || !contentDisposition.regionMatches(
                        true, 0, "attachment", 0, 10)) {
            // Add for Carrier Feature - When open an audio/video link, prompt a dialog
            // to let the user choose play or download operation.
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();
            Log.v(LOGTAG, "scheme:" + scheme + ", mimetype:" + mimetype);
            // Some mimetype for audio/video files is not started with "audio" or "video",
            // such as ogg audio file with mimetype "application/ogg". So we also check
            // file type by MediaFile.isAudioFileType() and MediaFile.isVideoFileType().
            // For those file types other than audio or video, download it immediately.
            Object[] params = {mimetype};
            Class[] type = new Class[] {String.class};
            Integer result = (Integer) ReflectHelper.invokeMethod("android.media.MediaFile",
                                           "getFileTypeForMimeType", type, params);
            int fileType = result.intValue();
            if ("http".equalsIgnoreCase(scheme) &&
                    (mimetype.startsWith("audio/") ||
                        mimetype.startsWith("video/") ||
                            isAudioFileType(fileType) ||
                                isVideoFileType(fileType))) {
                new AlertDialog.Builder(activity)
                .setTitle(R.string.application_name)
                .setIcon(R.drawable.default_video_poster)
                .setMessage(R.string.http_video_msg)
                .setPositiveButton(R.string.video_save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        onDownloadStartNoStream(activity, url, userAgent, contentDisposition,
                                mimetype, referer, auth, privateBrowsing, contentLength);
                    }
                 })
                .setNegativeButton(R.string.video_play, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse(url), mimetype);
                        try {
                            String trimmedcontentDisposition = trimContentDisposition(contentDisposition);
                            String title = URLUtil.guessFileName(url, trimmedcontentDisposition, mimetype);
                            intent.putExtra(Intent.EXTRA_TITLE, title);
                            activity.startActivity(intent);
                        } catch (ActivityNotFoundException ex) {
                            Log.w(LOGTAG, "When http stream play, activity not found for "
                                    + mimetype + " over " + Uri.parse(url).getScheme(), ex);
                        }
                    }
                }).show();

                return true;
            }
            // query the package manager to see if there's a registered handler
            //     that matches.
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), mimetype);
            ResolveInfo info = activity.getPackageManager().resolveActivity(intent,
                    PackageManager.MATCH_DEFAULT_ONLY);
            if (info != null) {
                ComponentName myName = activity.getComponentName();
                // If we resolved to ourselves, we don't want to attempt to
                // load the url only to try and download it again.
                if (!myName.getPackageName().equals(
                        info.activityInfo.packageName)
                        || !myName.getClassName().equals(
                                info.activityInfo.name)) {
                    // someone (other than us) knows how to handle this mime
                    // type with this scheme, don't download.
                    try {
                        activity.startActivity(intent);
                        return false;
                    } catch (ActivityNotFoundException ex) {
                        if (LOGD_ENABLED) {
                            Log.d(LOGTAG, "activity not found for " + mimetype
                                    + " over " + Uri.parse(url).getScheme(),
                                    ex);
                        }
                        // Best behavior is to fall back to a download in this
                        // case
                    }
                }
            }
        }
        onDownloadStartNoStream(activity, url, userAgent, contentDisposition,
                mimetype, referer, auth, privateBrowsing, contentLength);
        return false;
    }

    // This is to work around the fact that java.net.URI throws Exceptions
    // instead of just encoding URL's properly
    // Helper method for onDownloadStartNoStream
    private static String encodePath(String path) {
        char[] chars = path.toCharArray();

        boolean needed = false;
        for (char c : chars) {
            if (c == '[' || c == ']' || c == '|') {
                needed = true;
                break;
            }
        }
        if (needed == false) {
            return path;
        }

        StringBuilder sb = new StringBuilder("");
        for (char c : chars) {
            if (c == '[' || c == ']' || c == '|') {
                sb.append('%');
                sb.append(Integer.toHexString(c));
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * Notify the host application a download should be done, even if there
     * is a streaming viewer available for thise type.
     * @param activity Activity requesting the download.
     * @param url The full url to the content that should be downloaded
     * @param userAgent User agent of the downloading application.
     * @param contentDisposition Content-disposition http header, if present.
     * @param mimetype The mimetype of the content reported by the server
     * @param referer The referer associated with the downloaded url
     * @param privateBrowsing If the request is coming from a private browsing tab.
     */
    /* package */static void onDownloadStartNoStream(Activity activity,
            String url, String userAgent, String contentDisposition,
            String mimetype, String referer, String auth,
            boolean privateBrowsing, long contentLength) {

        contentDisposition = trimContentDisposition(contentDisposition);

        String filename = URLUtil.guessFileName(url,
                contentDisposition, mimetype);

        // Check to see if we have an SDCard
        String status = Environment.getExternalStorageState();
        if (!status.equals(Environment.MEDIA_MOUNTED)) {
            int title;
            String msg;

            // Check to see if the SDCard is busy, same as the music app
            if (status.equals(Environment.MEDIA_SHARED)) {
                msg = activity.getString(R.string.download_sdcard_busy_dlg_msg);
                title = R.string.download_sdcard_busy_dlg_title;
            } else {
                msg = activity.getString(R.string.download_no_sdcard_dlg_msg, filename);
                title = R.string.download_no_sdcard_dlg_title;
            }

            new AlertDialog.Builder(activity)
                .setTitle(title)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(msg)
                .setPositiveButton(R.string.ok, null)
                .show();
            return;
        }

        if (mimetype == null) {
            // We must have long pressed on a link or image to download it. We
            // are not sure of the mimetype in this case, so do a head request
            new FetchUrlMimeType(activity, url, userAgent, referer, auth,
                    privateBrowsing, filename).start();
        } else {
            if (DownloadDirRestriction.getInstance().downloadsAllowed()) {
                startDownloadSettings(activity, url, userAgent, contentDisposition, mimetype, referer,
                        auth, privateBrowsing, contentLength, filename);
            }
            else {
                Toast.makeText(activity, R.string.managed_by_your_administrator, Toast.LENGTH_SHORT)
                .show();
            }
        }

    }

    static String trimContentDisposition(String contentDisposition) {
        final Pattern CONTENT_DISPOSITION_PATTERN =
            Pattern.compile("filename\\s*=\\s*(\"?)([^\";]*)\\1\\s*",
                Pattern.CASE_INSENSITIVE);

        if (contentDisposition != null) {

            try {
                Matcher m = CONTENT_DISPOSITION_PATTERN.matcher(contentDisposition);
                if (m.find()) {
                    contentDisposition = "attachment; filename="+m.group(2);
                }

                return contentDisposition;
            } catch (IllegalStateException ex) {
                // This function is defined as returning null when it can't parse the header
            }
        }
        return null;
    }

    public static void initStorageDefaultPath(Context context, String downloadPath) {
        mExternalStorage = getExternalStorageDirectory(context, downloadPath);
        if (isPhoneStorageSupported()) {
            mInternalStorage = Environment.getExternalStorageDirectory().getPath();
        } else {
            mInternalStorage = null;
        }
    }

    public static void startDownloadSettings(Activity activity,
            String url, String userAgent, String contentDisposition,
            String mimetype, String referer, String auth,
            boolean privateBrowsing, long contentLength,
            String filename) {
        Bundle fileInfo = new Bundle();
        fileInfo.putString("url", url);
        fileInfo.putString("userAgent", userAgent);
        fileInfo.putString("contentDisposition", contentDisposition);
        fileInfo.putString("mimetype", mimetype);
        fileInfo.putString("referer", referer);
        fileInfo.putString("authorization", auth);
        fileInfo.putLong("contentLength", contentLength);
        fileInfo.putBoolean("privateBrowsing", privateBrowsing);
        fileInfo.putString("filename", filename);
        Intent intent = new Intent("android.intent.action.BROWSERDOWNLOAD");

        // Since there could be multiple browsers capable of handling
        //  the same intent we assure that the same package handles it
        intent.setPackage(activity.getPackageName());

        intent.putExtras(fileInfo);
        activity.startActivity(intent);
    }

    public static void setAppointedFolder(String downloadPath) {
        File file = new File(downloadPath);
        if (file.exists()) {
            if (!file.isDirectory()) {
                throw new IllegalStateException(file.getAbsolutePath() +
                        " already exists and is not a directory");
            }
        } else {
            if (!file.mkdir()) {
                throw new IllegalStateException("Unable to create directory: " +
                        file.getAbsolutePath());
            }
        }
    }

    private static void setDestinationDir(String downloadPath, String filename, Request request) {
        File file = new File(downloadPath);
        if (file.exists()) {
            if (!file.isDirectory()) {
                throw new IllegalStateException(file.getAbsolutePath() +
                        " already exists and is not a directory");
            }
        } else {
            if (!file.mkdir()) {
                throw new IllegalStateException("Unable to create directory: " +
                        file.getAbsolutePath());
            }
        }
        setDestinationFromBase(file, filename, request);
    }

    private static void setDestinationFromBase(File file, String filename, Request request) {
        if (filename == null) {
            throw new NullPointerException("filename cannot be null");
        }
        request.setDestinationUri(Uri.withAppendedPath(Uri.fromFile(file), filename));
    }

    public static void fileExistQueryDialog(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.download_file_exist)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setMessage(R.string.download_file_exist_msg)
                // if yes, delete existed file and start new download thread
                .setPositiveButton(R.string.ok, null)
                // if no, do nothing at all
                .show();
    }

    public static long getAvailableMemory(String root) {
        StatFs stat = new StatFs(root);
        final long LEFT10MByte = 2560;
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks() - LEFT10MByte;
        return availableBlocks * blockSize;
    }

    public static void showNoEnoughMemoryDialog(Activity mContext) {
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.download_no_enough_memory)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(R.string.download_no_enough_memory)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    public static boolean manageNoEnoughMemory(long contentLength, String root) {
        long mAvailableBytes = getAvailableMemory(root);
        if (mAvailableBytes > 0) {
            if (contentLength > mAvailableBytes) {
                return true;
            }
        } else {
            return true;
        }
        return false;
    }

    public static void showStartDownloadToast(Activity activity,
            boolean privateBrowsing) {
        if (!privateBrowsing) {
            Intent intent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } else {
            activity.finish();
        }
        Toast.makeText(activity, R.string.download_pending, Toast.LENGTH_SHORT)
                .show();
    }

    /**
     * wheather the storage status OK for download file
     *
     * @param activity
     * @param filename the download file's name
     * @param downloadPath the download file's path will be in
     * @return boolean true is ok,and false is not
     */
    public static boolean isStorageStatusOK(Activity activity, String filename, String downloadPath) {
        if (downloadPath.equals(INVALID_PATH)) {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.path_wrong)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.invalid_path)
                    .setPositiveButton(R.string.ok, null)
                    .show();
            return false;
        }

        // assure that internal storage is initialized before
        // comparing it with download path
        if (mInternalStorage == null) {
            initStorageDefaultPath(activity, downloadPath);
        }

        if (!(isPhoneStorageSupported() && downloadPath.contains(mInternalStorage))) {
            String status = getExternalStorageState(activity, downloadPath);
            if (!status.equals(Environment.MEDIA_MOUNTED)) {
                int title;
                String msg;

                // Check to see if the SDCard is busy, same as the music app
                if (status.equals(Environment.MEDIA_SHARED)) {
                    msg = activity.getString(R.string.download_sdcard_busy_dlg_msg);
                    title = R.string.download_sdcard_busy_dlg_title;
                } else {
                    msg = activity.getString(R.string.download_no_sdcard_dlg_msg, filename);
                    title = R.string.download_no_sdcard_dlg_title;
                }

                new AlertDialog.Builder(activity)
                        .setTitle(title)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(msg)
                        .setPositiveButton(R.string.ok, null)
                        .show();
                return false;
            }
        } else {
            String status = Environment.getExternalStorageState();
            if (!status.equals(Environment.MEDIA_MOUNTED)) {
                int mTitle = R.string.download_path_unavailable_dlg_title;
                String mMsg = activity.getString(R.string.download_path_unavailable_dlg_msg);
                new AlertDialog.Builder(activity)
                        .setTitle(mTitle)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(mMsg)
                        .setPositiveButton(R.string.ok, null)
                        .show();
                return false;
            }
        }
        return true;
    }

    /**
     * wheather support Phone Storage
     *
     * @return boolean true support Phone Storage ,false will be not
     */
    public static boolean isPhoneStorageSupported() {
        return true;
    }

    /**
     * show Dialog to warn filename is null
     *
     * @param activity
     */
    public static void showFilenameEmptyDialog(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.filename_empty_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.filename_empty_msg)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    /**
     * get the filename except the suffix and dot
     *
     * @return String the filename except suffix and dot
     */
    public static String getFilenameBase(String filename) {
        int dotindex = filename.lastIndexOf('.');
        if (dotindex != -1) {
            return filename.substring(0, dotindex);
        } else {
            return "";
        }
    }

    /**
     * get the filename's extension from filename
     *
     * @param filename the download filename, may be the user entered
     * @return String the filename's extension
     */
    public static String getFilenameExtension(String filename) {
        int dotindex = filename.lastIndexOf('.');
        if (dotindex != -1) {
            return filename.substring(dotindex + 1);
        } else {
            return "";
        }
    }

    public static String getDefaultDownloadPath(Context context, String downloadPath) {
        String defaultDownloadPath;

        String defaultStorage;
        if (isPhoneStorageSupported()) {
            defaultStorage = Environment.getExternalStorageDirectory().getPath();
        } else {
            defaultStorage = getExternalStorageDirectory(context, downloadPath);
        }

        defaultDownloadPath = defaultStorage + DownloadDirRestriction.getInstance().getDownloadDirectory();
        Log.e(LOGTAG, "defaultStorage directory is : " + defaultDownloadPath);
        return defaultDownloadPath;
    }

    /**
     * translate the directory name into a name which is easy to know for user
     *
     * @param activity
     * @param downloadPath
     * @return String
     */
    public static String getDownloadPathForUser(Activity activity, String downloadPath) {
        if (downloadPath == null) {
            return downloadPath;
        }
        final String phoneStorageDir;
        final String sdCardDir = getExternalStorageDirectory(activity, downloadPath);
        if (isPhoneStorageSupported()) {
            phoneStorageDir = Environment.getExternalStorageDirectory().getPath();
        } else {
            phoneStorageDir = null;
        }

        if (sdCardDir != null && downloadPath.startsWith(sdCardDir)) {
            String sdCardLabel = activity.getResources().getString(
                    R.string.download_path_sd_card_label);
            downloadPath = downloadPath.replace(sdCardDir, sdCardLabel);
        } else if ((phoneStorageDir != null) && downloadPath.startsWith(phoneStorageDir)) {
            String phoneStorageLabel = activity.getResources().getString(
                    R.string.download_path_phone_storage_label);
            downloadPath = downloadPath.replace(phoneStorageDir, phoneStorageLabel);
        }
        return downloadPath;
    }

    private static boolean isRemovable(Object obj) {
        return (Boolean) ReflectHelper.invokeMethod(obj,
                "isRemovable", null, null);
    }

    private static boolean allowMassStorage(Object obj) {
        return (Boolean) ReflectHelper.invokeMethod(obj,
                "allowMassStorage", null, null);
    }

    private static String getPath(Object obj) {
        return (String) ReflectHelper.invokeMethod(obj,
                "getPath", null, null);
    }

    private static String getExternalStorageDirectory(Context context, String downloadPath) {
        String sd = null;
        StorageManager mStorageManager = (StorageManager) context
                .getSystemService(Context.STORAGE_SERVICE);
        Object[] volumes = (Object[]) ReflectHelper.invokeMethod(
                                 mStorageManager, "getVolumeList", null, null);
        for (int i = 0; i < volumes.length; i++) {
            if (isRemovable(volumes[i])
                && ((allowMassStorage(volumes[i]) && downloadPath == null)
                    || (downloadPath != null
                        && downloadPath.startsWith(getPath(volumes[i]))))) {
                sd = getPath(volumes[i]);
                break;
            }
        }
        return sd;
    }

    private static String getExternalStorageState(Context context, String downloadPath) {
        StorageManager mStorageManager = (StorageManager) context
                .getSystemService(Context.STORAGE_SERVICE);
        String path = getExternalStorageDirectory(context, downloadPath);
        Object[] params  = {path};
        Class[] type = new Class[] {String.class};
        return (String) ReflectHelper.invokeMethod(mStorageManager,
                "getVolumeState", type, params);
    }
}
