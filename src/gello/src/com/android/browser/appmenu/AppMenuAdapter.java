// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.android.browser.appmenu;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.TextView;

import org.chromium.base.ApiCompatibilityUtils;
import com.android.browser.R;
import org.chromium.ui.base.LocalizationUtils;
import org.chromium.ui.interpolators.BakedBezierInterpolator;

import java.util.List;

/**
 * ListAdapter to customize the view of items in the list.
 */
class AppMenuAdapter extends BaseAdapter {
    private static final int VIEW_TYPE_COUNT = 9;

    /**
     * Regular Android menu item that contains a title and an icon if icon is specified.
     */
    private static final int STANDARD_MENU_ITEM = 0;
    /**
     * Menu item that has two buttons, the first one is a title and the second one is an icon.
     * It is different from the regular menu item because it contains two separate buttons.
     */
    private static final int TITLE_BUTTON_MENU_ITEM = 1;
    /**
     * Menu item that has one button plus menu button. Every one of these buttons is displayed as an icon.
     */
    private static final int ONE_BUTTON_PLUS_MENU_ITEM = 2;
    /**
     * Menu item that has two buttons. Every one of these buttons is displayed as an icon.
     */
    private static final int TWO_BUTTON_MENU_ITEM = 3;
    /**
     * Menu item that has two buttons plus menu. Every one of these buttons is displayed as an icon.
     */
    private static final int TWO_BUTTON_PLUS_MENU_ITEM = 4;
    /**
     * Menu item that has three buttons. Every one of these buttons is displayed as an icon.
     */
    private static final int THREE_BUTTON_MENU_ITEM = 5;
    /**
     * Menu item that has three buttons plus menu. Every one of these buttons is displayed as an icon.
     */
    private static final int THREE_BUTTON_PLUS_MENU_ITEM = 6;
    /**
     * Menu item that has four buttons. Every one of these buttons is displayed as an icon.
     */
    private static final int FOUR_BUTTON_MENU_ITEM = 7;
    /**
     * Menu item that has two buttons, the first one is a title and the second is a menu icon.
     * This is similar to {@link #TITLE_BUTTON_MENU_ITEM} but has some slight layout differences.
     */
    private static final int MENU_BUTTON_MENU_ITEM = 8;

    /** MenuItem Animation Constants */
    private static final int ENTER_ITEM_DURATION_MS = 350;
    private static final int ENTER_ITEM_BASE_DELAY_MS = 80;
    private static final int ENTER_ITEM_ADDL_DELAY_MS = 30;
    private static final float ENTER_STANDARD_ITEM_OFFSET_Y_DP = -10.f;
    private static final float ENTER_STANDARD_ITEM_OFFSET_X_DP = 10.f;

    /** Menu Button Layout Constants */
    private static final float MENU_BUTTON_WIDTH_DP = 59.f;
    private static final float MENU_BUTTON_START_PADDING_DP = 21.f;

    private final AppMenu mAppMenu;
    private final LayoutInflater mInflater;
    private final List<MenuItem> mMenuItems;
    private final int mNumMenuItems;
    private final boolean mShowMenuButton;
    private final float mDpToPx;

    public AppMenuAdapter(AppMenu appMenu, List<MenuItem> menuItems, LayoutInflater inflater,
            boolean showMenuButton) {
        mAppMenu = appMenu;
        mMenuItems = menuItems;
        mInflater = inflater;
        mNumMenuItems = menuItems.size();
        mShowMenuButton = showMenuButton;
        mDpToPx = inflater.getContext().getResources().getDisplayMetrics().density;
    }

    @Override
    public int getCount() {
        return mNumMenuItems;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        MenuItem item = getItem(position);
        boolean hasMenuButton = mShowMenuButton && position == 0;
        int viewCount = item.hasSubMenu() ? item.getSubMenu().size() : 1;

        if (viewCount == 4) {
            return FOUR_BUTTON_MENU_ITEM;
        } else if (viewCount == 3) {
            if (hasMenuButton) {
                return THREE_BUTTON_PLUS_MENU_ITEM;
            }
            return THREE_BUTTON_MENU_ITEM;
        } else if (viewCount == 2) {
            if (hasMenuButton) {
                return TWO_BUTTON_PLUS_MENU_ITEM;
            }
            return TWO_BUTTON_MENU_ITEM;
        } else if (hasMenuButton) {
            return ONE_BUTTON_PLUS_MENU_ITEM;
        }
        return STANDARD_MENU_ITEM;
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getItemId();
    }

    @Override
    public MenuItem getItem(int position) {
        if (position == ListView.INVALID_POSITION) return null;
        assert position >= 0;
        assert position < mMenuItems.size();
        return mMenuItems.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final boolean hasMenuButton = mShowMenuButton && position == 0;
        final MenuItem item = getItem(position);
        switch (getItemViewType(position)) {
            case STANDARD_MENU_ITEM: {
                StandardMenuItemViewHolder holder = null;
                if (convertView == null) {
                    holder = new StandardMenuItemViewHolder();
                    convertView = mInflater.inflate(R.layout.swe_menu_item, parent, false);
                    holder.text = (TextView) convertView.findViewById(R.id.menu_item_text);
                    holder.image = (AppMenuItemIcon) convertView.findViewById(R.id.menu_item_icon);
                    holder.checkbox = (CheckBox) convertView.findViewById(R.id.menu_item_checkbox);
                    convertView.setTag(holder);
                    convertView.setTag(R.id.menu_item_enter_anim_id,
                            buildStandardItemEnterAnimator(convertView, position));
                } else {
                    holder = (StandardMenuItemViewHolder) convertView.getTag();
                }

                convertView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mAppMenu.onItemClick(item);
                    }
                });
                // Set up the icon.
                Drawable icon = item.getIcon();
                holder.image.setImageDrawable(icon);
                holder.image.setVisibility(icon == null ? View.GONE : View.VISIBLE);

                holder.checkbox.setVisibility(item.isCheckable() ? View.VISIBLE : View.GONE);
                holder.checkbox.setChecked(item.isChecked());
                holder.checkbox.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mAppMenu.onItemClick(item);
                    }
                });

                holder.text.setText(item.getTitle());
                boolean isEnabled = item.isEnabled();
                // Set the text color (using a color state list).
                holder.text.setEnabled(isEnabled);
                // This will ensure that the item is not highlighted when selected.
                convertView.setEnabled(isEnabled);
                break;
            }
            case ONE_BUTTON_PLUS_MENU_ITEM: {
                TwoButtonMenuItemViewHolder holder = null;
                if (convertView == null) {
                    holder = new TwoButtonMenuItemViewHolder();
                    convertView = mInflater.inflate(R.layout.one_button_plus_menu_item, parent, false);
                    holder.buttons[0] = (ImageButton) convertView.findViewById(R.id.button_one);
                    holder.buttons[1] = (ImageButton) convertView.findViewById(R.id.button_two);
                    convertView.setTag(holder);
                    convertView.setTag(R.id.menu_item_enter_anim_id,
                            buildIconItemEnterAnimator(holder.buttons, hasMenuButton));
                } else {
                    holder = (TwoButtonMenuItemViewHolder) convertView.getTag();
                }
                setupImageButton(holder.buttons[0], item.getSubMenu().getItem(0));
                setupMenuButton(holder.buttons[1]);
                convertView.setFocusable(false);
                convertView.setEnabled(false);
                break;
            }
            case TWO_BUTTON_MENU_ITEM: {
                TwoButtonMenuItemViewHolder holder = null;
                if (convertView == null) {
                    holder = new TwoButtonMenuItemViewHolder();
                    convertView = mInflater.inflate(R.layout.two_button_menu_item, parent, false);
                    holder.buttons[0] = (ImageButton) convertView.findViewById(R.id.button_one);
                    holder.buttons[1] = (ImageButton) convertView.findViewById(R.id.button_two);
                    convertView.setTag(holder);
                    convertView.setTag(R.id.menu_item_enter_anim_id,
                            buildIconItemEnterAnimator(holder.buttons, hasMenuButton));
                } else {
                    holder = (TwoButtonMenuItemViewHolder) convertView.getTag();
                }
                setupImageButton(holder.buttons[0], item.getSubMenu().getItem(0));
                setupImageButton(holder.buttons[1], item.getSubMenu().getItem(1));
                convertView.setFocusable(false);
                convertView.setEnabled(false);
                break;
            }
            case TWO_BUTTON_PLUS_MENU_ITEM: {
                ThreeButtonMenuItemViewHolder holder = null;
                if (convertView == null) {
                    holder = new ThreeButtonMenuItemViewHolder();
                    convertView = mInflater.inflate(R.layout.two_button_plus_menu_item, parent, false);
                    holder.buttons[0] = (ImageButton) convertView.findViewById(R.id.button_one);
                    holder.buttons[1] = (ImageButton) convertView.findViewById(R.id.button_two);
                    holder.buttons[2] = (ImageButton) convertView.findViewById(R.id.button_three);
                    convertView.setTag(holder);
                    convertView.setTag(R.id.menu_item_enter_anim_id,
                            buildIconItemEnterAnimator(holder.buttons, hasMenuButton));
                } else {
                    holder = (ThreeButtonMenuItemViewHolder) convertView.getTag();
                }
                setupImageButton(holder.buttons[0], item.getSubMenu().getItem(0));
                setupImageButton(holder.buttons[1], item.getSubMenu().getItem(1));
                setupMenuButton(holder.buttons[2]);
                convertView.setFocusable(false);
                convertView.setEnabled(false);
                break;
            }
            case THREE_BUTTON_MENU_ITEM: {
                ThreeButtonMenuItemViewHolder holder = null;
                if (convertView == null) {
                    holder = new ThreeButtonMenuItemViewHolder();
                    convertView = mInflater.inflate(R.layout.three_button_menu_item, parent, false);
                    holder.buttons[0] = (ImageButton) convertView.findViewById(R.id.button_one);
                    holder.buttons[1] = (ImageButton) convertView.findViewById(R.id.button_two);
                    holder.buttons[2] = (ImageButton) convertView.findViewById(R.id.button_three);
                    convertView.setTag(holder);
                    convertView.setTag(R.id.menu_item_enter_anim_id,
                            buildIconItemEnterAnimator(holder.buttons, hasMenuButton));
                } else {
                    holder = (ThreeButtonMenuItemViewHolder) convertView.getTag();
                }
                setupImageButton(holder.buttons[0], item.getSubMenu().getItem(0));
                setupImageButton(holder.buttons[1], item.getSubMenu().getItem(1));
                setupImageButton(holder.buttons[2], item.getSubMenu().getItem(2));
                convertView.setFocusable(false);
                convertView.setEnabled(false);
                break;
            }
            case THREE_BUTTON_PLUS_MENU_ITEM: {
                FourButtonMenuItemViewHolder holder = null;
                if (convertView == null) {
                    holder = new FourButtonMenuItemViewHolder();
                    convertView = mInflater.inflate(R.layout.three_button_plus_menu_item, parent, false);
                    holder.buttons[0] = (ImageButton) convertView.findViewById(R.id.button_one);
                    holder.buttons[1] = (ImageButton) convertView.findViewById(R.id.button_two);
                    holder.buttons[2] = (ImageButton) convertView.findViewById(R.id.button_three);
                    holder.buttons[3] = (ImageButton) convertView.findViewById(R.id.button_four);
                    convertView.setTag(holder);
                    convertView.setTag(R.id.menu_item_enter_anim_id,
                            buildIconItemEnterAnimator(holder.buttons, hasMenuButton));
                } else {
                    holder = (FourButtonMenuItemViewHolder) convertView.getTag();
                }
                setupImageButton(holder.buttons[0], item.getSubMenu().getItem(0));
                setupImageButton(holder.buttons[1], item.getSubMenu().getItem(1));
                setupImageButton(holder.buttons[2], item.getSubMenu().getItem(2));
                setupMenuButton(holder.buttons[3]);
                convertView.setFocusable(false);
                convertView.setEnabled(false);
                break;
            }
            case FOUR_BUTTON_MENU_ITEM: {
                FourButtonMenuItemViewHolder holder = null;
                if (convertView == null) {
                    holder = new FourButtonMenuItemViewHolder();
                    convertView = mInflater.inflate(R.layout.four_button_menu_item, parent, false);
                    holder.buttons[0] = (ImageButton) convertView.findViewById(R.id.button_one);
                    holder.buttons[1] = (ImageButton) convertView.findViewById(R.id.button_two);
                    holder.buttons[2] = (ImageButton) convertView.findViewById(R.id.button_three);
                    holder.buttons[3] = (ImageButton) convertView.findViewById(R.id.button_four);
                    convertView.setTag(holder);
                    convertView.setTag(R.id.menu_item_enter_anim_id,
                            buildIconItemEnterAnimator(holder.buttons, hasMenuButton));
                } else {
                    holder = (FourButtonMenuItemViewHolder) convertView.getTag();
                }
                setupImageButton(holder.buttons[0], item.getSubMenu().getItem(0));
                setupImageButton(holder.buttons[1], item.getSubMenu().getItem(1));
                setupImageButton(holder.buttons[2], item.getSubMenu().getItem(2));
                setupImageButton(holder.buttons[3], item.getSubMenu().getItem(3));
                convertView.setFocusable(false);
                convertView.setEnabled(false);
                break;
            }
            case TITLE_BUTTON_MENU_ITEM:
                // Fall through.
            case MENU_BUTTON_MENU_ITEM: {
                TitleButtonMenuItemViewHolder holder = null;
                if (convertView == null) {
                    holder = new TitleButtonMenuItemViewHolder();
                    convertView = mInflater.inflate(R.layout.title_button_menu_item, parent, false);
                    holder.title = (TextView) convertView.findViewById(R.id.title);
                    holder.button = (ImageButton) convertView.findViewById(R.id.button);

                    View animatedView = hasMenuButton ? holder.title : convertView;

                    convertView.setTag(holder);
                    convertView.setTag(R.id.menu_item_enter_anim_id,
                            buildStandardItemEnterAnimator(animatedView, position));
                } else {
                    holder = (TitleButtonMenuItemViewHolder) convertView.getTag();
                }
                final MenuItem titleItem = item.hasSubMenu() ? item.getSubMenu().getItem(0) : item;
                holder.title.setText(titleItem.getTitle());
                holder.title.setEnabled(titleItem.isEnabled());
                holder.title.setFocusable(titleItem.isEnabled());
                holder.title.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mAppMenu.onItemClick(titleItem);
                    }
                });

                if (hasMenuButton) {
                    holder.button.setVisibility(View.VISIBLE);
                    setupMenuButton(holder.button);
                } else if (item.getSubMenu().getItem(1).getIcon() != null) {
                    holder.button.setVisibility(View.VISIBLE);
                    setupImageButton(holder.button, item.getSubMenu().getItem(1));
                } else {
                    holder.button.setVisibility(View.GONE);
                }
                convertView.setFocusable(false);
                convertView.setEnabled(false);
                break;
            }
            default:
                assert false : "Unexpected MenuItem type";
        }
        return convertView;
    }

    private void setupImageButton(ImageButton button, final MenuItem item) {
        // Store and recover the level of image as button.setimageDrawable
        // resets drawable to default level.
        int currentLevel = item.getIcon().getLevel();
        button.setImageDrawable(item.getIcon());
        item.getIcon().setLevel(currentLevel);
        button.setContentDescription(item.getTitle());
        button.setEnabled(item.isEnabled());
        button.setFocusable(item.isEnabled());
        button.setSelected(item.isChecked());
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mAppMenu.onItemClick(item);
            }
        });
    }

    private void setupMenuButton(ImageButton button) {
        button.setSelected(true);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mAppMenu.dismiss();
            }
        });
    }

    /**
     * This builds an {@link Animator} for the enter animation of a standard menu item.  This means
     * it will animate the alpha from 0 to 1 and translate the view from -10dp to 0dp on the y axis.
     *
     * @param view     The menu item {@link View} to be animated.
     * @param position The position in the menu.  This impacts the start delay of the animation.
     * @return         The {@link Animator}.
     */
    private Animator buildStandardItemEnterAnimator(final View view, int position) {
        final float offsetYPx = ENTER_STANDARD_ITEM_OFFSET_Y_DP * mDpToPx;
        final int startDelay = ENTER_ITEM_BASE_DELAY_MS + ENTER_ITEM_ADDL_DELAY_MS * position;

        AnimatorSet animation = new AnimatorSet();
        animation.playTogether(
                ObjectAnimator.ofFloat(view, View.ALPHA, 0.f, 1.f),
                ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, offsetYPx, 0.f));
        animation.setDuration(ENTER_ITEM_DURATION_MS);
        animation.setStartDelay(startDelay);
        animation.setInterpolator(BakedBezierInterpolator.FADE_IN_CURVE);

        animation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                view.setAlpha(0.f);
            }
        });
        return animation;
    }

    /**
     * This builds an {@link Animator} for the enter animation of icon row menu items.  This means
     * it will animate the alpha from 0 to 1 and translate the views from 10dp to 0dp on the x axis.
     *
     * @param views        The list if icons in the menu item that should be animated.
     * @param skipLastItem Whether or not the last item should be animated or not.
     * @return             The {@link Animator}.
     */
    private Animator buildIconItemEnterAnimator(final ImageView[] views, boolean skipLastItem) {
        final boolean rtl = false; //LocalizationUtils.isLayoutRtl();
        final float offsetXPx = ENTER_STANDARD_ITEM_OFFSET_X_DP * mDpToPx * (rtl ? -1.f : 1.f);
        final int maxViewsToAnimate = views.length - (skipLastItem ? 1 : 0);

        AnimatorSet animation = new AnimatorSet();
        AnimatorSet.Builder builder = null;
        for (int i = 0; i < maxViewsToAnimate; i++) {
            final int startDelay = ENTER_ITEM_ADDL_DELAY_MS * i;

            Animator alpha = ObjectAnimator.ofFloat(views[i], View.ALPHA, 0.f, 1.f);
            Animator translate = ObjectAnimator.ofFloat(views[i], View.TRANSLATION_X, offsetXPx, 0);
            alpha.setStartDelay(startDelay);
            translate.setStartDelay(startDelay);
            alpha.setDuration(ENTER_ITEM_DURATION_MS);
            translate.setDuration(ENTER_ITEM_DURATION_MS);

            if (builder == null) {
                builder = animation.play(alpha);
            } else {
                builder.with(alpha);
            }
            builder.with(translate);
        }
        animation.setStartDelay(ENTER_ITEM_BASE_DELAY_MS);
        animation.setInterpolator(BakedBezierInterpolator.FADE_IN_CURVE);

        animation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                for (int i = 0; i < maxViewsToAnimate; i++) {
                    views[i].setAlpha(0.f);
                }
            }
        });
        return animation;
    }

    static class StandardMenuItemViewHolder {
        public TextView text;
        public AppMenuItemIcon image;
        public CheckBox checkbox;
    }

    static class TwoButtonMenuItemViewHolder {
        public ImageButton[] buttons = new ImageButton[2];
    }

    static class ThreeButtonMenuItemViewHolder {
        public ImageButton[] buttons = new ImageButton[3];
    }

    static class FourButtonMenuItemViewHolder {
        public ImageButton[] buttons = new ImageButton[4];
    }

    static class TitleButtonMenuItemViewHolder {
        public TextView title;
        public ImageButton button;
    }
}