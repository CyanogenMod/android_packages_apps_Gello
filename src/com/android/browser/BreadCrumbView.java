/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import com.android.browser.R;

/**
 * Simple bread crumb view
 * Use setController to receive callbacks from user interactions
 * Use pushView, popView, clear, and getTopData to change/access the view stack
 */
public class BreadCrumbView extends RelativeLayout implements OnClickListener {
    private static final int DIVIDER_PADDING = 12; // dips
    private static final int CRUMB_PADDING = 8; // dips

    public interface Controller {
        public void onTop(BreadCrumbView view, int level, Object data);
    }

    private ImageButton mBackButton;
    private LinearLayout mCrumbLayout;
    private LinearLayout mBackLayout;
    private Controller mController;
    private List<Crumb> mCrumbs;
    private boolean mUseBackButton;
    private Drawable mSeparatorDrawable;
    private float mDividerPadding;
    private int mMaxVisible = -1;
    private Context mContext;
    private int mCrumbPadding;
    private TextView mOverflowView;

    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public BreadCrumbView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    /**
     * @param context
     * @param attrs
     */
    public BreadCrumbView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * @param context
     */
    public BreadCrumbView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context ctx) {
        mContext = ctx;
        setFocusable(true);
        setGravity(Gravity.CENTER_VERTICAL);
        mUseBackButton = false;
        mCrumbs = new ArrayList<Crumb>();
        mSeparatorDrawable = ctx.getResources().getDrawable(
                                android.R.drawable.divider_horizontal_dark);
        float density = mContext.getResources().getDisplayMetrics().density;
        mDividerPadding = DIVIDER_PADDING * density;
        mCrumbPadding = (int) (CRUMB_PADDING * density);
        addCrumbLayout();
        addBackLayout();
    }

    public void setUseBackButton(boolean useflag) {
        mUseBackButton = useflag;
        updateVisible();
    }

    public void setController(Controller ctl) {
        mController = ctl;
    }

    public int getMaxVisible() {
        return mMaxVisible;
    }

    public void setMaxVisible(int max) {
        mMaxVisible = max;
        updateVisible();
    }

    public int getTopLevel() {
        return mCrumbs.size();
    }

    public Object getTopData() {
        Crumb c = getTopCrumb();
        if (c != null) {
            return c.data;
        }
        return null;
    }

    public int size() {
        return mCrumbs.size();
    }

    public void clear() {
        while (mCrumbs.size() > 1) {
            pop(false);
        }
        pop(true);
    }

    public void notifyController() {
        if (mController != null) {
            if (mCrumbs.size() > 0) {
                mController.onTop(this, mCrumbs.size(), getTopCrumb().data);
            } else {
                mController.onTop(this, 0, null);
            }
        }
    }

    public View pushView(String name, Object data) {
        return pushView(name, true, data);
    }

    public View pushView(String name, boolean canGoBack, Object data) {
        Crumb crumb = new Crumb(name, canGoBack, data);
        pushCrumb(crumb);
        return crumb.crumbView;
    }

    public void addOverflowLabel(TextView view) {
        mOverflowView = view;
        if (view != null) {
            view.setTextAppearance(mContext, R.style.BookmarkPathText);
            view.setPadding(mCrumbPadding, 0, mCrumbPadding, 0);
            view.setGravity(Gravity.CENTER_VERTICAL);
            view.setText("... >");
        }
    }

    public void pushView(View view, Object data) {
        Crumb crumb = new Crumb(view, true, data);
        pushCrumb(crumb);
    }

    public void popView() {
        pop(true);
    }

    private void addBackButton() {
        mBackButton = new ImageButton(mContext);
        mBackButton.setImageResource(R.drawable.icon_up);
        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(
                android.R.attr.selectableItemBackground, outValue, true);
        int resid = outValue.resourceId;
        mBackButton.setBackgroundResource(resid);
        mBackButton.setPadding(mCrumbPadding, 0, mCrumbPadding, 0);
        mBackButton.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT));
        mBackButton.setOnClickListener(this);
        mBackButton.setContentDescription(mContext.getText(
                R.string.accessibility_button_bookmarks_folder_up));
        mBackLayout.addView(mBackButton);
    }

    private void addParentLabel() {
        TextView tv = new TextView(mContext);
        tv.setTextAppearance(mContext, android.R.style.TextAppearance_Medium);
        tv.setPadding(mCrumbPadding, 0, 0, 0);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));
        tv.setText("/ .../");
        tv.setSingleLine();
        tv.setVisibility(View.GONE);
        mCrumbLayout.addView(tv);
    }

    private void addCrumbLayout() {
        mCrumbLayout = new LinearLayout(mContext);
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        params.addRule(ALIGN_PARENT_LEFT, TRUE);
        params.setMargins(0, 0, 4 * mCrumbPadding, 0);
        mCrumbLayout.setLayoutParams(params);
        mCrumbLayout.setVisibility(View.VISIBLE);
        //addParentLabel();
        addView(mCrumbLayout);
    }

    private void addBackLayout() {
        mBackLayout= new LinearLayout(mContext);
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        params.addRule(ALIGN_PARENT_RIGHT, TRUE);
        mBackLayout.setLayoutParams(params);
        mBackLayout.setVisibility(View.GONE);
        addSeparator();
        addBackButton();
        addView(mBackLayout);
    }

    private void pushCrumb(Crumb crumb) {
        mCrumbs.add(crumb);
        mCrumbLayout.addView(crumb.crumbView);
        updateVisible();
        crumb.crumbView.setOnClickListener(this);
    }

    private void addSeparator() {
        View sep = makeDividerView();
        sep.setLayoutParams(makeDividerLayoutParams());
        mBackLayout.addView(sep);
    }

    private ImageView makeDividerView() {
        ImageView result = new ImageView(mContext);
        result.setImageDrawable(mSeparatorDrawable);
        result.setScaleType(ImageView.ScaleType.FIT_XY);
        return result;
    }

    private LinearLayout.LayoutParams makeDividerLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        return params;
    }

    private void pop(boolean notify) {
        int n = mCrumbs.size();
        if (n > 0) {
            removeLastView();
            mCrumbs.remove(n - 1);
            if (mUseBackButton) {
                Crumb top = getTopCrumb();
                if (top != null && top.canGoBack) {
                    mBackLayout.setVisibility(View.VISIBLE);
                } else {
                    mBackLayout.setVisibility(View.GONE);
                }
            }
            updateVisible();
            if (notify) {
                notifyController();
            }
        }
    }

    private void updateVisible() {
        // start at index 1 (0 == parent label)
        int childIndex = 0;
        if (mMaxVisible >= 0) {
            int invisibleCrumbs = size() - mMaxVisible;
            if (invisibleCrumbs > 0) {
                int crumbIndex = 0;
                if (mOverflowView != null) {
                    mOverflowView.setVisibility(VISIBLE);
                    mOverflowView.setOnClickListener(this);
                }
                while (crumbIndex < invisibleCrumbs) {
                    // Set the crumb to GONE.
                    mCrumbLayout.getChildAt(childIndex).setVisibility(View.GONE);
                    childIndex++;
                    // Move to the next crumb.
                    crumbIndex++;
                }
            } else {
                if (mOverflowView != null) {
                    mOverflowView.setVisibility(GONE);
                }
            }
            // Make sure the last is visible.
            int childCount = mCrumbLayout.getChildCount();
            while (childIndex < childCount) {
                mCrumbLayout.getChildAt(childIndex).setVisibility(View.VISIBLE);
                childIndex++;
            }
        } else {
            int count = getChildCount();
            for (int i = childIndex; i < count ; i++) {
                getChildAt(i).setVisibility(View.VISIBLE);
            }
        }
        if (mUseBackButton) {
            boolean canGoBack = getTopCrumb() != null ? getTopCrumb().canGoBack : false;
            mBackLayout.setVisibility(canGoBack ? View.VISIBLE : View.GONE);
            if (canGoBack) {
                mCrumbLayout.getChildAt(0).setVisibility(VISIBLE);
            } else {
                mCrumbLayout.getChildAt(0).setVisibility(GONE);
            }
        } else {
            mBackLayout.setVisibility(View.GONE);
        }
    }

    private void removeLastView() {
        int ix = mCrumbLayout.getChildCount();
        if (ix > 0) {
            mCrumbLayout.removeViewAt(ix-1);
        }
    }

    Crumb getTopCrumb() {
        Crumb crumb = null;
        if (mCrumbs.size() > 0) {
            crumb = mCrumbs.get(mCrumbs.size() - 1);
        }
        return crumb;
    }

    @Override
    public void onClick(View v) {
        if (mBackButton == v) {
            popView();
            notifyController();
        } else if (mOverflowView == v) {
            int maxVisible = getMaxVisible();
            while (maxVisible > 0) {
                pop(false);
                maxVisible--;
            }
            notifyController();
        } else {
            // pop until view matches crumb view
            while (v != getTopCrumb().crumbView) {
                pop(false);
            }
            notifyController();
        }
    }
    @Override
    public int getBaseline() {
        int ix = getChildCount();
        if (ix > 0) {
            // If there is at least one crumb, the baseline will be its
            // baseline.
            return getChildAt(ix-1).getBaseline();
        }
        return super.getBaseline();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int height = mSeparatorDrawable.getIntrinsicHeight();
        if (getMeasuredHeight() < height) {
            // This should only be an issue if there are currently no separators
            // showing; i.e. if there is one crumb and no back button.
            int mode = View.MeasureSpec.getMode(heightMeasureSpec);
            switch(mode) {
                case View.MeasureSpec.AT_MOST:
                    if (View.MeasureSpec.getSize(heightMeasureSpec) < height) {
                        return;
                    }
                    break;
                case View.MeasureSpec.EXACTLY:
                    return;
                default:
                    break;
            }
            setMeasuredDimension(getMeasuredWidth(), height);
        }
    }

    class Crumb {

        public View crumbView;
        public boolean canGoBack;
        public Object data;

        public Crumb(String title, boolean backEnabled, Object tag) {
            init(makeCrumbView(title), backEnabled, tag);
        }

        public Crumb(View view, boolean backEnabled, Object tag) {
            init(view, backEnabled, tag);
        }

        private void init(View view, boolean back, Object tag) {
            canGoBack = back;
            crumbView = view;
            data = tag;
        }

        private TextView makeCrumbView(String name) {
            TextView tv = new TextView(mContext);
            tv.setTextAppearance(mContext, R.style.BookmarkPathText);
            tv.setPadding(mCrumbPadding, 0, mCrumbPadding, 0);
            tv.setGravity(Gravity.CENTER_VERTICAL);
            tv.setText(name + " >");
            tv.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.MATCH_PARENT));
            tv.setSingleLine();
            tv.setEllipsize(TextUtils.TruncateAt.END);
            return tv;
        }

    }

}
