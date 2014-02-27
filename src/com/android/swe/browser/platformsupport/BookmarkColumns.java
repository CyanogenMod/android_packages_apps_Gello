/*
 * Copyright (c) 2013 The Linux Foundation. All rights reserved.
 * Not a contribution.
 *
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.swe.browser.platformsupport;

public class BookmarkColumns {
    /**
     * The URL of the bookmark or history item.
     * <p>Type: TEXT (URL)</p>
     */
    public static final String URL = "url";

    /**
     * The number of time the item has been visited.
     * <p>Type: NUMBER</p>
     */
    public static final String VISITS = "visits";

    /**
     * The date the item was last visited, in milliseconds since the epoch.
     * <p>Type: NUMBER (date in milliseconds since January 1, 1970)</p>
     */
    public static final String DATE = "date";

    /**
     * Flag indicating that an item is a bookmark. A value of 1 indicates a bookmark, a value
     * of 0 indicates a history item.
     * <p>Type: INTEGER (boolean)</p>
     */
    public static final String BOOKMARK = "bookmark";

    /**
     * The user visible title of the bookmark or history item.
     * <p>Type: TEXT</p>
     */
    public static final String TITLE = "title";

    /**
     * The date the item created, in milliseconds since the epoch.
     * <p>Type: NUMBER (date in milliseconds since January 1, 1970)</p>
     */
    public static final String CREATED = "created";

    /**
     * The favicon of the bookmark. Must decode via {@link BitmapFactory#decodeByteArray}.
     * <p>Type: BLOB (image)</p>
     */
    public static final String FAVICON = "favicon";

    /**
     * @hide
     */
    public static final String THUMBNAIL = "thumbnail";

    /**
     * @hide
     */
    public static final String TOUCH_ICON = "touch_icon";

    /**
     * @hide
     */
    public static final String USER_ENTERED = "user_entered";
}
