/*
 * Copyright (C) 2009 The Android Open Source Project
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

import org.codeaurora.swe.GeolocationPermissions;
import org.codeaurora.swe.GeolocationPermissions.GeolocationPolicyChange;
import org.codeaurora.swe.GeolocationPermissions.GeolocationPolicyListener;
import org.json.JSONArray;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class GeolocationPermissionsPrompt extends LinearLayout implements
        GeolocationPolicyListener {
    private TextView mMessage;
    private Button mShareButton;
    private Button mShareForLimitedTimeButton;
    private Button mDontShareButton;
    private CheckBox mRemember;
    private android.webkit.GeolocationPermissions.Callback mCallback;
    private String mOrigin;
    private GeolocationPermissions mGeolocationPermissions;

    private static final long MILLIS_PER_DAY = 86400000;

    public GeolocationPermissionsPrompt(Context context) {
        this(context, null);
    }

    public GeolocationPermissionsPrompt(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void init(boolean privateBrowsing) {
        mMessage = (TextView) findViewById(R.id.message);
        mShareButton = (Button) findViewById(R.id.share_button);
        mShareForLimitedTimeButton = (Button)
                findViewById(R.id.share_for_limited_time_button);
        mDontShareButton = (Button) findViewById(R.id.dont_share_button);
        mRemember = (CheckBox) findViewById(R.id.remember);

        mRemember.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mShareForLimitedTimeButton.setEnabled(isChecked);
            }
        });

        mShareButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleButtonClick(true, GeolocationPermissions.DO_NOT_EXPIRE);
            }
        });
        mShareForLimitedTimeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleButtonClick(true, System.currentTimeMillis() + MILLIS_PER_DAY);
            }
        });
        mDontShareButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleButtonClick(false, GeolocationPermissions.DO_NOT_EXPIRE);
            }
        });

        mGeolocationPermissions = (privateBrowsing ? GeolocationPermissions.getIncognitoInstance()
                : GeolocationPermissions.getInstance());
        mGeolocationPermissions.addListener(this);
    }

    /**
     * Shows the prompt for the given origin. When the user clicks on one of
     * the buttons, the supplied callback is be called.
     */
    public void show(String origin,
            android.webkit.GeolocationPermissions.Callback callback) {
        mOrigin = origin;
        mCallback = callback;
        Uri uri = Uri.parse(mOrigin);
        setMessage("http".equals(uri.getScheme()) ?  mOrigin.substring(7) : mOrigin);
        // The checkbox should always be intially checked.
        mRemember.setChecked(true);
        setVisibility(View.VISIBLE);
    }

    /**
     * This method is called when the user modifies the geolocation policy in a different Tab.
     */
    public void onGeolocationPolicyChanged(GeolocationPolicyChange change) {
        int action = change.getAction();

        if (action == GeolocationPermissions.ALL_POLICIES_REMOVED) {
            // do not dismiss when policy is removed
            return;
        } else if (change.getOrigin() != null && mOrigin.equals(change.getOrigin())) {
            boolean allow = false;
            switch (action) {
                case GeolocationPermissions.ORIGIN_POLICY_REMOVED:
                    // do not dismiss when policy is removed
                    break;
                case GeolocationPermissions.ORIGIN_ALLOWED:
                    allow = true;
                case GeolocationPermissions.ORIGIN_DENIED:
                    hide();
                    JSONArray jsonArray = new JSONArray();
                    jsonArray.put(System.currentTimeMillis());
                    jsonArray.put(mOrigin);
                    // no need to retain policy since it has already been saved
                    mCallback.invoke(jsonArray.toString(), allow, false /*retain*/);
                    break;
                default:
                    return;
            }
        }
    }

    /**
     * This method is called when the user navigates to a new URL (onPageStarted). In this case,
     * we respond as if user denied access without retaining the policy.
     */
    public void dismiss() {
        hide();
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(System.currentTimeMillis());
        jsonArray.put(mOrigin);
        mCallback.invoke(jsonArray.toString(), false /*allow*/, false /*retain*/);
    }
    /**
     * Hides the prompt.
     */
    public void hide() {
        setVisibility(View.GONE);
        mGeolocationPermissions.removeListener(this);
    }

    /**
     * Handles a click on one the buttons by invoking the callback.
     */
    private void handleButtonClick(boolean allow, long expirationTime) {
        hide();

        boolean remember = mRemember.isChecked();
        if (remember) {
            Toast toast = Toast.makeText(
                    getContext(),
                    allow ? R.string.geolocation_permissions_prompt_toast_allowed :
                            R.string.geolocation_permissions_prompt_toast_disallowed,
                    Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM, 0, 0);
            toast.show();
        }

        // Encode the expirationTime and origin as a JSON string.
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(expirationTime);
        jsonArray.put(mOrigin);
        mCallback.invoke(jsonArray.toString(), allow, remember);
    }

    /**
     * Sets the prompt's message.
     */
    private void setMessage(CharSequence origin) {
        mMessage.setText(String.format(
            getResources().getString(R.string.geolocation_permissions_prompt_message),
            origin));
    }
}
