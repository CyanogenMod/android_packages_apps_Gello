/*
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
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

import com.android.browser.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.widget.EditText;

public class BrowserUtils {

    private static final String LOGTAG = "BrowserUtils";
    public static final int FILENAME_MAX_LENGTH = 48;
    public static final int ADDRESS_MAX_LENGTH = 2048;
    private static AlertDialog.Builder mAlertDialog = null;

    public static void maxLengthFilter(final Context context, final EditText editText,
            final int max_length) {
        InputFilter[] contentFilters = new InputFilter[1];
        contentFilters[0] = new InputFilter.LengthFilter(max_length) {
            public CharSequence filter(CharSequence source, int start, int end,
                    Spanned dest, int dstart, int dend) {
                int keep = max_length - (dest.length() - (dend - dstart));
                if (keep <= 0) {
                    showWarningDialog(context, max_length);
                    return "";
                } else if (keep >= end - start) {
                    return null;
                } else {
                    if (keep < source.length()) {
                        showWarningDialog(context, max_length);
                    }
                    return source.subSequence(start, start + keep);
                }
            }
        };
        editText.setFilters(contentFilters);
    }

    private static void showWarningDialog(final Context context, int max_length) {
        if (mAlertDialog != null)
            return;

        mAlertDialog = new AlertDialog.Builder(context);
        mAlertDialog.setTitle(R.string.browser_max_input_title)
            //    .setIcon(android.R.drawable.ic_dialog_info)
                .setMessage(context.getString(R.string.browser_max_input, max_length))
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                return;
                            }
                        })
                .show()
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        Log.w("BrowserUtils", "onDismiss");
                        mAlertDialog = null;
                        return;
                    }
                });
    }
}
