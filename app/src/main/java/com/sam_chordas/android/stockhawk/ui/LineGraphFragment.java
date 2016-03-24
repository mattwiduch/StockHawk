package com.sam_chordas.android.stockhawk.ui;

import android.database.Cursor;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.db.chart.Tools;
import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.ChartView;
import com.db.chart.view.LineChartView;
import com.db.chart.view.animation.Animation;
import com.db.chart.view.animation.easing.CircEase;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import java.text.DecimalFormat;

import butterknife.Bind;
import butterknife.ButterKnife;

public class LineGraphFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int CURSOR_LOADER_ID = 1;
    private Toolbar mToolbar;
    private LineChartView mChart;
    private String mStockSymbol;

    @Bind(R.id.stock_symbol_textview)
    TextView stockSymbolTextview;
    @Bind(R.id.stock_price_textview)
    TextView stockPriceTextview;
    @Bind(R.id.stock_change_textview)
    TextView stockChangeTextview;
    @Bind(R.id.stock_prev_close_textview)
    TextView stockPrevCloseTextview;
    @Bind(R.id.stock_open_textview)
    TextView stockOpenTextview;
    @Bind(R.id.stock_low_textview)
    TextView stockLowTextview;
    @Bind(R.id.stock_high_textview)
    TextView stockHighTextview;
    @Bind(R.id.stock_52wk_low_textview)
    TextView stock52wkLowTextview;
    @Bind(R.id.stock_52wk_high_textview)
    TextView stock52wkHighTextview;
    @Bind(R.id.stock_mkt_capital_textview)
    TextView stockMktCapitalTextview;
    @Bind(R.id.stock_volume_textview)
    TextView stockVolumeTextview;
    @Bind(R.id.stock_1y_target_textview)
    TextView stock1yTargetTextview;
    @Bind(R.id.stock_avg_volume_textview)
    TextView stockAvgVolumeTextview;

    public LineGraphFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mStockSymbol = getActivity().getIntent().getStringExtra(getString(R.string.line_graph_extra));
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_line_graph, container, false);
        ButterKnife.bind(this, rootView);
        mToolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        mChart = (LineChartView) rootView.findViewById(R.id.line_chart);
        return rootView;
    }

    private void buildLineGraph(float[] values, String[] labels, float minBid, float maxBid, int isUp) {
        // Line chart customization
        LineSet dataSet = new LineSet(labels, values);
        dataSet.setThickness(Tools.fromDpToPx(2.5f));
        dataSet.setColor(ContextCompat.getColor(getContext(), android.R.color.white));
        mChart.addData(dataSet);

        // Generic chart customization
        @ColorInt
        int bgColor = isUp == 1 ? ContextCompat.getColor(getContext(), R.color.green_high)
                : ContextCompat.getColor(getContext(), R.color.red_low);
        mChart.setXAxis(false);
        mChart.setYAxis(false);
        mChart.setBackgroundColor(bgColor);
        int padding = getResources().getDimensionPixelSize(R.dimen.content_padding);
        mChart.setPadding(padding, padding * 3, padding, padding + padding / 2);

        // Paint object used to draw Grid
        Paint gridPaint = new Paint();
        gridPaint.setColor(ContextCompat.getColor(getContext(),R.color.text_light_hint));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setAntiAlias(true);
        gridPaint.setStrokeWidth(Tools.fromDpToPx(1f));
        gridPaint.setPathEffect(new DashPathEffect(new float[]{8.0f, 8.0f}, 0));
        mChart.setGrid(ChartView.GridType.HORIZONTAL, 10, 10, gridPaint);

        // Labels
        mChart.setXLabels(AxisController.LabelPosition.NONE);
        mChart.setYLabels(AxisController.LabelPosition.OUTSIDE);
        // Calculate min and max values to display on Y axis
        int minValue, maxValue;
        if (minBid - (int) minBid < 0.5) {
            minValue = (int) minBid > 0 ? (int) minBid - 1 : 0;
        } else {
            minValue = (int) minBid;
        }
        if (maxBid - (int) maxBid < 0.5) {
            maxValue = (int) maxBid + 1;
        } else {
            maxValue = (int) maxBid + 2;
        }
        // Set Y axis labels using custom step
        int step = maxValue - minValue;
        if (step == 1) {
            mChart.setAxisBorderValues(minValue, maxValue);
        } else if (step % 2 == 1) {
            mChart.setAxisBorderValues(minValue, maxValue + 1, (step + 1) / 2);
        } else {
            mChart.setAxisBorderValues(minValue, maxValue, step / 2);
        }

        mChart.setLabelsFormat(new DecimalFormat("#"));
        mChart.setLabelsColor(ContextCompat.getColor(getContext(), R.color.text_light_secondary));
        mChart.setFontSize(45);
        mChart.setAxisLabelsSpacing(48f);

        // Animation customization
        Animation anim = new Animation();
        anim.setEasing(new CircEase());
        anim.setOverlap(0.5f, new int[]{3, 2, 4, 1, 7, 5, 0, 6, 8});
        anim.setStartPoint(0.0f, 1.0f);
        //mChart.show(anim);
        mChart.show();
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),
                QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.NAME, QuoteColumns.BID_PRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.CREATED, QuoteColumns.IS_UP},
                QuoteColumns.SYMBOL + " = ?",
                new String[]{mStockSymbol},
                QuoteColumns.CREATED + " ASC");
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        if (data.moveToLast()) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (mToolbar != null) {
                activity.setSupportActionBar(mToolbar);
                ActionBar actionBar = activity.getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setDisplayHomeAsUpEnabled(true);
                    actionBar.setTitle(data.getString(data.getColumnIndex(QuoteColumns.NAME)));
                }
            }

            int isUp = data.getInt(data.getColumnIndex(QuoteColumns.IS_UP));
            stockSymbolTextview.setText(mStockSymbol);
            stockPriceTextview.setText(data.getString(data.getColumnIndex(QuoteColumns.BID_PRICE)));
            stockChangeTextview.setText(data.getString(data.getColumnIndex(QuoteColumns.PERCENT_CHANGE)));
            stockPrevCloseTextview.setText(getResources().getString(R.string.data_not_available));
            stockOpenTextview.setText(getResources().getString(R.string.data_not_available));
            stockLowTextview.setText(getResources().getString(R.string.data_not_available));
            stockHighTextview.setText(getResources().getString(R.string.data_not_available));
            stock52wkLowTextview.setText(getResources().getString(R.string.data_not_available));
            stock52wkHighTextview.setText(getResources().getString(R.string.data_not_available));
            stockMktCapitalTextview.setText(getResources().getString(R.string.data_not_available));
            stockVolumeTextview.setText(getResources().getString(R.string.data_not_available));
            stock1yTargetTextview.setText(getResources().getString(R.string.data_not_available));
            stockAvgVolumeTextview.setText(getResources().getString(R.string.data_not_available));

            // Prepare data to be displayed on the graph
            float[] stockValues = new float[data.getCount()];
            String[] stockLabels = new String[data.getCount()];
            float minBid = Float.MAX_VALUE;
            float maxBid = Float.MIN_VALUE;

            for (int position = 0; position < data.getCount(); position++) {
                data.moveToPosition(position);
                String bid = data.getString(data.getColumnIndex(QuoteColumns.BID_PRICE));
                stockValues[position] = bid.equals(getString(R.string.data_not_available))
                        ? 0f : Float.parseFloat(bid);
                minBid = Math.min(minBid, stockValues[position]);
                maxBid = Math.max(maxBid, stockValues[position]);
                stockLabels[position] = "";
            }

            // Duplicate data point if there is only one so line always shows on the graph
            if (data.getCount() == 1) {
                stockValues = new float[]{stockValues[0], stockValues[0]};
                stockLabels = new String[]{stockLabels[0], stockLabels[0]};
            }

            buildLineGraph(stockValues, stockLabels, minBid, maxBid, isUp);
        }
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
    }
}
