package com.podnoms.android.podcatcher.ui.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.TextView;
import com.podnoms.android.podcatcher.R;
import com.podnoms.android.podcatcher.providers.PodNomsInterface;
import com.podnoms.android.podcatcher.util.DateUtils;

public class FragmentAudioItemView extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private TextView _txtTitle;
    private TextView _txtSubTitle;
    private TextView _txtDate;
    private TextView _txtDownloaded;
    private WebView _ctlWebView;
    private CheckBox _optDownloaded;
    private int _entryId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        _rootView = (ViewGroup) inflater.inflate(R.layout.fragment_audioitemview, null);

        _txtTitle = (TextView) _rootView.findViewById(R.id.podcast_title);
        _txtSubTitle = (TextView) _rootView.findViewById(R.id.podcast_subtitle);
        _txtDate = (TextView) _rootView.findViewById(R.id.podcast_date);
        _ctlWebView = (WebView) _rootView.findViewById(R.id.podcast_description);
        _optDownloaded = ((CheckBox) _rootView.findViewById(R.id.star_button));

        return _rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        _setupPlayerFragment();
        getLoaderManager().initLoader(PodNomsInterface.LOADER_AUDIO_ITEM, null, this);
    }

    private void _setupPlayerFragment() {
        Fragment playerFragment = new FragmentPlayer();
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_audioitemview_fragment_player, playerFragment).commit();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Intent intent = getActivity().getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            Uri localUri;
            if (extras.containsKey("uri")) {
                localUri = (Uri) extras.getParcelable("uri");
            } else if (extras.containsKey("entry_id")) {
                localUri = PodNomsInterface.Entry.buildPodcastDetailsUri(extras.getLong("entry_id"));
            } else {
                return null;
            }
            return new CursorLoader(getActivity(), localUri, PodNomsInterface.Entry.getProjectionMap(), null, null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        _loadTrackDetails(cursor);
    }

    private void _loadTrackDetails(Cursor cursor) {
        if ((cursor != null) && (cursor.moveToFirst())) {
            _entryId = cursor.getInt(cursor.getColumnIndex(PodNomsInterface.Entry._ID));
            _txtTitle.setText(cursor.getString(cursor.getColumnIndex(PodNomsInterface.Entry.COLUMN_NAME_TITLE)));
            _txtSubTitle.setText(cursor.getString(cursor.getColumnIndex(PodNomsInterface.Entry.COLUMN_NAME_TITLE)));
            String date = cursor.getString(cursor.getColumnIndex(PodNomsInterface.Entry.COLUMN_NAME_DATE_CREATED));
            _txtDate.setText(String.format("Downloaded: %s", DateUtils.getShortDate(date)));
            _ctlWebView.loadData(cursor.getString(cursor.getColumnIndex(PodNomsInterface.Entry.COLUMN_NAME_DESCRIPTION)), "text/html", null);
            int _entryDownloaded = cursor.getInt(cursor.getColumnIndex(PodNomsInterface.Entry.COLUMN_NAME_DOWNLOADED));
            int _entryLength = cursor.getInt(cursor.getColumnIndex(PodNomsInterface.Entry.COLUMN_NAME_ENTRY_LENGTH));

            if (_entryDownloaded != _entryLength)
                _optDownloaded.setChecked(false);
            else
                _optDownloaded.setChecked(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }
}
