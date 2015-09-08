/*
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *       * Redistributions in binary form must reproduce the above
 *         copyright notice, this list of conditions and the following
 *         disclaimer in the documentation and/or other materials provided
 *         with the distribution.
 *       * Neither the name of The Linux Foundation nor the names of its
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

package com.android.browser.mynavigation;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.util.TypedValue;
import android.util.Log;

import com.android.browser.R;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyNavigationTemplate {

    private static final String LOGTAG = "MyNavigationTemplate";
    private static HashMap<Integer, MyNavigationTemplate> sCachedTemplates =
            new HashMap<Integer, MyNavigationTemplate>();
    private static boolean sCountryChanged = false;
    private static String sCurrentCountry = "US";

    private List<Entity> mTemplate;
    private HashMap<String, Object> mData = new HashMap<String, Object>();

    public static MyNavigationTemplate getCachedTemplate(Context context, int id) {

        String changeToCountry = context.getResources().getConfiguration().locale
                .getDisplayCountry();
        Log.d(LOGTAG, "MyNavigationTemplate.getCachedTemplate() display country :"
                + changeToCountry + ", before country :" + sCurrentCountry);
        if (changeToCountry != null && !changeToCountry.equals(sCurrentCountry)) {
            sCountryChanged = true;
            sCurrentCountry = changeToCountry;
        }
        synchronized (sCachedTemplates) {
            MyNavigationTemplate template = sCachedTemplates.get(id);
            if (template == null || sCountryChanged) {
                sCountryChanged = false;
                template = new MyNavigationTemplate(context, id);
                sCachedTemplates.put(id, template);
            }
            return template.copy();
        }
    }

    interface Entity {
        void write(OutputStream stream, EntityData params) throws IOException;
    }

    interface EntityData {
        void writeValue(OutputStream stream, String key) throws IOException;

        ListEntityIterator getListIterator(String key);
    }

    interface ListEntityIterator extends EntityData {
        void reset();

        boolean moveToNext();
    }

    static class StringEntity implements Entity {
        byte[] mValue;

        public StringEntity(String value) {
            mValue = value.getBytes();
        }

        @Override
        public void write(OutputStream stream, EntityData params) throws IOException {
            stream.write(mValue);
        }
    }

    static class SimpleEntity implements Entity {
        String mKey;

        public SimpleEntity(String key) {
            mKey = key;
        }

        @Override
        public void write(OutputStream stream, EntityData params) throws IOException {
            params.writeValue(stream, mKey);
        }
    }

    static class ListEntity implements Entity {
        String mKey;
        MyNavigationTemplate mSubTemplate;

        public ListEntity(Context context, String key, String subTemplate) {
            mKey = key;
            mSubTemplate = new MyNavigationTemplate(context, subTemplate);
        }

        @Override
        public void write(OutputStream stream, EntityData params) throws IOException {
            ListEntityIterator iter = params.getListIterator(mKey);
            if (null == iter) {
                return;
            }
            iter.reset();
            while (iter.moveToNext()) {
                mSubTemplate.write(stream, iter);
            }
        }
    }

    public abstract static class CursorListEntityWrapper implements ListEntityIterator {
        private Cursor mCursor;

        public CursorListEntityWrapper(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public boolean moveToNext() {
            return mCursor.moveToNext();
        }

        @Override
        public void reset() {
            mCursor.moveToPosition(-1);
        }

        @Override
        public ListEntityIterator getListIterator(String key) {
            return null;
        }

        public Cursor getCursor() {
            return mCursor;
        }
    }

    static class HashMapEntityData implements EntityData {
        HashMap<String, Object> mData;

        public HashMapEntityData(HashMap<String, Object> map) {
            mData = map;
        }

        @Override
        public ListEntityIterator getListIterator(String key) {
            return (ListEntityIterator) mData.get(key);
        }

        @Override
        public void writeValue(OutputStream stream, String key) throws IOException {
            stream.write((byte[]) mData.get(key));
        }
    }

    private MyNavigationTemplate(Context context, int tid) {
        this(context, readRaw(context, tid));
    }

    private MyNavigationTemplate(Context context, String template) {
        mTemplate = new ArrayList<Entity>();
        template = replaceConsts(context, template);
        parseTemplate(context, template);
    }

    private MyNavigationTemplate(MyNavigationTemplate copy) {
        mTemplate = copy.mTemplate;
    }

    MyNavigationTemplate copy() {
        return new MyNavigationTemplate(this);
    }

    void parseTemplate(Context context, String template) {
        final Pattern pattern = Pattern.compile("<%([=\\{])\\s*(\\w+)\\s*%>");
        Matcher m = pattern.matcher(template);
        int start = 0;
        while (m.find()) {
            String staticPart = template.substring(start, m.start());
            if (staticPart.length() > 0) {
                mTemplate.add(new StringEntity(staticPart));
            }
            String type = m.group(1);
            String name = m.group(2);
            if (type.equals("=")) {
                mTemplate.add(new SimpleEntity(name));
            } else if (type.equals("{")) {
                Pattern p = Pattern.compile("<%\\}\\s*" + Pattern.quote(name) + "\\s*%>");
                Matcher end = p.matcher(template);
                if (end.find(m.end())) {
                    start = m.end();
                    m.region(end.end(), template.length());
                    String subTemplate = template.substring(start, end.start());
                    mTemplate.add(new ListEntity(context, name, subTemplate));
                    start = end.end();
                    continue;
                }
            }
            start = m.end();
        }
        String staticPart = template.substring(start, template.length());
        if (staticPart.length() > 0) {
            mTemplate.add(new StringEntity(staticPart));
        }
    }

    public void assign(String name, String value) {
        mData.put(name, value.getBytes());
    }

    public void assignLoop(String name, ListEntityIterator iter) {
        mData.put(name, iter);
    }

    public void write(OutputStream stream) throws IOException {
        write(stream, new HashMapEntityData(mData));
    }

    public void write(OutputStream stream, EntityData data) throws IOException {
        for (Entity ent : mTemplate) {
            ent.write(stream, data);
        }
    }

    private static String replaceConsts(Context context, String template) {
        final Pattern pattern = Pattern.compile("<%@\\s*(\\w+/\\w+)\\s*%>");
        final Resources res = context.getResources();
        Matcher m = pattern.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String name = m.group(1);
            if (name.startsWith("drawable/")) {
                m.appendReplacement(sb, "res/" + name);
            } else {
                final String packageName = R.class.getPackage().getName();
                int id = res.getIdentifier(name, null, packageName);
                if(id == 0) {
                    id = res.getIdentifier(name, null, context.getPackageName());
                }
                if (id != 0) {
                    TypedValue value = new TypedValue();
                    res.getValue(id, value, true);
                    String replacement;
                    if (value.type == TypedValue.TYPE_DIMENSION) {
                        float dimen = res.getDimension(id);
                        int dimeni = (int) dimen;
                        if (dimeni == dimen) {
                            replacement = Integer.toString(dimeni);
                        } else {
                            replacement = Float.toString(dimen);
                        }
                    } else {
                        replacement = value.coerceToString().toString();
                    }
                    m.appendReplacement(sb, replacement);
                }
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String readRaw(Context context, int id) {
        InputStream ins = context.getResources().openRawResource(id);
        try {
            byte[] buf = new byte[ins.available()];
            ins.read(buf);
            return new String(buf, "utf-8");
        } catch (IOException ex) {
            return "<html><body>Error</body></html>";
        }
    }
}
