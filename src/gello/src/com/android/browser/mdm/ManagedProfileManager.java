/*
 * Copyright (c) 2015, The Linux Foundation. All rights reserved.
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

package com.android.browser.mdm;

import android.annotation.TargetApi;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.util.Log;

import com.android.browser.Browser;

import org.codeaurora.swe.util.Observable;

/**
 * Restrictions manager and merger. Layers 3 levels of restrictions:
 *
 *  1. Device administrator policies (device administrators are for example the Email app, or
 *     traditional MDMs)
 *
 *  2. User restricted profile policies (different users, such as the 'guest' user can have
 *     restrictions on APPs). Note that we don't advertise the restrictions, which is a
 *     possibility allowed by the framework and which can be used by parents to restrict certain
 *     aspects of APPs to children, for example.
 *
 *  3. Provisioned properties, for example from the 'Android at Work' project in which
 *     strings with an understood meaning are available for the apps to be read.
 *
 * Persistency:  The Android framework makes sure the properties will persist even in case of
 *               uninstalling the application (just for 3, 2 and 1 are app independent).
 *
 * Availability: Restrictions are available right after creating this class. If a delegate is
 *               passed in the constructor it will be notified of events, such as the change in
 *               provisioned policies (3).
 *
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class ManagedProfileManager extends Observable {

    private final static String TAG = "ManagedProfileManager";

    // this string is literal because not publicly defined as of API 21
    private final static String INTENT_ACTION_POLICY_CHANGE = "android.intent.action.APPLICATION_RESTRICTIONS_CHANGED";

    /* Generic platform restriction keys. Booleans default to false, missing from bundle means unrestricted */
    private static final String VOLUME_CHANGE_DISABLE = "VolumeChangeDisable"; // boolean
    private static final String VPN_CHANGE_DISABLED = "VpnChangeDisabled"; // boolean
    private static final String LOCATION_DISABLED = "LocationDisabled"; // boolean
    private static final String CAMERA_DISABLED = "CameraDisabled"; // boolean
    private static final String MICROPHONE_DISABLED = "MicrophoneDisabled"; // boolean
    private static final String SCREEN_CAPTURE_DISABLED = "ScreenCaptureDisabled"; // boolean
    private static final String WEB_DEVELOPMENT_DISABLED = "WebDevelopmentDisabled"; // boolean
    private static final String STORAGE_ENCRYPTION_REQUIRED = "StorageEncryptionRequired"; // boolean
    private static final String PASSWORDS_MINIMUM_LENGTH = "PasswordsMinimumLength"; // integer
    private static final String PASSWORDS_MINIMUM_LETTERS = "PasswordsMinimumLetters"; // integer
    private static final String PASSWORDS_MINIMUM_NON_LETTERS = "PasswordsMinimumNonLetters"; // integer

    private static ManagedProfileManager sInstance = null;

    private final Context mContext;

    private final DevicePolicyManager mDevicePolicyManager;
    private final UserManager mUserPolicyManager;

    // cached policies; the first two won't change during execution, the third may
    private Bundle mDeviceAdministratorRestrictions;
    private Bundle mUserRestrictions;
    private Bundle mMdmProvisioningRestrictions;

    private BroadcastReceiver mMdmBroadcastReceiver;

    public static ManagedProfileManager getInstance() {
        if (sInstance == null)
            sInstance = new ManagedProfileManager(Browser.getContext());
        return sInstance;
    }

    private ManagedProfileManager(Context context) {
        mContext = context;

        mDevicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mUserPolicyManager = (UserManager) context.getSystemService(Context.USER_SERVICE);

        // Fetch restrictions
        getMdmPackageRestrictions(mContext.getPackageName());
        getDeviceAdministratorRestrictions();
        getUserRestrictions();

        mergeRestrictions();

        // listen for any change
        registerMdmPolicyChangeListener();
    }

    /**
     * Reads the restrictions which are set by the MDM Administrator
     * @param packageName
     */
    private void getMdmPackageRestrictions(String packageName) {
        try {
            // no need to map the MDM restrictions since they're already in the right format
            // the keys in the bundle have the values of the Restriction.* constants
            mMdmProvisioningRestrictions =
                    mUserPolicyManager.getApplicationRestrictions(packageName);
        } catch (SecurityException e) {
            // Only the system can get/set restrictions on other apps
        }
    }

    /**
     * Reads and maps any Android User Restriction (e.g. 'user can't use the microphone')
     */
    private void getUserRestrictions() {
        Bundle b;
        try {
            b = mUserPolicyManager.getUserRestrictions();
        } catch (Exception e) {
            return;
        }

        // map pertinent user restrictions to our restrictions (the list comes from the
        // UserManager doc, and was last revised for API 21)
        mUserRestrictions = new Bundle();
        if (Build.VERSION.SDK_INT >= 18) {
            if (b.getBoolean(UserManager.DISALLOW_SHARE_LOCATION, false))
                mUserRestrictions.putBoolean(LOCATION_DISABLED, true);
        }
        if (Build.VERSION.SDK_INT >= 21) {
            if (b.getBoolean(UserManager.DISALLOW_ADJUST_VOLUME, false))
                mUserRestrictions.putBoolean(VOLUME_CHANGE_DISABLE, true);
            if (b.getBoolean(UserManager.DISALLOW_CONFIG_VPN, false))
                mUserRestrictions.putBoolean(VPN_CHANGE_DISABLED, true);
            if (b.getBoolean(UserManager.DISALLOW_DEBUGGING_FEATURES, false))
                mUserRestrictions.putBoolean(WEB_DEVELOPMENT_DISABLED, true);
            if (b.getBoolean(UserManager.DISALLOW_UNMUTE_MICROPHONE, false))
                mUserRestrictions.putBoolean(MICROPHONE_DISABLED, true);
        }
    }

    /**
     * Reads and maps any Android Device Restriction (e.g. 'this device can't access location'),
     * which are set by legacy MDMs.
     */
    @SuppressWarnings("ConstantConditions")
    private void getDeviceAdministratorRestrictions() {
        mDeviceAdministratorRestrictions = new Bundle();

        // We are checking all the administrators, not just targeting one in particular - we'll
        // get the more restrictive set of values
        final ComponentName n = null;

        try {
            // map pertinent administrators restrictions to our restrictions
            if (mDevicePolicyManager.getCameraDisabled(n))
                mDeviceAdministratorRestrictions.putBoolean(CAMERA_DISABLED, true);
            int passwordMinimumLength = mDevicePolicyManager.getPasswordMinimumLength(n);
            if (passwordMinimumLength > 0)
                mDeviceAdministratorRestrictions.putInt(PASSWORDS_MINIMUM_LENGTH,
                        passwordMinimumLength);
            int passwordMinimumLetters = mDevicePolicyManager.getPasswordMinimumLetters(n);
            // default minimum number of letters required is 1
            if (passwordMinimumLetters > 1)
                mDeviceAdministratorRestrictions.putInt(PASSWORDS_MINIMUM_LETTERS,
                        passwordMinimumLetters);
            int passwordMinimumNonletters = mDevicePolicyManager.getPasswordMinimumNonLetter(n);
            if (passwordMinimumNonletters > 0)
                mDeviceAdministratorRestrictions.putInt(PASSWORDS_MINIMUM_NON_LETTERS,
                        passwordMinimumNonletters);
            // NOTE: there are more passwords requirement which haven't been parsed yet because
            // the author deemed that superfluous
            if (mDevicePolicyManager.getStorageEncryption(n))
                mDeviceAdministratorRestrictions.putBoolean(STORAGE_ENCRYPTION_REQUIRED, true);

            if (Build.VERSION.SDK_INT >= 21) {
                if (mDevicePolicyManager.getScreenCaptureDisabled(n))
                    mDeviceAdministratorRestrictions.putBoolean(SCREEN_CAPTURE_DISABLED, true);
            }
        } catch (Exception e) {
            // better safe than sorry
            Log.e(TAG, "Error reading from the policy manager: " + e.getMessage());
        }
    }

    public void onActivityDestroy() {
        unregisterMdmPolicyChangeListener();
    }

    /* private implementation ahead */

    private void mergeRestrictions() {
        // Merge restrictions
        Bundle restrictions = new Bundle();
        if (mDeviceAdministratorRestrictions != null)
            restrictions.putAll(mDeviceAdministratorRestrictions);
        if (mUserRestrictions != null)
            restrictions.putAll(mUserRestrictions);
        if (mMdmProvisioningRestrictions != null)
            restrictions.putAll(mMdmProvisioningRestrictions);

        // notify observers
        set(restrictions);
    }

    private void registerMdmPolicyChangeListener() {
        // create a listener that acts upon receiving data
        mMdmBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (action == INTENT_ACTION_POLICY_CHANGE) {
                    getMdmPackageRestrictions(mContext.getPackageName());
                    mergeRestrictions();
                }
            }
        };

        // listen in broadcast
        final IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_ACTION_POLICY_CHANGE);
        mContext.registerReceiver(mMdmBroadcastReceiver, filter);
    }

    private void unregisterMdmPolicyChangeListener() {
        if (mMdmBroadcastReceiver != null) {
            mContext.unregisterReceiver(mMdmBroadcastReceiver);
            mMdmBroadcastReceiver = null;
        }
    }

    /**
     * Added for testing
     * @param restrictions The set of restrictions to apply.
     */
    public void setMdmRestrictions(Bundle restrictions) {
        mMdmProvisioningRestrictions = restrictions;
        mergeRestrictions();
    }
}
