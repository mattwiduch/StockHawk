package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.facebook.stetho.Stetho;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.jakewharton.threetenabp.AndroidThreeTen;
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
    private static final String DIALOG_TAG = "track_stock_dialog";

    private static final String SORT_DEFAULT = "null";
    private static final String SORT_SYMBOL_ASC = QuoteColumns.SYMBOL + " ASC";
    private static final String SORT_SYMBOL_DSC = QuoteColumns.SYMBOL + " DESC";
    private static final String SORT_PRICE_ASC = QuoteColumns.BID_PRICE + " ASC";
    private static final String SORT_PRICE_DSC = QuoteColumns.BID_PRICE + " DESC";
    private static final String SORT_CHANGE_ASC = QuoteColumns.PERCENT_CHANGE + " ASC";
    private static final String SORT_CHANGE_DSC = QuoteColumns.PERCENT_CHANGE + " DESC";

    private Intent mServiceIntent;
    private ItemTouchHelper mItemTouchHelper;
    private QuoteCursorAdapter mCursorAdapter;
    private Context mContext;
    private Cursor mCursor;
    private TrackStockDialog mDialog;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private String mSortOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Stetho.initializeWithDefaults(this);
        AndroidThreeTen.init(getApplication());
        setContentView(R.layout.activity_my_stocks);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mContext = this;

        // Retrieve user's preferred sort order
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        mSortOrder = sp.getString(getString(R.string.pref_sort_order), SORT_DEFAULT);

        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately
        mServiceIntent = new Intent(this, StockIntentService.class);

        if (savedInstanceState == null) {
            // Run the initialize task service so that some stocks appear upon an empty database
            mServiceIntent.putExtra(StockIntentService.TASK_TAG, StockIntentService.TASK_TYPE_INIT);
            if (Utils.isNetworkAvailable(this)) {
                startService(mServiceIntent);
            } else {
                networkSnackbar();
            }
        }

        // Prepare RecyclerView
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, R.drawable.divider));

        View emptyView = findViewById(R.id.recycler_view_empty);
        mCursorAdapter = new QuoteCursorAdapter(this, null, emptyView);

        recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        if (position < mCursor.getCount()) {
                            mCursor = mCursorAdapter.getCursor();
                            mCursor.moveToPosition(position);
                            String symbol = mCursor.getString(mCursor.getColumnIndex("symbol"));
                            Intent intent = new Intent(MyStocksActivity.this, LineGraphActivity.class)
                                    .putExtra(getString(R.string.line_graph_extra), symbol);

                            ActivityCompat.startActivity(MyStocksActivity.this, intent, null);
                        }
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
                mServiceIntent.putExtra(StockIntentService.TASK_TAG, StockIntentService.TASK_TYPE_INIT);
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
                    mDialog.show(getSupportFragmentManager(), DIALOG_TAG);
                } else {
                    networkSnackbar();
                }
            }
        });

        if (Utils.isNetworkAvailable(this)) {
            long period = 3600L;
            long flex = 10L;

            // create a periodic task to pull stocks once every hour after the app has been opened. This
            // is so Widget data stays up to date.
            PeriodicTask periodicTask = new PeriodicTask.Builder()
                    .setService(StockTaskService.class)
                    .setPeriod(period)
                    .setFlex(flex)
                    .setTag(StockIntentService.TASK_TYPE_PERIODIC)
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
            if (Utils.showPercent) {
                item.setIcon(R.drawable.ic_action_percent_white);
            } else {
                item.setIcon(R.drawable.ic_attach_money_white_24dp);
            }
            Utils.showPercent = !Utils.showPercent;
            this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
        }

        if (id == R.id.action_sort) {
            createSortDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Creates dialog that lets user choose stock sorting method.
     */
    private void createSortDialog() {
        int defaultChoice = 0;
        if (mSortOrder.equals(SORT_SYMBOL_ASC)) defaultChoice = 1;
        if (mSortOrder.equals(SORT_SYMBOL_DSC)) defaultChoice = 2;
        if (mSortOrder.equals(SORT_PRICE_ASC)) defaultChoice = 3;
        if (mSortOrder.equals(SORT_PRICE_DSC)) defaultChoice = 4;
        if (mSortOrder.equals(SORT_CHANGE_ASC)) defaultChoice = 5;
        if (mSortOrder.equals(SORT_CHANGE_DSC)) defaultChoice = 6;

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.DialogSortStocks);
        dialogBuilder.setTitle(R.string.sort_dialog_title);
        dialogBuilder.setSingleChoiceItems(R.array.sort_type, defaultChoice, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MyStocksActivity.this);
                SharedPreferences.Editor spe = sp.edit();

                switch (which) {
                    case 0:
                        spe.putString(MyStocksActivity.this.getString(R.string.pref_sort_order), SORT_DEFAULT);
                        break;
                    case 1:
                        spe.putString(MyStocksActivity.this.getString(R.string.pref_sort_order), SORT_SYMBOL_ASC);
                        break;
                    case 2:
                        spe.putString(MyStocksActivity.this.getString(R.string.pref_sort_order), SORT_SYMBOL_DSC);
                        break;
                    case 3:
                        spe.putString(MyStocksActivity.this.getString(R.string.pref_sort_order), SORT_PRICE_ASC);
                        break;
                    case 4:
                        spe.putString(MyStocksActivity.this.getString(R.string.pref_sort_order), SORT_PRICE_DSC);
                        break;
                    case 5:
                        spe.putString(MyStocksActivity.this.getString(R.string.pref_sort_order), SORT_CHANGE_ASC);
                        break;
                    case 6:
                        spe.putString(MyStocksActivity.this.getString(R.string.pref_sort_order), SORT_CHANGE_DSC);
                        break;
                }

                spe.apply();
                dialog.dismiss();
                getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, MyStocksActivity.this);
            }
        });

        // Creates dialog
        AlertDialog sortDialog = dialogBuilder.create();
        // Shows dialog
        sortDialog.show();
    }


    private void networkSnackbar() {
        mSwipeRefreshLayout.setRefreshing(false);
        Snackbar snackbar = Snackbar
                .make(findViewById(R.id.layout_my_stocks), getString(R.string.error_no_network),
                        Snackbar.LENGTH_INDEFINITE)
                .setAction("RETRY", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mServiceIntent.putExtra(StockIntentService.TASK_TAG,
                                StockIntentService.TASK_TYPE_INIT);
                        if (Utils.isNetworkAvailable(mContext)) {
                            startService(mServiceIntent);
                        } else {
                            networkSnackbar();
                        }
                    }
                })
                .setActionTextColor(ContextCompat.getColor(mContext, R.color.green_action));
        snackbar.show();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.NAME, QuoteColumns.BID_PRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.IS_UP},
                QuoteColumns.IS_CURRENT + " = ?",
                new String[]{"1"},
                mSortOrder);
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
        // Stock Hawk Status
        if (key.equals(getString(R.string.pref_hawk_status_key))) {
            @StockTaskService.HawkStatus int status = Utils.getHawkStatus(this);
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
                    if (mDialog != null)
                        mDialog.setErrorMessage(getString(R.string.error_symbol_invalid));
                    break;
                case StockTaskService.HAWK_STATUS_DATA_CORRUPTED:
                    showSnackbar(getString(R.string.error_corrupted_data));
                    break;
                case StockTaskService.HAWK_STATUS_UTF8_NOT_SUPPORTED:
                    showSnackbar(getString(R.string.error_utf8_not_supported));
                    break;
                case StockTaskService.HAWK_STATUS_OK:
                    break;
                default:
                    break;
            }

            if (status != StockTaskService.HAWK_STATUS_UNKNOWN
                    && status != StockTaskService.HAWK_STATUS_SYMBOL_INVALID) {
                if (mDialog != null) mDialog.dismiss();
            }
            mSwipeRefreshLayout.setRefreshing(false);
        }
        // Sort Order
        if (key.equals(getString(R.string.pref_sort_order))) {
            mSortOrder = sharedPreferences.getString(getString(R.string.pref_sort_order), SORT_DEFAULT);
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
        mDialog = (TrackStockDialog) getSupportFragmentManager().findFragmentByTag(DIALOG_TAG);
    }
}
