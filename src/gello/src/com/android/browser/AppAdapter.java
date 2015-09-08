/*
    * Copyright (c) 2014, The Linux Foundation. All rights reserved.
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

package com.android.browser;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.ImageView;

import java.util.List;

public class AppAdapter extends ArrayAdapter<ResolveInfo> {
    private PackageManager pm = null;
    private Context context = null;
    private int layoutResourceId = -1;


    public AppAdapter (Context context, PackageManager pm, int layoutResourceId, List<ResolveInfo> apps) {
      super(context, layoutResourceId, apps);
      this.context = context;
      this.pm = pm;
      this.layoutResourceId = layoutResourceId;
    }

    /*
    * Overide this method in order to create your own view
    */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (convertView == null) {
        convertView = newView(parent);
      }

      bindView(position, convertView);
      return(convertView);
    }

    private View newView(ViewGroup parent) {
      LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
      return(layoutInflater.inflate(layoutResourceId, parent, false));
    }

    private void bindView(int position, View row) {

      TextView label = (TextView)row.findViewById(R.id.app_label);
      label.setText(  getItem(position).loadLabel(pm));

      ImageView icon = (ImageView)row.findViewById(R.id.app_icon);
      icon.setImageDrawable( getItem(position).loadIcon(pm));

    }
}
