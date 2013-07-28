package com.podnoms.android.podcatcher.ui.adapters.lazy;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class LazyAdapter extends SimpleCursorAdapter {

    private static LayoutInflater _inflater = null;
    public ImageLoader imageLoader;
    protected int _layout;
    protected Cursor _cursor;

    public LazyAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        _cursor = c;
        _layout = layout;
        _inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        imageLoader = new ImageLoader(context.getApplicationContext());
    }

    @Override
    public Cursor swapCursor(Cursor c) {
        _cursor = c;
        return super.swapCursor(c);
    }

    @Override
    public int getCount() {
       if (_cursor == null)
            return 0;
        return _cursor.getCount();
    }

    @Override
    public abstract View getView(int position, View convertView, ViewGroup parent);

    @Override
    public long getItemId(int position) {
        if (_cursor != null) {
            if (_cursor.moveToPosition(position))
                return _cursor.getLong(_cursor.getColumnIndex(BaseColumns._ID));
        }
        return position;
    }

    protected View _inflateLayout(View convertView) {
        View vi = convertView;
        if (convertView == null)
            vi = _inflater.inflate(_layout, null);
        return vi;
    }
}