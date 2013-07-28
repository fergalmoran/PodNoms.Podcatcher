package com.podnoms.android.podcatcher.ui.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.*;
import android.view.MenuInflater;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import com.actionbarsherlock.view.*;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.ShareActionProvider;
import com.podnoms.android.podcatcher.PodNomsApplication;
import com.podnoms.android.podcatcher.R;
import com.podnoms.android.podcatcher.providers.PodNomsInterface;
import com.podnoms.android.podcatcher.providers.sync.api.ApiHandler;
import com.podnoms.android.podcatcher.ui.BaseActivity;
import com.podnoms.android.podcatcher.ui.activities.phone.ActivityEntryDetails;
import com.podnoms.android.podcatcher.ui.activities.phone.ActivityEntryList;
import com.podnoms.android.podcatcher.ui.adapters.PodcastListAdapter;
import com.podnoms.android.podcatcher.ui.adapters.lazy.LazyAdapter;
import com.podnoms.android.podcatcher.util.DataUtils;
import com.podnoms.android.podcatcher.util.LogHandler;

import java.security.InvalidParameterException;

public class FragmentPodcastList extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor>, ActionMode.Callback {

    private LazyAdapter _adapter;
    private Uri _contentUri;
    private long _entryId;
    private ShareActionProvider _shareActionProvider;

    public static FragmentPodcastList newInstance(Uri contentUri) {

        FragmentPodcastList pageFragment = new FragmentPodcastList();
        Bundle bundle = new Bundle();
        bundle.putParcelable("content_uri", contentUri);
        pageFragment.setArguments(bundle);
        return pageFragment;
    }

    @Override
    public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
        inflater.inflate(R.menu.menu_podcast, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        _rootView = (ViewGroup) inflater.inflate(R.layout.fragment_podcastlist, null);

        setHasOptionsMenu(true);
        //bind the loaders
        String[] uiBindFrom = {PodNomsInterface.Podcast.COLUMN_NAME_TITLE, PodNomsInterface.Podcast.COLUMN_NAME_DESCRIPTION};
        int[] uiBindTo = {R.id.include_podcast_entry_desc_firstline, R.id.include_podcast_entry_desc_secondline};
        _contentUri = (Uri) getArguments().get("content_uri");
        getLoaderManager().initLoader(PodNomsInterface.LOADER_PODCAST_LIST, null, this);
        _adapter = new PodcastListAdapter(
                getActivity(),
                R.layout.listrow_entry,
                null,
                uiBindFrom,
                uiBindTo,
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        );

        final ListView listView = (ListView) _rootView.findViewById(R.id.fragment_podcastlist_list);
        listView.setAdapter(_adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor c = (Cursor) parent.getItemAtPosition(position);
                int typeIndex = c.getColumnIndex("type");
                if (typeIndex == -1) { //we're a podcast
                    final Uri entriesUri = PodNomsInterface.Entry.buildPodcastEntriesUri(c.getLong(c.getColumnIndex(PodNomsInterface.Podcast._ID)));
                    final Intent intent = new Intent(FragmentPodcastList.this.getActivity(), ActivityEntryList.class);
                    intent.putExtra("content_uri", entriesUri);
                    ((BaseActivity) getActivity()).openActivityOrFragment(intent);
                } else { //we're an entry
                    final Uri entriesUri = PodNomsInterface.Entry.buildPodcastDetailsUri(c.getLong(c.getColumnIndex(PodNomsInterface.Podcast._ID)));
                    final Intent intent = new Intent(FragmentPodcastList.this.getActivity(), ActivityEntryDetails.class);
                    intent.putExtra("uri", entriesUri);
                    intent.putExtra("file", c.getString(c.getColumnIndex(PodNomsInterface.Entry.COLUMN_NAME_LOCAL_FILE)));
                    intent.putExtra("title", c.getString(c.getColumnIndex(PodNomsInterface.Entry.COLUMN_NAME_PODCAST_TITLE)));
                    intent.putExtra("description", c.getString(c.getColumnIndex(PodNomsInterface.Entry.COLUMN_NAME_DESCRIPTION)));
                    intent.putExtra("position", c.getLong(c.getColumnIndex(PodNomsInterface.Entry.COLUMN_NAME_POSITION)));
                    intent.putExtra("entry_id", c.getLong(c.getColumnIndex(PodNomsInterface.Entry._ID)));
                    ((BaseActivity) getActivity()).openActivityOrFragment(intent);
                }
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                _entryId = id;
                FragmentPodcastList.this.getSherlockActivity().startActionMode(FragmentPodcastList.this);
                listView.setSelected(true);
                return true;
            }
        });
        return _rootView;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.menu_action_podcast, menu);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        return new CursorLoader(
                getActivity(),
                _contentUri,
                PodNomsInterface.Podcast.getProjectionMap(),
                null, null, null);
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

    private void setShareIntent(Intent shareIntent) {
        if (_shareActionProvider != null) {
            _shareActionProvider.setShareIntent(shareIntent);
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        com.actionbarsherlock.view.MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.menu_action_podcast, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_podcast_unsubscribe:
                new DeletePodcastTask().execute(_entryId);
                mode.finish();
                return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {

    }

    private class DeletePodcastTask extends AsyncTask<Long, Void, Long> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(
                    getActivity(),
                    "",
                    getActivity().getResources().getString(R.string.deleting_podcast),
                    true,
                    false);
        }

        @Override
        protected Long doInBackground(Long... params) {
            Long ret = (long) -1;

            long id = params[0];
            progressDialog.show();
            String uri = DataUtils.getPodcastUri(PodNomsApplication.getContext(), id);
            if (!TextUtils.isEmpty(uri)) {
                ApiHandler handler = new ApiHandler();
                if (handler.deletePodcast(uri))
                    DataUtils.deletePodcast(PodNomsApplication.getContext(), id);


                ret = (long)0;
                progressDialog.cancel();
            }
            return ret;
        }

        @Override
        protected void onPostExecute(Long result) {
            super.onPostExecute(result);
        }
    }

}

