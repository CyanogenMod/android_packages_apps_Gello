/*
 * Copyright (c) 2015, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
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

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FolderTileView extends ViewGroup {

    // created in the constructor
    private View mInnerLayout;
    private TextView mTextView;
    private TextView mLabelView;
    private int mPaddingLeft = 0;
    private int mPaddingTop = 0;
    private int mPaddingRight = 0;
    private int mPaddingBottom = 0;

    // runtime params set on Layout
    private int mCurrentWidth;
    private int mCurrentHeight;

    // static objects, to be recycled amongst instances (this is an optimization)
    private static Paint sBackgroundPaint;
    private String mText;
    private String mLabel;


    /* XML constructors */

    public FolderTileView(Context context) {
        super(context);
        xmlInit(null, 0);
    }

    public FolderTileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        xmlInit(attrs, 0);
    }

    public FolderTileView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        xmlInit(attrs, defStyle);
    }


    /* Programmatic Constructors */

    public FolderTileView(Context context, String text, String label) {
        super(context);
        mText = text;
        mLabel = label;
        init();
    }


    /**
     * Replaces the main text of the bookmark tile
     */
    public void setText(String text) {
        mText = text;
        if (mTextView != null)
            mTextView.setText(mText);
    }

    /**
     * Replaces the subtitle, for example "32 items"
     */
    public void setLabel(String label) {
        mLabel = label;
        if (mLabelView != null)
            mLabelView.setText(mLabel);
    }


    /* private stuff ahead */

    private void xmlInit(AttributeSet attrs, int defStyle) {
        // load attributes
        final TypedArray a = getContext().obtainStyledAttributes(attrs,
                R.styleable.FolderTileView, defStyle, 0);

        // saves the text for later
        setText(a.getString(R.styleable.FolderTileView_android_text));

        // saves the label for later
        setLabel(a.getString(R.styleable.FolderTileView_android_label));

        // delete attribute resolution
        a.recycle();

        // proceed with real initialization
        init();
    }

    private void init() {
        // create new Views for us from the XML (and automatically add them)
        inflate(getContext(), R.layout.folder_tile_view, this);

        // we make the assumption that the XML file will always have 1 and only 1 child
        mInnerLayout = getChildAt(0);
        mInnerLayout.setVisibility(View.VISIBLE);

        // reference objects
        mTextView = (TextView) mInnerLayout.findViewById(android.R.id.text1);
        if (mText != null && !mText.isEmpty())
            mTextView.setText(mText);
        mLabelView = (TextView) mInnerLayout.findViewById(android.R.id.text2);
        if (mLabel != null && !mLabel.isEmpty())
            mLabelView.setText(mLabel);

        // load the common statics, also for the SiteTileView if needed
        final Resources resources = getResources();
        SiteTileView.ensureCommonLoaded(resources);
        ensureCommonLoaded(resources);

        // get the padding rect from the Tile View (to stay synced in size)
        final Rect padding = SiteTileView.getBackgroundDrawablePadding();
        mPaddingLeft = padding.left;
        mPaddingTop = padding.top;
        mPaddingRight = padding.right;
        mPaddingBottom = padding.bottom;

        // we'll draw our background (usually ViewGroups don't)
        setWillNotDraw(false);
    }

    private static void ensureCommonLoaded(Resources r) {
        if (sBackgroundPaint != null)
            return;

        // shared tiles background paint
        sBackgroundPaint = new Paint();
        sBackgroundPaint.setColor(r.getColor(R.color.FolderTileBackground));
        sBackgroundPaint.setAntiAlias(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // layout the xml inflated contents
        if (mInnerLayout != null) {
            final int desiredWidth = MeasureSpec.getSize(widthMeasureSpec);
            final int desiredHeight = MeasureSpec.getSize(heightMeasureSpec);
            if (desiredHeight > 0 || desiredHeight > 0)
                mInnerLayout.measure(
                        MeasureSpec.EXACTLY | desiredWidth - mPaddingLeft - mPaddingRight,
                        MeasureSpec.EXACTLY | desiredHeight - mPaddingTop - mPaddingBottom);
            else
                mInnerLayout.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // update current params
        mCurrentWidth = right - left;
        mCurrentHeight = bottom - top;

        // layout the inflated XML layout using the same Padding as the SiteTileView
        if (mInnerLayout != null)
            mInnerLayout.layout(mPaddingLeft, mPaddingTop,
                    mCurrentWidth - mPaddingRight, mCurrentHeight - mPaddingBottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int left = mPaddingLeft;
        final int top = mPaddingTop;
        final int right = mCurrentWidth - mPaddingRight;
        final int bottom = mCurrentHeight - mPaddingBottom;

        // draw the background rectangle
        float roundedRadius = SiteTileView.sRoundedRadius;
        if (roundedRadius >= 1.) {
            SiteTileView.sRectF.set(left, top, right, bottom);
            canvas.drawRoundRect(SiteTileView.sRectF, roundedRadius, roundedRadius, sBackgroundPaint);
        } else
            canvas.drawRect(left, top, right, bottom, sBackgroundPaint);
    }

}
