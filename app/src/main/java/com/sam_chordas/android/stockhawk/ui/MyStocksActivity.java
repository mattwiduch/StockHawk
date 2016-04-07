package com.sam_chordas.android.stockhawk.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.facebook.stetho.Stetho;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;

public class MyStocksActivity extends AppCompatActivity implements MyStocksFragment.Callback {

    public static final String GRAPH_FRAGMENT_TAG = "GF_TAG";

    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Stetho.initializeWithDefaults(this);
        AndroidThreeTen.init(getApplication());
        setContentView(R.layout.activity_my_stocks);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(R.mipmap.ic_toolbar);
        }

        if (findViewById(R.id.stock_detail_container) != null) {
            // The detail container view will be present only in the large-screen layouts
            // (res/layout-sw600dp). If this view is present, then the activity should be
            // in two-pane mode.
            mTwoPane = true;
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (savedInstanceState == null) {
                Cursor data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                        new String[]{QuoteColumns.SYMBOL},
                        QuoteColumns.IS_CURRENT + " = ?",
                        new String[]{"1"},
                        PreferenceManager.getDefaultSharedPreferences(this).
                                getString(getString(R.string.pref_sort_order), "null"));

                LineGraphFragment fragment = new LineGraphFragment();

                if (data != null && data.moveToFirst()) {
                    Bundle args = new Bundle();
                    args.putString(LineGraphFragment.LGF_SYMBOL, data.getString(
                            data.getColumnIndex(QuoteColumns.SYMBOL)));
                    fragment.setArguments(args);
                    data.close();
                }

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.stock_detail_container, fragment, GRAPH_FRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;
        }

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
                    .setUpdateCurrent(true)
                    .build();
            // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
            // are updated.
            GcmNetworkManager.getInstance(this).schedule(periodicTask);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String symbol = intent.getStringExtra(LineGraphFragment.LGF_SYMBOL);
        if (symbol != null) {
            Bundle args = new Bundle();
            args.putString(LineGraphFragment.LGF_SYMBOL, symbol);

            LineGraphFragment fragment = new LineGraphFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.stock_detail_container, fragment, GRAPH_FRAGMENT_TAG)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(String symbol) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle args = new Bundle();
            args.putString(LineGraphFragment.LGF_SYMBOL, symbol);

            LineGraphFragment fragment = new LineGraphFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.stock_detail_container, fragment, GRAPH_FRAGMENT_TAG)
                    .commit();
        } else {
            Intent intent = new Intent(this, LineGraphActivity.class)
                    .putExtra(LineGraphFragment.LGF_SYMBOL, symbol);

            ActivityCompat.startActivity(this, intent, null);
        }
    }
}
