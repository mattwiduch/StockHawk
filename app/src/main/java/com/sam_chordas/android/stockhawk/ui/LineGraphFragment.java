package com.sam_chordas.android.stockhawk.ui;

import android.database.Cursor;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
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

public class LineGraphFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
    private static final int CURSOR_LOADER_ID = 0;
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

    private final float[][] mValues = {{3.5f, 4.7f, 4.3f, 8f, 6.5f, 9.9f, 7f, 8.3f, 7.0f},
            {4.5f, 2.5f, 2.5f, 9f, 4.5f, 9.5f, 5f, 8.3f, 1.8f}};

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

    private void buildLineGraph(int isUp) {
        int color = isUp == 1 ? getResources().getColor(R.color.green_high) : getResources().getColor(R.color.red_low);
        // Line chart customization
        LineSet dataset = new LineSet(new String[] {"", "", "", "", "", "", "", "", ""}, mValues[1]);
        dataset.setThickness(Tools.fromDpToPx(2.5f));
        dataset.setColor(Color.parseColor("#ffffff"));
        dataset.setDotsRadius(Tools.fromDpToPx(4.5f));
        dataset.setDotsColor(color);
        dataset.setDotsStrokeColor(Color.parseColor("#ffffff"));
        dataset.setDotsStrokeThickness(6f);
        mChart.addData(dataset);

// Generic chart customization
        mChart.setXAxis(false);
        mChart.setYAxis(false);
        mChart.setBackgroundColor(color);
        int padding = getResources().getDimensionPixelSize(R.dimen.content_padding);
        mChart.setPadding(padding, padding*3, padding, padding + padding/2);


// Paint object used to draw Grid
        Paint gridPaint = new Paint();
        gridPaint.setColor(getResources().getColor(R.color.text_light_hint));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setAntiAlias(true);
        gridPaint.setStrokeWidth(Tools.fromDpToPx(1f));
        gridPaint.setPathEffect(new DashPathEffect(new float[]{8.0f, 8.0f}, 0));
        mChart.setGrid(ChartView.GridType.HORIZONTAL, gridPaint);

// Labels
        mChart.setXLabels(AxisController.LabelPosition.NONE);
        mChart.setYLabels(AxisController.LabelPosition.OUTSIDE);
        mChart.setAxisBorderValues(0, 100, 50);
        mChart.setLabelsFormat(new DecimalFormat("#"));
        mChart.setLabelsColor(getResources().getColor(R.color.text_light_secondary));
        mChart.setFontSize(45);
        mChart.setAxisLabelsSpacing(48f);

// Animation customization
        Animation anim = new Animation();
        anim.setEasing(new CircEase());
        anim.setOverlap(0.5f, new int[]{3, 2, 4, 1, 7, 5, 0, 6, 8});
        anim.setStartPoint(0.0f, 1.0f);
        mChart.show(anim);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),
                QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.NAME, QuoteColumns.BID_PRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.IS_UP},
                QuoteColumns.SYMBOL + " = ?",
                new String[]{mStockSymbol},
                QuoteColumns.CREATED + " ASC");
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        if (data.moveToLast()) {
            AppCompatActivity activity = (AppCompatActivity)getActivity();
            if (mToolbar != null) {
                activity.setSupportActionBar(mToolbar);
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                activity.getSupportActionBar().setTitle(data.getString(data.getColumnIndex(QuoteColumns.NAME)));
            }

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

            buildLineGraph(data.getInt(data.getColumnIndex(QuoteColumns.IS_UP)));
        }
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {}

//    /** Override up button behaviour so it navigates back to parent activity without recreating it. */
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case android.R.id.home:
//                getActivity().onBackPressed();
//                return (true);
//        }
//
//        return (super.onOptionsItemSelected(item));
//    }
}
