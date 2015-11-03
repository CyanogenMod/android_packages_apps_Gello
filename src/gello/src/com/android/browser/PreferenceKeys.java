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

package com.android.browser;

public interface PreferenceKeys {

    static final String PREF_AUTOFILL_ACTIVE_PROFILE_ID = "autofill_active_profile_id";
    static final String PREF_DEBUG_MENU = "debug_menu";

    // ----------------------
    // Keys for accessibility_preferences.xml
    // ----------------------
    static final String PREF_MIN_FONT_SIZE = "min_font_size";
    static final String PREF_TEXT_SIZE = "text_size";
    static final String PREF_TEXT_ZOOM = "text_zoom";
    static final String PREF_DOUBLE_TAP_ZOOM = "double_tap_zoom";
    static final String PREF_FORCE_USERSCALABLE = "force_userscalable";
    static final String PREF_INVERTED = "inverted";
    static final String PREF_INVERTED_CONTRAST = "inverted_contrast";

    // ----------------------
    // Keys for advanced_preferences.xml
    // ----------------------
    static final String PREF_AUTOFIT_PAGES = "autofit_pages";
    static final String PREF_POPUP_WINDOWS = "popup_windows";
    static final String PREF_DEFAULT_TEXT_ENCODING = "default_text_encoding";
    static final String PREF_ENABLE_JAVASCRIPT = "enable_javascript";
    static final String PREF_ENABLE_MEMORY_MONITOR = "enable_memory_monitor";
    static final String PREF_LOAD_PAGE = "load_page";
    static final String PREF_OPEN_IN_BACKGROUND = "open_in_background";
    static final String PREF_RESET_DEFAULT_PREFERENCES = "reset_default_preferences";
    static final String PREF_SEARCH_ENGINE = "search_engine";
    static final String PREF_WEBSITE_SETTINGS = "website_settings";
    static final String PREF_ALLOW_APP_TABS = "allow_apptabs";
    // Keys for download path settings
    static final String PREF_DOWNLOAD_PATH = "download_path_setting_screen";
    // ----------------------
    // Keys for debug_preferences.xml
    // ----------------------
    static final String PREF_ENABLE_HARDWARE_ACCEL = "enable_hardware_accel";
    static final String PREF_ENABLE_HARDWARE_ACCEL_SKIA = "enable_hardware_accel_skia";
    static final String PREF_DISABLE_PERF = "disable_perf";

    // ----------------------
    // Keys for general_preferences.xml
    // ----------------------
    static final String PREF_AUTOFILL_ENABLED = "autofill_enabled";
    static final String PREF_AUTOFILL_PROFILE = "autofill_profile";
    static final String PREF_HOMEPAGE = "homepage";
    static final String PREF_POWERSAVE_ENABLED = "powersave_enabled";
    static final String PREF_COLOREDSB_ENABLED = "coloredsb_enabled";
    static final String PREF_NIGHTMODE_ENABLED = "nightmode_enabled";
    static final String PREF_SYNC_WITH_CHROME = "sync_with_chrome";

    // ----------------------
    // Keys for hidden_debug_preferences.xml
    // ----------------------
    static final String PREF_ENABLE_LIGHT_TOUCH = "enable_light_touch";
    static final String PREF_ENABLE_NAV_DUMP = "enable_nav_dump";
    static final String PREF_ENABLE_TRACING = "enable_tracing";
    static final String PREF_ENABLE_VISUAL_INDICATOR = "enable_visual_indicator";
    static final String PREF_ENABLE_CPU_UPLOAD_PATH = "enable_cpu_upload_path";
    static final String PREF_JS_ENGINE_FLAGS = "js_engine_flags";
    static final String PREF_NORMAL_LAYOUT = "normal_layout";
    static final String PREF_SMALL_SCREEN = "small_screen";
    static final String PREF_WIDE_VIEWPORT = "wide_viewport";
    static final String PREF_RESET_PRELOGIN = "reset_prelogin";

    // ----------------------
    // Keys for lab_preferences.xml
    // ----------------------
    static final String PREF_FULLSCREEN = "fullscreen";

    // ----------------------
    // Keys for privacy_security_preferences.xml
    // ----------------------
    static final String PREF_CLEAR_SELECTED_DATA = "privacy_clear_selected";
    static final String PREF_ACCEPT_COOKIES = "accept_cookies";
    static final String PREF_ENABLE_GEOLOCATION = "enable_geolocation";
    static final String PREF_PRIVACY_CLEAR_CACHE = "privacy_clear_cache";
    static final String PREF_PRIVACY_CLEAR_COOKIES = "privacy_clear_cookies";
    static final String PREF_PRIVACY_CLEAR_FORM_DATA = "privacy_clear_form_data";
    static final String PREF_PRIVACY_CLEAR_GEOLOCATION_ACCESS = "privacy_clear_geolocation_access";
    static final String PREF_PRIVACY_CLEAR_HISTORY = "privacy_clear_history";
    static final String PREF_PRIVACY_CLEAR_PASSWORDS = "privacy_clear_passwords";
    static final String PREF_REMEMBER_PASSWORDS = "remember_passwords";
    static final String PREF_SAVE_FORMDATA = "save_formdata";
    static final String PREF_SHOW_SECURITY_WARNINGS = "show_security_warnings";
    static final String PREF_DO_NOT_TRACK = "do_not_track";
    static final String PREF_WEB_REFINER = "web_refiner";

    // ----------------------
    // Keys for bandwidth_preferences.xml
    // ----------------------
    static final String PREF_DATA_PRELOAD = "preload_when";
    static final String PREF_LINK_PREFETCH = "link_prefetch_when";
    static final String PREF_LOAD_IMAGES = "load_images";

    // ----------------------
    // Keys for browser recovery
    // ----------------------
    /**
     * The last time recovery was started as System.currentTimeMillis.
     * 0 if not set.
     */
    static final String KEY_LAST_RECOVERED = "last_recovered";

    /**
     * Key for whether or not the last run was paused.
     */
    static final String KEY_LAST_RUN_PAUSED = "last_paused";

    // ----------------------
    // Keys for about_preferences.xml
    // ----------------------
    static final String PREF_ABOUT = "about";
    static final String PREF_VERSION = "version";
    static final String PREF_BUILD_DATE = "built";
    static final String PREF_BUILD_HASH = "hash";
    static final String PREF_USER_AGENT = "user_agent";
    static final String PREF_HELP = "help_about";
    static final String PREF_FEEDBACK = "feedback";
    static final String PREF_AUTO_UPDATE = "update_notification";
    static final String PREF_EDGE_SWIPE = "edge_swiping_action";
    static final String PREF_LEGAL = "legal";

    // ----------------------
    // Keys for legal_preferences.xml
    // ----------------------
    static final String PREF_LEGAL_CREDITS = "legal_credits";
    static final String PREF_LEGAL_EULA = "legal_eula";
    static final String PREF_LEGAL_PRIVACY_POLICY = "legal_privacy_policy";

    static final String ACTION_RELOAD_PAGE = "reload";

    // User preference
    static final String PREF_USER_SEARCH_ENGINE = "user_search_engine-";

}
