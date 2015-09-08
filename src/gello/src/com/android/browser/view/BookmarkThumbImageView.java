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

package com.android.browser.view;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class BookmarkThumbImageView extends ImageView {

    public BookmarkThumbImageView(Context context) {
        this(context, null);
    }

    public BookmarkThumbImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BookmarkThumbImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();
        int containerWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int containerHeight = getHeight() - getPaddingTop() - getPaddingBottom();

        float scale;
        Matrix m = new Matrix();
        if ( (drawableWidth * containerHeight) > (containerWidth * drawableHeight)) {
            scale = (float) containerHeight / (float) drawableHeight;
        } else {
            scale = (float) containerWidth / (float) drawableWidth;
            float translateY = (containerHeight - drawableHeight * scale) / 2;
            if (translateY < 0) {
                translateY = 0;
            }
            m.postTranslate(0, translateY + 0.5f);
        }
        m.setScale(scale, scale);

        this.setScaleType(ScaleType.MATRIX);
        this.setImageMatrix(m);
        super.setImageDrawable(drawable);
    }
}
