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
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Patterns;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.android.browser.SuggestionsAdapter.CompletionListener;
import com.android.browser.SuggestionsAdapter.SuggestItem;
import com.android.browser.reflect.ReflectHelper;
import com.android.browser.search.SearchEngine;
import com.android.browser.search.SearchEngineInfo;
import com.android.browser.search.SearchEngines;

/**
 * url/search input view
 * handling suggestions
 */
public class UrlInputView extends AutoCompleteTextView
        implements OnEditorActionListener,
        CompletionListener, OnItemClickListener, TextWatcher {

    static final String TYPED = "browser-type";
    static final String SUGGESTED = "browser-suggest";
    static final String LATIN_INPUTMETHOD_PACKAGE_NAME = "com.android.inputmethod.latin";

    static final int POST_DELAY = 100;
    static final int URL_MAX_LENGTH = 2048;

    static interface StateListener {
        static final int STATE_NORMAL = 0;
        static final int STATE_HIGHLIGHTED = 1;
        static final int STATE_EDITED = 2;

        public void onStateChanged(int state);
    }

    private UrlInputListener   mListener;
    private InputMethodManager mInputManager;
    private SuggestionsAdapter mAdapter;
    private View mContainer;
    private boolean mLandscape;
    private boolean mIncognitoMode;
    private boolean mNeedsUpdate;
    private Context mContext;

    private int mState;
    private StateListener mStateListener;

    public UrlInputView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public UrlInputView(Context context, AttributeSet attrs) {
        // SWE_TODO : Needs Fix
        //this(context, attrs, R.attr.autoCompleteTextViewStyle);
        this(context, attrs, 0);
    }

    public UrlInputView(Context context) {
        this(context, null);
    }

    private void init(Context ctx) {
        mInputManager = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
        setOnEditorActionListener(this);
        mAdapter = new SuggestionsAdapter(ctx, this);
        setAdapter(mAdapter);
        setSelectAllOnFocus(true);
        onConfigurationChanged(ctx.getResources().getConfiguration());
        setThreshold(1);
        setOnItemClickListener(this);
        mNeedsUpdate = false;
        addTextChangedListener(this);
        setDropDownAnchor(R.id.taburlbar);
        mState = StateListener.STATE_NORMAL;
        mContext = ctx;

        this.setFilters(new InputFilter[] {
            new UrlLengthFilter(URL_MAX_LENGTH)
        });
    }

    protected void onFocusChanged(boolean focused, int direction, Rect prevRect) {
        super.onFocusChanged(focused, direction, prevRect);
        int state = -1;
        if (focused) {
            if (hasSelection()) {
                state = StateListener.STATE_HIGHLIGHTED;
            } else {
                state = StateListener.STATE_EDITED;
            }
        } else {
            // reset the selection state
            state = StateListener.STATE_NORMAL;
        }
        final int s = state;
        post(new Runnable() {
            public void run() {
                changeState(s);
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent evt) {
        boolean hasSelection = hasSelection();
        boolean res = super.onTouchEvent(evt);
        if ((MotionEvent.ACTION_DOWN == evt.getActionMasked())
              && hasSelection) {
            postDelayed(new Runnable() {
                public void run() {
                    changeState(StateListener.STATE_EDITED);
                }}, POST_DELAY);
        }
        return res;
    }

    /**
     * check if focus change requires a title bar update
     */
    boolean needsUpdate() {
        return mNeedsUpdate;
    }

    /**
     * clear the focus change needs title bar update flag
     */
    void clearNeedsUpdate() {
        mNeedsUpdate = false;
    }

    void setController(UiController controller) {
        UrlSelectionActionMode urlSelectionMode
                = new UrlSelectionActionMode(controller);
        setCustomSelectionActionModeCallback(urlSelectionMode);
    }

    void setContainer(View container) {
        mContainer = container;
    }

    public void setUrlInputListener(UrlInputListener listener) {
        mListener = listener;
    }

    public void setStateListener(StateListener listener) {
        mStateListener = listener;
        // update listener
        changeState(mState);
    }

    private void changeState(int newState) {
        mState = newState;
        if (mStateListener != null) {
            mStateListener.onStateChanged(mState);
        }
    }

    int getState() {
        return mState;
    }

    @Override
    protected void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        mLandscape = (config.orientation &
                Configuration.ORIENTATION_LANDSCAPE) != 0;
        mAdapter.setLandscapeMode(mLandscape);
        if (isPopupShowing() && (getVisibility() == View.VISIBLE)) {
            dismissDropDown();
            showDropDown();
            performFiltering(getText(), 0);
        }
    }

    @Override
    public void dismissDropDown() {
        super.dismissDropDown();
        mAdapter.clearCache();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        finishInput(getText().toString(), null, TYPED);
        return true;
    }

    void forceFilter() {
        showDropDown();
    }

    void hideIME() {
        mInputManager.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    void showIME() {
        //mInputManager.focusIn(this);
        Object[] params  = {this};
        Class[] type = new Class[] {View.class};
        ReflectHelper.invokeMethod(mInputManager, "focusIn", type, params);
        mInputManager.showSoftInput(this, 0);
    }

    private void finishInput(String url, String extra, String source) {
        mNeedsUpdate = true;
        dismissDropDown();
        mInputManager.hideSoftInputFromWindow(getWindowToken(), 0);
        if (TextUtils.isEmpty(url)) {
            mListener.onDismiss();
        } else {
            if (mIncognitoMode && isSearch(url)) {
                // To prevent logging, intercept this request
                // TODO: This is a quick hack, refactor this
                SearchEngine searchEngine = BrowserSettings.getInstance()
                        .getSearchEngine();
                if (searchEngine == null) return;
                SearchEngineInfo engineInfo = SearchEngines
                        .getSearchEngineInfo(getContext(), searchEngine.getName());
                if (engineInfo == null) return;
                url = engineInfo.getSearchUriForQuery(url);
                // mLister.onAction can take it from here without logging
            }
            mListener.onAction(url, extra, source);
        }
    }

    boolean isSearch(String inUrl) {
        String url = UrlUtils.fixUrl(inUrl).trim();
        if (TextUtils.isEmpty(url)) return false;

        if (Patterns.WEB_URL.matcher(url).matches()
                || UrlUtils.ACCEPTED_URI_SCHEMA.matcher(url).matches()) {
            return false;
        }
        return true;
    }

    // Completion Listener

    @Override
    public void onSearch(String search) {
        mListener.onCopySuggestion(search);
    }

    @Override
    public void onSelect(String url, int type, String extra) {
        finishInput(url, extra, SUGGESTED);
    }

    @Override
    public void onItemClick(
            AdapterView<?> parent, View view, int position, long id) {
        SuggestItem item = mAdapter.getItem(position);
        onSelect(SuggestionsAdapter.getSuggestionUrl(item), item.type, item.extra);
    }

    interface UrlInputListener {

        public void onDismiss();

        public void onAction(String text, String extra, String source);

        public void onCopySuggestion(String text);

    }

    public void setIncognitoMode(boolean incognito) {
        mIncognitoMode = incognito;
        mAdapter.setIncognitoMode(mIncognitoMode);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent evt) {
        if (keyCode == KeyEvent.KEYCODE_ESCAPE && !isInTouchMode()) {
            finishInput(null, null, null);
            return true;
        }
        return super.onKeyDown(keyCode, evt);
    }

    public SuggestionsAdapter getAdapter() {
        return mAdapter;
    }

    /*
     * no-op to prevent scrolling of webview when embedded titlebar
     * gets edited
     */
    @Override
    public boolean requestRectangleOnScreen(Rect rect, boolean immediate) {
        return false;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (StateListener.STATE_HIGHLIGHTED == mState ||
            StateListener.STATE_EDITED == mState) {
            changeState(StateListener.STATE_EDITED);
        }
    }

    @Override
    public void afterTextChanged(Editable s) { }

    /**
     * It will prompt the toast if the text length greater than the given length.
     */
    private class UrlLengthFilter extends LengthFilter {
        public UrlLengthFilter(int max) {
            super(max);
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                int dstart, int dend) {
            CharSequence result = super.filter(source, start, end, dest, dstart, dend);
            if (result != null) {
                // If the result is not null, it means the text length is greater than
                // the given length. We will prompt the toast to alert the user.
                CharSequence alert = getResources().getString(R.string.max_url_character_limit_msg);
                Toast t = Toast.makeText(mContext , alert, Toast.LENGTH_SHORT);
                t.setGravity(Gravity.CENTER, 0, 0);
                t.show();
            }
            return result;
        }
    }

}
