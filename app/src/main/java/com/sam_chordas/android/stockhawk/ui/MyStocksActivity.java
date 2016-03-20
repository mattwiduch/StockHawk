package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    private static final int CURSOR_LOADER_ID = 0;

    private Intent mServiceIntent;
    private ItemTouchHelper mItemTouchHelper;
    private QuoteCursorAdapter mCursorAdapter;
    private Context mContext;
    private Cursor mCursor;
    private TrackStockDialog mDialog;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Stetho.initializeWithDefaults(this);
        setContentView(R.layout.activity_my_stocks);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mContext = this;

        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately
        mServiceIntent = new Intent(this, StockIntentService.class);

        if (savedInstanceState == null) {
            // Run the initialize task service so that some stocks appear upon an empty database
            mServiceIntent.putExtra("tag", "init");
            if (Utils.isNetworkAvailable(this)) {
                startService(mServiceIntent);
            } else {
                networkSnackbar();
            }
        }

        // Prepare RecyclerView
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        View emptyView = findViewById(R.id.recycler_view_empty);
        mCursorAdapter = new QuoteCursorAdapter(this, null, emptyView);
        recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        //TODO:
                        // do something on item click
                    }
                }));
        recyclerView.setAdapter(mCursorAdapter);
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mServiceIntent.putExtra("tag", "init");
                if (Utils.isNetworkAvailable(mContext)) {
                    startService(mServiceIntent);

                } else {
                    networkSnackbar();
                }
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Utils.isNetworkAvailable(mContext)) {
                    mDialog = new TrackStockDialog();
                    mDialog.show(getSupportFragmentManager(), "TRACK_STOCK_DIALOG");
                } else {
                    networkSnackbar();
                }
            }
        });

        if (Utils.isNetworkAvailable(this)) {
            long period = 3600L;
            long flex = 10L;
            String periodicTag = "periodic";

            // create a periodic task to pull stocks once every hour after the app has been opened. This
            // is so Widget data stays up to date.
            PeriodicTask periodicTask = new PeriodicTask.Builder()
                    .setService(StockTaskService.class)
                    .setPeriod(period)
                    .setFlex(flex)
                    .setTag(periodicTag)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();
            // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
            // are updated.
            GcmNetworkManager.getInstance(this).schedule(periodicTask);
        }
    }

    @Override
    protected void onPause() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(this);
        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_change_units) {
            // this is for changing stock changes from percent value to dollar value
            Utils.showPercent = !Utils.showPercent;
            this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
        }

        return super.onOptionsItemSelected(item);
    }

    private void networkSnackbar() {
        mSwipeRefreshLayout.setRefreshing(false);
        Snackbar snackbar = Snackbar
                .make(findViewById(R.id.layout_my_stocks), getString(R.string.error_no_network),
                        Snackbar.LENGTH_INDEFINITE)
                .setAction("RETRY", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mServiceIntent.putExtra("tag", "init");
                        if (Utils.isNetworkAvailable(mContext)) {
                            startService(mServiceIntent);
                        } else {
                            networkSnackbar();
                        }
                    }
                })
                .setActionTextColor(ContextCompat.getColor(mContext, R.color.material_green_A400));
        snackbar.show();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
        mCursor = data;
        mSwipeRefreshLayout.setEnabled(mCursorAdapter.getItemCount() > 1);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_hawk_status_key))) {
            @StockTaskService.HawkStatus int status = Utils.getHawkStatus(this);
            if (status != StockTaskService.HAWK_STATUS_UNKNOWN &&
                    status != StockTaskService.HAWK_STATUS_SYMBOL_INVALID && mDialog != null) {
                mDialog.dismiss();
            }
            switch (status) {
                case StockTaskService.HAWK_STATUS_UNKNOWN:
                    break;
                case StockTaskService.HAWK_STATUS_SERVER_DOWN:
                    showSnackbar(getString(R.string.error_server_down));
                    Utils.resetHawkStatus(this);
                    break;
                case StockTaskService.HAWK_STATUS_SERVER_INVALID:
                    showSnackbar(getString(R.string.error_server_invalid));
                    Utils.resetHawkStatus(this);
                    break;
                case StockTaskService.HAWK_STATUS_SYMBOL_INVALID:
                    if (mDialog != null) {
                        mDialog.setErrorMessage(getString(R.string.error_symbol_invalid));
                    }
                    Utils.resetHawkStatus(this);
                    break;
                case StockTaskService.HAWK_STATUS_DATA_CORRUPTED:
                    showSnackbar(getString(R.string.error_corrupted_data));
                    Utils.resetHawkStatus(this);
                    break;
                case StockTaskService.HAWK_STATUS_UTF8_NOT_SUPPORTED:
                    showSnackbar(getString(R.string.error_utf8_not_supported));
                    Utils.resetHawkStatus(this);
                    break;
                case StockTaskService.HAWK_STATUS_OK:
                    Utils.resetHawkStatus(this);
                    break;
                default:
                    break;
            }
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    /*
        Shows snackbar with provided message.
     */
    private void showSnackbar(String message) {
        Snackbar snackbar = Snackbar
                .make(findViewById(R.id.layout_my_stocks), message, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mDialog = (TrackStockDialog) getSupportFragmentManager().findFragmentByTag("TRACK_STOCK_DIALOG");
    }
}
