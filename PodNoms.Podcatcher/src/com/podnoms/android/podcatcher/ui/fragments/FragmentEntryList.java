package com.podnoms.android.podcatcher.ui.fragments;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.podnoms.android.podcatcher.R;
import com.podnoms.android.podcatcher.providers.PodNomsInterface;
import com.podnoms.android.podcatcher.providers.sync.PodNomsSyncAdapter;
import com.podnoms.android.podcatcher.services.DownloadService;
import com.podnoms.android.podcatcher.ui.BaseActivity;
import com.podnoms.android.podcatcher.ui.activities.phone.ActivityEntryDetails;
import com.podnoms.android.podcatcher.ui.adapters.PodcastListAdapter;
import com.podnoms.android.podcatcher.ui.adapters.lazy.LazyAdapter;
import com.podnoms.android.podcatcher.util.LogHandler;
import com.podnoms.android.podcatcher.util.state.Constants;
import com.podnoms.android.podcatcher.util.state.PersistentStateHandler;

public class FragmentEntryList extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor>, ActionMode.Callback {

    private LazyAdapter _adapter;
    private Uri _contentUri;
    private long _entryId = -1;

    private final int LOADER_SORT_DATE_DESC = 5;
    private final int LOADER_SORT_PLAYED_DESC = 6;
    private LoaderManager loaderManager;

    public static Fragment newInstance(Uri contentUri) {
        FragmentEntryList pageFragment = new FragmentEntryList();
        Bundle bundle = new Bundle();
        bundle.putParcelable("content_uri", contentUri);
        pageFragment.setArguments(bundle);
        return pageFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        _rootView = (ViewGroup) inflater.inflate(R.layout.fragment_podcastlist, null);
        final ListView listView = (ListView) _rootView.findViewById(R.id.fragment_podcastlist_list);

        //depending on entry method, either we'll have
        // 1. arguments (if we were instantiated)
        // 2. intent data from the Activity
        if (getArguments() != null && getArguments().containsKey("content_uri"))
            _contentUri = (Uri) getArguments().get("content_uri");
        else
            _contentUri = (Uri) getActivity().getIntent().getExtras().get("content_uri");
        String[] uiBindFrom = {PodNomsInterface.Entry.COLUMN_NAME_TITLE, PodNomsInterface.Entry.COLUMN_NAME_DESCRIPTION};
        int[] uiBindTo = {R.id.include_podcast_entry_desc_firstline, R.id.include_podcast_entry_desc_secondline};

        loaderManager = getLoaderManager();
        loaderManager.initLoader(PodNomsInterface.LOADER_ENTRY_LIST_DEFAULT, null, this);
        _adapter = new PodcastListAdapter(
                getActivity(),
                R.layout.listrow_entry,
                null,
                uiBindFrom,
                uiBindTo,
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        );
        listView.setAdapter(_adapter);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                _entryId = id;
                FragmentEntryList.this.getSherlockActivity().startActionMode(FragmentEntryList.this);
                listView.setSelected(true);
                return true;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                _entryId = id;
                Cursor cursor = (Cursor) _adapter.getItem(position);
                _showPodcastDetails(_entryId, cursor);
            }
        });

        setHasOptionsMenu(true);
        return _rootView;
    }

    public void _showPodcastDetails(long podcastEntryId, Cursor c) {
        final Uri entriesUri = PodNomsInterface.Entry.buildPodcastDetailsUri(podcastEntryId);
        final Intent intent = new Intent(this.getActivity(), ActivityEntryDetails.class);
        intent.putExtra("uri", entriesUri);
        intent.putExtra("file", c.getString(c.getColumnIndex(PodNomsInterface.Entry.COLUMN_NAME_LOCAL_FILE)));
        intent.putExtra("title", c.getString(c.getColumnIndex(PodNomsInterface.Entry.COLUMN_NAME_PODCAST_TITLE)));
        intent.putExtra("description", c.getString(c.getColumnIndex(PodNomsInterface.Entry.COLUMN_NAME_DESCRIPTION)));
        intent.putExtra("position", c.getLong(c.getColumnIndex(PodNomsInterface.Entry.COLUMN_NAME_POSITION)));
        intent.putExtra("entry_id", c.getLong(c.getColumnIndex(PodNomsInterface.Entry._ID)));

        ((BaseActivity) getActivity()).openActivityOrFragment(intent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String sort;
        switch (i) {
            case PodNomsInterface.LOADER_ENTRY_LIST_DEFAULT:
            case PodNomsInterface.LOADER_ENTRY_LIST_SORT_PLAYED:
            default:
                sort = String.format("%s.%s DESC", PodNomsInterface.Entry.TABLE_NAME, PodNomsInterface.Entry.COLUMN_NAME_DATE_UPDATED);
                break;
            case PodNomsInterface.LOADER_ENTRY_LIST_SORT_DATE:
                sort = String.format("%s.%s DESC", PodNomsInterface.Entry.TABLE_NAME, PodNomsInterface.Entry.COLUMN_NAME_DATE_CREATED);
                break;
        }
        return new CursorLoader(
                getActivity(),
                _contentUri,
                PodNomsInterface.Entry.getProjectionMap(),
                null, null, sort);
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_entry, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_entry_sort_newest:
                getLoaderManager().initLoader(PodNomsInterface.LOADER_ENTRY_LIST_SORT_DATE, null, this);
                return true;
            case R.id.menu_entry_sort_last_played:
                getLoaderManager().initLoader(PodNomsInterface.LOADER_ENTRY_LIST_SORT_PLAYED, null, this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.menu_action_podcast_entry, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_podcast_entry_download:
                PodNomsSyncAdapter adapter =
                        new PodNomsSyncAdapter(getActivity(), DownloadService.MESSAGE_STARTDOWNLOADFOREPISODE, PersistentStateHandler.I().getString(Constants.AUTH_TOKEN));
                adapter.startSyncForEntry(_entryId);
                mode.finish();
                return true;
            case R.id.menu_podcast_entry_marklistened:
                ContentValues values = new ContentValues();
                values.put("position", -9999);
                getActivity().getContentResolver().update(PodNomsInterface.Entry.CONTENT_URI, values, String.format("_id = %d", _entryId), null);
                getActivity().getContentResolver().notifyChange(_contentUri, null);
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {

    }
}
