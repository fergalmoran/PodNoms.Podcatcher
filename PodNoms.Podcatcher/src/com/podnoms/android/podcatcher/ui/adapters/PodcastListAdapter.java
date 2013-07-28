package com.podnoms.android.podcatcher.ui.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.podnoms.android.podcatcher.R;
import com.podnoms.android.podcatcher.providers.PodNomsInterface;
import com.podnoms.android.podcatcher.ui.adapters.lazy.LazyAdapter;
import com.podnoms.android.podcatcher.util.UIUtils;

public class PodcastListAdapter extends LazyAdapter implements Filterable {

    static class Holder {
        private final TextView firstLine;
        private final TextView secondLine;
        private final ImageView percentPlayed;
        private final View strut;
        private final LinearLayout wrapper;
        public View downloadIndicator;
        public ImageView image;

        Holder(View vi) {
            this.firstLine = (TextView) vi.findViewById(R.id.include_podcast_entry_desc_firstline);
            this.secondLine = (TextView) vi.findViewById(R.id.include_podcast_entry_desc_secondline);
            this.percentPlayed = (ImageView) vi.findViewById(R.id.listrow_entry_played_progress);
            this.strut = vi.findViewById(R.id.strut);
            this.wrapper = (LinearLayout) vi.findViewById(R.id.listrow_entry_played_progress_wrapper);
            this.downloadIndicator = vi.findViewById(R.id.downloaded_indicator);
            this.image = (ImageView) vi.findViewById(R.id.include_podcast_entry_desc_image);
            vi.setTag(this);
        }
    }

    public PodcastListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = _inflateLayout(convertView);
        Holder holder = _inflateHolder(vi);

        if (holder != null && _cursor.moveToPosition(position)) {

            holder.firstLine.setText(_cursor.getString(_cursor.getColumnIndex(PodNomsInterface.Podcast.COLUMN_NAME_TITLE)));
            UIUtils.setTextMaybeHtml(holder.secondLine, _cursor.getString(_cursor.getColumnIndex(PodNomsInterface.Podcast.COLUMN_NAME_DESCRIPTION)));

            int columnIndexDownloaded = _cursor.getColumnIndex(PodNomsInterface.Entry.COLUMN_NAME_DOWNLOADED);
            int columnIndexFileLength = _cursor.getColumnIndex(PodNomsInterface.Entry.COLUMN_NAME_FILE_LENGTH);
            int columnIndexPercentagePlayed = _cursor.getColumnIndex(PodNomsInterface.Entry.V_COLUMN_NAME_PERCENTAGE_PLAYED);


            float percentagePlayed = 0;
            if (columnIndexPercentagePlayed != -1)
                percentagePlayed = _cursor.getFloat(columnIndexPercentagePlayed);

            if (percentagePlayed > 0) {
                holder.wrapper.setVisibility(View.VISIBLE);
                holder.percentPlayed.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, percentagePlayed));
                holder.strut.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 100 - percentagePlayed));
            } else {
                holder.wrapper.setVisibility(View.GONE);
            }

            if (columnIndexDownloaded != -1 && columnIndexFileLength != -1 && holder.downloadIndicator != null) {
                int downloaded = _cursor.getInt(columnIndexDownloaded);
                int size = _cursor.getInt(columnIndexFileLength);
                if (downloaded == size && size != 0)
                    holder.downloadIndicator.setVisibility(View.VISIBLE);
                else
                    holder.downloadIndicator.setVisibility(View.GONE);
            } else {
                holder.downloadIndicator.setVisibility(View.GONE);
            }
            imageLoader.DisplayImage(_cursor.getString(_cursor.getColumnIndex(PodNomsInterface.Podcast.COLUMN_NAME_IMAGE)), this.mContext, holder.image);
        }
        return vi;
    }

    private Holder _inflateHolder(View vi) {
        Holder ret;
        if (vi.getTag() == null) {
            ret = new Holder(vi);
        } else {
            ret = (Holder) vi.getTag();
        }
        return ret;
    }
}
