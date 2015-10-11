/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.browser.widget;

import com.android.browser.BrowserActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BookmarkWidgetProxy extends BroadcastReceiver {

    private static final String TAG = "BookmarkWidgetProxy";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BookmarkThumbnailWidgetService.ACTION_CHANGE_FOLDER.equals(intent.getAction())) {
            BookmarkThumbnailWidgetService.changeFolder(context, intent);
        } else if (BrowserActivity.ACTION_SHOW_BROWSER.equals(intent.getAction())) {
            startActivity(context,
                    new Intent(BrowserActivity.ACTION_SHOW_BROWSER,
                    null, context, BrowserActivity.class));
        } else {
            Intent view = new Intent(intent);
            view.setComponent(null);
            startActivity(context, view);
        }
    }

    void startActivity(Context context, Intent intent) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "Failed to start intent activity", e);
        }
    }
}
