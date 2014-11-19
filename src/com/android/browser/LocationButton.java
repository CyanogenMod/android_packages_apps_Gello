/*
 *  Copyright (c) 2014, The Linux Foundation. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 *      * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 *  ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 *  BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 *  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 *  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 *  OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 *  IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.android.browser;

import org.codeaurora.swe.GeolocationPermissions;
import org.codeaurora.swe.GeolocationPermissions.OnGeolocationPolicyModifiedListener;
import org.json.JSONArray;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.ValueCallback;
import android.widget.ImageButton;

public class LocationButton extends ImageButton
        implements OnGeolocationPolicyModifiedListener {
    private GeolocationPermissions mGeolocationPermissions;
    private long mCurrentTabId;
    private String mCurrentOrigin;
    private boolean mCurrentIncognito;

    private static final long MILLIS_PER_DAY = 86400000;

    protected long geolocationPolicyExpiration;
    protected boolean geolocationPolicyOriginAllowed;

    public LocationButton(Context context) {
        super(context);
    }

    public LocationButton(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public LocationButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        init();
    }

    private void updateGeolocationPermissions() {
        mGeolocationPermissions = mCurrentIncognito ?
                                    GeolocationPermissions.getIncognitoInstance() :
                                    GeolocationPermissions.getInstance();
        mGeolocationPermissions.registerOnGeolocationPolicyModifiedListener(this);
    }

    // TODO: Perform this initilalization only after the engine initialization is complete.
    private void init() {
        mCurrentTabId = -1;
        mCurrentOrigin = null;
        mCurrentIncognito = false;

        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mCurrentOrigin.isEmpty()) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    updateGeolocationPermissions();
                    final GeolocationPermissions geolocationPermissions = mGeolocationPermissions;
                    DialogInterface.OnClickListener alertDialogListener =
                            new AlertDialog.OnClickListener() {
                        public void onClick(DialogInterface dlg, int which) {
                            String origin = mCurrentOrigin;
                            int selectedPosition = ((AlertDialog)dlg)
                                    .getListView().getCheckedItemPosition();
                            switch (selectedPosition) {
                                case 0: // Deny forever
                                    geolocationPermissions.deny(origin);
                                    break;
                                case 1: // Extend for 24 hours
                                    // encode the expiration time and origin as a JSON string
                                    JSONArray jsonArray = new JSONArray();
                                    jsonArray.put(System.currentTimeMillis() + MILLIS_PER_DAY);
                                    jsonArray.put(origin);
                                    geolocationPermissions.allow(jsonArray.toString());
                                    break;
                                case 2: // Allow forever
                                    geolocationPermissions.allow(origin);
                                    break;
                                case 3: // Always ask
                                    geolocationPermissions.clear(origin);
                                    break;
                                default:
                                    break;
                            }
                        }};

                    builder.setTitle(String.format(getResources()
                            .getString(R.string.geolocation_settings_page_dialog_title),
                                "http".equals(Uri.parse(mCurrentOrigin).getScheme()) ?
                                        mCurrentOrigin.substring(7) : mCurrentOrigin))
                        .setPositiveButton(R.string.geolocation_settings_page_dialog_ok_button,
                                           alertDialogListener)
                        .setNegativeButton(R.string.geolocation_settings_page_dialog_cancel_button, null);

                    final ValueCallback<Long> getExpirationCallback = new ValueCallback<Long>() {
                        public void onReceiveValue(Long expirationTime) {
                            if (expirationTime != null) {
                                geolocationPolicyExpiration = expirationTime.longValue();
                                // Set radio button and location icon
                                if (!geolocationPolicyOriginAllowed) {
                                    // 0: Deny forever
                                    builder.setSingleChoiceItems(R.array.geolocation_settings_choices, 0, null);
                                } else {
                                    if (geolocationPolicyExpiration
                                            != GeolocationPermissions.DO_NOT_EXPIRE) {
                                        // 1: Allow for 24 hours
                                        builder.setSingleChoiceItems(R.array.geolocation_settings_choices, 1, null);
                                    } else {
                                        // 2: Allow forever
                                        builder.setSingleChoiceItems(R.array.geolocation_settings_choices, 2, null);
                                    }
                                }
                            }
                            builder.show();
                        }};

                    final ValueCallback<Boolean> getAllowedCallback = new ValueCallback<Boolean>() {
                        public void onReceiveValue(Boolean allowed) {
                            if (allowed != null) {
                                geolocationPolicyOriginAllowed = allowed.booleanValue();
                                //Get the policy expiration time
                                geolocationPermissions
                                    .getExpirationTime(mCurrentOrigin, getExpirationCallback);
                            }
                        }};

                    geolocationPermissions.hasOrigin(mCurrentOrigin,
                        new ValueCallback<Boolean>() {
                            public void onReceiveValue(Boolean hasOrigin) {
                                if (hasOrigin != null && hasOrigin.booleanValue()) {
                                    //Get whether origin is allowed or denied
                                    geolocationPermissions.getAllowed(mCurrentOrigin,
                                            getAllowedCallback);
                                }
                            }
                        });
                    }
                }
            });
    }

    public void onTabDataChanged(Tab tab) {
        long tabId = tab.getId();
        String origin = GeolocationPermissions.getOriginFromUrl(tab.getUrl());
        boolean incognito = tab.isPrivateBrowsingEnabled();

        if (mCurrentTabId != tabId) {
            mCurrentTabId = tabId;
            mCurrentOrigin = origin;
            update();
        }
        // Update icon if we are in the same tab and origin has changed
          else if (!((mCurrentOrigin == null && origin == null) ||
                (mCurrentOrigin != null && origin != null
                    && mCurrentOrigin.equals(origin)))) {
            mCurrentOrigin = origin;
            update();
        }
    }

    public void update() {
        if (mCurrentOrigin != null) {
            updateGeolocationPermissions();
            mGeolocationPermissions.hasOrigin(mCurrentOrigin,
                    new ValueCallback<Boolean>() {
                public void onReceiveValue(Boolean hasOrigin) {
                    if (hasOrigin != null && hasOrigin.booleanValue()) {
                        mGeolocationPermissions.getAllowed(mCurrentOrigin,
                                new ValueCallback<Boolean>() {
                            public void onReceiveValue(Boolean allowed) {
                                if (allowed != null) {
                                    if (allowed.booleanValue()) {
                                        LocationButton.this.setImageResource(R.drawable.ic_action_gps_on);
                                        LocationButton.this.setVisibility(VISIBLE);
                                    } else {
                                        LocationButton.this.setImageResource(R.drawable.ic_action_gps_off);
                                        LocationButton.this.setVisibility(VISIBLE);
                                    }
                                }
                            }
                        });
                    } else {
                        LocationButton.this.setVisibility(GONE);
                    }
                }
            });
        } else {
            this.setVisibility(GONE);
        }
    }

    @Override
    public void onGeolocationPolicyAdded(String origin, boolean allow) {
        if (mCurrentOrigin != null && mCurrentOrigin.equals(origin)) {
            this.setImageResource(allow ? R.drawable.ic_action_gps_on :
                R.drawable.ic_action_gps_off);
            this.setVisibility(VISIBLE);
        }
    }

    @Override
    public void onGeolocationPolicyCleared(String origin) {
        if (mCurrentOrigin != null && mCurrentOrigin.equals(origin)) {
            this.setVisibility(GONE);
        }
    }

    @Override
    public void onGeolocationPolicyClearedAll() {
        this.setVisibility(GONE);
    }

}
