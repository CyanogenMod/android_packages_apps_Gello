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

package com.android.browser.preferences;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.ValueCallback;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.browser.BrowserSettings;
import com.android.browser.R;
import com.android.browser.SiteTileView;
import com.android.browser.UrlUtils;
import com.android.browser.WebStorageSizeManager;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.codeaurora.swe.GeolocationPermissions;
import org.codeaurora.swe.PermissionsServiceFactory;
import org.codeaurora.swe.WebRefiner;
import org.codeaurora.swe.WebStorage;

/**
 * Manage the settings for an origin.
 * We use it to keep track of the 'HTML5' settings, i.e. database (webstorage)
 * and Geolocation.
 */
public class WebsiteSettingsFragment extends ListFragment implements OnClickListener {
    private SiteAdapter mAdapter = null;

    private class Site implements OnClickListener {
        private String mOrigin;
        private String mTitle;
        private Bitmap mIcon;
        private View mView;
        private Bitmap mDefaultIcon = mAdapter.mDefaultIcon;

        public Site(String origin) {
            mOrigin = origin;
            mTitle = null;
            mIcon = null;
            fetchFavicon();
        }

        private void fetchFavicon() {
            // Fetch favicon and set it
            PermissionsServiceFactory.getFavicon(mOrigin, getActivity(),
                    new ValueCallback<Bitmap>() {
                        @Override
                        public void onReceiveValue(Bitmap value) {
                            setIcon(value);

                        }
                    });
        }

        public void updateView(View view){
            mView = view;
            fetchFavicon();
        }

        public String getOrigin() {
            return mOrigin;
        }

        public void setTitle(String title) {
            mTitle = title;
        }

        public void setIcon(Bitmap image) {
            mIcon = image;
            if (mView != null) {
                SiteTileView icon = (SiteTileView) mView.findViewById(R.id.icon);
                icon.replaceFavicon((image == null) ? mDefaultIcon : image);
                icon.setVisibility(View.VISIBLE);
                icon.setOnClickListener(this);
            }
        }

        public Bitmap getIcon() {
            return mIcon;
        }

        public String getPrettyOrigin() {
            return mTitle == null ? null : hideHttp(mOrigin);
        }

        public String getPrettyTitle() {
            return mTitle == null ? hideHttp(mOrigin) : mTitle;
        }

        private String hideHttp(String str) {
            if (str == null)
                return null;
            Uri uri = Uri.parse(str);
            return "http".equals(uri.getScheme()) ?  str.substring(7) : str;
        }

        @Override
        public void onClick(View v) {
            clickHandler(this);
        }
    }

    class SiteAdapter extends ArrayAdapter<Site>
            implements AdapterView.OnItemClickListener {
        private int mResource;
        private LayoutInflater mInflater;
        private Bitmap mDefaultIcon;
        private PermissionsServiceFactory.PermissionsService mPermServ;
        private boolean mReady;

        public SiteAdapter(Context context, int rsc) {
            super(context, rsc);
            mResource = rsc;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mDefaultIcon = BitmapFactory.decodeResource(getResources(),
                    R.drawable.ic_deco_favicon_normal);
            mReady = false;
            askForOrigins();
        }

        public void askForOrigins() {
            if (mPermServ == null) {
                PermissionsServiceFactory.getPermissionsService(
                        new ValueCallback<PermissionsServiceFactory.PermissionsService>() {
                            @Override
                            public void onReceiveValue(
                                    PermissionsServiceFactory.PermissionsService value) {
                                mPermServ = value;
                                Map<String, Site> sites = new HashMap<>();

                                Set<String> origins = mPermServ.getOrigins();
                                for (String origin : origins) {
                                    if (!TextUtils.isEmpty(origin))
                                        sites.put(origin, new Site(origin));
                                }

                                // Create a map from host to origin. This is used to add metadata
                                // (title, icon) for this origin from the bookmarks DB. We must do
                                // the DB access on a background thread.
                                //new UpdateFromBookmarksDbTask(ctx, sites).execute();

                                populateOrigins(sites);
                                mReady = true;
                            }
                        }
                );
            }
        }

        public void deleteAllOrigins() {
            if (mPermServ != null) {
                Set<String> origins = mPermServ.getOrigins();
                String[] originArray = origins.toArray(new String[origins.size()]);

                for (String origin : originArray) {
                    PermissionsServiceFactory.PermissionsService.OriginInfo info =
                            mPermServ.getOriginInfo(origin);
                    if (info != null) {
                        info.clearAllStoredData();
                    }
                }
                // purge the permissionservice since its not needed
                mPermServ.purge();
                mPermServ = null;

                // reset all site settings
                PermissionsServiceFactory.resetSiteSettings();

                WebRefiner refiner = WebRefiner.getInstance();
                if (refiner != null) {
                    refiner.useDefaultPermissionForOrigins(originArray);
                }
            }
        }

        private void populateOrigins(Map<String, Site> sites) {
            clear();

            // We can now simply populate our array with Site instances
            Set<Map.Entry<String, Site>> elements = sites.entrySet();
            Iterator<Map.Entry<String, Site>> entryIterator = elements.iterator();
            while (entryIterator.hasNext()) {
                Map.Entry<String, Site> entry = entryIterator.next();
                Site site = entry.getValue();
                add(site);
            }

            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            final TextView title;
            final TextView subtitle;

            if (convertView == null) {
                view = mInflater.inflate(mResource, parent, false);
            } else {
                view = convertView;
            }

            title = (TextView) view.findViewById(R.id.title);
            subtitle = (TextView) view.findViewById(R.id.subtitle);

            Site site = getItem(position);
            site.updateView(view);
            title.setText(site.getPrettyTitle());
            String subtitleText = site.getPrettyOrigin();
            if (subtitleText != null) {
                title.setMaxLines(1);
                title.setSingleLine(true);
                subtitle.setVisibility(View.VISIBLE);
                subtitle.setText(subtitleText);
            } else {
                subtitle.setVisibility(View.GONE);
                title.setMaxLines(2);
                title.setSingleLine(false);
            }
            // We set the site as the view's tag,
            // so that we can get it in onItemClick()
            view.setTag(site);

            return view;
        }

        public void onItemClick(AdapterView<?> parent,
                                View view,
                                int position,
                                long id) {
            clickHandler((Site) view.getTag());
        }
    }

    private void clickHandler(Site site) {
        Activity activity = getActivity();
        if (activity != null) {
            Bundle args = new Bundle();
            args.putString(SiteSpecificPreferencesFragment.EXTRA_ORIGIN, site.getOrigin());
            if(site.getIcon() != null) {
                ByteArrayOutputStream favicon = new ByteArrayOutputStream();
                site.getIcon().compress(Bitmap.CompressFormat.PNG, 100, favicon);
                args.putByteArray(SiteSpecificPreferencesFragment.EXTRA_FAVICON,
                        favicon.toByteArray());
            }
            FragmentManager fragmentManager = activity.getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            Fragment newFragment = new SiteSpecificPreferencesFragment();
            newFragment.setArguments(args);
            fragmentTransaction.replace(getId(), newFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.swe_website_settings, container, false);
        View new_site = view.findViewById(R.id.add_new_site);
        new_site.setVisibility(View.VISIBLE);
        new_site.setOnClickListener(this);
        View clear = view.findViewById(R.id.clear_all_button);
        clear.setVisibility(View.VISIBLE);
        clear.setOnClickListener(this);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new SiteAdapter(getActivity(), R.layout.website_settings_row);
        getListView().setAdapter(mAdapter);
        getListView().setOnItemClickListener(mAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.askForOrigins();
        ActionBar bar = getActivity().getActionBar();
        if (bar != null) {
            bar.setTitle(R.string.pref_extras_website_settings);
            bar.setDisplayHomeAsUpEnabled(false);
            bar.setHomeButtonEnabled(false);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.clear_all_button:
            // Show the prompt to clear all origins of their data and geolocation permissions.
            ClearAllDialog clearFragment = ClearAllDialog.newInstance();
            clearFragment.setAdapter(mAdapter);
            clearFragment.show(getActivity().getFragmentManager(), "clearAll dialog");
            break;

        case R.id.add_new_site:
            NewSiteDialog newFragment = NewSiteDialog.newInstance();
            newFragment.setTargetFragment(this, -1);
            newFragment.show(getActivity().getFragmentManager(), "newSite dialog");
            break;
        }
    }

    /*
   Added this class to manage AlertDialog lifecycle.
 */
    public static class NewSiteDialog extends DialogFragment {
        private final String SAVED = "saved";
        private EditText editText = null;
        public static NewSiteDialog newInstance() {
            NewSiteDialog frag = new NewSiteDialog();
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            editText = new EditText(getActivity());
            String siteOrigin = savedInstanceState != null ?
                    savedInstanceState.getString(SAVED): "";
            editText.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_URI);
            editText.setText(siteOrigin);
            editText.setSingleLine(true);
            editText.setSelection(editText.getText().length());// Always move the cursor to the end.
            editText.setImeActionLabel(null, EditorInfo.IME_ACTION_DONE);
            final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setView(editText)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String siteOrigin = editText.getText().toString();
                            Fragment frag = getTargetFragment();
                            if (frag == null || !(frag instanceof WebsiteSettingsFragment)) {
                                Log.e("NewSiteDialog", "get target fragment error!");
                                return;
                            }
                            Bundle args = new Bundle();
                            args.putString(SiteSpecificPreferencesFragment.EXTRA_SITE,
                                    siteOrigin);

                            FragmentTransaction fragmentTransaction =
                                    getActivity().getFragmentManager().beginTransaction();

                            Fragment newFragment = new SiteSpecificPreferencesFragment();
                            newFragment.setArguments(args);
                            fragmentTransaction.replace(frag.getId(), newFragment);
                            fragmentTransaction.addToBackStack(null);
                            fragmentTransaction.commit();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .setTitle(R.string.website_settings_add_origin)
                    .setMessage(R.string.pref_security_origin_name)
                    .create();

            editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                        return true;
                    }
                    return false;
                }
            });

            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

            return dialog;
        }

        @Override
        public void onSaveInstanceState(Bundle outState){
            super.onSaveInstanceState(outState);
            outState.putString(SAVED, editText.getText().toString().trim());
        }
    }

    /*
Added this class to manage AlertDialog lifecycle.
*/
    public static class ClearAllDialog extends DialogFragment {
        private SiteAdapter adapter;
        public static ClearAllDialog newInstance() {
            ClearAllDialog frag = new ClearAllDialog();
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.website_settings_clear_all_dialog_message)
                    .setPositiveButton(R.string.ok,
                            new AlertDialog.OnClickListener() {
                                public void onClick(DialogInterface dlg, int which) {
                                    if (adapter == null) {
                                        return;
                                    }
                                    adapter.deleteAllOrigins();
                                    if (GeolocationPermissions.isIncognitoCreated()) {
                                        GeolocationPermissions.getIncognitoInstance().clearAll();
                                    }
                                    WebStorageSizeManager.resetLastOutOfSpaceNotificationTime();
                                    adapter.askForOrigins();
                                    Activity activity = getActivity();
                                    if (activity != null) {
                                        getActivity().getFragmentManager().popBackStack();
                                    }
                                }
                            })
                    .setNegativeButton(R.string.cancel, null)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .create();

            return dialog;
        }

        public void setAdapter(SiteAdapter inAdapter) {
            adapter = inAdapter;
        }
    }
}
