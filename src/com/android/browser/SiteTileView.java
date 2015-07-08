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
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

/**
 * This represents a WebSite Tile that is created from a Drawable and will scale across any
 * area this is externally layouted to. There are 3 possible looks:
 *   - just the favicon (TYPE_SMALL)
 *   - drop-shadow plus a thin overlay border (1dp) (TYPE_MEDIUM)
 *   - centered favicon, extended color, rounded base (TYPE_LARGE)
 *
 * By centralizing everything in this class we make customization of looks much easier.
 *
 * NOTES:
 *   - do not set a background from the outside; this overrides it automatically
 */
public class SiteTileView extends View {

    // public trust level constants
    public static final int TRUST_UNKNOWN   =    0; // default
    public static final int TRUST_AVOID     = 0x01;
    public static final int TRUST_UNTRUSTED = 0x02;
    public static final int TRUST_TRUSTED   = 0x04;
    private static final int TRUST_MASK     = 0x07;


    // static configuration
    private static final int THRESHOLD_MEDIUM_DP = 32;
    private static final int THRESHOLD_LARGE_DP = 64;
    private static final int LARGE_FAVICON_SIZE_DP = 48;
    private static final int BACKGROUND_DRAWABLE_RES = R.drawable.img_tile_background;
    private static final float FILLER_RADIUS_DP = 2f; // sync with the bg image radius
    private static final int FILLER_FALLBACK_COLOR = Color.WHITE; // in case there is no favicon
    private static final int OVERLINE_WIDTH_RES = R.dimen.SiteTileOverlineWidth;
    private static final int OVERLINE_COLOR_DEFAULT_RES = R.color.SiteTileOverlineUnknown;
    private static final int OVERLINE_COLOR_TRUSTED_RES = R.color.SiteTileOverlineTrusted;
    private static final int OVERLINE_COLOR_UNTRUSTED_RES = R.color.SiteTileOverlineUntrusted;
    private static final int OVERLINE_COLOR_AVOID_RES = R.color.SiteTileOverlineAvoid;
    private static final boolean SHOW_EXTRAS_BLOCKED_COUNT = true;

    // internal enums
    private static final int TYPE_SMALL = 1;
    private static final int TYPE_MEDIUM = 2;
    private static final int TYPE_LARGE = 3;
    private static final int TYPE_AUTO = 0;
    private static final int COLOR_AUTO = 0;


    // configuration
    private Bitmap mFaviconBitmap = null;
    private Paint mFundamentalPaint = null;
    private int mFaviconWidth = 0;
    private int mFaviconHeight = 0;
    private int mForcedType = TYPE_AUTO;
    private int mForcedFundamentalColor = COLOR_AUTO;
    private boolean mFloating = false;
    private int mTrustLevel = TRUST_UNKNOWN;
    private int mExtraBlockedObjectsCount = 0;
    private boolean mExtrasShown = false;

    // runtime params set on Layout
    private int mCurrentWidth = 0;
    private int mCurrentHeight = 0;
    private int mCurrentType = TYPE_MEDIUM;
    private int mPaddingLeft = 0;
    private int mPaddingTop = 0;
    private int mPaddingRight = 0;
    private int mPaddingBottom = 0;
    private boolean mCurrentBackgroundDrawn = false;

    // static objects, to be recycled amongst instances (this is an optimization)
    private static int sMediumPxThreshold = -1;
    private static int sLargePxThreshold = -1;
    private static int sLargeFaviconPx = -1;
    private static float sRoundedRadius = -1;
    private static Paint sBitmapPaint = null;
    private static Paint sExtrasPaint = null;
    private static Rect sSrcRect = new Rect();
    private static Rect sDstRect = new Rect();
    private static RectF sRectF = new RectF();
    private static Paint sOverlineOutlinePaint = null;
    private static Drawable sBackgroundDrawable = null;
    private static Rect sBackgroundDrawablePadding = new Rect();


    /* XML constructors */

    public SiteTileView(Context context) {
        super(context);
        xmlInit(null, 0);
    }

    public SiteTileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        xmlInit(attrs, 0);
    }

    public SiteTileView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        xmlInit(attrs, defStyle);
    }


    /* Programmatic Constructors */

    public SiteTileView(Context context, Bitmap favicon) {
        super(context);
        init(favicon, COLOR_AUTO);
    }

    public SiteTileView(Context context, Bitmap favicon, int fundamentalColor) {
        super(context);
        init(favicon, fundamentalColor);
    }


    /**
     * Changes the current favicon (and associated fundamental color) on the fly
     */
    public void replaceFavicon(Bitmap favicon) {
        replaceFavicon(favicon, COLOR_AUTO);
    }

    /**
     * Changes the current favicon (and associated fundamental color) on the fly
     * @param favicon the new favicon
     * @param fundamentalColor the new fudamental color, or COLOR_AUTO
     */
    public void replaceFavicon(Bitmap favicon, int fundamentalColor) {
        init(favicon, fundamentalColor);
        requestLayout();
    }

    /**
     * Disables the automatic background and filling. Useful for things that are not really
     * "Website Tiles", like folders.
     * @param floating true to disable the background (defaults to false)
     */
    public void setFloating(boolean floating) {
        if (mFloating != floating) {
            mFloating = floating;
            invalidate();
        }
    }

    /**
     * For 'Medium' tiles updates the border and corner badges
     * @param trustLevel one of the TRUST_ constants
     */
    public void setTrustLevel(int trustLevel) {
        if (mTrustLevel != trustLevel) {
            mTrustLevel = trustLevel;
            if (requiresOverline())
                invalidate();
        }
    }

    /**
     * Sets the number of objects blocked (a positive contribution to the page). Presentation
     * may or may not have the number indication.
     * @param sessionCounter Counter of blocked objects. Use 0 to not display anything.
     */
    public void setExtraBlockedObjectsCount(int sessionCounter) {
        if (sessionCounter != mExtraBlockedObjectsCount) {
            mExtraBlockedObjectsCount = sessionCounter;
            updateExtrasShown();
            if (SHOW_EXTRAS_BLOCKED_COUNT)
                invalidate();
        }
    }

    private void updateExtrasShown() {
        boolean shouldBeShown = mExtraBlockedObjectsCount > 0;
        if (shouldBeShown != mExtrasShown) {
            mExtrasShown = shouldBeShown;
            invalidate();
        }
    }



    /**
     * @return The fundamental color representing the site.
     */
    public int getFundamentalColor() {
        if (mForcedFundamentalColor != COLOR_AUTO)
            return mForcedFundamentalColor;
        if (mFundamentalPaint == null)
            mFundamentalPaint = createFundamentalPaint(mFaviconBitmap, COLOR_AUTO);
        return mFundamentalPaint.getColor();
    }


    /*** private stuff ahead ***/

    private void xmlInit(AttributeSet attrs, int defStyle) {
        // load attributes
        final TypedArray a = getContext().obtainStyledAttributes(attrs,
                R.styleable.SiteTileView, defStyle, 0);

        // fetch the drawable, if defined - then just extract and use the bitmap
        final Drawable drawable = a.getDrawable(R.styleable.SiteTileView_android_src);
        final Bitmap favicon = drawable instanceof BitmapDrawable ?
                ((BitmapDrawable) drawable).getBitmap() : null;

        // check if we disable shading (plain favicon)
        if (a.getBoolean(R.styleable.SiteTileView_flat, false))
            mForcedType = TYPE_SMALL;

        // check if we want it floating (disable shadow and filler)
        setFloating(a.getBoolean(R.styleable.SiteTileView_floating, false));

        // read the trust level (unknown, aka 'default', if not present)
        setTrustLevel(a.getInteger(R.styleable.SiteTileView_trustLevel, TRUST_UNKNOWN)
                & TRUST_MASK);

        // read the amount of blocked objects (or 0 if not present)
        setExtraBlockedObjectsCount(a.getInteger(R.styleable.SiteTileView_blockedObjects, 0));

        // delete attribute resolution
        a.recycle();

        // proceed with real initialization
        init(favicon, COLOR_AUTO);
    }

    private void init(Bitmap favicon, int fundamentalColor) {
        mFaviconBitmap = favicon;
        if (mFaviconBitmap != null) {
            mFaviconWidth = mFaviconBitmap.getWidth();
            mFaviconHeight = mFaviconBitmap.getHeight();
        }

        // don't compute the paint right now, just save any hint for later
        mFundamentalPaint = null;
        mForcedFundamentalColor = fundamentalColor;

        // shared (static) resources initialization; except for background, inited on-demand
        if (sMediumPxThreshold < 0) {
            final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

            // heuristics thresholds
            sMediumPxThreshold = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    THRESHOLD_MEDIUM_DP, displayMetrics);
            sLargePxThreshold = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    THRESHOLD_LARGE_DP, displayMetrics);
            sLargeFaviconPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    LARGE_FAVICON_SIZE_DP, displayMetrics);

            // rounded radius
            sRoundedRadius = FILLER_RADIUS_DP > 0 ? TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, FILLER_RADIUS_DP, displayMetrics) : 0;

            // bitmap paint (copy, smooth scale)
            sBitmapPaint = new Paint();
            sBitmapPaint.setColor(Color.BLACK);
            sBitmapPaint.setFilterBitmap(true);

            // overline configuration (null if we don't need it)
            float ovlWidthPx = getResources().getDimension(OVERLINE_WIDTH_RES);
            if (ovlWidthPx > 0.5) {
                sOverlineOutlinePaint = new Paint();
                sOverlineOutlinePaint.setStrokeWidth(ovlWidthPx);
                sOverlineOutlinePaint.setStyle(Paint.Style.STROKE);
            }

            // extras paint (anti-aliased)
            sExtrasPaint = new Paint();
            sExtrasPaint.setAntiAlias(true);
            sExtrasPaint.setColor(getResources().getColor(OVERLINE_COLOR_TRUSTED_RES));
            sExtrasPaint.setStrokeWidth(Math.max(ovlWidthPx, 1));
        }

        // change when clicked
        setClickable(true);
        // disable by default the long click
        setLongClickable(false);
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        mCurrentWidth = right - left;
        mCurrentHeight = bottom - top;

        // auto-determine the "TYPE_" from the physical size of the layout
        if (mForcedType == TYPE_AUTO) {
            if (mCurrentWidth < sMediumPxThreshold && mCurrentHeight < sMediumPxThreshold)
                mCurrentType = TYPE_SMALL;
            else if (mCurrentWidth < sLargePxThreshold && mCurrentHeight < sLargePxThreshold)
                mCurrentType = TYPE_MEDIUM;
            else
                mCurrentType = TYPE_LARGE;
        } else {
            // or use the forced one, if defined
            mCurrentType = mForcedType;
        }

        // set or remove the background (if the need changed!)
        boolean requiresBackground = mCurrentType >= TYPE_MEDIUM;
        if (requiresBackground && !mCurrentBackgroundDrawn) {
            // draw the background
            mCurrentBackgroundDrawn = true;

            // load the background just the first time, on demand (it may fail too)
            if (sBackgroundDrawable == null) {
                sBackgroundDrawable = getResources().getDrawable(BACKGROUND_DRAWABLE_RES);
                if (sBackgroundDrawable != null)
                    sBackgroundDrawable.getPadding(sBackgroundDrawablePadding);
            }

            // background -> padding
            mPaddingLeft = sBackgroundDrawablePadding.left;
            mPaddingTop = sBackgroundDrawablePadding.top;
            mPaddingRight = sBackgroundDrawablePadding.right;
            mPaddingBottom = sBackgroundDrawablePadding.bottom;
        } else if (!requiresBackground && mCurrentBackgroundDrawn) {
            // turn off background drawing
            mCurrentBackgroundDrawn = false;

            // no background -> no padding
            mPaddingLeft = 0;
            mPaddingTop = 0;
            mPaddingRight = 0;
            mPaddingBottom = 0;
        }

        // just proceed, do nothing here
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        // schedule a repaint to show pressed/released
        invalidate();
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        // schedule a repaint to show selected
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Selection State: make everything smaller
        if (isSelected()) {
            float scale = 0.8f;
            canvas.translate(mCurrentWidth * (1 - scale) / 2, mCurrentHeight * (1 - scale) / 2);
            canvas.scale(scale, scale);
        }

        // Pressed state: make the button reach the finger
        if (isPressed()) {
            float scale = 1.1f;
            canvas.translate(mCurrentWidth * (1 - scale) / 2, mCurrentHeight * (1 - scale) / 2);
            canvas.scale(scale, scale);
        }

        final int left = mPaddingLeft;
        final int top = mPaddingTop;
        final int right = mCurrentWidth - mPaddingRight;
        final int bottom = mCurrentHeight - mPaddingBottom;
        final int contentWidth = right - left;
        final int contentHeight = bottom - top;

        // A. the background drawable (if set)
        boolean requiresBackground = mCurrentBackgroundDrawn && sBackgroundDrawable != null
                && !isPressed() && !mFloating;
        if (requiresBackground) {
            sBackgroundDrawable.setBounds(0, 0, mCurrentWidth, mCurrentHeight);
            sBackgroundDrawable.draw(canvas);
        }

        // B. (when needed) draw the background rectangle; sharp our rounded
        boolean requiresFundamentalFiller = mCurrentType >= TYPE_LARGE && !mFloating;
        if (requiresFundamentalFiller) {
            // create the filler paint on demand (not all icons need it)
            if (mFundamentalPaint == null)
                mFundamentalPaint = createFundamentalPaint(mFaviconBitmap, mForcedFundamentalColor);

            // paint if not white, since requiresBackground already painted it white
            int fundamentalColor = mFundamentalPaint.getColor();
            if (fundamentalColor != COLOR_AUTO &&
                    (fundamentalColor != Color.WHITE || !requiresBackground)) {
                if (sRoundedRadius >= 1.) {
                    sRectF.set(left, top, right, bottom);
                    canvas.drawRoundRect(sRectF, sRoundedRadius, sRoundedRadius, mFundamentalPaint);
                } else
                    canvas.drawRect(left, top, right, bottom, mFundamentalPaint);
            }
        }

        // C. (if present) draw the favicon
        boolean requiresFavicon = mFaviconBitmap != null
                && mFaviconWidth > 1 && mFaviconHeight > 1;
        if (requiresFavicon) {
            // destination can either fill, or auto-center
            boolean fillSpace = mCurrentType <= TYPE_MEDIUM;
            if (fillSpace || contentWidth < sLargeFaviconPx || contentHeight < sLargeFaviconPx) {
                sDstRect.set(left, top, right, bottom);
            } else {
                int dstLeft = left + (contentWidth - sLargeFaviconPx) / 2;
                int dstTop = top + (contentHeight - sLargeFaviconPx) / 2;
                sDstRect.set(dstLeft, dstTop, dstLeft + sLargeFaviconPx, dstTop + sLargeFaviconPx);
            }

            // source has to 'crop proportionally' to keep the dest aspect ratio
            sSrcRect.set(0, 0, mFaviconWidth, mFaviconHeight);
            int sW = sSrcRect.width();
            int sH = sSrcRect.height();
            int dW = sDstRect.width();
            int dH = sDstRect.height();
            if (sW > 4 && sH > 4 && dW > 4 && dH > 4) {
                float hScale = (float) dW / (float) sW;
                float vScale = (float) dH / (float) sH;
                if (hScale == vScale) {
                    // no transformation needed, just zoom
                } else if (hScale < vScale) {
                    // horizontal crop
                    float hCrop = 1 - hScale / vScale;
                    int hCropPx = (int) (sW * hCrop / 2 + 0.5);
                    sSrcRect.left += hCropPx;
                    sSrcRect.right -= hCropPx;
                    canvas.drawBitmap(mFaviconBitmap, sSrcRect, sDstRect, sBitmapPaint);
                } else {
                    // vertical crop
                    float vCrop = 1 - vScale / hScale;
                    int vCropPx = (int) (sH * vCrop / 2 + 0.5);
                    sSrcRect.top += vCropPx;
                    sSrcRect.bottom -= vCropPx;
                }
            }

            // blit favicon, croppped, scaled
            canvas.drawBitmap(mFaviconBitmap, sSrcRect, sDstRect, sBitmapPaint);
        }

        // D. (when needed) draw the thin over-line
        if (requiresOverline()) {
            int colorRes;
            switch (mTrustLevel) {
                case TRUST_TRUSTED:
                    colorRes = OVERLINE_COLOR_TRUSTED_RES;
                    break;
                case TRUST_UNTRUSTED:
                    colorRes = OVERLINE_COLOR_UNTRUSTED_RES;
                    break;
                case TRUST_AVOID:
                    colorRes = OVERLINE_COLOR_AVOID_RES;
                    break;
                default:
                    colorRes = OVERLINE_COLOR_DEFAULT_RES;
                    break;
            }
            int lineColor = getResources().getColor(colorRes);
            if (lineColor != Color.TRANSPARENT) {
                // draw the white inline first
                boolean needSeparation = mTrustLevel != TRUST_UNKNOWN;
                if (needSeparation) {
                    sOverlineOutlinePaint.setColor(Color.WHITE);
                    float d = sOverlineOutlinePaint.getStrokeWidth();
                    canvas.drawRect(left + d, top + d, right - d, bottom - d, sOverlineOutlinePaint);
                }

                // then draw the outline
                sOverlineOutlinePaint.setColor(lineColor);
                canvas.drawRect(left, top, right, bottom, sOverlineOutlinePaint);
            }
        }

        // E. show extra, if requested
        if (mExtrasShown) {
            // as default, we show a bubble
            int eRad = Math.min(2 * contentWidth / 5, sMediumPxThreshold / 4);
            int eCX = Math.min(right - eRad / 2, mCurrentWidth - eRad); //left + (4 * contentWidth / 5) - eRad;
            int eCY = Math.min(bottom - eRad / 4, mCurrentHeight - eRad);

            // circle back
            //canvas.drawCircle(eCX, eCY, eRad, sExtrasPaint);

            // round rect back
            sRectF.set(eCX - eRad, eCY - eRad, eCX + eRad, eCY + eRad);
            sExtrasPaint.setStyle(Paint.Style.FILL);
            sExtrasPaint.setColor(0xff666666);
            canvas.drawRoundRect(sRectF, eRad / 2, eRad / 2, sExtrasPaint);

            // DEBUG! -- draw blocked count
            if (SHOW_EXTRAS_BLOCKED_COUNT && mExtraBlockedObjectsCount > 0) {
                final Paint paint = new Paint();
                float textSize = eRad * 1.2f;
                paint.setColor(Color.WHITE);
                paint.setAntiAlias(true);
                paint.setTextSize(textSize);
                String text = String.valueOf(mExtraBlockedObjectsCount);
                int textWidth = Math.round(paint.measureText(text) / 2);
                canvas.drawText(text, eCX - textWidth - 1, eCY + textSize / 3 + 1, paint);
            }

            // round rect stroke
            sExtrasPaint.setStyle(Paint.Style.STROKE);
            sExtrasPaint.setColor(0xFFeeeeee);
            canvas.drawRoundRect(sRectF, eRad / 2, eRad / 2, sExtrasPaint);
        }

        /*if (true) { // DEBUG TYPE
            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setTextSize(20);
            canvas.drawText(String.valueOf(mCurrentType), 30, 30, paint);
        }*/
    }

    private boolean requiresOverline() {
        return mCurrentType == TYPE_MEDIUM && sOverlineOutlinePaint != null && !mFloating;
    }


    /**
     * Creates a fill Paint from the favicon, or using the forced color (if not COLOR_AUTO)
     */
    private static Paint createFundamentalPaint(Bitmap favicon, int forceFillColor) {
        final Paint fillPaint = new Paint();
        if (forceFillColor != COLOR_AUTO)
            fillPaint.setColor(forceFillColor);
        else
            fillPaint.setColor(guessFundamentalColor(favicon));
        return fillPaint;
    }

    /**
     * This uses very stupid mechanism - a 9x9 grid sample on the borders and center - and selects
     * the color with the most frequency, or the center.
     *
     * @param bitmap the bitmap to guesss the color about
     * @return a Color
     */
    private static int guessFundamentalColor(Bitmap bitmap) {
        if (bitmap == null)
            return FILLER_FALLBACK_COLOR;
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        if (height < 2 || width < 2)
            return FILLER_FALLBACK_COLOR;

        // pick up to 9 colors
        // NOTE: the order of sampling sets the precendece, in case of ties
        int[] pxColors = new int[9];
        int idx = 0;
        if ((pxColors[idx] = sampleColor(bitmap, width / 2, height / 2)) != 0) idx++;
        if ((pxColors[idx] = sampleColor(bitmap, width / 2, height - 1)) != 0) idx++;
        if ((pxColors[idx] = sampleColor(bitmap, width - 1, height - 1)) != 0) idx++;
        if ((pxColors[idx] = sampleColor(bitmap, width - 1, height / 2)) != 0) idx++;
        if ((pxColors[idx] = sampleColor(bitmap,         0, 0         )) != 0) idx++;
        if ((pxColors[idx] = sampleColor(bitmap, width / 2, 0         )) != 0) idx++;
        if ((pxColors[idx] = sampleColor(bitmap, width - 1, 0         )) != 0) idx++;
        if ((pxColors[idx] = sampleColor(bitmap, 0        , height / 2)) != 0) idx++;
        if ((pxColors[idx] = sampleColor(bitmap, 0        , height - 1)) != 0) idx++;

        // find the most popular
        int popColor = -1;
        int popCount = -1;
        for (int i = 0; i < idx; i++) {
            int thisColor = pxColors[i];
            int thisCount = 0;
            for (int j = 0; j < idx; j++) {
                if (pxColors[j] == thisColor)
                    thisCount++;
            }
            if (thisCount > popCount) {
                popColor = thisColor;
                popCount = thisCount;
            }
        }
        return popCount > -1 ? popColor : FILLER_FALLBACK_COLOR;
    }

    /**
     * @return Color, but if it's 0, you should discard it (not representative)
     */
    private static int sampleColor(Bitmap bitmap, int x, int y) {
        final int color = bitmap.getPixel(x, y);

        // discard semi-transparent pixels, because they're probably from a spurious border
        //if ((color >>> 24) <= 128)
        //    return 0;

        // compose transparent pixels with white, since the BG will be white anyway
        final int alpha = Color.alpha(color);
        if (alpha == 0)
            return Color.WHITE;
        if (alpha < 255) {
            // perform simplified Porter-Duff source-over
            int dstContribution = 255 - alpha;
            return Color.argb(255,
                    ((alpha * Color.red(color)) >> 8) + dstContribution,
                    ((alpha * Color.green(color)) >> 8) + dstContribution,
                    ((alpha * Color.blue(color)) >> 8) + dstContribution
            );
        }

        // discard black pixels, because black is not a color (well, not a good looking one)
        if ((color & 0xFFFFFF) == 0)
            return 0;

        return color;
    }

}