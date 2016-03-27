package com.sam_chordas.android.stockhawk.widget;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;

/**
 * Intent Service used to update Quote Widgets.
 */
public class QuoteWidgetIntentService extends IntentService {
    public QuoteWidgetIntentService() {
        super("QuoteWidgetIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Retrieve all of the Today widget ids: these are the widgets we need to update
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,
                QuoteWidgetProvider.class));

        // Load data from the database
        String symbol = "AMZN";
        Cursor data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.NAME, QuoteColumns.BID_PRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.IS_UP, QuoteColumns.IS_CURRENT},
                QuoteColumns.SYMBOL + " = ? AND " + QuoteColumns.IS_CURRENT + " = ?",
                new String[]{symbol, "1"},
                null);

        if (data == null) {
            return;
        }
        if (!data.moveToFirst()) {
            data.close();
            return;
        }

        //String symbol = data.getString(data.getColumnIndex(QuoteColumns.SYMBOL));
        String name = data.getString(data.getColumnIndex(QuoteColumns.NAME));
        String price = data.getString(data.getColumnIndex(QuoteColumns.BID_PRICE));
        String change = data.getString(data.getColumnIndex(QuoteColumns.PERCENT_CHANGE));
        int isUp = data.getInt(data.getColumnIndex(QuoteColumns.IS_UP));

        // Perform this loop procedure for each Today widget
        for (int appWidgetId : appWidgetIds) {
            int layoutId = R.layout.widget_large;
            RemoteViews views = new RemoteViews(getPackageName(), layoutId);

            // Get correct color & icon
            int color = R.color.blue_flat;
            int icon = R.drawable.ic_trending_flat_18dp;
            if (isUp == -1) {
                icon = R.drawable.ic_trending_down_18dp;
                color = R.color.red_low;
            } else if (isUp == 1){
                icon = R.drawable.ic_trending_up_18dp;
                color = R.color.green_high;
            }

            // Add the data to the RemoteViews
            views.setImageViewResource(R.id.widget_icon, icon);
            views.setTextViewText(R.id.widget_stock_symbol, symbol);
            views.setTextViewText(R.id.widget_stock_name, name);
            views.setTextViewText(R.id.widget_bid_price, price);
            views.setTextViewText(R.id.widget_change, change);
            views.setTextColor(R.id.widget_change, ContextCompat.getColor(this, color));
            views.setContentDescription(R.id.widget_icon, symbol);

            // Create an Intent to launch MainActivity
            Intent launchIntent = new Intent(this, MyStocksActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, 0);
            views.setOnClickPendingIntent(R.id.widget_large, pendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
