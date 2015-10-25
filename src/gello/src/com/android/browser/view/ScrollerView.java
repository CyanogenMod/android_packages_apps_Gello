/*
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

package com.android.browser.view;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.FocusFinder;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.OverScroller;
import android.widget.TextView;

import java.util.List;

/**
 * Layout container for a view hierarchy that can be scrolled by the user,
 * allowing it to be larger than the physical display.  A ScrollView
 * is a {@link FrameLayout}, meaning you should place one child in it
 * containing the entire contents to scroll; this child may itself be a layout
 * manager with a complex hierarchy of objects.  A child that is often used
 * is a {@link LinearLayout} in a vertical orientation, presenting a vertical
 * array of top-level items that the user can scroll through.
 *
 * <p>The {@link TextView} class also
 * takes care of its own scrolling, so does not require a ScrollView, but
 * using the two together is possible to achieve the effect of a text view
 * within a larger container.
 *
 * <p>ScrollView only supports vertical scrolling.
 *
 * @attr ref android.R.styleable#ScrollView_fillViewport
 */
public class ScrollerView extends FrameLayout {
    static final int ANIMATED_SCROLL_GAP = 250;

    static final float MAX_SCROLL_FACTOR = 0.5f;

    private long mLastScroll;

    private final Rect mTempRect = new Rect();
    protected OverScroller mScroller;

    /**
     * Position of the last motion event.
     */
    private float mLastMotionY;

    /**
     * True when the layout has changed but the traversal has not come through yet.
     * Ideally the view hierarchy would keep track of this for us.
     */
    private boolean mIsLayoutDirty = true;

    /**
     * The child to give focus to in the event that a child has requested focus while the
     * layout is dirty. This prevents the scroll from being wrong if the child has not been
     * laid out before requesting focus.
     */
    protected View mChildToScrollTo = null;

    /**
     * True if the user is currently dragging this ScrollView around. This is
     * not the same as 'is being flinged', which can be checked by
     * mScroller.isFinished() (flinging begins when the user lifts his finger).
     */
    protected boolean mIsBeingDragged = false;

    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;

    /**
     * When set to true, the scroll view measure its child to make it fill the currently
     * visible area.
     */
    @ViewDebug.ExportedProperty(category = "layout")
    private boolean mFillViewport;

    /**
     * Whether arrow scrolling is animated.
     */
    private boolean mSmoothScrollingEnabled = true;

    private int mTouchSlop;
    protected int mMinimumVelocity;
    private int mMaximumVelocity;

    private int mOverscrollDistance;
    private int mOverflingDistance;

    /**
     * ID of the active pointer. This is used to retain consistency during
     * drags/flings if multiple pointers are used.
     */
    private int mActivePointerId = INVALID_POINTER;

    private static class ThreadSpanState {
        public Span mActiveHead;    // doubly-linked list.
        public int mActiveSize;
        public Span mFreeListHead;  // singly-linked list.  only changes at head.
        public int mFreeListSize;
    }

    public static class Span {
        private String mName;
        private long mCreateMillis;
        private Span mNext;
        private Span mPrev;  // not used when in freeList, only active
        private final ThreadSpanState mContainerState;

        Span(ThreadSpanState threadState) {
            mContainerState = threadState;
        }

        // Empty constructor for the NO_OP_SPAN
        protected Span() {
            mContainerState = null;
        }

        /**
         * To be called when the critical span is complete (i.e. the
         * animation is done animating).  This can be called on any
         * thread (even a different one from where the animation was
         * taking place), but that's only a defensive implementation
         * measure.  It really makes no sense for you to call this on
         * thread other than that where you created it.
         *
         * @hide
         */
        public void finish() {
            ThreadSpanState state = mContainerState;
            synchronized (state) {
                if (mName == null) {
                    // Duplicate finish call.  Ignore.
                    return;
                }

                // Remove ourselves from the active list.
                if (mPrev != null) {
                    mPrev.mNext = mNext;
                }
                if (mNext != null) {
                    mNext.mPrev = mPrev;
                }
                if (state.mActiveHead == this) {
                    state.mActiveHead = mNext;
                }

                state.mActiveSize--;

                this.mCreateMillis = -1;
                this.mName = null;
                this.mPrev = null;
                this.mNext = null;

                // Add ourselves to the freeList, if it's not already
                // too big.
                if (state.mFreeListSize < 5) {
                    this.mNext = state.mFreeListHead;
                    state.mFreeListHead = this;
                    state.mFreeListSize++;
                }
            }
        }
    }

    private static final Span NO_OP_SPAN = new Span() {
        public void finish() {
            // Do nothing.
        }
    };

    /**
     * The StrictMode "critical time span" objects to catch animation
     * stutters.  Non-null when a time-sensitive animation is
     * in-flight.  Must call finish() on them when done animating.
     * These are no-ops on user builds.
     */
    private Span mScrollStrictSpan = null;  // aka "drag"
    private Span mFlingStrictSpan = null;

    /**
     * Sentinel value for no current active pointer.
     * Used by {@link #mActivePointerId}.
     */
    private static final int INVALID_POINTER = -1;

    /**
     * orientation of the scrollview
     */
    protected boolean mHorizontal;

    protected boolean mIsOrthoDragged;
    private float mLastOrthoCoord;
    private View mDownView;
    private PointF mDownCoords;


    public ScrollerView(Context context) {
        this(context, null);
    }

    public ScrollerView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.scrollViewStyle);
    }

    public ScrollerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initScrollView();
        // SWE_TODO : Fix me
        /*
        TypedArray a =
            context.obtainStyledAttributes(attrs, R.styleable.ScrollView, defStyle, 0);
        setFillViewport(a.getBoolean(R.styleable.ScrollView_android_fillViewport, false));
        a.recycle();*/
        setFillViewport(false);
    }

    private void initScrollView() {
        mScroller = new OverScroller(getContext());
        setFocusable(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setWillNotDraw(false);
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mOverscrollDistance = configuration.getScaledOverscrollDistance();
        mOverflingDistance = configuration.getScaledOverflingDistance();
        mDownCoords = new PointF();
    }

    public void setOrientation(int orientation) {
        mHorizontal = (orientation == LinearLayout.HORIZONTAL);
        requestLayout();
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return true;
    }

    @Override
    protected float getTopFadingEdgeStrength() {
        if (getChildCount() == 0) {
            return 0.0f;
        }
        if (mHorizontal) {
            final int length = getHorizontalFadingEdgeLength();
            if (getScrollX() < length) {
                return getScrollX() / (float) length;
            }
        } else {
            final int length = getVerticalFadingEdgeLength();
            if (getScrollY() < length) {
                return getScrollY() / (float) length;
            }
        }
        return 1.0f;
    }

    @Override
    protected float getBottomFadingEdgeStrength() {
        if (getChildCount() == 0) {
            return 0.0f;
        }
        if (mHorizontal) {
            final int length = getHorizontalFadingEdgeLength();
            final int bottomEdge = getWidth() - getPaddingRight();
            final int span = getChildAt(0).getRight() - getScrollX() - bottomEdge;
            if (span < length) {
                return span / (float) length;
            }
        } else {
            final int length = getVerticalFadingEdgeLength();
            final int bottomEdge = getHeight() - getPaddingBottom();
            final int span = getChildAt(0).getBottom() - getScrollY() - bottomEdge;
            if (span < length) {
                return span / (float) length;
            }
        }
        return 1.0f;
    }

    /**
     * @return The maximum amount this scroll view will scroll in response to
     *   an arrow event.
     */
    public int getMaxScrollAmount() {
        return (int) (MAX_SCROLL_FACTOR * (mHorizontal
                ? (getRight() - getLeft()) : (getBottom() - getTop())));
    }


    @Override
    public void addView(View child) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }

        super.addView(child);
    }

    @Override
    public void addView(View child, int index) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }

        super.addView(child, index);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }

        super.addView(child, params);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }

        super.addView(child, index, params);
    }

    /**
     * @return Returns true this ScrollView can be scrolled
     */
    private boolean canScroll() {
        View child = getChildAt(0);
        if (child != null) {
            if (mHorizontal) {
                return getWidth() < child.getWidth() + getPaddingLeft() + getPaddingRight();
            } else {
                return getHeight() < child.getHeight() + getPaddingTop() + getPaddingBottom();
            }
        }
        return false;
    }

    /**
     * Indicates whether this ScrollView's content is stretched to fill the viewport.
     *
     * @return True if the content fills the viewport, false otherwise.
     *
     * @attr ref android.R.styleable#ScrollView_fillViewport
     */
    public boolean isFillViewport() {
        return mFillViewport;
    }

    /**
     * Indicates this ScrollView whether it should stretch its content height to fill
     * the viewport or not.
     *
     * @param fillViewport True to stretch the content's height to the viewport's
     *        boundaries, false otherwise.
     *
     * @attr ref android.R.styleable#ScrollView_fillViewport
     */
    public void setFillViewport(boolean fillViewport) {
        if (fillViewport != mFillViewport) {
            mFillViewport = fillViewport;
            requestLayout();
        }
    }

    /**
     * @return Whether arrow scrolling will animate its transition.
     */
    public boolean isSmoothScrollingEnabled() {
        return mSmoothScrollingEnabled;
    }

    /**
     * Set whether arrow scrolling will animate its transition.
     * @param smoothScrollingEnabled whether arrow scrolling will animate its transition
     */
    public void setSmoothScrollingEnabled(boolean smoothScrollingEnabled) {
        mSmoothScrollingEnabled = smoothScrollingEnabled;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (!mFillViewport) {
            return;
        }

        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == MeasureSpec.UNSPECIFIED) {
            return;
        }

        if (getChildCount() > 0) {
            final View child = getChildAt(0);
            if (mHorizontal) {
                int width = getMeasuredWidth();
                if (child.getMeasuredWidth() < width) {
                    final FrameLayout.LayoutParams lp = (LayoutParams) child
                            .getLayoutParams();

                    int childHeightMeasureSpec = getChildMeasureSpec(
                            heightMeasureSpec, getPaddingTop() + getPaddingBottom(),
                            lp.height);
                    width -= getPaddingLeft();
                    width -= getPaddingRight();
                    int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                            width, MeasureSpec.EXACTLY);

                    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
                }
            } else {
                int height = getMeasuredHeight();
                if (child.getMeasuredHeight() < height) {
                    final FrameLayout.LayoutParams lp = (LayoutParams) child
                            .getLayoutParams();

                    int childWidthMeasureSpec = getChildMeasureSpec(
                            widthMeasureSpec, getPaddingLeft() + getPaddingRight(),
                            lp.width);
                    height -= getPaddingTop();
                    height -= getPaddingBottom();
                    int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                            height, MeasureSpec.EXACTLY);

                    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
                }
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Let the focused view and/or our descendants get the key first
        return super.dispatchKeyEvent(event) || executeKeyEvent(event);
    }

    /**
     * You can call this function yourself to have the scroll view perform
     * scrolling from a key event, just as if the event had been dispatched to
     * it by the view hierarchy.
     *
     * @param event The key event to execute.
     * @return Return true if the event was handled, else false.
     */
    public boolean executeKeyEvent(KeyEvent event) {
        mTempRect.setEmpty();

        if (!canScroll()) {
            if (isFocused() && event.getKeyCode() != KeyEvent.KEYCODE_BACK) {
                View currentFocused = findFocus();
                if (currentFocused == this) currentFocused = null;
                View nextFocused = FocusFinder.getInstance().findNextFocus(this,
                        currentFocused, View.FOCUS_DOWN);
                return nextFocused != null
                        && nextFocused != this
                        && nextFocused.requestFocus(View.FOCUS_DOWN);
            }
            return false;
        }

        boolean handled = false;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (!event.isAltPressed()) {
                        handled = arrowScroll(View.FOCUS_UP);
                    } else {
                        handled = fullScroll(View.FOCUS_UP);
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (!event.isAltPressed()) {
                        handled = arrowScroll(View.FOCUS_DOWN);
                    } else {
                        handled = fullScroll(View.FOCUS_DOWN);
                    }
                    break;
                case KeyEvent.KEYCODE_SPACE:
                    pageScroll(event.isShiftPressed() ? View.FOCUS_UP : View.FOCUS_DOWN);
                    break;
            }
        }

        return handled;
    }

    private boolean inChild(int x, int y) {
        if (getChildCount() > 0) {
            final int scrollY = getScrollY();
            final View child = getChildAt(0);
            return !(y < child.getTop() - scrollY
                    || y >= child.getBottom() - scrollY
                    || x < child.getLeft()
                    || x >= child.getRight());
        }
        return false;
    }

    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (disallowIntercept) {
            recycleVelocityTracker();
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onMotionEvent will be called and we do the actual
         * scrolling there.
         */

        /*
         * Shortcut the most recurring case: the user is in the dragging state
         * and he is moving his finger. We want to intercept this motion.
         */
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged)) {
            return true;
        }
        if ((action == MotionEvent.ACTION_MOVE) && (mIsOrthoDragged)) {
            return true;
        }
        switch (action & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_MOVE: {
            /*
             * mIsBeingDragged == false, otherwise the shortcut would have
             * caught it. Check whether the user has moved far enough from his
             * original down touch.
             */

            /*
             * Locally do absolute value. mLastMotionY is set to the y value of
             * the down event.
             */
            final int activePointerId = mActivePointerId;
            if (activePointerId == INVALID_POINTER) {
                // If we don't have a valid id, the touch down wasn't on
                // content.
                break;
            }

            final int pointerIndex = ev.findPointerIndex(activePointerId);
            final float y = mHorizontal ? ev.getX(pointerIndex) : ev
                    .getY(pointerIndex);
            final int yDiff = (int) Math.abs(y - mLastMotionY);
            if (yDiff > mTouchSlop) {
                mIsBeingDragged = true;
                mLastMotionY = y;
                initVelocityTrackerIfNotExists();
                mVelocityTracker.addMovement(ev);
                if (mScrollStrictSpan == null) {
                    /*mScrollStrictSpan = StrictMode
                            .enterCriticalSpan("ScrollView-scroll");*/
                    mScrollStrictSpan = NO_OP_SPAN;
                }
            } else {
                final float ocoord = mHorizontal ? ev.getY(pointerIndex) : ev
                        .getX(pointerIndex);
                if (Math.abs(ocoord - mLastOrthoCoord) > mTouchSlop) {
                    mIsOrthoDragged = true;
                    mLastOrthoCoord = ocoord;
                    initVelocityTrackerIfNotExists();
                    mVelocityTracker.addMovement(ev);
                }
            }
            break;
        }

        case MotionEvent.ACTION_DOWN: {
            final float y = mHorizontal ? ev.getX() : ev.getY();
            mDownCoords.x = ev.getX();
            mDownCoords.y = ev.getY();
            if (!inChild((int) ev.getX(), (int) ev.getY())) {
                mIsBeingDragged = false;
                recycleVelocityTracker();
                break;
            }

            /*
             * Remember location of down touch. ACTION_DOWN always refers to
             * pointer index 0.
             */
            mLastMotionY = y;
            mActivePointerId = ev.getPointerId(0);

            initOrResetVelocityTracker();
            mVelocityTracker.addMovement(ev);
            /*
             * If being flinged and user touches the screen, initiate drag;
             * otherwise don't. mScroller.isFinished should be false when being
             * flinged.
             */
            mIsBeingDragged = !mScroller.isFinished();
            if (mIsBeingDragged && mScrollStrictSpan == null) {
                /*mScrollStrictSpan = StrictMode
                        .enterCriticalSpan("ScrollView-scroll");*/
                mScrollStrictSpan = NO_OP_SPAN;
            }
            mIsOrthoDragged = false;
            final float ocoord = mHorizontal ? ev.getY() : ev.getX();
            mLastOrthoCoord = ocoord;
            mDownView = findViewAt((int) ev.getX(), (int) ev.getY());
            break;
        }

        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_UP:
            /* Release the drag */
            mIsBeingDragged = false;
            mIsOrthoDragged = false;
            mActivePointerId = INVALID_POINTER;
            recycleVelocityTracker();
            if (mScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0,
                    getScrollRange())) {
                invalidate();
            }
            break;
        case MotionEvent.ACTION_POINTER_UP:
            onSecondaryPointerUp(ev);
            break;
        }

        /*
         * The only time we want to intercept motion events is if we are in the
         * drag mode.
         */
        return mIsBeingDragged || mIsOrthoDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        initVelocityTrackerIfNotExists();
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                mIsBeingDragged = getChildCount() != 0;
                if (!mIsBeingDragged) {
                    return false;
                }

                /*
                 * If being flinged and user touches, stop the fling. isFinished
                 * will be false if being flinged.
                 */
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                    if (mFlingStrictSpan != null) {
                        mFlingStrictSpan.finish();
                        mFlingStrictSpan = null;
                    }
                }

                // Remember where the motion event started
                mLastMotionY = mHorizontal ? ev.getX() : ev.getY();
                mActivePointerId = ev.getPointerId(0);
                break;
            }
            case MotionEvent.ACTION_MOVE:
                if (mIsOrthoDragged) {
                    // Scroll to follow the motion event
                    final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                    final float x = ev.getX(activePointerIndex);
                    final float y = ev.getY(activePointerIndex);
                    if (isOrthoMove(x - mDownCoords.x, y - mDownCoords.y)) {
                        onOrthoDrag(mDownView, mHorizontal
                                ? y - mDownCoords.y
                                : x - mDownCoords.x);
                    }
                } else if (mIsBeingDragged) {
                    // Scroll to follow the motion event
                    final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                    final float y = mHorizontal ? ev.getX(activePointerIndex)
                            : ev.getY(activePointerIndex);
                    final int deltaY = (int) (mLastMotionY - y);
                    mLastMotionY = y;

                    final int oldX = getScrollX();
                    final int oldY = getScrollY();
                    final int range = getScrollRange();
                    if (mHorizontal) {
                        if (overScrollBy(deltaY, 0, getScrollX(), 0, range, 0,
                                mOverscrollDistance, 0, true)) {
                            // Break our velocity if we hit a scroll barrier.
                            mVelocityTracker.clear();
                        }
                    } else {
                        if (overScrollBy(0, deltaY, 0, getScrollY(), 0, range,
                                0, mOverscrollDistance, true)) {
                            // Break our velocity if we hit a scroll barrier.
                            mVelocityTracker.clear();
                        }
                    }
                    onScrollChanged(getScrollX(), getScrollY(), oldX, oldY);

                    final int overscrollMode = getOverScrollMode();
                    if (overscrollMode == OVER_SCROLL_ALWAYS ||
                            (overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && range > 0)) {
                        final int pulledToY = mHorizontal ? oldX + deltaY : oldY + deltaY;
                        if (pulledToY < 0) {
                            onPull(pulledToY);
                        } else if (pulledToY > range) {
                            onPull(pulledToY - range);
                        } else {
                            onPull(0);
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                final VelocityTracker vtracker = mVelocityTracker;
                vtracker.computeCurrentVelocity(1000, mMaximumVelocity);
                if (isOrthoMove(vtracker.getXVelocity(mActivePointerId),
                        vtracker.getYVelocity(mActivePointerId))
                        && mMinimumVelocity < Math.abs((mHorizontal ? vtracker.getYVelocity()
                                : vtracker.getXVelocity()))) {
                    onOrthoFling(mDownView, mHorizontal ? vtracker.getYVelocity()
                            : vtracker.getXVelocity());
                    break;
                }
                if (mIsOrthoDragged) {
                    onOrthoDragFinished(mDownView);
                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                } else if (mIsBeingDragged) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = mHorizontal
                            ? (int) velocityTracker.getXVelocity(mActivePointerId)
                            : (int) velocityTracker.getYVelocity(mActivePointerId);

                    if (getChildCount() > 0) {
                        if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
                            fling(-initialVelocity);
                        } else {
                            final int bottom = getScrollRange();
                            if (mHorizontal) {
                                if (mScroller.springBack(getScrollX(), getScrollY(), 0,
                                                         bottom, 0, 0)) {
                                    invalidate();
                                }
                            } else {
                                if (mScroller.springBack(getScrollX(), getScrollY(), 0,
                                                         0, 0, bottom)) {
                                    invalidate();
                                }
                            }
                        }
                        onPull(0);
                    }

                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsOrthoDragged) {
                    onOrthoDragFinished(mDownView);
                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                } else if (mIsBeingDragged && getChildCount() > 0) {
                    if (mHorizontal) {
                        if (mScroller.springBack(getScrollX(), getScrollY(), 0,
                                                 getScrollRange(), 0, 0)) {
                            invalidate();
                        }
                    } else {
                        if (mScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0,
                                                 getScrollRange())) {
                            invalidate();
                        }
                    }
                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN: {
                final int index = ev.getActionIndex();
                final float y = mHorizontal ? ev.getX(index) : ev.getY(index);
                mLastMotionY = y;
                mLastOrthoCoord = mHorizontal ? ev.getY(index) : ev.getX(index);
                mActivePointerId = ev.getPointerId(index);
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                mLastMotionY = mHorizontal
                        ? ev.getX(ev.findPointerIndex(mActivePointerId))
                        : ev.getY(ev.findPointerIndex(mActivePointerId));
                break;
        }
        return true;
    }

    protected View findViewAt(int x, int y) {
        // subclass responsibility
        return null;
    }

    protected void onPull(int delta) {
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >>
                MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            // TODO: Make this decision more intelligent.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionY = mHorizontal ? ev.getX(newPointerIndex) : ev.getY(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
            mLastOrthoCoord = mHorizontal ? ev.getY(newPointerIndex)
                    : ev.getX(newPointerIndex);
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_CLASS_POINTER) != 0) {
            switch (event.getAction()) {
            case MotionEvent.ACTION_SCROLL: {
                if (!mIsBeingDragged) {
                    if (mHorizontal) {
                        final float hscroll = event
                                .getAxisValue(MotionEvent.AXIS_HSCROLL);
                        if (hscroll != 0) {
                            /* SWE_TODO : - disruptive getHorizontalScrollFactor()*/
                            final int delta = (int) (hscroll * 10);
                            final int range = getScrollRange();
                            int oldScrollX = getScrollX();
                            int newScrollX = oldScrollX - delta;
                            if (newScrollX < 0) {
                                newScrollX = 0;
                            } else if (newScrollX > range) {
                                newScrollX = range;
                            }
                            if (newScrollX != oldScrollX) {
                                super.scrollTo(newScrollX, getScrollY());
                                return true;
                            }
                        }
                    } else {
                        final float vscroll = event
                                .getAxisValue(MotionEvent.AXIS_VSCROLL);
                        if (vscroll != 0) {
                            /* SWE_TODO : - disruptive getVerticalScrollFactor()*/
                            final int delta = (int) (vscroll * 10);
                            final int range = getScrollRange();
                            int oldScrollY = getScrollY();
                            int newScrollY = oldScrollY - delta;
                            if (newScrollY < 0) {
                                newScrollY = 0;
                            } else if (newScrollY > range) {
                                newScrollY = range;
                            }
                            if (newScrollY != oldScrollY) {
                                super.scrollTo(getScrollX(), newScrollY);
                                return true;
                            }
                        }
                    }
                }
            }
            }
        }
        return super.onGenericMotionEvent(event);
    }

    protected void onOrthoDrag(View draggedView, float distance) {
    }

    protected void onOrthoDragFinished(View draggedView) {
    }

    protected void onOrthoFling(View draggedView, float velocity) {
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY,
            boolean clampedX, boolean clampedY) {
        // Treat animating scrolls differently; see #computeScroll() for why.
        if (!mScroller.isFinished()) {
            setScrollX(scrollX);
            setScrollY(scrollY);
            if (isHardwareAccelerated() && getParent() instanceof View) {
                ((View) getParent()).invalidate();
            }
            if (mHorizontal && clampedX) {
                mScroller.springBack(getScrollX(), getScrollY(), 0, getScrollRange(), 0, 0);
            } else if (!mHorizontal && clampedY) {
                mScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0, getScrollRange());
            }
        } else {
            super.scrollTo(scrollX, scrollY);
        }
        awakenScrollBars();
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setScrollable(true);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setScrollable(true);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        // Do not append text content to scroll events they are fired frequently
        // and the client has already received another event type with the text.
        if (event.getEventType() != AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            super.dispatchPopulateAccessibilityEvent(event);
        }
        return false;
    }

    private int getScrollRange() {
        int scrollRange = 0;
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            if (mHorizontal) {
                scrollRange = Math.max(0,
                        child.getWidth() - (getWidth() - getPaddingRight() - getPaddingLeft()));
            } else {
                scrollRange = Math.max(0,
                        child.getHeight() - (getHeight() - getPaddingBottom() - getPaddingTop()));
            }
        }
        return scrollRange;
    }

    /**
     * <p>
     * Finds the next focusable component that fits in this View's bounds
     * (excluding fading edges) pretending that this View's top is located at
     * the parameter top.
     * </p>
     *
     * @param topFocus           look for a candidate at the top of the bounds if topFocus is true,
     *                           or at the bottom of the bounds if topFocus is false
     * @param top                the top offset of the bounds in which a focusable must be
     *                           found (the fading edge is assumed to start at this position)
     * @param preferredFocusable the View that has highest priority and will be
     *                           returned if it is within my bounds (null is valid)
     * @return the next focusable component in the bounds or null if none can be found
     */
    private View findFocusableViewInMyBounds(final boolean topFocus,
            final int top, View preferredFocusable) {
        /*
         * The fading edge's transparent side should be considered for focus
         * since it's mostly visible, so we divide the actual fading edge length
         * by 2.
         */
        final int fadingEdgeLength = (mHorizontal
                ? getHorizontalFadingEdgeLength()
                : getVerticalFadingEdgeLength()) / 2;
        final int topWithoutFadingEdge = top + fadingEdgeLength;
        final int bottomWithoutFadingEdge = top + (mHorizontal ? getWidth() : getHeight()) - fadingEdgeLength;

        if ((preferredFocusable != null)
                && ((mHorizontal ? preferredFocusable.getLeft() : preferredFocusable.getTop())
                        < bottomWithoutFadingEdge)
                && ((mHorizontal ? preferredFocusable.getRight() : preferredFocusable.getBottom()) > topWithoutFadingEdge)) {
            return preferredFocusable;
        }

        return findFocusableViewInBounds(topFocus, topWithoutFadingEdge,
                bottomWithoutFadingEdge);
    }

    /**
     * <p>
     * Finds the next focusable component that fits in the specified bounds.
     * </p>
     *
     * @param topFocus look for a candidate is the one at the top of the bounds
     *                 if topFocus is true, or at the bottom of the bounds if topFocus is
     *                 false
     * @param top      the top offset of the bounds in which a focusable must be
     *                 found
     * @param bottom   the bottom offset of the bounds in which a focusable must
     *                 be found
     * @return the next focusable component in the bounds or null if none can
     *         be found
     */
    private View findFocusableViewInBounds(boolean topFocus, int top, int bottom) {

        List<View> focusables = getFocusables(View.FOCUS_FORWARD);
        View focusCandidate = null;

        /*
         * A fully contained focusable is one where its top is below the bound's
         * top, and its bottom is above the bound's bottom. A partially
         * contained focusable is one where some part of it is within the
         * bounds, but it also has some part that is not within bounds.  A fully contained
         * focusable is preferred to a partially contained focusable.
         */
        boolean foundFullyContainedFocusable = false;

        int count = focusables.size();
        for (int i = 0; i < count; i++) {
            View view = focusables.get(i);
            int viewTop = mHorizontal ? view.getLeft() : view.getTop();
            int viewBottom = mHorizontal ? view.getRight() : view.getBottom();

            if (top < viewBottom && viewTop < bottom) {
                /*
                 * the focusable is in the target area, it is a candidate for
                 * focusing
                 */

                final boolean viewIsFullyContained = (top < viewTop) &&
                        (viewBottom < bottom);

                if (focusCandidate == null) {
                    /* No candidate, take this one */
                    focusCandidate = view;
                    foundFullyContainedFocusable = viewIsFullyContained;
                } else {
                    final int ctop = mHorizontal ? focusCandidate.getLeft() : focusCandidate.getTop();
                    final int cbot = mHorizontal ? focusCandidate.getRight() : focusCandidate.getBottom();
                    final boolean viewIsCloserToBoundary =
                            (topFocus && viewTop < ctop) ||
                                    (!topFocus && viewBottom > cbot);

                    if (foundFullyContainedFocusable) {
                        if (viewIsFullyContained && viewIsCloserToBoundary) {
                            /*
                             * We're dealing with only fully contained views, so
                             * it has to be closer to the boundary to beat our
                             * candidate
                             */
                            focusCandidate = view;
                        }
                    } else {
                        if (viewIsFullyContained) {
                            /* Any fully contained view beats a partially contained view */
                            focusCandidate = view;
                            foundFullyContainedFocusable = true;
                        } else if (viewIsCloserToBoundary) {
                            /*
                             * Partially contained view beats another partially
                             * contained view if it's closer
                             */
                            focusCandidate = view;
                        }
                    }
                }
            }
        }

        return focusCandidate;
    }

    // i was here

    /**
     * <p>Handles scrolling in response to a "page up/down" shortcut press. This
     * method will scroll the view by one page up or down and give the focus
     * to the topmost/bottommost component in the new visible area. If no
     * component is a good candidate for focus, this scrollview reclaims the
     * focus.</p>
     *
     * @param direction the scroll direction: {@link android.view.View#FOCUS_UP}
     *                  to go one page up or
     *                  {@link android.view.View#FOCUS_DOWN} to go one page down
     * @return true if the key event is consumed by this method, false otherwise
     */
    public boolean pageScroll(int direction) {
        boolean down = direction == View.FOCUS_DOWN;
        int height = getHeight();

        if (down) {
            mTempRect.top = getScrollY() + height;
            int count = getChildCount();
            if (count > 0) {
                View view = getChildAt(count - 1);
                if (mTempRect.top + height > view.getBottom()) {
                    mTempRect.top = view.getBottom() - height;
                }
            }
        } else {
            mTempRect.top = getScrollY() - height;
            if (mTempRect.top < 0) {
                mTempRect.top = 0;
            }
        }
        mTempRect.bottom = mTempRect.top + height;

        return scrollAndFocus(direction, mTempRect.top, mTempRect.bottom);
    }

    /**
     * <p>Handles scrolling in response to a "home/end" shortcut press. This
     * method will scroll the view to the top or bottom and give the focus
     * to the topmost/bottommost component in the new visible area. If no
     * component is a good candidate for focus, this scrollview reclaims the
     * focus.</p>
     *
     * @param direction the scroll direction: {@link android.view.View#FOCUS_UP}
     *                  to go the top of the view or
     *                  {@link android.view.View#FOCUS_DOWN} to go the bottom
     * @return true if the key event is consumed by this method, false otherwise
     */
    public boolean fullScroll(int direction) {
        boolean down = direction == View.FOCUS_DOWN;
        int height = getHeight();

        mTempRect.top = 0;
        mTempRect.bottom = height;

        if (down) {
            int count = getChildCount();
            if (count > 0) {
                View view = getChildAt(count - 1);
                mTempRect.bottom = view.getBottom() + getPaddingBottom();
                mTempRect.top = mTempRect.bottom - height;
            }
        }

        return scrollAndFocus(direction, mTempRect.top, mTempRect.bottom);
    }

    /**
     * <p>Scrolls the view to make the area defined by <code>top</code> and
     * <code>bottom</code> visible. This method attempts to give the focus
     * to a component visible in this area. If no component can be focused in
     * the new visible area, the focus is reclaimed by this ScrollView.</p>
     *
     * @param direction the scroll direction: {@link android.view.View#FOCUS_UP}
     *                  to go upward, {@link android.view.View#FOCUS_DOWN} to downward
     * @param top       the top offset of the new area to be made visible
     * @param bottom    the bottom offset of the new area to be made visible
     * @return true if the key event is consumed by this method, false otherwise
     */
    private boolean scrollAndFocus(int direction, int top, int bottom) {
        boolean handled = true;

        int height = getHeight();
        int containerTop = getScrollY();
        int containerBottom = containerTop + height;
        boolean up = direction == View.FOCUS_UP;

        View newFocused = findFocusableViewInBounds(up, top, bottom);
        if (newFocused == null) {
            newFocused = this;
        }

        if (top >= containerTop && bottom <= containerBottom) {
            handled = false;
        } else {
            int delta = up ? (top - containerTop) : (bottom - containerBottom);
            doScrollY(delta);
        }

        if (newFocused != findFocus()) newFocused.requestFocus(direction);

        return handled;
    }

    /**
     * Handle scrolling in response to an up or down arrow click.
     *
     * @param direction The direction corresponding to the arrow key that was
     *                  pressed
     * @return True if we consumed the event, false otherwise
     */
    public boolean arrowScroll(int direction) {

        View currentFocused = findFocus();
        if (currentFocused == this) currentFocused = null;

        View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused, direction);

        final int maxJump = getMaxScrollAmount();

        if (nextFocused != null && isWithinDeltaOfScreen(nextFocused, maxJump, getHeight())) {
            nextFocused.getDrawingRect(mTempRect);
            offsetDescendantRectToMyCoords(nextFocused, mTempRect);
            int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(mTempRect);
            doScrollY(scrollDelta);
            nextFocused.requestFocus(direction);
        } else {
            // no new focus
            int scrollDelta = maxJump;

            if (direction == View.FOCUS_UP && getScrollY() < scrollDelta) {
                scrollDelta = getScrollY();
            } else if (direction == View.FOCUS_DOWN) {
                if (getChildCount() > 0) {
                    int daBottom = getChildAt(0).getBottom();
                    int screenBottom = getScrollY() + getHeight() - getPaddingBottom();
                    if (daBottom - screenBottom < maxJump) {
                        scrollDelta = daBottom - screenBottom;
                    }
                }
            }
            if (scrollDelta == 0) {
                return false;
            }
            doScrollY(direction == View.FOCUS_DOWN ? scrollDelta : -scrollDelta);
        }

        if (currentFocused != null && currentFocused.isFocused()
                && isOffScreen(currentFocused)) {
            // previously focused item still has focus and is off screen, give
            // it up (take it back to ourselves)
            // (also, need to temporarily force FOCUS_BEFORE_DESCENDANTS so we are
            // sure to
            // get it)
            final int descendantFocusability = getDescendantFocusability();  // save
            setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
            requestFocus();
            setDescendantFocusability(descendantFocusability);  // restore
        }
        return true;
    }

    private boolean isOrthoMove(float moveX, float moveY) {
        return mHorizontal && Math.abs(moveY) > Math.abs(moveX)
                || !mHorizontal && Math.abs(moveX) > Math.abs(moveY);
    }

    /**
     * @return whether the descendant of this scroll view is scrolled off
     *  screen.
     */
    private boolean isOffScreen(View descendant) {
        if (mHorizontal) {
            return !isWithinDeltaOfScreen(descendant, getWidth(), 0);
        } else {
            return !isWithinDeltaOfScreen(descendant, 0, getHeight());
        }
    }

    /**
     * @return whether the descendant of this scroll view is within delta
     *  pixels of being on the screen.
     */
    private boolean isWithinDeltaOfScreen(View descendant, int delta, int height) {
        descendant.getDrawingRect(mTempRect);
        offsetDescendantRectToMyCoords(descendant, mTempRect);
        if (mHorizontal) {
            return (mTempRect.right + delta) >= getScrollX()
            && (mTempRect.left - delta) <= (getScrollX() + height);
        } else {
            return (mTempRect.bottom + delta) >= getScrollY()
            && (mTempRect.top - delta) <= (getScrollY() + height);
        }
    }

    /**
     * Smooth scroll by a Y delta
     *
     * @param delta the number of pixels to scroll by on the Y axis
     */
    private void doScrollY(int delta) {
        if (delta != 0) {
            if (mSmoothScrollingEnabled) {
                if (mHorizontal) {
                    smoothScrollBy(0, delta);
                } else {
                    smoothScrollBy(delta, 0);
                }
            } else {
                if (mHorizontal) {
                    scrollBy(0, delta);
                } else {
                    scrollBy(delta, 0);
                }
            }
        }
    }

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param dx the number of pixels to scroll by on the X axis
     * @param dy the number of pixels to scroll by on the Y axis
     */
    public final void smoothScrollBy(int dx, int dy) {
        if (getChildCount() == 0) {
            // Nothing to do.
            return;
        }
        long duration = AnimationUtils.currentAnimationTimeMillis() - mLastScroll;
        if (duration > ANIMATED_SCROLL_GAP) {
            if (mHorizontal) {
                final int width = getWidth() - getPaddingRight() - getPaddingLeft();
                final int right = getChildAt(0).getWidth();
                final int maxX = Math.max(0, right - width);
                final int scrollX = getScrollX();
                dx = Math.max(0, Math.min(scrollX + dx, maxX)) - scrollX;
                mScroller.startScroll(scrollX, getScrollY(), dx, 0);
            } else {
                final int height = getHeight() - getPaddingBottom() - getPaddingTop();
                final int bottom = getChildAt(0).getHeight();
                final int maxY = Math.max(0, bottom - height);
                final int scrollY = getScrollY();
                dy = Math.max(0, Math.min(scrollY + dy, maxY)) - scrollY;
                mScroller.startScroll(getScrollX(), scrollY, 0, dy);
            }
            invalidate();
        } else {
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
                if (mFlingStrictSpan != null) {
                    mFlingStrictSpan.finish();
                    mFlingStrictSpan = null;
                }
            }
            scrollBy(dx, dy);
        }
        mLastScroll = AnimationUtils.currentAnimationTimeMillis();
    }

    /**
     * Like {@link #scrollTo}, but scroll smoothly instead of immediately.
     *
     * @param x the position where to scroll on the X axis
     * @param y the position where to scroll on the Y axis
     */
    public final void smoothScrollTo(int x, int y) {
        smoothScrollBy(x - getScrollX(), y - getScrollY());
    }

    /**
     * <p>
     * The scroll range of a scroll view is the overall height of all of its
     * children.
     * </p>
     */
    @Override
    protected int computeVerticalScrollRange() {
        if (mHorizontal) {
            return super.computeVerticalScrollRange();
        }
        final int count = getChildCount();
        final int contentHeight = getHeight() - getPaddingBottom() - getPaddingTop();
        if (count == 0) {
            return contentHeight;
        }

        int scrollRange = getChildAt(0).getBottom();
        final int scrollY = getScrollY();
        final int overscrollBottom = Math.max(0, scrollRange - contentHeight);
        if (scrollY < 0) {
            scrollRange -= scrollY;
        } else if (scrollY > overscrollBottom) {
            scrollRange += scrollY - overscrollBottom;
        }

        return scrollRange;
    }

    /**
     * <p>
     * The scroll range of a scroll view is the overall height of all of its
     * children.
     * </p>
     */
    @Override
    protected int computeHorizontalScrollRange() {
        if (!mHorizontal) {
            return super.computeHorizontalScrollRange();
        }
        final int count = getChildCount();
        final int contentWidth = getWidth() - getPaddingRight() - getPaddingLeft();
        if (count == 0) {
            return contentWidth;
        }

        int scrollRange = getChildAt(0).getRight();
        final int scrollX = getScrollX();
        final int overscrollBottom = Math.max(0, scrollRange - contentWidth);
        if (scrollX < 0) {
            scrollRange -= scrollX;
        } else if (scrollX > overscrollBottom) {
            scrollRange += scrollX - overscrollBottom;
        }

        return scrollRange;
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return Math.max(0, super.computeVerticalScrollOffset());
    }

    @Override
    protected int computeHorizontalScrollOffset() {
        return Math.max(0, super.computeHorizontalScrollOffset());
    }

    @Override
    protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
        ViewGroup.LayoutParams lp = child.getLayoutParams();

        int childWidthMeasureSpec;
        int childHeightMeasureSpec;

        if (mHorizontal) {
            childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec, getPaddingTop()
                    + getPaddingBottom(), lp.height);

            childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        } else {
            childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec, getPaddingLeft()
                    + getPaddingRight(), lp.width);

            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed,
            int parentHeightMeasureSpec, int heightUsed) {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        int childWidthMeasureSpec;
        int childHeightMeasureSpec;
        if (mHorizontal) {
            childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                    getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin
                            + heightUsed, lp.height);
            childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                    lp.leftMargin + lp.rightMargin, MeasureSpec.UNSPECIFIED);
        } else {
            childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                    getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin
                            + widthUsed, lp.width);
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                    lp.topMargin + lp.bottomMargin, MeasureSpec.UNSPECIFIED);
        }
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            // This is called at drawing time by ViewGroup.  We don't want to
            // re-show the scrollbars at this point, which scrollTo will do,
            // so we replicate most of scrollTo here.
            //
            //         It's a little odd to call onScrollChanged from inside the drawing.
            //
            //         It is, except when you remember that computeScroll() is used to
            //         animate scrolling. So unless we want to defer the onScrollChanged()
            //         until the end of the animated scrolling, we don't really have a
            //         choice here.
            //
            //         I agree.  The alternative, which I think would be worse, is to post
            //         something and tell the subclasses later.  This is bad because there
            //         will be a window where getScrollX()/Y is different from what the app
            //         thinks it is.
            //
            int oldX = getScrollX();
            int oldY = getScrollY();
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();

            if (oldX != x || oldY != y) {
                if (mHorizontal) {
                    overScrollBy(x - oldX, y - oldY, oldX, oldY, getScrollRange(), 0,
                            mOverflingDistance, 0, false);
                } else {
                    overScrollBy(x - oldX, y - oldY, oldX, oldY, 0, getScrollRange(),
                            0, mOverflingDistance, false);
                }
                onScrollChanged(getScrollX(), getScrollY(), oldX, oldY);
            }
            awakenScrollBars();

            // Keep on drawing until the animation has finished.
            postInvalidate();
        } else {
            if (mFlingStrictSpan != null) {
                mFlingStrictSpan.finish();
                mFlingStrictSpan = null;
            }
        }
    }

    /**
     * Scrolls the view to the given child.
     *
     * @param child the View to scroll to
     */
    private void scrollToChild(View child) {
        child.getDrawingRect(mTempRect);

        /* Offset from child's local coordinates to ScrollView coordinates */
        offsetDescendantRectToMyCoords(child, mTempRect);
        scrollToChildRect(mTempRect, true);
    }

    /**
     * If rect is off screen, scroll just enough to get it (or at least the
     * first screen size chunk of it) on screen.
     *
     * @param rect      The rectangle.
     * @param immediate True to scroll immediately without animation
     * @return true if scrolling was performed
     */
    private boolean scrollToChildRect(Rect rect, boolean immediate) {
        final int delta = computeScrollDeltaToGetChildRectOnScreen(rect);
        final boolean scroll = delta != 0;
        if (scroll) {
            if (immediate) {
                if (mHorizontal) {
                    scrollBy(delta, 0);
                } else {
                    scrollBy(0, delta);
                }
            } else {
                if (mHorizontal) {
                    smoothScrollBy(delta, 0);
                } else {
                    smoothScrollBy(0, delta);
                }
            }
        }
        return scroll;
    }

    /**
     * Compute the amount to scroll in the Y direction in order to get
     * a rectangle completely on the screen (or, if taller than the screen,
     * at least the first screen size chunk of it).
     *
     * @param rect The rect.
     * @return The scroll delta.
     */
    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        if (mHorizontal) {
            return computeScrollDeltaToGetChildRectOnScreenHorizontal(rect);
        } else {
            return computeScrollDeltaToGetChildRectOnScreenVertical(rect);
        }
    }

    private int computeScrollDeltaToGetChildRectOnScreenVertical(Rect rect) {
        if (getChildCount() == 0) return 0;

        int height = getHeight();
        int screenTop = getScrollY();
        int screenBottom = screenTop + height;

        int fadingEdge = getVerticalFadingEdgeLength();

        // leave room for top fading edge as long as rect isn't at very top
        if (rect.top > 0) {
            screenTop += fadingEdge;
        }

        // leave room for bottom fading edge as long as rect isn't at very bottom
        if (rect.bottom < getChildAt(0).getHeight()) {
            screenBottom -= fadingEdge;
        }

        int scrollYDelta = 0;

        if (rect.bottom > screenBottom && rect.top > screenTop) {
            // need to move down to get it in view: move down just enough so
            // that the entire rectangle is in view (or at least the first
            // screen size chunk).

            if (rect.height() > height) {
                // just enough to get screen size chunk on
                scrollYDelta += (rect.top - screenTop);
            } else {
                // get entire rect at bottom of screen
                scrollYDelta += (rect.bottom - screenBottom);
            }

            // make sure we aren't scrolling beyond the end of our content
            int bottom = getChildAt(0).getBottom();
            int distanceToBottom = bottom - screenBottom;
            scrollYDelta = Math.min(scrollYDelta, distanceToBottom);

        } else if (rect.top < screenTop && rect.bottom < screenBottom) {
            // need to move up to get it in view: move up just enough so that
            // entire rectangle is in view (or at least the first screen
            // size chunk of it).

            if (rect.height() > height) {
                // screen size chunk
                scrollYDelta -= (screenBottom - rect.bottom);
            } else {
                // entire rect at top
                scrollYDelta -= (screenTop - rect.top);
            }

            // make sure we aren't scrolling any further than the top our content
            scrollYDelta = Math.max(scrollYDelta, -getScrollY());
        }
        return scrollYDelta;
    }

    private int computeScrollDeltaToGetChildRectOnScreenHorizontal(Rect rect) {
        if (getChildCount() == 0) return 0;

        int width = getWidth();
        int screenLeft = getScrollX();
        int screenRight = screenLeft + width;

        int fadingEdge = getHorizontalFadingEdgeLength();

        // leave room for left fading edge as long as rect isn't at very left
        if (rect.left > 0) {
            screenLeft += fadingEdge;
        }

        // leave room for right fading edge as long as rect isn't at very right
        if (rect.right < getChildAt(0).getWidth()) {
            screenRight -= fadingEdge;
        }

        int scrollXDelta = 0;

        if (rect.right > screenRight && rect.left > screenLeft) {
            // need to move right to get it in view: move right just enough so
            // that the entire rectangle is in view (or at least the first
            // screen size chunk).

            if (rect.width() > width) {
                // just enough to get screen size chunk on
                scrollXDelta += (rect.left - screenLeft);
            } else {
                // get entire rect at right of screen
                scrollXDelta += (rect.right - screenRight);
            }

            // make sure we aren't scrolling beyond the end of our content
            int right = getChildAt(0).getRight();
            int distanceToRight = right - screenRight;
            scrollXDelta = Math.min(scrollXDelta, distanceToRight);

        } else if (rect.left < screenLeft && rect.right < screenRight) {
            // need to move right to get it in view: move right just enough so that
            // entire rectangle is in view (or at least the first screen
            // size chunk of it).

            if (rect.width() > width) {
                // screen size chunk
                scrollXDelta -= (screenRight - rect.right);
            } else {
                // entire rect at left
                scrollXDelta -= (screenLeft - rect.left);
            }

            // make sure we aren't scrolling any further than the left our content
            scrollXDelta = Math.max(scrollXDelta, -getScrollX());
        }
        return scrollXDelta;
    }


    @Override
    public void requestChildFocus(View child, View focused) {
        if (!mIsLayoutDirty) {
            scrollToChild(focused);
        } else {
            // The child may not be laid out yet, we can't compute the scroll yet
            mChildToScrollTo = focused;
        }
        super.requestChildFocus(child, focused);
    }


    /**
     * When looking for focus in children of a scroll view, need to be a little
     * more careful not to give focus to something that is scrolled off screen.
     *
     * This is more expensive than the default {@link android.view.ViewGroup}
     * implementation, otherwise this behavior might have been made the default.
     */
    @Override
    protected boolean onRequestFocusInDescendants(int direction,
            Rect previouslyFocusedRect) {

        // convert from forward / backward notation to up / down / left / right
        // (ugh).
        if (mHorizontal) {
            if (direction == View.FOCUS_FORWARD) {
                direction = View.FOCUS_RIGHT;
            } else if (direction == View.FOCUS_BACKWARD) {
                direction = View.FOCUS_LEFT;
            }
        } else {
            if (direction == View.FOCUS_FORWARD) {
                direction = View.FOCUS_DOWN;
            } else if (direction == View.FOCUS_BACKWARD) {
                direction = View.FOCUS_UP;
            }
        }

        final View nextFocus = previouslyFocusedRect == null ?
                FocusFinder.getInstance().findNextFocus(this, null, direction) :
                FocusFinder.getInstance().findNextFocusFromRect(this,
                        previouslyFocusedRect, direction);

        if (nextFocus == null) {
            return false;
        }

        if (isOffScreen(nextFocus)) {
            return false;
        }

        return nextFocus.requestFocus(direction, previouslyFocusedRect);
    }

    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rectangle,
            boolean immediate) {
        // offset into coordinate space of this scroll view
        rectangle.offset(child.getLeft() - child.getScrollX(),
                child.getTop() - child.getScrollY());

        return scrollToChildRect(rectangle, immediate);
    }

    @Override
    public void requestLayout() {
        mIsLayoutDirty = true;
        super.requestLayout();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mScrollStrictSpan != null) {
            mScrollStrictSpan.finish();
            mScrollStrictSpan = null;
        }
        if (mFlingStrictSpan != null) {
            mFlingStrictSpan.finish();
            mFlingStrictSpan = null;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mIsLayoutDirty = false;
        // Give a child focus if it needs it
        if (mChildToScrollTo != null && isViewDescendantOf(mChildToScrollTo, this)) {
            scrollToChild(mChildToScrollTo);
        }
        mChildToScrollTo = null;

        // Calling this with the present values causes it to re-clam them
        scrollTo(getScrollX(), getScrollY());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        View currentFocused = findFocus();
        if (null == currentFocused || this == currentFocused)
            return;

        // If the currently-focused view was visible on the screen when the
        // screen was at the old height, then scroll the screen to make that
        // view visible with the new screen height.
        if (isWithinDeltaOfScreen(currentFocused, 0, oldh)) {
            currentFocused.getDrawingRect(mTempRect);
            offsetDescendantRectToMyCoords(currentFocused, mTempRect);
            int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(mTempRect);
            doScrollY(scrollDelta);
        }
    }

    /**
     * Return true if child is an descendant of parent, (or equal to the parent).
     */
    private boolean isViewDescendantOf(View child, View parent) {
        if (child == parent) {
            return true;
        }

        final ViewParent theParent = child.getParent();
        return (theParent instanceof ViewGroup) && isViewDescendantOf((View) theParent, parent);
    }

    /**
     * Fling the scroll view
     *
     * @param velocityY The initial velocity in the Y direction. Positive
     *                  numbers mean that the finger/cursor is moving down the screen,
     *                  which means we want to scroll towards the top.
     */
    public void fling(int velocityY) {
        if (getChildCount() > 0) {
            if (mHorizontal) {
                int width = getWidth() - getPaddingRight() - getPaddingLeft();
                int right = getChildAt(0).getWidth();

                mScroller.fling(getScrollX(), getScrollY(), velocityY, 0,
                        0, Math.max(0, right - width), 0, 0, width/2, 0);
            } else {
                int height = getHeight() - getPaddingBottom() - getPaddingTop();
                int bottom = getChildAt(0).getHeight();

                mScroller.fling(getScrollX(), getScrollY(), 0, velocityY, 0, 0, 0,
                        Math.max(0, bottom - height), 0, height/2);
            }
            if (mFlingStrictSpan == null) {
                //mFlingStrictSpan = StrictMode.enterCriticalSpan("ScrollView-fling");
                mFlingStrictSpan = NO_OP_SPAN;
            }

            invalidate();
        }
    }

    private void endDrag() {
        mIsBeingDragged = false;
        mIsOrthoDragged = false;
        mDownView = null;
        recycleVelocityTracker();
        if (mScrollStrictSpan != null) {
            mScrollStrictSpan.finish();
            mScrollStrictSpan = null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This version also clamps the scrolling to the bounds of our child.
     */
    @Override
    public void scrollTo(int x, int y) {
        // we rely on the fact the View.scrollBy calls scrollTo.
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            x = clamp(x, getWidth() - getPaddingRight() - getPaddingLeft(), child.getWidth());
            y = clamp(y, getHeight() - getPaddingBottom() - getPaddingTop(), child.getHeight());
            if (x != getScrollX() || y != getScrollY()) {
                super.scrollTo(x, y);
            }
        }
    }

    private int clamp(int n, int my, int child) {
        if (my >= child || n < 0) {
            /* my >= child is this case:
             *                    |--------------- me ---------------|
             *     |------ child ------|
             * or
             *     |--------------- me ---------------|
             *            |------ child ------|
             * or
             *     |--------------- me ---------------|
             *                                  |------ child ------|
             *
             * n < 0 is this case:
             *     |------ me ------|
             *                    |-------- child --------|
             *     |-- getScrollX() --|
             */
            return 0;
        }
        if ((my+n) > child) {
            /* this case:
             *                    |------ me ------|
             *     |------ child ------|
             *     |-- getScrollX() --|
             */
            return child-my;
        }
        return n;
    }

}
