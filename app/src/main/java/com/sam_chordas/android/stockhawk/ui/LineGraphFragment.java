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
import com.db.chart.view.animation.easing.LinearEase;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class LineGraphFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int CURSOR_LOADER_ID = 1;
    private String mStockSymbol;

    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.line_chart)
    LineChartView lineChart;
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
        //getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_line_graph, container, false);
        ButterKnife.bind(this, rootView);
        toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        lineChart = (LineChartView) rootView.findViewById(R.id.line_chart);
        return rootView;
    }

    private void buildLineGraph(float[] values, String[] labels, float minBid, float maxBid, int isUp) {
        // Line chart customization
        LineSet dataSet = new LineSet(labels, values);
        dataSet.setThickness(Tools.fromDpToPx(2.5f));
        dataSet.setColor(ContextCompat.getColor(getContext(), android.R.color.white));
        lineChart.addData(dataSet);

        // Generic chart customization
        @ColorInt
        int bgColor;
        if (isUp == -1) {
            bgColor = ContextCompat.getColor(getContext(), R.color.red_low);
        } else if (isUp == 0) {
            bgColor = ContextCompat.getColor(getContext(), R.color.blue_flat);
        } else {
            bgColor = ContextCompat.getColor(getContext(), R.color.green_high);
        }

        lineChart.setXAxis(false);
        lineChart.setYAxis(false);
        lineChart.setBackgroundColor(bgColor);
        int padding = getResources().getDimensionPixelSize(R.dimen.content_padding);
        lineChart.setPadding(padding, padding * 3, padding, padding);

        // Paint object used to draw Grid
        Paint gridPaint = new Paint();
        gridPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_light_hint));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setAntiAlias(true);
        gridPaint.setStrokeWidth(Tools.fromDpToPx(1f));
        gridPaint.setPathEffect(new DashPathEffect(new float[]{8.0f, 8.0f}, 0));
        lineChart.setGrid(ChartView.GridType.HORIZONTAL, 10, 10, gridPaint);

        // Labels
        lineChart.setXLabels(AxisController.LabelPosition.OUTSIDE);
        lineChart.setYLabels(AxisController.LabelPosition.OUTSIDE);
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
            lineChart.setAxisBorderValues(minValue, maxValue, 1);
        } else {
            if (step % 2 == 0) {
                lineChart.setAxisBorderValues(minValue, maxValue, step / 2);
            } else {
                lineChart.setAxisBorderValues(minValue, maxValue + 1, (step + 1) / 2);
            }
        }

        lineChart.setLabelsFormat(new DecimalFormat("#"));
        lineChart.setLabelsColor(ContextCompat.getColor(getContext(), R.color.text_light_secondary));
        lineChart.setFontSize(42);
        lineChart.setAxisLabelsSpacing(48f);

        // Animation customization
        int[] order = new int[values.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        Animation anim = new Animation(750);
        anim.setEasing(new LinearEase());
        anim.setOverlap(0.5f, order);
        lineChart.show(anim);
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
            if (toolbar != null) {
                activity.setSupportActionBar(toolbar);
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
            List<Float> stockValues = new ArrayList<>();
            List<String> stockLabels = new ArrayList<>();
            float minBid = Float.MAX_VALUE;
            float maxBid = Float.MIN_VALUE;
            Integer[] dateStamps = new Integer[]{0, 1, 2, 3, 4};
            int offset = 0;

            // Calculate positions of labels to be displayed on x axis
            if (data.getCount() % 4 == 1) {
                dateStamps[1] = (data.getCount() - 1) / 4;
                dateStamps[2] = (data.getCount() - 1) / 2;
                dateStamps[3] = (data.getCount() - 1) / 4 + (data.getCount() - 1) / 2;
                dateStamps[4] = data.getCount() - 1;
            } else if (data.getCount() > 4) {
                offset = (data.getCount() % 4) == 0 ? 1 : 5 - data.getCount() % 4;
                int count = data.getCount() + offset;
                if (offset == 1) {
                    dateStamps[1] = ((count - 1) / 4) - 1;
                    dateStamps[2] = ((count - 1) / 2) - 1;
                    dateStamps[3] = ((count - 1) / 4 + (count - 1) / 2) - 1;
                }
                if (offset == 2) {
                    dateStamps[1] = ((count - 1) / 4) - 1;
                    dateStamps[2] = ((count - 1) / 2) - 2;
                    dateStamps[3] = ((count - 1) / 4 + (count - 1) / 2) - 2;
                }
                if (offset == 3) {
                    dateStamps[1] = ((count - 1) / 4) - 1;
                    dateStamps[2] = ((count - 1) / 2) - 2;
                    dateStamps[3] = ((count - 1) / 4 + (count - 1) / 2) - 3;
                }
                dateStamps[4] = data.getCount() - 1;
            }

            // Get stock values
            for (int position = 0; position < data.getCount(); position++) {
                data.moveToPosition(position);
                String bid = data.getString(data.getColumnIndex(QuoteColumns.BID_PRICE));
                float bidValue = bid.equals(getString(R.string.data_not_available))
                        ? 0f : Float.parseFloat(bid);
                minBid = Math.min(minBid, bidValue);
                maxBid = Math.max(maxBid, bidValue);
                stockValues.add(bidValue);
                if (Arrays.asList(dateStamps).contains(position)) {
                    stockLabels.add(Utils.formatGraphDateLabels(
                            data.getString(data.getColumnIndex(QuoteColumns.CREATED))));
                } else {
                    stockLabels.add("");
                }
            }

            // Duplicate data point if there is only one so line always shows on the graph
            if (stockValues.size() == 1) {
                stockValues.add(stockValues.get(0));
                stockLabels.add(Utils.formatGraphDateLabels(
                        data.getString(data.getColumnIndex(QuoteColumns.CREATED))));
            }

            // Add arbitrary points so labels format nicely
            if (offset > 0) {
                stockValues.add(1, (stockValues.get(0) + stockValues.get(1)) / 2);
                stockLabels.add(1, "");
                if (offset > 1) {
                    stockValues.add(dateStamps[1] + 2,
                            (stockValues.get(dateStamps[1]) + stockValues.get(dateStamps[1] + 1)) / 2);
                    stockLabels.add(dateStamps[1] + 2, "");
                    if (offset > 2) {
                        stockValues.add(dateStamps[2] + 3,
                                (stockValues.get(dateStamps[2]) + stockValues.get(dateStamps[2] + 1)) / 2);
                        stockLabels.add(dateStamps[2] + 3, "");
                    }
                }
            }

            float[] valuesArray = new float[stockValues.size()];
            String[] labelsArray = stockLabels.toArray(new String[stockLabels.size()]);
            for (int i = 0; i < valuesArray.length; i++) {
                valuesArray[i] = stockValues.get(i);
            }
            buildLineGraph(valuesArray, labelsArray, minBid, maxBid, isUp);
        }
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
    }
}
