/*
 * Copyright (C) 2016 Mateusz Widuch
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
package com.sam_chordas.android.stockhawk.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

/**
 * Presents RecyclerView that contains list of stocks and their respective data.
 */
public class MyStocksFragment extends Fragment implements android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int CURSOR_LOADER_ID = 0;
    private static final String DIALOG_TAG = "Dialog.trackSymbol";
    private static final String RECYCLER_VIEW_FOCUSED_ITEM_KEY = "RecyclerView.focusedItem";
    private static final String RECYCLER_VIEW_STATE_KEY = "RecyclerView.state";
    private static final String DIALOG_SORT_STOCKS_STATE_KEY = "Dialog.SortStocks.state";

    private static final String SORT_DEFAULT = "null";
    private static final String SORT_SYMBOL_ASC = QuoteColumns.SYMBOL + " ASC";
    private static final String SORT_SYMBOL_DSC = QuoteColumns.SYMBOL + " DESC";
    private static final String SORT_PRICE_ASC = QuoteColumns.BID_PRICE + " ASC";
    private static final String SORT_PRICE_DSC = QuoteColumns.BID_PRICE + " DESC";
    private static final String SORT_CHANGE_ASC = QuoteColumns.PERCENT_CHANGE + " ASC";
    private static final String SORT_CHANGE_DSC = QuoteColumns.PERCENT_CHANGE + " DESC";
    private static Bundle mStateBundle;
    private Intent mServiceIntent;
    private RecyclerView mRecyclerView;
    private int mFocusedItemPosition;
    private QuoteCursorAdapter mCursorAdapter;
    private AppCompatActivity mActivity;
    private View mRootView;
    private Cursor mCursor;
    private DialogTrackStock mDialogTrackStock;
    private AlertDialog mDialogSortStocks;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private String mSortOrder;
    private boolean mIsPercent;

    public MyStocksFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mActivity = (AppCompatActivity) getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_my_stocks, container, false);
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        mActivity.setSupportActionBar(toolbar);

        // Retrieve user's preferred sort order
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mActivity);
        mSortOrder = sp.getString(getString(R.string.pref_sort_order), SORT_DEFAULT);
        mIsPercent = sp.getBoolean(getString(R.string.pref_units_key), true);

        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately
        mServiceIntent = new Intent(mActivity, StockIntentService.class);

        // Prepare RecyclerView
        mRecyclerView = (RecyclerView) mRootView.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(mActivity, R.drawable.divider));

        View emptyView = mRootView.findViewById(R.id.recycler_view_empty);
        mCursorAdapter = new QuoteCursorAdapter(mActivity, null, emptyView);

        mRecyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(mActivity,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        if (position < mCursor.getCount()) {
                            mCursor = mCursorAdapter.getCursor();
                            mCursor.moveToPosition(position);
                            String symbol = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL));
                            // TODO: Remove haptic feedback on item touch
                            ((Callback) getActivity()).onItemSelected(symbol);
                        }
                    }
                }));
        mRecyclerView.setAdapter(mCursorAdapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(mRecyclerView);

        mSwipeRefreshLayout = (SwipeRefreshLayout) mRootView.findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mServiceIntent.putExtra(StockIntentService.TASK_TAG, StockIntentService.TASK_TYPE_INIT);
                if (Utils.isNetworkAvailable(mActivity)) {
                    mActivity.startService(mServiceIntent);
                } else {
                    networkSnackbar();
                }
            }
        });

        // TODO: Show loading indicator when app launches
//        mSwipeRefreshLayout.setEnabled(true);
//        mSwipeRefreshLayout.measure(View.MEASURED_SIZE_MASK,View.MEASURED_HEIGHT_STATE_SHIFT);
//        mSwipeRefreshLayout.setRefreshing(true);
//        mSwipeRefreshLayout.post(new Runnable() {
//            @Override
//            public void run() {
//                mSwipeRefreshLayout.setRefreshing(true);
//            }
//        });

        // Update last updated text view periodically
        final TextView lastUpdatedTextView = (TextView) mRootView.findViewById(R.id.last_update_textview);
        final Handler handler = new Handler();
        final Runnable updateTask = new Runnable() {
            @Override
            public void run() {
                if (lastUpdatedTextView != null) {
                    handler.postDelayed(this, 30000);
                }
            }
        };
        handler.postDelayed(updateTask, 1000);

        return mRootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            // Run the initialize task service so that some stocks appear upon an empty database
            mServiceIntent.putExtra(StockIntentService.TASK_TAG, StockIntentService.TASK_TYPE_INIT);
            if (Utils.isNetworkAvailable(mActivity)) {
                mActivity.startService(mServiceIntent);
            } else {
                networkSnackbar();
            }
        }

        // Set up FAB's on click listener
        FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Utils.isNetworkAvailable(mActivity)) {
                    mDialogTrackStock = new DialogTrackStock();
                    mDialogTrackStock.show(mActivity.getSupportFragmentManager(), DIALOG_TAG);
                } else {
                    networkSnackbar();
                }
            }
        });
        // Initialise loader manager
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister Preference Change Listener
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mActivity);
        sp.unregisterOnSharedPreferenceChangeListener(this);
        // Save RecyclerView state
        mStateBundle = new Bundle();
        Parcelable listState = mRecyclerView.getLayoutManager().onSaveInstanceState();
        mStateBundle.putParcelable(RECYCLER_VIEW_STATE_KEY, listState);
        // Save position of currently focused item
        mFocusedItemPosition = mRecyclerView.getChildAdapterPosition(mRecyclerView.getFocusedChild());
        mStateBundle.putInt(RECYCLER_VIEW_FOCUSED_ITEM_KEY, mFocusedItemPosition);
        // Send broadcast to widgets to update
        Intent dataUpdatedIntent = new Intent(StockTaskService.ACTION_DATA_UPDATED).setPackage(mActivity.getPackageName());
        mActivity.sendBroadcast(dataUpdatedIntent);
        // Dismiss sort stocks dialog if present
        if (mDialogSortStocks != null && mDialogSortStocks.isShowing()) {
            mDialogSortStocks.dismiss();
            mStateBundle.putBoolean(DIALOG_SORT_STOCKS_STATE_KEY, true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Restart loader to show latest data
        //getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, MyStocksFragment.this);
        // Register Shared Preference Change Listener
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mActivity);
        sp.registerOnSharedPreferenceChangeListener(this);
        // Restore RecyclerView state
        if (mStateBundle != null) {
            Parcelable listState = mStateBundle.getParcelable(RECYCLER_VIEW_STATE_KEY);
            mRecyclerView.getLayoutManager().onRestoreInstanceState(listState);
            // TODO: Clears previously selected view
            mCursorAdapter.notifyItemChanged(mCursorAdapter.getSelectedItem());
            // Restore position of previously focused item
            mFocusedItemPosition = mStateBundle.getInt(RECYCLER_VIEW_FOCUSED_ITEM_KEY);
            mCursorAdapter.setFocusedItem(mFocusedItemPosition);
            mRecyclerView.scrollToPosition(mFocusedItemPosition);
            if (mStateBundle.getBoolean(DIALOG_SORT_STOCKS_STATE_KEY, false)) {
                mDialogSortStocks = createSortDialog();
                mDialogSortStocks.show();
            }
        }
        mDialogTrackStock = (DialogTrackStock) mActivity.getSupportFragmentManager().findFragmentByTag(DIALOG_TAG);
        updateLastUpdateTime(sp);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.my_stocks_fragment, menu);
        menu.getItem(0).setTitle(mIsPercent ?
                getString(R.string.a11y_change_units, getString(R.string.a11y_currency))
                : getString(R.string.a11y_change_units, getString(R.string.a11y_percent)));
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_change_units) {
            // this is for changing stock changes from percent value to dollar value
            if (mIsPercent) {
                item.setIcon(R.drawable.ic_action_percent_white);
                item.setTitle(getString(R.string.a11y_change_units, getString(R.string.a11y_percent)));
            } else {
                item.setIcon(R.drawable.ic_attach_money_white_24dp);
                item.setTitle(getString(R.string.a11y_change_units, getString(R.string.a11y_currency)));
            }
            SharedPreferences.Editor spe = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
            spe.putBoolean(getString(R.string.pref_units_key), !mIsPercent);
            spe.apply();
            mActivity.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
        }

        if (id == R.id.action_sort) {
            mDialogSortStocks = createSortDialog();
            mDialogSortStocks.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(mActivity,
                QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.NAME, QuoteColumns.BID_PRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.IS_UP},
                QuoteColumns.IS_CURRENT + " = ?",
                new String[]{"1"},
                mSortOrder);
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
        mCursor = data;
        mSwipeRefreshLayout.setEnabled(mCursorAdapter.getItemCount() > 1);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Stock Hawk Status
        if (key.equals(getString(R.string.pref_hawk_status_key))) {
            @StockTaskService.HawkStatus int status = Utils.getHawkStatus(mActivity);
            switch (status) {
                case StockTaskService.HAWK_STATUS_UNKNOWN:
                    break;
                case StockTaskService.HAWK_STATUS_SERVER_DOWN:
                    showSnackbar(getString(R.string.error_server_down));
                    break;
                case StockTaskService.HAWK_STATUS_SERVER_INVALID:
                    showSnackbar(getString(R.string.error_server_invalid));
                    break;
                case StockTaskService.HAWK_STATUS_SYMBOL_INVALID:
                    if (mDialogTrackStock != null)
                        mDialogTrackStock.setErrorMessage(getString(R.string.dialog_track_error_symbol_invalid));
                    break;
                case StockTaskService.HAWK_STATUS_DATA_CORRUPTED:
                    showSnackbar(getString(R.string.error_corrupted_data));
                    break;
                case StockTaskService.HAWK_STATUS_UTF8_NOT_SUPPORTED:
                    showSnackbar(getString(R.string.error_utf8_not_supported));
                    break;
                case StockTaskService.HAWK_STATUS_OK:
                    // Refresh details fragment data if app is in two pane mode
                    LineGraphFragment lgf = (LineGraphFragment) mActivity.getSupportFragmentManager().findFragmentByTag(MyStocksActivity.GRAPH_FRAGMENT_TAG);
                    if (null != lgf) {
                        lgf.onDatabaseUpdate();
                    }
                    break;
                default:
                    break;
            }

            if (status != StockTaskService.HAWK_STATUS_UNKNOWN
                    && status != StockTaskService.HAWK_STATUS_SYMBOL_INVALID) {
                if (mDialogTrackStock != null) mDialogTrackStock.dismiss();
            }
            mSwipeRefreshLayout.setRefreshing(false);
        }
        // Sort Order
        if (key.equals(getString(R.string.pref_sort_order))) {
            mSortOrder = sharedPreferences.getString(getString(R.string.pref_sort_order), SORT_DEFAULT);
        }
        // Last Update Time
        if (key.equals(getString(R.string.pref_last_update))) {
            updateLastUpdateTime(sharedPreferences);
        }
        // Preferred units
        if (key.equals(getString(R.string.pref_units_key))) {
            mIsPercent = sharedPreferences.getBoolean(getString(R.string.pref_units_key), true);
        }
    }

    /**
     * Creates dialog that lets user choose stock sorting method.
     */
    private AlertDialog createSortDialog() {
        int defaultChoice = 0;
        if (mSortOrder.equals(SORT_SYMBOL_ASC)) defaultChoice = 1;
        if (mSortOrder.equals(SORT_SYMBOL_DSC)) defaultChoice = 2;
        if (mSortOrder.equals(SORT_PRICE_ASC)) defaultChoice = 3;
        if (mSortOrder.equals(SORT_PRICE_DSC)) defaultChoice = 4;
        if (mSortOrder.equals(SORT_CHANGE_ASC)) defaultChoice = 5;
        if (mSortOrder.equals(SORT_CHANGE_DSC)) defaultChoice = 6;

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mActivity, R.style.AppTheme_Dialog_Alert);
        dialogBuilder.setTitle(R.string.sort_dialog_title);
        dialogBuilder.setSingleChoiceItems(R.array.sort_type, defaultChoice, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mActivity);
                SharedPreferences.Editor spe = sp.edit();

                switch (which) {
                    case 0:
                        spe.putString(mActivity.getString(R.string.pref_sort_order), SORT_DEFAULT);
                        break;
                    case 1:
                        spe.putString(mActivity.getString(R.string.pref_sort_order), SORT_SYMBOL_ASC);
                        break;
                    case 2:
                        spe.putString(mActivity.getString(R.string.pref_sort_order), SORT_SYMBOL_DSC);
                        break;
                    case 3:
                        spe.putString(mActivity.getString(R.string.pref_sort_order), SORT_PRICE_ASC);
                        break;
                    case 4:
                        spe.putString(mActivity.getString(R.string.pref_sort_order), SORT_PRICE_DSC);
                        break;
                    case 5:
                        spe.putString(mActivity.getString(R.string.pref_sort_order), SORT_CHANGE_ASC);
                        break;
                    case 6:
                        spe.putString(mActivity.getString(R.string.pref_sort_order), SORT_CHANGE_DSC);
                        break;
                }

                spe.apply();
                dialog.dismiss();
                getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, MyStocksFragment.this);
            }
        });
        dialogBuilder.setNegativeButton(R.string.dialog_button_negative, null);
        return dialogBuilder.create();
    }

    private void showSnackbar(String message) {
        Snackbar snackbar = Snackbar
                .make(getActivity().findViewById(R.id.activity_my_stocks), message, Snackbar.LENGTH_LONG);
        View snackBarView = snackbar.getView();
        snackBarView.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.grey_card));
        snackbar.show();
    }

    private void networkSnackbar() {
        if (mSwipeRefreshLayout != null) mSwipeRefreshLayout.setRefreshing(false);
        Snackbar snackbar = Snackbar
                .make(getActivity().findViewById(R.id.activity_my_stocks), getString(R.string.error_no_network),
                        Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.network_snackbar_button_retry, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mServiceIntent.putExtra(StockIntentService.TASK_TAG,
                                StockIntentService.TASK_TYPE_INIT);
                        if (Utils.isNetworkAvailable(mActivity)) {
                            mActivity.startService(mServiceIntent);
                        } else {
                            networkSnackbar();
                        }
                    }
                })
                .setActionTextColor(ContextCompat.getColor(mActivity, R.color.green_action));
        View snackBarView = snackbar.getView();
        snackBarView.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.grey_card));
        snackbar.show();
    }

    /**
     * Updates text view that shows last update time.
     */
    private void updateLastUpdateTime(SharedPreferences sharedPreferences) {
        TextView textView = (TextView) mRootView.findViewById(R.id.last_update_textview);
        if (textView != null) {
            String updateTime = sharedPreferences.getString(getString(R.string.pref_last_update),
                    getString(R.string.last_updated_never_key));

            if (updateTime.equals(getString(R.string.last_updated_never_key))) {
                textView.setText(getString(R.string.last_updated_never));
            } else {
                textView.setText(getString(R.string.last_updated, Utils.formatLastUpdateTime(
                        mActivity, updateTime)));
            }
        }
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        void onItemSelected(String symbol);
    }
}
