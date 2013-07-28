package com.podnoms.android.podcatcher.ui.fragments;

import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import com.podnoms.android.podcatcher.R;
import com.podnoms.android.podcatcher.providers.PodNomsInterface;
import com.podnoms.android.podcatcher.providers.sync.PodNomsSyncAdapter;
import com.podnoms.android.podcatcher.providers.sync.api.ApiHandler;
import com.podnoms.android.podcatcher.services.DownloadService;
import com.podnoms.android.podcatcher.ui.adapters.PodcastListAdapter;
import com.podnoms.android.podcatcher.util.LogHandler;
import com.podnoms.android.podcatcher.util.RssUtils;
import com.podnoms.android.podcatcher.util.state.Constants;
import com.podnoms.android.podcatcher.util.state.PersistentStateHandler;
import com.podnoms.android.podcatcher.util.state.VolatileStateHandler;

public class FragmentManagePodcasts extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    //TODO: refactor out this & FragmentPodcastList
    //TODO: huge amount of shared code
    private PodcastListAdapter _adapter;
    private ListView _listView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        _rootView = (ViewGroup) inflater.inflate(R.layout.fragment_managepodcasts, null);
        String[] uiBindFrom = {PodNomsInterface.Podcast.COLUMN_NAME_TITLE, PodNomsInterface.Podcast.COLUMN_NAME_DESCRIPTION};
        int[] uiBindTo = {R.id.include_podcast_entry_desc_firstline, R.id.include_podcast_entry_desc_secondline};

        getLoaderManager().initLoader(PodNomsInterface.LOADER_MANAGE_PODCASTS, null, this);
        _adapter = new PodcastListAdapter(
                getActivity(),
                R.layout.listrow_entry,
                null,
                uiBindFrom,
                uiBindTo,
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        );
        _listView = (ListView) _rootView.findViewById(R.id.podcast_list);
        _listView.setAdapter(_adapter);
        _rootView.findViewById(R.id.cmd_addPodcast).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText txtUrl = ((EditText) _rootView.findViewById(R.id.txt_podcastUrl));
                String url = txtUrl.getText().toString();
                new AddPodcastTask().execute(url);
            }
        });

        return _rootView;
    }

    private class AddPodcastTask extends AsyncTask<String, Void, Long> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(
                    getActivity(),
                    "",
                    getActivity().getResources().getString(R.string.validating_podcast),
                    true,
                    false);
        }

        @Override
        protected Long doInBackground(String... params) {
            Long ret = (long) -1;

            String url = params[0];
            progressDialog.show();
            if (!TextUtils.isEmpty(url)) {
                ApiHandler handler = new ApiHandler();
                ApiHandler.ApiValidatorResult result = handler.addPodcast(url);
                progressDialog.cancel();
            }
            return ret;
        }

        @Override
        protected void onPostExecute(Long result) {
            super.onPostExecute(result);
            if (result != -1) {
                ((EditText) _rootView.findViewById(R.id.txt_podcastUrl)).setText("");
                _updatePodcast(result);
            }else{
                Toast.makeText(getActivity(), getResources().getString(R.string.invalid_podcasturl), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void _updatePodcast(final Long id) {
        //spin up background thread to get the podcast details
        //list adapter is observable so should update automatically
        PodNomsSyncAdapter syncAdapter =
                new PodNomsSyncAdapter(getActivity(), DownloadService.MESSAGE_STARTDOWNLOAD, PersistentStateHandler.I().getString(Constants.AUTH_TOKEN));
        syncAdapter.startSyncForPodcast(id);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = PodNomsInterface.Podcast.getProjectionMap();
        CursorLoader loader = new CursorLoader(
                getActivity(),
                PodNomsInterface.Podcast.CONTENT_URI,
                projection,
                null, null, null);

        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        try {
            _adapter.swapCursor(cursor);
        } catch (Exception ex) {
            LogHandler.reportError(getActivity(), "Error creating podcast list fragment", ex);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        cursorLoader = null;
    }
}
